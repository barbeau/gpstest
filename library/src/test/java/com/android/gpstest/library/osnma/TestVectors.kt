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

import java.util.zip.GZIPInputStream

/**
 * Loads the OSNMA test vectors published by the European GNSS Service Centre
 * (https://www.gsc-europa.eu/support-to-developers/osnma-test-vectors), see
 * src/test/resources/osnma/README.md.
 */
object TestVectors {

    class Page(val svn: Int, val gst: Gst, val page: InavPage)

    /** Merkle tree root of the test vectors' "configuration 1" */
    const val MERKLE_ROOT_1 = "0E63F552C8021709043C239032EFFE941BF22C8389032F5F2701E0FBC80148B8"

    /** Public key PKID 1 of the test vectors' "configuration 1" */
    const val PUBLIC_KEY_1 = "0374A925CFA0FF1805E5C5A58FDBA31BF0145D5B5BE2F062D3F8BB2EE98F0F6DB0"

    fun resource(name: String): ByteArray =
        TestVectors::class.java.getResourceAsStream("/osnma/$name")!!.use { it.readBytes() }

    /**
     * Pages of "configuration 1" (16 Aug 2023 05:00:01 GST, trimmed to 8 minutes), ordered in
     * time and then by satellite, as a receiver would produce them.
     */
    fun configuration1(): List<Page> {
        val start = Gst(1251, 277201)
        val text = GZIPInputStream(
            TestVectors::class.java.getResourceAsStream("/osnma/16_AUG_2023_GST_05_00_01_8min.csv.gz")!!
        ).bufferedReader().use { it.readText() }
        val perSatellite = text.lines().drop(1).filter { it.isNotBlank() }.map { line ->
            val fields = line.split(',')
            val svn = fields[0].toInt()
            val bytes = Bits.fromHex(fields[2])
            svn to (0 until bytes.size / 30).map { bytes.copyOfRange(it * 30, it * 30 + 30) }
        }
        val numPages = perSatellite.maxOf { it.second.size }
        val pages = ArrayList<Page>()
        for (i in 0 until numPages) {
            for ((svn, list) in perSatellite) {
                if (i < list.size) pages.add(Page(svn, start.addSeconds(2 * i), InavPage.fromFullPage(list[i])))
            }
        }
        return pages
    }

    fun publicKey1(source: KeySource = KeySource.IMPORTED): OsnmaPublicKey =
        OsnmaPublicKey.fromSec1(1, Bits.fromHex(PUBLIC_KEY_1), source)!!
}
