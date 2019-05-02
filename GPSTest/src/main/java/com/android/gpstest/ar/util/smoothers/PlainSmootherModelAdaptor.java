// Copyright 2010 Google Inc.
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

package com.android.gpstest.ar.util.smoothers;

import android.content.SharedPreferences;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.android.gpstest.Application;
import com.android.gpstest.R;
import com.android.gpstest.ar.AstronomerModel;
import com.android.gpstest.ar.Vector3;
import com.android.gpstest.util.MiscUtils;

/**
 * Adapts sensor output for use with the astronomer model.
 *
 * @author John Taylor
 */
public class PlainSmootherModelAdaptor implements SensorListener {
    private static final String TAG = MiscUtils.getTag(PlainSmootherModelAdaptor.class);
    private Vector3 magneticValues = Application.INITIAL_SOUTH.copy();
    private Vector3 acceleration = Application.INITIAL_DOWN.copy();
    private AstronomerModel model;
    private boolean reverseMagneticZaxis;

    PlainSmootherModelAdaptor(AstronomerModel model, SharedPreferences sharedPreferences) {
        this.model = model;
        reverseMagneticZaxis = sharedPreferences.getBoolean(
                Application.get().getString(R.string.pref_key_reverse_magnetic_z), false);
    }

    @Override
    public void onSensorChanged(int sensor, float[] values) {
        if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
            acceleration.x = values[0];
            acceleration.y = values[1];
            acceleration.z = values[2];
        } else if (sensor == SensorManager.SENSOR_MAGNETIC_FIELD) {
            magneticValues.x = values[0];
            magneticValues.y = values[1];
            // The z direction for the mag magneticField sensor is in the opposite
            // direction to that for accelerometer, except on some phones that are doing it wrong.
            // Yes that's right, the right thing to do is to invert it.  So if we reverse that,
            // we don't invert it.  Got it?
            // TODO(johntaylor): this might not be the best place to do this.
            magneticValues.z = reverseMagneticZaxis ? values[2] : -values[2];
        } else {
            Log.e(TAG, "Pump is receiving values that aren't accel or magnetic");
        }
        model.setPhoneSensorValues(acceleration, magneticValues);
    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) {
        // Do nothing, at present.
    }
}
