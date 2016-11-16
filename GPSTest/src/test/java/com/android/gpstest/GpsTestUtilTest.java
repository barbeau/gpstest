/*
 * Copyright (C) 2016 Sean J. Barbeau (sjbarbeau@gmail.com)
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
package com.android.gpstest;

import com.android.gpstest.util.GpsTestUtil;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class GpsTestUtilTest {

    /**
     * Test getting altitude above mean sea level (geoid) from NMEA sentences
     */
    @Test
    public void testGetAltitudeFromNmea() {
        double altitude;
        final String gpsSentence
                = "$GPGGA,032739.0,2804.732835,N,08224.639709,W,1,08,0.8,19.2,M,-24.0,M,,*5B";
        final String gnssSentence
                = "$GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,78.9,-24.0,,*23";

        altitude = GpsTestUtil.getAltitudeMeanSeaLevel(gpsSentence);
        assertEquals(19.2d, altitude);

        altitude = GpsTestUtil.getAltitudeMeanSeaLevel(gnssSentence);
        assertEquals(78.9d, altitude);
    }
}
