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
import com.android.gpstest.model.SbasType
import com.android.gpstest.util.SatelliteUtil.altitudeComparedTo
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
        assertTrue(location.altitudeComparedTo(geoidAltitude))

        location.altitude = -5.91111
        assertTrue(location.altitudeComparedTo(geoidAltitude))

        location.altitude = -5.99999
        assertFalse(location.altitudeComparedTo(geoidAltitude))
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
}