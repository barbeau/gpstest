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
 * 2. Other pages get a rough estimate from the arrival time: with the receiver clock when the
 *    platform provides it (GnssClock, see [updateReceiverClock]) and the typical delivery delay,
 *    otherwise with the median offset of the time stamps.
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

    /** Start of the last page of each satellite whose time was certain */
    private val lastCertain = HashMap<Int, Long>()

    // Receiver clock: local monotonic time minus GST, both in ms, and when it was measured
    private var receiverOffsetMillis: Long? = null
    private var receiverOffsetAt = 0L

    // Delay between the end of a page and its delivery, from pages with time stamps
    private val delaySamples = ArrayDeque<Long>()

    /** True if the receiver clock is being used for page timing */
    val usingReceiverClock: Boolean get() = receiverOffsetMillis != null

    /**
     * Provides the receiver's GNSS time: [gstMillis] (GST in ms since the GST epoch) at the local
     * monotonic time [elapsedMillis]. Android gives both in GnssClock (FullBiasNanos and
     * ElapsedRealtimeNanos).
     */
    fun updateReceiverClock(elapsedMillis: Long, gstMillis: Long) {
        receiverOffsetMillis = elapsedMillis - gstMillis
        receiverOffsetAt = elapsedMillis
    }

    private fun receiverOffset(arrivalMillis: Long): Long? =
        receiverOffsetMillis?.takeIf {
            abs(arrivalMillis - receiverOffsetAt) <= MAX_RECEIVER_CLOCK_AGE_MILLIS
        }

    // Consistency of the platform's subframe numbers with the time stamps in the data
    private var subframeNumberAgree = 0
    private var subframeNumberDisagree = 0

    /** True if the last page returned by [gstFor] carried its own time stamp */
    var lastWasExact = false
        private set

    /**
     * False if the time of the last page could be off by a page: its word type occupies several
     * slots and the estimate fell close to the middle between two of them, or it has no known slot
     */
    var lastTimeCertain = false
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
        val wn = weekNumber ?: receiverOffset(arrivalMillis)?.let {
            ((arrivalMillis - it) / 1000 / Gst.SECS_IN_WEEK).toInt()
        }
        val exact = page.gstFromWord(wn)
        if (exact != null) {
            if (page.wordType != 6) weekNumber = exact.wn
            checkSubframeNumber(exact, subframeNumber)
            val estimate = estimate(svn, page, arrivalMillis, subframeNumber)
            if (estimate != null && estimate != exact) corrections++
            // Set after estimate(), which updates them
            lastWasExact = true
            lastTimeCertain = true
            lastCertain[svn] = exact.totalSeconds
            samples.addLast(Sample(arrivalMillis - exact.totalSeconds * 1000, arrivalMillis))
            while (samples.size > maxSamples) samples.removeFirst()
            receiverOffset(arrivalMillis)?.let { offset ->
                val delay = arrivalMillis - offset - (exact.totalSeconds * 1000 + 2000)
                if (delay in -2000..10_000) {
                    delaySamples.addLast(delay)
                    while (delaySamples.size > maxSamples) delaySamples.removeFirst()
                }
            }
            return exact
        }
        lastWasExact = false
        val gst = estimate(svn, page, arrivalMillis, subframeNumber)
        if (gst != null && lastTimeCertain) lastCertain[svn] = gst.totalSeconds
        return gst
    }

    private fun checkSubframeNumber(exact: Gst, subframeNumber: Int) {
        if (subframeNumber in 1..SUBFRAMES_PER_FRAME) {
            if (subframeOf(exact.totalSeconds) == subframeNumber - 1) {
                subframeNumberAgree++
            } else {
                subframeNumberDisagree++
            }
        }
    }

    private fun subframeNumbersTrusted() =
        subframeNumberAgree >= 3 && subframeNumberDisagree * 10 <= subframeNumberAgree

    private fun estimate(svn: Int, page: InavPage, arrivalMillis: Long, subframeNumber: Int): Gst? {
        while (samples.isNotEmpty() &&
            arrivalMillis - samples.first().arrivalMillis > maxSampleAgeMillis
        ) {
            samples.removeFirst()
        }
        val receiver = receiverOffset(arrivalMillis)
        val roughMillis = if (receiver != null) {
            // Page start = delivery time in GST - delivery delay - 2 s page duration
            val delay = if (delaySamples.isEmpty()) {
                DEFAULT_DELAY_MILLIS
            } else {
                delaySamples.sorted()[delaySamples.size / 2]
            }
            arrivalMillis - receiver - delay - 2000
        } else {
            if (samples.isEmpty()) return null
            val sorted = samples.map { it.offsetMillis }.sorted()
            arrivalMillis - sorted[sorted.size / 2]
        }
        // Nearest page start (odd second)
        val rough = 2 * floor((roughMillis - 1000) / 2000.0 + 0.5).toLong() + 1

        val slots = WORD_TYPE_SLOTS[page.wordType]
        val useSubframe = subframeNumber in 1..SUBFRAMES_PER_FRAME && subframeNumbersTrusted()
        val candidates = when {
            slots == null -> emptyList()
            useSubframe -> slots.flatMap {
                val position = (subframeNumber - 1) * PAGES_PER_SUBFRAME + it
                nearestCandidates(rough, position, FRAME_SECONDS)
            }
            else -> slots.flatMap { nearestCandidates(rough, it, SUBFRAME_SECONDS) }
        }.distinct().sortedBy { abs(it - rough) }
        // A satellite sends a page every 2 s: if its previous page's time is known and a candidate
        // follows it directly, that's the one (resolves word types in adjacent slots)
        val follower = lastCertain[svn]?.plus(2)
            ?.takeIf { it in candidates && abs(it - rough) <= MAX_FOLLOW_SECONDS }
        val snappedSeconds = follower ?: candidates.firstOrNull() ?: rough
        // Certain if the next possible slot is clearly further away than the chosen one
        val margin = if (candidates.size >= 2) {
            abs(candidates[1] - rough) - abs(candidates[0] - rough)
        } else {
            Long.MAX_VALUE
        }
        lastTimeCertain = follower != null ||
                (candidates.isNotEmpty() && margin >= MIN_MARGIN_SECONDS)
        if (snappedSeconds != rough) snapped++
        if (snappedSeconds < 0) return null
        return Gst.fromTotalSeconds(snappedSeconds)
    }

    fun reset() {
        samples.clear()
        weekNumber = null
        lastCertain.clear()
        delaySamples.clear()
        receiverOffsetMillis = null
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
        fun subframeOf(gstSeconds: Long): Int =
            ((gstSeconds % FRAME_SECONDS) / SUBFRAME_SECONDS).toInt()

        /** Receiver clock readings older than this aren't used */
        private const val MAX_RECEIVER_CLOCK_AGE_MILLIS = 120_000L

        /** Typical delay between the end of a page and its delivery, until measured */
        private const val DEFAULT_DELAY_MILLIS = 500L

        /** Required difference between the best and second best slot for a certain time */
        private const val MIN_MARGIN_SECONDS = 4L

        /** How far from the estimate the page following a satellite's previous page may be */
        private const val MAX_FOLLOW_SECONDS = 5L

        /**
         * Page start times around [rough] whose position (in pages) within a period of
         * [periodSeconds] is [position]
         */
        private fun nearestCandidates(rough: Long, position: Int, periodSeconds: Long): List<Long> {
            val periodStart = Math.floorDiv(rough, periodSeconds) * periodSeconds
            val candidate = periodStart + 2L * position + 1
            return listOf(candidate - periodSeconds, candidate, candidate + periodSeconds)
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
