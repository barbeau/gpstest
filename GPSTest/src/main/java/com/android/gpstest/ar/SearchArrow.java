// Copyright 2008 Google Inc. From StarDroid.
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

import com.android.gpstest.R;
import com.android.gpstest.ar.util.FixedPoint;
import com.android.gpstest.ar.util.SearchHelper;
import com.android.gpstest.ar.util.TextureManager;
import com.android.gpstest.ar.util.TextureReference;
import com.android.gpstest.ar.util.TexturedQuad;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.VectorUtil;

import javax.microedition.khronos.opengles.GL10;

public class SearchArrow {
    // The arrow quad is 10% of the screen width or height, whichever is smaller.
    private final float ARROW_SIZE = 0.1f;
    // The circle quad is 40% of the screen width or height, whichever is smaller.
    private final float CIRCLE_SIZE = 0.4f;

    // The target position is (1, theta, phi) in spherical coordinates.
    private float mTargetTheta = 0;
    private float mTargetPhi = 0;
    private TexturedQuad mCircleQuad = null;
    private TexturedQuad mArrowQuad = null;
    private float mArrowOffset = 0;
    private float mCircleSizeFactor = 1;
    private float mArrowSizeFactor = 1;
    private float mFullCircleScaleFactor = 1;

    private TextureReference mArrowTex = null;
    private TextureReference mCircleTex = null;

    public void reloadTextures(GL10 gl, Resources res, TextureManager textureManager) {
        gl.glEnable(GL10.GL_TEXTURE_2D);

        mArrowTex = textureManager.getTextureFromResource(gl, R.drawable.arrow);
        mCircleTex = textureManager.getTextureFromResource(gl, R.drawable.arrowcircle);

        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    public void resize(GL10 gl, int screenWidth, int screenHeight, float fullCircleSize) {
        mArrowSizeFactor = ARROW_SIZE * Math.min(screenWidth, screenHeight);
        mArrowQuad = new TexturedQuad(mArrowTex,
                0, 0, 0,
                0.5f, 0, 0,
                0, 0.5f, 0);

        mFullCircleScaleFactor = fullCircleSize;
        mCircleSizeFactor = CIRCLE_SIZE * mFullCircleScaleFactor;
        mCircleQuad = new TexturedQuad(mCircleTex,
                0, 0, 0,
                0.5f, 0, 0,
                0, 0.5f, 0);

        mArrowOffset = mCircleSizeFactor + mArrowSizeFactor;
    }

    public void draw(GL10 gl, Vector3 lookDir, Vector3 upDir, SearchHelper searchHelper,
                     boolean nightVisionMode) {
        float lookPhi = (float) Math.acos(lookDir.y);
        float lookTheta = (float) Math.atan2(lookDir.z, lookDir.x);

        // Positive diffPhi means you need to look up.
        float diffPhi = lookPhi - mTargetPhi;

        // Positive diffTheta means you need to look right.
        float diffTheta = lookTheta - mTargetTheta;

        // diffTheta could potentially be in the range from (-2*Pi, 2*Pi), but we need it
        // in the range (-Pi, Pi).
        if (diffTheta > MathUtils.PI) {
            diffTheta -= MathUtils.TWO_PI;
        } else if (diffTheta < -MathUtils.PI) {
            diffTheta += MathUtils.TWO_PI;
        }

        // The image I'm using is an arrow pointing right, so an angle of 0 corresponds to that.
        // This is why we're taking arctan(diffPhi / diffTheta), because diffTheta corresponds to
        // the amount we need to rotate in the xz plane and diffPhi in the up direction.
        float angle = (float) Math.atan2(diffPhi, diffTheta);

        // Need to add on the camera roll, which is the amount you need to rotate the vector (0, 1, 0)
        // about the look direction in order to get it in the same plane as the up direction.
        float roll = angleBetweenVectorsWithRespectToAxis(new Vector3(0, 1, 0), upDir, lookDir);

        angle += roll;

        // Distance is a normalized value of the distance.
        float distance = 1.0f / (1.414f * MathUtils.PI) *
                (float) Math.sqrt(diffTheta * diffTheta + diffPhi * diffPhi);

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        gl.glPushMatrix();
        gl.glRotatef(angle * 180.0f / MathUtils.PI, 0, 0, -1);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_BLEND);

        // 0 means the circle is not expanded at all.  1 means fully expanded.
        float expandFactor = searchHelper.getTransitionFactor();

        if (expandFactor == 0) {
            gl.glColor4x(FixedPoint.ONE, FixedPoint.ONE, FixedPoint.ONE, FixedPoint.ONE);

            float redFactor, blueFactor;
            if (nightVisionMode) {
                redFactor = 0.6f;
                blueFactor = 0;
            } else {
                redFactor = 1.0f - distance;
                blueFactor = distance;
            }

            gl.glTexEnvfv(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_COLOR,
                    new float[]{redFactor, 0.0f, blueFactor, 0.0f}, 0);

            gl.glPushMatrix();
            float circleScale = mCircleSizeFactor;
            gl.glScalef(circleScale, circleScale, circleScale);
            mCircleQuad.draw(gl);
            gl.glPopMatrix();

            gl.glPushMatrix();
            float arrowScale = mArrowSizeFactor;
            gl.glTranslatef(mArrowOffset * 0.5f, 0, 0);
            gl.glScalef(arrowScale, arrowScale, arrowScale);
            mArrowQuad.draw(gl);
            gl.glPopMatrix();
        } else {
            gl.glColor4x(FixedPoint.ONE, FixedPoint.ONE, FixedPoint.ONE,
                    FixedPoint.floatToFixedPoint(0.7f));

            gl.glTexEnvfv(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_COLOR,
                    new float[]{1, nightVisionMode ? 0 : 0.5f, 0, 0.0f}, 0);

            gl.glPushMatrix();
            float circleScale = mFullCircleScaleFactor * expandFactor +
                    mCircleSizeFactor * (1 - expandFactor);
            gl.glScalef(circleScale, circleScale, circleScale);
            mCircleQuad.draw(gl);
            gl.glPopMatrix();
        }
        gl.glPopMatrix();

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

        gl.glDisable(GL10.GL_BLEND);
    }

    public void setTarget(Vector3 position) {
        position = VectorUtil.normalized(position);
        mTargetPhi = (float) Math.acos(position.y);
        mTargetTheta = (float) Math.atan2(position.z, position.x);
    }

    // Given vectors v1 and v2, and an axis, this function returns the angle which you must rotate v1
    // by in order for it to be in the same plane as v2 and axis.  Assumes that all vectors are unit
    // vectors and v2 and axis are perpendicular.
    private static float angleBetweenVectorsWithRespectToAxis(Vector3 v1, Vector3 v2, Vector3 axis) {
        // Make v1 perpendicular to axis.  We want an orthonormal basis for the plane perpendicular
        // to axis.  After rotating v1, the projection of v1 and v2 into this plane should be equal.
        Vector3 v1proj = VectorUtil.difference(v1, VectorUtil.projectOntoUnit(v1, axis));
        v1proj = VectorUtil.normalized(v1proj);

        // Get the vector perpendicular to the one you're rotating and the axis.  Since axis and v1proj
        // are orthonormal, this one must be a unit vector perpendicular to all three.
        Vector3 perp = VectorUtil.crossProduct(axis, v1proj);

        // v2 is perpendicular to axis, so therefore it's already in the same plane as v1proj perp.
        float cosAngle = VectorUtil.dotProduct(v1proj, v2);
        float sinAngle = -VectorUtil.dotProduct(perp, v2);

        return (float) Math.atan2(sinAngle, cosAngle);
    }
}
