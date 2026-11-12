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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The root key parts and the page grid shown on the OSNMA card, on a real log */
class OsnmaProgressViewTest {

    private val log = "/osnma/pixel11pro_gal_inav.txt"

    @Test
    fun rootKeyParts_matchProgress() {
        var sawUnknownTotal = false
        var sawKnownTotal = false
        var sawPartial = false
        NavLogReplay.replay(log, offsetBits = 0) { engine ->
            val status = engine.status()
            val blocks = status.krootBlocks ?: return@replay
            val progress = status.krootProgress!!
            val (fill, known) = blocks
            // Full parts are the ones the progress counts
            assertEquals(progress.first, fill.count { it >= 1f })
            assertTrue(fill.all { it in 0f..1f })
            if (known) {
                sawKnownTotal = true
                assertEquals(progress.second, fill.size)
            } else {
                sawUnknownTotal = true
                assertEquals(0, progress.second)
                assertTrue(fill.size >= OsnmaEstimator.EXPECTED_KROOT_PARTS)
            }
            if (fill.any { it > 0f && it < 1f }) sawPartial = true
        }
        assertTrue(sawUnknownTotal)
        assertTrue(sawKnownTotal)
        // Parts are collected byte by byte, so some are partly received at times
        assertTrue(sawPartial)
    }

    @Test
    fun pageGrid() {
        var views = 0
        var sawBeforeTracking = false
        NavLogReplay.replay(log, offsetBits = 0) { engine ->
            val status = engine.status()
            val view = status.pages
            if (status.gst == null) {
                assertNull(view)
                return@replay
            }
            view!!
            views++
            assertEquals(status.gst!!.subframe(), view.subframe)
            assertTrue(view.currentPage in 0 until SubframeLayout.PAGES)
            assertEquals(view.satellites.map { it.svn }.sorted(), view.satellites.map { it.svn })
            view.satellites.forEach { sat ->
                assertTrue(sat.trackedFrom in 0..SubframeLayout.PAGES)
                // Nothing received before tracking started
                for (page in 0 until minOf(sat.trackedFrom, SubframeLayout.PAGES)) {
                    assertFalse(sat.received(page))
                }
                if (sat.trackedFrom in 1 until SubframeLayout.PAGES) sawBeforeTracking = true
            }
        }
        assertTrue(views > 1000)
        // The log starts in the middle of a subframe
        assertTrue(sawBeforeTracking)
    }
}
