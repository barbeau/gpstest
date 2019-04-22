package com.android.gpstest.model

import kotlin.math.abs

/**
 * Model class for holding average error for many error measurements
 */
data class AvgError(var avgError: Float = 0F,
                    var avgVertError: Double = Double.NaN,
                    var avgVertAbsError: Double = Double.NaN,
                    var count: Int = 0) {

    var errorRunningSum: Float = 0F
    var vertErrorRunningSum: Double = 0.0
    var vertErrorAbsRunningSum: Double = 0.0

    /**
     * Adds a [measuredError] to the current running average
     */
    @Synchronized fun addMeasurement(measuredError: MeasuredError) {
        count++
        errorRunningSum += measuredError.error
        avgError = errorRunningSum / count
        vertErrorRunningSum += measuredError.vertError
        vertErrorAbsRunningSum += abs(measuredError.vertError)
        avgVertError = vertErrorRunningSum / count
        avgVertAbsError = vertErrorAbsRunningSum / count
    }

    /**
     * Resets all averages and counts to 0
     */
    @Synchronized fun reset() {
        count = 0
        avgError = 0F
        avgVertError = 0.0
        avgVertAbsError = 0.0
        errorRunningSum = 0F
        vertErrorRunningSum = 0.0
        vertErrorAbsRunningSum = 0.0
    }
}