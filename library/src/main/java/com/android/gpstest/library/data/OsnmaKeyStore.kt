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
package com.android.gpstest.library.data

import android.content.Context
import android.content.SharedPreferences
import com.android.gpstest.library.R
import com.android.gpstest.library.osnma.ImportedKeyMaterial
import com.android.gpstest.library.osnma.KeySource
import com.android.gpstest.library.osnma.OsnmaKeyFiles
import com.android.gpstest.library.osnma.OsnmaPublicKey

/**
 * Persists the OSNMA cryptographic material in SharedPreferences: public keys received from the
 * signal or imported by the user, and an optional imported Merkle tree root that replaces the
 * built-in one.
 */
object OsnmaKeyStore {

    /**
     * Root of the Galileo OSNMA Merkle tree, which authenticates the ECDSA public keys broadcast
     * in the signal (DSM-PKR, every 6 hours). From the GSC file
     * OSNMA_MerkleTree_20251210100000_newPKID_2.xml (unchanged since the January 2024 tree).
     * https://www.gsc-europa.eu/gsc-products/OSNMA/MT
     */
    const val GALILEO_MERKLE_ROOT = "832E15EDE55655EAC6E399A539477B7C034CCE24C3C93FFC904ACD9BF842F04E"

    /**
     * The Merkle tree root in use: an imported one, or the built-in one. Null if an OSNMA Alert
     * Message revoked it.
     */
    fun merkleRoot(context: Context, prefs: SharedPreferences): ByteArray? {
        val imported = prefs.getString(context.getString(R.string.pref_key_osnma_merkle_root), null)
        val root = imported ?: GALILEO_MERKLE_ROOT
        val revoked = prefs.getString(context.getString(R.string.pref_key_osnma_revoked_merkle_root), null)
        return if (root == revoked) null else hexToBytes(root)
    }

    /** True after an OSNMA Alert Message, until new key material is imported */
    fun isRevoked(context: Context, prefs: SharedPreferences): Boolean =
        merkleRoot(context, prefs) == null &&
                prefs.getStringSet(context.getString(R.string.pref_key_osnma_public_keys), null).isNullOrEmpty()

    fun publicKeys(context: Context, prefs: SharedPreferences): List<OsnmaPublicKey> {
        val stored = prefs.getStringSet(context.getString(R.string.pref_key_osnma_public_keys), emptySet())
            ?: emptySet()
        return stored.mapNotNull { entry ->
            val parts = entry.split('|')
            if (parts.size != 3) return@mapNotNull null
            val pkid = parts[0].toIntOrNull() ?: return@mapNotNull null
            val source = runCatching { KeySource.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            OsnmaPublicKey.fromSec1(pkid, hexToBytes(parts[2]), source)
        }.sortedBy { it.pkid }
    }

    fun savePublicKey(context: Context, prefs: SharedPreferences, key: OsnmaPublicKey) {
        val prefKey = context.getString(R.string.pref_key_osnma_public_keys)
        val existing = prefs.getStringSet(prefKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        // One key per PKID, and only keep the two most recent PKIDs (current and next)
        existing.removeAll { it.substringBefore('|') == key.pkid.toString() }
        existing.add("${key.pkid}|${key.source.name}|${bytesToHex(key.sec1)}")
        val trimmed = existing.sortedBy { it.substringBefore('|').toIntOrNull() ?: 0 }.takeLast(2).toSet()
        prefs.edit().putStringSet(prefKey, trimmed).apply()
    }

    /**
     * Imports a file downloaded from the GSC (Merkle tree XML, public key XML, certificate or
     * PEM). Throws [OsnmaKeyFiles.ImportException] if nothing usable is found.
     */
    fun import(context: Context, prefs: SharedPreferences, content: ByteArray, fileName: String?): ImportedKeyMaterial {
        val material = OsnmaKeyFiles.parse(content, fileName)
        if (material.merkleRoot == null && material.publicKeys.isEmpty()) {
            throw OsnmaKeyFiles.ImportException("No OSNMA key material found")
        }
        material.merkleRoot?.let {
            val hex = bytesToHex(it)
            val editor = prefs.edit()
            if (hex == GALILEO_MERKLE_ROOT) {
                editor.remove(context.getString(R.string.pref_key_osnma_merkle_root))
            } else {
                editor.putString(context.getString(R.string.pref_key_osnma_merkle_root), hex)
            }
            editor.apply()
        }
        material.publicKeys.forEach { savePublicKey(context, prefs, it) }
        return material
    }

    /**
     * Handles an authenticated OSNMA Alert Message (OSNMA SIS ICD section 5.6): the Merkle tree and
     * all keys must no longer be trusted, until a new Merkle tree is imported.
     */
    fun revokeAll(context: Context, prefs: SharedPreferences) {
        val current = prefs.getString(context.getString(R.string.pref_key_osnma_merkle_root), null)
            ?: GALILEO_MERKLE_ROOT
        prefs.edit()
            .putString(context.getString(R.string.pref_key_osnma_revoked_merkle_root), current)
            .remove(context.getString(R.string.pref_key_osnma_public_keys))
            .remove(context.getString(R.string.pref_key_osnma_merkle_root))
            .apply()
    }

    /** Forgets all stored keys and the imported Merkle tree root */
    fun clear(context: Context, prefs: SharedPreferences) {
        prefs.edit()
            .remove(context.getString(R.string.pref_key_osnma_public_keys))
            .remove(context.getString(R.string.pref_key_osnma_merkle_root))
            .apply()
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(2 * it, 2 * it + 2).toInt(16).toByte() }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
