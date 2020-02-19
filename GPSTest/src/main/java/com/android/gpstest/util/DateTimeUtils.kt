/*
 * Copyright (C) 2020 Sean J. Barbeau
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

package com.android.gpstest.util

import android.os.Build
import androidx.annotation.VisibleForTesting
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Utilities for comparing two locations to measure error
 */
class DateTimeUtils {

    companion object {
        val NUM_DAYS_TIME_VALID = 5
        /**
         * Returns true if the provided UTC time of the fix, in milliseconds since January 1, 1970,
         * is valid, and false if it is not
         */
        fun isTimeValid(time: Long): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // If the GPS time is less than five days different than system clock time, consider it valid
                Duration.between(Instant.ofEpochMilli(time), Instant.now()).toDays() < NUM_DAYS_TIME_VALID
            } else {
                isTimeValidLegacy(time)
            }
        }

        @VisibleForTesting
        internal fun isTimeValidLegacy(time: Long): Boolean {
            return TimeUnit.MILLISECONDS.toDays(Math.abs(System.currentTimeMillis() - time)) < NUM_DAYS_TIME_VALID
        }
    }
}