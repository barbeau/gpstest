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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class ColorBuffer {

    public ColorBuffer(int numVertices) {
        reset(numVertices);
    }

    public ColorBuffer() {
        mNumVertices = 0;
    }

    public ColorBuffer(boolean useVBO) {
        mNumVertices = 0;
        mUseVBO = useVBO;
    }

    public int size() {
        return mNumVertices;
    }

    public void reset(int numVertices) {
        mNumVertices = numVertices;
        regenerateBuffer();
    }

    // Call this when we have to re-create the surface and reloading all OpenGL resources.
    public void reload() {
        mGLBuffer.reload();
    }

    public void addColor(int a, int r, int g, int b) {
        addColor(((a & 0xff) << 24) | ((b & 0xff) << 16) | ((g & 0xff) << 8) | (r & 0xff));
    }

    public void addColor(int abgr) {
        mColorBuffer.put(abgr);
    }

    public void set(GL10 gl) {
        if (mNumVertices == 0) {
            return;
        }
        mColorBuffer.position(0);

        if (mUseVBO && GLBuffer.canUseVBO()) {
            GL11 gl11 = (GL11) gl;
            mGLBuffer.bind(gl11, mColorBuffer, 4 * mColorBuffer.capacity());
            gl11.glColorPointer(4, GL10.GL_UNSIGNED_BYTE, 0, 0);
        } else {
            gl.glColorPointer(4, GL10.GL_UNSIGNED_BYTE, 0, mColorBuffer);
        }
    }

    private void regenerateBuffer() {
        if (mNumVertices == 0) {
            return;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(4 * mNumVertices);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer ib = bb.asIntBuffer();
        ib.position(0);
        mColorBuffer = ib;
    }

    private IntBuffer mColorBuffer = null;
    private int mNumVertices;
    private GLBuffer mGLBuffer = new GLBuffer(GL11.GL_ARRAY_BUFFER);
    private boolean mUseVBO;
}
