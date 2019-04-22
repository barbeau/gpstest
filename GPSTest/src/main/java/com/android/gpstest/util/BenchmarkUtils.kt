package com.android.gpstest.util

import android.location.Location

import com.android.gpstest.model.MeasuredError

/**
 * Utilities for comparing two locations to measure error
 */
class BenchmarkUtils {
    companion object {
        /**
         * Returns the error between the provided [location] and [groundTruth] location
         */
        fun measureError(location: Location, groundTruth: Location): MeasuredError {
            val horError = location.distanceTo(groundTruth)
            return if (groundTruth.hasAltitude() && location.hasAltitude()) {
                MeasuredError(horError, groundTruth.altitude - location.altitude)
            } else {
                // Just horizontal error
                MeasuredError(horError)
            }
        }
    }
}
