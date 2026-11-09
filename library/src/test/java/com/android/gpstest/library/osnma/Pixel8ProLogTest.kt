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

/**
 * Regression test with Galileo I/NAV messages recorded on a Pixel 8 Pro, which packs the page
 * 4 bits later than documented and delivers only a fraction of the pages (see OSNMA.md)
 */
class Pixel8ProLogTest {

    private val log = "/osnma/pixel8pro_gal_inav.txt"

    @Test
    fun shiftedLayout_detected() {
        val (layout, crcFailures) = NavLogReplay.crcFailures(log)
        assertEquals(4, layout.offsetBits)
        assertEquals(0, crcFailures)
    }

    @Test
    fun replay_noFalseResults() {
        val r = NavLogReplay.replay(log, offsetBits = 4)
        val status = r.engine.status()
        val stats = r.engine.statistics
        assertTrue("used ${r.used} of ${r.messages}", r.used > r.messages * 9 / 10)
        // Time stamps that disagreed with the estimate from the other pages
        assertTrue(
            "corrections ${r.tracker.corrections} of ${r.stamped}",
            r.tracker.corrections * 20 < r.stamped
        )
        assertEquals(0, r.assembler.repaired)
        assertEquals(0, r.assembler.repeated)
        assertEquals(0L, stats.crcErrors)
        // Operational OSNMA with the built-in key, TESLA root key still being collected
        assertEquals(NmaStatus.OPERATIONAL, status.nmaHeader?.nmaStatus)
        assertEquals(2, status.publicKey?.pkid)
        assertTrue(stats.osnmaPages > 300)
        assertTrue((status.krootProgress?.first ?: 0) > 0)
        // Nothing may fail on real data
        assertEquals(0, stats.krootFailed)
        assertEquals(0, stats.pkrFailed)
        assertEquals(0, stats.teslaKeysFailed)
        assertEquals(0, stats.mackFailed)
        assertEquals(0, stats.tagsFailed)

        // The DSM bytes collected are kept for the next session
        val saved = r.engine.exportDsm()
        assertTrue(saved.isNotEmpty())
        val root = Bits.fromHex(GalileoKeys.MERKLE_ROOT)
        val next = OsnmaEngine(root, GalileoKeys.builtInKeys(root), storedDsm = saved)
        assertEquals(saved, next.exportDsm())
    }
}
