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

/*
 * Field definitions of the OSNMA messages, following the Galileo OSNMA SIS ICD v1.1
 * (https://www.gsc-europa.eu/sites/default/files/sites/all/files/Galileo_OSNMA_SIS_ICD_v1.1.pdf).
 */

/** NMA status (NMAS), ICD section 3.1.1 */
enum class NmaStatus { RESERVED, TEST, OPERATIONAL, DONT_USE }

/** Chain and public key status (CPKS), ICD section 3.1.3 */
enum class Cpks {
    RESERVED, NOMINAL, END_OF_CHAIN, CHAIN_REVOKED, NEW_PUBLIC_KEY, PUBLIC_KEY_REVOKED,
    NEW_MERKLE_TREE, ALERT_MESSAGE
}

/** The NMA header in the first byte of each HKROOT message, ICD Figure 4 */
data class NmaHeader(val raw: Int) {
    val nmaStatus: NmaStatus get() = NmaStatus.values()[(raw ushr 6) and 3]
    val chainId: Int get() = (raw ushr 4) and 3
    val cpks: Cpks get() = Cpks.values()[(raw ushr 1) and 7]
}

enum class HashFunction { SHA_256, SHA3_256 }
enum class MacFunction { HMAC_SHA_256, CMAC_AES }

/** DSM-KROOT message, ICD Figure 7 */
internal class DsmKroot(val data: ByteArray) {
    val numberOfBlocks: Int? get() = Bits.readInt(data, 0, 4).let { if (it in 1..8) it + 6 else null }
    val publicKeyId: Int get() = Bits.readInt(data, 4, 4)
    val chainId: Int get() = Bits.readInt(data, 8, 2)
    val hashFunction: HashFunction?
        get() = when (Bits.readInt(data, 12, 2)) {
            0 -> HashFunction.SHA_256
            2 -> HashFunction.SHA3_256
            else -> null
        }
    val macFunction: MacFunction?
        get() = when (Bits.readInt(data, 14, 2)) {
            0 -> MacFunction.HMAC_SHA_256
            1 -> MacFunction.CMAC_AES
            else -> null
        }

    /** TESLA key size in bits (all values are whole bytes) */
    val keySizeBits: Int?
        get() = when (Bits.readInt(data, 16, 4)) {
            0 -> 96; 1 -> 104; 2 -> 112; 3 -> 120; 4 -> 128; 5 -> 160; 6 -> 192; 7 -> 224; 8 -> 256
            else -> null
        }

    /** MAC tag size in bits */
    val tagSizeBits: Int?
        get() = when (Bits.readInt(data, 20, 4)) {
            5 -> 20; 6 -> 24; 7 -> 28; 8 -> 32; 9 -> 40
            else -> null
        }

    val macLookupTable: Int get() = Bits.readInt(data, 24, 8)
    val krootWn: Int get() = Bits.readInt(data, 36, 12)
    val krootTowh: Int get() = Bits.readInt(data, 48, 8)
    val alpha: Long get() = Bits.read(data, 56, 48)

    val kroot: ByteArray? get() = keySizeBits?.let { data.copyOfRange(13, 13 + it / 8) }

    /** The ECDSA curve is not signalled explicitly; infer it from the DSM length */
    val curve: OsnmaCrypto.Curve?
        get() {
            val krootLen = (keySizeBits ?: return null) / 8
            val remaining = data.size - 13 - krootLen
            fun fits(sigLen: Int): Boolean {
                val padding = (13 - (krootLen + sigLen) % 13) % 13
                return remaining == sigLen + padding
            }
            return when {
                fits(64) -> OsnmaCrypto.Curve.P256
                fits(132) -> OsnmaCrypto.Curve.P521
                else -> null
            }
        }

    val signature: ByteArray?
        get() {
            val c = curve ?: return null
            val start = 13 + keySizeBits!! / 8
            return data.copyOfRange(start, start + 2 * c.coordBytes)
        }

    val padding: ByteArray?
        get() {
            val c = curve ?: return null
            val start = 13 + keySizeBits!! / 8 + 2 * c.coordBytes
            return data.copyOfRange(start, data.size)
        }

