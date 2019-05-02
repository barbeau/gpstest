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

import com.android.gpstest.ar.Matrix4x4;
import com.android.gpstest.ar.Vector3;

public class SearchHelper {
    public void resize(int width, int height) {
        mHalfScreenWidth = width * 0.5f;
        mHalfScreenHeight = height * 0.5f;
    }

    public void setTarget(Vector3 target, String targetName) {
        mTargetName = targetName;
        mTarget = target.copy();
        mTransformedPosition = null;
        mLastUpdateTime = System.currentTimeMillis();
        mTransitionFactor = targetInFocusRadiusImpl() ? 1 : 0;
    }

    public void setTransform(Matrix4x4 transformMatrix) {
        mTransformMatrix = transformMatrix;
        mTransformedPosition = null;
    }

    public Vector3 getTransformedPosition() {
        if (mTransformedPosition == null && mTransformMatrix != null) {
            // Transform the label position by our transform matrix
            mTransformedPosition = Matrix4x4.transformVector(mTransformMatrix, mTarget);
        }
        return mTransformedPosition;
    }

    public boolean targetInFocusRadius() {
        return mWasInFocusLastCheck;
    }

    public void setTargetFocusRadius(float radius) {
        mTargetFocusRadius = radius;
    }

    // Returns a number between 0 and 1, 0 meaning that we should draw the UI as if the target
    // is not in focus, 1 meaning it should be fully in focus, and between the two meaning
    // it just transitioned between the two, so we should be drawing the transition.
    public float getTransitionFactor() {
        return mTransitionFactor;
    }

    // Checks whether the search target is in the focus or not, and updates the seconds in the state
    // accordingly.
    public void checkState() {
        boolean inFocus = targetInFocusRadiusImpl();
        mWasInFocusLastCheck = inFocus;
        long time = System.currentTimeMillis();
        float delta = 0.001f * (time - mLastUpdateTime);
        mTransitionFactor += delta * (inFocus ? 1 : -1);
        mTransitionFactor = Math.min(1, Math.max(0, mTransitionFactor));
        mLastUpdateTime = time;
    }

    public String getTargetName() {
        return mTargetName;
    }

    // Returns the distance from the center of the screen, in pixels, if the target is in front of
    // the viewer.  Returns infinity if the point is behind the viewer.
    private float getDistanceFromCenterOfScreen() {
        Vector3 position = getTransformedPosition();
        if (position.z > 0) {
            float dx = position.x * mHalfScreenWidth;
            float dy = position.y * mHalfScreenHeight;
            return (float) Math.sqrt(dx * dx + dy * dy);
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    private boolean targetInFocusRadiusImpl() {
        float distFromCenter = getDistanceFromCenterOfScreen();
        return 0.5f * mTargetFocusRadius > distFromCenter;
    }

    private Vector3 mTarget = new Vector3(0, 0, 0);
    private Vector3 mTransformedPosition = new Vector3(0, 0, 0);
    private float mHalfScreenWidth = 1;
    private float mHalfScreenHeight = 1;
    private Matrix4x4 mTransformMatrix = null;
    private float mTargetFocusRadius = 0;
    private float mTransitionFactor = 0;
    private long mLastUpdateTime = 0;
    private boolean mWasInFocusLastCheck = false;
    private String mTargetName = "Default target name";
}
