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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsnmaCryptoTest {

    private fun hex(s: String) = Bits.fromHex(s)

    @Test
    fun sha3_256_knownAnswers() {
        assertEquals(
            "A7FFC6F8BF1ED76651C14756A061D662F580FF4DE43B49FA82D80A4B80F8434A",
            Bits.toHex(OsnmaCrypto.sha3_256(ByteArray(0)))
        )
        assertEquals(
            "3A985DA74FE225B2045C172D6BD390BD855F086E3E9D525B46BFE24511431532",
            Bits.toHex(OsnmaCrypto.sha3_256("abc".toByteArray()))
        )
        // Longer than one 136-byte block
        val long = ByteArray(200) { 0xA3.toByte() }
        assertEquals(
            "79F38ADEC5C20307A98EF76E8324AFBFD46CFD81B22E3973C65FA1BD9DE31787",
            Bits.toHex(OsnmaCrypto.sha3_256(long))
        )
    }

    /** RFC 4493 test vectors */
    @Test
    fun cmacAes_rfc4493() {
        val key = hex("2B7E151628AED2A6ABF7158809CF4F3C")
        val msg = hex(
            "6BC1BEE22E409F96E93D7E117393172AAE2D8A571E03AC9C9EB76FAC45AF8E51" +
                    "30C81C46A35CE411E5FBC1191A0A52EFF69F2445DF4F9B17AD2B417BE66C3710"
        )
        assertEquals("BB1D6929E95937287FA37D129B756746", Bits.toHex(OsnmaCrypto.cmacAes(key, ByteArray(0))))
        assertEquals("070A16B46B4D4144F79BDD9DD04A287C", Bits.toHex(OsnmaCrypto.cmacAes(key, msg.copyOf(16))))
        assertEquals("DFA66747DE9AE63030CA32611497C827", Bits.toHex(OsnmaCrypto.cmacAes(key, msg.copyOf(40))))
        assertEquals("51F0BEBF7E3B9D92FC49741779363CFE", Bits.toHex(OsnmaCrypto.cmacAes(key, msg)))
    }

    @Test
    fun publicKey_compressedPointRoundTrip() {
        val key = TestVectors.publicKey1()
        assertEquals(OsnmaCrypto.Curve.P256, key.curve)
        assertArrayEquals(hex(TestVectors.PUBLIC_KEY_1), key.sec1)
        // P-521 key from the test vectors (PKID 9)
        val p521 = OsnmaPublicKey.fromSec1(
            9,
            hex(
                "03004BE2D894FCA1C3585DB7FC4D29EB695E41E366B1039CC4637E6814216D34EAE874FF6652C49BE0" +
                        "54EF2E9D229BDD03048039B40D230FFC7671E766B7420AF22EEE"
            ),
            KeySource.IMPORTED
        )
        assertNotNull(p521)
        assertEquals(OsnmaCrypto.Curve.P521, p521!!.curve)
        // Invalid point: x >= p
        assertNull(OsnmaPublicKey.fromSec1(1, hex("02" + "FF".repeat(32)), KeySource.IMPORTED))
    }

    @Test
    fun crc24q_validatesTestVectorPages() {
        val pages = TestVectors.configuration1().take(200)
        assertTrue(pages.all { it.page.crcOk })
        val bits = pages[0].page.toByteArray()
        Bits.set(bits, 100, 1 - Bits.get(bits, 100))
        assertFalse(InavPage.fromAndroid(bits)!!.crcOk)
    }

    @Test
    fun merkleTreeFile_keyVerifiedAgainstRoot() {
        val material = OsnmaKeyFiles.parse(
            TestVectors.resource("OSNMA_MerkleTree_test_vectors_1.xml"),
            "OSNMA_MerkleTree_test_vectors_1.xml"
        )
        assertArrayEquals(hex(TestVectors.MERKLE_ROOT_1), material.merkleRoot)
        assertEquals(1, material.publicKeys.size)
        val key = material.publicKeys[0]
        assertEquals(1, key.pkid)
        assertEquals(KeySource.MERKLE_FILE, key.source)
        assertArrayEquals(hex(TestVectors.PUBLIC_KEY_1), key.sec1)
    }

    @Test
    fun publicKeyFile_parsed() {
        val material = OsnmaKeyFiles.parse(
            TestVectors.resource("OSNMA_PublicKey_test_vectors_PKID_1.xml"),
            "OSNMA_PublicKey_test_vectors_PKID_1.xml"
        )
        assertNull(material.merkleRoot)
        assertEquals(1, material.publicKeys.single().pkid)
    }

    @Test
    fun pemPublicKey_pkidFromFileName() {
        val pem = """
            -----BEGIN PUBLIC KEY-----
            MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEdKklz6D/GAXlxaWP26Mb8BRdW1vi
            8GLT+Lsu6Y8PbbCBqF1dQK+JP+3Ss8YUnBSSGfDcEp9YCpRYO8lZE3fSqQ==
            -----END PUBLIC KEY-----
        """.trimIndent()
        val material = OsnmaKeyFiles.parse(pem.toByteArray(), "OSNMA_PublicKey_20230803105952_newPKID_1.pem")
        val key = material.publicKeys.single()
        assertEquals(1, key.pkid)
        assertArrayEquals(hex(TestVectors.PUBLIC_KEY_1), key.sec1)
    }

    @Test
    fun gst_arithmetic() {
        val g = Gst(1251, Gst.SECS_IN_WEEK - 10)
        assertEquals(Gst(1252, 20), g.addSeconds(30))
        assertEquals(Gst(1251, Gst.SECS_IN_WEEK - 30), g.subframe())
        assertEquals(1, Gst(1252, 0).subframesSince(Gst(1251, Gst.SECS_IN_WEEK - 30)))
        assertTrue(Gst(1252, 0).isSubframe)
    }
}
