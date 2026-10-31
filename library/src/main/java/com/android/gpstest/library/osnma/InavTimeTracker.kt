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

import kotlin.math.abs
import kotlin.math.floor

/**
 * Works out the GST of each I/NAV page when the platform doesn't provide it, as with Android's
 * GnssNavigationMessage.
 *
 * 1. Word types 0 (with time), 5 and 6 carry the GST at the start of their page. Each gives a
 *    sample of the offset between the local clock and GST (mostly decoding and delivery latency).
 * 2. Other pages get a rough estimate from the arrival time and the median offset.
 * 3. The estimate is then snapped to the page slot implied by the word type: the E1-B nominal
 *    subframe has a fixed word sequence (OS SIS ICD 4.3.2). When the platform reports the
 *    subframe number within the 720 s frame (getMessageId, 1-24) and it agrees with the time
 *    stamps, the slot is located within the frame instead of the subframe. This tolerates
 *    delivery jitter of several seconds (up to ±14 s for most word types).
 *
 * Until the first time stamp has been received, [gstFor] returns null and the page should be
 * skipped (this takes at most one subframe, 30 s).
 */
class InavTimeTracker(
    /** Offset samples older than this are discarded */
    private val maxSampleAgeMillis: Long = 10 * 60 * 1000L,
    private val maxSamples: Int = 31
) {
    private class Sample(val offsetMillis: Long, val arrivalMillis: Long)

    private val samples = ArrayDeque<Sample>()
    private var weekNumber: Int? = null

    // Consistency of the platform's subframe numbers with the time stamps in the data
    private var subframeNumberAgree = 0
    private var subframeNumberDisagree = 0

    /** True if the last page returned by [gstFor] carried its own time stamp */
    var lastWasExact = false
        private set

    /** Number of time stamps that disagreed with the estimate (a measure of timing problems) */
    var corrections = 0
        private set

    /** Pages whose estimate was moved to a different slot by the word type check */
    var snapped = 0
        private set

    /** The platform's subframe numbers have been confirmed against the time stamps */
    val usingSubframeNumbers: Boolean get() = subframeNumbersTrusted()

    /**
     * @param arrivalMillis a monotonic local clock reading taken when the page was received,
     * e.g. SystemClock.elapsedRealtime()
     * @param subframeNumber subframe number within the frame as reported by the platform (1-24),
     * or any other value if unknown
     */
    fun gstFor(
        svn: Int,
        page: InavPage,
        arrivalMillis: Long,
        subframeNumber: Int = -1
    ): Gst? {
        val exact = page.gstFromWord(weekNumber)
        if (exact != null) {
            lastWasExact = true
            if (page.wordType != 6) weekNumber = exact.wn
            checkSubframeNumber(exact, subframeNumber)
            val estimate = estimate(page, arrivalMillis, subframeNumber)
            if (estimate != null && estimate != exact) corrections++
            samples.addLast(Sample(arrivalMillis - exact.totalSeconds * 1000, arrivalMillis))
            while (samples.size > maxSamples) samples.removeFirst()
            return exact
        }
        lastWasExact = false
        return estimate(page, arrivalMillis, subframeNumber)
    }

    private fun checkSubframeNumber(exact: Gst, subframeNumber: Int) {
        if (subframeNumber in 1..SUBFRAMES_PER_FRAME) {
            if (subframeOf(exact.totalSeconds) == subframeNumber - 1) subframeNumberAgree++ else subframeNumberDisagree++
        }
    }

    private fun subframeNumbersTrusted() =
        subframeNumberAgree >= 3 && subframeNumberDisagree * 10 <= subframeNumberAgree

    private fun estimate(page: InavPage, arrivalMillis: Long, subframeNumber: Int): Gst? {
        while (samples.isNotEmpty() && arrivalMillis - samples.first().arrivalMillis > maxSampleAgeMillis) {
            samples.removeFirst()
        }
        if (samples.isEmpty()) return null
        val sorted = samples.map { it.offsetMillis }.sorted()
        val offset = sorted[sorted.size / 2]
        val roughMillis = arrivalMillis - offset
        // Nearest page start (odd second)
        val rough = 2 * floor((roughMillis - 1000) / 2000.0 + 0.5).toLong() + 1

        val slots = WORD_TYPE_SLOTS[page.wordType]
        val useSubframe = subframeNumber in 1..SUBFRAMES_PER_FRAME && subframeNumbersTrusted()
        val snappedSeconds = when {
            slots == null -> rough
            useSubframe -> slots.map {
                nearestWithPosition(rough, (subframeNumber - 1) * PAGES_PER_SUBFRAME + it, FRAME_SECONDS)
            }.minByOrNull { abs(it - rough) }!!
            else -> slots.map { nearestWithPosition(rough, it, SUBFRAME_SECONDS) }.minByOrNull { abs(it - rough) }!!
        }
        if (snappedSeconds != rough) snapped++
        if (snappedSeconds < 0) return null
        return Gst.fromTotalSeconds(snappedSeconds)
    }

    fun reset() {
        samples.clear()
        weekNumber = null
        subframeNumberAgree = 0
        subframeNumberDisagree = 0
    }

    companion object {
        private const val PAGES_PER_SUBFRAME = 15
        private const val SUBFRAMES_PER_FRAME = 24
        private const val SUBFRAME_SECONDS = 30L
        private const val FRAME_SECONDS = 720L

        /** Page slot (0-14) of the page starting at [gstSeconds] within its subframe */
        fun slotOf(gstSeconds: Long): Int = ((gstSeconds / 2) % PAGES_PER_SUBFRAME).toInt()

        /** Subframe (0-23) of [gstSeconds] within its 720 s frame */
        fun subframeOf(gstSeconds: Long): Int = ((gstSeconds % FRAME_SECONDS) / SUBFRAME_SECONDS).toInt()

        /**
         * The page start time nearest to [rough] whose position (in pages) within a period of
         * [periodSeconds] is [position]
         */
        private fun nearestWithPosition(rough: Long, position: Int, periodSeconds: Long): Long {
            val periodStart = Math.floorDiv(rough, periodSeconds) * periodSeconds
            val candidate = periodStart + 2L * position + 1
            return listOf(candidate - periodSeconds, candidate, candidate + periodSeconds)
                .minByOrNull { abs(it - rough) }!!
        }

        /**
         * Page slots of each word type in the E1-B nominal subframe (OS SIS ICD Table 42, checked
         * against the GSC test vectors and a receiver recording). Word types 0 and 16 fill
         * several slots; word types without an entry (e.g. 63, dummy) aren't snapped.
         */
        private val WORD_TYPE_SLOTS: Map<Int, List<Int>> = mapOf(
            2 to listOf(0), 4 to listOf(1), 6 to listOf(2),
            7 to listOf(3), 9 to listOf(3), 8 to listOf(4), 10 to listOf(4),
            17 to listOf(5), 18 to listOf(5), 19 to listOf(6), 20 to listOf(6),
            16 to listOf(7, 14), 0 to listOf(5, 6, 7, 8, 9, 13, 14),
            1 to listOf(10), 3 to listOf(11), 5 to listOf(7, 12, 14)
        )
    }
}
