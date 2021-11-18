/*
 * Copyright 2021 Sean Barbeau
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
package com.android.gpstest.model

/**
 * Container class holding rotation sensor timestamp, and [values], where the first index is the
 * orientation (X) for display (which has magnetic correction applied if available as well as
 * rotation correction), the second is the tilt (Y), and the third is the yaw (Z)
 */
data class Orientation(val elapsedRealtimeNanos: Long, val values: DoubleArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Orientation

        if (elapsedRealtimeNanos != other.elapsedRealtimeNanos) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elapsedRealtimeNanos.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}