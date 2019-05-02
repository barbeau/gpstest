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

import com.android.gpstest.ar.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class VertexBuffer {

    //Creates an empty buffer.  Must call reset() before adding vertices.
    public VertexBuffer() {
        mNumVertices = 0;
        mUseVBO = false;
    }

    public VertexBuffer(boolean useVBO) {
        mNumVertices = 0;
        mUseVBO = useVBO;
    }

    public VertexBuffer(int numVertices) {
        this(numVertices, false);
    }

    public VertexBuffer(int numVertices, boolean useVBO) {
        mUseVBO = useVBO;
        reset(numVertices);
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

    public void addPoint(Vector3 p) {
        addPoint(p.x, p.y, p.z);
    }

    public void addPoint(float x, float y, float z) {
        mPositionBuffer.put(FixedPoint.floatToFixedPoint(x));
        mPositionBuffer.put(FixedPoint.floatToFixedPoint(y));
        mPositionBuffer.put(FixedPoint.floatToFixedPoint(z));
    }

    public void set(GL10 gl) {
        if (mNumVertices == 0) {
            return;
        }

        mPositionBuffer.position(0);

        if (mUseVBO && GLBuffer.canUseVBO()) {
            GL11 gl11 = (GL11) gl;
            mGLBuffer.bind(gl11, mPositionBuffer, 4 * mPositionBuffer.capacity());
            gl11.glVertexPointer(3, GL10.GL_FIXED, 0, 0);
        } else {
            gl.glVertexPointer(3, GL10.GL_FIXED, 0, mPositionBuffer);
        }
    }

    private void regenerateBuffer() {
        if (mNumVertices == 0) {
            return;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 3 * mNumVertices);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer ib = bb.asIntBuffer();
        ib.position(0);
        mPositionBuffer = ib;
    }

    private IntBuffer mPositionBuffer = null;
    private int mNumVertices = 0;
    private GLBuffer mGLBuffer = new GLBuffer(GL11.GL_ARRAY_BUFFER);
    private boolean mUseVBO = false;
}
