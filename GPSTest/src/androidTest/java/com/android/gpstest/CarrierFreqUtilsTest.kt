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

import android.os.Build
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.util.CarrierFreqUtils
import com.android.gpstest.library.util.CarrierFreqUtils.CF_UNKNOWN
import com.android.gpstest.library.util.CarrierFreqUtils.CF_UNSUPPORTED
import junit.framework.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class CarrierFreqUtilsTest {

    /**
     * Test converting GNSS signal carrier frequencies to labels like "L1"
     */
    @Test
    fun testGetCarrierFrequencyLabel() {
        var label: String

        // NAVSTAR (GPS)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL1(1, true))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("L1", label)
        } else {
            assertEquals("unsupported", label)
            // The rest of the responses will be the same for API < 26, so we can skip remaining tests
            return
        }
        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL2())
        assertEquals("L2", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL3())
        assertEquals("L3", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL4())
        assertEquals("L4", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL5(1, true))
        assertEquals("L5", label)

        // GLONASS
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL1variant1())
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL1variant2())
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL2variant1())
        assertEquals("L2", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL2variant2())
        assertEquals("L2", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL3())
        assertEquals("L3", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL5())
        assertEquals("L5", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(glonassL1Cdma())
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
                25f)
        qzssL1.hasCarrierFrequency = true
        qzssL1.carrierFrequencyHz = 1575420000.0

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
                25f)
        qzssL2.hasCarrierFrequency = true
        qzssL2.carrierFrequencyHz = 1227600000.0

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
                25f)
        qzssL5.hasCarrierFrequency = true
        qzssL5.carrierFrequencyHz = 1176450000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(qzssL5)
        assertEquals("L5", label)

        // QZSS L6
        val qzssL6 = SatelliteStatus(1,
                GnssType.QZSS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        qzssL6.hasCarrierFrequency = true
        qzssL6.carrierFrequencyHz = 1278750000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(qzssL6)
        assertEquals("L6", label)

        // Galileo
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE1(1, true))
        assertEquals("E1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE5(1, true))
        assertEquals("E5", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE5a(1, true))
        assertEquals("E5a", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE5b(1, true))
        assertEquals("E5b", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galileoE6(1, true))
        assertEquals("E6", label)

        // Beidou

        // Beidou B1I
        val beidouB1 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        beidouB1.hasCarrierFrequency = true
        beidouB1.carrierFrequencyHz = 1561098000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB1)
        assertEquals("B1I", label)

        // Beidou B1C
        val beidouB1c = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        beidouB1c.hasCarrierFrequency = true
        beidouB1c.carrierFrequencyHz = 1575420000.0

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
                25f)
        beidouB1c202.hasCarrierFrequency = true
        beidouB1c202.carrierFrequencyHz = 1575450000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB1c202)
        assertEquals("B1C", label)

        // Beidou B2a
        val beidouB2a = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        beidouB2a.hasCarrierFrequency = true
        beidouB2a.carrierFrequencyHz = 1176450000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB2a)
        assertEquals("B2a", label)

	
        // Beidou B2b
        val beidouB2b = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        beidouB2b.hasCarrierFrequency = true
        beidouB2b.carrierFrequencyHz = 1207140000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB2b)
        assertEquals("B2b", label)

        // Beidou B3I
        val beidouB3 = SatelliteStatus(1,
                GnssType.BEIDOU,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        beidouB3.hasCarrierFrequency = true
        beidouB3.carrierFrequencyHz = 1268520000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(beidouB3)
        assertEquals("B3I", label)

        // IRNSS

        // IRNSS L1
        val irnssL1 = SatelliteStatus(1,
                GnssType.IRNSS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        irnssL1.hasCarrierFrequency = true
        irnssL1.carrierFrequencyHz = 1575420000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(irnssL1)
        assertEquals("L1", label)

        // IRNSS L5
        val irnssL5 = SatelliteStatus(1,
                GnssType.IRNSS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        irnssL5.hasCarrierFrequency = true
        irnssL5.carrierFrequencyHz = 1176450000.0

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
                25f)
        irnssS.hasCarrierFrequency = true
        irnssS.carrierFrequencyHz = 2492028000.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(irnssS)
        assertEquals("S", label)

        // GAGAN (SBAS)
        val gagan = SatelliteStatus(127,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        gagan.hasCarrierFrequency = true
        gagan.carrierFrequencyHz = 1575420000.0
        gagan.sbasType = SbasType.GAGAN

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gagan)
        assertEquals("L1", label)

        // EGNOS - ID 121 L1
        val egnos121 = SatelliteStatus(121,
                                       GnssType.SBAS,
                                       30f,
                                       true,
                                       true,
                                       true,
                                       72f,
                                       25f)
        egnos121.hasCarrierFrequency = true
        egnos121.carrierFrequencyHz = 1575420000.0
        egnos121.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos121)
        assertEquals("L1", label)

        // EGNOS - ID 123 L1
        val egnos123 = SatelliteStatus(123,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos123.hasCarrierFrequency = true
        egnos123.carrierFrequencyHz = 1575420000.0
        egnos123.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos123)
        assertEquals("L1", label)

        // EGNOS - ID 126 L1
        val egnos126 = SatelliteStatus(126,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos126.hasCarrierFrequency = true
        egnos126.carrierFrequencyHz = 1575420000.0
        egnos126.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos126)
        assertEquals("L1", label)

        // EGNOS - ID 136 L1
        val egnos136 = SatelliteStatus(136,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos136.hasCarrierFrequency = true
        egnos136.carrierFrequencyHz = 1575420000.0
        egnos136.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos136)
        assertEquals("L1", label)

        // EGNOS - ID 121 L5
        val egnos121L5 = SatelliteStatus(121,
                                         GnssType.SBAS,
                                         30f,
                                         true,
                                         true,
                                         true,
                                         72f,
                                         25f)
        egnos121L5.hasCarrierFrequency = true
        egnos121L5.carrierFrequencyHz = 1176450000.0
        egnos121L5.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos121L5)
        assertEquals("L5", label)

        // EGNOS - ID 123 L5
        val egnos123L5 = SatelliteStatus(123,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos123L5.hasCarrierFrequency = true
        egnos123L5.carrierFrequencyHz = 1176450000.0
        egnos123L5.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos123L5)
        assertEquals("L5", label)

        // EGNOS - ID 126 L5
        val egnos126L5 = SatelliteStatus(126,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos126L5.hasCarrierFrequency = true
        egnos126L5.carrierFrequencyHz = 1176450000.0
        egnos126L5.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos126L5)
        assertEquals("L5", label)

        // EGNOS - ID 136 L5
        val egnos136L5 = SatelliteStatus(136,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos136L5.hasCarrierFrequency = true
        egnos136L5.carrierFrequencyHz = 1176450000.0
        egnos136L5.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos136L5)
        assertEquals("L5", label)

        // INMARSAT_4F3 (SBAS)

        // EGNOS - ID 133 L1
        val egnos133L1 = SatelliteStatus(133,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos133L1.hasCarrierFrequency = true
        egnos133L1.carrierFrequencyHz = 1575420000.0
        egnos133L1.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos133L1)
        assertEquals("L1", label)

        // EGNOS - ID 133 L5
        val egnos133L5 = SatelliteStatus(133,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        egnos133L5.hasCarrierFrequency = true
        egnos133L5.carrierFrequencyHz = 1176450000.0
        egnos133L5.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(egnos133L5)
        assertEquals("L5", label)

        // WAAS 131
        label = CarrierFreqUtils.getCarrierFrequencyLabel(waas_131L1(true))
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(waas_131L5(true))
        assertEquals("L5", label)

        // WAAS 133
        label = CarrierFreqUtils.getCarrierFrequencyLabel(waas_133L1(true))
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(waas_133L5(true))
        assertEquals("L5", label)

        // Galaxy 15 (WAAS 135)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galaxy15_135L1(true))
        assertEquals("L1", label)
        label = CarrierFreqUtils.getCarrierFrequencyLabel(galaxy15_135L5(true))
        assertEquals("L5", label)

        // ANIK F1R (WAAS 138) L1
        val anik15_138L1 = SatelliteStatus(138,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        anik15_138L1.hasCarrierFrequency = true
        anik15_138L1.carrierFrequencyHz = 1575420000.0
        anik15_138L1.sbasType = SbasType.WAAS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(anik15_138L1)
        assertEquals("L1", label)

        // ANIK F1R (WAAS 138) L5
        val anik15_138L5 = SatelliteStatus(138,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        anik15_138L5.hasCarrierFrequency = true
        anik15_138L5.carrierFrequencyHz = 1176450000.0
        anik15_138L5.sbasType = SbasType.WAAS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(anik15_138L5)
        assertEquals("L5", label)

        // SES_5 - 136 L1 (EGNOS)
        val ses5_136L1 = SatelliteStatus(136,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        ses5_136L1.hasCarrierFrequency = true
        ses5_136L1.carrierFrequencyHz = 1575420000.0
        ses5_136L1.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(ses5_136L1)
        assertEquals("L1", label)

        // SES_5 - 136 L5 (EGNOS)
        val ses5_136L5 = SatelliteStatus(136,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        ses5_136L5.hasCarrierFrequency = true
        ses5_136L5.carrierFrequencyHz = 1176450000.0
        ses5_136L5.sbasType = SbasType.EGNOS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(ses5_136L5)
        assertEquals("L5", label)

        // MSAS (Japan)

        // MSAS 129 - L1
        val msas129L1 = SatelliteStatus(129,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        msas129L1.hasCarrierFrequency = true
        msas129L1.carrierFrequencyHz = 1575420000.0
        msas129L1.sbasType = SbasType.MSAS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(msas129L1)
        assertEquals("L1", label)

        // MSAS 129 - L5
        val msas129L5 = SatelliteStatus(129,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        msas129L5.hasCarrierFrequency = true
        msas129L5.carrierFrequencyHz = 1176450000.0
        msas129L5.sbasType = SbasType.MSAS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(msas129L5)
        assertEquals("L5", label)

        // MSAS L1 - 137
        val msas137L1 = SatelliteStatus(137,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        msas137L1.hasCarrierFrequency = true
        msas137L1.carrierFrequencyHz = 1575420000.0
        msas137L1.sbasType = SbasType.MSAS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(msas137L1)
        assertEquals("L1", label)

        // MSAS L5 - 137
        val msas137L5 = SatelliteStatus(137,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        msas137L5.hasCarrierFrequency = true
        msas137L5.carrierFrequencyHz = 1176450000.0
        msas137L5.sbasType = SbasType.MSAS

        label = CarrierFreqUtils.getCarrierFrequencyLabel(msas137L5)
        assertEquals("L5", label)

      // SDCM L1 - 125
      val sdcm125L1 = SatelliteStatus(125,
                                      GnssType.SBAS,
                                      30f,
                                      true,
                                      true,
                                      true,
                                      72f,
                                      25f)
        sdcm125L1.hasCarrierFrequency = true
      sdcm125L1.carrierFrequencyHz = 1575420000.0
      sdcm125L1.sbasType = SbasType.SDCM

      label = CarrierFreqUtils.getCarrierFrequencyLabel(sdcm125L1)
      assertEquals("L1-C", label)

      // SDCM L5 - 141
      val sdcm125L5 = SatelliteStatus(141,
                                      GnssType.SBAS,
                                      30f,
                                      true,
                                      true,
                                      true,
                                      72f,
                                      25f)
        sdcm125L5.hasCarrierFrequency = true
      sdcm125L5.carrierFrequencyHz = 1176450000.0
      sdcm125L5.sbasType = SbasType.SDCM

      label = CarrierFreqUtils.getCarrierFrequencyLabel(sdcm125L5)
      assertEquals("L5", label)
	  
        // INMARSAT_4F1 (SBAS)

        // SouthPAN - ID 122 L1
        val southpan122L1 = SatelliteStatus(122,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        southpan122L1.hasCarrierFrequency = true
        southpan122L1.carrierFrequencyHz = 1575420000.0
        southpan122L1.sbasType = SbasType.SOUTHPAN

        label = CarrierFreqUtils.getCarrierFrequencyLabel(southpan122L1)
        assertEquals("L1", label)

        // SouthPAN - ID 122 L5
        val southpan122L5 = SatelliteStatus(122,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        southpan122L5.hasCarrierFrequency = true
        southpan122L5.carrierFrequencyHz = 1176450000.0
        southpan122L5.sbasType = SbasType.SOUTHPAN

        label = CarrierFreqUtils.getCarrierFrequencyLabel(southpan122L5)
        assertEquals("L5", label)	  
	  

        // Test variations on the "same" numbers to make sure floating point equality works
        val gpsL1variation = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        gpsL1variation.hasCarrierFrequency = true
        gpsL1variation.carrierFrequencyHz = 1575420000.0000000

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL1variation)
        assertEquals("L1", label)

        // Test a satellite without a carrier frequency
        val gpsL1noCf = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f)

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL1noCf)
        assertEquals(CF_UNSUPPORTED, label)

        // Test a satellite with a bad (unknown) carrier frequency
        val gpsL1badCf = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f)
        gpsL1badCf.hasCarrierFrequency = true
        gpsL1badCf.carrierFrequencyHz = 12345.0

        label = CarrierFreqUtils.getCarrierFrequencyLabel(gpsL1badCf)
        assertEquals(CF_UNKNOWN, label)
    }

    /**
     * Tests detecting if a carrier frequency label is for a primary frequency such as L1 and not
     * L5
     */
    @Test
    fun testIsPrimaryCarrier() {
        assertTrue(CarrierFreqUtils.isPrimaryCarrier("L1"))
        assertTrue(CarrierFreqUtils.isPrimaryCarrier("E1"))
        assertTrue(CarrierFreqUtils.isPrimaryCarrier("B1I"))
        assertTrue(CarrierFreqUtils.isPrimaryCarrier("B1C"))
        assertTrue(CarrierFreqUtils.isPrimaryCarrier("L1-C"))

        assertFalse(CarrierFreqUtils.isPrimaryCarrier("L2"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("L3"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("L4"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("L5"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("E5"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("E5a"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("E5b"))
        assertFalse(CarrierFreqUtils.isPrimaryCarrier("E6"))
    }
}
