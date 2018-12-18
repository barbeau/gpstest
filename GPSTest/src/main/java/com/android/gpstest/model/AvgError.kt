package com.android.gpstest.model

/**
 * Model class for holding average error for many error measurements
 */
data class AvgError(var avgError: Float = 0F,
                    var avgVertError: Double = 0.0,
                    var count: Int = 0) {

    var errorRunningSum: Float = 0F
    var vertErrorRunningSum: Double = 0.0

    /**
     * Adds a [measuredError] to the current running average
     */
    @Synchronized fun addMeasurement(measuredError: MeasuredError) {
        count++
        errorRunningSum += measuredError.error
        avgError = errorRunningSum / count
        vertErrorRunningSum += measuredError.vertError
        avgVertError = vertErrorRunningSum / count
    }

    /**
     * Resets all averages and counts to 0
     */
    @Synchronized fun reset() {
        count = 0
        avgError = 0F
        avgVertError = 0.0
        errorRunningSum = 0F
        vertErrorRunningSum = 0.0
    }
}