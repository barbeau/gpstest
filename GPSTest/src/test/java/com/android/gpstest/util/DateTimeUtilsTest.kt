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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.concurrent.TimeUnit

class DateTimeUtilsTest {

    @Test
    fun isTimeValid() {
        // Valid times within 5 days of now
        assertTrue(DateTimeUtils.isTimeValid(Instant.now().toEpochMilli()))
        assertTrue(DateTimeUtils.isTimeValid(Instant.now().toEpochMilli() + TimeUnit.DAYS.toMillis(4)))
        assertTrue(DateTimeUtils.isTimeValid(Instant.now().toEpochMilli() - TimeUnit.DAYS.toMillis(4)))

        // Invalid times more than 5 days  from now (past or future)
        assertFalse(DateTimeUtils.isTimeValid(Instant.now().toEpochMilli() + TimeUnit.DAYS.toMillis(6)))
        assertFalse(DateTimeUtils.isTimeValid(Instant.now().toEpochMilli() - TimeUnit.DAYS.toMillis(6)))
    }

    @Test
    fun isTimeValidLegacy() {
        // Valid times within 5 days of now
        assertTrue(DateTimeUtils.isTimeValidLegacy(Instant.now().toEpochMilli()))
        assertTrue(DateTimeUtils.isTimeValidLegacy(Instant.now().toEpochMilli() + TimeUnit.DAYS.toMillis(4)))
        assertTrue(DateTimeUtils.isTimeValidLegacy(Instant.now().toEpochMilli() - TimeUnit.DAYS.toMillis(4)))

        // Invalid times more than 5 days from now (past or future)
        assertFalse(DateTimeUtils.isTimeValidLegacy(Instant.now().toEpochMilli() + TimeUnit.DAYS.toMillis(6)))
        assertFalse(DateTimeUtils.isTimeValidLegacy(Instant.now().toEpochMilli() - TimeUnit.DAYS.toMillis(6)))
    }
}