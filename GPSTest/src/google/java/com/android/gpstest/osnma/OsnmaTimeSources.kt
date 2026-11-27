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
package com.android.gpstest.osnma

import android.content.Context
import android.util.Log
import com.android.gpstest.library.data.OsnmaRepository
import com.android.gpstest.library.data.TrustedTimeSource
import com.google.android.gms.time.TrustedTime

/**
 * Time sources for the Galileo OSNMA time check in the google build flavor: Google Play services'
 * TrustedTime API, which is independent of GNSS
 * (https://developer.android.com/develop/connectivity/trusted-time)
 */
object OsnmaTimeSources {
    private const val TAG = "OsnmaTimeSources"

    @Volatile
    private var requested = false

    /** Creates the TrustedTime client once and hands it to [repository] when it's ready */
    fun install(context: Context, repository: OsnmaRepository) {
        if (requested) return
        requested = true
        try {
            TrustedTime.createClient(context.applicationContext).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val client = task.result
                    repository.trustedTimeSource = TrustedTimeSource {
                        // Null until the device has synchronized with a time server since boot
                        client?.computeCurrentUnixEpochMillis()
                    }
                } else {
                    Log.w(TAG, "TrustedTime not available: ${task.exception}")
                }
            }
        } catch (e: Exception) {
            // E.g. no Google Play services: network time or the device clock is used instead
            Log.w(TAG, "TrustedTime not available: $e")
        }
    }
}
