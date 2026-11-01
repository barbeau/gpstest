/*
 * Copyright (C) 2026 Sean J. Barbeau
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

import com.android.gpstest.library.util.IOUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class IOUtilsTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun testFormatLocationTimestamp() {
        val timeMillis = 1587042000000L // 2020-04-16 13:00:00 UTC

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("2020-04-16 13:00:00 UTC", IOUtils.formatLocationTimestamp(timeMillis))

        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        assertEquals("2020-04-16 09:00:00 EDT", IOUtils.formatLocationTimestamp(timeMillis))
    }
}
