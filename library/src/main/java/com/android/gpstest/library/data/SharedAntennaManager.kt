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
import android.location.GnssAntennaInfo
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.gpstest.library.R
import com.android.gpstest.library.util.CarrierFreqUtils
import com.android.gpstest.library.util.IOUtils
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

private const val TAG = "SharedAntennaManager"

/**
 * Wraps the GnssAntennaInfo updates in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedAntennaManager constructor(
    private val context: Context,
    externalScope: CoroutineScope,
    prefs: SharedPreferences
) {
    @RequiresApi(Build.VERSION_CODES.R)
    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _antennaUpdates = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val callback = GnssAntennaInfo.Listener { list: List<GnssAntennaInfo> ->
            // Capture capabilities in preferences
            PreferenceUtils.saveInt(
                context.getString(R.string.capability_key_num_antenna),
                list.size,
                prefs
            )
            val cfs: MutableList<String> = ArrayList(2)
            for (info in list) {
                cfs.add(CarrierFreqUtils.getCarrierFrequencyLabel(info))
            }
            if (cfs.isNotEmpty()) {
                cfs.sort()
                PreferenceUtils.saveString(
                    context.getString(R.string.capability_key_antenna_cf),
                    IOUtils.trimEnds(cfs.toString()),
                    prefs
                )
            }

            //Log.d(TAG, "New antennas: $list")
            // Send the new antennas to the Flow observers
            trySend(list)
        }

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) close()

        Log.d(TAG, "Starting antenna updates")

        try {
            locationManager.registerAntennaInfoListener(context.mainExecutor, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in location flow: $e")
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d(TAG, "Stopping antenna updates")
            locationManager.unregisterAntennaInfoListener(callback) // clean up when Flow collection ends
        }
    }.shareIn(
        externalScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    @RequiresApi(Build.VERSION_CODES.R)
    @ExperimentalCoroutinesApi
    fun antennaFlow(): Flow<List<GnssAntennaInfo>> {
        return _antennaUpdates
    }
}