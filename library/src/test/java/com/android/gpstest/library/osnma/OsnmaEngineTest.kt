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
            val arrival = (p.gst.totalSeconds - 756_000_000L) * 1000 + 2000 + 80 + random.nextInt(-400, 401)
            val android = InavPage.fromAndroid(p.page.toByteArray())!!
            val gst = tracker.gstFor(p.svn, android, arrival) ?: return@forEach
            known++
            if (gst == p.gst) correct++
            engine.feedPage(p.svn, gst, android)
        }
        assertEquals(known, correct)
        // Only the pages before the first time stamp are lost
        assertTrue(known > TestVectors.configuration1().size - 30 * 26)
        assertEquals(0, engine.statistics.tagsFailed)
        assertTrue(engine.statistics.tagsOk > 1100)
    }
}
