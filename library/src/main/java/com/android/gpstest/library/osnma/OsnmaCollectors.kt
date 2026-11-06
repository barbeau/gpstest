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
 * Stores the OSNMA field of every page received, by satellite and subframe, so that the HKROOT and
 * MACK messages can be used even when some pages are missing.
 *
 * Each page carries 8 HKROOT bits and 32 MACK bits (ICD section 2). Instead of waiting for all 15
 * pages of a subframe, the engine uses whatever parts of the messages have arrived: the DSM bytes
 * of any satellite, the TESLA key from the pages of any satellite (all transmit the same key in a
 * subframe) and each tag as soon as its own pages are there. Everything assembled this way is
 * still verified cryptographically, so mixing up parts can only make a check fail.
 */
internal class PageStore(private val depthSubframes: Int = 13) {

    class SubframePages(val svn: Int, val gstSubframe: Gst) {
        val hkroot = ByteArray(SubframeLayout.HKROOT_BYTES)
        val mack = ByteArray(SubframeLayout.MACK_BYTES)
        /** Bit i set if page i of the subframe was received */
        var mask = 0
        val complete: Boolean get() = mask == SubframeLayout.ALL_PAGES
        fun has(page: Int) = mask and (1 shl page) != 0

        /** True if all pages carrying MACK bits [start, start + length) were received */
        fun hasMackBits(start: Int, length: Int): Boolean {
            if (length <= 0) return true
            for (page in start / 32..(start + length - 1) / 32) if (!has(page)) return false
            return true
        }
    }

    private val bySubframe = LinkedHashMap<Gst, HashMap<Int, SubframePages>>()

    /** NMA header (HKROOT byte 0) per subframe; the same for all satellites */
    private val headers = HashMap<Gst, Int>()

    private var first: Gst? = null

    /** (Satellite, subframe) pairs with OSNMA data, not counting the first, partial subframe */
    var started = 0L
        private set

    /** Pairs completed (all 15 pages), all subframes */
    var completed = 0L
        private set

    /** Pairs completed in the first subframe */
    var firstCompleted = 0L
        private set

    class Stored(val pages: SubframePages, val page: Int, val newlyComplete: Boolean)

    fun store(svn: Int, gst: Gst, osnma: ByteArray): Stored {
        val subframe = gst.subframe()
        val page = SubframeLayout.pageOf(gst)
        if (first == null) first = subframe
        val map = bySubframe.getOrPut(subframe) { HashMap() }
        val pages = map.getOrPut(svn) {
            if (subframe != first) started++
            SubframePages(svn, subframe)
        }
        val wasComplete = pages.complete
        pages.hkroot[page] = osnma[0]
        System.arraycopy(osnma, 1, pages.mack, page * 4, 4)
        pages.mask = pages.mask or (1 shl page)
        if (page == 0) headers[subframe] = osnma[0].toInt() and 0xFF
        val newlyComplete = !wasComplete && pages.complete
        if (newlyComplete) {
            completed++
            if (subframe == first) firstCompleted++
        }
        prune(subframe)
        return Stored(pages, page, newlyComplete)
    }

    private fun prune(latest: Gst) {
        val oldest = latest.addSubframes(-depthSubframes)
        val it = bySubframe.keys.iterator()
        while (it.hasNext()) {
            val k = it.next()
            if (k < oldest) {
                it.remove()
                headers.remove(k)
            }
        }
    }

    fun get(svn: Int, gstSubframe: Gst): SubframePages? = bySubframe[gstSubframe]?.get(svn)

    fun subframe(gstSubframe: Gst): Collection<SubframePages> =
        bySubframe[gstSubframe]?.values ?: emptyList()

    fun header(gstSubframe: Gst): NmaHeader? = headers[gstSubframe]?.let { NmaHeader(it) }

    /** Distinct NMA headers seen recently, newest first */
    fun recentHeaders(): List<NmaHeader> =
        headers.entries
            .sortedByDescending { it.key }
            .map { it.value }
            .distinct()
            .map { NmaHeader(it) }

    fun clear() {
        bySubframe.clear()
        headers.clear()
    }
}

internal object SubframeLayout {
    const val PAGES = 15
    const val HKROOT_BYTES = 15
    const val MACK_BYTES = 60
    const val ALL_PAGES = (1 shl PAGES) - 1

    /** Page (0-14) of the page starting at [gst] within its subframe */
    fun pageOf(gst: Gst): Int = (gst.tow / 2) % PAGES
}

/**
 * Collects DSM-KROOT and DSM-PKR messages byte by byte (ICD section 3.2). All satellites broadcast
 * the same DSMs, so each byte of a DSM block needs to be received only once, from any satellite and
 * any repetition. A block's ID comes from the DSM header (HKROOT byte 1, page 1), so the bytes of a
 * satellite's subframe are only used once its page 1 has been received.
 */
internal class DsmAssembler {

    private class Entry {
        val blocks = arrayOfNulls<ByteArray>(MAX_BLOCKS)
        val masks = IntArray(MAX_BLOCKS)
        /** Bytes restored from an earlier session and not received again since */
        val restoredMasks = IntArray(MAX_BLOCKS)
        var lastProcessed: ByteArray? = null
    }

