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

/**
 * A simple class for rendering a textured quad.
 *
 * @author James Powell
 */
public class TexturedQuad {
    private TexCoordBuffer mTexCoords = null;
    private VertexBuffer mPosition = null;
    private TextureReference mTexture = null;

    /**
     * Constructs the textured quad.
     * p is the point at the center of the quad.
     * u is the vector from the center of quad, pointing right.
     * v is the vector from the center of the quad, pointing up.
     * The four vertices of the quad are: by p +/- u +/- v
     *
     * @param tex The texture to apply to the quad
     * @param px
     * @param py
     * @param pz
     * @param ux
     * @param uy
     * @param uz
     * @param vx
     * @param vy
     * @param vz
     */
    public TexturedQuad(TextureReference tex,
                        float px, float py, float pz,
                        float ux, float uy, float uz,
                        float vx, float vy, float vz) {
        mPosition = new VertexBuffer(12);
        mTexCoords = new TexCoordBuffer(12);

        VertexBuffer vertexBuffer = mPosition;
        TexCoordBuffer texCoordBuffer = mTexCoords;

        // Upper left
        vertexBuffer.addPoint(px - ux - vx, py - uy - vy, pz - uz - vz);
        texCoordBuffer.addTexCoords(0, 1);

        // upper left
        vertexBuffer.addPoint(px - ux + vx, py - uy + vy, pz - uz + vz);
        texCoordBuffer.addTexCoords(0, 0);

        // lower right
        vertexBuffer.addPoint(px + ux - vx, py + uy - vy, pz + uz - vz);
        texCoordBuffer.addTexCoords(1, 1);

        // upper right
        vertexBuffer.addPoint(px + ux + vx, py + uy + vy, pz + uz + vz);
        texCoordBuffer.addTexCoords(1, 0);

        mTexture = tex;
    }

    public void draw(GL10 gl) {
        gl.glEnable(GL10.GL_TEXTURE_2D);
        mTexture.bind(gl);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

        mPosition.set(gl);
        mTexCoords.set(gl);

        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
