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

package com.android.gpstest

import androidx.test.runner.AndroidJUnit4
import com.android.gpstest.model.GnssType
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.util.CarrierFreqUtils
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarrierFreqUtilsTest {

    /**
     * Test converting GNSS signal carrier frequencies to labels like "L1"
     */
    @Test
    fun testGetCarrierFrequencyLabel() {
        var label: String

        // NAVSTAR (GPS)
        // GPS L1
        val gpsL1 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL1.hasCarrierFrequency = true
        gpsL1.carrierFrequencyHz = 1575420000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL1)
        assertEquals("L1", label)

        // GPS L2
        val gpsL2 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL2.hasCarrierFrequency = true
        gpsL2.carrierFrequencyHz = 1227600000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL2)
        assertEquals("L2", label)

        // GPS L3
        val gpsL3 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL3.hasCarrierFrequency = true
        gpsL3.carrierFrequencyHz = 1381050000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL3)
        assertEquals("L3", label)

        // GPS L4
        val gpsL4 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL4.hasCarrierFrequency = true
        gpsL4.carrierFrequencyHz = 1379913000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL4)
        assertEquals("L4", label)

        // GPS L5
        val gpsL5 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL5.hasCarrierFrequency = true
        gpsL5.carrierFrequencyHz = 1176450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL5)
        assertEquals("L5", label)

        // GLONASS

        // GLONASS L1
        val glonassL1 = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL1.hasCarrierFrequency = true
        glonassL1.carrierFrequencyHz = 1598062500.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL1)
        assertEquals("L1", label)

        // GLONASS L1 again
        val glonassL1again = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL1again.hasCarrierFrequency = true
        glonassL1again.carrierFrequencyHz = 1605375000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL1again)
        assertEquals("L1", label)

        // GLONASS L2
        val glonassL2 = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL2.hasCarrierFrequency = true
        glonassL2.carrierFrequencyHz = 1242937500.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL2)
        assertEquals("L2", label)

        // GLONASS L2
        val glonassL2again = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL2again.hasCarrierFrequency = true
        glonassL2again.carrierFrequencyHz = 1248625000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL2again)
        assertEquals("L2", label)

        // GLONASS L3
        val glonassL3 = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL3.hasCarrierFrequency = true
        glonassL3.carrierFrequencyHz = 1207140000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL3)
        assertEquals("L3", label)

        // GLONASS L5
        val glonassL5 = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL5.hasCarrierFrequency = true
        glonassL5.carrierFrequencyHz = 1176450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL5)
        assertEquals("L5", label)

        // GLONASS L1 CDMA
        val glonassL1Cdma = SatelliteStatus(1,
                GnssType.GLONASS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        glonassL1Cdma.hasCarrierFrequency = true
        glonassL1Cdma.carrierFrequencyHz = 1575420000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL1Cdma)
        assertEquals("L1-C", label)

        // QZSS

        // QZSS L1
        val qzssL1 = SatelliteStatus(1,
                GnssType.QZSS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        qzssL1.hasCarrierFrequency = true
        qzssL1.carrierFrequencyHz = 1575420000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(qzssL1)
        assertEquals("L1", label)

        // QZSS L2
        val qzssL2 = SatelliteStatus(1,
                GnssType.QZSS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        qzssL2.hasCarrierFrequency = true
        qzssL2.carrierFrequencyHz = 1227600000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(qzssL2)
        assertEquals("L2", label)

        // QZSS L5
        val qzssL5 = SatelliteStatus(1,
                GnssType.QZSS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        qzssL5.hasCarrierFrequency = true
        qzssL5.carrierFrequencyHz = 1176450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(qzssL5)
        assertEquals("L5", label)

        // QZSS L6
        val qzssL6 = SatelliteStatus(1,
                GnssType.GALILEO,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        qzssL6.hasCarrierFrequency = true
        qzssL6.carrierFrequencyHz = 1278750000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(qzssL6)
        assertEquals("L6", label)

        // Galileo

        // Galileo E1
        val galileoE1 = SatelliteStatus(1,
                GnssType.GALILEO,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        galileoE1.hasCarrierFrequency = true
        galileoE1.carrierFrequencyHz = 1575420000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE1)
        assertEquals("E1", label)

        // Galileo E5
        val galileoE5 = SatelliteStatus(1,
                GnssType.GALILEO,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        galileoE5.hasCarrierFrequency = true
        galileoE5.carrierFrequencyHz = 1191795000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE5)
        assertEquals("E5", label)

        // Galileo E5a
        val galileoE5a = SatelliteStatus(1,
                GnssType.GALILEO,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        galileoE5a.hasCarrierFrequency = true
        galileoE5a.carrierFrequencyHz = 1176450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE5a)
        assertEquals("E5a", label)

        // Galileo E5b
        val galileoE5b = SatelliteStatus(1,
                GnssType.GALILEO,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        galileoE5b.hasCarrierFrequency = true
        galileoE5b.carrierFrequencyHz = 1207140000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE5b)
        assertEquals("E5b", label)

        // Galileo E6
        val galileoE6 = SatelliteStatus(1,
                GnssType.GALILEO,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        galileoE6.hasCarrierFrequency = true
        galileoE6.carrierFrequencyHz = 1278750000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE6)
        assertEquals("E6", label)

        // Beidou

        // Beidou B1
        val beidouB1 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        galileoE6.hasCarrierFrequency = true
        galileoE6.carrierFrequencyHz = 1561098000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB1)
        assertEquals("B1", label)

        // Beidou B1-2
        val beidouB1_2 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        beidouB1_2.hasCarrierFrequency = true
        beidouB1_2.carrierFrequencyHz = 1589742000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB1_2)
        assertEquals("B1-2", label)

        // Beidou B1C
        val beidouB1c = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        beidouB1c.hasCarrierFrequency = true
        beidouB1c.carrierFrequencyHz = 1589742000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB1c)
        assertEquals("B1C", label)

        // Beidou B1C - See #202 - We're seeing 1575.450 from Xiaomi devices
        val beidouB1c202 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        beidouB1c202.hasCarrierFrequency = true
        beidouB1c202.carrierFrequencyHz = 1575450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB1c202)
        assertEquals("B1C", label)

        // Beidou B2
        val beidouB2 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        beidouB2.hasCarrierFrequency = true
        beidouB2.carrierFrequencyHz = 1207140000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB2)
        assertEquals("B2", label)

        // Beidou B2a
        val beidouB2a = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        beidouB2a.hasCarrierFrequency = true
        beidouB2a.carrierFrequencyHz = 1176450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB2a)
        assertEquals("B2a", label)

        // Beidou B3
        val beidouB3 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        beidouB3.hasCarrierFrequency = true
        beidouB3.carrierFrequencyHz = 1268520000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB3)
        assertEquals("B3", label)

        // IRNSS

        // IRNSS L5
        val irnssL5 = SatelliteStatus(1,
                GnssType.IRNSS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        irnssL5.hasCarrierFrequency = true
        irnssL5.carrierFrequencyHz = 1176450000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(irnssL5)
        assertEquals("L5", label)

        // IRNSS S
        val irnssS = SatelliteStatus(1,
                GnssType.IRNSS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        irnssS.hasCarrierFrequency = true
        irnssS.carrierFrequencyHz = 2492028000.0f

        label = CarrierFreqUtils.getCarrierFrequencyLabel(irnssS)
        assertEquals("S", label)

        // GAGAN (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 127, 1575.42f)
        assertEquals("L1", label)

        // EGNOS
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 120, 1575.42f)
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 123, 1575.42f)
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 126, 1575.42f)
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 1575.42f)
        assertEquals("L1", label)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 120, 1176.45f)
        assertEquals("L5", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 123, 1176.45f)
        assertEquals("L5", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 126, 1176.45f)
        assertEquals("L5", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 1176.45f)
        assertEquals("L5", label)

        // INMARSAT_4F3 (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 133, 1575.42f)
        assertEquals("L1", label)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 133, 1176.45f)
        assertEquals("L5", label)

        // Galaxy 15 (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 135, 1575.42f)
        assertEquals("L1", label)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 135, 1176.45f)
        assertEquals("L5", label)

        // ANIK F1R (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 138, 1575.42f)
        assertEquals("L1", label)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 138, 1176.45f)
        assertEquals("L5", label)

        // SES_5 (SBAS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 1575.42f)
        assertEquals("L1", label)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 136, 1176.45f)
        assertEquals("L5", label)

        // MSAS (Japan)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 129, 1575.42f)
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 137, 1575.42f)
        assertEquals("L1", label)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 129, 1176.45f)
        assertEquals("L5", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.SBAS, 137, 1176.45f)
        assertEquals("L5", label)

        // Test variations on the "same" numbers to make sure floating point equality works
        label = CarrierFreqUtils.getCarrierFrequencyLabel(GnssType.NAVSTAR, 1, 1575.420000f)
        assertEquals("L1", label)
    }
}
