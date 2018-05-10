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

import android.location.GnssStatus;

import com.android.gpstest.util.GpsTestUtil;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class GpsTestUtilTest {

    /**
     * Test getting altitude above mean sea level (geoid) from NMEA sentences
     */
    @Test
    public void testGetAltitudeFromNmea() {
        Double altitude;
        final String gpsSentence
                = "$GPGGA,032739.0,2804.732835,N,08224.639709,W,1,08,0.8,19.2,M,-24.0,M,,*5B";
        final String gnssSentence
                = "$GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,78.9,-24.0,,*23";

        altitude = GpsTestUtil.getAltitudeMeanSeaLevel(gpsSentence);
        assertEquals(19.2d, altitude);

        altitude = GpsTestUtil.getAltitudeMeanSeaLevel(gnssSentence);
        assertEquals(78.9d, altitude);

        final String badGnssSentence
                = "$GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,BAD,-24.0,,*23";
        altitude = GpsTestUtil.getAltitudeMeanSeaLevel(badGnssSentence);
        assertNull(altitude);
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

        dop = GpsTestUtil.getDop(s1);

        assertEquals(1.3d, dop.getPositionDop());
        assertEquals(1.0d, dop.getHorizontalDop());
        assertEquals(0.8d, dop.getVerticalDop());

        dop = GpsTestUtil.getDop(s2);

        assertEquals(3.6d, dop.getPositionDop());
        assertEquals(1.8d, dop.getHorizontalDop());
        assertEquals(3.1d, dop.getVerticalDop());

        dop = GpsTestUtil.getDop(s3);

        assertEquals(3.6d, dop.getPositionDop());
        assertEquals(1.8d, dop.getHorizontalDop());
        assertEquals(3.1d, dop.getVerticalDop());

        dop = GpsTestUtil.getDop(s4);

        assertEquals(3.6d, dop.getPositionDop());
        assertEquals(2.1d, dop.getHorizontalDop());
        assertEquals(2.2d, dop.getVerticalDop());

        dop = GpsTestUtil.getDop(s5);

        assertEquals(1.7d, dop.getPositionDop());
        assertEquals(1.0d, dop.getHorizontalDop());
        assertEquals(1.3d, dop.getVerticalDop());

        dop = GpsTestUtil.getDop(s6);

        assertEquals(2.5d, dop.getPositionDop());
        assertEquals(1.3d, dop.getHorizontalDop());
        assertEquals(2.1d, dop.getVerticalDop());
    }

    /**
     * Test converting GNSS signal carrier frequencies to labels like "L1"
     */
    @Test
    public void testGetCarrierFrequencyLabel() {
        String label;

        // NAVSTAR (GPS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GPS, 1, 1575.42f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GPS, 1, 1227.6f);
        assertEquals("L2",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GPS, 1, 1381.05f);
        assertEquals("L3",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GPS, 1, 1379.913f);
        assertEquals("L4",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GPS, 1, 1176.45f);
        assertEquals("L5",label);

        // GLONASS
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 	1598.0625f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 1609.3125f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 1242.9375f);
        assertEquals("L2",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 1251.6875f);
        assertEquals("L2",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 1202.025f);
        assertEquals("L3",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 1207.14f);
        assertEquals("L3",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GLONASS, 1, 	1176.45f);
        assertEquals("L5",label);

        // QZSS
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_QZSS, 1, 	1575.42f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_QZSS, 1, 	1227.6f);
        assertEquals("L2",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_QZSS, 1, 	1176.45f);
        assertEquals("L5",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_QZSS, 1, 	1278.75f);
        assertEquals("LEX",label);

        // Galileo
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GALILEO, 1, 	1575.42f);
        assertEquals("E1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GALILEO, 1, 	1191.795f);
        assertEquals("E5",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GALILEO, 1, 	1176.45f);
        assertEquals("E5a",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GALILEO, 1, 	1207.14f);
        assertEquals("E5b",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GALILEO, 1, 	1278.75f);
        assertEquals("E6",label);

        // Beidou
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_BEIDOU, 1, 	1561.098f);
        assertEquals("B1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_BEIDOU, 1, 	1589.742f);
        assertEquals("B1-2",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_BEIDOU, 1, 	1207.14f);
        assertEquals("B2",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_BEIDOU, 1, 	1176.45f);
        assertEquals("B2a",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_BEIDOU, 1, 	1268.52f);
        assertEquals("B3",label);

        // GAGAN (SBAS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 127, 	1575.42f);
        assertEquals("L1",label);

        // INMARSAT_3F2 (SBAS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 120, 	1575.42f);
        assertEquals("L1",label);

        // INMARSAT_4F3 (SBAS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 133, 	1575.42f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 133, 	1176.45f);
        assertEquals("L5",label);

        // Galaxy 15 (SBAS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 135, 	1575.42f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 135, 	1176.45f);
        assertEquals("L5",label);

        // ANIK F1R (SBAS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 138, 	1575.42f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 138, 	1176.45f);
        assertEquals("L5",label);

        // SES_5 (SBAS)
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 136, 	1575.42f);
        assertEquals("L1",label);

        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_SBAS, 136, 	1176.45f);
        assertEquals("L5",label);

        // Test variations on the "same" numbers to make sure floating point equality works
        label = GpsTestUtil.getCarrierFrequencyLabel(GnssStatus.CONSTELLATION_GPS, 1, 1575.420000f);
        assertEquals("L1",label);
    }
}
