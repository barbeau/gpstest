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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.android.gpstest.util.MiscUtils;

/**
 * Sets the direction of view from the orientation sensors.
 *
 * @author John Taylor
 */
public class SensorOrientationController extends AbstractController
        implements SensorEventListener {

    private final static String TAG = MiscUtils.getTag(SensorOrientationController.class);

    private SensorManager manager;
    private Sensor rotationSensor;

    public SensorOrientationController(SensorManager manager) {
        this.manager = manager;
        this.rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    public void start() {
        if (manager != null) {
            manager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        Log.d(TAG, "Registered sensor listener");
    }

    @Override
    public void stop() {
        Log.d(TAG, "Unregistering sensor listeners: " + this);
        manager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor != rotationSensor) {
            return;
        }
        model.setPhoneSensorValues(event.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }
}
