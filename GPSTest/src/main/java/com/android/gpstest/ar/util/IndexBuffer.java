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
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class IndexBuffer {
    public IndexBuffer(int numVertices) {
        this(numVertices, false);
    }

    public IndexBuffer() {
        mNumIndices = 0;
    }

    public IndexBuffer(boolean useVBO) {
        mNumIndices = 0;
        mUseVbo = useVBO;
    }

    public IndexBuffer(int numVertices, boolean useVbo) {
        mUseVbo = useVbo;
        reset(numVertices);
    }

    public int size() {
        return mNumIndices;
    }

    public void reset(int numVertices) {
        mNumIndices = numVertices;
        regenerateBuffer();
    }

    // Call this when we have to re-create the surface and reloading all OpenGL resources.
    public void reload() {
        mGLBuffer.reload();
    }

    private void regenerateBuffer() {
        if (mNumIndices == 0) {
            return;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(2 * mNumIndices);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer ib = bb.asShortBuffer();
        ib.position(0);
        mIndexBuffer = ib;
    }

    public void addIndex(short index) {
        mIndexBuffer.put(index);
    }

    public void draw(GL10 gl, int primitiveType) {
        if (mNumIndices == 0) {
            return;
        }
        mIndexBuffer.position(0);
        if (mUseVbo && GLBuffer.canUseVBO()) {
            GL11 gl11 = (GL11) gl;
            mGLBuffer.bind(gl11, mIndexBuffer, 2 * mIndexBuffer.capacity());
            gl11.glDrawElements(primitiveType, size(), GL10.GL_UNSIGNED_SHORT, 0);
            GLBuffer.unbind(gl11);
        } else {
            gl.glDrawElements(primitiveType, size(), GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
        }
    }

    private ShortBuffer mIndexBuffer = null;
    private int mNumIndices = 0;
    private GLBuffer mGLBuffer = new GLBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER);
    private boolean mUseVbo = false;
}
