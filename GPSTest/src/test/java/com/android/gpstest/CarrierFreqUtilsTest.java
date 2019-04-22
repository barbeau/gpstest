/*
 * Copyright (C) 2016-2019 Sean J. Barbeau (sjbarbeau@gmail.com)
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

import com.android.gpstest.model.GnssType;
import com.android.gpstest.util.CarrierFreqUtils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class CarrierFreqUtilsTest {

    /**
     * Test converting GNSS signal carrier frequencies to labels like "L1"
     */
    @Test
    public void testGetCarrierFrequencyLabel() {
        String label;

        // NAVSTAR (GPS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1227.6f);
        assertEquals("L2",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1381.05f);
        assertEquals("L3",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1379.913f);
        assertEquals("L4",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1176.45f);
        assertEquals("L5",label);

        // GLONASS
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 	1598.0625f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 1609.3125f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 1242.9375f);
        assertEquals("L2",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 1251.6875f);
        assertEquals("L2",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 1207.14f);
        assertEquals("L3",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 	1176.45f);
        assertEquals("L5",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GLONASS, 1, 	1575.420f);
        assertEquals("L1-C",label);

        // QZSS
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.QZSS, 1, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.QZSS, 1, 	1227.6f);
        assertEquals("L2",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.QZSS, 1, 	1176.45f);
        assertEquals("L5",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.QZSS, 1, 	1278.75f);
        assertEquals("L6",label);

        // Galileo
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GALILEO, 1, 	1575.42f);
        assertEquals("E1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GALILEO, 1, 	1191.795f);
        assertEquals("E5",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GALILEO, 1, 	1176.45f);
        assertEquals("E5a",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GALILEO, 1, 	1207.14f);
        assertEquals("E5b",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.GALILEO, 1, 	1278.75f);
        assertEquals("E6",label);

        // Beidou
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1561.098f);
        assertEquals("B1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1589.742f);
        assertEquals("B1-2",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1575.42f);
        assertEquals("B1C",label);

        // See #202 - We're seeing 1575.450 from Xiaomi devices
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1575.450f);
        assertEquals("B1C",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1207.14f);
        assertEquals("B2",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1176.45f);
        assertEquals("B2a",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.BEIDOU, 1, 	1268.52f);
        assertEquals("B3",label);

        // GAGAN (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 127, 	1575.42f);
        assertEquals("L1",label);

        // EGNOS
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 120, 	1575.42f);
        assertEquals("L1",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 123, 	1575.42f);
        assertEquals("L1",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 126, 	1575.42f);
        assertEquals("L1",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 120, 	1176.45f);
        assertEquals("L5",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 123, 	1176.45f);
        assertEquals("L5",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 126, 	1176.45f);
        assertEquals("L5",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 	1176.45f);
        assertEquals("L5",label);

        // INMARSAT_4F3 (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 133, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 133, 	1176.45f);
        assertEquals("L5",label);

        // Galaxy 15 (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 135, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 135, 	1176.45f);
        assertEquals("L5",label);

        // ANIK F1R (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 138, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 138, 	1176.45f);
        assertEquals("L5",label);

        // SES_5 (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 	1176.45f);
        assertEquals("L5",label);

        // MSAS (Japan)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 129, 	1575.42f);
        assertEquals("L1",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 137, 	1575.42f);
        assertEquals("L1",label);

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 129, 	1176.45f);
        assertEquals("L5",label);
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 137, 	1176.45f);
        assertEquals("L5",label);

        // Test variations on the "same" numbers to make sure floating point equality works
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1575.420000f);
        assertEquals("L1",label);
    }
}
