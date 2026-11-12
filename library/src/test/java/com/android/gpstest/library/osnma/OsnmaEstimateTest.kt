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

import com.android.gpstest.library.osnma.OsnmaEstimate.WaitingFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** The estimate of when OSNMA authenticates, replayed on the logs of three phones */
class OsnmaEstimateTest {

    /** One estimate per page: GST seconds, the estimate, and when it authenticated (or null) */
    private class Run(val estimates: List<Pair<Long, OsnmaEstimate?>>, val authenticatedAt: Long?)

    private fun run(log: String, offsetBits: Int, builtInKroots: List<BuiltInKroot>): Run {
        val estimator = OsnmaEstimator()
        val estimates = ArrayList<Pair<Long, OsnmaEstimate?>>()
        var authenticatedAt: Long? = null
        NavLogReplay.replay(log, offsetBits, builtInKroots) { engine ->
            val status = engine.status()
            val now = status.gst?.totalSeconds ?: return@replay
            val estimate = estimator.update(status, status.statistics.subframeCompleteness)
            estimates.add(now to estimate)
            if (authenticatedAt == null && status.phase == OsnmaStatus.Phase.AUTHENTICATING) {
                authenticatedAt = now
            }
        }
        return Run(estimates, authenticatedAt)
    }

    /** Pixel 10 Pro XL: the root key comes from the signal; the estimate tracks it */
    @Test
    fun rootKeyFromSignal_predictsAuthentication() {
        val r = run("/osnma/pixel10proxl_gal_inav.txt", 0, emptyList())
        val authenticated = r.authenticatedAt!!
        val start = r.estimates.first().first
        // From 3 minutes on, every predicted time is within 2 minutes of the real one
        val predictions = r.estimates
            .filter { (t, e) -> t >= start + 180 && t < authenticated && e?.seconds != null }
            .map { (t, e) -> t + e!!.seconds!! }
        assertTrue(predictions.isNotEmpty())
        predictions.forEach {
            assertTrue("predicted ${it - start}, was ${authenticated - start}", abs(it - authenticated) <= 120)
        }
        // While receiving the root key, the parts are reported
        val kroot = r.estimates.mapNotNull { it.second }.first { it.waitingFor == WaitingFor.KROOT && it.parts != null }
        assertNotNull(kroot.parts)
        // Authenticated: no estimate
        assertNull(r.estimates.first { it.first == authenticated }.second)
    }

    /** Pixel 11 Pro with the built-in root key: waiting for the first tags from the start */
    @Test
    fun builtInRootKey_waitsForTags() {
        val r = run("/osnma/pixel11pro_gal_inav.txt", 0, GalileoKeys.builtInKroots())
        val first = r.estimates.first().second!!
        assertEquals(WaitingFor.TAGS, first.waitingFor)
        assertEquals(OsnmaEstimator.TAGS_SECONDS.toLong(), first.seconds)
        assertTrue(r.authenticatedAt!! - r.estimates.first().first < 180)
    }

    /**
     * Pixel 8 Pro: so few pages that no rate can be measured for over 10 minutes, which is
     * overdue; once more parts arrive, there's an estimate again
     */
    @Test
    fun tooFewPages_overdueUntilPartsArrive() {
        val r = run("/osnma/pixel8pro_gal_inav.txt", 4, emptyList())
        assertNull(r.authenticatedAt)
        val start = r.estimates.first().first
        val estimates = r.estimates.mapNotNull { (t, e) -> e?.let { t - start to it } }
        assertTrue(estimates.all { it.second.waitingFor == WaitingFor.KROOT })
        // No number for the first 10 minutes: one part only
        assertTrue(estimates.filter { it.first < OsnmaEstimator.MAX_UNKNOWN_SECONDS }.all { it.second.seconds == null })
        // Then overdue, until more parts arrive
        val overdue = estimates.filter { it.second.overdue }
        assertTrue(overdue.isNotEmpty())
        assertTrue(overdue.all { it.first > OsnmaEstimator.MAX_UNKNOWN_SECONDS && it.second.seconds == null })
        val later = estimates.filter { it.first > overdue.last().first }
        assertTrue(later.isNotEmpty())
        assertTrue(later.all { !it.second.overdue && it.second.seconds != null })
    }

    /** Pixel 11 Pro without the built-in root key: stuck at 7 of 8 parts, which becomes overdue */
    @Test
    fun stuckRootKey_overdue() {
        val r = run("/osnma/pixel11pro_gal_inav.txt", 0, emptyList())
        val last = r.estimates.last().second!!
        assertEquals(WaitingFor.KROOT, last.waitingFor)
        assertEquals(7 to 8, last.parts)
        assertTrue(last.overdue)
    }

    @Test
    fun noStatus_noEstimate() {
        assertNull(OsnmaEstimator().update(null, null))
    }
}
