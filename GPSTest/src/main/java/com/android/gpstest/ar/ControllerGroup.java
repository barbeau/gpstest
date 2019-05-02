// Copyright 2008 Google Inc. From StarDroid.
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

import android.content.Context;
import android.util.Log;

import com.android.gpstest.util.MiscUtils;

import java.util.ArrayList;

/**
 * Manages all the different controllers that affect the model of the observer.
 * Is both a factory and acts as a facade to the underlying controllers.
 *
 * @author John Taylor
 */
public class ControllerGroup implements Controller {
    private final static String TAG = MiscUtils.getTag(ControllerGroup.class);
    private final ArrayList<Controller> controllers = new ArrayList<Controller>();
    private ZoomController zoomController;
    private ManualOrientationController manualDirectionController;
    private SensorOrientationController sensorOrientationController;
    private TeleportingController teleportingController;
    private boolean usingAutoMode = true;
    private AstronomerModel model;

    // TODO(jontayler): inject everything else.
    ControllerGroup(Context context, SensorOrientationController sensorOrientationController,
                    LocationController locationController) {
        addController(locationController);
        this.sensorOrientationController = sensorOrientationController;
        addController(sensorOrientationController);
        manualDirectionController = new ManualOrientationController();
        addController(manualDirectionController);
        zoomController = new ZoomController();
        addController(zoomController);
        teleportingController = new TeleportingController();
        addController(teleportingController);
        setAutoMode(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        Log.i(TAG, "Enabling all controllers");
        for (Controller controller : controllers) {
            controller.setEnabled(enabled);
        }
    }

    @Override
    public void setModel(AstronomerModel model) {
        Log.i(TAG, "Setting model");
        for (Controller controller : controllers) {
            controller.setModel(model);
        }
        this.model = model;
        model.setAutoUpdatePointing(usingAutoMode);
        model.setClock();
    }

    /**
     * Are we in auto mode (aka sensor mode) or manual?
     */
    public boolean isAutoMode() {
        return usingAutoMode;
    }

    /**
     * Sets auto mode (true) or manual mode (false).
     */
    public void setAutoMode(boolean enabled) {
        manualDirectionController.setEnabled(!enabled);
        sensorOrientationController.setEnabled(enabled);
        if (model != null) {
            model.setAutoUpdatePointing(enabled);
        }
        usingAutoMode = enabled;
    }

    @Override
    public void start() {
        Log.i(TAG, "Starting controllers");
        for (Controller controller : controllers) {
            controller.start();
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping controllers");
        for (Controller controller : controllers) {
            controller.stop();
        }
    }

    /**
     * Moves the pointing right and left.
     *
     * @param radians the angular change in the pointing in radians (only
     *                accurate in the limit as radians tends to 0.)
     */
    public void changeRightLeft(float radians) {
        manualDirectionController.changeRightLeft(radians);
    }

    /**
     * Moves the pointing up and down.
     *
     * @param radians the angular change in the pointing in radians (only
     *                accurate in the limit as radians tends to 0.)
     */
    public void changeUpDown(float radians) {
        manualDirectionController.changeUpDown(radians);
    }

    /**
     * Rotates the view about the current center point.
     */
    public void rotate(float degrees) {
        manualDirectionController.rotate(degrees);
    }

    /**
     * Sends the astronomer's pointing to the new target.
     *
     * @param target the destination
     */
    public void teleport(GeocentricCoordinates target) {
        teleportingController.teleport(target);
    }

    /**
     * Adds a new controller to this
     */
    public void addController(Controller controller) {
        controllers.add(controller);
    }

    public void zoomBy(float ratio) {
        zoomController.zoomBy(ratio);
    }
}
