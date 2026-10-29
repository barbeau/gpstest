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
 * Operational Galileo OSNMA key material built into the app, so that authentication can start
 * without waiting for the public key broadcast (every 6 hours) or importing files.
 *
 * Taken from the GSC file OSNMA_MerkleTree_20251210100000_newPKID_2.xml
 * (https://www.gsc-europa.eu/gsc-products/OSNMA/MT). The public key is also broadcast in the
 * signal (DSM-PKR). It's only used if its Merkle tree path leads to the Merkle tree root in use,
 * so it can't weaken authentication: an imported or revoked root simply excludes it.
 */
object GalileoKeys {

    /** Root of the Galileo OSNMA Merkle tree (unchanged since the January 2024 tree) */
    const val MERKLE_ROOT = "832E15EDE55655EAC6E399A539477B7C034CCE24C3C93FFC904ACD9BF842F04E"

    private class BuiltInKey(
        val pkid: Int,
        val point: String,
        /** Index of the key's leaf in the Merkle tree */
        val leafIndex: Int,
        /** Sibling nodes from the leaf level up to the level below the root */
        val path: List<String>
    )

    private val KEYS = listOf(
        BuiltInKey(
            pkid = 2,
            point = "02219204B5CA6C46B623EEED6CDD2CDDB1F7D6A7532767E5B8DA0DE1EBD695FC99",
            leafIndex = 1,
            path = listOf(
                "7B944FA20915C7931D48DD016D94F9C6381FD37DC6C125D97015272FDDE41393",
                "6AAFDE28017BF0744D42819CE40E3A0CDA1ECA3F7A4EA67E134E7AA714C1E843",
                "DE73D209E4C5BCDC34CD117F2FE40FD08B110009997AD2B3291D3A2CF29943F9",
                "84DE3669E6DA551292979E5B8D045787FA967C57CC23638A30237614EDD9171A"
            )
        )
    )

    /** The built-in public keys that are authenticated by the Merkle tree [root] */
    fun builtInKeys(root: ByteArray): List<OsnmaPublicKey> = KEYS.mapNotNull { k ->
        val key = OsnmaPublicKey.fromSec1(k.pkid, Bits.fromHex(k.point), KeySource.BUILT_IN)
            ?: return@mapNotNull null
        val computed = MerkleTree.computeRoot(MerkleTree.leafFor(key), k.leafIndex) {
            Bits.fromHex(k.path[it])
        }
        if (computed.contentEquals(root)) key else null
    }
}