    /** Message signed by the ECDSA signature: NMA header || DSM-KROOT fields after NB_DK and PKID */
    fun signedMessage(nmaHeader: NmaHeader): ByteArray {
        val end = 13 + keySizeBits!! / 8
        val m = ByteArray(end)
        m[0] = nmaHeader.raw.toByte()
        System.arraycopy(data, 1, m, 1, end - 1)
        return m
    }

    /** ICD Eq. 7: the padding is a truncated SHA-256 of the signed message and the signature */
    fun paddingOk(nmaHeader: NmaHeader): Boolean {
        val p = padding ?: return false
        val h = OsnmaCrypto.sha256(signedMessage(nmaHeader), signature!!)
        return h.copyOf(p.size).contentEquals(p)
    }
}

/** New public key type (NPKT) in a DSM-PKR, ICD Table 5 */
enum class NewPublicKeyType { P256, P521, ALERT_MESSAGE, RESERVED }

/** DSM-PKR message, ICD Figure 6 */
internal class DsmPkr(val data: ByteArray) {
    val numberOfBlocks: Int? get() = Bits.readInt(data, 0, 4).let { if (it in 7..10) it + 6 else null }
    val messageId: Int get() = Bits.readInt(data, 4, 4)
    fun intermediateTreeNode(n: Int): ByteArray = data.copyOfRange(1 + 32 * n, 1 + 32 * (n + 1))
    val newPublicKeyType: NewPublicKeyType
        get() = when (Bits.readInt(data, 1032, 4)) {
            1 -> NewPublicKeyType.P256
            3 -> NewPublicKeyType.P521
            4 -> NewPublicKeyType.ALERT_MESSAGE
            else -> NewPublicKeyType.RESERVED
        }
    val newPublicKeyId: Int get() = Bits.readInt(data, 1036, 4)

    private val keySize: Int?
        get() = when (newPublicKeyType) {
            NewPublicKeyType.P256 -> 33
            NewPublicKeyType.P521 -> 67
            NewPublicKeyType.ALERT_MESSAGE -> numberOfBlocks?.let { it * 13 - 130 }
            NewPublicKeyType.RESERVED -> null
        }

    val newPublicKey: ByteArray? get() = keySize?.let { data.copyOfRange(130, 130 + it) }

    /** Merkle tree leaf: NPKT || NPKID || NPK */
    val merkleLeaf: ByteArray? get() = keySize?.let { data.copyOfRange(129, 130 + it) }
}

/** ADKD values, ICD Table 14 */
enum class Adkd { INAV_CED, INAV_TIMING, SLOW_MAC, RESERVED }

/** A Tag-Info entry of a MACK message (tag + PRND + ADKD + COP), ICD Figure 11 */
internal class TagAndInfo(private val mack: ByteArray, private val offset: Int, private val tagSizeBits: Int) {
    val tagOffset: Int get() = offset
    val tagLength: Int get() = tagSizeBits
    val tagInfoOffset: Int get() = offset + tagSizeBits
    val prnd: Int get() = Bits.readInt(mack, offset + tagSizeBits, 8)
    val adkd: Adkd
        get() = when (Bits.readInt(mack, offset + tagSizeBits + 8, 4)) {
            0 -> Adkd.INAV_CED
            4 -> Adkd.INAV_TIMING
            12 -> Adkd.SLOW_MAC
            else -> Adkd.RESERVED
        }
    val cop: Int get() = Bits.readInt(mack, offset + tagSizeBits + 12, 4)
}

/** A 480-bit MACK message interpreted with the chain's key and tag sizes, ICD Figure 8 */
internal class Mack(val data: ByteArray, val keySizeBits: Int, val tagSizeBits: Int) {
    val numTags: Int get() = (MACK_BITS - keySizeBits) / (tagSizeBits + 16)
    val macseq: Int get() = Bits.readInt(data, tagSizeBits, 12)
    val cop: Int get() = Bits.readInt(data, tagSizeBits + 12, 4)

    /** Tag-Info [n] for n in 1 until numTags (n = 0 is Tag0, which has no info field) */
    fun tag(n: Int): TagAndInfo {
        require(n in 1 until numTags)
        return TagAndInfo(data, n * (tagSizeBits + 16), tagSizeBits)
    }

