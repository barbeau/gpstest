/*
 * Copyright (C) 2019 Sean J. Barbeau
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

import android.location.GnssMeasurement.*
import android.location.Location
import android.os.Build
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.android.gpstest.model.GeoidAltitude
import com.android.gpstest.model.GnssType
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.model.SatelliteStatus.Companion.NO_DATA
import com.android.gpstest.model.SbasType
import com.android.gpstest.util.SatelliteUtil.altitudeComparedTo
import com.android.gpstest.util.SatelliteUtil.isMissingData
import com.android.gpstest.util.SatelliteUtil.isTimeEqualTo
import com.android.gpstest.util.SatelliteUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test utilities used to manage GNSS satellites and signals
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class SatelliteUtilsTest {

    /**
     * Test creating unique keys for satellites given a signal
     */
    @Test
    fun testCreateGnssSatelliteKey() {
        // GPS L1 - with CF
        val gpsL1 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL1.hasCarrierFrequency = true
        gpsL1.carrierFrequencyHz = 1575420000.0

        val gpsL1key = SatelliteUtils.createGnssSatelliteKey(gpsL1)
        assertEquals("1 NAVSTAR", gpsL1key)

        // GPS L1 - without CF
        val gpsL1NoCf = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);

        val gpsL1NoCfkey = SatelliteUtils.createGnssSatelliteKey(gpsL1NoCf)
        assertEquals("1 NAVSTAR", gpsL1NoCfkey)

        // SBAS WAAS L1 - with CF
        val sbasWaasL1 = SatelliteStatus(1,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        sbasWaasL1.hasCarrierFrequency = true
        sbasWaasL1.carrierFrequencyHz = 1575420000.0
        sbasWaasL1.sbasType = SbasType.WAAS

        val sbasWaasL1key = SatelliteUtils.createGnssSatelliteKey(sbasWaasL1)
        assertEquals("1 SBAS WAAS", sbasWaasL1key)

        // SBAS WAAS L1 - without CF
        val sbasWaasL1NoCf = SatelliteStatus(1,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        sbasWaasL1NoCf.sbasType = SbasType.WAAS

        val sbasWaasL1NoCfkey = SatelliteUtils.createGnssSatelliteKey(sbasWaasL1NoCf)
        assertEquals("1 SBAS WAAS", sbasWaasL1NoCfkey)

        // SBAS SDCM L1 - without CF
        val sbasSdcm125L1 = SatelliteStatus(125,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        sbasSdcm125L1.sbasType = SbasType.SDCM

        val sbasSdcmL1key = SatelliteUtils.createGnssSatelliteKey(sbasSdcm125L1)
        assertEquals("125 SBAS SDCM", sbasSdcmL1key)

        // SBAS SDCM L1 - with CF
        val sbasSdcm125L1WithCf = SatelliteStatus(125,
                GnssType.SBAS,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        sbasSdcm125L1WithCf.hasCarrierFrequency = true
        sbasSdcm125L1WithCf.carrierFrequencyHz = 1575420000.0
        sbasSdcm125L1WithCf.sbasType = SbasType.SDCM

        val sbasSdcm125L1WithCfkey = SatelliteUtils.createGnssSatelliteKey(sbasSdcm125L1WithCf)
        assertEquals("125 SBAS SDCM", sbasSdcm125L1WithCfkey)
    }

    /**
     * Test creating unique keys for signals
     */
    @Test
    fun testCreateGnssStatusKey() {
        // GPS L1 - with CF
        val gpsL1 = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsL1.hasCarrierFrequency = true
        gpsL1.carrierFrequencyHz = 1575420000.0

        val gpsL1key = SatelliteUtils.createGnssStatusKey(gpsL1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("1 NAVSTAR L1", gpsL1key)
        } else {
            assertEquals("1 NAVSTAR unsupported", gpsL1key)
        }

        // GPS L1 - with CF
        val gpsL1NoCf = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);

        val gpsL1NoCfkey = SatelliteUtils.createGnssStatusKey(gpsL1NoCf)
        assertEquals("1 NAVSTAR unsupported", gpsL1NoCfkey)

        // GPS with bad CF
        val gpsBadCf = SatelliteStatus(1,
                GnssType.NAVSTAR,
                30f,
                true,
                true,
                true,
                72f,
                25f);
        gpsBadCf.hasCarrierFrequency = true
        gpsBadCf.carrierFrequencyHz = 9999999.0

        val gpsBadCfKey = SatelliteUtils.createGnssStatusKey(gpsBadCf)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals("1 NAVSTAR unknown", gpsBadCfKey)
        } else {
            assertEquals("1 NAVSTAR unsupported", gpsBadCfKey)
        }
    }

    /**
     * Test checking support for accumulated delta range state
     */
    @Test
    fun testAccumulatedDeltaRangeStateSupport() {
        // Valid states
        var adrState = ADR_STATE_VALID
        assertTrue(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_VALID or ADR_STATE_HALF_CYCLE_REPORTED
        assertTrue(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_VALID or ADR_STATE_HALF_CYCLE_RESOLVED
        assertTrue(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_VALID or ADR_STATE_HALF_CYCLE_REPORTED or ADR_STATE_RESET
        assertTrue(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_VALID or ADR_STATE_HALF_CYCLE_REPORTED or ADR_STATE_HALF_CYCLE_RESOLVED
        assertTrue(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_VALID or ADR_STATE_HALF_CYCLE_REPORTED or ADR_STATE_HALF_CYCLE_RESOLVED or ADR_STATE_CYCLE_SLIP
        assertTrue(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        // Invalid states
        adrState = ADR_STATE_UNKNOWN
        assertFalse(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_HALF_CYCLE_REPORTED
        assertFalse(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_CYCLE_SLIP
        assertFalse(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_RESET
        assertFalse(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_CYCLE_SLIP or ADR_STATE_HALF_CYCLE_REPORTED
        assertFalse(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))

        adrState = ADR_STATE_CYCLE_SLIP or ADR_STATE_HALF_CYCLE_REPORTED or ADR_STATE_RESET
        assertFalse(SatelliteUtils.isAccumulatedDeltaRangeStateValid(adrState))
    }

    /**
     * Test for comparing WGS84 altitude, height of the geoid above the WGS84 ellipsoid,
     * and altitude above mean sea level.
     *
     * H = -N + h
     *
     * or
     * N = h - H
     *
     * ..where:
     * * H = geoidAltitude.altitudeMsl, or geoid altitude
     * * N = geoidAltitude.heightOfGeoid above the WGS84 ellipsoid
     * * h = Location.altitude], or the location WGS84 altitude (height above the WGS84 ellipsoid)
     *
     * See https://issuetracker.google.com/issues/191674805 for details.
     */
    @Test
    fun testLocationAltitudeComparedToGeoidAltitude() {
        val geoidAltitude = GeoidAltitude(altitudeMsl = 18.1, heightOfGeoid = -24.0)
        val location = Location("test")
        location.altitude = -5.9
        assertTrue(location.altitudeComparedTo(geoidAltitude).isSame)

        location.altitude = -5.91111
        assertTrue(location.altitudeComparedTo(geoidAltitude).isSame)

        location.altitude = -5.99999
        assertFalse(location.altitudeComparedTo(geoidAltitude).isSame)

        // Based on data from Samsung Galaxy S21+
        val `geoidAltitude16-1` = GeoidAltitude(altitudeMsl = 16.1, heightOfGeoid = -24.0)
        location.altitude = -7.88262939453125
        assertTrue(location.altitudeComparedTo(`geoidAltitude16-1`).isSame)

        val `geoidAltitude16-0` = GeoidAltitude(altitudeMsl = 16.0, heightOfGeoid = -24.0)
        location.altitude = -7.979248046875
        assertTrue(location.altitudeComparedTo(`geoidAltitude16-0`).isSame)

        location.altitude = -7.99603271484375
        assertTrue(location.altitudeComparedTo(`geoidAltitude16-0`).isSame)

        location.altitude = -7.9793701171875
        assertTrue(location.altitudeComparedTo(`geoidAltitude16-0`).isSame)

        val `geoidAltitude15-7` = GeoidAltitude(altitudeMsl = 15.7, heightOfGeoid = -24.0)
        location.altitude = -8.302093505859375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.3095703125
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.311126708984375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.3275146484375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.252044677734375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.2552490234375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.277252197265625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.28192138671875
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.284088134765625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.286651611328125
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.30511474609375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.306427001953125
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        location.altitude = -8.346435546875
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-7`).isSame)

        val `geoidAltitude15-8` = GeoidAltitude(altitudeMsl = 15.8, heightOfGeoid = -24.0)
        location.altitude = -8.194122314453125
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.181121826171875
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.1817626953125
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.185455322265625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.18609619140625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.2017822265625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.21240234375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.2166748046875
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.237640380859375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.2392578125
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.243896484375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        location.altitude = -8.245269775390625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-8`).isSame)

        val `geoidAltitude15-6` = GeoidAltitude(altitudeMsl = 15.6, heightOfGeoid = -24.0)
        location.altitude = -8.353424072265625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-6`).isSame)

        location.altitude = -8.37744140625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-6`).isSame)

        location.altitude = -8.37994384765625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-6`).isSame)

        location.altitude = -8.38427734375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-6`).isSame)

        val `geoidAltitude15-5` = GeoidAltitude(altitudeMsl = 15.5, heightOfGeoid = -24.0)
        location.altitude = -8.51861572265625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-5`).isSame)

        location.altitude = -8.523681640625
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-5`).isSame)

        location.altitude = -8.529998779296875
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-5`).isSame)

        location.altitude = -8.53466796875
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-5`).isSame)

        val `geoidAltitude15-4` = GeoidAltitude(altitudeMsl = 15.4, heightOfGeoid = -24.0)
        location.altitude = -8.589202880859375
        assertTrue(location.altitudeComparedTo(`geoidAltitude15-4`).isSame)
    }

    /**
     * Tests if a location time is approximately equal to geoidAltitude time
     */
    @Test
    fun testLocationIsTimeEqualTo() {
        val geoidAltitude = GeoidAltitude(1000, altitudeMsl = 18.1, heightOfGeoid = -24.0)
        val location = Location("test")
        location.time = 1000
        assertTrue(location.isTimeEqualTo(geoidAltitude))

        location.time = 1099
        assertTrue(location.isTimeEqualTo(geoidAltitude))

        location.time = 901
        assertTrue(location.isTimeEqualTo(geoidAltitude))

        location.time = 1100
        assertFalse(location.isTimeEqualTo(geoidAltitude))

        location.time = 900
        assertFalse(location.isTimeEqualTo(geoidAltitude))
    }

    @Test
    fun testStatusMissingData() {
        // GPS with missing data
        val statusMissingDataNotUsed = SatelliteStatus(1,
            GnssType.NAVSTAR,
            30f,
            false,
            false,
            false,
            NO_DATA,
            NO_DATA);
        assertTrue(statusMissingDataNotUsed.isMissingData())

        val statusMissingDataUsed = SatelliteStatus(1,
            GnssType.NAVSTAR,
            30f,
            false,
            true,
            true,
            NO_DATA,
            NO_DATA);
        assertTrue(statusMissingDataUsed.isMissingData())

        // Good signal
        assertFalse(gpsL1(1, true).isMissingData())
    }
}