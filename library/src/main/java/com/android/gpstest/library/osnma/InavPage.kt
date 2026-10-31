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
 * A Galileo E1-B I/NAV nominal page (even + odd page part), as delivered by Android's
 * GnssNavigationMessage for TYPE_GAL_I: 2 x 114 bits (sync and tail removed) packed MSB first
 * into 29 bytes.
 *
 * Layout (bit offsets into the 228 bits):
 * - even part:  0 E/O, 1 page type, 2..113 data k (112 bits)
 * - odd part: 114 E/O, 115 page type, 116..131 data j (16 bits), 132..171 OSNMA (40 bits),
 *   172..193 SAR, 194..195 spare, 196..219 CRC-24Q, 220..227 SSP
 *
 * See the Galileo OS SIS ICD, section 4.3.2.
 */
class InavPage private constructor(private val bits: ByteArray) {

    val isEvenFirst: Boolean get() = Bits.get(bits, 0) == 0 && Bits.get(bits, ODD_START) == 1

    /** 0 = nominal page, 1 = alert page */
    val pageType: Int get() = Bits.get(bits, 1)

    val isNominal: Boolean get() = isEvenFirst && pageType == 0 && Bits.get(bits, ODD_START + 1) == 0

    /** The CRC-24Q covers the even part and the odd part up to (excluding) the CRC */
    val crcOk: Boolean
        get() {
            val computed = Crc24q.compute(bits, 0, CRC_OFFSET)
            val received = Bits.read(bits, CRC_OFFSET, 24).toInt()
            return computed == received
        }

    /** The 128-bit I/NAV word (data k + data j) */
    val word: ByteArray
        get() {
            val w = ByteArray(16)
            Bits.copy(bits, 2, w, 0, 112)
            Bits.copy(bits, ODD_START + 2, w, 112, 16)
            return w
        }

    val wordType: Int get() = Bits.readInt(bits, 2, 6)

    /** The 40-bit OSNMA field: 8 bits HKROOT section followed by 32 bits MACK section */
    val osnma: ByteArray get() = Bits.slice(bits, OSNMA_OFFSET, 40)

    /** True if the OSNMA field is all zeros, meaning this satellite doesn't transmit OSNMA */
    val osnmaEmpty: Boolean
        get() = (0 until 40).all { Bits.get(bits, OSNMA_OFFSET + it) == 0 }

    /**
     * GST at the start of this page, if the word carries a complete time stamp (word types 0 with
     * time field '10', 5, and 6 when the week number is known). Returns null otherwise.
     */
    fun gstFromWord(knownWn: Int?): Gst? {
        val w = word
        return when (Bits.readInt(w, 0, 6)) {
            0 -> if (Bits.readInt(w, 6, 2) == 2) {
                gstOrNull(Bits.readInt(w, 96, 12), Bits.readInt(w, 108, 20))
            } else null
            5 -> gstOrNull(Bits.readInt(w, 73, 12), Bits.readInt(w, 85, 20))
            6 -> if (knownWn != null) gstOrNull(knownWn, Bits.readInt(w, 105, 20)) else null
            else -> null
        }
    }

    private fun gstOrNull(wn: Int, tow: Int): Gst? =
        if (tow < Gst.SECS_IN_WEEK) Gst(wn, tow) else null

    fun toByteArray(): ByteArray = bits.copyOf()

    companion object {
        const val SIZE_BYTES = 29
        private const val ODD_START = 114
        private const val OSNMA_OFFSET = ODD_START + 18
        private const val CRC_OFFSET = ODD_START + 82

        /** Wraps the 29-byte Android TYPE_GAL_I data. Returns null if the size is wrong. */
        fun fromAndroid(data: ByteArray): InavPage? =
            if (data.size >= SIZE_BYTES) InavPage(data.copyOf(SIZE_BYTES)) else null

        /**
         * Converts a full 240-bit nominal page (2 x 120 bits including tail bits, as used by the
         * GSC OSNMA test vectors) into the Android layout.
         */
        fun fromFullPage(page: ByteArray): InavPage {
            require(page.size >= 30)
            val out = ByteArray(SIZE_BYTES)
            Bits.copy(page, 0, out, 0, 114)
            Bits.copy(page, 120, out, 114, 114)
            return InavPage(out)
        }
    }
}

/** CRC-24Q as used by Galileo I/NAV (generator 0x1864CFB, init 0) */
internal object Crc24q {
    private val table = IntArray(256).also { t ->
        for (i in 0 until 256) {
            var crc = i shl 16
            repeat(8) {
                crc = crc shl 1
                if (crc and 0x1000000 != 0) crc = crc xor 0x1864CFB
            }
            t[i] = crc and 0xFFFFFF
        }
    }

    fun compute(data: ByteArray, offsetBits: Int, lengthBits: Int): Int {
        // The CRC is defined over a bit string; left-pad with zeros to a byte boundary, which
        // doesn't change a CRC with zero initial value.
        val pad = (8 - lengthBits % 8) % 8
        val total = lengthBits + pad
        val buf = ByteArray(total / 8)
        Bits.copy(data, offsetBits, buf, pad, lengthBits)
        var crc = 0
        for (b in buf) {
            crc = ((crc shl 8) xor table[((crc ushr 16) xor (b.toInt() and 0xFF)) and 0xFF]) and 0xFFFFFF
        }
        return crc
    }
}
