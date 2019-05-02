// Copyright 2009 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar;

import android.content.res.Resources;
import android.opengl.GLU;
import android.util.Log;

import com.android.gpstest.ar.util.ColoredQuad;
import com.android.gpstest.ar.util.SearchHelper;
import com.android.gpstest.ar.util.TextureManager;
import com.android.gpstest.util.VectorUtil;

import javax.microedition.khronos.opengles.GL10;

public class OverlayManager extends RendererObjectManager {
    private int mWidth = 2;
    private int mHeight = 2;
    private Matrix4x4 mGeoToViewerTransform = Matrix4x4.createIdentity();
    private Vector3 mLookDir = new Vector3(0, 0, 0);
    private Vector3 mUpDir = new Vector3(0, 1, 0);
    private Vector3 mTransformedLookDir = new Vector3(0, 0, 0);
    private Vector3 mTransformedUpDir = new Vector3(0, 1, 0);
    private boolean mMustUpdateTransformedOrientation = true;

    private boolean mSearching = false;
    private SearchHelper mSearchHelper = new SearchHelper();
    private ColoredQuad mDarkQuad = null;
    private SearchArrow mSearchArrow = new SearchArrow();
    private CrosshairOverlay mCrosshair = new CrosshairOverlay();

    private TextureManager mTextureManager;

    public OverlayManager(int layer, TextureManager manager) {
        super(layer, manager);
    }

    @Override
    public void reload(GL10 gl, boolean fullReload) {
        Resources res = getRenderState().getResources();
        mSearchArrow.reloadTextures(gl, res, textureManager());
        mCrosshair.reloadTextures(gl, res, textureManager());
    }

    public void resize(GL10 gl, int screenWidth, int screenHeight) {
        mWidth = screenWidth;
        mHeight = screenHeight;

        // If the search target is within this radius of the center of the screen, the user is
        // considered to have "found" it.
        float searchTargetRadius = Math.min(screenWidth, screenHeight) - 20;
        mSearchHelper.setTargetFocusRadius(searchTargetRadius);
        mSearchHelper.resize(screenWidth, screenHeight);

        mSearchArrow.resize(gl, screenWidth, screenHeight, searchTargetRadius);
        mCrosshair.resize(gl, screenWidth, screenHeight);

        mDarkQuad = new ColoredQuad(0, 0, 0, 0.6f,
                0, 0, 0,
                screenWidth, 0, 0,
                0, screenHeight, 0);
    }

    public void setViewOrientation(GeocentricCoordinates lookDir, GeocentricCoordinates upDir) {
        mLookDir = lookDir;
        mUpDir = upDir;
        mMustUpdateTransformedOrientation = true;
    }

    @Override
    public void drawInternal(GL10 gl) {
        updateTransformedOrientationIfNecessary();

        setupMatrices(gl);

        if (mSearching) {
            mSearchHelper.setTransform(getRenderState().getTransformToDeviceMatrix());
            mSearchHelper.checkState();

            float transitionFactor = mSearchHelper.getTransitionFactor();

            // Darken the background.
            mDarkQuad.draw(gl);

            // Draw the crosshair.
            mCrosshair.draw(gl, mSearchHelper, getRenderState().getNightVisionMode());

            // Draw the search arrow.
            mSearchArrow.draw(gl, mTransformedLookDir, mTransformedUpDir, mSearchHelper,
                    getRenderState().getNightVisionMode());
        }

        restoreMatrices(gl);
    }

    // viewerUp MUST be normalized.
    public void setViewerUpDirection(GeocentricCoordinates viewerUp) {
        // Log.d("OverlayManager", "Setting viewer up " + viewerUp);
        if ((float) Math.abs(viewerUp.y) < 0.999f) {
            Vector3 cp = VectorUtil.crossProduct(viewerUp, new Vector3(0, 1, 0));
            cp = VectorUtil.normalized(cp);
            mGeoToViewerTransform = Matrix4x4.createRotation((float) Math.acos(viewerUp.y), cp);
        } else {
            mGeoToViewerTransform = Matrix4x4.createIdentity();
        }
        mMustUpdateTransformedOrientation = true;
    }

    public void enableSearchOverlay(GeocentricCoordinates target, String targetName) {
        Log.d("OverlayManager", "Searching for " + target);
        mSearching = true;
        mSearchHelper.setTransform(getRenderState().getTransformToDeviceMatrix());
        mSearchHelper.setTarget(target, targetName);
        Vector3 transformedPosition = Matrix4x4.multiplyMV(mGeoToViewerTransform, target);
        mSearchArrow.setTarget(transformedPosition);
        queueForReload(false);
    }

    public void disableSearchOverlay() {
        mSearching = false;
    }

    private void setupMatrices(GL10 gl) {
        // Save the matrix values.
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        float left = mWidth / 2;
        float bottom = mHeight / 2;
        gl.glLoadIdentity();
        GLU.gluOrtho2D(gl, left, -left, bottom, -bottom);
    }

    private void restoreMatrices(GL10 gl) {
        // Restore the matrices.
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    private void updateTransformedOrientationIfNecessary() {
        if (mMustUpdateTransformedOrientation && mSearching) {
            mTransformedLookDir = Matrix4x4.multiplyMV(mGeoToViewerTransform, mLookDir);
            mTransformedUpDir = Matrix4x4.multiplyMV(mGeoToViewerTransform, mUpDir);
            mMustUpdateTransformedOrientation = false;
        }
    }
}
