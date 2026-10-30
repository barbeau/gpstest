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
 * Galileo Open Service Navigation Message Authentication (OSNMA) receiver.
 *
 * Feed it every Galileo E1-B I/NAV page with [feedPage]. It reassembles the OSNMA messages,
 * authenticates the TESLA root key with the ECDSA public key (which can itself be authenticated
 * from the signal with the Merkle tree root), follows the TESLA key chain and verifies the MAC
 * tags on the navigation data (ADKD 0, 4 and 12). [status] gives a snapshot for display.
 *
 * The processing follows the Galileo OSNMA SIS ICD v1.1 and Receiver Guidelines v1.3, and is
 * modelled on the galileo-osnma Rust library by Daniel Estévez
 * (https://github.com/daniestevez/galileo-osnma, MIT/Apache-2.0).
 *
 * This class is not thread safe: call it from a single thread.
 */
class OsnmaEngine(
    merkleRoot: ByteArray?,
    initialKeys: List<OsnmaPublicKey> = emptyList(),
    private val listener: Listener? = null,
    /** Only process Slow MAC (ADKD 12) tags, as recommended when time sync is loose */
    private val onlySlowMac: Boolean = false
) {

    interface Listener {
        /** A public key was authenticated from the signal (DSM-PKR); apps may persist it */
        fun onPublicKeyAuthenticated(key: OsnmaPublicKey) {}

        /** An authenticated OSNMA Alert Message was received; all crypto material was erased */
        fun onAlertMessage() {}

        fun onEvent(event: OsnmaEvent) {}
    }

    private var merkleTree: MerkleTree? = merkleRoot?.let { MerkleTree(it) }

    private val subframes = SubframeCollector()
    private val dsmCollector = DsmCollector()
    private val macks = MackStore()
    private val navData = NavDataStore()

    // Public keys: the one in force and possibly the next one (ICD section 5.3)
    private var currentPubKey: OsnmaPublicKey? = null
    private var nextPubKey: OsnmaPublicKey? = null

    // Up to two TESLA chains: the one in force and one about to start (ICD section 5.2)
    private val keySlots = arrayOfNulls<TeslaKey>(2)
    private var chainInForce: Int? = null
    private var chainStart: Gst? = null

    // DSM-KROOT waiting for its public key
    private var pendingKroot: Triple<DsmKroot, NmaHeader, Gst>? = null

    // Status tracking
    private var latestGst: Gst? = null
    private var lastHeader: NmaHeader? = null
    private var lastHeaderAuthenticated = false
    private var krootGst: Gst? = null
    private var alertReceived = false
    private val satellites = HashMap<Int, SatTracking>()
    private val events = ArrayDeque<OsnmaEvent>()
    private val stats = OsnmaStatistics()

    private class AuthRecord(val data: ByteArray, var bits: Int, var lastGst: Gst)

    private class SatTracking {
        var lastPageGst: Gst? = null
        var transmitsOsnma = false
        var lastOsnmaGst: Gst? = null
        var ced: AuthRecord? = null
        var timing: AuthRecord? = null
        var tagsOk = 0
        var tagsFailed = 0
        var lastFailureGst: Gst? = null
    }

    init {
        initialKeys.sortedBy { it.pkid }.forEach { storePublicKey(it, notify = false) }
    }

    // ------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------

    /**
     * Processes one I/NAV page from satellite [svn] (1-36), where [gst] is the GST at the start
     * of the page (page TOWs are odd). Pages from all satellites must be fed in time order.
     *
     * @return false if the page was discarded (not a nominal page or bad CRC)
     */
    fun feedPage(svn: Int, gst: Gst, page: InavPage, checkCrc: Boolean = true): Boolean {
        if (svn !in 1..36) return false
        if (!page.isNominal) {
            stats.pagesRejected++
            return false
        }
        if (checkCrc && !page.crcOk) {
            stats.crcErrors++
            return false
        }
        stats.pages++
        if (latestGst.let { it == null || gst > it }) latestGst = gst
        val sat = satellites.getOrPut(svn) { SatTracking() }
        sat.lastPageGst = gst

        // OSNMA isn't provided in Dummy Messages (word type 63); their OSNMA field may not be
        // zero but is invalid (OSNMA SIS ICD section 2)
        if (page.wordType == DUMMY_WORD_TYPE) return true

        navData.feed(svn, gst.subframe(), page.word)

        if (page.osnmaEmpty) return true
        stats.osnmaPages++
        sat.transmitsOsnma = true
        sat.lastOsnmaGst = gst
        subframes.feed(page.osnma, svn, gst)?.let {
            processSubframe(it.hkroot, it.mack, svn, it.gstSubframe)
        }
        return true
    }

    /** Adds a public key from outside the signal (e.g. imported by the user) */
    fun addPublicKey(key: OsnmaPublicKey) {
        storePublicKey(key, notify = false)
        retryPendingKroot()
    }

    fun setMerkleRoot(root: ByteArray) {
        merkleTree = MerkleTree(root)
    }

    // ------------------------------------------------------------------------------------------
    // Subframe processing
    // ------------------------------------------------------------------------------------------

    private fun processSubframe(hkroot: ByteArray, mack: ByteArray, svn: Int, gst: Gst) {
        stats.subframes++
        val header = NmaHeader(hkroot[0].toInt() and 0xFF)
        if (header != lastHeader) lastHeaderAuthenticated = false
        lastHeader = header
        // The NMA status isn't authenticated here. It's only stored and later used as an input to
        // the tag MACs, so a forged status just makes the tags fail.
        macks.store(mack, svn, gst, header.nmaStatus)

        val block = hkroot.copyOfRange(2, 15)
        dsmCollector.feed(hkroot[1].toInt() and 0xFF, block)?.let { dsm ->
            if (dsm.isPkr) processDsmPkr(DsmPkr(dsm.data), gst) else processDsmKroot(DsmKroot(dsm.data), header, gst)
        }
        validateKey(mack, gst)
    }

    private fun processDsmKroot(dsm: DsmKroot, header: NmaHeader, gst: Gst) {
        val pkid = dsm.publicKeyId
        val pubKey = applicablePubKey(pkid)
        if (pubKey == null) {
            pendingKroot = Triple(dsm, header, gst)
            event(gst, OsnmaEvent.Type.INFO, "DSM-KROOT received, waiting for public key PKID $pkid")
            return
        }
        pendingKroot = null
        val chain = TeslaChain.fromDsmKroot(dsm)
        val curve = dsm.curve
        val error = when {
            chain == null -> "reserved value in DSM-KROOT"
            curve == null -> "unknown DSM-KROOT signature length"
            curve != pubKey.curve -> "public key type doesn't match DSM-KROOT"
            !dsm.paddingOk(header) -> "wrong DSM-KROOT padding"
            !OsnmaCrypto.verifyEcdsa(curve, pubKey.key, dsm.signedMessage(header), dsm.signature!!) ->
                "invalid DSM-KROOT signature"
            else -> null
        }
        if (error != null) {
            stats.krootFailed++
            event(gst, OsnmaEvent.Type.ERROR, "Could not verify KROOT with PKID $pkid: $error")
            return
        }
        stats.krootVerified++
        makePubKeyCurrent(pkid)
        // The KROOT is the key of the subframe before WN_K / TOWH_K
        val krootStart = Gst(dsm.krootWn, dsm.krootTowh * 3600)
        val kroot = TeslaKey(dsm.kroot!!, chain!!, krootStart.addSeconds(-30))
        if (krootGst == null) {
            event(gst, OsnmaEvent.Type.SUCCESS, "KROOT of chain ${chain.chainId} verified with public key PKID $pkid")
        }
        krootGst = gst
        lastHeader = header
        lastHeaderAuthenticated = true
        storeKroot(kroot, header, gst)
        processNmaHeader(header, pkid, gst)
    }

    private fun retryPendingKroot() {
        val (dsm, header, gst) = pendingKroot ?: return
        if (applicablePubKey(dsm.publicKeyId) != null) processDsmKroot(dsm, header, gst)
    }

    private fun processNmaHeader(header: NmaHeader, pkid: Int, gst: Gst) {
        if (header.nmaStatus == NmaStatus.DONT_USE) {
            when (header.cpks) {
                Cpks.CHAIN_REVOKED -> revokeChain(header.chainId)
                Cpks.PUBLIC_KEY_REVOKED -> {
                    // The KROOT already refers to the new key; revoke the older ones
                    revokePubKeysBefore(pkid)
                    revokeChain(header.chainId)
                }
                else -> {}
            }
        }
        when (header.cpks) {
            Cpks.CHAIN_REVOKED -> event(gst, OsnmaEvent.Type.WARNING, "TESLA chain revoked")
            Cpks.PUBLIC_KEY_REVOKED -> event(gst, OsnmaEvent.Type.WARNING, "Public key revoked")
            Cpks.NEW_MERKLE_TREE -> event(gst, OsnmaEvent.Type.WARNING, "New Merkle tree announced")
            Cpks.ALERT_MESSAGE -> alertMessageReceived(gst)
            else -> {}
        }
    }

    private fun alertMessageReceived(gst: Gst) {
        if (!alertReceived) {
            event(gst, OsnmaEvent.Type.ERROR, "OSNMA Alert Message: all cryptographic material erased")
        }
        alertReceived = true
        merkleTree = null
        currentPubKey = null
        nextPubKey = null
        keySlots.fill(null)
        chainInForce = null
        chainStart = null
        pendingKroot = null
        listener?.onAlertMessage()
    }

    private fun processDsmPkr(pkr: DsmPkr, gst: Gst) {
        val tree = merkleTree
        if (tree == null) {
            event(gst, OsnmaEvent.Type.INFO, "DSM-PKR received but no Merkle tree root is loaded")
            return
        }
        when (pkr.newPublicKeyType) {
            NewPublicKeyType.P256, NewPublicKeyType.P521 -> {
                val key = tree.publicKeyFrom(pkr)
                if (key == null) {
                    stats.pkrFailed++
                    event(gst, OsnmaEvent.Type.ERROR, "Could not verify public key PKID ${pkr.newPublicKeyId} against the Merkle tree")
                } else {
                    stats.pkrVerified++
                    val isNew = storePublicKey(key, notify = true)
                    if (isNew) {
                        event(gst, OsnmaEvent.Type.SUCCESS, "Public key PKID ${key.pkid} received and verified against the Merkle tree")
                    }
                    retryPendingKroot()
                }
            }
            NewPublicKeyType.ALERT_MESSAGE -> {
                if (tree.verifies(pkr)) alertMessageReceived(gst)
                else event(gst, OsnmaEvent.Type.ERROR, "Could not verify OSNMA Alert Message")
            }
            NewPublicKeyType.RESERVED -> event(gst, OsnmaEvent.Type.ERROR, "Reserved NPKT in DSM-PKR")
        }
    }

    // ------------------------------------------------------------------------------------------
    // Public key store
    // ------------------------------------------------------------------------------------------

    private fun applicablePubKey(pkid: Int): OsnmaPublicKey? = when (pkid) {
        currentPubKey?.pkid -> currentPubKey
        nextPubKey?.pkid -> nextPubKey
        else -> null
    }

    private fun makePubKeyCurrent(pkid: Int) {
        if (currentPubKey?.pkid == pkid) return
        if (nextPubKey?.pkid == pkid) {
            currentPubKey = nextPubKey
            nextPubKey = null
        }
    }

    /** Returns true if the key wasn't known before */
    private fun storePublicKey(key: OsnmaPublicKey, notify: Boolean): Boolean {
        val current = currentPubKey
        val isNew: Boolean
        if (current == null) {
            currentPubKey = key
            isNew = true
        } else if (key.pkid < current.pkid) {
            return false
        } else if (key.pkid == current.pkid) {
            // Prefer a key authenticated by the Merkle tree over an imported one
            if (key.sec1.contentEquals(current.sec1) && current.source == KeySource.IMPORTED &&
                key.source != KeySource.IMPORTED) {
                currentPubKey = key
            }
            return false
        } else {
            val next = nextPubKey
            isNew = next == null || key.pkid > next.pkid
            if (isNew) nextPubKey = key
        }
        if (isNew && notify) listener?.onPublicKeyAuthenticated(key)
        return isNew
    }

    private fun revokePubKeysBefore(pkid: Int) {
        if ((currentPubKey?.pkid ?: Int.MAX_VALUE) < pkid) currentPubKey = null
        if ((nextPubKey?.pkid ?: Int.MAX_VALUE) < pkid) nextPubKey = null
        if (currentPubKey == null && nextPubKey != null) {
            currentPubKey = nextPubKey
            nextPubKey = null
        }
    }

    // ------------------------------------------------------------------------------------------
    // TESLA key store
    // ------------------------------------------------------------------------------------------

    private fun storeKroot(key: TeslaKey, header: NmaHeader, gst: Gst) {
        val kid = key.chain.chainId
        val cid = header.chainId
        val k0 = keySlots[0]
        val k1 = keySlots[1]
        when {
            k0 != null && k0.chain.chainId == kid -> {}
            k1 != null && k1.chain.chainId == kid -> {}
            k0 == null -> keySlots[0] = key
            k1 == null -> keySlots[1] = key
            // Both occupied: don't overwrite the chain in force
            k0.chain.chainId == cid -> keySlots[1] = key
            else -> keySlots[0] = key
        }
        val previous = chainInForce
        if (previous != null && previous != cid) {
            chainStart = gst
            event(gst, OsnmaEvent.Type.INFO, "TESLA chain $cid now in force")
        } else if (previous == null) {
            chainStart = null
        }
        chainInForce = cid
    }

    private fun storeKey(key: TeslaKey) {
        for (i in keySlots.indices) {
            if (keySlots[i]?.chain?.chainId == key.chain.chainId) {
                keySlots[i] = key
                return
            }
        }
    }

    private fun currentKey(): TeslaKey? {
        val cid = chainInForce ?: return null
        return keySlots.firstOrNull { it != null && it.chain.chainId == cid }
    }

    /** Like [currentKey], but returns the previous chain's key for times before the switch */
    private fun keyForGst(gst: Gst): TeslaKey? {
        val cid = chainInForce ?: return null
        val start = chainStart
        return if (start != null && start > gst) {
            keySlots.firstOrNull { it != null && it.chain.chainId != cid }
        } else currentKey()
    }

    private fun revokeChain(cid: Int) {
        for (i in keySlots.indices) {
            if (keySlots[i]?.chain?.chainId == cid) keySlots[i] = null
        }
    }

    private fun validateKey(mackBytes: ByteArray, gst: Gst) {
        val current = currentKey() ?: return
        val chain = current.chain
        val mack = Mack(mackBytes, chain.keySizeBits, chain.tagSizeBits)
        val newKey = TeslaKey(mack.key(), chain, gst)
        when {
            current.gstSubframe == newKey.gstSubframe -> {}
            current.gstSubframe > newKey.gstSubframe -> {}
            current.authenticates(newKey) -> {
                stats.teslaKeysVerified++
                storeKey(newKey)
                processTags(newKey)
            }
            else -> {
                stats.teslaKeysFailed++
                event(gst, OsnmaEvent.Type.ERROR, "TESLA key at $gst could not be verified")
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Tag verification
    // ------------------------------------------------------------------------------------------

    private fun processTags(key: TeslaKey) {
        val gstMack = key.gstSubframe.addSeconds(-30)
        val gstSlowMac = gstMack.addSeconds(-300)
        // The key that authenticates the MACSEQ of the Slow MAC MACK may be from the previous chain
        val gstKeySlowMac = key.gstSubframe.addSeconds(-300)
        val slowMacKey = keyForGst(gstKeySlowMac)?.let { k ->
            val steps = k.gstSubframe.subframesSince(gstKeySlowMac)
            if (steps >= 0) k.derive(steps) else null
        }
        for (svn in 1..36) {
            if (!onlySlowMac) {
                macks.get(svn, gstMack)?.let { stored ->
                    val mack = Mack(stored.mack, key.chain.keySizeBits, key.chain.tagSizeBits)
                    if (mackValid(mack, key, svn, gstMack)) {
                        processMack(mack, key, svn, gstMack, stored.nmaStatus)
                    }
                }
            }
            if (slowMacKey != null) {
                macks.get(svn, gstSlowMac)?.let { stored ->
                    val mack = Mack(stored.mack, key.chain.keySizeBits, key.chain.tagSizeBits)
                    // slowMacKey authenticates the MACK; the current key authenticates its tags
                    if (mackValid(mack, slowMacKey, svn, gstSlowMac)) {
                        processMackSlowMac(mack, key, svn, gstSlowMac, stored.nmaStatus)
                    }
                }
            }
        }
    }

    private fun mackValid(mack: Mack, key: TeslaKey, prna: Int, gstMack: Gst): Boolean {
        if (!MacLookupTable.isKnown(key.chain.macLookupTable)) {
            stats.mackFailed++
            return false
        }
        if (!key.verifyMacseq(mack, prna, gstMack)) {
            stats.mackFailed++
            event(gstMack, OsnmaEvent.Type.ERROR, "Wrong MACSEQ from E%02d".format(prna))
            return false
        }
        for (j in 1 until mack.numTags) {
            if (!key.chain.adkdValid(j, mack.tag(j), prna, gstMack)) {
                stats.mackFailed++
                event(gstMack, OsnmaEvent.Type.ERROR, "Tag $j from E%02d doesn't match the MAC look-up table".format(prna))
                return false
            }
        }
        return true
    }

    private fun processMack(mack: Mack, key: TeslaKey, prna: Int, gstMack: Gst, nmaStatus: NmaStatus) {
        val gstNav = gstMack.addSeconds(-30)
        // Tag0: self-authentication of the CED and status of the transmitting satellite
        verifyTagFor(key, mack, 0, mack.cop, Adkd.INAV_CED, prna, prna, gstMack, gstNav, nmaStatus)
        for (j in 1 until mack.numTags) {
            val tag = mack.tag(j)
            if (tag.adkd == Adkd.INAV_CED || tag.adkd == Adkd.INAV_TIMING) {
                if (tag.prnd !in 1..36) continue
                verifyTagFor(key, mack, j, tag.cop, tag.adkd, tag.prnd, prna, gstMack, gstNav, nmaStatus)
            }
            // Slow MAC tags need a key with an extra delay; see processMackSlowMac
        }
    }

    private fun processMackSlowMac(mack: Mack, key: TeslaKey, prna: Int, gstMack: Gst, nmaStatus: NmaStatus) {
        val gstNav = gstMack.addSeconds(-30)
        for (j in 1 until mack.numTags) {
            val tag = mack.tag(j)
            if (tag.adkd != Adkd.SLOW_MAC || tag.prnd !in 1..36) continue
            verifyTagFor(key, mack, j, tag.cop, Adkd.SLOW_MAC, tag.prnd, prna, gstMack, gstNav, nmaStatus)
        }
    }

    private fun verifyTagFor(
        key: TeslaKey,
        mack: Mack,
        index: Int,
        cop: Int,
        adkd: Adkd,
        prnd: Int,
        prna: Int,
        gstTag: Gst,
        gstNav: Gst,
        nmaStatus: NmaStatus
    ) {
        val tagOffset = if (index == 0) 0 else mack.tag(index).tagOffset
        val tagLength = key.chain.tagSizeBits
        val isTiming = adkd == Adkd.INAV_TIMING
        val navBits = if (isTiming) NavDataStore.TIMING_BITS else NavDataStore.CED_BITS
        val tagPrnd = if (index == 0) null else prnd
        val ctr = index + 1
        if (cop == 0) {
            // Dummy tag: the navigation data field is all zeros
            val ok = key.verifyTag(mack.data, tagOffset, tagLength, gstTag, tagPrnd, prna, ctr, nmaStatus, null, navBits)
            if (ok) stats.dummyTagsOk++ else stats.dummyTagsFailed++
            return
        }
        val nav = if (isTiming) navData.timing(prnd, gstNav) else navData.cedAndStatus(prnd, gstNav)
        // The cut-off point (COP) limits how old the authenticated data can be
        if (nav == null || nav.maxAge + 1 > cop) return
        val ok = key.verifyTag(mack.data, tagOffset, tagLength, gstTag, tagPrnd, prna, ctr, nmaStatus, nav.bits, navBits)
        val sat = satellites.getOrPut(prnd) { SatTracking() }
        if (ok) {
            stats.tagsOk++
            when (adkd) {
                Adkd.INAV_CED -> stats.tagsOkAdkd0++
                Adkd.INAV_TIMING -> stats.tagsOkAdkd4++
                Adkd.SLOW_MAC -> stats.tagsOkAdkd12++
                else -> {}
            }
            sat.tagsOk++
            // Only an operational or test NMA status lets the tag count towards authentication
            if (nmaStatus == NmaStatus.OPERATIONAL || nmaStatus == NmaStatus.TEST) {
                val rec = if (isTiming) sat.timing else sat.ced
                if (rec != null && rec.data.contentEquals(nav.bits)) {
                    rec.bits += tagLength
                    if (gstTag > rec.lastGst) rec.lastGst = gstTag
                } else {
                    val fresh = AuthRecord(nav.bits, tagLength, gstTag)
                    if (isTiming) sat.timing = fresh else sat.ced = fresh
                }
            }
        } else {
            stats.tagsFailed++
            sat.tagsFailed++
            sat.lastFailureGst = gstTag
            event(gstTag, OsnmaEvent.Type.ERROR, "Tag for E%02d (%s) sent by E%02d failed verification".format(prnd, adkd.label(), prna))
        }
    }

    private fun Adkd.label(): String = when (this) {
        Adkd.INAV_CED -> "ADKD 0"
        Adkd.INAV_TIMING -> "ADKD 4"
        Adkd.SLOW_MAC -> "ADKD 12"
        Adkd.RESERVED -> "reserved ADKD"
    }

    // ------------------------------------------------------------------------------------------
    // Status
    // ------------------------------------------------------------------------------------------

    private fun event(gst: Gst, type: OsnmaEvent.Type, message: String) {
        val e = OsnmaEvent(gst, type, message)
        // Avoid flooding the log with the same message
        if (events.lastOrNull()?.let { it.type == type && it.message == message } == true) return
        events.addLast(e)
        while (events.size > MAX_EVENTS) events.removeFirst()
        listener?.onEvent(e)
    }

    val statistics: OsnmaStatistics
        get() = stats.copy(
            subframesStarted = subframes.started,
            subframesCompletedCounted = stats.subframes - subframes.firstCompleted
        )

    fun status(): OsnmaStatus {
        val now = latestGst
        val current = currentKey()
        val sats = satellites.entries.sortedBy { it.key }.map { (svn, s) ->
            val latestCed = now?.let { navData.cedAndStatus(svn, it.subframe()) }
            SatelliteAuthStatus(
                svn = svn,
                transmitsOsnma = s.transmitsOsnma && s.lastOsnmaGst.recent(now, 120),
                cedAuthBits = s.ced?.bits ?: 0,
                cedAuthGst = s.ced?.lastGst,
                cedCurrent = s.ced != null && latestCed != null && s.ced!!.data.contentEquals(latestCed.bits),
                timingAuthBits = s.timing?.bits ?: 0,
                timingAuthGst = s.timing?.lastGst,
                tagsOk = s.tagsOk,
                tagsFailed = s.tagsFailed,
                lastFailureGst = s.lastFailureGst,
                lastPageGst = s.lastPageGst
            )
        }
        return OsnmaStatus(
            gst = now,
            merkleRootLoaded = merkleTree != null,
            publicKey = currentPubKey,
            nextPublicKey = nextPubKey,
            nmaHeader = lastHeader,
            nmaHeaderAuthenticated = lastHeaderAuthenticated,
            krootGst = krootGst,
            chainInForce = chainInForce,
            teslaKey = current?.let { TeslaKeyInfo(it.chain, it.gstSubframe) },
            alertMessage = alertReceived,
            satellites = sats,
            events = events.toList(),
            statistics = statistics
        )
    }

    private fun Gst?.recent(now: Gst?, seconds: Int): Boolean =
        this != null && now != null && now.totalSeconds - totalSeconds <= seconds

    companion object {
        const val MAX_EVENTS = 50
        private const val DUMMY_WORD_TYPE = 63
    }
}

class OsnmaEvent(val gst: Gst, val type: Type, val message: String) {
    enum class Type { INFO, SUCCESS, WARNING, ERROR }

    override fun toString(): String = "[$gst] $type $message"
}

data class OsnmaStatistics(
    var pages: Long = 0,
    var pagesRejected: Long = 0,
    var crcErrors: Long = 0,
    var osnmaPages: Long = 0,
    var subframes: Long = 0,
    /**
     * (Satellite, subframe) pairs with OSNMA data. [subframes] counts the ones received completely
     * (all 15 pages); a low ratio means pages are missing.
     */
    var subframesStarted: Long = 0,
    /** Completed subframes counted against [subframesStarted] */
    var subframesCompletedCounted: Long = 0,
    var krootVerified: Int = 0,
    var krootFailed: Int = 0,
    var pkrVerified: Int = 0,
    var pkrFailed: Int = 0,
    var teslaKeysVerified: Int = 0,
    var teslaKeysFailed: Int = 0,
    var mackFailed: Int = 0,
    var tagsOk: Int = 0,
    var tagsFailed: Int = 0,
    var tagsOkAdkd0: Int = 0,
    var tagsOkAdkd4: Int = 0,
    var tagsOkAdkd12: Int = 0,
    var dummyTagsOk: Int = 0,
    var dummyTagsFailed: Int = 0
) {
    /** Share of subframes received completely, or null if there isn't enough data yet */
    val subframeCompleteness: Double?
        get() = if (subframesStarted >= MIN_SUBFRAMES_FOR_RATIO) {
            subframesCompletedCounted.toDouble() / subframesStarted
        } else null

    companion object {
        const val MIN_SUBFRAMES_FOR_RATIO = 20L
    }
}

data class TeslaKeyInfo(val chain: TeslaChain, val gstSubframe: Gst)

data class SatelliteAuthStatus(
    val svn: Int,
    /** OSNMA data was seen from this satellite recently */
    val transmitsOsnma: Boolean,
    /** Accumulated tag bits authenticating the last authenticated CED and status (ADKD 0/12) */
    val cedAuthBits: Int,
    val cedAuthGst: Gst?,
    /** The authenticated CED is the one the satellite is currently broadcasting */
    val cedCurrent: Boolean,
    val timingAuthBits: Int,
    val timingAuthGst: Gst?,
    val tagsOk: Int,
    val tagsFailed: Int,
    val lastFailureGst: Gst?,
    val lastPageGst: Gst?
) {
    /**
     * Ephemeris, clock and health are authenticated: at least [MIN_AUTH_BITS] tag bits verified
     * (Receiver Guidelines section 3.4) on the data currently broadcast.
     */
    val cedAuthenticated: Boolean get() = cedAuthBits >= MIN_AUTH_BITS && cedCurrent

    val timingAuthenticated: Boolean get() = timingAuthBits >= MIN_AUTH_BITS

    companion object {
        /** Minimum equivalent tag length, 40 bits since January 2024 */
        const val MIN_AUTH_BITS = 40
    }
}

data class OsnmaStatus(
    val gst: Gst?,
    val merkleRootLoaded: Boolean,
    val publicKey: OsnmaPublicKey?,
    val nextPublicKey: OsnmaPublicKey?,
    val nmaHeader: NmaHeader?,
    /** The NMA header was part of a DSM-KROOT whose signature was verified */
    val nmaHeaderAuthenticated: Boolean,
    val krootGst: Gst?,
    val chainInForce: Int?,
    val teslaKey: TeslaKeyInfo?,
    val alertMessage: Boolean,
    val satellites: List<SatelliteAuthStatus>,
    val events: List<OsnmaEvent>,
    val statistics: OsnmaStatistics
) {
    val authenticatedSatellites: List<SatelliteAuthStatus>
        get() = satellites.filter { it.cedAuthenticated }

    /** Overall state, in order of progress */
    val phase: Phase
        get() = when {
            alertMessage -> Phase.ALERT
            gst == null -> Phase.NO_DATA
            statistics.osnmaPages == 0L -> Phase.NO_OSNMA
            nmaHeader?.nmaStatus == NmaStatus.DONT_USE -> Phase.DONT_USE
            publicKey == null -> Phase.WAITING_FOR_PUBLIC_KEY
            teslaKey == null -> Phase.WAITING_FOR_KROOT
            authenticatedSatellites.isEmpty() -> Phase.WAITING_FOR_TAGS
            else -> Phase.AUTHENTICATING
        }

    enum class Phase {
        NO_DATA, NO_OSNMA, WAITING_FOR_PUBLIC_KEY, WAITING_FOR_KROOT, WAITING_FOR_TAGS,
        AUTHENTICATING, DONT_USE, ALERT
    }
}
