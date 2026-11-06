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
 * Writes the OSNMA status to the CSV log: one record per satellite with OSNMA data, once per
 * 30 s subframe.
 */
class OsnmaCsvLog {
    private var lastSubframe: Long? = null
    private var lastKroot: String? = null

    /**
     * @return the records to log for [status], or an empty list if this subframe was already
     * logged or there is nothing to log yet
     */
    fun records(status: OsnmaStatus?, utcMillis: Long): List<String> {
        val gst = status?.gst ?: return emptyList()
        val subframe = gst.subframe().totalSeconds
        if (subframe == lastSubframe) return emptyList()
        lastSubframe = subframe
        val common = listOf(
            "OSNMA",
            utcMillis.toString(),
            gst.wn.toString(),
            gst.tow.toString(),
            status.phase.name,
            status.nmaHeader?.nmaStatus?.name ?: "",
            status.chainInForce?.toString() ?: "",
            status.publicKey?.pkid?.toString() ?: ""
        )
        val satellites = status.satellites.filter { it.transmitsOsnma }
        if (satellites.isEmpty()) {
            return listOf((common + List(SATELLITE_FIELDS) { "" }).joinToString(","))
        }
        return satellites.sortedBy { it.svn }.map { s ->
            (common + listOf(
                s.svn.toString(),
                if (s.cedAuthenticated) "1" else "0",
                s.cedAuthBits.toString(),
                if (s.timingAuthenticated) "1" else "0",
                s.timingAuthBits.toString(),
                s.tagsOk.toString(),
                s.tagsFailed.toString()
            )).joinToString(",")
        }
    }

    /**
     * @return a record with a newly verified DSM-KROOT ([kroot], hex), or null if it was logged
     * already
     */
    fun krootRecord(kroot: String?, utcMillis: Long): String? {
        if (kroot == null || kroot == lastKroot) return null
        lastKroot = kroot
        return "OSNMA_KROOT,$utcMillis,$kroot"
    }

    fun reset() {
        lastSubframe = null
    }

    companion object {
        private const val SATELLITE_FIELDS = 7
    }
}

/**
 * Decides when to warn the user about failed MAC tags: as soon as new failures appear, but at
 * most once per [intervalMillis], so that a spoofing attack or bad reception doesn't flood the
 * user with notifications.
 */
class OsnmaFailureMonitor(private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS) {
    private val failedBySatellite = HashMap<Int, Int>()
    private var lastWarningMillis: Long? = null
    private var pending = sortedSetOf<Int>()

    /**
     * @param nowMillis a monotonic clock reading
     * @return the satellites (SVN) with new failed tags to warn about now, or an empty list
     */
    fun check(status: OsnmaStatus?, nowMillis: Long): List<Int> {
        if (status == null) {
            failedBySatellite.clear()
            pending.clear()
            return emptyList()
        }
        status.satellites.forEach { s ->
            // A lower count means the engine started over: it's taken as the new baseline
            val before = failedBySatellite[s.svn] ?: 0
            if (s.tagsFailed > before) pending.add(s.svn)
            failedBySatellite[s.svn] = s.tagsFailed
        }
        if (pending.isEmpty()) return emptyList()
        val last = lastWarningMillis
        if (last != null && nowMillis - last < intervalMillis) return emptyList()
        lastWarningMillis = nowMillis
        return pending.toList().also { pending = sortedSetOf() }
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 10 * 60 * 1000L
    }
}
