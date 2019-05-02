// Copyright 2010 Google Inc. From StarDroid.
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

package com.android.gpstest.ar.touch;

import android.util.Log;
import android.view.MotionEvent;

import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.MiscUtils;

/**
 * Detects map drags, rotations and pinch zooms.
 *
 * @author John Taylor
 */
public class DragRotateZoomGestureDetector {
    /**
     * Listens for the gestures detected by the {@link DragRotateZoomGestureDetector}.
     *
     * @author John Taylor
     */
    public interface DragRotateZoomGestureDetectorListener {
        boolean onDrag(float xPixels, float yPixels);

        boolean onStretch(float ratio);

        boolean onRotate(float radians);
    }

    private static final String TAG = MiscUtils.getTag(DragRotateZoomGestureDetector.class);

    private DragRotateZoomGestureDetectorListener listener;
    ;

    public DragRotateZoomGestureDetector(DragRotateZoomGestureDetectorListener listener) {
        this.listener = listener;
    }

    private enum State {READY, DRAGGING, DRAGGING2}

    private State currentState = State.READY;
    private float last1X;
    private float last1Y;

    private float last2X;
    private float last2Y;


    public boolean onTouchEvent(MotionEvent ev) {
        // The state changes are as follows.
        // READY -> DRAGGING -> DRAGGING2 -> READY
        //
        // ACTION_DOWN: READY->DRAGGING
        //   last position = current position
        //
        // ACTION_MOVE: no state change
        //   calculate move = current position - last position
        //   last position = current position
        //
        // ACTION_UP: DRAGGING->READY
        //   last position = null
        // ...or...from DRAGGING
        //
        // ACTION_POINTER_DOWN: DRAGGING->DRAGGING2
        //   we're in multitouch mode
        //   last position1 = current position1
        //   last poisiton2 = current position2
        //
        // ACTION_MOVE:
        //   calculate move
        //   last position1 = current position1
        //   last position2 = current position2
        int actionCode = ev.getAction() & MotionEvent.ACTION_MASK;
        // Log.d(TAG, "Action: " + actionCode + ", current state " + currentState);
        if (actionCode == MotionEvent.ACTION_DOWN || currentState == State.READY) {
            currentState = State.DRAGGING;
            last1X = ev.getX();
            last1Y = ev.getY();
            // Log.d(TAG, "Down.  Store last position " + last1X + ", " + last1Y);
            return true;
        }

        if (actionCode == MotionEvent.ACTION_MOVE && currentState == State.DRAGGING) {
            // Log.d(TAG, "Move");
            float current1X = ev.getX();
            float current1Y = ev.getY();
            // Log.d(TAG, "Move.  Last position " + last1X + ", " + last1Y +
            //    "Current position " + current1X + ", " + current1Y);
            listener.onDrag(current1X - last1X, current1Y - last1Y);
            last1X = current1X;
            last1Y = current1Y;
            return true;
        }

        if (actionCode == MotionEvent.ACTION_MOVE && currentState == State.DRAGGING2) {
            // Log.d(TAG, "Move with two fingers");
            int pointerCount = ev.getPointerCount();
            if (pointerCount != 2) {
                Log.w(TAG, "Expected exactly two pointers but got " + pointerCount);
                return false;
            }
            float current1X = ev.getX(0);
            float current1Y = ev.getY(0);
            float current2X = ev.getX(1);
            float current2Y = ev.getY(1);
            // Log.d(TAG, "Old Point 1: " + lastPointer1X + ", " + lastPointer1Y);
            // Log.d(TAG, "Old Point 2: " + lastPointer2X + ", " + lastPointer2Y);
            // Log.d(TAG, "New Point 1: " + current1X + ", " + current1Y);
            // Log.d(TAG, "New Point 2: " + current2X + ", " + current2Y);

            float distanceMovedX1 = current1X - last1X;
            float distanceMovedY1 = current1Y - last1Y;
            float distanceMovedX2 = current2X - last2X;
            float distanceMovedY2 = current2Y - last2Y;

            // Log.d(TAG, "Point 1 moved by: " + distanceMovedX1 + ", " + distanceMovedY1);
            // Log.d(TAG, "Point 2 moved by: " + distanceMovedX2 + ", " + distanceMovedY2);

            // Dragging map by the mean of the points
            listener.onDrag((distanceMovedX1 + distanceMovedX2) / 2,
                    (distanceMovedY1 + distanceMovedY2) / 2);

            // These are the vectors between the two points.
            float vectorLastX = last1X - last2X;
            float vectorLastY = last1Y - last2Y;
            float vectorCurrentX = current1X - current2X;
            float vectorCurrentY = current1Y - current2Y;

            // Log.d(TAG, "Previous vector: " + vectorBeforeX + ", " + vectorBeforeY);
            // Log.d(TAG, "Current vector: " + vectorCurrentX + ", " + vectorCurrentY);

            float lengthRatio = (float) Math.sqrt(normSquared(vectorCurrentX, vectorCurrentY)
                    / normSquared(vectorLastX, vectorLastY));
            // Log.d(TAG, "Stretching map by ratio " + ratio);
            listener.onStretch(lengthRatio);
            float angleLast = (float) Math.atan2(vectorLastX, vectorLastY);
            float angleCurrent = (float) Math.atan2(vectorCurrentX, vectorCurrentY);
            // Log.d(TAG, "Angle before " + angleBefore);
            // Log.d(TAG, "Angle after " + angleAfter);
            float angleDelta = angleCurrent - angleLast;
            // Log.d(TAG, "Rotating map by angle delta " + angleDelta);
            listener.onRotate(angleDelta * MathUtils.RADIANS_TO_DEGREES);

            last1X = current1X;
            last1Y = current1Y;
            last2X = current2X;
            last2Y = current2Y;
            return true;
        }

        if (actionCode == MotionEvent.ACTION_UP && currentState != State.READY) {
            // Log.d(TAG, "Up");
            currentState = State.READY;
            return true;
        }

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN && currentState == State.DRAGGING) {
            //Log.d(TAG, "Non primary pointer down " + pointer);
            int pointerCount = ev.getPointerCount();
            if (pointerCount != 2) {
                Log.w(TAG, "Expected exactly two pointers but got " + pointerCount);
                return false;
            }
            currentState = State.DRAGGING2;
            last1X = ev.getX(0);
            last1Y = ev.getY(0);
            last2X = ev.getX(1);
            last2Y = ev.getY(1);
            return true;
        }

        if (actionCode == MotionEvent.ACTION_POINTER_UP && currentState == State.DRAGGING2) {
            // Log.d(TAG, "Non primary pointer up " + pointer);
            // Let's just drop dragging for now - can worry about continuity with one finger
            // drag later.
            currentState = State.READY;
            return true;
        }
        // Log.d(TAG, "End state " + currentState);
        return false;
    }

    private static float normSquared(float x, float y) {
        return (x * x + y * y);
    }
}