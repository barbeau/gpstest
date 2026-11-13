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
 * What OSNMA is waiting for, and roughly how long it will take until the first satellite is
 * authenticated.
 *
 * @param seconds the expected time in seconds, or null if there isn't enough data for a number
 * yet, none can be given (public key), or it's [overdue]
 * @param parts for [WaitingFor.KROOT], the parts of the TESLA root key received and needed (the
 * total is 0 while not known yet)
 * @param overdue it already takes much longer than expected, usually because pages are missing
 */
data class OsnmaEstimate(
    val waitingFor: WaitingFor,
    val seconds: Long?,
    val parts: Pair<Int, Int>? = null,
    val overdue: Boolean = false
) {
    enum class WaitingFor {
        /** No public key: a new one is only broadcast every 6 hours */
        PUBLIC_KEY,

        /** The TESLA root key (DSM-KROOT) is being received */
        KROOT,

        /** Keys are there, the first tags are being collected and checked */
        TAGS
    }
}

/**
 * Estimates when OSNMA will authenticate the first satellite, from what the engine is waiting for
 * and how fast data arrives. All times are Galileo System Time from the signal, so the estimate
 * doesn't depend on when the platform delivers the pages.
 *
 * - TESLA root key: the missing parts divided by the rate at which parts arrived so far (once two
 *   have arrived, measured up to now, so the estimate grows while none arrive), plus
 *   [AFTER_KROOT_SECONDS]: the tags and keys collected meanwhile are checked as soon as the root
 *   key is there. Until the number of parts is known, the current Galileo configuration's
 *   [EXPECTED_KROOT_PARTS] is assumed. If no part arrived for much longer than usual, it's
 *   [OsnmaEstimate.overdue].
 * - First tags: a tag can only be checked with the TESLA key sent one subframe later, so about
 *   [TAGS_SECONDS] with all pages, stretched by the share of complete subframes when pages are
 *   missing, counting down from when this phase started. Once that has passed, it's
 *   [OsnmaEstimate.overdue].
 */
class OsnmaEstimator {

    private var phaseStart: Long? = null
    private var phase: OsnmaStatus.Phase? = null
    private var krootFirst: Pair<Long, Int>? = null
    private var krootLast: Pair<Long, Int>? = null

    /**
     * @param status the engine status, or null before the first page with a known time
     * @param subframeCompleteness share of complete subframes, or null if not known yet
     * @return what OSNMA is waiting for and how long it may take, or null if it isn't waiting
     * (no data, authenticated, don't use or alert)
     */
    fun update(status: OsnmaStatus?, subframeCompleteness: Double?): OsnmaEstimate? {
        val now = status?.gst?.totalSeconds
        val current = status?.phase
        if (now == null || current == null) {
            reset()
            return null
        }
        if (current != phase) {
            phase = current
            phaseStart = now
        }
        val tagsSeconds = (TAGS_SECONDS / (subframeCompleteness ?: 1.0)
            .coerceAtLeast(MIN_COMPLETENESS)).toLong()
        return when (current) {
            OsnmaStatus.Phase.WAITING_FOR_PUBLIC_KEY ->
                OsnmaEstimate(OsnmaEstimate.WaitingFor.PUBLIC_KEY, null)
            OsnmaStatus.Phase.WAITING_FOR_KROOT -> {
                val parts = status.krootProgress
                val remaining = parts?.let { krootSeconds(now, it) }
                // Too slow to even measure a rate for this long: overdue
                val tooLong = remaining == null && now - (phaseStart ?: now) > MAX_UNKNOWN_SECONDS
                OsnmaEstimate(
                    OsnmaEstimate.WaitingFor.KROOT,
                    remaining?.takeIf { it >= 0 }?.plus(AFTER_KROOT_SECONDS),
                    parts,
                    overdue = remaining == OVERDUE || tooLong
                )
            }
            OsnmaStatus.Phase.WAITING_FOR_TAGS -> {
                clearKroot()
                val remaining = tagsSeconds - (now - (phaseStart ?: now))
                OsnmaEstimate(
                    OsnmaEstimate.WaitingFor.TAGS,
                    remaining.takeIf { it > 0 },
                    overdue = remaining <= 0
                )
            }
            else -> {
                clearKroot()
                null
            }
        }
    }

    /**
     * Remaining time for the root key from the rate its parts arrived at, null without enough
     * data, or [OVERDUE] if no part arrived for much longer than usual
     */
    private fun krootSeconds(now: Long, parts: Pair<Int, Int>): Long? {
        val (have, knownTotal) = parts
        val total = if (knownTotal > 0) knownTotal else maxOf(EXPECTED_KROOT_PARTS, have + 1)
        val last = krootLast
        if (last == null || have < last.second) {
            // First progress, or the root key started over (new DSM ID)
            krootFirst = now to have
            krootLast = now to have
        } else if (have > last.second) {
            krootLast = now to have
        }
        if (have >= total) return 0
        val first = krootFirst ?: return null
        val latest = krootLast ?: return null
        val received = latest.second - first.second
        if (received < MIN_PARTS_FOR_RATE) return null
        // Seconds per part so far, up to now: grows while no part arrives
        val perPart = (now - first.first).toDouble() / received
        if (now - latest.first > perPart * STALL_FACTOR) return OVERDUE
        return ((total - have) * perPart).toLong()
    }

    private fun clearKroot() {
        krootFirst = null
        krootLast = null
    }

    fun reset() {
        phase = null
        phaseStart = null
        clearKroot()
    }

    companion object {
        /**
         * First tags with all pages: up to one subframe until the next one starts, the subframe
         * with the tags, and the next one with the key that checks them, on average about this
         */
        const val TAGS_SECONDS = 90.0

        /** From the complete root key to the first authenticated satellite */
        const val AFTER_KROOT_SECONDS = 30L

        /** Waiting this long for the root key without a measurable rate: overdue */
        const val MAX_UNKNOWN_SECONDS = 600L

        /** Below this share of complete subframes, the estimate would be meaningless anyway */
        const val MIN_COMPLETENESS = 0.25

        /** Parts of the root key needed before their rate is used */
        const val MIN_PARTS_FOR_RATE = 2

        /**
         * Parts of the TESLA root key in the current Galileo configuration (P-256 public key,
         * 2026), assumed until the DSM header says
         */
        const val EXPECTED_KROOT_PARTS = 8

        /** No part for this many times the usual interval: overdue */
        const val STALL_FACTOR = 4.0

        private const val OVERDUE = -1L
    }
}
