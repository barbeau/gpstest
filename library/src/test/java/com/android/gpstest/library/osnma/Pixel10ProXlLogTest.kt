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
 * Regression test with Galileo I/NAV messages recorded with GnssLogger on a Pixel 10 Pro XL
 * (Samsung chipset). The page layout is the documented one and the pages arrive once, but the
 * message ID isn't the subframe number, so it must not be used for page timing (see OSNMA.md and
 * https://issuetracker.google.com/issues/583449762).
 */
class Pixel10ProXlLogTest {

    private val log = "/osnma/pixel10proxl_gal_inav.txt"

    @Test
    fun documentedLayout_detected() {
        val (layout, crcFailures) = NavLogReplay.crcFailures(log)
        assertEquals(0, layout.offsetBits)
        assertEquals(0, crcFailures)
    }

    @Test
    fun messageIds_notUsedAsSubframeNumbers() {
        val r = NavLogReplay.replay(log, offsetBits = 0)
        assertFalse(r.tracker.usingSubframeNumbers)
        assertEquals(0L, r.assembler.repeated)
        assertEquals(0, r.assembler.repaired)
        assertTrue("used ${r.used} of ${r.messages}", r.used > r.messages * 9 / 10)
        assertTrue(
            "corrections ${r.tracker.corrections} of ${r.stamped}",
            r.tracker.corrections * 20 < r.stamped
        )
    }

    /** The TESLA root key is verified from the signal, then 5 satellites authenticate */
    @Test
    fun replay_fromSignal_authenticates() {
        val r = NavLogReplay.replay(log, offsetBits = 0)
        val status = r.engine.status()
        val stats = r.engine.statistics
        assertEquals(OsnmaStatus.Phase.AUTHENTICATING, status.phase)
        assertEquals(NmaStatus.OPERATIONAL, status.nmaHeader?.nmaStatus)
        assertEquals(2, status.publicKey?.pkid)
        assertEquals(1, status.chainInForce)
        assertEquals(1, stats.krootVerified)
        assertTrue("subframes ${stats.subframes}", stats.subframes >= 60)
        assertTrue("TESLA keys ${stats.teslaKeysVerified}", stats.teslaKeysVerified >= 60)
        assertTrue("tags ${stats.tagsOk}", stats.tagsOk >= 200)
        val authenticated = status.satellites.filter { it.cedAuthenticated }.map { it.svn }
        assertEquals(listOf(3, 25, 30, 32, 36), authenticated.sorted())
        assertEquals(0L, stats.crcErrors)
        assertEquals(0, stats.krootFailed)
        assertEquals(0, stats.pkrFailed)
        assertEquals(0, stats.teslaKeysFailed)
        assertEquals(0, stats.mackFailed)
        assertEquals(0, stats.tagsFailed)
    }
}
