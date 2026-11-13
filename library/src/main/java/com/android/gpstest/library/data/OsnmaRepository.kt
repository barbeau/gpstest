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
import android.location.GnssMeasurementsEvent
import android.location.GnssNavigationMessage
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.android.gpstest.library.R
import com.android.gpstest.library.osnma.BuiltInKroot
import com.android.gpstest.library.osnma.GalileoKeys
import com.android.gpstest.library.osnma.InavLayoutDetector
import com.android.gpstest.library.osnma.InavPage
import com.android.gpstest.library.osnma.InavPageAssembler
import com.android.gpstest.library.osnma.InavTimeTracker
import com.android.gpstest.library.osnma.KeySource
import com.android.gpstest.library.osnma.OsnmaCardState
import com.android.gpstest.library.osnma.OsnmaEngine
import com.android.gpstest.library.osnma.OsnmaEstimate
import com.android.gpstest.library.osnma.OsnmaEstimator
import com.android.gpstest.library.osnma.OsnmaEvent
import com.android.gpstest.library.osnma.OsnmaPublicKey
import com.android.gpstest.library.osnma.OsnmaStatus
import com.android.gpstest.library.osnma.StoredTeslaKey
import com.android.gpstest.library.osnma.TimeSync
import com.android.gpstest.library.util.PreferenceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DateTimeException
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
     * Difference between the reference time ([timeSource]) and the Galileo System Time
     * read from the signal, in seconds, or null if unknown. OSNMA is only meaningful if the
     * receiver knows the time independently of the signal it authenticates (OSNMA Receiver
     * Guidelines sections 2.1 and 5.3.1); see TimeSync.
     */
    val clockOffsetSeconds: Long? = null,
    /** Where the reference time for [clockOffsetSeconds] comes from */
    val timeSource: TimeSource = TimeSource.DEVICE_CLOCK,
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
    val inavSeenBefore: Boolean = false,
    /** Pages restored from two messages whose halves were paired the wrong way round */
    val pagesRepaired: Long = 0,
    /** Pages dropped because the platform delivered them twice */
    val pagesRepeated: Long = 0,
    /** Bit offset of the page in the platform's data (0 as documented, 4 on some devices) */
    val layoutOffsetBits: Int? = null,
    /** Page times are estimated with the receiver's GNSS clock (GnssClock) */
    val timingUsesReceiverClock: Boolean = false,
    /** Why the receiver clock is or isn't used for page timing */
    val receiverClockStatus: ReceiverClockStatus = ReceiverClockStatus.NO_MEASUREMENTS,
    /** The last DSM-KROOT verified from the signal (hex, see BuiltInKroot), for logging */
    val lastVerifiedKroot: String? = null,
    /** What authentication is waiting for and how long it may take, or null if it isn't waiting */
    val estimate: OsnmaEstimate? = null
) {
    /**
     * Pages arrive, but too many are missing to complete the OSNMA subframes, or they stopped.
     * Usually caused by the chipset saving power (duty cycling) or tracking the E1 pilot. On a
     * Pixel 8 Pro this happens even with full tracking requested, reported as
     * https://issuetracker.google.com/issues/580885286.
     */
    val poorReception: Boolean
        get() = pagesStalled ||
                (status?.statistics?.subframeCompleteness
                        ?.let { it < MIN_SUBFRAME_COMPLETENESS } == true)

    /** Main state and warnings shown by the OSNMA card */
    val cardState: OsnmaCardState
        get() = OsnmaCardState.of(
            status,
            inavPagesReceived,
            keysRevoked,
            clockOffsetSeconds,
            poorReception
        )

    /** Tags that may be used, given [clockOffsetSeconds] */
    val timeSync: TimeSync
        get() = TimeSync.forOffset(clockOffsetSeconds)

    /** The time is known, but too far off to use all tags */
    val clockOffsetTooLarge: Boolean
        get() = clockOffsetSeconds != null && timeSync != TimeSync.FULL

    companion object {
        /** Below this share of complete subframes, authentication becomes slow or impossible */
        const val MIN_SUBFRAME_COMPLETENESS = 0.5
    }
}

/**
 * A source of the current time that's independent of GNSS, e.g. Google's TrustedTime API in the
 * google build flavor (see [OsnmaRepository.trustedTimeSource])
 */
fun interface TrustedTimeSource {
    /** The current UTC time in ms since the Unix epoch, or null if not available right now */
    fun currentUtcMillis(): Long?
}

/** Reference time used to check the Galileo time in the signal, in order of preference */
enum class TimeSource {
    /** A [TrustedTimeSource], e.g. Google's TrustedTime API (google build flavor) */
    TRUSTED_TIME,

    /** Android's network time (NTP), Android 13 and later */
    NETWORK_TIME,

