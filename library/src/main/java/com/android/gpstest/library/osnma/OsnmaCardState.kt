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
 * The overall OSNMA state shown in the main chip of the card. Exactly one applies at a time; in
 * case several conditions hold, the first matching one in this order wins: ALERT, DONT_USE,
 * NO_DATA, TIME_NOT_VERIFIED, AUTHENTICATED, IN_PROGRESS.
 */
enum class OsnmaMainState {
    /** No Galileo I/NAV pages yet, or no OSNMA data in them */
    NO_DATA,

    /**
     * Pages are arriving and authentication is progressing: synchronizing with Galileo time,
     * waiting for the public key, the TESLA root key or the first tags
     */
    IN_PROGRESS,

    /** At least one satellite's navigation data is authenticated */
    AUTHENTICATED,

    /** Galileo set the OSNMA status to "don't use" */
    DONT_USE,

    /** An OSNMA Alert Message was received: all keys are revoked until new ones are imported */
    ALERT,

    /**
     * The time is known but more than 165 s off the Galileo time in the signal, so no tags may
     * be used (see [TimeSync.NONE])
     */
    TIME_NOT_VERIFIED
}

/**
 * Warnings that can apply in addition to any [OsnmaMainState], each shown as its own chip. Any
 * combination can occur at the same time.
 */
enum class OsnmaWarning {
    /** A satellite's tag failed verification in the last 10 minutes */
    FAILED_TAGS,

    /**
     * Pages arrive, but too many are missing to complete the subframes, or they stopped arriving
     * while tracking (power saving or E1 pilot tracking), so authentication is slow or stalled
     */
    POOR_RECEPTION,

    /** The time is 15 to 165 s off, so only Slow MAC tags are used (see [TimeSync]) */
    SLOW_MAC_ONLY
}

/** The main state and warnings of the OSNMA card (see [OsnmaMainState] and [OsnmaWarning]) */
data class OsnmaCardState(val main: OsnmaMainState, val warnings: Set<OsnmaWarning>) {

    companion object {
        /**
         * @param status the engine status, or null before the first page with a known time
         * @param pagesReceived Galileo I/NAV pages received so far
         * @param keysRevoked an alert message revoked the stored keys
         * @param clockOffsetSeconds difference between the reference time and the Galileo time
         * in the signal, or null if not known yet
         * @param poorReception too many pages are missing, or they stopped arriving
         */
        fun of(
            status: OsnmaStatus?,
            pagesReceived: Long,
            keysRevoked: Boolean,
            clockOffsetSeconds: Long?,
            poorReception: Boolean
        ): OsnmaCardState {
            val timeSync = TimeSync.forOffset(clockOffsetSeconds)
            val phase = status?.phase
            val main = when {
                keysRevoked || phase == OsnmaStatus.Phase.ALERT -> OsnmaMainState.ALERT
                phase == OsnmaStatus.Phase.DONT_USE -> OsnmaMainState.DONT_USE
                status == null -> {
                    if (pagesReceived > 0) OsnmaMainState.IN_PROGRESS else OsnmaMainState.NO_DATA
                }
                phase == OsnmaStatus.Phase.NO_DATA || phase == OsnmaStatus.Phase.NO_OSNMA ->
                    OsnmaMainState.NO_DATA
                (phase == OsnmaStatus.Phase.WAITING_FOR_TAGS ||
                        phase == OsnmaStatus.Phase.AUTHENTICATING) &&
                        clockOffsetSeconds != null && timeSync == TimeSync.NONE ->
                    OsnmaMainState.TIME_NOT_VERIFIED
                phase == OsnmaStatus.Phase.AUTHENTICATING -> OsnmaMainState.AUTHENTICATED
                else -> OsnmaMainState.IN_PROGRESS
            }
            val warnings = sortedSetOf<OsnmaWarning>()
            if (status != null && status.recentFailures().isNotEmpty()) {
                warnings.add(OsnmaWarning.FAILED_TAGS)
            }
            if (poorReception && main != OsnmaMainState.ALERT) {
                warnings.add(OsnmaWarning.POOR_RECEPTION)
            }
            if (timeSync == TimeSync.SLOW_MAC_ONLY) warnings.add(OsnmaWarning.SLOW_MAC_ONLY)
            return OsnmaCardState(main, warnings)
        }
    }
}
