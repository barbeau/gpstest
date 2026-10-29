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
 * Big-endian (MSB first) bit helpers over byte arrays, as used by all OSNMA and I/NAV fields.
 */
internal object Bits {

    fun get(data: ByteArray, bit: Int): Int =
        (data[bit ushr 3].toInt() ushr (7 - (bit and 7))) and 1

    fun set(data: ByteArray, bit: Int, value: Int) {
        val idx = bit ushr 3
        val mask = 1 shl (7 - (bit and 7))
        data[idx] = if (value != 0) {
            (data[idx].toInt() or mask).toByte()
        } else {
            (data[idx].toInt() and mask.inv()).toByte()
        }
    }

    /** Reads [length] (<= 63) bits starting at bit [offset] as an unsigned value */
    fun read(data: ByteArray, offset: Int, length: Int): Long {
        require(length in 0..63)
        var v = 0L
        for (i in 0 until length) {
            v = (v shl 1) or get(data, offset + i).toLong()
        }
        return v
    }

    fun readInt(data: ByteArray, offset: Int, length: Int): Int = read(data, offset, length).toInt()

    /** Writes the [length] least significant bits of [value] starting at bit [offset] */
    fun write(data: ByteArray, offset: Int, length: Int, value: Long) {
        for (i in 0 until length) {
            set(data, offset + i, ((value ushr (length - 1 - i)) and 1L).toInt())
        }
    }

    /** Copies [length] bits from [src] at [srcOffset] into [dst] at [dstOffset] */
    fun copy(src: ByteArray, srcOffset: Int, dst: ByteArray, dstOffset: Int, length: Int) {
        for (i in 0 until length) {
            set(dst, dstOffset + i, get(src, srcOffset + i))
        }
    }

    /** Extracts [length] bits starting at [offset] into a new, left-aligned byte array */
    fun slice(src: ByteArray, offset: Int, length: Int): ByteArray {
        val out = ByteArray((length + 7) / 8)
        copy(src, offset, out, 0, length)
        return out
    }

    /** Compares [length] bits of two arrays, each starting at its own offset */
    fun equal(a: ByteArray, aOffset: Int, b: ByteArray, bOffset: Int, length: Int): Boolean {
        for (i in 0 until length) {
            if (get(a, aOffset + i) != get(b, bOffset + i)) return false
        }
        return true
    }

    fun toHex(data: ByteArray): String = data.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    fun fromHex(hex: String): ByteArray {
        val clean = hex.trim().replace(" ", "")
        require(clean.length % 2 == 0) { "Odd hex length" }
        return ByteArray(clean.length / 2) {
            clean.substring(2 * it, 2 * it + 2).toInt(16).toByte()
        }
    }
}

/**
 * Accumulates bit fields MSB-first into a growable byte buffer. Used to build MAC inputs, which
 * are not byte aligned.
 */
internal class BitWriter(capacityBytes: Int = 64) {
    private var buf = ByteArray(capacityBytes)
    var bitLength = 0
        private set

    private fun ensure(bits: Int) {
        val needed = (bitLength + bits + 7) / 8
        if (needed > buf.size) buf = buf.copyOf(maxOf(needed, buf.size * 2))
    }

    fun put(value: Long, length: Int): BitWriter {
        ensure(length)
        Bits.write(buf, bitLength, length, value)
        bitLength += length
        return this
    }

    fun putBytes(bytes: ByteArray): BitWriter = putBits(bytes, 0, bytes.size * 8)

    fun putBits(src: ByteArray, offset: Int, length: Int): BitWriter {
        ensure(length)
        Bits.copy(src, offset, buf, bitLength, length)
        bitLength += length
        return this
    }

    /** Appends [length] zero bits */
    fun putZeros(length: Int): BitWriter {
        ensure(length)
        // The buffer is zero-initialized and never written past bitLength, so just advance
        bitLength += length
        return this
    }

    /** Returns the content zero-padded to a whole number of bytes */
    fun toByteArray(): ByteArray = buf.copyOf((bitLength + 7) / 8)
}