    /** The device clock, which some devices set from GNSS: not independent of the signal */
    DEVICE_CLOCK
}

/** Whether the receiver clock (GnssClock) can be used to time Galileo pages, and if not, why */
enum class ReceiverClockStatus {
    OK,

    /** No GNSS measurements received (not supported, or not started) */
    NO_MEASUREMENTS,

    /** Android 9 or older: no common time base with the page arrival times */
    ANDROID_TOO_OLD,

    /** The chipset doesn't report the GNSS time (FullBiasNanos) */
    NO_FULL_BIAS,

    /** The chipset reports the GNSS time with more than 1 ms uncertainty */
    UNCERTAIN,

    /** The chipset doesn't report when the measurement was taken (ElapsedRealtimeNanos) */
    NO_ELAPSED_TIME,

    /** The reported values are implausible */
    INVALID
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
    private var inavSeen =
        prefs.getBoolean(context.getString(R.string.pref_key_osnma_inav_seen), false)

    private val _state =
        MutableStateFlow(OsnmaUiState(enabled = isEnabled(), inavSeenBefore = inavSeen))
    val state: StateFlow<OsnmaUiState> = _state

    // Only accessed from the executor thread
    private var engine: OsnmaEngine? = null
    private val timeTracker = InavTimeTracker()
    private val assembler = InavPageAssembler()
    private val layout = InavLayoutDetector()
    private var lastArrivalMillis = 0L
    private var lastPublishMillis = 0L
    private var inavPages = 0L
    private var crcFailures = 0
    private var crcChecked = 0
    private var trustPlatformParity = false
    private var clockOffsetSeconds: Long? = null
    private var timeSource = TimeSource.DEVICE_CLOCK

    /**
     * Independent time source to check the Galileo time against, set by the app when one is
     * available (e.g. TrustedTime in the google build flavor). Network time and then the device
     * clock are used otherwise.
     */
    @Volatile
    var trustedTimeSource: TrustedTimeSource? = null
    @Volatile
    private var receiverClockStatus = ReceiverClockStatus.NO_MEASUREMENTS

    // Network time minus SystemClock.elapsedRealtime(), refreshed every minute
    private var networkTimeOffset: Long? = null
    private var networkTimeCheckedAt = 0L
    private var engineKeys: Set<String> = emptySet()
    private var lastKeySaveMillis = 0L
    private var lastDsmSaveMillis = 0L
    private var lastVerifiedKroot: String? = null
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

        override fun onTeslaKeyVerified(key: StoredTeslaKey) {
            // Keep the latest verified key for a warm start; no need to write every 30 s
            val now = SystemClock.elapsedRealtime()
            if (lastKeySaveMillis == 0L || now - lastKeySaveMillis >= KEY_SAVE_INTERVAL_MILLIS) {
                lastKeySaveMillis = now
                OsnmaKeyStore.saveTeslaKey(context, prefs, key)
            }
        }

        override fun onStoredKeyDiscarded() {
            OsnmaKeyStore.clearTeslaKey(context, prefs)
        }

