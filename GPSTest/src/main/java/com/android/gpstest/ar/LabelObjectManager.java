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

import android.graphics.Paint;
import android.graphics.Typeface;

import com.android.gpstest.ar.util.FixedPoint;
import com.android.gpstest.ar.util.GLBuffer;
import com.android.gpstest.ar.util.LabelMaker;
import com.android.gpstest.ar.util.SkyRegionMap;
import com.android.gpstest.ar.util.TextureManager;
import com.android.gpstest.ar.util.TextureReference;
import com.android.gpstest.util.MathUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/**
 * Manages rendering of text labels.
 *
 * @author James Powell
 */
public class LabelObjectManager extends RendererObjectManager {
    // Should we compute the regions for the labels?
    // If false, we just put them in the catchall region.
    private static final boolean COMPUTE_REGIONS = true;

    private Paint mLabelPaint = null;
    private LabelMaker mLabelMaker = null;
    private Label[] mLabels = new Label[0];
    private SkyRegionMap<ArrayList<Label>> mSkyRegions = new SkyRegionMap<ArrayList<Label>>();

    private IntBuffer mQuadBuffer;

    // These are intermediate variables set in beginDrawing() and used in
    // draw() to make the transformations more efficient
    private Vector3 mLabelOffset = new Vector3(0, 0, 0);
    private float mDotProductThreshold;

    private TextureReference mTexture = null;

    public LabelObjectManager(int layer, TextureManager textureManager) {
        super(layer, textureManager);

        mLabelPaint = new Paint();
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setTypeface(Typeface.create("Verdana", Typeface.NORMAL));

        ByteBuffer quadBuffer = ByteBuffer.allocateDirect(4 * 2 * 4);
        quadBuffer.order(ByteOrder.nativeOrder());
        mQuadBuffer = quadBuffer.asIntBuffer();
        mQuadBuffer.position(0);
        // A quad with size 1 on each size, so we just need to multiply
        // by the label's width and height to get it to the right size for each
        // label.
        float[] vertices = {
                -0.5f, -0.5f,   // lower left
                -0.5f, 0.5f,   // upper left
                0.5f, -0.5f,   // lower right
                0.5f, 0.5f};  // upper right
        for (float f : vertices) {
            mQuadBuffer.put(FixedPoint.floatToFixedPoint(f));
        }
        mQuadBuffer.position(0);

        // We want to initialize the labels of a sky region to an empty list.
        mSkyRegions.setRegionDataFactory(
                new SkyRegionMap.RegionDataFactory<ArrayList<Label>>() {
                    public ArrayList<Label> construct() {
                        return new ArrayList<Label>();
                    }
                });
    }

    @Override
    public void reload(GL10 gl, boolean fullReload) {
        // We need to regenerate the texture.  If we're re-creating the surface
        // (fullReload=true), all resources were automatically released by OpenGL,
        // so we don't want to try to release it again.  Otherwise, we need to
        // release it to avoid a resource leak (mLabelMaker.shutdown takes
        // care of freeing the texture).
        //
        // TODO(jpowell): This whole reload interface is horrendous, and I should
        // make a better way of scheduling reloads.
        //
        // TODO(jpowell): LabelMaker and textures have gone through some changes
        // since they were originally created, and I feel like it might not make
        // sense for it to own the texture anymore.  I should see if I can just
        // let it create but not own it.
        if (!fullReload && mLabelMaker != null) {
            mLabelMaker.shutdown(gl);
        }

        mLabelMaker = new LabelMaker(true);
        mTexture = mLabelMaker.initialize(gl, mLabelPaint, mLabels,
                getRenderState().getResources(),
                textureManager());
    }

    public void updateObjects(List<TextSource> labels, EnumSet<UpdateType> updateType) {
        if (updateType.contains(UpdateType.Reset)) {
            mLabels = new Label[labels.size()];
            for (int i = 0; i < labels.size(); i++) {
                mLabels[i] = new Label(labels.get(i));
            }
            queueForReload(false);
        } else if (updateType.contains(UpdateType.UpdatePositions)) {
            if (labels.size() != mLabels.length) {
                logUpdateMismatch("LabelObjectManager", mLabels.length, labels.size(), updateType);
                return;
            }
            // Since we don't store the positions in any GPU memory, and do the
            // transformations manually, we can just update the positions stored
            // on the label objects.
            for (int i = 0; i < mLabels.length; i++) {
                GeocentricCoordinates pos = labels.get(i).getLocation();
                mLabels[i].x = pos.x;
                mLabels[i].y = pos.y;
                mLabels[i].z = pos.z;
            }
        }

        // Put all of the labels in their sky regions.
        // TODO(jpowell): Get this from the label source itself once it supports
        // this.
        mSkyRegions.clear();
        for (Label l : mLabels) {
            int region;
            if (COMPUTE_REGIONS) {
                region = SkyRegionMap.getObjectRegion(new GeocentricCoordinates(l.x, l.y, l.z));
            } else {
                region = SkyRegionMap.CATCHALL_REGION_ID;
            }
            mSkyRegions.getRegionData(region).add(l);
        }
    }

