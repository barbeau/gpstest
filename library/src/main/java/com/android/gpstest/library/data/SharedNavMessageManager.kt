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
import android.location.GnssNavigationMessage
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.android.gpstest.library.R
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.SatelliteUtils
import com.android.gpstest.library.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

private const val TAG = "SharedNavMessageManager"

/**
 * Wraps Navigation Message updates in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedNavMessageManager(
    private val context: Context,
    externalScope: CoroutineScope,
    prefs: SharedPreferences
) {
    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _navMessageUpdates = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Check explicit support on Android S and higher here - Android R and lower are checked in status callbacks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkNavMessageSupport(context, locationManager, prefs)
        }
        val callback: GnssNavigationMessage.Callback =
            object : GnssNavigationMessage.Callback() {
                override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
                    //Log.d(TAG, "New nav message: ${event}")
                    // Send the new nav message info to the Flow observers
                    trySend(event)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(status: Int) {
                    // These status messages are deprecated on Android S and higher and should not be used
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        return
                    }
                    handleLegacyNavMessageStatus(context, status, prefs)
                }
            }

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) close()

        Log.d(TAG, "Starting NavMessage updates")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.registerGnssNavigationMessageCallback(
                    ContextCompat.getMainExecutor(
                        context
                    ), callback
                )
            } else {
                locationManager.registerGnssNavigationMessageCallback(
                    callback,
                    Handler(Looper.getMainLooper())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in location flow: $e")
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d(TAG, "Stopping NavMessage updates")
            locationManager.unregisterGnssNavigationMessageCallback(callback) // clean up when Flow collection ends
        }
    }.shareIn(
        externalScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    @ExperimentalCoroutinesApi
    fun navMessageFlow(): Flow<GnssNavigationMessage> {
        return _navMessageUpdates
    }
}

@RequiresApi(api = Build.VERSION_CODES.S)
private fun checkNavMessageSupport(context: Context,locationManager: LocationManager, prefs: SharedPreferences) {
    // TODO - surface this status message in UI somewhere, like when user returned from Settings like before?  For now just disable logging option in Settings, will surface in Dashboard later
    val uiStatusMessage: String
    uiStatusMessage = if (SatelliteUtils.isNavMessagesSupported(locationManager)) {
        PreferenceUtils.saveInt(
            context.getString(R.string.capability_key_nav_messages),
            PreferenceUtils.CAPABILITY_SUPPORTED,
            prefs
        )
        context.getString(R.string.gnss_nav_msg_status_ready)
    } else {
        PreferenceUtils.saveInt(
            context.getString(R.string.capability_key_nav_messages),
            PreferenceUtils.CAPABILITY_NOT_SUPPORTED,
            prefs
        )
        context.getString(R.string.gnss_nav_msg_status_not_supported)
    }
}

private fun handleLegacyNavMessageStatus(context: Context, status: Int, prefs: SharedPreferences) {
    val uiStatusMessage: String
    when (status) {
        GnssNavigationMessage.Callback.STATUS_LOCATION_DISABLED -> {
            uiStatusMessage = context.getString(R.string.gnss_nav_msg_status_loc_disabled)
            PreferenceUtils.saveInt(
                context.getString(R.string.capability_key_nav_messages),
                PreferenceUtils.CAPABILITY_LOCATION_DISABLED,
                prefs
            )
        }
        GnssNavigationMessage.Callback.STATUS_NOT_SUPPORTED -> {
            uiStatusMessage = context.getString(R.string.gnss_nav_msg_status_not_supported)
            PreferenceUtils.saveInt(
                context.getString(R.string.capability_key_nav_messages),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED,
                prefs
            )
        }
        GnssNavigationMessage.Callback.STATUS_READY -> {
            uiStatusMessage = context.getString(R.string.gnss_nav_msg_status_ready)
            PreferenceUtils.saveInt(
                context.getString(R.string.capability_key_nav_messages),
                PreferenceUtils.CAPABILITY_SUPPORTED,
                prefs
            )
        }
        else -> uiStatusMessage = context.getString(R.string.gnss_status_unknown)
    }
    Log.d(
        TAG,
        "GnssNavigationMessage.Callback.onStatusChanged() - $uiStatusMessage"
    )
}