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

import android.hardware.GeomagneticField;

/**
 * Encapsulates the calculation of magnetic declination for the user's location
 * and position.
 *
 * @author John Taylor
 */
public class RealMagneticDeclinationCalculator implements MagneticDeclinationCalculator {
    private GeomagneticField geomagneticField;

    /**
     * {@inheritDoc}
     * Silently returns zero if the time and location have not been set.
     */
    @Override
    public float getDeclination() {
        if (geomagneticField == null) {
            return 0;
        }
        return geomagneticField.getDeclination();
    }

    /**
     * Sets the user's current location and time.
     */
    @Override
    public void setLocationAndTime(LatLong location, long timeInMillis) {
        geomagneticField = new GeomagneticField(location.getLatitude(),
                location.getLongitude(),
                0,
                timeInMillis);
    }

    @Override
    public String toString() {
        return "Real Magnetic Correction";
    }
}
