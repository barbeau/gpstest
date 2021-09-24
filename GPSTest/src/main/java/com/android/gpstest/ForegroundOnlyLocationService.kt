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
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
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
import com.android.gpstest.util.IOUtils.*
import com.android.gpstest.util.SharedPreferenceUtil.isCsvLoggingEnabled
import com.android.gpstest.util.SharedPreferenceUtil.isJsonLoggingEnabled
import com.android.gpstest.util.SharedPreferenceUtil.writeAntennaInfoToFileCsv
import com.android.gpstest.util.SharedPreferenceUtil.writeAntennaInfoToFileJson
import com.android.gpstest.util.SharedPreferenceUtil.writeLocationToFile
import com.android.gpstest.util.SharedPreferenceUtil.writeMeasurementToLogcat
import com.android.gpstest.util.SharedPreferenceUtil.writeMeasurementsToFile
import com.android.gpstest.util.SharedPreferenceUtil.writeNavMessageToFile
import com.android.gpstest.util.SharedPreferenceUtil.writeNavMessageToLogcat
import com.android.gpstest.util.SharedPreferenceUtil.writeNmeaTimestampToLogcat
import com.android.gpstest.util.SharedPreferenceUtil.writeNmeaToAndroidMonitor
import com.android.gpstest.util.SharedPreferenceUtil.writeNmeaToFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private var nmeaFlow: Job? = null
    private var navMessageFlow: Job? = null
    private var measurementFlow: Job? = null
    private var antennaFlow: Job? = null

    lateinit var csvFileLogger: CsvFileLogger
    lateinit var jsonFileLogger: JsonFileLogger

    // Preference listener that will init the loggers if the user changes Settings while Service is running
    private val loggingSettingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferenceUtil.newFileLoggingListener { initLogging() }
    private var deletedFiles = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        csvFileLogger = CsvFileLogger(applicationContext)
        jsonFileLogger = JsonFileLogger(applicationContext)

        // Observe logging setting changes
        Application.prefs.registerOnSharedPreferenceChangeListener(loggingSettingListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification == true) {
            unsubscribeToLocationUpdates()
        } else {
            if (!isStarted) {
                isStarted = true
                GlobalScope.launch(Dispatchers.IO) {
                    initLogging()
                }
                try {
                    observeFlows()
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
        stopLogging()
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
            cancelFlows()
            stopSelf()
            stopLogging()
            isStarted = false
            PreferenceUtils.saveTrackingStarted(false)
            removeOngoingActivityNotification()
            currentLocation = null
        } catch (unlikely: SecurityException) {
            PreferenceUtils.saveTrackingStarted(true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    @SuppressLint("NewApi")
    @ExperimentalCoroutinesApi
    private fun observeFlows() {
        observeLocationFlow()
        observeNmeaFlow()
        observeNavMessageFlow()
        observeMeasurementsFlow()
        if (SatelliteUtils.isGnssAntennaInfoSupported(getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
            observeAntennaFlow()
        }
    }

    private fun cancelFlows() {
        locationFlow?.cancel()
        nmeaFlow?.cancel()
        navMessageFlow?.cancel()
        measurementFlow?.cancel()
        antennaFlow?.cancel()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service location: ${it.toNotificationTitle()}")
                currentLocation = it

                // Show location in notification
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(currentLocation)
                )

                GlobalScope.launch(Dispatchers.IO) {
                    if (writeLocationToFile() &&
                        applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        initLogging()
                        csvFileLogger.onLocationChanged(currentLocation)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeNmeaFlow() {
        if (nmeaFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        nmeaFlow = repository.getNmea()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service NMEA: $it")
                GlobalScope.launch(Dispatchers.IO) {
                    if (writeNmeaToAndroidMonitor()) {
                        writeNmeaToAndroidStudio(
                            it.message,
                            if (writeNmeaTimestampToLogcat()) it.timestamp else Long.MIN_VALUE
                        )
                    }
                    if (writeNmeaToFile() &&
                        applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        initLogging()
                        csvFileLogger.onNmeaReceived(it.timestamp, it.message)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeNavMessageFlow() {
        if (navMessageFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        navMessageFlow = repository.getNavMessages()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service nav message: $it")
                GlobalScope.launch(Dispatchers.IO) {
                    if (writeNavMessageToLogcat()) {
                        writeNavMessageToAndroidStudio(it)
                    }
                    if (writeNavMessageToFile() &&
                        applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        initLogging()
                        csvFileLogger.onGnssNavigationMessageReceived(it)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeMeasurementsFlow() {
        if (measurementFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        measurementFlow = repository.getMeasurements()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service measurement: $it")
                GlobalScope.launch(Dispatchers.IO) {
                    if (writeMeasurementToLogcat()) {
                        for (m in it.measurements) {
                            writeMeasurementToLogcat(m)
                        }
                    }
                    if (writeMeasurementsToFile() &&
                        applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        initLogging()
                        csvFileLogger.onGnssMeasurementsReceived(it)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @ExperimentalCoroutinesApi
    private fun observeAntennaFlow() {
        if (antennaFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        antennaFlow = repository.getAntennas()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service antennas: $it")
                GlobalScope.launch(Dispatchers.IO) {
                    if (!applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        return@launch
                    }
                    if (writeAntennaInfoToFileCsv() || writeAntennaInfoToFileJson()) {
                        initLogging()
                    }
                    if (writeAntennaInfoToFileCsv()) {
                        csvFileLogger.onGnssAntennaInfoReceived(it)
                    }
                    if (writeAntennaInfoToFileJson()) {
                        jsonFileLogger.onGnssAntennaInfoReceived(it)
                    }
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
        // FIXME - when launching from this Intent the Activity re-starts and never shows info
        val launchActivityIntent = Intent(this, GpsTestActivity::class.java)
        val openActivityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)
        val stopServicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(summaryText)
            .setSmallIcon(R.drawable.ic_sat_notification)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(PRIORITY_LOW)
            .setContentIntent(openActivityPendingIntent)
            .addAction(
                R.drawable.ic_baseline_launch_24, getString(R.string.open),
                openActivityPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.stop),
                stopServicePendingIntent
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
     * Initialize and start logging if permissions have been granted.
     *
     * Note that this is called from each of the flows that log data, because when the user initially
     * enables logging in the settings the preference change callback happens before the user grants
     * file permissions. So we need to call this on each update in case the user just granted file
     * permissions but logging hasn't been started yet.
     */
    private fun initLogging() {
        if (!applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return
        }
        val date = Date()
        if (!csvFileLogger.isStarted && isCsvLoggingEnabled()) {
            // User has granted permissions and has chosen to log at least one data type
            csvFileLogger.startLog(null, date)
        }

        if (!jsonFileLogger.isStarted && isJsonLoggingEnabled()) {
            jsonFileLogger.startLog(null, date)
        }
        maybeDeleteFiles()
    }

    private fun maybeDeleteFiles() {
        if (deletedFiles) {
            // If we've already deleted files on this application execution, don't do it again
            return
        }
        if (applicationContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) && (csvFileLogger.isStarted || jsonFileLogger.isStarted)) {
            // Base directories should be the same, so we only need one of the two (whichever is logging) to clear old files
            var baseDirectory: File = csvFileLogger.baseDirectory
            if (baseDirectory == null) {
                baseDirectory = jsonFileLogger.baseDirectory
            }
            deleteOldFiles(baseDirectory, csvFileLogger.file, jsonFileLogger.file)
            deletedFiles = true
        }
    }

    private fun stopLogging() {
        csvFileLogger.close()
        jsonFileLogger.close()
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
