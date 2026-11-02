/*
 * Copyright (C) 2026 The GPSTest contributors
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
package com.android.gpstest.library.osnma

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsnmaReportingTest {

    /** Runs the test vector and returns the final status */
    private fun authenticatedStatus(): OsnmaStatus {
        val engine =
            OsnmaEngine(Bits.fromHex(TestVectors.MERKLE_ROOT_1), listOf(TestVectors.publicKey1()))
        TestVectors.configuration1().forEach { engine.feedPage(it.svn, it.gst, it.page) }
        return engine.status()
    }

    @Test
    fun csvLog_oneRecordPerSatelliteAndSubframe() {
        val status = authenticatedStatus()
        val log = OsnmaCsvLog()
        val records = log.records(status, 1_700_000_000_000L)
        val withOsnma = status.satellites.count { it.transmitsOsnma }
        assertEquals(withOsnma, records.size)
        // Fields of the OSNMA format line in the CSV file header (see LOGGING.md)
        val fields = 15
        records.forEach { assertEquals(fields, it.split(",").size) }
        assertTrue(records.all { it.startsWith("OSNMA,1700000000000,") })
        assertTrue(records.any { it.split(",")[9] == "1" })
        // Same subframe: nothing new
        assertEquals(0, log.records(status, 1_700_000_001_000L).size)
        // Next subframe
        val next = status.copy(gst = status.gst!!.addSeconds(30))
        assertEquals(withOsnma, log.records(next, 1_700_000_030_000L).size)
    }

    @Test
    fun marks_failedTakesPrecedence() {
        val status = authenticatedStatus()
        val marks = status.marks()
        assertTrue(marks.isNotEmpty())
        assertTrue(marks.values.all { it == OsnmaMark.AUTHENTICATED })
        val svn = marks.keys.first()
        val failed = status.copy(satellites = status.satellites.map {
            if (it.svn == svn) it.copy(tagsFailed = 1, lastFailureGst = status.gst) else it
        })
        assertEquals(OsnmaMark.FAILED, failed.marks()[svn])
    }

    @Test
    fun failureMonitor_rateLimited() {
        val status = authenticatedStatus()
        val svn = status.satellites.first().svn
        fun withFailures(svnFailures: Map<Int, Int>) =
            status.copy(satellites = status.satellites.map {
                it.copy(tagsFailed = svnFailures[it.svn] ?: 0)
            })
        val monitor = OsnmaFailureMonitor(intervalMillis = 600_000)
        assertEquals(emptyList<Int>(), monitor.check(withFailures(emptyMap()), 0))
        assertEquals(listOf(svn), monitor.check(withFailures(mapOf(svn to 1)), 1000))
        // More failures within the interval: held back
        val other = status.satellites[1].svn
        val both = withFailures(mapOf(svn to 2, other to 1))
        assertEquals(emptyList<Int>(), monitor.check(both, 60_000))
        assertEquals(emptyList<Int>(), monitor.check(both, 120_000))
        // After the interval, the held back ones are reported
        assertEquals(listOf(svn, other).sorted(), monitor.check(both, 601_001))
        // No new failures: nothing
        assertEquals(emptyList<Int>(), monitor.check(both, 1_300_000))
        // Engine restarted (counts back to 0): no warning
        assertEquals(emptyList<Int>(), monitor.check(withFailures(emptyMap()), 1_400_000))
    }
}
