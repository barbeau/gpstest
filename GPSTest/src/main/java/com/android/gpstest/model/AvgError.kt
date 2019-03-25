/*
 * Copyright (C) 2018-2019 Sean J. Barbeau
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
package com.android.gpstest.model

/**
 * Model class for holding average error for many error measurements
 */
data class AvgError(var avgError: Float = 0F,
                    var avgVertError: Double = Double.NaN,
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