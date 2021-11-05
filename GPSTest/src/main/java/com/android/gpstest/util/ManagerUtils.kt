/*
 * Copyright 2019-2021 Google LLC, Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.android.gpstest.model.CoordinateType

/**
 * Returns the `location` object as a human readable string for use in a notification title
 */
fun Location?.toNotificationTitle(): String {
    return if (this != null) {
        FormatUtils.formatLatOrLon(latitude, CoordinateType.LATITUDE) +
                "," +
                FormatUtils.formatLatOrLon(longitude, CoordinateType.LONGITUDE)
    } else {
        "Unknown location"
    }
}

/**
 * Returns the `location` object as a human readable string for use in a notification summary
 */
fun Location?.toNotificationSummary(): String {
    return if (this != null) {
        val lat = FormatUtils.formatLatOrLon(latitude, CoordinateType.LATITUDE)
        val lon = FormatUtils.formatLatOrLon(longitude, CoordinateType.LONGITUDE)
        val alt = FormatUtils.formatAltitude(this)
        "$lat $lon $alt"
    } else {
        "Unknown location"
    }
}

/**
 * Helper functions to simplify permission checks/requests.
 */
fun Context.hasPermission(permission: String): Boolean {

    // Background permissions didn't exit prior to Q, so it's approved by default.
    if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION &&
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        return true
    }

    return ActivityCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}