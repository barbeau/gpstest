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

package com.android.gpstest.ar.util.smoothers;

import android.hardware.SensorListener;


public abstract class SensorSmoother implements SensorListener {

    protected SensorListener listener;

    public SensorSmoother(SensorListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) {
        // Do Nothing
    }

    public abstract void onSensorChanged(int sensor, float[] values);
}
