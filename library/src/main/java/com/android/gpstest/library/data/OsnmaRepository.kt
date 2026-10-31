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
import android.location.GnssNavigationMessage
import android.os.SystemClock
import android.util.Log
import com.android.gpstest.library.R
import com.android.gpstest.library.osnma.InavPage
import com.android.gpstest.library.osnma.InavTimeTracker
import com.android.gpstest.library.osnma.KeySource
import com.android.gpstest.library.osnma.OsnmaEngine
import com.android.gpstest.library.osnma.OsnmaEvent
import com.android.gpstest.library.osnma.OsnmaPublicKey
import com.android.gpstest.library.osnma.OsnmaStatus
import com.android.gpstest.library.util.PreferenceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OsnmaRepository"

/**
 * State of Galileo OSNMA authentication for the UI
 */
data class OsnmaUiState(
    val enabled: Boolean = true,
    /** Null until the first Galileo I/NAV page has been processed */
    val status: OsnmaStatus? = null,
    /** Galileo I/NAV pages received from the platform, including ones not yet time-tagged */
    val inavPagesReceived: Long = 0,
    /** CRC errors were frequent, so the platform's parity flag is trusted instead */
    val trustingPlatformParity: Boolean = false,
    /** An OSNMA Alert Message revoked the key material; new keys must be imported */
    val keysRevoked: Boolean = false,
    /**
     * Difference between the device clock and the Galileo System Time read from the signal, in
     * seconds, or null if unknown. OSNMA is only meaningful if the receiver knows the time
     * independently of the signal it authenticates (OSNMA Receiver Guidelines section 3.1).
     */
    val clockOffsetSeconds: Long? = null,
    /** Galileo pages were being received but stopped arriving while tracking continued */
    val pagesStalled: Boolean = false,
    /** Page timing diagnostics, see [InavTimeTracker] */
    val timingUsesSubframeNumbers: Boolean = false,
    val timingCorrections: Int = 0,
    /** Pages skipped because their Galileo time wasn't known yet */
    val pagesWithoutTime: Long = 0,
    /** Pages whose data had a wrong size */
    val pagesWrongSize: Long = 0,
    /** Pages the platform marked as parity passed */
    val pagesParityPassed: Long = 0,
    /** Pages with a wrong CRC that the platform marked as parity passed */
    val crcFailedButParityPassed: Long = 0,
    /** Pages dropped because of a wrong CRC (or failed platform parity, when trusted) */
    val pagesCorrupted: Long = 0,
    /** This device has delivered Galileo I/NAV pages before (in this or an earlier session) */
    val inavSeenBefore: Boolean = false
) {
    /**
     * Pages arrive, but too many are missing to complete the OSNMA subframes, or they stopped.
     * Usually caused by the chipset saving power (duty cycling) or tracking the E1 pilot.
     */
    val poorReception: Boolean
        get() = pagesStalled ||
                (status?.statistics?.subframeCompleteness?.let { it < MIN_SUBFRAME_COMPLETENESS } == true)

    val clockOffsetTooLarge: Boolean
        get() = clockOffsetSeconds != null && kotlin.math.abs(clockOffsetSeconds) > MAX_CLOCK_OFFSET_SECONDS

    companion object {
        /** TESLA keys are disclosed 30 s after their tags, so time must be known better than that */
        const val MAX_CLOCK_OFFSET_SECONDS = 30L

        /** Below this share of complete subframes, authentication becomes slow or impossible */
        const val MIN_SUBFRAME_COMPLETENESS = 0.5
    }
}

/**
 * Feeds the Galileo I/NAV navigation messages received from the platform into an
 * [OsnmaEngine] and publishes its status. Processing happens on a dedicated thread.
 */
