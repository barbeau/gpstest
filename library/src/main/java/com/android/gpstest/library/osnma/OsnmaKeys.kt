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

import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec

/** Where a public key came from, for display */
enum class KeySource {
    /** Broadcast in the signal (DSM-PKR) and verified against the Merkle tree root */
    SIGNAL,
    /** Taken from an imported GSC Merkle tree file and verified against its root */
    MERKLE_FILE,
    /** Imported by the user from a certificate, PEM or XML file */
    IMPORTED
}

/** An OSNMA ECDSA public key with its public key ID */
class OsnmaPublicKey internal constructor(
    val pkid: Int,
    val curve: OsnmaCrypto.Curve,
    /** SEC1 compressed point */
    val sec1: ByteArray,
    val source: KeySource,
    internal val key: ECPublicKey
) {
    val curveName: String get() = if (curve == OsnmaCrypto.Curve.P256) "P-256" else "P-521"

    override fun toString(): String = "PKID $pkid ($curveName, ${source.name.lowercase()})"

    companion object {
        /** Builds a key from its SEC1 encoding. Returns null if the point is invalid. */
        fun fromSec1(pkid: Int, sec1: ByteArray, source: KeySource): OsnmaPublicKey? {
            val curve = when (sec1.size) {
                33, 65 -> OsnmaCrypto.Curve.P256
                67, 133 -> OsnmaCrypto.Curve.P521
                else -> return null
            }
            val key = OsnmaCrypto.publicKeyFromSec1(curve, sec1) ?: return null
            return OsnmaPublicKey(pkid, curve, OsnmaCrypto.compress(curve, key), source, key)
        }

        internal fun fromPlatformKey(pkid: Int, key: ECPublicKey, source: KeySource): OsnmaPublicKey? {
            val curve = OsnmaCrypto.curveOf(key) ?: return null
            return fromSec1(pkid, OsnmaCrypto.compress(curve, key), source)
        }
    }
}

/** The OSNMA Merkle tree root, used to authenticate public keys (ICD section 6.2) */
class MerkleTree(val root: ByteArray) {
    init {
        require(root.size == 32)
    }

    /**
     * Authenticates the leaf of a DSM-PKR. Returns true if the intermediate tree nodes lead to
     * the root.
     */
    internal fun verifies(pkr: DsmPkr): Boolean {
        val leaf = pkr.merkleLeaf ?: return false
        return computeRoot(leaf, pkr.messageId) { pkr.intermediateTreeNode(it) }.contentEquals(root)
    }

    internal fun publicKeyFrom(pkr: DsmPkr): OsnmaPublicKey? {
        if (!verifies(pkr)) return null
        val curveOk = pkr.newPublicKeyType == NewPublicKeyType.P256 ||
                pkr.newPublicKeyType == NewPublicKeyType.P521
        if (!curveOk) return null
        return OsnmaPublicKey.fromSec1(pkr.newPublicKeyId, pkr.newPublicKey!!, KeySource.SIGNAL)
    }

    companion object {
        const val DEPTH = 4

        /** Computes the root from a leaf with index [leafIndex] and its authentication path */
        fun computeRoot(leaf: ByteArray, leafIndex: Int, path: (Int) -> ByteArray): ByteArray {
            var node = OsnmaCrypto.sha256(leaf)
            var id = leafIndex
            for (level in 0 until DEPTH) {
                val sibling = path(level)
                node = if (id and 1 == 0) {
                    OsnmaCrypto.sha256(node, sibling)
                } else {
                    OsnmaCrypto.sha256(sibling, node)
                }
                id = id ushr 1
            }
            return node
        }

        /** Leaf content for a public key: NPKT (4 bits) || NPKID (4 bits) || key */
        fun leafFor(key: OsnmaPublicKey): ByteArray {
            val npkt = if (key.curve == OsnmaCrypto.Curve.P256) 1 else 3
            return byteArrayOf(((npkt shl 4) or (key.pkid and 0xF)).toByte()) + key.sec1
        }
    }
}

/** Result of importing an OSNMA crypto file */
data class ImportedKeyMaterial(
    val merkleRoot: ByteArray?,
    val publicKeys: List<OsnmaPublicKey>
)

/**
 * Parses the cryptographic material distributed by the European GNSS Service Centre
 * (https://www.gsc-europa.eu/gsc-products/OSNMA/PKI):
 * - Merkle tree XML (OSNMA_MerkleTree_*.xml): root and the public keys it contains. Keys are
 *   verified against the root using the tree nodes in the file.
 * - Public key XML (OSNMA_PublicKey_*.xml)
 * - X.509 certificate (OSNMA_PublicKey_*.crt, PEM or DER) or a PEM public key. The PKID can't be
 *   read from these, so it's taken from the file name (…PKID_<n>…) or [defaultPkid].
 */
object OsnmaKeyFiles {

    class ImportException(message: String) : Exception(message)

