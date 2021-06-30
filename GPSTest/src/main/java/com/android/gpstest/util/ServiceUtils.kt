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

import com.android.gpstest.model.Location


/**
 * Returns the `location` object as a human readable string.
 */
fun Location?.toText(): String {
    return if (this != null) {
        toString(latitude, longitude)
    } else {
        "Unknown location"
    }
}

/**
 * Returns the project model `location` object from an Android location object
 */
fun android.location.Location?.toLocation(): Location? {
    return if (this != null) {
        Location(
                time = time,
                latitude = latitude,
                longitude = longitude
        )
    } else {
        return null
    }
}

/**
 * Returns the `location` object as a human readable string.
 */
fun android.location.Location?.toText(): String {
    return if (this != null) {
        toString(latitude, longitude)
    } else {
        "Unknown location"
    }
}

fun toString(lat: Double, lon: Double): String {
    return "($lat, $lon)"
}
