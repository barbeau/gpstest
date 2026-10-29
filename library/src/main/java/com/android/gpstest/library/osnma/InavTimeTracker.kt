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

import kotlin.math.floor

/**
 * Works out the GST of each I/NAV page when the platform doesn't provide it, as with Android's
 * GnssNavigationMessage.
 *
 * Word types 0 (with time), 5 and 6 carry the GST at the start of their page. Each of those
 * pages gives a sample of the offset between the local clock and GST (mostly the decoding and
 * delivery latency). Other pages are timed with the median of recent offsets: pages start at odd
 * GST seconds, so the estimate is right as long as the delivery jitter of a page stays below
 * about 1 s.
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
    private val lastPerSatellite = HashMap<Int, Gst>()

    /** Number of time stamps that disagreed with the estimate (a measure of delivery jitter) */
    var corrections = 0
        private set

    /**
     * @param arrivalMillis a monotonic local clock reading taken when the page was received,
     * e.g. SystemClock.elapsedRealtime()
     */
    fun gstFor(svn: Int, page: InavPage, arrivalMillis: Long): Gst? {
        val exact = page.gstFromWord(weekNumber)
        if (exact != null) {
            if (page.wordType != 6) weekNumber = exact.wn
            val estimate = estimate(arrivalMillis)
            if (estimate != null && estimate != exact) corrections++
            samples.addLast(Sample(arrivalMillis - exact.totalSeconds * 1000, arrivalMillis))
            while (samples.size > maxSamples) samples.removeFirst()
            lastPerSatellite[svn] = exact
            return exact
        }
        var estimate = estimate(arrivalMillis) ?: return null
        // A satellite sends one page every 2 s: never go back in time or repeat a slot
        val last = lastPerSatellite[svn]
        if (last != null && estimate <= last && last.totalSeconds - estimate.totalSeconds < 10) {
            estimate = last.addSeconds(2)
        }
        lastPerSatellite[svn] = estimate
        return estimate
    }

    private fun estimate(arrivalMillis: Long): Gst? {
        while (samples.isNotEmpty() && arrivalMillis - samples.first().arrivalMillis > maxSampleAgeMillis) {
            samples.removeFirst()
        }
        if (samples.isEmpty()) return null
        val sorted = samples.map { it.offsetMillis }.sorted()
        val offset = sorted[sorted.size / 2]
        val gstMillis = arrivalMillis - offset
        // Nearest odd second
        val seconds = 2 * floor((gstMillis - 1000) / 2000.0 + 0.5).toLong() + 1
        if (seconds < 0) return null
        return Gst.fromTotalSeconds(seconds)
    }

    fun reset() {
        samples.clear()
        weekNumber = null
        lastPerSatellite.clear()
    }
}
