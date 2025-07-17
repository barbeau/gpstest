/*
 * Copyright 2019-2021 Google LLC, Sean Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest.library.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.android.gpstest.library.util.PreferenceUtil.minTimeMillis
import com.android.gpstest.library.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.TimeUnit

private const val TAG = "SharedGnssStatusManager"

/**
 * Wraps the GnssStatus updates in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedGnssStatusManager constructor(
    private val context: Context,
    externalScope: CoroutineScope,
    prefs: SharedPreferences
) {
    // State of GnssStatus
    private val _statusState = MutableStateFlow<GnssStatusState>(GnssStatusState.Stopped)
    val statusState: StateFlow<GnssStatusState> = _statusState

    // State of ongoing GNSS fix
    private val _fixState = MutableStateFlow<FixState>(FixState.NotAcquired)
    val fixState: StateFlow<FixState> = _fixState

    // State of first GNSS fix
    private val _firstFixState = MutableStateFlow<FirstFixState>(FirstFixState.NotAcquired)
    val firstFixState: StateFlow<FirstFixState> = _firstFixState

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _gnssStatusUpdates = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val callback: GnssStatus.Callback = object : GnssStatus.Callback() {
            override fun onStarted() {
                _statusState.value = GnssStatusState.Started
            }

            override fun onStopped() {
                _statusState.value = GnssStatusState.Stopped
            }

            override fun onFirstFix(ttffMillis: Int) {
                _firstFixState.value = FirstFixState.Acquired(ttffMillis)
                _fixState.value = FixState.Acquired
            }

            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    _fixState.value = checkHaveFix(context, location, prefs)
                } else {
                    _fixState.value = FixState.NotAcquired
                }
                //Log.d(TAG, "New gnssStatus: ${status}")
                // Send the new location to the Flow observers
                trySend(status)
            }
        }

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) close()

        Log.d(TAG, "Starting GnssStatus updates")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.registerGnssStatusCallback(
                    ContextCompat.getMainExecutor(context),
                    callback
                )
            } else {
                locationManager.registerGnssStatusCallback(
                    callback,
                    Handler(Looper.getMainLooper())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in location flow: $e")
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d(TAG, "Stopping GnssStatus updates")
            locationManager.unregisterGnssStatusCallback(callback) // clean up when Flow collection ends
            _fixState.value = FixState.NotAcquired
            _firstFixState.value = FirstFixState.NotAcquired
        }
    }.shareIn(
        externalScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    /**
     * Returns a flow of GnssStatus backed by the Android system GnssStatus API.
     *
     * Note that for other flows in this class to return up-to-date data this flow must be active.
     */
    @ExperimentalCoroutinesApi
    fun statusFlow(): Flow<GnssStatus> {
        return _gnssStatusUpdates
    }
}

private fun checkHaveFix(context: Context, location: Location, prefs: SharedPreferences): FixState {
    val threshold = if (minTimeMillis(context, prefs) >= 1000L) {
        // Use two requested update intervals (it missed two updates)
        TimeUnit.MILLISECONDS.toNanos(minTimeMillis(context, prefs) * 2)
    } else {
        // Most Android devices can't refresh faster than 1Hz, so use 1.5 seconds - see #544
        TimeUnit.MILLISECONDS.toNanos(1500)
    }
    val nanosSinceFix = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
    return if (nanosSinceFix > threshold) {
        // We lost the GNSS fix
        FixState.NotAcquired
    } else {
        // We have a GNSS fix
        FixState.Acquired
    }
}

// Started/stopped states
sealed class GnssStatusState {
    object Started : GnssStatusState()
    object Stopped : GnssStatusState()
}

// GNSS ongoing fix acquired states
sealed class FixState {
    object Acquired : FixState()
    object NotAcquired : FixState()
}

// GNSS first fix state
sealed class FirstFixState {
    /**
     * [ttffMillis] the time from start of GNSS to first fix in milliseconds.
     */
    data class Acquired(val ttffMillis: Int) : FirstFixState()
    object NotAcquired : FirstFixState()
}