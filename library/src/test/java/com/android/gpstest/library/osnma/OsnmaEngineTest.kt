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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OsnmaEngineTest {

    private fun newEngine(keys: List<OsnmaPublicKey> = listOf(TestVectors.publicKey1())) =
        OsnmaEngine(Bits.fromHex(TestVectors.MERKLE_ROOT_1), keys)

    /**
     * The expected counts were cross-checked with the galileo-osnma Rust library
     * (https://github.com/daniestevez/galileo-osnma) on the same data: 1219 correct tags, of
     * which 108 Slow MAC and 123 timing tags.
     */
    @Test
    fun configuration1_authenticatesNavigationData() {
        val engine = newEngine()
        for (p in TestVectors.configuration1()) {
            assertTrue(engine.feedPage(p.svn, p.gst, p.page))
        }
        val stats = engine.statistics
        assertEquals(0L, stats.crcErrors)
        assertEquals(1219, stats.tagsOk + stats.dummyTagsOk)
        assertEquals(988, stats.tagsOkAdkd0)
        assertEquals(123, stats.tagsOkAdkd4)
        assertEquals(108, stats.tagsOkAdkd12)
        assertEquals(0, stats.tagsFailed)
        assertEquals(0, stats.dummyTagsFailed)
        assertEquals(0, stats.teslaKeysFailed)
        assertEquals(0, stats.krootFailed)
        assertTrue(stats.krootVerified > 0)

        val status = engine.status()
        assertEquals(OsnmaStatus.Phase.AUTHENTICATING, status.phase)
        assertEquals(NmaStatus.TEST, status.nmaHeader!!.nmaStatus)
        assertTrue(status.nmaHeaderAuthenticated)
        assertEquals(1, status.publicKey!!.pkid)
        assertEquals(3, status.chainInForce)
        val chain = status.teslaKey!!.chain
        assertEquals(HashFunction.SHA_256, chain.hashFunction)
        assertEquals(MacFunction.HMAC_SHA_256, chain.macFunction)
        assertTrue(status.authenticatedSatellites.size >= 20)
        status.authenticatedSatellites.forEach {
            assertTrue(it.cedAuthBits >= SatelliteAuthStatus.MIN_AUTH_BITS)
            assertEquals(0, it.tagsFailed)
        }
    }

    @Test
    fun withoutPublicKey_waitsForKey() {
        val engine = newEngine(keys = emptyList())
        TestVectors.configuration1().forEach { engine.feedPage(it.svn, it.gst, it.page) }
        val status = engine.status()
        assertEquals(OsnmaStatus.Phase.WAITING_FOR_PUBLIC_KEY, status.phase)
        assertNull(status.teslaKey)
        assertEquals(0, engine.statistics.tagsOk)
    }

    @Test
    fun publicKeyAddedLater_verifiesPendingKroot() {
        val engine = newEngine(keys = emptyList())
        val pages = TestVectors.configuration1()
        // Feed the first two minutes: the DSM-KROOT completes but can't be verified yet
        val split = pages.indexOfFirst { it.gst.tow >= 277201 + 120 }
        pages.subList(0, split).forEach { engine.feedPage(it.svn, it.gst, it.page) }
        assertNull(engine.status().teslaKey)
        engine.addPublicKey(TestVectors.publicKey1())
        assertNotNull(engine.status().teslaKey)
        pages.subList(split, pages.size).forEach { engine.feedPage(it.svn, it.gst, it.page) }
        assertTrue(engine.statistics.tagsOk > 0)
        assertEquals(0, engine.statistics.tagsFailed)
    }

    @Test
    fun wrongPublicKey_rejectsKroot() {
        // A valid point on P-256 that isn't the test vectors' key (the curve generator)
        val other = OsnmaPublicKey.fromSec1(
            1,
            Bits.fromHex("036B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296"),
            KeySource.IMPORTED
        )!!
        val engine = newEngine(keys = listOf(other))
        TestVectors.configuration1().forEach { engine.feedPage(it.svn, it.gst, it.page) }
        assertTrue(engine.statistics.krootFailed > 0)
        assertEquals(0, engine.statistics.krootVerified)
        assertEquals(0, engine.statistics.tagsOk)
        assertNull(engine.status().teslaKey)
    }

    /**
     * Simulates a spoofer that alters the ephemeris of one satellite. Its CRC is recomputed so
     * that the page looks valid; the OSNMA tags must then fail and the satellite must not be
     * reported as authenticated.
     */
    @Test
    fun alteredEphemeris_failsAuthentication() {
        val target = 11
        val engine = newEngine()
        TestVectors.configuration1().forEach { p ->
            var page = p.page
            if (p.svn == target && page.wordType == 1) {
                page = tamperWord(page)
            }
            engine.feedPage(p.svn, p.gst, page)
        }
        val sat = engine.status().satellites.first { it.svn == target }
        assertTrue(sat.tagsFailed > 0)
        assertFalse(sat.cedAuthenticated)
        assertTrue(engine.statistics.tagsFailed > 0)
        // Other satellites are unaffected
        assertTrue(engine.status().authenticatedSatellites.size >= 19)
    }

    private fun tamperWord(page: InavPage): InavPage {
        val bits = page.toByteArray()
        // Flip a bit of the IODnav/t0e field in word type 1 (even part, data bits)
        val bit = 2 + 6 + 12
        Bits.set(bits, bit, 1 - Bits.get(bits, bit))
        // Recompute the CRC over the even part and the odd part up to the CRC
        val crc = Crc24q.compute(bits, 0, 196)
        Bits.write(bits, 196, 24, crc.toLong())
        val tampered = InavPage.fromAndroid(bits)!!
        assertTrue(tampered.crcOk)
        return tampered
    }

    /** Missing pages (power saving, weak signal) show up as a low subframe completeness */
    @Test
    fun missingPages_lowerSubframeCompleteness() {
        val full = newEngine()
        TestVectors.configuration1().forEach { full.feedPage(it.svn, it.gst, it.page) }
        val complete = full.statistics.subframeCompleteness!!
        assertTrue("completeness $complete", complete > 0.9)

        // Drop 1 page in 10, like a receiver decoding 90 % of the pages
        val random = Random(7)
        val lossy = newEngine()
        TestVectors.configuration1().forEach {
            if (random.nextInt(10) != 0) lossy.feedPage(it.svn, it.gst, it.page)
        }
        val partial = lossy.statistics.subframeCompleteness!!
        assertTrue("completeness $partial", partial < 0.5)
        assertEquals(0, lossy.statistics.tagsFailed)
    }

    private fun runWithTimeTracker(
        jitterMillis: Int,
        useSubframeNumbers: Boolean,
        duplicates: Boolean
    ): Pair<OsnmaStatistics, Int> {
        val random = Random(99)
        val tracker = InavTimeTracker()
        val engine = newEngine()
        var wrong = 0
        TestVectors.configuration1().forEach { p ->
            val arrival = (p.gst.totalSeconds - 756_000_000L) * 1000 + 2000 + 80 +
                    random.nextInt(-jitterMillis, jitterMillis + 1)
            val android = InavPage.fromAndroid(p.page.toByteArray())!!
            val subframeNumber =
                if (useSubframeNumbers) InavTimeTracker.subframeOf(p.gst.totalSeconds) + 1 else -1
            repeat(if (duplicates) 2 else 1) {
                val gst = tracker.gstFor(p.svn, android, arrival, subframeNumber) ?: return@repeat
                // Dummy pages (word type 63) carry nothing and are ignored by the engine
                if (gst != p.gst && android.wordType != 63) wrong++
                engine.feedPage(p.svn, gst, android, timeCertain = tracker.lastTimeCertain)
            }
        }
        return engine.statistics to wrong
    }

    /** Delivery jitter of several seconds is absorbed by snapping to the word type's page slot */
    @Test
    fun timeTracker_largeJitter_wordTypeSlots() {
        val (stats, wrong) =
            runWithTimeTracker(jitterMillis = 6000, useSubframeNumbers = false, duplicates = false)
        // Word type 16 occupies two slots 14 s apart, so a few can be off at this jitter
        assertTrue("wrong $wrong", wrong < TestVectors.configuration1().size / 100)
        assertEquals(0, stats.tagsFailed)
        assertTrue(stats.tagsOk > 1000)
    }

    /** With the platform's subframe numbers and pages delivered twice, timing stays right */
    @Test
    fun timeTracker_subframeNumbersAndDuplicates() {
        val (stats, wrong) =
            runWithTimeTracker(jitterMillis = 3000, useSubframeNumbers = true, duplicates = true)
        assertTrue("wrong $wrong", wrong < TestVectors.configuration1().size / 100)
        assertEquals(0, stats.tagsFailed)
        assertTrue(stats.tagsOk > 1100)
    }

    /**
     * Some chipsets pair the odd half of page n-1 with the even half of page n. Those pages must
     * be rejoined, and then authenticate like the original ones.
     */
    @Test
    fun swappedPageHalves_areRejoined() {
        val pages = TestVectors.configuration1()
        val previous = HashMap<Int, TestVectors.Page>()
        val assembler = InavPageAssembler()
        val tracker = InavTimeTracker()
        val engine = newEngine()
        var wrongTime = 0
        pages.forEach { p ->
            val original = p.page.toByteArray()
            // Half of the satellites deliver swapped halves: odd part of page n-1 + even part of
            // page n, received when the even part ends (1 s into page n)
            val swappedSat = p.svn % 2 == 0
            val message = if (swappedSat) {
                val prev = previous.put(p.svn, p) ?: return@forEach
                val swapped = ByteArray(InavPage.SIZE_BYTES)
                Bits.copy(prev.page.toByteArray(), 114, swapped, 0, 114)
                Bits.copy(original, 0, swapped, 114, 114)
                swapped
            } else original
            val arrival =
                (p.gst.totalSeconds - 756_000_000L) * 1000 + 2080 - (if (swappedSat) 1000 else 0)
            val assembled = assembler.assemble(p.svn, InavPage.fromAndroid(message)!!, arrival)
                ?: return@forEach
            assertTrue(assembled.page.isNominal)
            assertTrue(assembled.page.crcOk)
            // A rejoined page is the one before the message's even part
            val expectedGst = if (swappedSat) p.gst.addSeconds(-2) else p.gst
            val gst = tracker.gstFor(p.svn, assembled.page, assembled.arrivalMillis)
                ?: return@forEach
            if (gst != expectedGst && assembled.page.wordType != 63) wrongTime++
            engine.feedPage(p.svn, gst, assembled.page, timeCertain = tracker.lastTimeCertain)
        }
        assertTrue(assembler.repaired > 1000)
        assertTrue("wrong time $wrongTime", wrongTime < pages.size / 100)
        assertEquals(0, engine.statistics.tagsFailed)
        assertTrue(engine.statistics.tagsOk > 1000)
    }

    /**
     * Some devices (a Pixel 8 Pro) put the 4 unused bits at the start of the 29 bytes instead of
     * the end. The layout must be detected and the pages read correctly.
     */
    @Test
    fun shiftedAndroidLayout_isDetected() {
        val detector = InavLayoutDetector()
        val engine = newEngine()
        TestVectors.configuration1().forEach { p ->
            val shifted = ByteArray(InavPage.SIZE_BYTES)
            Bits.copy(p.page.toByteArray(), 0, shifted, 4, InavPage.PAGE_BITS)
            val page = detector.read(shifted)!!
            assertTrue(page.crcOk)
            engine.feedPage(p.svn, p.gst, page)
        }
        assertEquals(4, detector.offsetBits)
        assertEquals(1219, engine.statistics.tagsOk + engine.statistics.dummyTagsOk)

        // The documented layout is detected too
        val documented = InavLayoutDetector()
        TestVectors.configuration1().take(50).forEach { documented.read(it.page.toByteArray()) }
        assertEquals(0, documented.offsetBits)
    }

    /**
     * With pages missing, the DSM-KROOT is assembled byte by byte across satellites, TESLA keys
     * from the pages of several satellites and tags from partial MACKs. With 50 % of the pages
     * lost, practically no subframe is complete, but authentication must still work.
     */
    @Test
    fun missingPages_partialSubframesStillAuthenticate() {
        val random = Random(11)
        val engine = newEngine()
        TestVectors.configuration1().forEach {
            if (random.nextInt(100) >= 50) engine.feedPage(it.svn, it.gst, it.page)
        }
        val stats = engine.statistics
        assertEquals(0L, stats.subframesCompletedCounted)
        assertTrue(stats.krootVerified > 0)
        assertTrue(stats.teslaKeysVerified > 0)
        assertTrue("tags ${stats.tagsOk}", stats.tagsOk > 50)
        assertEquals(0, stats.tagsFailed)
        assertEquals(0, stats.teslaKeysFailed)
        assertTrue(engine.status().authenticatedSatellites.isNotEmpty())
    }

    /**
     * A TESLA key stored in an earlier session lets a new session authenticate without the
     * DSM-KROOT, the public key or the Merkle tree.
     */
    @Test
    fun warmStart_withStoredTeslaKey() {
        val pages = TestVectors.configuration1()
        val half = pages.indexOfFirst { it.gst.tow >= 277201 + 240 }
        var stored: StoredTeslaKey? = null
        val first = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            object : OsnmaEngine.Listener {
                override fun onTeslaKeyVerified(key: StoredTeslaKey) {
                    stored = key
                }
            }
        )
        pages.subList(0, half).forEach { first.feedPage(it.svn, it.gst, it.page) }
        val key = StoredTeslaKey.decode(stored!!.encode())!!
        assertTrue(key.key.contentEquals(stored!!.key))

        // Second session: no Merkle tree, no public key, and a gap of a minute
        val second = OsnmaEngine(null, emptyList(), storedKey = key)
        val resume = pages.indexOfFirst { it.gst.tow >= 277201 + 300 }
        var firstTagGst: Gst? = null
        pages.subList(resume, pages.size).forEach {
            second.feedPage(it.svn, it.gst, it.page)
            if (firstTagGst == null && second.statistics.tagsOk > 0) firstTagGst = it.gst
        }
        val stats = second.statistics
        assertEquals(0, stats.krootVerified)
        assertTrue(stats.tagsOk > 300)
        assertEquals(0, stats.tagsFailed)
        // Tags verified within about a minute of resuming
        assertTrue(firstTagGst!!.totalSeconds - pages[resume].gst.totalSeconds <= 90)
        val status = second.status()
        assertTrue(status.warmStart)
        assertEquals(OsnmaStatus.Phase.AUTHENTICATING, status.phase)
    }

    /** A stored key of another chain than the one in force is discarded */
    @Test
    fun warmStart_otherChainDiscarded() {
        var stored: StoredTeslaKey? = null
        val first = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            object : OsnmaEngine.Listener {
                override fun onTeslaKeyVerified(key: StoredTeslaKey) {
                    stored = key
                }
            }
        )
        val pages = TestVectors.configuration1()
        pages.take(pages.size / 2).forEach { first.feedPage(it.svn, it.gst, it.page) }
        val s = stored!!
        val otherChain =
            StoredTeslaKey(s.chain.copy(chainId = (s.chain.chainId + 1) % 4), s.key, s.gstSubframe)
        var discarded = false
        val second = OsnmaEngine(null, emptyList(), object : OsnmaEngine.Listener {
            override fun onStoredKeyDiscarded() {
                discarded = true
            }
        }, storedKey = otherChain)
        pages.drop(pages.size / 2).forEach { second.feedPage(it.svn, it.gst, it.page) }
        assertTrue(discarded)
        assertNull(second.status().teslaKey)
        assertEquals(0, second.statistics.tagsOk)
    }

    /**
     * With the receiver clock (GnssClock), pages are timed without waiting for a time stamp and
     * despite large delivery jitter.
     */
    @Test
    fun timeTracker_receiverClock() {
        val random = Random(3)
        val tracker = InavTimeTracker()
        val engine = newEngine()
        var wrong = 0
        var known = 0
        val pages = TestVectors.configuration1()
        pages.forEach { p ->
            val elapsedBase = p.gst.totalSeconds * 1000 - 756_000_000_000L
            // The receiver clock is known from the start; delivery 300 ms after the page, +-3 s
            // jitter
            tracker.updateReceiverClock(elapsedBase, p.gst.totalSeconds * 1000)
            val arrival = elapsedBase + 2300 + random.nextInt(-3000, 3001)
            val android = InavPage.fromAndroid(p.page.toByteArray())!!
            val gst = tracker.gstFor(p.svn, android, arrival) ?: return@forEach
            known++
            if (gst != p.gst && android.wordType != 63) wrong++
            engine.feedPage(p.svn, gst, android, timeCertain = tracker.lastTimeCertain)
        }
        assertTrue(tracker.usingReceiverClock)
        // No waiting for the first time stamp
        assertEquals(pages.size, known)
        assertTrue("wrong $wrong", wrong < pages.size / 100)
        assertEquals(0, engine.statistics.tagsFailed)
        assertTrue(engine.statistics.tagsOk > 1000)
    }

    @Test
    fun timeSync_thresholds() {
        assertEquals(TimeSync.NONE, TimeSync.forOffset(null))
        assertEquals(TimeSync.FULL, TimeSync.forOffset(0))
        assertEquals(TimeSync.FULL, TimeSync.forOffset(-15))
        assertEquals(TimeSync.SLOW_MAC_ONLY, TimeSync.forOffset(16))
        assertEquals(TimeSync.SLOW_MAC_ONLY, TimeSync.forOffset(-165))
        assertEquals(TimeSync.NONE, TimeSync.forOffset(166))
    }

    /** With loose time sync, only slow MAC tags are used; without, none, but keys still are */
    @Test
    fun timeSync_limitsTags() {
        val pages = TestVectors.configuration1()
        val slow = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            timeSync = TimeSync.SLOW_MAC_ONLY
        )
        val none = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            timeSync = TimeSync.NONE
        )
        pages.forEach {
            slow.feedPage(it.svn, it.gst, it.page)
            none.feedPage(it.svn, it.gst, it.page)
        }
        val s = slow.statistics
        assertEquals(0, s.tagsOkAdkd0)
        assertEquals(0, s.tagsOkAdkd4)
        assertTrue("slow MAC tags ${s.tagsOkAdkd12}", s.tagsOkAdkd12 > 0)
        assertEquals(0, s.tagsFailed)
        assertEquals(TimeSync.SLOW_MAC_ONLY, slow.status().timeSync)

        assertEquals(0, none.statistics.tagsOk)
        assertTrue(none.statistics.teslaKeysVerified > 0)
        assertTrue(none.status().authenticatedSatellites.isEmpty())

        // Getting stricter forgets what was authenticated with tags no longer allowed
        val full = newEngine()
        pages.forEach { full.feedPage(it.svn, it.gst, it.page) }
        assertTrue(full.status().authenticatedSatellites.isNotEmpty())
        full.timeSync = TimeSync.NONE
        assertTrue(full.status().authenticatedSatellites.isEmpty())
    }

    /** The fast derivation loop gives the same keys as stepping back one key at a time */
    @Test
    fun teslaKey_deriveMatchesPrevious() {
        for (hash in HashFunction.values()) {
            val chain = TeslaChain(1, hash, MacFunction.HMAC_SHA_256, 16, 40, 34, 0x25D3964DA3A2L)
            val key = TeslaKey(ByteArray(16) { (it * 7).toByte() }, chain, Gst(1419, 30 * 1000))
            var stepwise = key
            repeat(50) { stepwise = stepwise.previous() }
            val fast = key.derive(50)
            assertEquals(stepwise.gstSubframe, fast.gstSubframe)
            assertEquals(Bits.toHex(stepwise.data), Bits.toHex(fast.data))
        }
    }

    /**
     * A built-in DSM-KROOT lets the engine verify TESLA keys and tags before the DSM-KROOT is
     * received; a tampered one isn't used
     */
    @Test
    fun builtInKroot_usedBeforeSignalKroot() {
        val pages = TestVectors.configuration1()
        var captured: BuiltInKroot? = null
        val first = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            object : OsnmaEngine.Listener {
                override fun onKrootVerified(kroot: BuiltInKroot) {
                    captured = kroot
                }
            }
        )
        pages.forEach { first.feedPage(it.svn, it.gst, it.page) }
        val kroot = BuiltInKroot.decode(captured!!.encode())!!

        val engine = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            builtInKroots = listOf(kroot)
        )
        assertTrue(engine.status().warmStartBuiltIn)
        val withoutBuiltIn = newEngine()
        var firstTag: Gst? = null
        var firstTagWithout: Gst? = null
        // With 60 % of the pages lost, as on some phones, the DSM-KROOT takes minutes
        val random = Random(8)
        pages.forEach {
            if (random.nextInt(100) < 60) return@forEach
            engine.feedPage(it.svn, it.gst, it.page)
            withoutBuiltIn.feedPage(it.svn, it.gst, it.page)
            if (firstTag == null && engine.statistics.tagsOk > 0) firstTag = it.gst
            if (firstTagWithout == null && withoutBuiltIn.statistics.tagsOk > 0) {
                firstTagWithout = it.gst
            }
        }
        assertTrue(firstTag!! < firstTagWithout!!)
        assertEquals(0, engine.statistics.tagsFailed)
        assertEquals(0, engine.statistics.teslaKeysFailed)

        // One bit changed in the TESLA root key: the signature fails and it isn't used
        val tampered = kroot.dsmKroot.copyOf()
        tampered[20] = (tampered[20].toInt() xor 1).toByte()
        val withTampered = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            builtInKroots = listOf(BuiltInKroot(kroot.nmaHeader, tampered))
        )
        assertNull(withTampered.status().teslaKey)
        assertFalse(withTampered.status().warmStartBuiltIn)
    }

    /**
     * A built-in or stored key from an earlier chain with the same chain ID doesn't block
     * authentication: the signed DSM-KROOT from the signal replaces it
     */
    @Test
    fun warmStart_staleKeyWithSameChainIdReplaced() {
        var stored: StoredTeslaKey? = null
        val first = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            object : OsnmaEngine.Listener {
                override fun onTeslaKeyVerified(key: StoredTeslaKey) {
                    stored = key
                }
            }
        )
        val pages = TestVectors.configuration1()
        pages.take(pages.size / 4).forEach { first.feedPage(it.svn, it.gst, it.page) }
        val s = stored!!
        val stale = StoredTeslaKey(s.chain, ByteArray(s.key.size) { 0x55 }, s.gstSubframe)
        val engine = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            storedKey = stale
        )
        pages.drop(pages.size / 4).forEach { engine.feedPage(it.svn, it.gst, it.page) }
        assertTrue(engine.statistics.krootVerified > 0)
        assertTrue(engine.statistics.tagsOk > 300)
        assertEquals(0, engine.statistics.tagsFailed)
    }

    /** The KROOT built into the app verifies with the built-in public key */
    @Test
    fun galileoBuiltInKroot_verifies() {
        val root = Bits.fromHex(GalileoKeys.MERKLE_ROOT)
        val engine = OsnmaEngine(
            root,
            GalileoKeys.builtInKeys(root),
            builtInKroots = GalileoKeys.builtInKroots()
        )
        val status = engine.status()
        assertTrue(status.warmStartBuiltIn)
        assertEquals(1, status.chainInForce)
        assertEquals(1419, status.teslaKey!!.gstSubframe.wn)
    }

    /** Verifying a key half a year after the built-in KROOT is fast enough for a phone */
    @Test
    fun teslaKey_longDerivationSpeed() {
        val chain = TeslaChain(1, HashFunction.SHA_256, MacFunction.HMAC_SHA_256, 16, 40, 34, 1L)
        val key = TeslaKey(ByteArray(16), chain, Gst(1419, 0))
        val start = System.nanoTime()
        key.derive(183 * 24 * 120)
        val millis = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $millis ms", millis < 5000)
    }

    /** DSM bytes collected in one session are completed in the next one */
    @Test
    fun dsmBytes_carriedOverToNextSession() {
        val pages = TestVectors.configuration1()
        val random = Random(21)
        // First session: 4 minutes with 60 % of the pages lost, KROOT not complete
        val first = newEngine()
        val firstPart = pages.filter { it.gst.tow < 277201 + 240 }
        firstPart.forEach { if (random.nextInt(100) >= 60) first.feedPage(it.svn, it.gst, it.page) }
        val saved = first.exportDsm()
        assertTrue(saved.isNotEmpty())

        // Second session continues with the saved bytes
        val withSaved = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1()),
            storedDsm = saved
        )
        val withoutSaved = newEngine()
        var krootWith: Gst? = null
        var krootWithout: Gst? = null
        val secondRandom = Random(22)
        pages.filter { it.gst.tow >= 277201 + 240 }.forEach {
            if (secondRandom.nextInt(100) < 60) return@forEach
            withSaved.feedPage(it.svn, it.gst, it.page)
            withoutSaved.feedPage(it.svn, it.gst, it.page)
            if (krootWith == null && withSaved.statistics.krootVerified > 0) krootWith = it.gst
            if (krootWithout == null && withoutSaved.statistics.krootVerified > 0) {
                krootWithout = it.gst
            }
        }
        assertNotNull(krootWith)
        assertTrue(krootWithout == null || krootWith!! < krootWithout!!)
        assertEquals(0, withSaved.statistics.krootFailed)
        assertEquals(0, withSaved.statistics.tagsFailed)
    }

    @Test
    fun corruptedPage_isRejectedByCrc() {
        val p = TestVectors.configuration1().first()
        val bits = p.page.toByteArray()
        Bits.set(bits, 50, 1 - Bits.get(bits, 50))
        val engine = newEngine()
        assertFalse(engine.feedPage(p.svn, p.gst, InavPage.fromAndroid(bits)!!))
        assertEquals(1L, engine.statistics.crcErrors)
    }

    /**
     * Android doesn't give the GST of each page. Check that deriving it with [InavTimeTracker]
     * from arrival times with delivery jitter gives the right times and the same results.
     */
    @Test
    fun timeTracker_derivesGstFromArrivalTimes() {
        val random = Random(1234)
        val tracker = InavTimeTracker()
        val engine = newEngine()
        var known = 0
        var correct = 0
        TestVectors.configuration1().forEach { p ->
            // Pages arrive after their 2 s transmission, with latency and +-400 ms jitter
            val arrival =
                (p.gst.totalSeconds - 756_000_000L) * 1000 + 2000 + 80 + random.nextInt(-400, 401)
            val android = InavPage.fromAndroid(p.page.toByteArray())!!
            val gst = tracker.gstFor(p.svn, android, arrival) ?: return@forEach
            known++
            if (gst == p.gst) correct++
            engine.feedPage(p.svn, gst, android, timeCertain = tracker.lastTimeCertain)
        }
        assertEquals(known, correct)
        // Only the pages before the first time stamp are lost
        assertTrue(known > TestVectors.configuration1().size - 30 * 26)
        assertEquals(0, engine.statistics.tagsFailed)
        assertTrue(engine.statistics.tagsOk > 1100)
    }
}
