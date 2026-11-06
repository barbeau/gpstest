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
 * Phones often deliver only part of the pages, so the OSNMA messages are used page by page
 * instead of per complete subframe (see [PageStore]): DSM blocks are assembled byte by byte across
 * satellites, the TESLA key of a subframe from the pages of any satellite, and each tag as soon as
 * its own pages are there. A TESLA key verified in an earlier session can be passed in as
 * [storedKey] ("warm start"), so that new keys can be verified without waiting for the DSM-KROOT.
 * Signed DSM-KROOT messages built into the app ([builtInKroots]) serve the same purpose for the
 * first session, after their signature has been verified.
 *
 * This class is not thread safe: call it from a single thread.
 */
class OsnmaEngine(
    merkleRoot: ByteArray?,
    initialKeys: List<OsnmaPublicKey> = emptyList(),
    private val listener: Listener? = null,
    /** How well the receiver knows GST independently of the signal; see [timeSync] */
    timeSync: TimeSync = TimeSync.FULL,
    /** A TESLA key verified in an earlier session, to start without the DSM-KROOT */
    storedKey: StoredTeslaKey? = null,
    /** DSM bytes collected in an earlier session (see [exportDsm]) */
    storedDsm: String? = null,
    /** Signed DSM-KROOT messages known in advance, verified before use (see [BuiltInKroot]) */
    builtInKroots: List<BuiltInKroot> = emptyList()
) {

    interface Listener {
        /** A public key was authenticated from the signal (DSM-PKR); apps may persist it */
        fun onPublicKeyAuthenticated(key: OsnmaPublicKey) {}

        /** An authenticated OSNMA Alert Message was received; all crypto material was erased */
        fun onAlertMessage() {}

        fun onEvent(event: OsnmaEvent) {}

        /** A TESLA key was verified; apps may persist it for a warm start (see [StoredTeslaKey]) */
        fun onTeslaKeyVerified(key: StoredTeslaKey) {}

        /** The key passed as storedKey can't be used (chain changed or revoked); forget it */
        fun onStoredKeyDiscarded() {}

        /** A DSM-KROOT was verified; it can be built into apps as a [BuiltInKroot] */
        fun onKrootVerified(kroot: BuiltInKroot) {}
    }

    private var merkleTree: MerkleTree? = merkleRoot?.let { MerkleTree(it) }

    /**
     * Which tags may be used, given how well the receiver knows the time (OSNMA Receiver
     * Guidelines section 2.1). TESLA keys are verified in any case. When it gets stricter, what
     * was authenticated with tags no longer allowed is forgotten.
     */
    var timeSync: TimeSync = timeSync
        set(value) {
            if (value.ordinal > field.ordinal) {
                satellites.values.forEach { s ->
                    s.timing = null
                    if (value == TimeSync.NONE) s.ced = null
                }
            }
            field = value
        }

    private val pageStore = PageStore()
    private val dsmAssembler = DsmAssembler()
    private val navData = NavDataStore()

    // Attempts to assemble and verify the TESLA key of each subframe
    private val keyAttempts = HashMap<Gst, Int>()

    // Warm start: the chain in force comes from a key stored in an earlier session, and no
    // DSM-KROOT has been verified yet in this one
    private var warmStart = false
    private var warmStartBuiltIn = false
    private var warmStartFailures = 0

    // Verifying a key against an old one takes up to a million hashes: not more often than this
    private var lastLongDerivation: Gst? = null
    private var lastKrootDsmId: Int? = null
    private var krootDsmIdInProcess: Int? = null
    private var pkrDsmIdInProcess: Int? = null

    // Public keys: the one in force and possibly the next one (ICD section 5.3)
    private var currentPubKey: OsnmaPublicKey? = null
    private var nextPubKey: OsnmaPublicKey? = null

    // Up to two TESLA chains: the one in force and one about to start (ICD section 5.2)
    private val keySlots = arrayOfNulls<TeslaKey>(2)
    private var chainInForce: Int? = null
    private var chainStart: Gst? = null

    // DSM-KROOT waiting for its public key
    private var pendingKroot: Triple<DsmKroot, List<NmaHeader>, Gst>? = null

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
        var startKey = storedKey?.let { TeslaKey(it.key, it.chain, it.gstSubframe) }
        for (builtIn in builtInKroots) {
            val key = verifyBuiltInKroot(builtIn) ?: continue
            if (startKey == null || key.gstSubframe > startKey.gstSubframe) {
                startKey = key
                warmStartBuiltIn = true
            }
        }
        startKey?.let {
            keySlots[0] = it
            chainInForce = it.chain.chainId
            warmStart = true
        }
        storedDsm?.let { dsmAssembler.import(it) }
    }

    /**
     * Checks a built-in DSM-KROOT like a received one and returns its TESLA root key, or null if
     * it can't be used (unknown public key, wrong signature, or a header that rules it out)
     */
    private fun verifyBuiltInKroot(builtIn: BuiltInKroot): TeslaKey? {
        val header = NmaHeader(builtIn.nmaHeader)
        if (header.nmaStatus == NmaStatus.DONT_USE || header.cpks == Cpks.CHAIN_REVOKED ||
            header.cpks == Cpks.PUBLIC_KEY_REVOKED || header.cpks == Cpks.ALERT_MESSAGE
        ) {
            return null
        }
        val dsm = DsmKroot(builtIn.dsmKroot)
        val pubKey = applicablePubKey(dsm.publicKeyId) ?: return null
        val chain = TeslaChain.fromDsmKroot(dsm) ?: return null
        val curve = dsm.curve ?: return null
        val signature = dsm.signature ?: return null
        val kroot = dsm.kroot ?: return null
        if (curve != pubKey.curve || !dsm.paddingOk(header)) return null
        if (!OsnmaCrypto.verifyEcdsa(curve, pubKey.key, dsm.signedMessage(header), signature)) {
            return null
        }
        val krootStart = Gst(dsm.krootWn, dsm.krootTowh * 3600)
        event(
            krootStart,
            OsnmaEvent.Type.INFO,
            "Built-in TESLA root key of chain ${chain.chainId} verified with PKID ${pubKey.pkid}"
        )
        return TeslaKey(kroot, chain, krootStart.addSeconds(-30))
    }

    /**
     * DSM bytes collected so far, so that a later session can continue collecting the TESLA root
     * key instead of starting over. Whatever is restored is still verified (ECDSA signature,
     * Merkle tree), and bytes of a DSM that fails verification are dropped.
     */
    fun exportDsm(): String = dsmAssembler.export()

    // ------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------

    /**
     * Processes one I/NAV page from satellite [svn] (1-36), where [gst] is the GST at the start
     * of the page (page TOWs are odd). Pages from all satellites must be fed in time order.
     * Pass timeCertain = false if [gst] might be off by a page (see
     * InavTimeTracker.lastTimeCertain):
     * the page is then not used for OSNMA data.
     *
     * @return false if the page was discarded (not a nominal page or bad CRC)
     */
    fun feedPage(
        svn: Int,
        gst: Gst,
        page: InavPage,
        checkCrc: Boolean = true,
        timeCertain: Boolean = true
    ): Boolean {
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
        // With pages used one by one, a page in the wrong slot would corrupt the MACK bits
        if (!timeCertain) return true
        sat.transmitsOsnma = true
        sat.lastOsnmaGst = gst
        processOsnmaField(svn, gst, page.osnma)
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

    private fun processOsnmaField(svn: Int, gst: Gst, osnma: ByteArray) {
        val stored = pageStore.store(svn, gst, osnma)
        val pages = stored.pages
        val subframe = pages.gstSubframe
        if (stored.newlyComplete) stats.subframes++
        if (stored.page == 0) processNmaHeaderPage(NmaHeader(osnma[0].toInt() and 0xFF), subframe)

        // DSM bytes: usable once the DSM header (page 1) of this satellite's subframe is known
        when {
            stored.page == 1 -> for (i in 2 until SubframeLayout.PAGES) {
                if (pages.has(i)) writeDsmByte(pages, i)
            }
            stored.page >= 2 && pages.has(1) -> writeDsmByte(pages, stored.page)
        }
        tryTeslaKey(subframe, stored.page)
    }

    private fun processNmaHeaderPage(header: NmaHeader, subframe: Gst) {
        if (header != lastHeader) lastHeaderAuthenticated = false
        lastHeader = header
        if (warmStart) {
            val cid = chainInForce
            val reason = when {
                header.nmaStatus == NmaStatus.DONT_USE -> "NMA status is don't use"
                header.cpks == Cpks.CHAIN_REVOKED || header.cpks == Cpks.PUBLIC_KEY_REVOKED ||
                        header.cpks == Cpks.ALERT_MESSAGE -> "chain or key revoked"
                cid != null && header.chainId != cid -> "chain ${header.chainId} is now in force"
                else -> null
            }
            if (reason != null) discardStoredKey(subframe, reason)
        }
    }

    private fun discardStoredKey(gst: Gst, reason: String) {
        val cid = chainInForce
        if (cid != null) revokeChain(cid)
        chainInForce = null
        chainStart = null
        warmStart = false
        val source = if (warmStartBuiltIn) "Built-in TESLA root key" else
            "TESLA key from the previous session"
        warmStartBuiltIn = false
        event(gst, OsnmaEvent.Type.WARNING, "$source not used: $reason")
        listener?.onStoredKeyDiscarded()
    }

    private fun writeDsmByte(pages: PageStore.SubframePages, page: Int) {
        val header = pages.hkroot[1].toInt() and 0xFF
        val dsmId = header ushr 4
        val blockId = header and 0xF
        if (dsmId < 12) lastKrootDsmId = dsmId
        val completed = dsmAssembler.write(dsmId, blockId, page - 2, pages.hkroot[page]) ?: return
        val gst = pages.gstSubframe
        if (completed.isPkr) {
            pkrDsmIdInProcess = completed.id
            processDsmPkr(DsmPkr(completed.data), gst)
        } else {
            krootDsmIdInProcess = completed.id
            processDsmKroot(DsmKroot(completed.data), gst)
        }
    }

    /**
     * The NMA header is part of the signed message. Since the DSM may have been assembled from
     * pages of several subframes, the headers seen recently are tried in turn.
     */
    private fun processDsmKroot(dsm: DsmKroot, gst: Gst) {
        val candidates = (listOfNotNull(pageStore.header(gst)) + pageStore.recentHeaders() +
                listOfNotNull(lastHeader)).distinct()
        if (candidates.isEmpty()) return
        processDsmKroot(dsm, candidates, gst)
    }

    private fun processDsmKroot(dsm: DsmKroot, headers: List<NmaHeader>, gst: Gst) {
        val pkid = dsm.publicKeyId
        val pubKey = applicablePubKey(pkid)
        if (pubKey == null) {
            pendingKroot = Triple(dsm, headers, gst)
            event(
                gst,
                OsnmaEvent.Type.INFO,
                "DSM-KROOT received, waiting for public key PKID $pkid"
            )
            return
        }
        pendingKroot = null
        val chain = TeslaChain.fromDsmKroot(dsm)
        val curve = dsm.curve
        var error: String? = null
        var header: NmaHeader? = null
        for (candidate in headers) {
            error = when {
                chain == null -> "reserved value in DSM-KROOT"
                curve == null -> "unknown DSM-KROOT signature length"
                curve != pubKey.curve -> "public key type doesn't match DSM-KROOT"
                !dsm.paddingOk(candidate) -> "wrong DSM-KROOT padding"
                !OsnmaCrypto.verifyEcdsa(
                    curve,
                    pubKey.key,
                    dsm.signedMessage(candidate),
                    dsm.signature!!
                ) ->
                    "invalid DSM-KROOT signature"
                else -> null
            }
            if (error == null) {
                header = candidate
                break
            }
        }
        if (header == null) {
            stats.krootFailed++
            event(gst, OsnmaEvent.Type.ERROR, "Could not verify KROOT with PKID $pkid: $error")
            // Bytes restored from an earlier session may be outdated: collect those anew
            krootDsmIdInProcess?.let { dsmAssembler.dropRestored(it) }
            return
        }
        stats.krootVerified++
        makePubKeyCurrent(pkid)
        // The KROOT is the key of the subframe before WN_K / TOWH_K
        val krootStart = Gst(dsm.krootWn, dsm.krootTowh * 3600)
        val kroot = TeslaKey(dsm.kroot!!, chain!!, krootStart.addSeconds(-30))
        if (krootGst == null) {
            event(
                gst,
                OsnmaEvent.Type.SUCCESS,
                "KROOT of chain ${chain.chainId} verified with public key PKID $pkid"
            )
        }
        krootGst = gst
        lastHeader = header
        lastHeaderAuthenticated = true
        if (warmStart) {
            warmStart = false
            warmStartBuiltIn = false
            // Continue from the signed KROOT: a stored or built-in key with the same chain ID
            // might belong to an earlier chain
            for (i in keySlots.indices) {
                if (keySlots[i]?.chain?.chainId == chain.chainId) keySlots[i] = null
            }
            event(gst, OsnmaEvent.Type.INFO, "TESLA chain confirmed by the DSM-KROOT")
        }
        listener?.onKrootVerified(BuiltInKroot(header.raw, dsm.data.copyOf()))
        storeKroot(kroot, header, gst)
        processNmaHeader(header, pkid, gst)
        // Keys of recent subframes may have been received before this KROOT: try them now
        retryRecentTeslaKeys(gst)
    }

    private fun retryRecentTeslaKeys(gst: Gst) {
        for (k in 1 downTo 0) {
            val subframe = gst.addSubframes(-k)
            keyAttempts.remove(subframe)
            val current = currentKey() ?: return
            val chain = current.chain
            val numTags = (Mack.MACK_BITS - chain.keySizeBits) / (chain.tagSizeBits + 16)
            tryTeslaKey(subframe, numTags * (chain.tagSizeBits + 16) / 32)
        }
    }

    private fun retryPendingKroot() {
        val (dsm, headers, gst) = pendingKroot ?: return
        if (applicablePubKey(dsm.publicKeyId) != null) processDsmKroot(dsm, headers, gst)
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
            event(
                gst,
                OsnmaEvent.Type.ERROR,
                "OSNMA Alert Message: all cryptographic material erased"
            )
        }
        alertReceived = true
        merkleTree = null
        currentPubKey = null
        nextPubKey = null
        keySlots.fill(null)
        chainInForce = null
        chainStart = null
        pendingKroot = null
        warmStart = false
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
                    pkrDsmIdInProcess?.let { dsmAssembler.dropRestored(it) }
                    event(
                        gst,
                        OsnmaEvent.Type.ERROR,
                        "Could not verify public key PKID ${pkr.newPublicKeyId} " +
                            "against the Merkle tree"
                    )
                } else {
                    stats.pkrVerified++
                    val isNew = storePublicKey(key, notify = true)
                    if (isNew) {
                        event(
                            gst,
                            OsnmaEvent.Type.SUCCESS,
                            "Public key PKID ${key.pkid} received and verified " +
                                "against the Merkle tree"
                        )
                    }
                    retryPendingKroot()
                }
            }
            NewPublicKeyType.ALERT_MESSAGE -> {
                if (tree.verifies(pkr)) alertMessageReceived(gst)
                else event(gst, OsnmaEvent.Type.ERROR, "Could not verify OSNMA Alert Message")
            }
            NewPublicKeyType.RESERVED ->
                event(gst, OsnmaEvent.Type.ERROR, "Reserved NPKT in DSM-PKR")
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

    /**
     * All satellites transmit the same TESLA key in a subframe, so it's assembled from the pages
     * of any of them as soon as the pages carrying it have arrived.
     */
    private fun tryTeslaKey(subframe: Gst, page: Int) {
        val current = currentKey() ?: return
        if (subframe <= current.gstSubframe) return
        val chain = current.chain
        val numTags = (Mack.MACK_BITS - chain.keySizeBits) / (chain.tagSizeBits + 16)
        val offset = numTags * (chain.tagSizeBits + 16)
        val firstPage = offset / 32
        val lastPage = (offset + chain.keySizeBits - 1) / 32
        if (page !in firstPage..lastPage) return
        val attempts = keyAttempts[subframe] ?: 0
        if (attempts >= MAX_KEY_ATTEMPTS) return
        val buffer = ByteArray(SubframeLayout.MACK_BYTES)
        val sources = pageStore.subframe(subframe)
        for (p in firstPage..lastPage) {
            val source = sources.firstOrNull { it.has(p) } ?: return
            System.arraycopy(source.mack, p * 4, buffer, p * 4, 4)
        }
        keyAttempts[subframe] = attempts + 1
        keyAttempts.keys.removeAll { it < subframe.addSubframes(-PageStoreDepth) }
        val key = TeslaKey(Bits.slice(buffer, offset, chain.keySizeBits), chain, subframe)
        validateKey(key, current)
    }

    private fun validateKey(newKey: TeslaKey, current: TeslaKey) {
        val maxDerivations =
            if (warmStart) TeslaKey.MAX_DERIVATIONS_STORED else TeslaKey.MAX_DERIVATIONS
        val steps = newKey.gstSubframe.subframesSince(current.gstSubframe)
        if (warmStart && steps > TeslaKey.MAX_DERIVATIONS) {
            // Hashing back to an old key is costly: try that once every few minutes at most
            val last = lastLongDerivation
            val sinceLast = last?.let { newKey.gstSubframe.subframesSince(it) }
            if (sinceLast != null && sinceLast < LONG_DERIVATION_SUBFRAMES) return
            lastLongDerivation = newKey.gstSubframe
        }
        if (current.authenticates(newKey, maxDerivations)) {
            stats.teslaKeysVerified++
            warmStartFailures = 0
            storeKey(newKey)
            listener?.onTeslaKeyVerified(newKey.toStored())
            // Keys of subframes in between (not received completely) follow from the new key: use
            // them for the tags of those subframes, oldest first
            val skipped = newKey.gstSubframe.subframesSince(current.gstSubframe) - 1
            for (k in minOf(skipped, PageStoreDepth) downTo 1) {
                processTags(newKey.derive(k))
            }
            processTags(newKey)
        } else {
            stats.teslaKeysFailed++
            event(
                newKey.gstSubframe,
                OsnmaEvent.Type.ERROR,
                "TESLA key at ${newKey.gstSubframe} could not be verified"
            )
            if (warmStart && ++warmStartFailures >= MAX_WARM_START_FAILURES) {
                discardStoredKey(newKey.gstSubframe, "new keys don't follow from it")
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
        if (timeSync == TimeSync.NONE) return
        for (svn in 1..36) {
            if (timeSync == TimeSync.FULL) {
                pageStore.get(svn, gstMack)
                    ?.let { processMack(it, key, key, gstMack, slowMac = false) }
            }
            if (slowMacKey != null) {
                // slowMacKey authenticates the MACK; the current key authenticates its tags
                pageStore.get(svn, gstSlowMac)
                    ?.let { processMack(it, slowMacKey, key, gstSlowMac, slowMac = true) }
            }
        }
    }

    /**
     * Verifies the tags of a MACK message, using only the parts that were received.
     *
     * - The MACSEQ (ICD 6.6) protects the Tag-Info of the flexible tags. It's checked when its
     *   pages are there; a wrong MACSEQ rejects the whole MACK. Without it, flexible tags are
     *   skipped.
     * - Each tag whose pages were received is checked against the MAC look-up table and verified.
     *   The tag MAC binds the PRND, PRNA, GST, counter and NMA status, and fixed tags' Tag-Info
     *   isn't covered by the MACSEQ anyway, so this is as strong as with the complete MACK.
     */
    private fun processMack(
        pages: PageStore.SubframePages,
        macseqKey: TeslaKey,
        tagKey: TeslaKey,
        gstMack: Gst,
        slowMac: Boolean
    ) {
        // The NMA status is an input to the tag MACs: from page 0 of any satellite
        val nmaStatus = pageStore.header(gstMack)?.nmaStatus ?: return
        val chain = tagKey.chain
        val prna = pages.svn
        if (!MacLookupTable.isKnown(chain.macLookupTable)) {
            stats.mackFailed++
            return
        }
        val mack = Mack(pages.mack, chain.keySizeBits, chain.tagSizeBits)
        val step = chain.tagSizeBits + 16
        val msg = (gstMack.tow / 30) % 2
        val flex = MacLookupTable.flexIndices(chain.macLookupTable, msg)!!
        val headerReceived = pages.hasMackBits(0, step)
        val macseqReceived = flex.all { pages.hasMackBits(it * step + chain.tagSizeBits, 16) }
        val macseqVerified = if (headerReceived && macseqReceived) {
            if (!macseqKey.verifyMacseq(mack, prna, gstMack)) {
                stats.mackFailed++
                event(gstMack, OsnmaEvent.Type.ERROR, "Wrong MACSEQ from E%02d".format(prna))
                return
            }
            true
        } else false

        val usable = ArrayList<Int>()
        for (j in 1 until mack.numTags) {
            if (!pages.hasMackBits(j * step, step)) continue
            if (j in flex && !macseqVerified) continue
            if (!chain.adkdValid(j, mack.tag(j), prna, gstMack)) {
                if (macseqVerified && pages.complete) {
                    stats.mackFailed++
                    event(
                        gstMack,
                        OsnmaEvent.Type.ERROR,
                        "Tag $j from E%02d doesn't match the MAC look-up table".format(prna)
                    )
                    return
                }
                continue
            }
            usable.add(j)
        }

        val gstNav = gstMack.addSeconds(-30)
        if (!slowMac) {
            // Tag0: self-authentication of the CED and status of the transmitting satellite
            if (headerReceived) {
                verifyTagFor(
                    tagKey, mack, 0, mack.cop, Adkd.INAV_CED, prna, prna, gstMack, gstNav, nmaStatus
                )
            }
            for (j in usable) {
                val tag = mack.tag(j)
                val navAdkd = tag.adkd == Adkd.INAV_CED || tag.adkd == Adkd.INAV_TIMING
                if (navAdkd && tag.prnd in 1..36) {
                    verifyTagFor(
                        tagKey, mack, j, tag.cop, tag.adkd, tag.prnd, prna, gstMack, gstNav,
                        nmaStatus
                    )
                }
                // Slow MAC tags need a key with an extra delay
            }
        } else {
            for (j in usable) {
                val tag = mack.tag(j)
                if (tag.adkd == Adkd.SLOW_MAC && tag.prnd in 1..36) {
                    verifyTagFor(
                        tagKey, mack, j, tag.cop, Adkd.SLOW_MAC, tag.prnd, prna, gstMack, gstNav,
                        nmaStatus
                    )
                }
            }
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
            val ok = key.verifyTag(
                mack.data, tagOffset, tagLength, gstTag, tagPrnd, prna, ctr, nmaStatus, null,
                navBits
            )
            if (ok) stats.dummyTagsOk++ else stats.dummyTagsFailed++
            return
        }
        val nav = if (isTiming) navData.timing(prnd, gstNav) else navData.cedAndStatus(prnd, gstNav)
        // The cut-off point (COP) limits how old the authenticated data can be
        if (nav == null || nav.maxAge + 1 > cop) return
        val ok = key.verifyTag(
            mack.data, tagOffset, tagLength, gstTag, tagPrnd, prna, ctr, nmaStatus, nav.bits,
            navBits
        )
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
            event(
                gstTag,
                OsnmaEvent.Type.ERROR,
                "Tag for E%02d (%s) sent by E%02d failed verification"
                    .format(prnd, adkd.label(), prna)
            )
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
            subframesStarted = pageStore.started,
            subframesCompletedCounted = stats.subframes - pageStore.firstCompleted
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
                cedCurrent = s.ced != null && latestCed != null &&
                    s.ced!!.data.contentEquals(latestCed.bits),
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
            warmStart = warmStart && current != null,
            warmStartBuiltIn = warmStart && warmStartBuiltIn && current != null,
            timeSync = timeSync,
            krootProgress = lastKrootDsmId?.let { dsmAssembler.progress(it) },
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
        private const val MAX_KEY_ATTEMPTS = 3
        private const val MAX_WARM_START_FAILURES = 3
        private const val LONG_DERIVATION_SUBFRAMES = 5
        private const val PageStoreDepth = 13
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
    /** The chain in force comes from a TESLA key stored in an earlier session or built in */
    val warmStart: Boolean,
    /** The chain in force comes from a built-in TESLA root key (see [BuiltInKroot]) */
    val warmStartBuiltIn: Boolean = false,
    /** Tags in use, given how well the time is known */
    val timeSync: TimeSync = TimeSync.FULL,
    /** Blocks of the DSM-KROOT received so far and the number needed (0 until known) */
    val krootProgress: Pair<Int, Int>?,
    val satellites: List<SatelliteAuthStatus>,
    val events: List<OsnmaEvent>,
    val statistics: OsnmaStatistics
) {
    val authenticatedSatellites: List<SatelliteAuthStatus>
        get() = satellites.filter { it.cedAuthenticated }

    /** Satellites with a failed tag in the last [seconds] */
    fun recentFailures(seconds: Long = RECENT_FAILURE_SECONDS): List<SatelliteAuthStatus> {
        val now = gst ?: return emptyList()
        return satellites.filter { sat ->
            val last = sat.lastFailureGst
            last != null && now.totalSeconds - last.totalSeconds <= seconds
        }
    }

    /**
     * Marks for satellite lists and the sky view, by Galileo SVN: authenticated satellites, and
     * satellites with recently failed tags (which take precedence)
     */
    fun marks(): Map<Int, OsnmaMark> {
        if (phase == Phase.ALERT || phase == Phase.DONT_USE) return emptyMap()
        val marks = HashMap<Int, OsnmaMark>()
        authenticatedSatellites.forEach { marks[it.svn] = OsnmaMark.AUTHENTICATED }
        recentFailures().forEach { marks[it.svn] = OsnmaMark.FAILED }
        return marks
    }

    /** Overall state, in order of progress */
    val phase: Phase
        get() = when {
            alertMessage -> Phase.ALERT
            gst == null -> Phase.NO_DATA
            statistics.osnmaPages == 0L -> Phase.NO_OSNMA
            nmaHeader?.nmaStatus == NmaStatus.DONT_USE -> Phase.DONT_USE
            publicKey == null && teslaKey == null -> Phase.WAITING_FOR_PUBLIC_KEY
            teslaKey == null -> Phase.WAITING_FOR_KROOT
            authenticatedSatellites.isEmpty() -> Phase.WAITING_FOR_TAGS
            else -> Phase.AUTHENTICATING
        }

    enum class Phase {
        NO_DATA, NO_OSNMA, WAITING_FOR_PUBLIC_KEY, WAITING_FOR_KROOT, WAITING_FOR_TAGS,
        AUTHENTICATING, DONT_USE, ALERT
    }

    companion object {
        /** How long a satellite with a failed tag stays flagged */
        const val RECENT_FAILURE_SECONDS = 600L
    }
}

/**
 * How well the receiver knows Galileo System Time without relying on the signal it
 * authenticates, which decides the tags that may be used (OSNMA Receiver Guidelines 2.1, 5.3.1)
 */
enum class TimeSync {
    /** Within T_L / 2 = 15 s: all tags */
    FULL,

    /** Within (T_L + 300 s) / 2 = 165 s: only Slow MAC tags (ADKD 12), whose key comes later */
    SLOW_MAC_ONLY,

    /** Unknown or further off: OSNMA must not be used */
    NONE;

    companion object {
        const val FULL_MAX_OFFSET_SECONDS = 15L
        const val SLOW_MAC_MAX_OFFSET_SECONDS = 165L

        /**
         * @param offsetSeconds difference between the receiver's own time and the GST read from
         * the signal, or null if not known yet
         */
        fun forOffset(offsetSeconds: Long?): TimeSync = when {
            offsetSeconds == null -> NONE
            kotlin.math.abs(offsetSeconds) <= FULL_MAX_OFFSET_SECONDS -> FULL
            kotlin.math.abs(offsetSeconds) <= SLOW_MAC_MAX_OFFSET_SECONDS -> SLOW_MAC_ONLY
            else -> NONE
        }
    }
}

/** OSNMA state of a satellite, as shown in satellite lists */
enum class OsnmaMark { AUTHENTICATED, FAILED }
