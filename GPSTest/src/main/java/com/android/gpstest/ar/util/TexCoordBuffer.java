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

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class TexCoordBuffer {
    public TexCoordBuffer(int numVertices) {
        reset(numVertices);
    }

    // Creates an empty buffer.  Must call reset() before adding vertices.
    public TexCoordBuffer() {
        mNumVertices = 0;
    }

    public TexCoordBuffer(boolean useVBO) {
        mNumVertices = 0;
        mUseVBO = useVBO;
    }

    public int size() {
        return mNumVertices;
    }

    public void reset(int numVertices) {
        if (numVertices < 0) {
            Log.e("TexCoordBuffer", "reset attempting to set numVertices to " + numVertices);
            numVertices = 0;
        }
        mNumVertices = numVertices;
        regenerateBuffer();
    }

    // Call this when we have to re-create the surface and reloading all OpenGL resources.
    public void reload() {
        mGLBuffer.reload();
    }

    public void addTexCoords(float u, float v) {
        mTexCoordBuffer.put(FixedPoint.floatToFixedPoint(u));
        mTexCoordBuffer.put(FixedPoint.floatToFixedPoint(v));
    }

    public void set(GL10 gl) {
        if (mNumVertices == 0) {
            return;
        }
        mTexCoordBuffer.position(0);

        if (mUseVBO && GLBuffer.canUseVBO()) {
            GL11 gl11 = (GL11) gl;
            mGLBuffer.bind(gl11, mTexCoordBuffer, 4 * mTexCoordBuffer.capacity());
            gl11.glTexCoordPointer(2, GL10.GL_FIXED, 0, 0);
        } else {
            gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mTexCoordBuffer);
        }
    }

    private void regenerateBuffer() {
        if (mNumVertices == 0) {
            return;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 2 * mNumVertices);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer ib = bb.asIntBuffer();
        ib.position(0);
        mTexCoordBuffer = ib;
    }

    private IntBuffer mTexCoordBuffer = null;
    private int mNumVertices = 0;
    private GLBuffer mGLBuffer = new GLBuffer(GL11.GL_ARRAY_BUFFER);
    private boolean mUseVBO = false;
}