        override fun onKrootVerified(kroot: BuiltInKroot) {
            val hex = kroot.encode()
            if (hex != lastVerifiedKroot) {
                // Can be built into the app (GalileoKeys) to speed up the first session
                Log.i(TAG, "Verified DSM-KROOT: $hex")
                lastVerifiedKroot = hex
            }
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
            { checkStalled() },
            STALL_CHECK_MILLIS, STALL_CHECK_MILLIS, TimeUnit.MILLISECONDS
        )
    }

    /**
     * Republishes while pages are expected, and starts over once none have arrived for
     * [RESET_GAP_MILLIS], e.g. after tracking stopped. Otherwise the card, the satellite seals and
     * logs would keep showing the old session's results until the next page arrives.
     */
    private fun checkStalled() {
        if (inavPages == 0L) return
        if (lastArrivalMillis != 0L &&
            SystemClock.elapsedRealtime() - lastArrivalMillis > RESET_GAP_MILLIS
        ) {
            resetEngine()
            lastArrivalMillis = 0L
        }
        publish(force = true)
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
        val received = layout.read(data)
        if (received == null) {
            pagesWrongSize++
            Log.w(
                TAG,
                "Galileo I/NAV message with ${data.size} bytes instead of ${InavPage.SIZE_BYTES}"
            )
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
            prefs.edit()
                .putBoolean(context.getString(R.string.pref_key_osnma_inav_seen), true)
                .apply()
        }
        if (parityPassed) pagesParityPassed++

        // Rejoin pages whose halves the platform paired the wrong way round, and drop pages
        // delivered twice
        val assembled = assembler.assemble(svid, received, arrival)
        if (assembled == null) {
            publish(force = false)
            return
        }
        val page = assembled.page
        val pageArrival = assembled.arrivalMillis
        val pageWallClock = wallClock - (arrival - pageArrival)

        // Some platforms might not fill in the CRC. If it fails on practically every page while
        // the platform says the parity passed, rely on the platform instead. Pages that pass the
        // CRC prove that it's filled in, so then the platform's flag isn't used.
        if (!trustPlatformParity && page.isNominal) {
            crcChecked++
            if (!page.crcOk) {
                crcFailures++
                if (parityPassed) crcFailedButParityPassed++
            }
            // Only if the platform says those pages are fine: otherwise they're just bit errors
            if (crcChecked >= 100 && crcFailures >= crcChecked * 99 / 100 &&
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
            // Shown in the page grid as arrived but unusable, placed by its arrival time
            timeTracker.roughGst(pageArrival)?.let { engine?.notePageUnusable(svid, it) }
            publish(force = false)
            return
        }

        val gst = timeTracker.gstFor(svid, page, pageArrival, subframeNumber)
        if (gst != null) {
            if (timeTracker.lastWasExact) {
                // Only pages with their own time stamp: the page ended about 2 s after its
                // start, plus decoding and delivery latency
                val trustedUtc = trustedTimeAt(pageArrival)
                val networkUtc = if (trustedUtc == null) networkTimeAt(pageArrival) else null
                timeSource = when {
                    trustedUtc != null -> TimeSource.TRUSTED_TIME
                    networkUtc != null -> TimeSource.NETWORK_TIME
                    else -> TimeSource.DEVICE_CLOCK
                }
                val referenceUtc = trustedUtc ?: networkUtc ?: pageWallClock
                clockOffsetSeconds = deviceGstSeconds(referenceUtc) - 2 - gst.totalSeconds
            }
            val e = engine ?: newEngine().also { engine = it }
            e.timeSync = TimeSync.forOffset(clockOffsetSeconds)
            e.feedPage(
                svid,
                gst,
                page,
                checkCrc = !trustPlatformParity,
                timeCertain = timeTracker.lastTimeCertain
            )
            saveDsm(e, force = false)
        } else {
            pagesWithoutTime++
        }
        publish(force = false)
    }

    /**
     * Keeps the DSM bytes collected so far, so that the next session doesn't have to receive
     * every part of the TESLA root key again (important on devices that miss most pages)
     */
    private fun saveDsm(e: OsnmaEngine, force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastDsmSaveMillis < KEY_SAVE_INTERVAL_MILLIS) return
        lastDsmSaveMillis = now
        if (OsnmaKeyStore.isRevoked(context, prefs)) return
        val dsm = e.exportDsm()
        if (dsm.isNotEmpty()) OsnmaKeyStore.saveDsm(context, prefs, dsm)
    }

    /**
     * Called for every GNSS measurement event. The receiver clock (GnssClock) gives the GNSS time
     * at a known local time, which places pages in time without waiting for a time stamp in the
     * data, and despite delivery jitter.
     */
    fun onMeasurements(event: GnssMeasurementsEvent) {
        val status = receiverClockStatus(event)
        receiverClockStatus = status.first
        val (elapsedMillis, gstMillis) = status.second ?: return
        executor.execute { timeTracker.updateReceiverClock(elapsedMillis, gstMillis) }
    }

    /**
     * Whether the receiver clock in [event] can be used for page timing, and if so the local
     * monotonic time and the GST (both in ms) it gives
     */
    private fun receiverClockStatus(
        event: GnssMeasurementsEvent
    ): Pair<ReceiverClockStatus, Pair<Long, Long>?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ReceiverClockStatus.ANDROID_TOO_OLD to null
        }
        val clock = event.clock
        if (!clock.hasFullBiasNanos()) return ReceiverClockStatus.NO_FULL_BIAS to null
        // A bias uncertainty above 1 ms means the receiver doesn't know the time well yet
        if (clock.hasBiasUncertaintyNanos() && clock.biasUncertaintyNanos > 1_000_000.0) {
            return ReceiverClockStatus.UNCERTAIN to null
        }
        // Same clock as SystemClock.elapsedRealtime(), used for the page arrival times
        if (!clock.hasElapsedRealtimeNanos()) return ReceiverClockStatus.NO_ELAPSED_TIME to null
        val bias = if (clock.hasBiasNanos()) clock.biasNanos else 0.0
        val gpsNanos = clock.timeNanos - clock.fullBiasNanos - bias.toLong()
        val gstMillis = gpsNanos / 1_000_000 - GST_GPS_WEEK_OFFSET_MILLIS
        val elapsedMillis = clock.elapsedRealtimeNanos / 1_000_000
        if (elapsedMillis <= 0 || gstMillis <= 0) return ReceiverClockStatus.INVALID to null
        return ReceiverClockStatus.OK to Pair(elapsedMillis, gstMillis)
    }

    /** The [trustedTimeSource]'s time at the local monotonic time [elapsedMillis], or null */
    private fun trustedTimeAt(elapsedMillis: Long): Long? {
        val now = try {
            trustedTimeSource?.currentUtcMillis()
        } catch (e: Exception) {
            null
        } ?: return null
        return now - (SystemClock.elapsedRealtime() - elapsedMillis)
    }

    /**
     * Android's network time (NTP) at the local monotonic time [elapsedMillis], or null if it's
     * not available (before Android 13, or no network time yet). It's independent of GNSS,
     * unlike the device clock on some devices.
     */
    private fun networkTimeAt(elapsedMillis: Long): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val now = SystemClock.elapsedRealtime()
        if (networkTimeCheckedAt == 0L || now - networkTimeCheckedAt >= NETWORK_TIME_CHECK_MILLIS) {
            networkTimeCheckedAt = now
            networkTimeOffset = try {
                SystemClock.currentNetworkTimeClock().millis() - SystemClock.elapsedRealtime()
            } catch (e: DateTimeException) {
                null
            }
        }
        return networkTimeOffset?.let { elapsedMillis + it }
    }

    private fun newEngine(): OsnmaEngine {
        val keys = OsnmaKeyStore.publicKeys(context, prefs)
        engineKeys = keys.map { it.fingerprint() }.toSet()
        return OsnmaEngine(
            merkleRoot = OsnmaKeyStore.merkleRoot(context, prefs),
            initialKeys = keys,
            listener = listener,
            storedKey = OsnmaKeyStore.teslaKey(context, prefs),
            storedDsm = OsnmaKeyStore.dsm(context, prefs),
            builtInKroots = GalileoKeys.builtInKroots(),
            timeSync = TimeSync.forOffset(clockOffsetSeconds)
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

    /** Estimates when the first satellite will be authenticated, from the published status */
    private val estimator = OsnmaEstimator()

    private fun resetEngine() {
        engine?.let { saveDsm(it, force = true) }
        engine = null
        estimator.reset()
        timeTracker.reset()
        assembler.reset()
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
        val status = engine?.status()
        _state.value = OsnmaUiState(
            enabled = isEnabled(),
            status = status,
            inavPagesReceived = inavPages,
            trustingPlatformParity = trustPlatformParity,
            keysRevoked = OsnmaKeyStore.isRevoked(context, prefs),
            clockOffsetSeconds = clockOffsetSeconds,
            timeSource = timeSource,
            pagesStalled = pagesStalled(),
            timingUsesSubframeNumbers = timeTracker.usingSubframeNumbers,
            timingCorrections = timeTracker.corrections,
            pagesWithoutTime = pagesWithoutTime,
            pagesWrongSize = pagesWrongSize,
            pagesParityPassed = pagesParityPassed,
            crcFailedButParityPassed = crcFailedButParityPassed,
            pagesCorrupted = pagesCorrupted,
            inavSeenBefore = inavSeen,
            pagesRepaired = assembler.repaired,
            pagesRepeated = assembler.repeated,
            layoutOffsetBits = layout.offsetBits,
            timingUsesReceiverClock = timeTracker.usingReceiverClock,
            receiverClockStatus = receiverClockStatus,
            lastVerifiedKroot = lastVerifiedKroot,
            estimate = estimator.update(status, status?.statistics?.subframeCompleteness)
        )
    }

    companion object {
        private const val PUBLISH_INTERVAL_MILLIS = 1000L
        private const val RESET_GAP_MILLIS = 10 * 60 * 1000L
        private const val STALL_MILLIS = 30 * 1000L
        private const val KEY_SAVE_INTERVAL_MILLIS = 60 * 1000L
        private const val NETWORK_TIME_CHECK_MILLIS = 60 * 1000L
        private const val STALL_CHECK_MILLIS = 5 * 1000L

        /** GPS time - UTC, in seconds (leap seconds since 1980, unchanged since 2017) */
        private const val GPS_UTC_LEAP_SECONDS = 18L

        /** The GST week 0 starts at GPS week 1024 */
        private const val GST_GPS_WEEK_OFFSET_MILLIS = 1024L * 7 * 24 * 3600 * 1000

        /** GST from a UTC time in milliseconds since the Unix epoch */
        fun deviceGstSeconds(utcMillis: Long): Long {
            val gpsSeconds = utcMillis / 1000 - 315_964_800L + GPS_UTC_LEAP_SECONDS
            // The GST week 0 starts at GPS week 1024 (22 August 1999)
            return gpsSeconds - 1024L * 7 * 24 * 3600
        }
    }
}
