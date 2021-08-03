/*
 * Copyright 2019-2021 Google LLC, Sean J. Barbeau
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
package com.android.gpstest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.toText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service tracks location when requested and updates Activity via binding.
 *
 */
@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() {
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()
    private var isBound = false
    private var isStarted = false
    private var isForeground = false

//    // FusedLocationProviderClient - Main class for receiving location updates.
//    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
//
//    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
//    // updates, the priority, etc.
//    private lateinit var locationRequest: LocationRequest
//
//    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
//    private lateinit var locationCallback: LocationCallback

    // We save a local reference to last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    // Data store (in this case, Room database) where the service will persist the location data, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        // TODO - get onLocationChanged() callback and pass to Room

//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                super.onLocationResult(locationResult)
//
//                currentLocation = locationResult.lastLocation
//
//                // Notify our Activity that a new location was observed by adding to repository
//                currentLocation.toLocation()?.let {
//                    lifecycleScope.launch {
//                        repository.updateLocation(it)
//                    }
//                }
//
//                // Updates notification content if this service is running as a foreground
//                // service.
//                if (serviceRunningInForeground) {
//                    notificationManager.notify(
//                        NOTIFICATION_ID,
//                        generateNotification(currentLocation))
//                }
//            }
//        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification =
                intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification == true) {
            unsubscribeToLocationUpdates()
            stopSelf()
        } else {
            if (!isStarted) {
                isStarted = true

                // We may have been restarted by the system. Manage our lifetime accordingly.
                goForegroundOrStopSelf()
            }
        }

        // Tells the system to recreate the service after it's been killed.
        return super.onStartCommand(intent, flags, START_NOT_STICKY)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind()")
        configurationChange = false
        handleBind()
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        //stopForeground(true)
        //serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
        handleBind()
    }

    private fun handleBind() {
        if (!isBound) {
            isBound = true
            // Start ourself. This will begin collecting exercise state if we aren't already.
            //startService(Intent(this, this::class.java))
            // As long as a UI client is bound to us, we can hide the ongoing activity notification.
            //removeOngoingActivityNotification()
        }
    }


    override fun onUnbind(intent: Intent): Boolean {
        isBound = false
        lifecycleScope.launch {
            // Client can unbind because it went through a configuration change, in which case it
            // will be recreated and bind again shortly. Wait a few seconds, and if still not bound,
            // manage our lifetime accordingly.
            delay(UNBIND_DELAY_MILLIS)
            if (!isBound) {
                goForegroundOrStopSelf()
            }
        }
        // Allow clients to re-bind. We will be informed of this in onRebind().
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        PreferenceUtils.saveServiceLocationTrackingPref(true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        try {
            // TODO: Step 1.5, Subscribe to location changes.
//            fusedLocationProviderClient.requestLocationUpdates(
//                locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            PreferenceUtils.saveServiceLocationTrackingPref(false)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            // TODO: Step 1.6, Unsubscribe to location changes.
//            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
//            removeTask.addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    Log.d(TAG, "Location Callback removed.")
                    stopSelf()
                    isStarted = false
//                } else {
//                    Log.d(TAG, "Failed to remove Location Callback.")
//                }
//            }
            PreferenceUtils.saveServiceLocationTrackingPref(false)
            removeOngoingActivityNotification()
        } catch (unlikely: SecurityException) {
            PreferenceUtils.saveServiceLocationTrackingPref(true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun goForegroundOrStopSelf() {
        lifecycleScope.launch {
            // We may have been restarted by the system - check if we're still monitoring data
            if (PreferenceUtils.getServiceLocationTrackingPref()) {
                // Monitoring GNSS data
                postOngoingActivityNotification()
            } else {
                // We have nothing to do, so we can stop.
                stopSelf()
                isStarted = false
            }
        }
    }

    private fun postOngoingActivityNotification() {
        if (!isForeground) {
            isForeground = true
            Log.d(TAG, "Posting ongoing activity notification")

            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification(currentLocation))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun buildNotification(location: Location?): Notification {
        Log.d(TAG, "generateNotification()")
        val mainNotificationText = location?.toText() ?: getString(R.string.no_location_text)
        val titleText = getString(R.string.app_name)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, GpsTestActivity::class.java)

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(PRIORITY_LOW)
            .addAction(
                R.drawable.ic_baseline_launch_24, getString(R.string.open),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.stop),
                servicePendingIntent
            )
            .build()
    }

    private fun removeOngoingActivityNotification() {
        if (isForeground) {
            Log.d(TAG, "Removing ongoing activity notification")
            isForeground = false
            stopForeground(true)
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val TAG = "LocationService"

        private const val PACKAGE_NAME = "com.android.gpstest"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL = "gsptest_channel_01"

        private const val UNBIND_DELAY_MILLIS = 3_000L
    }
}
