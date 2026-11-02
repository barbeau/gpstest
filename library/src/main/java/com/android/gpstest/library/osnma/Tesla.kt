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

import java.security.MessageDigest

/*
 * TESLA (Timed Efficient Stream Loss-tolerant Authentication) is the protocol OSNMA uses to
 * authenticate the navigation data (OSNMA SIS ICD section 6). Galileo signs one key of a long
 * one-way hash chain with ECDSA (the TESLA root key, broadcast in the DSM-KROOT). Each 30 s
 * subframe, satellites broadcast MAC tags computed over their navigation data with a secret key
 * of the chain, and disclose that key 30 s later. A receiver checks a disclosed key by hashing it
 * back to a key it already trusts, then uses it to verify the earlier tags. Because keys are
 * disclosed only after the tags, a receiver that knows the time well enough knows that nobody
 * could have computed the tags without the key.
 */

/** Parameters of a TESLA chain, taken from an authenticated DSM-KROOT */
data class TeslaChain(
    val chainId: Int,
    val hashFunction: HashFunction,
    val macFunction: MacFunction,
    val keySizeBytes: Int,
    val tagSizeBits: Int,
    val macLookupTable: Int,
    val alpha: Long
) {
    val keySizeBits: Int get() = keySizeBytes * 8

    /**
     * Checks the ADKD and PRND of Tag-Info [numTag] against the MAC look-up table (ICD Annex C).
     */
    internal fun adkdValid(numTag: Int, tag: TagAndInfo, prna: Int, gstTag: Gst): Boolean {
        val msg = (gstTag.tow / 30) % 2
        return when (val slot = MacLookupTable.slot(macLookupTable, msg, numTag)) {
            null -> false
            is MacLtSlot.Flex -> true
            is MacLtSlot.Fixed -> {
                val prnd = tag.prnd
                tag.adkd == slot.adkd && prnd in 1..36 && (!slot.selfAuth || prnd == prna)
            }
        }
    }

    companion object {
        internal fun fromDsmKroot(dsm: DsmKroot): TeslaChain? {
            return TeslaChain(
                chainId = dsm.chainId,
                hashFunction = dsm.hashFunction ?: return null,
                macFunction = dsm.macFunction ?: return null,
                keySizeBytes = (dsm.keySizeBits ?: return null) / 8,
                tagSizeBits = dsm.tagSizeBits ?: return null,
                macLookupTable = dsm.macLookupTable,
                alpha = dsm.alpha
            )
        }
    }
}

/**
 * A TESLA key of [chain], transmitted in the subframe starting at [gstSubframe]. Only keys that
 * have been authenticated (traced back to a verified KROOT) are stored by the engine.
 */
