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

import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.android.gpstest.ar.AstronomerModel;
import com.android.gpstest.ar.ControllerGroup;
import com.android.gpstest.ar.Geometry;
import com.android.gpstest.util.MiscUtils;

/**
 * Applies drags, zooms and rotations to the model.
 * Listens for events from the DragRotateZoomGestureDetector.
 *
 * @author John Taylor
 */
public class MapMover implements
        DragRotateZoomGestureDetector.DragRotateZoomGestureDetectorListener {

    private static final String TAG = MiscUtils.getTag(MapMover.class);
    private AstronomerModel model;
    private ControllerGroup controllerGroup;
    private float sizeTimesRadiansToDegrees;

    public MapMover(AstronomerModel model, ControllerGroup controllerGroup, Context context) {
        this.model = model;
        this.controllerGroup = controllerGroup;
        Display display = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenLongSize = display.getHeight();
        Log.i(TAG, "Screen height is " + screenLongSize + " pixels.");
        sizeTimesRadiansToDegrees = screenLongSize * Geometry.RADIANS_TO_DEGREES;
    }

    @Override
    public boolean onDrag(float xPixels, float yPixels) {
        // Log.d(TAG, "Dragging by " + xPixels + ", " + yPixels);
        final float pixelsToRadians = model.getFieldOfView() / sizeTimesRadiansToDegrees;
        controllerGroup.changeUpDown(-yPixels * pixelsToRadians);
        controllerGroup.changeRightLeft(-xPixels * pixelsToRadians);
        return true;
    }

    @Override
    public boolean onRotate(float degrees) {
        controllerGroup.rotate(-degrees);
        return true;
    }

    @Override
    public boolean onStretch(float ratio) {
        controllerGroup.zoomBy(1.0f / ratio);
        return true;
    }
}