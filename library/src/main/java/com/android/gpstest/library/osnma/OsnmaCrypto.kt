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

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.EllipticCurve
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic primitives needed by OSNMA, implemented on top of what every Android version from
 * API 24 provides (SHA-256, HMAC-SHA-256, AES, ECDSA). SHA3-256 and AES-CMAC are implemented here
 * because they aren't reliably available from the platform providers.
 */
object OsnmaCrypto {

    internal fun sha256(vararg parts: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        parts.forEach { md.update(it) }
        return md.digest()
    }

    internal fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** AES-CMAC (RFC 4493). [key] must be 16 bytes. */
    internal fun cmacAes(key: ByteArray, data: ByteArray): ByteArray {
        require(key.size == 16) { "CMAC-AES needs a 128-bit key" }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val l = cipher.doFinal(ByteArray(16))
        val k1 = shiftLeftXor(l)
        val k2 = shiftLeftXor(k1)
        val n = if (data.isEmpty()) 1 else (data.size + 15) / 16
        val lastComplete = data.isNotEmpty() && data.size % 16 == 0
        val last = ByteArray(16)
        val lastStart = (n - 1) * 16
        if (lastComplete) {
            for (i in 0 until 16) last[i] = (data[lastStart + i].toInt() xor k1[i].toInt()).toByte()
        } else {
            val rem = data.size - lastStart
            for (i in 0 until rem) last[i] = data[lastStart + i]
            last[rem] = 0x80.toByte()
            for (i in 0 until 16) last[i] = (last[i].toInt() xor k2[i].toInt()).toByte()
        }
        var x = ByteArray(16)
        for (b in 0 until n - 1) {
            val y = ByteArray(16) { (x[it].toInt() xor data[b * 16 + it].toInt()).toByte() }
            x = cipher.doFinal(y)
        }
        val y = ByteArray(16) { (x[it].toInt() xor last[it].toInt()).toByte() }
        return cipher.doFinal(y)
    }

    private fun shiftLeftXor(input: ByteArray): ByteArray {
        val out = ByteArray(16)
        var carry = 0
        for (i in 15 downTo 0) {
            val v = input[i].toInt() and 0xFF
            out[i] = ((v shl 1) or carry).toByte()
            carry = v ushr 7
        }
        if (input[0].toInt() and 0x80 != 0) out[15] = (out[15].toInt() xor 0x87).toByte()
        return out
    }

    /** SHA3-256 (FIPS 202) */
    internal fun sha3_256(vararg parts: ByteArray): ByteArray {
        val total = parts.sumOf { it.size }
        val msg = ByteArray(total)
        var p = 0
        parts.forEach { System.arraycopy(it, 0, msg, p, it.size); p += it.size }
        return Keccak.sha3_256(msg)
    }

    // ---------------------------------------------------------------------------------------
    // ECDSA
    // ---------------------------------------------------------------------------------------

    enum class Curve(
        val pHex: String,
        val aHex: String,
        val bHex: String,
        val gxHex: String,
        val gyHex: String,
        val nHex: String,
        val coordBytes: Int,
        val signatureAlgorithm: String
    ) {
        P256(
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC",
            "5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B",
            "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296",
            "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5",
            "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551",
            32,
            "SHA256withECDSA"
        ),
        P521(
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC",
            "0051953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F00",
            "00C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66",
            "011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409",
            66,
            "SHA512withECDSA"
        );

        val p: BigInteger by lazy { BigInteger(pHex, 16) }
        val a: BigInteger by lazy { BigInteger(aHex, 16) }
        val b: BigInteger by lazy { BigInteger(bHex, 16) }

        val spec: ECParameterSpec by lazy {
            ECParameterSpec(
                EllipticCurve(ECFieldFp(p), a, b),
                ECPoint(BigInteger(gxHex, 16), BigInteger(gyHex, 16)),
                BigInteger(nHex, 16),
                1
            )
        }

        /** Size of a SEC1 compressed point */
        val compressedSize: Int get() = coordBytes + 1
    }

