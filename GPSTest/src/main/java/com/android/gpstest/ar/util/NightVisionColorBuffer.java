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

/// Encapsulates a color vertex buffer where night vision can be enabled or diabled by a function call.
public class NightVisionColorBuffer {
    public NightVisionColorBuffer(int numVertices) {
        reset(numVertices);
    }

    public NightVisionColorBuffer() {
        mNormalBuffer = new ColorBuffer(false);
        mRedBuffer = new ColorBuffer(false);
    }

    public NightVisionColorBuffer(boolean useVBO) {
        mNormalBuffer = new ColorBuffer(useVBO);
        mRedBuffer = new ColorBuffer(useVBO);
    }

    public int size() {
        return mNormalBuffer.size();
    }

    public void reset(int numVertices) {
        mNormalBuffer.reset(numVertices);
        mRedBuffer.reset(numVertices);
    }

    // Call this when we have to re-create the surface and reloading all OpenGL resources.
    public void reload() {
        mNormalBuffer.reload();
        mRedBuffer.reload();
    }

    public void addColor(int a, int r, int g, int b) {
        mNormalBuffer.addColor(a, r, g, b);
        // I tried luminance here first, but many objects we care a lot about weren't very noticable because they were
        // bluish.  An average gets a better result.
        int avg = (r + g + b) / 3;
        mRedBuffer.addColor(a, avg, 0, 0);
    }

    public void addColor(int abgr) {
        int a = (abgr >> 24) & 0xff;
        int b = (abgr >> 16) & 0xff;
        int g = (abgr >> 8) & 0xff;
        int r = abgr & 0xff;
        addColor(a, r, g, b);
    }

    public void set(GL10 gl, boolean nightVisionMode) {
        if (nightVisionMode) {
            mRedBuffer.set(gl);
        } else {
            mNormalBuffer.set(gl);
        }
    }

    private ColorBuffer mNormalBuffer;
    private ColorBuffer mRedBuffer;
}