@Singleton
class OsnmaRepository @Inject constructor(
    private val context: Context,
    private val prefs: SharedPreferences
) {
    private val executor = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "OSNMA") }

    // Remembered so that the card can be shown right away on devices known to deliver I/NAV
    private var inavSeen = prefs.getBoolean(context.getString(R.string.pref_key_osnma_inav_seen), false)

    private val _state = MutableStateFlow(OsnmaUiState(enabled = isEnabled(), inavSeenBefore = inavSeen))
    val state: StateFlow<OsnmaUiState> = _state

    // Only accessed from the executor thread
    private var engine: OsnmaEngine? = null
    private val timeTracker = InavTimeTracker()
    private var lastArrivalMillis = 0L
    private var lastPublishMillis = 0L
    private var inavPages = 0L
    private var crcFailures = 0
    private var crcChecked = 0
    private var trustPlatformParity = false
    private var clockOffsetSeconds: Long? = null
    private var engineKeys: Set<String> = emptySet()
    private var pagesWithoutTime = 0L
    private var pagesWrongSize = 0L
    private var pagesParityPassed = 0L
    private var pagesCorrupted = 0L
    private var crcFailedButParityPassed = 0L

    private val listener = object : OsnmaEngine.Listener {
        override fun onPublicKeyAuthenticated(key: OsnmaPublicKey) {
            if (key.source != KeySource.BUILT_IN) OsnmaKeyStore.savePublicKey(context, prefs, key)
        }

        override fun onAlertMessage() {
            // All cryptographic material must be considered compromised (OSNMA SIS ICD 5.6)
            OsnmaKeyStore.revokeAll(context, prefs)
        }

        override fun onEvent(event: OsnmaEvent) {
            Log.d(TAG, event.toString())
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            context.getString(R.string.pref_key_osnma_enabled) -> executor.execute {
                if (!isEnabled()) resetEngine()
                publish(force = true)
            }
            context.getString(R.string.pref_key_osnma_public_keys),
            context.getString(R.string.pref_key_osnma_merkle_root) -> executor.execute {
                applyStoredKeys()
                publish(force = true)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        // Republish regularly so that the UI notices when pages stop arriving
        executor.scheduleWithFixedDelay(
            { if (inavPages > 0) publish(force = true) },
            STALL_CHECK_MILLIS, STALL_CHECK_MILLIS, TimeUnit.MILLISECONDS
        )
    }

    private fun pagesStalled(): Boolean =
        inavPages > 0 && PreferenceUtils.isTrackingStarted(prefs) &&
                SystemClock.elapsedRealtime() - lastArrivalMillis > STALL_MILLIS

    private fun isEnabled(): Boolean =
        prefs.getBoolean(context.getString(R.string.pref_key_osnma_enabled), true)

    /**
     * Called for every navigation message received from the platform. Only Galileo E1-B I/NAV
     * pages are used.
     */
    fun onNavigationMessage(message: GnssNavigationMessage) {
        if (message.type != GnssNavigationMessage.TYPE_GAL_I) return
        val arrival = SystemClock.elapsedRealtime()
        val wallClock = System.currentTimeMillis()
        val svid = message.svid
        val data = message.data ?: return
        val parityPassed = (message.status and GnssNavigationMessage.STATUS_PARITY_PASSED) != 0
        // For Galileo I/NAV: subframe number in the frame (1-24)
        val subframeNumber = message.messageId
        executor.execute { process(svid, data, parityPassed, arrival, wallClock, subframeNumber) }
    }

    private fun process(
        svid: Int,
        data: ByteArray,
        parityPassed: Boolean,
        arrival: Long,
        wallClock: Long,
        subframeNumber: Int
    ) {
        if (!isEnabled()) return
        val page = InavPage.fromAndroid(data)
        if (page == null) {
            pagesWrongSize++
            Log.w(TAG, "Galileo I/NAV message with ${data.size} bytes instead of ${InavPage.SIZE_BYTES}")
            publish(force = false)
            return
        }

        // After a long gap (tracking stopped), start over: TESLA keys can only be followed for
        // about a day and the time anchors are stale
        if (lastArrivalMillis != 0L && arrival - lastArrivalMillis > RESET_GAP_MILLIS) {
            resetEngine()
        }
        lastArrivalMillis = arrival
        inavPages++
        if (!inavSeen) {
            inavSeen = true
            prefs.edit().putBoolean(context.getString(R.string.pref_key_osnma_inav_seen), true).apply()
        }
        if (parityPassed) pagesParityPassed++

        // Some platforms might not fill in the CRC. If it's always wrong while the platform says
        // the parity passed, rely on the platform instead.
        if (!trustPlatformParity && page.isNominal) {
            crcChecked++
            if (!page.crcOk) {
                crcFailures++
                if (parityPassed) crcFailedButParityPassed++
            }
            // Only if the platform says those pages are fine: otherwise they're just bit errors
            if (crcChecked >= 100 && crcFailures > crcChecked * 9 / 10 &&
                crcFailedButParityPassed > crcFailures * 9 / 10
            ) {
                trustPlatformParity = true
                Log.w(TAG, "I/NAV CRC failing on most pages; trusting platform parity status")
            }
        }
        // Corrupted pages must not reach the time tracker (wrong time stamps) or the engine
        val valid = if (trustPlatformParity) parityPassed else page.crcOk
        if (!valid) {
            pagesCorrupted++
            publish(force = false)
            return
        }

        val gst = timeTracker.gstFor(svid, page, arrival, subframeNumber)
        if (gst != null) {
            if (timeTracker.lastWasExact) {
                // Only pages with their own time stamp: the page ended about 2 s after its
                // start, plus decoding and delivery latency
                clockOffsetSeconds = deviceGstSeconds(wallClock) - 2 - gst.totalSeconds
            }
            val e = engine ?: newEngine().also { engine = it }
            e.feedPage(svid, gst, page, checkCrc = !trustPlatformParity)
        } else {
            pagesWithoutTime++
        }
        publish(force = false)
    }

    private fun newEngine(): OsnmaEngine {
        val keys = OsnmaKeyStore.publicKeys(context, prefs)
        engineKeys = keys.map { it.fingerprint() }.toSet()
        return OsnmaEngine(
            merkleRoot = OsnmaKeyStore.merkleRoot(context, prefs),
            initialKeys = keys,
            listener = listener
        )
    }

    private fun OsnmaPublicKey.fingerprint(): String =
        "$pkid:" + sec1.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private fun applyStoredKeys() {
        val e = engine ?: return
        val root = OsnmaKeyStore.merkleRoot(context, prefs)
        val keys = OsnmaKeyStore.publicKeys(context, prefs)
        val fingerprints = keys.map { it.fingerprint() }.toSet()
        if (root == null || !fingerprints.containsAll(engineKeys)) {
            // Keys were removed or revoked: start over without them. Pages keep coming, so the
            // engine is recreated with the current material on the next one.
            resetEngine()
        } else {
            e.setMerkleRoot(root)
            keys.forEach { e.addPublicKey(it) }
            engineKeys = fingerprints
        }
    }

    private fun resetEngine() {
        engine = null
        timeTracker.reset()
        inavPages = 0
        crcChecked = 0
        crcFailures = 0
        trustPlatformParity = false
        clockOffsetSeconds = null
        pagesWithoutTime = 0
        pagesWrongSize = 0
        pagesParityPassed = 0
        crcFailedButParityPassed = 0
        pagesCorrupted = 0
    }

    private fun publish(force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPublishMillis < PUBLISH_INTERVAL_MILLIS) return
        lastPublishMillis = now
        _state.value = OsnmaUiState(
            enabled = isEnabled(),
            status = engine?.status(),
            inavPagesReceived = inavPages,
            trustingPlatformParity = trustPlatformParity,
            keysRevoked = OsnmaKeyStore.isRevoked(context, prefs),
            clockOffsetSeconds = clockOffsetSeconds,
            pagesStalled = pagesStalled(),
            timingUsesSubframeNumbers = timeTracker.usingSubframeNumbers,
            timingCorrections = timeTracker.corrections,
            pagesWithoutTime = pagesWithoutTime,
            pagesWrongSize = pagesWrongSize,
            pagesParityPassed = pagesParityPassed,
            crcFailedButParityPassed = crcFailedButParityPassed,
            pagesCorrupted = pagesCorrupted,
            inavSeenBefore = inavSeen
        )
    }

    companion object {
        private const val PUBLISH_INTERVAL_MILLIS = 1000L
        private const val RESET_GAP_MILLIS = 10 * 60 * 1000L
        private const val STALL_MILLIS = 30 * 1000L
        private const val STALL_CHECK_MILLIS = 5 * 1000L

        /** GPS time - UTC, in seconds (leap seconds since 1980, unchanged since 2017) */
        private const val GPS_UTC_LEAP_SECONDS = 18L

        /** GST from a UTC time in milliseconds since the Unix epoch */
        fun deviceGstSeconds(utcMillis: Long): Long {
            val gpsSeconds = utcMillis / 1000 - 315_964_800L + GPS_UTC_LEAP_SECONDS
            // The GST week 0 starts at GPS week 1024 (22 August 1999)
            return gpsSeconds - 1024L * 7 * 24 * 3600
        }
    }
}
