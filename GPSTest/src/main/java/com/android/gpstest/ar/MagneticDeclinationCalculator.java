// Copyright 2009 Google Inc.
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

/**
 * Passed to the {@link AstronomerModel} to calculate the local magnetic
 * declination.
 *
 * @author John Taylor
 */
public interface MagneticDeclinationCalculator {

    /**
     * Returns the magnetic declination in degrees, that is, the rotation between
     * magnetic North and true North.
     */
    float getDeclination();

    /**
     * Sets the user's location and time.
     */
    void setLocationAndTime(LatLong location, long timeInMillis);
}