class TeslaKey internal constructor(
    internal val data: ByteArray,
    val chain: TeslaChain,
    val gstSubframe: Gst
) {
    init {
        require(gstSubframe.isSubframe)
        require(data.size == chain.keySizeBytes)
    }

    private fun hash(input: ByteArray): ByteArray = when (chain.hashFunction) {
        HashFunction.SHA_256 -> OsnmaCrypto.sha256(input)
        HashFunction.SHA3_256 -> OsnmaCrypto.sha3_256(input)
    }

    /** One-way function: the key of the previous subframe (ICD Eq. 9) */
    fun previous(): TeslaKey {
        val prevGst = gstSubframe.addSeconds(-30)
        val w = BitWriter(64)
            .putBytes(data)
            .put(prevGst.wn.toLong(), 12)
            .put(prevGst.tow.toLong(), 20)
            .put(chain.alpha, 48)
        val h = hash(w.toByteArray())
        return TeslaKey(h.copyOf(chain.keySizeBytes), chain, prevGst)
    }

    fun derive(steps: Int): TeslaKey {
        if (steps <= 0) return this
        // Same as calling previous() [steps] times, without creating objects for each step: a
        // year of keys is about a million hashes
        val size = chain.keySizeBytes
        val input = ByteArray(size + 10)
        for (i in 0 until 6) {
            input[size + 4 + i] = (chain.alpha ushr (8 * (5 - i))).toByte()
        }
        val sha256 = if (chain.hashFunction == HashFunction.SHA_256) {
            MessageDigest.getInstance("SHA-256")
        } else {
            null
        }
        var key = data
        var gst = gstSubframe
        repeat(steps) {
            gst = gst.addSeconds(-30)
            System.arraycopy(key, 0, input, 0, size)
            val time = (gst.wn.toLong() shl 20) or gst.tow.toLong()
            for (i in 0 until 4) input[size + i] = (time ushr (8 * (3 - i))).toByte()
            val h = sha256?.digest(input) ?: OsnmaCrypto.sha3_256(input)
            key = h.copyOf(size)
        }
        return TeslaKey(key, chain, gst)
    }

    /**
     * Uses this (authenticated) key to authenticate [other], a later key of the same chain
     * (ICD section 6.4). Returns true if [other] is genuine.
     */
    fun authenticates(other: TeslaKey, maxDerivations: Int = MAX_DERIVATIONS): Boolean {
        if (other.chain != chain || other.gstSubframe <= gstSubframe) return false
        val steps = other.gstSubframe.subframesSince(gstSubframe)
        if (steps > maxDerivations) return false
        return other.derive(steps).data.contentEquals(data)
    }

    private fun mac(message: ByteArray): ByteArray = when (chain.macFunction) {
        MacFunction.HMAC_SHA_256 -> OsnmaCrypto.hmacSha256(data, message)
        MacFunction.CMAC_AES -> OsnmaCrypto.cmacAes(data, message)
    }

    /**
     * Verifies a tag (ICD section 6.7). [prnd] is null for Tag0. [navData] holds [navDataBits]
     * bits; for a dummy tag (COP = 0) pass null to authenticate an all-zero navigation data field.
     */
    internal fun verifyTag(
        mack: ByteArray,
        tagOffset: Int,
        tagLength: Int,
        gstTag: Gst,
        prnd: Int?,
        prna: Int,
        ctr: Int,
        nmaStatus: NmaStatus,
        navData: ByteArray?,
        navDataBits: Int
    ): Boolean {
        val w = BitWriter(96)
        if (prnd != null) w.put(prnd.toLong(), 8)
        w.put(prna.toLong(), 8)
            .put(gstTag.wn.toLong(), 12)
            .put(gstTag.tow.toLong(), 20)
            .put(ctr.toLong(), 8)
            .put(nmaStatus.ordinal.toLong(), 2)
        if (navData != null) w.putBits(navData, 0, navDataBits) else w.putZeros(navDataBits)
        val computed = mac(w.toByteArray())
        return Bits.equal(computed, 0, mack, tagOffset, tagLength)
    }

    /**
     * Verifies the MACSEQ field of [mack] (ICD section 6.6). This key must be the one
     * transmitted in the subframe after the MACK.
     */
    internal fun verifyMacseq(mack: Mack, prna: Int, gstMack: Gst): Boolean {
        val msg = (gstMack.tow / 30) % 2
        val flex = MacLookupTable.flexIndices(chain.macLookupTable, msg) ?: return false
        val w = BitWriter(64)
            .put(prna.toLong(), 8)
            .put(gstMack.wn.toLong(), 12)
            .put(gstMack.tow.toLong(), 20)
        for (idx in flex) {
            w.putBits(mack.data, mack.tag(idx).tagInfoOffset, 16)
        }
        val computed = mac(w.toByteArray())
        return Bits.read(computed, 0, 12).toInt() == mack.macseq
    }

    override fun toString(): String = "TESLA key CID ${chain.chainId} at $gstSubframe"

    fun toStored(): StoredTeslaKey = StoredTeslaKey(chain, data.copyOf(), gstSubframe)

    companion object {
        /** Arbitrary limit (slightly more than one day) on key derivations */
        const val MAX_DERIVATIONS = 3000

        /**
         * Limit when starting from a key stored in an earlier session or a built-in TESLA root
         * key: one year. Deriving that many keys takes a few seconds on a phone, so the engine
         * limits how often it does that (see OsnmaEngine).
         */
        const val MAX_DERIVATIONS_STORED = 366 * 24 * 120
    }
}

/**
 * An authenticated TESLA key with its chain parameters, saved between sessions for a warm start.
 *
 * Any later key of the same chain can be authenticated with it by applying the one-way function
 * (ICD 6.4), so a receiver that remembers it doesn't need to wait for the DSM-KROOT again. It's
 * dropped as soon as the signal says the chain changed or was revoked.
 */
class StoredTeslaKey(val chain: TeslaChain, val key: ByteArray, val gstSubframe: Gst) {

    /** Compact text form for storage */
    fun encode(): String = listOf(
        chain.chainId, chain.hashFunction.name, chain.macFunction.name, chain.keySizeBytes,
        chain.tagSizeBits, chain.macLookupTable, chain.alpha, gstSubframe.wn, gstSubframe.tow,
        Bits.toHex(key)
    ).joinToString("|")

    companion object {
        fun decode(text: String): StoredTeslaKey? = try {
            val f = text.split('|')
            val chain = TeslaChain(
                chainId = f[0].toInt(),
                hashFunction = HashFunction.valueOf(f[1]),
                macFunction = MacFunction.valueOf(f[2]),
                keySizeBytes = f[3].toInt(),
                tagSizeBits = f[4].toInt(),
                macLookupTable = f[5].toInt(),
                alpha = f[6].toLong()
            )
            val gst = Gst(f[7].toInt(), f[8].toInt())
            val key = Bits.fromHex(f[9])
            if (key.size == chain.keySizeBytes && gst.isSubframe) {
                StoredTeslaKey(chain, key, gst)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * A DSM-KROOT message with the NMA header it was broadcast with, as built into the app (see
 * GalileoKeys). It's verified like a received one: its ECDSA signature must check out with an
 * authenticated public key. It lets the first session start without receiving the DSM-KROOT,
 * as long as its chain is still in force.
 */
class BuiltInKroot(val nmaHeader: Int, val dsmKroot: ByteArray) {

    /** Hex text form: NMA header byte followed by the DSM-KROOT */
    fun encode(): String = "%02X".format(nmaHeader) + Bits.toHex(dsmKroot)

    companion object {
        fun decode(hex: String): BuiltInKroot? = try {
            val bytes = Bits.fromHex(hex)
            if (bytes.size > 1) {
                BuiltInKroot(bytes[0].toInt() and 0xFF, bytes.copyOfRange(1, bytes.size))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
