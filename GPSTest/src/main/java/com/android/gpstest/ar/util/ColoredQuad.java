// Copyright 2008 Google Inc.
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

package com.android.gpstest.ar.util;

import javax.microedition.khronos.opengles.GL10;

public class ColoredQuad {
    public ColoredQuad(float r, float g, float b, float a,
                       float px, float py, float pz,
                       float ux, float uy, float uz,
                       float vx, float vy, float vz) {
        mPosition = new VertexBuffer(12);
        VertexBuffer vertexBuffer = mPosition;

        // Upper left
        vertexBuffer.addPoint(px - ux - vx, py - uy - vy, pz - uz - vz);

        // upper left
        vertexBuffer.addPoint(px - ux + vx, py - uy + vy, pz - uz + vz);

        // lower right
        vertexBuffer.addPoint(px + ux - vx, py + uy - vy, pz + uz - vz);

        // upper right
        vertexBuffer.addPoint(px + ux + vx, py + uy + vy, pz + uz + vz);

        mR = r;
        mG = g;
        mB = b;
        mA = a;
    }

    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

        // Enable blending if alpha != 1.
        if (mA != 1) {
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        }

        gl.glDisable(GL10.GL_TEXTURE_2D);

        mPosition.set(gl);
        gl.glColor4f(mR, mG, mB, mA);

        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

        gl.glEnable(GL10.GL_TEXTURE_2D);

        // Disable blending if alpha != 1.
        if (mA != 1) {
            gl.glDisable(GL10.GL_BLEND);
        }
    }

    private VertexBuffer mPosition = null;
    private float mR, mG, mB, mA;
}