    val keyOffset: Int get() = numTags * (tagSizeBits + 16)
    fun key(): ByteArray = Bits.slice(data, keyOffset, keySizeBits)

    companion object {
        const val MACK_BITS = 480
    }
}

/** Entry of the MAC look-up table, ICD Annex C */
internal sealed class MacLtSlot {
    data class Fixed(val adkd: Adkd, val selfAuth: Boolean) : MacLtSlot()
    object Flex : MacLtSlot()
}

internal object MacLookupTable {
    private val F00S = MacLtSlot.Fixed(Adkd.INAV_CED, true)
    private val F00E = MacLtSlot.Fixed(Adkd.INAV_CED, false)
    private val F04S = MacLtSlot.Fixed(Adkd.INAV_TIMING, true)
    private val F12S = MacLtSlot.Fixed(Adkd.SLOW_MAC, true)
    private val F12E = MacLtSlot.Fixed(Adkd.SLOW_MAC, false)
    private val FLX = MacLtSlot.Flex

    private class Entry(val nt: Int, val sequence: Array<Array<MacLtSlot>>)

    // The first slot (always 00S, Tag0) is omitted. Missing slots are filled with FLX.
    private val TABLE: Map<Int, Entry> = mapOf(
        27 to Entry(6, arrayOf(
            arrayOf(F00E, F00E, F00E, F12S, F00E),
            arrayOf(F00E, F00E, F04S, F12S, F00E))),
        28 to Entry(10, arrayOf(
            arrayOf(F00E, F00E, F00E, F00S, F00E, F00E, F12S, F00E, F00E),
            arrayOf(F00E, F00E, F00S, F00E, F00E, F04S, F12S, F00E, F00E))),
        31 to Entry(5, arrayOf(
            arrayOf(F00E, F00E, F12S, F00E),
            arrayOf(F00E, F00E, F12S, F04S))),
        33 to Entry(6, arrayOf(
            arrayOf(F00E, F04S, F00E, F12S, F00E),
            arrayOf(F00E, F00E, F12S, F00E, F12E))),
        34 to Entry(6, arrayOf(
            arrayOf(FLX, F04S, FLX, F12S, F00E),
            arrayOf(FLX, F00E, F12S, F00E, F12E))),
        35 to Entry(6, arrayOf(
            arrayOf(FLX, F04S, FLX, F12S, FLX),
            arrayOf(FLX, FLX, F12S, FLX, FLX))),
        36 to Entry(5, arrayOf(
            arrayOf(FLX, F04S, FLX, F12S),
            arrayOf(FLX, F00E, F12S, F12E))),
        37 to Entry(5, arrayOf(
            arrayOf(F00E, F04S, F00E, F12S),
            arrayOf(F00E, F00E, F12S, F12E))),
        38 to Entry(5, arrayOf(
            arrayOf(FLX, F04S, FLX, F12S),
            arrayOf(FLX, FLX, F12S, FLX))),
        39 to Entry(4, arrayOf(
            arrayOf(FLX, F04S, FLX),
            arrayOf(FLX, F00E, F12S))),
        40 to Entry(4, arrayOf(
            arrayOf(F00E, F04S, F12S),
            arrayOf(F00E, F00E, F12E))),
        41 to Entry(4, arrayOf(
            arrayOf(FLX, F04S, FLX),
            arrayOf(FLX, FLX, F12S)))
    )

    fun isKnown(maclt: Int): Boolean = TABLE.containsKey(maclt)

    /** Slot for tag [numTag] (>= 1), or null if the table or tag number is invalid */
    fun slot(maclt: Int, msg: Int, numTag: Int): MacLtSlot? {
        val entry = TABLE[maclt] ?: return null
        if (numTag < 1 || numTag >= entry.nt) return null
        return entry.sequence[msg][numTag - 1]
    }

    /** Indices (>= 1) of the flexible tags, or null if the table is unknown */
    fun flexIndices(maclt: Int, msg: Int): List<Int>? {
        val entry = TABLE[maclt] ?: return null
        return (1 until entry.nt).filter { entry.sequence[msg][it - 1] == FLX }
    }
}
