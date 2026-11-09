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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test with Galileo I/NAV messages recorded with GnssLogger on a Pixel 11 Pro
 * (MediaTek chipset). The page layout is the documented one and the message ID is the subframe
 * number, but every page is delivered twice in a row, and the last page of each subframe carries
 * the number of the next subframe (see OSNMA.md).
 */
class Pixel11ProLogTest {

    private val log = "/osnma/pixel11pro_gal_inav.txt"

    @Test
    fun documentedLayout_detected() {
        val (layout, crcFailures) = NavLogReplay.crcFailures(log)
        assertEquals(0, layout.offsetBits)
        assertEquals(0, crcFailures)
    }

    @Test
    fun repeatedPages_droppedAndSubframeNumbersUsed() {
        val r = NavLogReplay.replay(log, offsetBits = 0)
        assertEquals(813L, r.assembler.repeated)
        assertEquals(0, r.assembler.repaired)
        assertTrue(r.tracker.usingSubframeNumbers)
        val distinct = r.messages - r.assembler.repeated
        assertTrue("used ${r.used} of $distinct", r.used > distinct * 9 / 10)
        assertTrue(
            "corrections ${r.tracker.corrections} of ${r.stamped}",
            r.tracker.corrections * 20 < r.stamped
        )
    }

    /** With the built-in TESLA root key, the pages of this log authenticate 4 satellites */
    @Test
    fun replay_builtInKroot_authenticates() {
        val r = NavLogReplay.replay(log, offsetBits = 0, GalileoKeys.builtInKroots())
        val status = r.engine.status()
        val stats = r.engine.statistics
        assertEquals(OsnmaStatus.Phase.AUTHENTICATING, status.phase)
        assertEquals(1, status.chainInForce)
        assertTrue("subframes ${stats.subframes}", stats.subframes >= 30)
        assertTrue("TESLA keys ${stats.teslaKeysVerified}", stats.teslaKeysVerified >= 30)
        assertTrue("tags ${stats.tagsOk}", stats.tagsOk >= 100)
        val authenticated = status.satellites.filter { it.cedAuthenticated }.map { it.svn }
        assertEquals(listOf(2, 7, 34, 36), authenticated.sorted())
        assertNoFailures(stats)
    }

    /** Without it, the TESLA root key from the signal is collected and nothing fails */
    @Test
    fun replay_fromSignal_noFalseResults() {
        val r = NavLogReplay.replay(log, offsetBits = 0)
        val status = r.engine.status()
        val stats = r.engine.statistics
        assertEquals(NmaStatus.OPERATIONAL, status.nmaHeader?.nmaStatus)
        assertEquals(2, status.publicKey?.pkid)
        assertTrue((status.krootProgress?.first ?: 0) >= 5)
        assertFalse(status.satellites.any { it.cedAuthenticated })
        assertNoFailures(stats)
    }

    private fun assertNoFailures(stats: OsnmaStatistics) {
        assertEquals(0L, stats.crcErrors)
        assertEquals(0, stats.krootFailed)
        assertEquals(0, stats.pkrFailed)
        assertEquals(0, stats.teslaKeysFailed)
        assertEquals(0, stats.mackFailed)
        assertEquals(0, stats.tagsFailed)
    }
}
