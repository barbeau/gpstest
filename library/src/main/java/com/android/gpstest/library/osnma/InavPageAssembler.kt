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
 * Repairs I/NAV pages whose two halves were paired the wrong way round by the platform.
 *
 * A nominal page is an even page part followed by an odd page part, 1 s each. Some chipsets
 * report the odd part of page n-1 together with the even part of page n instead, so every message
 * starts with an odd part. Rejoining the even part at the end of one such message with the odd
 * part at the start of the next one (2 s later, same satellite) restores page n. The CRC of the
 * restored page tells whether the repair was right.
 */
class InavPageAssembler {

    private class Pending(val data: ByteArray, val arrivalMillis: Long)

    private val pending = HashMap<Int, Pending>()

    /** Pages restored from two swapped messages */
    var repaired = 0L
        private set

    /** Result of [assemble]: the page to use and the arrival time of its last part */
    class Assembled(val page: InavPage, val arrivalMillis: Long, val repaired: Boolean)

    /**
     * @return the page to process (as given, or restored from the previous message), or null if
     * the message is a swapped one waiting for its partner
     */
    fun assemble(svn: Int, page: InavPage, arrivalMillis: Long): Assembled? {
        val bits = page.toByteArray()
        val swapped = Bits.get(bits, 0) == 1 && Bits.get(bits, HALF) == 0
        if (!swapped) {
            pending.remove(svn)
            return Assembled(page, arrivalMillis, false)
        }
        val previous = pending.put(svn, Pending(bits, arrivalMillis))
        if (previous == null) return null
        val gap = arrivalMillis - previous.arrivalMillis
        if (gap !in MIN_GAP_MILLIS..MAX_GAP_MILLIS) return null
        // Even part from the end of the previous message, odd part from the start of this one
        val joined = ByteArray(InavPage.SIZE_BYTES)
        Bits.copy(previous.data, HALF, joined, 0, HALF)
        Bits.copy(bits, 0, joined, HALF, HALF)
        repaired++
        // The odd part ended 1 s before this message (the even part of the next page followed)
        return Assembled(InavPage.fromAndroid(joined)!!, arrivalMillis - 1000, true)
    }

    fun reset() {
        pending.clear()
        repaired = 0
    }

    companion object {
        private const val HALF = 114
        private const val MIN_GAP_MILLIS = 1000L
        private const val MAX_GAP_MILLIS = 3500L
    }
}
