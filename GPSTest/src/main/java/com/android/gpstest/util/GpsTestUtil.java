/*
 * Copyright (C) 2013 Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gpstest.util;

public class GpsTestUtil {

    /**
     * Returns the Global Navigation Satellite System (GNSS) for a satellite given the PRN
     * @param prn PRN value provided by the GpsSatellite.getPrn() method
     * @return GnssType for the given PRN
     */
    public static GnssType getGnssType(int prn) {
        // See Issue #26 for details
        if (prn >= 65 && prn <= 88) {
            return GnssType.GLONASS;
        } else {
            // Assume US NAVSTAR for now, since we don't have any other info on sat-to-PRN mappings
            return GnssType.NAVSTAR;
        }
    }
}