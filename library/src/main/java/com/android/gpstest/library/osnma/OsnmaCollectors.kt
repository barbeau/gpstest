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
 * Collects the 15 OSNMA fields that each satellite transmits during a subframe into the 120-bit
 * HKROOT message and the 480-bit MACK message (ICD section 2).
 *
 * Pages must be fed in chronological order. A page from a new subframe discards the partial data
 * of the previous one.
 */
internal class SubframeCollector {
    private val hkroot = Array(NUM_SVNS) { ByteArray(HKROOT_BYTES) }
    private val mack = Array(NUM_SVNS) { ByteArray(MACK_BYTES) }
    private val numValid = IntArray(NUM_SVNS)
    private var current: Gst? = null

    class Completed(val hkroot: ByteArray, val mack: ByteArray, val gstSubframe: Gst)

    fun feed(osnma: ByteArray, svn: Int, gst: Gst): Completed? {
        val subframe = gst.subframe()
        if (subframe != current) {
            current = subframe
            numValid.fill(0)
        }
        val idx = svn - 1
        val wordNum = (gst.tow / 2) % WORDS_PER_SUBFRAME
        if (wordNum != numValid[idx]) {
            // A page is missing; this subframe can't be completed for this satellite
            return null
        }
        hkroot[idx][wordNum] = osnma[0]
        System.arraycopy(osnma, 1, mack[idx], wordNum * 4, 4)
        numValid[idx]++
        return if (numValid[idx] == WORDS_PER_SUBFRAME) {
            Completed(hkroot[idx].copyOf(), mack[idx].copyOf(), subframe)
        } else null
    }

    companion object {
        const val NUM_SVNS = 36
        const val WORDS_PER_SUBFRAME = 15
        const val HKROOT_BYTES = 15
        const val MACK_BYTES = 60
    }
}

/**
 * Collects the 13-byte DSM blocks broadcast in the HKROOT messages into complete DSM-KROOT and
 * DSM-PKR messages (ICD section 3.2). Blocks of the same DSM can come from any satellite.
 */
internal class DsmCollector {
    private val dsm = ByteArray(MAX_BLOCKS * BLOCK_BYTES)
    private val blockValid = BooleanArray(MAX_BLOCKS)
    private var dsmId: Int = -1
    private var done = false

    class Completed(val id: Int, val isPkr: Boolean, val data: ByteArray)

    fun feed(header: Int, block: ByteArray): Completed? {
        val id = (header ushr 4) and 0xF
        val blockId = header and 0xF
        if (id != dsmId) {
            dsmId = id
            blockValid.fill(false)
            done = false
        }
        if (done) return null
        if (!blockValid[blockId]) {
            System.arraycopy(block, 0, dsm, blockId * BLOCK_BYTES, BLOCK_BYTES)
            blockValid[blockId] = true
        }
        if (!blockValid[0]) return null
        val isPkr = id >= 12
        val nb = (dsm[0].toInt() ushr 4) and 0xF
        val n = if (isPkr) {
            if (nb in 7..10) nb + 6 else return null
        } else {
            if (nb in 1..8) nb + 6 else return null
        }
        if ((0 until n).any { !blockValid[it] }) return null
        done = true
        return Completed(id, isPkr, dsm.copyOf(n * BLOCK_BYTES))
    }

    companion object {
        const val MAX_BLOCKS = 16
        const val BLOCK_BYTES = 13
    }
}

/** MACK messages of recent subframes, needed because tags are verified with a delay */
internal class MackStore(private val depth: Int = 12) {
    class Stored(val mack: ByteArray, val nmaStatus: NmaStatus)

    private val bySubframe = LinkedHashMap<Gst, HashMap<Int, Stored>>()

    fun store(mack: ByteArray, svn: Int, gst: Gst, nmaStatus: NmaStatus) {
        bySubframe.getOrPut(gst) { HashMap() }[svn] = Stored(mack, nmaStatus)
        while (bySubframe.size > depth) {
            bySubframe.remove(bySubframe.keys.minOrNull())
        }
    }

    fun get(svn: Int, gst: Gst): Stored? = bySubframe[gst]?.get(svn)

    fun clear() = bySubframe.clear()
}

/** Navigation data authenticated by one of the ADKDs, assembled for a given subframe */
internal class NavData(val bits: ByteArray, val lengthBits: Int, val maxAge: Int)

/**
 * Stores the I/NAV words received from each satellite, by subframe, so that the navigation data
 * authenticated by a tag (data as of the subframe before the tag) can be rebuilt later.
 */
internal class NavDataStore(private val depthSubframes: Int = 30) {

    private class Entry(val subframe: Gst, val word: ByteArray)

    // svn -> word type -> receptions, oldest first
    private val words = HashMap<Int, HashMap<Int, ArrayDeque<Entry>>>()

    fun feed(svn: Int, gstSubframe: Gst, word: ByteArray) {
        val type = Bits.readInt(word, 0, 6)
        if (type !in STORED_TYPES) return
        val list = words.getOrPut(svn) { HashMap() }.getOrPut(type) { ArrayDeque() }
        val last = list.lastOrNull()
        if (last != null && last.subframe == gstSubframe) {
            list.removeLast()
        }
        list.addLast(Entry(gstSubframe, word.copyOf()))
        val oldest = gstSubframe.addSubframes(-depthSubframes)
        while (list.isNotEmpty() && list.first().subframe < oldest) list.removeFirst()
    }

    private fun latest(svn: Int, type: Int, subframe: Gst): Entry? =
        words[svn]?.get(type)?.lastOrNull { it.subframe <= subframe }

    private fun assemble(
        svn: Int,
        subframe: Gst,
        parts: List<Part>,
        lengthBits: Int
    ): NavData? {
        val out = ByteArray((lengthBits + 7) / 8)
        var maxAge = 0
        for (p in parts) {
            val e = latest(svn, p.type, subframe) ?: return null
            Bits.copy(e.word, p.from, out, p.to, p.length)
            maxAge = maxOf(maxAge, subframe.subframesSince(e.subframe))
        }
        return NavData(out, lengthBits, maxAge)
    }

    private class Part(val type: Int, val from: Int, val to: Int, val length: Int)

    /** ADKD 0 / 12: ephemeris, clock and status from word types 1 to 5 (549 bits) */
    fun cedAndStatus(svn: Int, subframe: Gst): NavData? = assemble(svn, subframe, CED_PARTS, CED_BITS)

    /** ADKD 4: timing parameters from word types 6 and 10 (141 bits) */
    fun timing(svn: Int, subframe: Gst): NavData? = assemble(svn, subframe, TIMING_PARTS, TIMING_BITS)

    fun clear() = words.clear()

    companion object {
        const val CED_BITS = 549
        const val TIMING_BITS = 141
        private val STORED_TYPES = setOf(1, 2, 3, 4, 5, 6, 10)
        private val CED_PARTS = listOf(
            Part(1, 6, 0, 120),
            Part(2, 6, 120, 120),
            Part(3, 6, 240, 122),
            Part(4, 6, 362, 120),
            Part(5, 6, 482, 67)
        )
        private val TIMING_PARTS = listOf(
            Part(6, 6, 0, 99),
            Part(10, 86, 99, 42)
        )
    }
}
