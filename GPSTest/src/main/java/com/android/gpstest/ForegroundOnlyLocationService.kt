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

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.io.CsvFileLogger
import com.android.gpstest.io.JsonFileLogger
import com.android.gpstest.util.*
import com.android.gpstest.util.SharedPreferenceUtil.getWriteAntennaInfoToFileCsv
import com.android.gpstest.util.SharedPreferenceUtil.getWriteAntennaInfoToFileJson
import com.android.gpstest.util.SharedPreferenceUtil.getWriteLocationToFile
import com.android.gpstest.util.SharedPreferenceUtil.getWriteNavMessageToFile
import com.android.gpstest.util.SharedPreferenceUtil.getWriteNmeaToFile
import com.android.gpstest.util.SharedPreferenceUtil.getWriteRawMeasurementsToFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
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
    private lateinit var notificationManager: NotificationManager

    // We save a local reference to last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null

    lateinit var csvFileLogger: CsvFileLogger
    lateinit var jsonFileLogger: JsonFileLogger

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        csvFileLogger = CsvFileLogger(applicationContext)
        jsonFileLogger = JsonFileLogger(applicationContext)
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
                initLogging()
                try {
                    registerLocationFlow()
                } catch (unlikely: Exception) {
                    PreferenceUtils.saveTrackingStarted(false)
                    Log.e(TAG, "Exception registering for updates: $unlikely")
                }

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
        csvFileLogger.close()
        jsonFileLogger.close()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        PreferenceUtils.saveTrackingStarted(true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            locationFlow?.cancel()
            stopSelf()
            isStarted = false
            PreferenceUtils.saveTrackingStarted(false)
            removeOngoingActivityNotification()
            currentLocation = null
        } catch (unlikely: SecurityException) {
            PreferenceUtils.saveTrackingStarted(true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    @ExperimentalCoroutinesApi
    private fun registerLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                Log.d(TAG, "Service location: ${it.toNotificationTitle()}")
                currentLocation = it

                // Show location in notification
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(currentLocation))

                if (getWriteLocationToFile() &&
                    applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    csvFileLogger.onLocationChanged(currentLocation)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun goForegroundOrStopSelf() {
        lifecycleScope.launch {
            // We may have been restarted by the system - check if we're still monitoring data
            if (PreferenceUtils.isTrackingStarted()) {
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
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun buildNotification(location: Location?): Notification {
        val titleText = location?.toNotificationTitle() ?: getString(R.string.no_location_text)
        val summaryText = location?.toNotificationSummary() ?: ""

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(summaryText)
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
            .setContentText(summaryText)
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

    private fun initLogging() {
        val date = Date()
        var isNewCSVFile = false
        var isNewJsonFile = false
        if (applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            && !csvFileLogger.isStarted && isCsvLoggingEnabled()
        ) {
            // User has granted permissions and has chosen to log at least one data type
            var existingCsvFile: File? = null

            //TODO - handle restart of logging
//            if (mLastSavedInstanceState != null) {
//                // See if this was an orientation change and we should continue logging to
//                // an existing file
//                existingCsvFile =
//                    mLastSavedInstanceState.getSerializable(GpsTestActivity.EXISTING_CSV_LOG_FILE)
//            }
            isNewCSVFile = csvFileLogger.startLog(existingCsvFile, date)
        }

        if (applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            && !jsonFileLogger.isStarted && isJsonLoggingEnabled()
        ) {
            // User has granted permissions and has chosen to log at least one data type
            var existingJsonFile: File? = null
            //TODO - handle restart of logging
//            if (mLastSavedInstanceState != null) {
//                // See if this was an orientation change and we should continue logging to
//                // an existing file
//                existingJsonFile =
//                    mLastSavedInstanceState.getSerializable(GpsTestActivity.EXISTING_JSON_LOG_FILE)
//            }
            isNewJsonFile = jsonFileLogger.startLog(existingJsonFile, date)
        }

        if (csvFileLogger.isStarted && !jsonFileLogger.isStarted) {
            if (isNewCSVFile) {
                // CSV logging only
                Toast.makeText(
                    applicationContext,
                    Application.get().getString(
                        R.string.logging_to_new_file,
                        csvFileLogger.file.absolutePath
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (!csvFileLogger.isStarted && jsonFileLogger.isStarted) {
            // JSON logging only
            if (isNewJsonFile) {
                // CSV logging only
                Toast.makeText(
                    applicationContext,
                    Application.get().getString(
                        R.string.logging_to_new_file,
                        jsonFileLogger.file.absolutePath
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (csvFileLogger.isStarted && jsonFileLogger.isStarted) {
            // CSV and JSON logging
            if (isNewCSVFile && isNewJsonFile) {
                Toast.makeText(
                    applicationContext,
                    Application.get().getString(
                        R.string.logging_to_new_file,
                        csvFileLogger.file.absolutePath
                    ) + " + .json",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) && (csvFileLogger.isStarted || jsonFileLogger.isStarted)) {
            // Base directories should be the same, so we only need one of the two (whichever is logging) to clear old files
            var baseDirectory: File = csvFileLogger.baseDirectory
            if (baseDirectory == null) {
                baseDirectory = jsonFileLogger.baseDirectory
            }
            IOUtils.deleteOldFiles(baseDirectory, csvFileLogger.file, jsonFileLogger.file)
        }
    }

    fun isFileLoggingEnabled(): Boolean {
        return getWriteNmeaToFile() || getWriteRawMeasurementsToFile() || getWriteNavMessageToFile() || getWriteLocationToFile() || getWriteAntennaInfoToFileJson() || getWriteAntennaInfoToFileCsv()
    }

    private fun isCsvLoggingEnabled(): Boolean {
        return getWriteNmeaToFile() || getWriteRawMeasurementsToFile() || getWriteNavMessageToFile() || getWriteLocationToFile() || getWriteAntennaInfoToFileCsv()
    }

    private fun isJsonLoggingEnabled(): Boolean {
        return getWriteAntennaInfoToFileJson()
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
