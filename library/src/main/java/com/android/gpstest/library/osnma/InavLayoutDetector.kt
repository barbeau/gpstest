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
 * Works out where the 228 I/NAV page bits sit in the 29 bytes Android delivers.
 *
 * The Android documentation puts them at the start, with the 4 unused bits at the end. Some
 * devices (a Pixel 8 Pro, Android 17) put the 4 unused bits at the start instead, reported as
 * https://issuetracker.google.com/issues/580675649. Each message is checked with both layouts;
 * a layout is adopted once several pages pass the CRC with it and none with the other. Until
 * then, the layout under which a page passes the CRC is used for that page.
 */
class InavLayoutDetector {

    private val passes = IntArray(OFFSETS.size)

    /** The layout in use, in bits from the start of the data, or null while still undecided */
    var offsetBits: Int? = null
        private set

    /** Returns the page read with the best layout for this message */
    fun read(data: ByteArray): InavPage? {
        offsetBits?.let { return InavPage.fromAndroid(data, it) }
        val candidates = OFFSETS.map { InavPage.fromAndroid(data, it) ?: return null }
        val passing = candidates.indices.filter { candidates[it].isNominal && candidates[it].crcOk }
        if (passing.size == 1) {
            val i = passing[0]
            passes[i]++
            if (passes[i] >= ADOPT_AFTER && passes.indices.all { it == i || passes[it] == 0 }) {
                offsetBits = OFFSETS[i]
            }
            return candidates[i]
        }
        // Undecided and the CRC fails with both: use the documented layout (the page is rejected)
        return candidates[0]
    }

    fun reset() {
        passes.fill(0)
        offsetBits = null
    }

    companion object {
        private val OFFSETS = intArrayOf(0, 4)
        private const val ADOPT_AFTER = 5
    }
}
