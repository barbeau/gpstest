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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.Buffer;

import javax.microedition.khronos.opengles.GL11;

/**
 * This is a utility class which encapsulates and OpenGL buffer object.  Several other classes
 * need to be able to lazily create OpenGL buffers, so this class takes care of the work of lazily
 * creating and updating them.
 *
 * @author jpowell
 */
public class GLBuffer {
    // TODO(jpowell): This is ugly, we should have a buffer factory which knows
    // this rather than a static constant.  I should refactor this accordingly
    // when I get a chance.
    private static boolean sCanUseVBO = false;

    private Buffer mBuffer = null;
    private int mBufferSize = 0;
    private int mGLBufferID = -1;
    private int mBufferType;
    private boolean mHasLoggedStackTraceOnError = false;

    GLBuffer(int bufferType) {
        mBufferType = bufferType;
    }

    public static void setCanUseVBO(boolean canUseVBO) {
        sCanUseVBO = canUseVBO;
    }

    // A caller should verify that this returns true before using a GLBuffer.
    // If this returns false, any operation using the VBO will be a no-op.
    public static boolean canUseVBO() {
        return sCanUseVBO;
    }

    // Unset any GL buffer which is set on the device.  You need to call this if you want to render
    // without VBOs.  Otherwise it will try to use whatever buffer is currently set.
    public static void unbind(GL11 gl) {
        if (canUseVBO()) {
            gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    public void bind(GL11 gl, Buffer buffer, int bufferSize) {
        if (canUseVBO()) {
            maybeRegenerateBuffer(gl, buffer, bufferSize);
            gl.glBindBuffer(mBufferType, mGLBufferID);
        } else {
            Log.e("GLBuffer", "Trying to use a VBO, but they are unsupported");
            // Log a stack trace the first time we see this for any given buffer.
            if (!mHasLoggedStackTraceOnError) {
                StringWriter writer = new StringWriter();
                new Throwable().printStackTrace(new PrintWriter(writer));
                Log.e("SkyRenderer", writer.toString());
                mHasLoggedStackTraceOnError = true;
            }
        }
    }

    public void reload() {
        // Just reset all of the values so we'll reload on the next call
        // to maybeRegenerateBuffer.
        mBuffer = null;
        mBufferSize = 0;
        mGLBufferID = -1;
    }

    private void maybeRegenerateBuffer(GL11 gl, Buffer buffer, int bufferSize) {
        if (buffer != mBuffer || bufferSize != mBufferSize) {
            mBuffer = buffer;
            mBufferSize = bufferSize;

            // Allocate the buffer ID if we don't already have one.
            if (mGLBufferID == -1) {
                int[] buffers = new int[1];
                gl.glGenBuffers(1, buffers, 0);
                mGLBufferID = buffers[0];
            }

            gl.glBindBuffer(mBufferType, mGLBufferID);
            gl.glBufferData(mBufferType, bufferSize, buffer, GL11.GL_STATIC_DRAW);
        }
    }
}
