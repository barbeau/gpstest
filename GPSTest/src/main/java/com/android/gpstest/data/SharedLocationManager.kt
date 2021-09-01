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
package com.android.gpstest.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import com.android.gpstest.Application
import com.android.gpstest.util.SharedPreferenceUtil.getMinDistance
import com.android.gpstest.util.SharedPreferenceUtil.getMinTimeMillis
import com.android.gpstest.util.hasPermission
import com.android.gpstest.util.toNotificationTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

private const val TAG = "SharedLocationManager"

/**
 * Wraps the LocationManager in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedLocationManager constructor(
    private val context: Context,
    externalScope: CoroutineScope
) {
    private val _receivingLocationUpdates: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val receivingLocationUpdates: StateFlow<Boolean>
        get() = _receivingLocationUpdates

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _locationUpdates = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val callback = LocationListener { location ->
            Log.d(TAG, "New location: ${location.toNotificationTitle()}")
            // Send the new location to the Flow observers
            trySend(location)
        }

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) close()

        Log.d(TAG, "Starting location updates")
        _receivingLocationUpdates.value = true

        try {
            // TODO - handle case where service is still running and user changes preferences for minTime and minDistance
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getMinTimeMillis(), getMinDistance(), callback, Application.get().mainLooper)
        } catch (e: Exception) {
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