    /**
     * Parses a SEC1-encoded public key (compressed 0x02/0x03 or uncompressed 0x04), validating
     * that the point is on the curve. Returns null if the encoding is invalid.
     */
    internal fun publicKeyFromSec1(curve: Curve, sec1: ByteArray): ECPublicKey? {
        return try {
            val x: BigInteger
            val y: BigInteger
            when {
                sec1.size == curve.coordBytes + 1 && (sec1[0].toInt() == 2 || sec1[0].toInt() == 3) -> {
                    x = BigInteger(1, sec1.copyOfRange(1, sec1.size))
                    if (x >= curve.p) return null
                    val rhs = x.modPow(BigInteger.valueOf(3), curve.p)
                        .add(curve.a.multiply(x)).add(curve.b).mod(curve.p)
                    // Both P-256 and P-521 have p = 3 mod 4, so sqrt(r) = r^((p+1)/4)
                    var yy = rhs.modPow(curve.p.add(BigInteger.ONE).shiftRight(2), curve.p)
                    if (yy.multiply(yy).mod(curve.p) != rhs) return null
                    val wantOdd = sec1[0].toInt() == 3
                    if (yy.testBit(0) != wantOdd) yy = curve.p.subtract(yy)
                    y = yy
                }
                sec1.size == 2 * curve.coordBytes + 1 && sec1[0].toInt() == 4 -> {
                    x = BigInteger(1, sec1.copyOfRange(1, 1 + curve.coordBytes))
                    y = BigInteger(1, sec1.copyOfRange(1 + curve.coordBytes, sec1.size))
                    val lhs = y.multiply(y).mod(curve.p)
                    val rhs = x.modPow(BigInteger.valueOf(3), curve.p)
                        .add(curve.a.multiply(x)).add(curve.b).mod(curve.p)
                    if (lhs != rhs) return null
                }
                else -> return null
            }
            KeyFactory.getInstance("EC")
                .generatePublic(ECPublicKeySpec(ECPoint(x, y), curve.spec)) as ECPublicKey
        } catch (e: Exception) {
            null
        }
    }

    /** SEC1 compressed encoding of [key] */
    internal fun compress(curve: Curve, key: ECPublicKey): ByteArray {
        val out = ByteArray(curve.compressedSize)
        out[0] = if (key.w.affineY.testBit(0)) 3 else 2
        val x = key.w.affineX.toByteArray().let { raw ->
            // Strip a sign byte or left-pad to the coordinate size
            if (raw.size > curve.coordBytes) raw.copyOfRange(raw.size - curve.coordBytes, raw.size) else raw
        }
        System.arraycopy(x, 0, out, out.size - x.size, x.size)
        return out
    }

    /** Detects the curve of a public key from the platform by its field size */
    internal fun curveOf(key: ECPublicKey): Curve? = when (key.params.curve.field.fieldSize) {
        256 -> Curve.P256
        521 -> Curve.P521
        else -> null
    }

    /**
     * Verifies an ECDSA signature given in the raw r || s form used by OSNMA (each half is the
     * size of the curve coordinates).
     */
    internal fun verifyEcdsa(curve: Curve, key: ECPublicKey, message: ByteArray, rawSignature: ByteArray): Boolean {
        if (rawSignature.size != 2 * curve.coordBytes) return false
        return try {
            val r = BigInteger(1, rawSignature.copyOfRange(0, curve.coordBytes))
            val s = BigInteger(1, rawSignature.copyOfRange(curve.coordBytes, rawSignature.size))
            val sig = Signature.getInstance(curve.signatureAlgorithm)
            sig.initVerify(key)
            sig.update(message)
            sig.verify(derSignature(r, s))
        } catch (e: Exception) {
            false
        }
    }

