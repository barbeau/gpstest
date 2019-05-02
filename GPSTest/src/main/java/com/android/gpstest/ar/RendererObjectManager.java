// Copyright 2009 Google Inc. From StarDroid.
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

import com.android.gpstest.ar.util.TextureManager;

import java.util.EnumSet;

import javax.microedition.khronos.opengles.GL10;


public abstract class RendererObjectManager implements Comparable<RendererObjectManager> {
    // Specifies options for updating a specific RendererObjectManager.
    public enum UpdateType {
        Reset,            // Throw away any previous data and set entirely new data.
        UpdatePositions,  // Only update positions of existing objects.
        UpdateImages      // Only update images of existing objects.
    }

    public RendererObjectManager(int layer, TextureManager textureManager) {
        mLayer = layer;
        mTextureManager = textureManager;
        synchronized (RendererObjectManager.class) {
            mIndex = sIndex++;
        }
    }

    public void enable(boolean enable) {
        mEnabled = enable;
    }

    public void setMaxRadiusOfView(float radiusOfView) {
        mMaxRadiusOfView = radiusOfView;
    }

    public int compareTo(RendererObjectManager rom) {
        if (getClass() != rom.getClass()) {
            return getClass().getName().compareTo(rom.getClass().getName());
        }
        if (mIndex < rom.mIndex) {
            return -1;
        } else if (mIndex == rom.mIndex) {
            return 0;
        } else {
            return 1;
        }
    }

    final int getLayer() {
        return mLayer;
    }

    final void draw(GL10 gl) {
        if (mEnabled && mRenderState.getRadiusOfView() <= mMaxRadiusOfView) {
            drawInternal(gl);
        }
    }

    final void setRenderState(RenderStateInterface state) {
        mRenderState = state;
    }

    final RenderStateInterface getRenderState() {
        return mRenderState;
    }

    interface UpdateListener {
        void queueForReload(RendererObjectManager rom, boolean fullReload);
    }

    final void setUpdateListener(UpdateListener listener) {
        mListener = listener;
    }

    // Notifies the renderer that the manager must be reloaded before the next time it is drawn.
    final void queueForReload(boolean fullReload) {
        mListener.queueForReload(this, fullReload);
    }

    protected void logUpdateMismatch(String managerType, int expectedLength, int actualLength,
                                     EnumSet<UpdateType> type) {
        Log.e("ImageObjectManager",
                "Trying to update objects in " + managerType + ", but number of input sources was "
                        + "different from the number currently set on the manager (" + actualLength
                        + " vs " + expectedLength + "\n"
                        + "Update options were: " + type + "\n"
                        + "Ignoring update");
    }

    protected TextureManager textureManager() {
        return mTextureManager;
    }

    // Reload all OpenGL resources needed by the object (ie, textures, VBOs).  If fullReload is true,
    // this means that the object needs to reload everything (this is the case when the object
    // is loaded for the first time, or when the activity is being recreated, and all the previous
    // resources have been invalid.  Sometimes a manager may only need to be partially reloaded (for
    // example, if new objects are set, they might need to be reloaded, but the texture shared
    // between them all is the same so it does not need to be).  The renderer will only ever do a
    // full reload - fullReload will only be false if the manager queues itself for a partial reload.
    public abstract void reload(GL10 gl, boolean fullReload);

    protected abstract void drawInternal(GL10 gl);

    private boolean mEnabled = true;
    private RenderStateInterface mRenderState = null;
    private UpdateListener mListener = null;
    private float mMaxRadiusOfView = 360;  // in degrees
    private int mLayer;
    private int mIndex;
    private final TextureManager mTextureManager;
    // Used to distinguish between different renderers, so we can have sets of them.
    private static int sIndex = 0;
}
