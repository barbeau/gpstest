/*
 * Copyright (C) 2013-2019 Sean J. Barbeau
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import com.android.gpstest.model.DilutionOfPrecision;
import com.android.gpstest.model.GeoidAltitude;

import org.junit.Test;

public class NmeaUtilsTest {
    /**
     * Test getting altitude above mean sea level (geoid) from NMEA sentences
     */
    @Test
    public void testGetAltitudeFromNmea() {
        GeoidAltitude geoidAltitude;
        final String gpsSentence
                = "$GPGGA,032739.0,2804.732835,N,08224.639709,W,1,08,0.8,19.2,M,-24.0,M,,*5B";
        final String gnssSentence
                = "$GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,78.9,-24.0,,*23";
        final String gnGga1 = "$GNGGA,114926.00,3206.341435,N,11850.092448,E,1,11,0.9,19.9,M,2.2,M,,*7E";
        final String gnGga2 = "$GNGGA,172814.00,2803.208136,N,08225.981423,W,1,08,1.1,-19.7,M,-24.8,M,,*5F";
        final String gngns2 = "$GNGNS,165422,2804.28021,N,8225.598206,W,AAANNN,20,0.6,18.1,-24,,,V*24,,,,,,,";

        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(gpsSentence);
        assert geoidAltitude != null;
        assertEquals(19.2d, geoidAltitude.getAltitudeMsl());
        assertEquals(-24.0d, geoidAltitude.getHeightOfGeoid());

        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(gnssSentence);
        assert geoidAltitude != null;
        assertEquals(78.9d, geoidAltitude.getAltitudeMsl());
        assertEquals(-24.0d, geoidAltitude.getHeightOfGeoid());

        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(gnGga1);
        assert geoidAltitude != null;
        assertEquals(19.9d, geoidAltitude.getAltitudeMsl());
        assertEquals(2.2d, geoidAltitude.getHeightOfGeoid());

        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(gnGga2);
        assert geoidAltitude != null;
        assertEquals(-19.7d, geoidAltitude.getAltitudeMsl());
        assertEquals(-24.8d, geoidAltitude.getHeightOfGeoid());

        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(gngns2);
        assert geoidAltitude != null;
        assertEquals(18.1d, geoidAltitude.getAltitudeMsl());
        assertEquals(-24.0d, geoidAltitude.getHeightOfGeoid());

        final String badGnssSentence1
                = "$GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,BAD,-24.0,,*23";
        final String badGnssSentence2
                = "$GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,-24.0,BAD,,*23";
        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(badGnssSentence1);
        assertNull(geoidAltitude);
        geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(badGnssSentence2);
        assertNull(geoidAltitude);
    }

    /**
     * Test getting DOP from NMEA sentences
     */
    @Test
    public void testGetDopFromNmea() {
        DilutionOfPrecision dop;

        // LG G5 w/ Android 6.0.1
        final String s1 = "$GNGSA,A,2,67,68,69,79,84,,,,,,,,1.3,1.0,0.8,2*3A";

        // LG G5 w/ Android 7.0
        final String s2 = "$GPGSA,A,3,03,14,16,22,23,26,,,,,,,3.6,1.8,3.1*38";
        final String s3 = "$GNGSA,A,3,03,14,16,22,23,26,,,,,,,3.6,1.8,3.1,1*3B";

        // From http://aprs.gids.nl/nmea/#gsa
        final String s4 = "$GPGSA,A,3,,,,,,16,18,,22,24,,,3.6,2.1,2.2*3C";
        final String s5 = "$GPGSA,A,3,19,28,14,18,27,22,31,39,,,,,1.7,1.0,1.3*35";

        // From http://www.gpsinformation.org/dale/nmea.htm#GSA
        final String s6 = "$GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39";

        dop = NmeaUtils.getDop(s1);

        assertEquals(1.3d, dop.getPositionDop());
        assertEquals(1.0d, dop.getHorizontalDop());
        assertEquals(0.8d, dop.getVerticalDop());

        dop = NmeaUtils.getDop(s2);

        assertEquals(3.6d, dop.getPositionDop());
        assertEquals(1.8d, dop.getHorizontalDop());
        assertEquals(3.1d, dop.getVerticalDop());

        dop = NmeaUtils.getDop(s3);

        assertEquals(3.6d, dop.getPositionDop());
        assertEquals(1.8d, dop.getHorizontalDop());
        assertEquals(3.1d, dop.getVerticalDop());

        dop = NmeaUtils.getDop(s4);

        assertEquals(3.6d, dop.getPositionDop());
        assertEquals(2.1d, dop.getHorizontalDop());
        assertEquals(2.2d, dop.getVerticalDop());

        dop = NmeaUtils.getDop(s5);

        assertEquals(1.7d, dop.getPositionDop());
        assertEquals(1.0d, dop.getHorizontalDop());
        assertEquals(1.3d, dop.getVerticalDop());

        dop = NmeaUtils.getDop(s6);

        assertEquals(2.5d, dop.getPositionDop());
        assertEquals(1.3d, dop.getHorizontalDop());
        assertEquals(2.1d, dop.getVerticalDop());
    }
}