    private fun derSignature(r: BigInteger, s: BigInteger): ByteArray {
        val body = ByteArrayOutputStream()
        derInteger(body, r)
        derInteger(body, s)
        val out = ByteArrayOutputStream()
        out.write(0x30)
        derLength(out, body.size())
        body.writeTo(out)
        return out.toByteArray()
    }

    private fun derInteger(out: ByteArrayOutputStream, v: BigInteger) {
        val bytes = v.toByteArray() // minimal two's complement, positive
        out.write(0x02)
        derLength(out, bytes.size)
        out.write(bytes)
    }

    private fun derLength(out: ByteArrayOutputStream, len: Int) {
        when {
            len < 0x80 -> out.write(len)
            len < 0x100 -> { out.write(0x81); out.write(len) }
            else -> { out.write(0x82); out.write(len ushr 8); out.write(len and 0xFF) }
        }
    }
}

/** Minimal Keccak-f[1600] sponge for SHA3-256 */
internal object Keccak {
    private val RC = listOf(
        "0000000000000001", "0000000000008082", "800000000000808A", "8000000080008000",
        "000000000000808B", "0000000080000001", "8000000080008081", "8000000000008009",
        "000000000000008A", "0000000000000088", "0000000080008009", "000000008000000A",
        "000000008000808B", "800000000000008B", "8000000000008089", "8000000000008003",
        "8000000000008002", "8000000000000080", "000000000000800A", "800000008000000A",
        "8000000080008081", "8000000000008080", "0000000080000001", "8000000080008008"
    ).map { BigInteger(it, 16).toLong() }.toLongArray() // Long.parseUnsignedLong needs API 26
    private val ROT = intArrayOf(
        0, 1, 62, 28, 27,
        36, 44, 6, 55, 20,
        3, 10, 43, 25, 39,
        41, 45, 15, 21, 8,
        18, 2, 61, 56, 14
    )

    private fun f(a: LongArray) {
        val b = LongArray(25)
        val c = LongArray(5)
        val d = LongArray(5)
        for (round in 0 until 24) {
            for (x in 0 until 5) c[x] = a[x] xor a[x + 5] xor a[x + 10] xor a[x + 15] xor a[x + 20]
            for (x in 0 until 5) d[x] = c[(x + 4) % 5] xor java.lang.Long.rotateLeft(c[(x + 1) % 5], 1)
            for (i in 0 until 25) a[i] = a[i] xor d[i % 5]
            for (x in 0 until 5) for (y in 0 until 5) {
                val idx = x + 5 * y
                b[y + 5 * ((2 * x + 3 * y) % 5)] = java.lang.Long.rotateLeft(a[idx], ROT[idx])
            }
            for (x in 0 until 5) for (y in 0 until 5) {
                a[x + 5 * y] = b[x + 5 * y] xor (b[(x + 1) % 5 + 5 * y].inv() and b[(x + 2) % 5 + 5 * y])
            }
            a[0] = a[0] xor RC[round]
        }
    }

    internal fun sha3_256(msg: ByteArray): ByteArray {
        val rate = 136
        val state = LongArray(25)
        val padded = ByteArray(((msg.size / rate) + 1) * rate)
        System.arraycopy(msg, 0, padded, 0, msg.size)
        padded[msg.size] = (padded[msg.size].toInt() xor 0x06).toByte()
        padded[padded.size - 1] = (padded[padded.size - 1].toInt() xor 0x80).toByte()
        for (block in 0 until padded.size / rate) {
            for (i in 0 until rate / 8) {
                var lane = 0L
                for (k in 0 until 8) {
                    lane = lane or ((padded[block * rate + i * 8 + k].toLong() and 0xFF) shl (8 * k))
                }
                state[i] = state[i] xor lane
            }
            f(state)
        }
        val out = ByteArray(32)
        for (i in 0 until 32) out[i] = (state[i / 8] ushr (8 * (i % 8))).toByte()
        return out
    }
}
