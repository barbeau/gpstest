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
 * Galileo System Time as week number (WN) and time of week (TOW) in seconds.
 *
 * OSNMA only needs whole seconds. Pages of the I/NAV message start at odd GST seconds, and
 * subframes (15 pages, 30 s) start at TOW values that are a multiple of 30 (+1 for the first
 * page).
 */
data class Gst(val wn: Int, val tow: Int) : Comparable<Gst> {

    init {
        require(tow in 0 until SECS_IN_WEEK) { "TOW out of range: $tow" }
    }

    /** Total seconds since the GST epoch */
    val totalSeconds: Long get() = wn.toLong() * SECS_IN_WEEK + tow

    fun addSeconds(seconds: Long): Gst = fromTotalSeconds(totalSeconds + seconds)

    fun addSeconds(seconds: Int): Gst = addSeconds(seconds.toLong())

    fun addSubframes(subframes: Int): Gst = addSeconds(subframes.toLong() * SECS_PER_SUBFRAME)

    /** GST at the start of the subframe containing this instant */
    fun subframe(): Gst = Gst(wn, tow / SECS_PER_SUBFRAME * SECS_PER_SUBFRAME)

    val isSubframe: Boolean get() = tow % SECS_PER_SUBFRAME == 0

    /** Number of subframes from [other] to this (positive if this is later) */
    fun subframesSince(other: Gst): Int =
        ((totalSeconds - other.totalSeconds) / SECS_PER_SUBFRAME).toInt()

    override fun compareTo(other: Gst): Int = totalSeconds.compareTo(other.totalSeconds)

    override fun toString(): String = "WN $wn TOW $tow"

    companion object {
        const val SECS_IN_WEEK = 7 * 24 * 3600
        const val SECS_PER_SUBFRAME = 30

        fun fromTotalSeconds(seconds: Long): Gst {
            require(seconds >= 0) { "Negative GST" }
            return Gst((seconds / SECS_IN_WEEK).toInt(), (seconds % SECS_IN_WEEK).toInt())
        }
    }
}