    private val entries = HashMap<Int, Entry>()

    class Completed(val id: Int, val isPkr: Boolean, val data: ByteArray)

    /** Writes byte [index] (0-12) of block [blockId] of DSM [dsmId] */
    fun write(dsmId: Int, blockId: Int, index: Int, value: Byte): Completed? {
        val entry = entries.getOrPut(dsmId) { Entry() }
        val block = entry.blocks[blockId]
            ?: ByteArray(BLOCK_BYTES).also { entry.blocks[blockId] = it }
        block[index] = value
        entry.masks[blockId] = entry.masks[blockId] or (1 shl index)
        entry.restoredMasks[blockId] = entry.restoredMasks[blockId] and (1 shl index).inv()
        return completed(dsmId, entry)
    }

    private fun completed(dsmId: Int, entry: Entry): Completed? {
        if (entry.masks[0] and 1 == 0) return null
        val isPkr = dsmId >= 12
        val nb = (entry.blocks[0]!![0].toInt() ushr 4) and 0xF
        val n = if (isPkr) {
            if (nb in 7..10) nb + 6 else return null
        } else {
            if (nb in 1..8) nb + 6 else return null
        }
        for (b in 0 until n) if (entry.masks[b] != FULL_BLOCK) return null
        val data = ByteArray(n * BLOCK_BYTES)
        for (b in 0 until n) {
            System.arraycopy(entry.blocks[b]!!, 0, data, b * BLOCK_BYTES, BLOCK_BYTES)
        }
        if (entry.lastProcessed?.contentEquals(data) == true) return null
        entry.lastProcessed = data
        return Completed(dsmId, isPkr, data)
    }

    /** Fraction of the blocks of [dsmId] received, for display; null if unknown */
    fun progress(dsmId: Int): Pair<Int, Int>? {
        val entry = entries[dsmId] ?: return null
        val full = entry.masks.count { it == FULL_BLOCK }
        val nb = entry.blocks[0]
            ?.takeIf { entry.masks[0] and 1 != 0 }
            ?.let { (it[0].toInt() ushr 4) and 0xF }
        val n = when {
            nb == null -> null
            dsmId >= 12 -> nb + 6
            else -> nb + 6
        }
        return full to (n ?: 0)
    }

    fun clear() = entries.clear()

    /**
     * Called when the assembled DSM [dsmId] failed verification: drops the bytes restored from an
     * earlier session that haven't been received again, since they may be outdated. Bytes
     * received in this session are kept; wrong ones get overwritten as pages keep coming.
     */
    fun dropRestored(dsmId: Int) {
        val entry = entries[dsmId] ?: return
        for (b in 0 until MAX_BLOCKS) {
            entry.masks[b] = entry.masks[b] and entry.restoredMasks[b].inv()
            entry.restoredMasks[b] = 0
        }
        entry.lastProcessed = null
    }

    /**
     * Collected bytes as text, to continue collecting in a later session. Format: entries
     * separated by ';', each "dsmId:blockId=maskHex/bytesHex,...".
     */
    fun export(): String = entries.entries.sortedBy { it.key }.joinToString(";") { (id, e) ->
        val blocks = (0 until MAX_BLOCKS).filter { e.masks[it] != 0 }.joinToString(",") {
            "$it=${Integer.toHexString(e.masks[it])}/${Bits.toHex(e.blocks[it]!!)}"
        }
        "$id:$blocks"
    }

    /** Restores bytes saved with [export]; invalid text is ignored */
    fun import(text: String) {
        try {
            for (part in text.split(';')) {
                if (part.isBlank()) continue
                val id = part.substringBefore(':').toInt()
                if (id !in 0 until MAX_BLOCKS) continue
                val entry = Entry()
                for (block in part.substringAfter(':').split(',')) {
                    if (block.isBlank()) continue
                    val blockId = block.substringBefore('=').toInt()
                    val mask = block.substringAfter('=').substringBefore('/').toInt(16)
                    val bytes = Bits.fromHex(block.substringAfter('/'))
                    if (blockId !in 0 until MAX_BLOCKS || bytes.size != BLOCK_BYTES) continue
                    entry.blocks[blockId] = bytes
                    entry.masks[blockId] = mask and FULL_BLOCK
                    entry.restoredMasks[blockId] = mask and FULL_BLOCK
                }
                entries[id] = entry
            }
        } catch (e: Exception) {
            entries.clear()
        }
    }

    companion object {
        const val MAX_BLOCKS = 16
        const val BLOCK_BYTES = 13
        private const val FULL_BLOCK = (1 shl BLOCK_BYTES) - 1
    }
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
    fun cedAndStatus(svn: Int, subframe: Gst): NavData? =
        assemble(svn, subframe, CED_PARTS, CED_BITS)

    /** ADKD 4: timing parameters from word types 6 and 10 (141 bits) */
    fun timing(svn: Int, subframe: Gst): NavData? =
        assemble(svn, subframe, TIMING_PARTS, TIMING_BITS)

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