    @Override
    protected void drawInternal(GL10 gl) {
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_MODULATE);

        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glActiveTexture(GL10.GL_TEXTURE0);
        mTexture.bind(gl);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_REPEAT);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_REPEAT);

        beginDrawing(gl);

        // Draw the labels for the active sky regions.
        SkyRegionMap.ActiveRegionData activeRegions = getRenderState().getActiveSkyRegions();
        ArrayList<ArrayList<Label>> allActiveLabels =
                mSkyRegions.getDataForActiveRegions(activeRegions);

        for (ArrayList<Label> labelsInRegion : allActiveLabels) {
            for (Label l : labelsInRegion) {
                drawLabel(gl, l);
            }
        }

        endDrawing(gl);
    }

    /**
     * Begin drawing labels. Sets the OpenGL state for rapid drawing.
     *
     * @param gl
     */
    public void beginDrawing(GL10 gl) {
        mTexture.bind(gl);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL10.GL_GREATER, 0.5f);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        // We're going to do the transformation on the CPU, so set the matrices
        // to the identity
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0, getRenderState().getScreenWidth(),
                0, getRenderState().getScreenHeight(),
                -1, 1);

        GLBuffer.unbind((GL11) gl);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

        RenderStateInterface rs = super.getRenderState();

        float viewWidth = rs.getScreenWidth();
        float viewHeight = rs.getScreenHeight();

        Matrix4x4 rotation = Matrix4x4.createRotation(rs.getUpAngle(), rs.getLookDir());
        mLabelOffset = Matrix4x4.multiplyMV(rotation, rs.getUpDir());

        // If a label isn't within the field of view angle from the target vector, it can't
        // be on the screen.  Compute the cosine of this angle so we can quickly identify these.
        // TODO(jpowell): I know I can make this tighter - do so.
        final float DEGREES_TO_RADIANS = MathUtils.PI / 180.0f;
        mDotProductThreshold = (float) Math.cos(rs.getRadiusOfView() * DEGREES_TO_RADIANS *
                (1 + viewWidth / viewHeight) * 0.5f);
    }

    /**
     * Ends the drawing and restores the OpenGL state.
     *
     * @param gl
     */
    public void endDrawing(GL10 gl) {
        gl.glDisable(GL10.GL_ALPHA_TEST);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glDisable(GL10.GL_TEXTURE_2D);

        gl.glColor4x(FixedPoint.ONE, FixedPoint.ONE, FixedPoint.ONE, FixedPoint.ONE);
    }

    /**
     * A private class which extends the LabelMaker's label data with an xyz position and rgba color values.
     * For the red-eye mode, it's easier to set the color in the texture to white and set the color when we render
     * the label than to have two textures, one with red labels and one without.
     */
    private static class Label extends LabelMaker.LabelData {
        public Label(TextSource ts) {
            super(ts.getText(), 0xffffffff, ts.getFontSize());
            if (ts.getText() == null || ts.getText().equals("")) {
                throw new RuntimeException("Bad Label: " + ts.getClass());
            }

            x = ts.getLocation().x;
            y = ts.getLocation().y;
            z = ts.getLocation().z;

            offset = ts.getOffset();

            int rgb = ts.getColor();
            int a = 0xff;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            fixedA = FixedPoint.floatToFixedPoint(a / 255.0f);
            fixedB = FixedPoint.floatToFixedPoint(b / 255.0f);
            fixedG = FixedPoint.floatToFixedPoint(g / 255.0f);
            fixedR = FixedPoint.floatToFixedPoint(r / 255.0f);
        }

        public float x;
        public float y;
        public float z;

        // The distance this should be rendered underneath the specified position, in world coordinates.
        public float offset;

        // Fixed point color values
        public int fixedR;
        public int fixedG;
        public int fixedB;
        public int fixedA;
    }


    private void drawLabel(GL10 gl, Label label) {
        Vector3 lookDir = getRenderState().getLookDir();
        if (lookDir.x * label.x + lookDir.y * label.y + lookDir.z * label.z < mDotProductThreshold) {
            return;
        }

        // Offset the label to be underneath the given position (so a label will
        // always appear underneath a star no matter how the phone is rotated)
        Vector3 v = new Vector3(
                label.x - mLabelOffset.x * label.offset,
                label.y - mLabelOffset.y * label.offset,
                label.z - mLabelOffset.z * label.offset);

        Vector3 screenPos = Matrix4x4.transformVector(
                getRenderState().getTransformToScreenMatrix(),
                v);

        // We want this to align consistently with the pixels on the screen, so we
        // snap to the nearest x/y coordinate, and add a magic offset of less than
        // half a pixel.  Without this, rounding error can cause the bottom and
        // top of a label to be one pixel off, which results in a noticeable
        // distortion in the text.
        final float MAGIC_OFFSET = 0.25f;
        screenPos.x = (int) screenPos.x + MAGIC_OFFSET;
        screenPos.y = (int) screenPos.y + MAGIC_OFFSET;

        gl.glPushMatrix();

        gl.glTranslatef(screenPos.x, screenPos.y, 0);
        gl.glRotatef(MathUtils.RADIANS_TO_DEGREES * getRenderState().getUpAngle(), 0, 0, -1);
        gl.glScalef(label.getWidthInPixels(), label.getHeightInPixels(), 1);

        gl.glVertexPointer(2, GL10.GL_FIXED, 0, mQuadBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, label.getTexCoords());
        if (getRenderState().getNightVisionMode()) {
            gl.glColor4x(FixedPoint.ONE, 0, 0, label.fixedA);
        } else {
            gl.glColor4x(label.fixedR, label.fixedG, label.fixedB, label.fixedA);
        }
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

        gl.glPopMatrix();
    }
}
