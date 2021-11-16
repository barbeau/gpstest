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

import android.location.Location
import android.os.Build
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.android.gpstest.util.FormatUtils.toLog
import com.android.gpstest.util.SatelliteUtil.isBearingAccuracySupported
import com.android.gpstest.util.SatelliteUtil.isSpeedAccuracySupported
import com.android.gpstest.util.SatelliteUtil.isVerticalAccuracySupported
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

        // Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters
        if (l.isSpeedAccuracySupported() && l.isBearingAccuracySupported() && l.isVerticalAccuracySupported()) {
            assertEquals(
                "Fix,test,45.34567899,12.45678901,56.2,19.2,98.7,100.1,12345,382.7,284.1,123456789,583.4",
                l.toLog()
            )
        } else {
            assertEquals(
                "Fix,test,45.34567899,12.45678901,56.2,19.2,98.7,100.1,12345,,,123456789,",
                l.toLog()
            )
        }
    }
}