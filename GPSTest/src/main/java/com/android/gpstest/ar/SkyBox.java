// Copyright 2009 Google Inc.
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

import android.util.Log;

import com.android.gpstest.ar.util.ColorBuffer;
import com.android.gpstest.ar.util.IndexBuffer;
import com.android.gpstest.ar.util.TextureManager;
import com.android.gpstest.ar.util.VertexBuffer;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.VectorUtil;

import javax.microedition.khronos.opengles.GL10;

public class SkyBox extends RendererObjectManager {

    public SkyBox(int layer, TextureManager textureManager) {
        super(layer, textureManager);

        int numVertices = NUM_VERTEX_BANDS * NUM_STEPS_IN_BAND;
        int numIndices = (NUM_VERTEX_BANDS - 1) * NUM_STEPS_IN_BAND * 6;
        mVertexBuffer.reset(numVertices);
        mColorBuffer.reset(numVertices);
        mIndexBuffer.reset(numIndices);

        float[] sinAngles = new float[NUM_STEPS_IN_BAND];
        float[] cosAngles = new float[NUM_STEPS_IN_BAND];

        float angleInBand = 0;
        float dAngle = MathUtils.TWO_PI / (NUM_STEPS_IN_BAND - 1);
        for (int i = 0; i < NUM_STEPS_IN_BAND; i++) {
            sinAngles[i] = (float) Math.sin(angleInBand);
            cosAngles[i] = (float) Math.cos(angleInBand);
            angleInBand += dAngle;
        }

        float bandStep = 2.0f / (NUM_VERTEX_BANDS - 1) + EPSILON;

        VertexBuffer vb = mVertexBuffer;
        ColorBuffer cb = mColorBuffer;
        float bandPos = 1;
        for (int band = 0; band < NUM_VERTEX_BANDS; band++, bandPos -= bandStep) {
            int color;
            if (bandPos > 0) {
                // TODO(jpowell): This isn't really intensity, name it more appropriately.
                // I=70 at bandPos = 1, I=50 at bandPos = 0
                byte intensity = (byte) (bandPos * 20 + 50);
                color = (intensity << 16) | 0xff000000;
            } else {
                // I=40 at bandPos = -1, I=0 at bandPos = 0
                byte intensity = (byte) (bandPos * 40 + 40);
                color = (intensity << 16) | (intensity << 8) | intensity | 0xff000000;
            }

            float sinPhi = bandPos > -1 ? (float) Math.sqrt(1 - bandPos * bandPos) : 0;
            for (int i = 0; i < NUM_STEPS_IN_BAND; i++) {
                vb.addPoint(cosAngles[i] * sinPhi, bandPos, sinAngles[i] * sinPhi);
                cb.addColor(color);
            }
        }
        Log.d("SkyBox", "Vertices: " + vb.size());

        IndexBuffer ib = mIndexBuffer;

        // Set the indices for the first band.
        short topBandStart = 0;
        short bottomBandStart = NUM_STEPS_IN_BAND;
        for (short triangleBand = 0; triangleBand < NUM_VERTEX_BANDS - 1; triangleBand++) {
            for (short offsetFromStart = 0; offsetFromStart < NUM_STEPS_IN_BAND - 1; offsetFromStart++) {
                // Draw one quad as two triangles.
                short topLeft = (short) (topBandStart + offsetFromStart);
                short topRight = (short) (topLeft + 1);

                short bottomLeft = (short) (bottomBandStart + offsetFromStart);
                short bottomRight = (short) (bottomLeft + 1);

                // First triangle
                ib.addIndex(topLeft);
                ib.addIndex(bottomRight);
                ib.addIndex(bottomLeft);

                // Second triangle
                ib.addIndex(topRight);
                ib.addIndex(bottomRight);
                ib.addIndex(topLeft);
            }

            // Last quad: connect the end with the beginning.

            // Top left, bottom right, bottom left
            ib.addIndex((short) (topBandStart + NUM_STEPS_IN_BAND - 1));
            ib.addIndex(bottomBandStart);
            ib.addIndex((short) (bottomBandStart + NUM_STEPS_IN_BAND - 1));

            // Top right, bottom right, top left
            ib.addIndex(topBandStart);
            ib.addIndex(bottomBandStart);
            ib.addIndex((short) (topBandStart + NUM_STEPS_IN_BAND - 1));


            topBandStart += NUM_STEPS_IN_BAND;
            bottomBandStart += NUM_STEPS_IN_BAND;
        }
        Log.d("SkyBox", "Indices: " + ib.size());
    }

    @Override
    public void reload(GL10 gl, boolean fullReload) {
        mVertexBuffer.reload();
        mColorBuffer.reload();
        mIndexBuffer.reload();
    }

    public void setSunPosition(GeocentricCoordinates pos) {
        mSunPos = pos.copy();
        //Log.d("SkyBox", "SunPos: " + pos.toString());
    }

    @Override
    protected void drawInternal(GL10 gl) {
        if (getRenderState().getNightVisionMode()) {
            return;
        }

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glFrontFace(GL10.GL_CW);
        gl.glCullFace(GL10.GL_BACK);

        gl.glShadeModel(GL10.GL_SMOOTH);

        gl.glPushMatrix();

        // Rotate the sky box to the position of the sun.
        Vector3 cp = VectorUtil.crossProduct(new Vector3(0, 1, 0), mSunPos);
        cp = VectorUtil.normalized(cp);
        float angle = 180.0f / MathUtils.PI * (float) Math.acos(mSunPos.y);
        gl.glRotatef(angle, cp.x, cp.y, cp.z);

        mVertexBuffer.set(gl);
        mColorBuffer.set(gl);

        mIndexBuffer.draw(gl, GL10.GL_TRIANGLES);

        gl.glPopMatrix();
    }

    private static final short NUM_VERTEX_BANDS = 8;
    // This number MUST be even
    private static final short NUM_STEPS_IN_BAND = 10;

    // Used to make sure rounding error doesn't make us have off-by-one errors in our iterations.
    private static final float EPSILON = 1e-3f;


    VertexBuffer mVertexBuffer = new VertexBuffer(true);
    ColorBuffer mColorBuffer = new ColorBuffer(true);
    IndexBuffer mIndexBuffer = new IndexBuffer(true);
    GeocentricCoordinates mSunPos = new GeocentricCoordinates(0, 1, 0);
}