    fun parse(content: ByteArray, fileName: String?, defaultPkid: Int? = null): ImportedKeyMaterial {
        val text = String(content, Charsets.UTF_8)
        return when {
            text.contains("<MerkleTree>") -> parseMerkleXml(text)
            text.contains("<PublicKey>") || (text.contains("<PKID>") && text.contains("<point>")) ->
                parsePublicKeyXml(text)
            else -> parseCertificateOrPem(content, text, pkidFromName(fileName) ?: defaultPkid)
        }
    }

    fun pkidFromName(fileName: String?): Int? {
        if (fileName == null) return null
        val m = Regex("PKID_(\\d+)", RegexOption.IGNORE_CASE).find(fileName) ?: return null
        return m.groupValues[1].toIntOrNull()
    }

    private fun tag(xml: String, name: String): String? =
        Regex("<$name>\\s*([^<]*?)\\s*</$name>").find(xml)?.groupValues?.get(1)

    private fun blocks(xml: String, name: String): List<String> =
        Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { it.groupValues[1] }.toList()

    private fun parseMerkleXml(xml: String): ImportedKeyMaterial {
        val nodes = blocks(xml, "TreeNode").mapNotNull { n ->
            val j = tag(n, "j")?.toIntOrNull()
            val i = tag(n, "i")?.toIntOrNull()
            val x = tag(n, "x_ji")
            if (j != null && i != null && x != null) Pair(j, i) to Bits.fromHex(x) else null
        }.toMap()
        val root = nodes[DEPTH_ROOT to 0] ?: throw ImportException("Merkle tree root not found")
        val merkle = MerkleTree(root)
        val keys = blocks(xml, "PublicKey").mapNotNull { pk ->
            val index = tag(pk, "i")?.toIntOrNull() ?: return@mapNotNull null
            val pkid = tag(pk, "PKID")?.toIntOrNull() ?: return@mapNotNull null
            val point = tag(pk, "point") ?: return@mapNotNull null
            val key = OsnmaPublicKey.fromSec1(pkid, Bits.fromHex(point), KeySource.MERKLE_FILE)
                ?: return@mapNotNull null
            // Authentication path: sibling of the node on the path at each level
            var idx = index
            val path = ArrayList<ByteArray>()
            for (level in 0 until MerkleTree.DEPTH) {
                val sibling = nodes[level to (idx xor 1)]
                if (sibling == null) {
                    // Can't verify against the root; still a file the user chose to trust
                    return@mapNotNull OsnmaPublicKey.fromSec1(pkid, key.sec1, KeySource.IMPORTED)
                }
                path.add(sibling)
                idx = idx ushr 1
            }
            val computed = MerkleTree.computeRoot(MerkleTree.leafFor(key), index) { path[it] }
            if (computed.contentEquals(root)) key else null
        }
        return ImportedKeyMaterial(merkle.root, keys)
    }

    private const val DEPTH_ROOT = MerkleTree.DEPTH

    private fun parsePublicKeyXml(xml: String): ImportedKeyMaterial {
        val entries = blocks(xml, "PublicKey").ifEmpty { listOf(xml) }
        val keys = entries.mapNotNull { pk ->
            val pkid = tag(pk, "PKID")?.toIntOrNull() ?: return@mapNotNull null
            val point = tag(pk, "point") ?: return@mapNotNull null
            OsnmaPublicKey.fromSec1(pkid, Bits.fromHex(point), KeySource.IMPORTED)
        }
        if (keys.isEmpty()) throw ImportException("No public key found in XML file")
        return ImportedKeyMaterial(null, keys)
    }

    private fun parseCertificateOrPem(content: ByteArray, text: String, pkid: Int?): ImportedKeyMaterial {
        val ecKey: ECPublicKey = when {
            text.contains("BEGIN PUBLIC KEY") -> {
                val der = pemBody(text, "PUBLIC KEY")
                KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der)) as? ECPublicKey
            }
            else -> try {
                CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(content)).publicKey as? ECPublicKey
            } catch (e: Exception) {
                null
            }
        } ?: throw ImportException("Unsupported file: expected a GSC OSNMA Merkle tree, public key or certificate")
        val id = pkid ?: throw ImportException("Couldn't determine the public key ID (PKID) from the file name")
        val key = OsnmaPublicKey.fromPlatformKey(id, ecKey, KeySource.IMPORTED)
            ?: throw ImportException("Unsupported elliptic curve")
        return ImportedKeyMaterial(null, listOf(key))
    }

    private fun pemBody(text: String, label: String): ByteArray {
        val body = text.substringAfter("-----BEGIN $label-----").substringBefore("-----END $label-----")
            .replace(Regex("\\s"), "")
        return base64Decode(body)
    }

    /** java.util.Base64 needs API 26 and android.util.Base64 isn't available in unit tests */
    internal fun base64Decode(s: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val out = java.io.ByteArrayOutputStream()
        var acc = 0
        var bits = 0
        for (c in s) {
            if (c == '=') break
            val v = alphabet.indexOf(c)
            if (v < 0) throw ImportException("Invalid PEM encoding")
            acc = (acc shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((acc ushr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}
