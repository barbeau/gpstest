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

/**
 * Replays the Galileo I/NAV `Nav` lines of a GPSTest or GnssLogger log
 * (Nav,Svid,Type,Status,MessageId,Sub-messageId,Data) through the same steps as the app: layout
 * detection, page assembly, page timing and the OSNMA engine.
 */
object NavLogReplay {

    /** How many messages later a repeat can come (other satellites' messages in between) */
    private const val MAX_REPEAT_DISTANCE = 4

    class Message(val svid: Int, val messageId: Int, val data: ByteArray)

    class Result(
        val messages: Int,
        val used: Int,
        val stamped: Int,
        val layout: InavLayoutDetector,
        val assembler: InavPageAssembler,
        val tracker: InavTimeTracker,
        val engine: OsnmaEngine
    )

    fun messages(resource: String): List<Message> {
        val text = NavLogReplay::class.java.getResourceAsStream(resource)!!
            .bufferedReader()
            .use { it.readText() }
        return text.lines().filter { it.startsWith("Nav,") }.map { line ->
            val f = line.split(',')
            val data = ByteArray(f.size - 6) { f[6 + it].toInt().toByte() }
            Message(f[1].toInt(), f[4].toInt(), data)
        }
    }

    /**
     * The logs have no arrival times. They're reconstructed from the pages that carry their own
     * time, interpolated over the order of the messages. A message that repeats the satellite's
     * previous one within a few messages gets its arrival time, as it was delivered together
     * with it.
     *
     * @param offsetBits position of the page in the data (see [InavLayoutDetector])
     */
    fun arrivalTimes(messages: List<Message>, offsetBits: Int): LongArray {
        val anchors = sortedMapOf<Int, Long>()
        var wn: Int? = null
        messages.forEachIndexed { i, m ->
            val page = InavPage.fromAndroid(m.data, offsetBits) ?: return@forEachIndexed
            if (!page.crcOk) return@forEachIndexed
            val gst = page.gstFromWord(wn) ?: return@forEachIndexed
            if (page.wordType != 6) wn = gst.wn
            // Delivered at the end of the page
            anchors[i] = (gst.totalSeconds + 2) * 1000
        }
        val keys = anchors.keys.toList()
        val times = LongArray(messages.size) { i ->
            val lo = keys.lastOrNull { it <= i } ?: keys.first()
            val hi = keys.firstOrNull { it >= i } ?: keys.last()
            val t = if (lo == hi) anchors[lo]!! else
                anchors[lo]!! + (anchors[hi]!! - anchors[lo]!!) * (i - lo) / (hi - lo)
            t + 80
        }
        val previous = HashMap<Int, Int>()
        messages.forEachIndexed { i, m ->
            val p = previous.put(m.svid, i)
            if (p != null && i - p <= MAX_REPEAT_DISTANCE &&
                messages[p].data.contentEquals(m.data)
            ) {
                times[i] = times[p]
            }
        }
        return times
    }

    fun replay(
        resource: String,
        offsetBits: Int,
        builtInKroots: List<BuiltInKroot> = emptyList()
    ): Result {
        val messages = messages(resource)
        val arrival = arrivalTimes(messages, offsetBits)
        val root = Bits.fromHex(GalileoKeys.MERKLE_ROOT)
        val engine = OsnmaEngine(root, GalileoKeys.builtInKeys(root), builtInKroots = builtInKroots)
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
        return Result(messages.size, used, stamped, layout, assembler, tracker, engine)
    }

    /** Pages that fail the CRC once the layout is known */
    fun crcFailures(resource: String): Pair<InavLayoutDetector, Int> {
        val layout = InavLayoutDetector()
        var failures = 0
        messages(resource).forEach { m ->
            val page = layout.read(m.data)!!
            if (layout.offsetBits != null && !page.crcOk) failures++
        }
        return layout to failures
    }
}
