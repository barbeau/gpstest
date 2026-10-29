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
        var k = this
        repeat(steps) { k = k.previous() }
        return k
    }

    /**
     * Uses this (authenticated) key to authenticate [other], a later key of the same chain
     * (ICD section 6.4). Returns true if [other] is genuine.
     */
    fun authenticates(other: TeslaKey): Boolean {
        if (other.chain != chain || other.gstSubframe <= gstSubframe) return false
        val steps = other.gstSubframe.subframesSince(gstSubframe)
        if (steps > MAX_DERIVATIONS) return false
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

    companion object {
        /** Arbitrary limit (slightly more than one day) on key derivations */
        const val MAX_DERIVATIONS = 3000
    }
}
