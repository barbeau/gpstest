/*
 * Copyright (C) 2021 Sean J. Barbeau
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

import android.location.GnssAntennaInfo
import android.location.Location
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.Orientation
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.util.FormatUtils.toLog
import com.android.gpstest.library.util.IOUtils
import com.android.gpstest.library.util.SatelliteUtil.isBearingAccuracySupported
import com.android.gpstest.library.util.SatelliteUtil.isSpeedAccuracySupported
import com.android.gpstest.library.util.SatelliteUtil.isVerticalAccuracySupported
import junit.framework.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class FormatUtilsTest {

    @Test
    fun locationToLog() {
        val l = Location("test")
        l.latitude = 45.34567899
        l.longitude = 12.45678901
        l.altitude = 56.2
        l.speed = 19.2f
        l.accuracy = 98.7f
        l.bearing = 100.1f
        l.time = 12345
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            l.speedAccuracyMetersPerSecond = 382.7f
            l.bearingAccuracyDegrees = 284.1f
            l.verticalAccuracyMeters = 583.4f
        }
        l.elapsedRealtimeNanos = 123456789

        // Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters,MockLocation
        if (l.isSpeedAccuracySupported() && l.isBearingAccuracySupported() && l.isVerticalAccuracySupported()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertEquals(
                    "Fix,test,45.34567899,12.45678901,56.2,19.2,98.7,100.1,12345,382.7,284.1,123456789,583.4,0",
                    l.toLog()
                )
            } else {
                assertEquals(
                    "Fix,test,45.34567899,12.45678901,56.2,19.2,98.7,100.1,12345,382.7,284.1,123456789,583.4,",
                    l.toLog()
                )
            }
        } else {
            assertEquals(
                "Fix,test,45.34567899,12.45678901,56.2,19.2,98.7,100.1,12345,,,123456789,,",
                l.toLog()
            )
        }
    }

    @Test
    fun locationToLog_NoExponentialNotation() {
        val l = Location("test")
        l.latitude = 0.00000000000000000001
        l.longitude = 0.00000000000000000001
        l.altitude = 0.00000000000000000001
        l.speed = 0.00000000000000000001f
        l.accuracy = 0.00000000000000000001f
        l.bearing = 0.00000000000000000001f
        l.time = 12345
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            l.speedAccuracyMetersPerSecond = 0.00000000000000000001f
            l.bearingAccuracyDegrees = 0.00000000000000000001f
            l.verticalAccuracyMeters = 0.00000000000000000001f
        }
        l.elapsedRealtimeNanos = 123456789

        // Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters
        if (l.isSpeedAccuracySupported() && l.isBearingAccuracySupported() && l.isVerticalAccuracySupported()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertEquals(
                    "Fix,test,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,12345,0.000000000000000000010,0.000000000000000000010,123456789,0.000000000000000000010,0",
                    l.toLog()
                )
            } else {
                assertEquals(
                    "Fix,test,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,12345,0.000000000000000000010,0.000000000000000000010,123456789,0.000000000000000000010,",
                    l.toLog()
                )
            }
        } else {
            assertEquals(
                "Fix,test,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,0.000000000000000000010,12345,,,123456789,,",
                l.toLog()
            )
        }
    }

    @Test
    fun locationToLog_MockLocation() {
        val l = Location("test")
        l.latitude = 45.34567899
        l.longitude = 12.45678901
        l.time = 12345
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            l.isMock = true
        }

        // Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters,MockLocation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertEquals(
                "Fix,test,45.34567899,12.45678901,0.0,0.0,0.0,0.0,12345,,,0,,1",
                l.toLog()
            )
        } else {
            assertEquals(
                "Fix,test,45.34567899,12.45678901,0.0,0.0,0.0,0.0,12345,,,0,,",
                l.toLog()
            )
        }
    }

    /**
     * Test writing GnssAntennaInfo to CSV format (only runs on Android R or higher)
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun testSerializeGnssAntennaInfo() {
        val builder = GnssAntennaInfo.Builder()
        builder.setCarrierFrequencyMHz(1575.42)
        builder.setPhaseCenterOffset(
            GnssAntennaInfo.PhaseCenterOffset(
                1.2,0.1,
                3.4,0.2,
                5.6,0.3))
        builder.setPhaseCenterVariationCorrections(
            GnssAntennaInfo.SphericalCorrections(
                buildPhaseCenterVariationCorrectionsArray(),
                buildPhaseCenterVariationCorrectionsUncertaintyArray()
            )
        )
        builder.setSignalGainCorrections(
            GnssAntennaInfo.SphericalCorrections(
                buildSignalGainCorrectionsArray(),
                buildSignalGainCorrectionsUncertaintyArray()
            )
        )

        val expected = "GnssAntennaInfo,1575.42,1.2,0.1,3.4,0.2,5.6,0.3," +
                "[11.22 33.44 55.66 77.88; 10.2 30.4 50.6 70.8; 12.2 34.4 56.6 78.8]," +
                "[0.1 0.2 0.3 0.4; 1.1 1.2 1.3 1.4; 2.1 2.2 2.3 2.4],60.0,120.0," +
                "[9.8 8.7 7.6 6.5; 5.4 4.3 3.2 2.1; 1.3 2.4 3.5 4.6]," +
                "[0.11 0.22 0.33 0.44; 0.55 0.66 0.77 0.88; 0.91 0.92 0.93 0.94],60.0,120.0"
        Assert.assertEquals(expected, builder.build().toLog())
    }

    /**
     * Test writing array of doubles to String (for serializing GnssAntennaInfo)
     */
    @Test
    fun testSerializeDoubleArray() {
        val data = buildPhaseCenterVariationCorrectionsArray()

        val expected = "[11.22 33.44 55.66 77.88; 10.2 30.4 50.6 70.8; 12.2 34.4 56.6 78.8]"
        Assert.assertEquals(expected, IOUtils.serialize(data))
    }

    private fun buildPhaseCenterVariationCorrectionsArray() : Array<DoubleArray> {
        val array1: DoubleArray = doubleArrayOf(11.22, 33.44, 55.66, 77.88)
        val array2: DoubleArray = doubleArrayOf(10.2, 30.4, 50.6, 70.8)
        val array3: DoubleArray = doubleArrayOf(12.2, 34.4, 56.6, 78.8)
        return arrayOf(array1, array2, array3)
    }

    private fun buildPhaseCenterVariationCorrectionsUncertaintyArray() : Array<DoubleArray> {
        val array1: DoubleArray = doubleArrayOf(0.1, 0.2, 0.3, 0.4)
        val array2: DoubleArray = doubleArrayOf(1.1, 1.2, 1.3, 1.4)
        val array3: DoubleArray = doubleArrayOf(2.1, 2.2, 2.3, 2.4)
        return arrayOf(array1, array2, array3)
    }

    private fun buildSignalGainCorrectionsArray() : Array<DoubleArray> {
        val array1: DoubleArray = doubleArrayOf(9.8, 8.7, 7.6, 6.5)
        val array2: DoubleArray = doubleArrayOf(5.4, 4.3, 3.2, 2.1)
        val array3: DoubleArray = doubleArrayOf(1.3, 2.4, 3.5, 4.6)
        return arrayOf(array1, array2, array3)
    }

    private fun buildSignalGainCorrectionsUncertaintyArray() : Array<DoubleArray> {
        val array1: DoubleArray = doubleArrayOf(0.11, 0.22, 0.33, 0.44)
        val array2: DoubleArray = doubleArrayOf(0.55, 0.66, 0.77, 0.88)
        val array3: DoubleArray = doubleArrayOf(0.91, 0.92, 0.93, 0.94)
        return arrayOf(array1, array2, array3)
    }

    @Test
    fun testSatelliteToLog() {
        val location = Location("test")
        location.time = 0

        val signalCount = 25
        val signalIndex = 0

        val gpsL1 = SatelliteStatus(
            10,
            GnssType.NAVSTAR,
            35.00f,
            hasAlmanac = true,
            hasEphemeris = true,
            usedInFix = true,
            elevationDegrees = 57.00f,
            azimuthDegrees = 136.00f
        );
        gpsL1.hasCarrierFrequency = true
        gpsL1.carrierFrequencyHz = 1575420032.0
        gpsL1.hasBasebandCn0DbHz = true
        gpsL1.basebandCn0DbHz = 30.0f
        assertEquals(
            "Status,0,25,0,1,10,1575420032,35.0,136.0,57.0,1,1,1,30.0",
            gpsL1.toLog(location.time, signalCount, signalIndex)
        )
    }

    @Test
    fun testOrientationToLog() {
        val currentTime = 1234L
        val timeAtBoot = 1000L

        assertEquals(
            "OrientationDeg,244,10000000,44444.44444,5555.5555,6666.66666",
            Orientation(10000000, doubleArrayOf(44444.44444, 5555.5555, 6666.66666)).toLog(
                currentTime,
                timeAtBoot
            )
        )
    }
}