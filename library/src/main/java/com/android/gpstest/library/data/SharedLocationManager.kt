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
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationListenerCompat
import com.android.gpstest.library.util.PreferenceUtil.minDistance
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

private const val TAG = "SharedLocationManager"

/**
 * Wraps the LocationManager in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedLocationManager constructor(
    private val context: Context,
    externalScope: CoroutineScope,
    prefs:SharedPreferences
) {
    private val _receivingLocationUpdates: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val receivingLocationUpdates: StateFlow<Boolean>
        get() = _receivingLocationUpdates

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _locationUpdates = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Use LocationListenerCompat to avoid crashes on API Level 30 and lower (#627)
        val callback = LocationListenerCompat { location ->
            //Log.d(TAG, "New location: ${location.toNotificationTitle()}")
            // Send the new location to the Flow observers
            trySend(location)
        }

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) close()

        Log.d(TAG, "Starting location updates with minTime=${minTimeMillis(context, prefs)}ms and minDistance=${minDistance(context, prefs)}m")
        _receivingLocationUpdates.value = true

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMillis(context, prefs),
                minDistance(context, prefs),
                callback,
                context.mainLooper
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception in location flow: $e")
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d(TAG, "Stopping location updates")
            _receivingLocationUpdates.value = false
            locationManager.removeUpdates(callback) // clean up when Flow collection ends
        }
    }.shareIn(
        externalScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    @ExperimentalCoroutinesApi
    fun locationFlow(): Flow<Location> {
        return _locationUpdates
    }
}