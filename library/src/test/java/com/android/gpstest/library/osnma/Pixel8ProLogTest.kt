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

    private class Message(val svid: Int, val messageId: Int, val data: ByteArray)

    /** The `Nav` lines of the GPSTest log: Nav,Svid,Type,Status,MessageId,Sub-messageId,Data */
    private fun messages(): List<Message> {
        val text = Pixel8ProLogTest::class.java
            .getResourceAsStream("/osnma/pixel8pro_gal_inav.txt")!!
            .bufferedReader()
            .use { it.readText() }
        return text.lines().filter { it.startsWith("Nav,") }.map { line ->
            val f = line.split(',')
            val data = ByteArray(f.size - 6) { f[6 + it].toInt().toByte() }
            Message(f[1].toInt(), f[4].toInt(), data)
        }
    }

    /**
     * The log has no arrival times. They're reconstructed from the pages that carry their own
     * time, interpolated over the order of the messages.
     */
    private fun arrivalTimes(messages: List<Message>): LongArray {
        val anchors = sortedMapOf<Int, Long>()
        var wn: Int? = null
        messages.forEachIndexed { i, m ->
            val page = InavPage.fromAndroid(m.data, 4) ?: return@forEachIndexed
            if (!page.crcOk) return@forEachIndexed
            val gst = page.gstFromWord(wn) ?: return@forEachIndexed
            if (page.wordType != 6) wn = gst.wn
            // Delivered at the end of the page
            anchors[i] = (gst.totalSeconds + 2) * 1000
        }
        val keys = anchors.keys.toList()
        return LongArray(messages.size) { i ->
            val lo = keys.lastOrNull { it <= i } ?: keys.first()
            val hi = keys.firstOrNull { it >= i } ?: keys.last()
            val t = if (lo == hi) anchors[lo]!! else
                anchors[lo]!! + (anchors[hi]!! - anchors[lo]!!) * (i - lo) / (hi - lo)
            t + 80
        }
    }

    @Test
    fun shiftedLayout_detected() {
        val layout = InavLayoutDetector()
        var crcFailures = 0
        messages().forEach { m ->
            val page = layout.read(m.data)!!
            if (layout.offsetBits != null && !page.crcOk) crcFailures++
        }
        assertEquals(4, layout.offsetBits)
        assertEquals(0, crcFailures)
    }

    @Test
    fun replay_noFalseResults() {
        val messages = messages()
        val arrival = arrivalTimes(messages)
        val root = Bits.fromHex(GalileoKeys.MERKLE_ROOT)
        val engine = OsnmaEngine(root, GalileoKeys.builtInKeys(root))
        val layout = InavLayoutDetector()
        val assembler = InavPageAssembler()
        val tracker = InavTimeTracker()
        var used = 0
        var stamped = 0
        messages.forEachIndexed { i, m ->
            val page = layout.read(m.data) ?: return@forEachIndexed
            val assembled = assembler.assemble(m.svid, page, arrival[i]) ?: return@forEachIndexed
            if (!assembled.page.crcOk) return@forEachIndexed
            val gst = tracker.gstFor(m.svid, assembled.page, assembled.arrivalMillis, m.messageId)
                ?: return@forEachIndexed
            if (tracker.lastWasExact) stamped++
            engine.feedPage(m.svid, gst, assembled.page, timeCertain = tracker.lastTimeCertain)
            used++
        }
        val status = engine.status()
        val stats = engine.statistics
        assertTrue("used $used of ${messages.size}", used > messages.size * 9 / 10)
        // Time stamps that disagreed with the estimate from the other pages
        assertTrue(
            "corrections ${tracker.corrections} of $stamped",
            tracker.corrections * 20 < stamped
        )
        assertEquals(0, assembler.repaired)
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
        val saved = engine.exportDsm()
        assertTrue(saved.isNotEmpty())
        val next = OsnmaEngine(root, GalileoKeys.builtInKeys(root), storedDsm = saved)
        assertEquals(saved, next.exportDsm())
    }
}
