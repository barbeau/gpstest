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

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.android.gpstest.ar.util.smoothers.PlainSmootherModelAdaptor;
import com.android.gpstest.util.MiscUtils;

/**
 * Sets the direction of view from the orientation sensors.
 *
 * @author John Taylor
 */
public class SensorOrientationController extends AbstractController
        implements SensorEventListener {
    // TODO(johntaylor): this class needs to be refactored to use the new
    // sensor API and to behave properly when sensors are not available.

    private static class SensorDampingSettings {
        public float damping;
        public int exponent;

        public SensorDampingSettings(float damping, int exponent) {
            this.damping = damping;
            this.exponent = exponent;
        }
    }

    private final static String TAG = MiscUtils.getTag(SensorOrientationController.class);
    /**
     * Parameters that control the smoothing of the accelerometer and
     * magnetic sensors.
     */
    private static final SensorDampingSettings[] ACC_DAMPING_SETTINGS = new SensorDampingSettings[]{
            new SensorDampingSettings(0.7f, 3),
            new SensorDampingSettings(0.7f, 3),
            new SensorDampingSettings(0.1f, 3),
            new SensorDampingSettings(0.1f, 3),
    };
    private static final SensorDampingSettings[] MAG_DAMPING_SETTINGS = new SensorDampingSettings[]{
            new SensorDampingSettings(0.05f, 3),  // Derived for the Nexus One
            new SensorDampingSettings(0.001f, 4),  // Derived for the unpatched MyTouch Slide
            new SensorDampingSettings(0.0001f, 5),  // Just guessed for Nexus 6
            new SensorDampingSettings(0.000001f, 5)  // Just guessed for Nexus 6
    };

    private SensorManager manager;
    private SensorListener accelerometerSmoother;
    private SensorListener compassSmoother;
    private PlainSmootherModelAdaptor modelAdaptorProvider;
    private Sensor rotationSensor;
    private SharedPreferences sharedPreferences;

    SensorOrientationController(PlainSmootherModelAdaptor modelAdaptorProvider,
                                SensorManager manager, SharedPreferences sharedPreferences) {
        this.manager = manager;
        this.modelAdaptorProvider = modelAdaptorProvider;
        this.rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        this.sharedPreferences = sharedPreferences;
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
        Log.d(
                TAG, "Unregistering sensor listeners: " + accelerometerSmoother + ", "
                        + compassSmoother + ", " + this);
        manager.unregisterListener(accelerometerSmoother);
        manager.unregisterListener(compassSmoother);
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
