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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.io.CsvFileLogger
import com.android.gpstest.io.JsonFileLogger
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.model.SatelliteGroup
import com.android.gpstest.library.model.SatelliteMetadata
import com.android.gpstest.library.util.FormatUtils.toNotificationTitle
import com.android.gpstest.library.util.IOUtils.deleteOldFiles
import com.android.gpstest.library.util.IOUtils.forcePsdsInjection
import com.android.gpstest.library.util.IOUtils.forceTimeInjection
import com.android.gpstest.library.util.IOUtils.writeMeasurementToLogcat
import com.android.gpstest.library.util.IOUtils.writeNavMessageToAndroidStudio
import com.android.gpstest.library.util.IOUtils.writeNmeaToAndroidStudio
import com.android.gpstest.library.util.LibUIUtils.toNotificationSummary
import com.android.gpstest.library.util.PreferenceUtil
import com.android.gpstest.library.util.PreferenceUtil.isCsvLoggingEnabled
import com.android.gpstest.library.util.PreferenceUtil.isJsonLoggingEnabled
import com.android.gpstest.library.util.PreferenceUtil.writeAntennaInfoToFileCsv
import com.android.gpstest.library.util.PreferenceUtil.writeAntennaInfoToFileJson
import com.android.gpstest.library.util.PreferenceUtil.writeLocationToFile
import com.android.gpstest.library.util.PreferenceUtil.writeMeasurementToLogcat
import com.android.gpstest.library.util.PreferenceUtil.writeMeasurementsToFile
import com.android.gpstest.library.util.PreferenceUtil.writeNavMessageToFile
import com.android.gpstest.library.util.PreferenceUtil.writeNavMessageToLogcat
import com.android.gpstest.library.util.PreferenceUtil.writeNmeaTimestampToLogcat
import com.android.gpstest.library.util.PreferenceUtil.writeNmeaToAndroidMonitor
import com.android.gpstest.library.util.PreferenceUtil.writeNmeaToFile
import com.android.gpstest.library.util.PreferenceUtil.writeOrientationToFile
import com.android.gpstest.library.util.PreferenceUtil.writeStatusToFile
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.SatelliteUtil.toSatelliteGroup
import com.android.gpstest.library.util.SatelliteUtil.toSatelliteStatus
import com.android.gpstest.library.util.SatelliteUtils
import com.android.gpstest.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * Service tracks location, logs to files, and shows a notification to the user.
 *
 * Flows are kept active by this Service while it is bound and started.
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

    // We save a local reference to last location and SatelliteStatus to create a Notification
    private var currentLocation: Location? = null
    private var currentSatellites: SatelliteGroup = SatelliteGroup(emptyMap(), SatelliteMetadata())

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null
    private var nmeaFlow: Job? = null
    private var navMessageFlow: Job? = null
    private var measurementFlow: Job? = null
    private var antennaFlow: Job? = null
    private var gnssFlow: Job? = null
    private var sensorFlow: Job? = null

    lateinit var csvFileLogger: CsvFileLogger
    lateinit var jsonFileLogger: JsonFileLogger

    // Preference listener that will init the loggers if the user changes Settings while Service is running
    private val loggingSettingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        PreferenceUtil.newFileLoggingListener(app, { initLogging() }, prefs)
    private var deletedFiles = false
    private var injectedAssistData = false

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
                    PreferenceUtils.saveTrackingStarted(false, prefs)
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

        PreferenceUtils.saveTrackingStarted(true, prefs)

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
            PreferenceUtils.saveTrackingStarted(false, prefs)
            removeOngoingActivityNotification()
            currentLocation = null
            currentSatellites = SatelliteGroup(emptyMap(), SatelliteMetadata())
        } catch (unlikely: SecurityException) {
            PreferenceUtils.saveTrackingStarted(true, prefs)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    @SuppressLint("NewApi")
    @ExperimentalCoroutinesApi
    private fun observeFlows() {
        observeLocationFlow()
        observeGnssFlow()
        observeNmeaFlow()
        observeNavMessageFlow()
        observeMeasurementsFlow()
        observeSensorFlow()
        if (SatelliteUtils.isGnssAntennaInfoSupported(getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
            observeAntennaFlow()
        }
    }

    private fun cancelFlows() {
        locationFlow?.cancel()
        gnssFlow?.cancel()
        nmeaFlow?.cancel()
        navMessageFlow?.cancel()
        measurementFlow?.cancel()
        antennaFlow?.cancel()
        sensorFlow?.cancel()
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
                    buildNotification(it, currentSatellites)
                )

                GlobalScope.launch(Dispatchers.IO) {
                    if (writeLocationToFile(app, prefs)) {
                        initLogging()
                        csvFileLogger.onLocationChanged(it)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeGnssFlow() {
        if (gnssFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        gnssFlow = repository.getGnssStatus()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .map { it.toSatelliteStatus() }
            .onEach {
                //Log.d(TAG, "Service SatelliteStatus: $it")
                // Note - this Flow needs to be active so the Activity/Fragments get TTFF
                // when it's created while the service is running in the background
                currentSatellites = it.toSatelliteGroup()

                // Show location in notification
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(currentLocation, currentSatellites)
                )
                // Log Status
                GlobalScope.launch(Dispatchers.IO) {
                    if (writeStatusToFile(app, prefs)) {
                        initLogging()
                        csvFileLogger.onGnssStatusChanged(it, currentLocation)
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
                    if (writeNmeaToAndroidMonitor(app, prefs)) {
                        writeNmeaToAndroidStudio(
                            it.message,
                            if (writeNmeaTimestampToLogcat(app, prefs)) it.timestamp else Long.MIN_VALUE
                        )
                    }
                    if (writeNmeaToFile(app, prefs)) {
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
                    if (writeNavMessageToLogcat(app, prefs)) {
                        writeNavMessageToAndroidStudio(it)
                    }
                    if (writeNavMessageToFile(app, prefs)) {
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
                    if (writeMeasurementToLogcat(app, prefs)) {
                        for (m in it.measurements) {
                            writeMeasurementToLogcat(m)
                        }
                    }
                    if (writeMeasurementsToFile(app, prefs)) {
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
                    if (writeAntennaInfoToFileCsv(app, prefs) || writeAntennaInfoToFileJson(app, prefs)) {
                        initLogging()
                    }
                    if (writeAntennaInfoToFileCsv(app, prefs)) {
                        csvFileLogger.onGnssAntennaInfoReceived(it)
                    }
                    if (writeAntennaInfoToFileJson(app, prefs)) {
                        jsonFileLogger.onGnssAntennaInfoReceived(it)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeSensorFlow() {
        if (sensorFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        sensorFlow = repository.getSensorUpdates()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service sensor: orientation ${it.values[0]}, tilt ${it.values[1]}")
                GlobalScope.launch(Dispatchers.IO) {
                    if (writeOrientationToFile(app, prefs)) {
                        initLogging()
                        csvFileLogger.onOrientationChanged(
                            it,
                            System.currentTimeMillis(),
                            SystemClock.elapsedRealtime()
                        )
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun goForegroundOrStopSelf() {
        lifecycleScope.launch {
            // We may have been restarted by the system - check if we're still monitoring data
            if (PreferenceUtils.isTrackingStarted(prefs)) {
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
            startForeground(NOTIFICATION_ID, buildNotification(currentLocation, currentSatellites))
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
    private fun buildNotification(location: Location?, satellites: SatelliteGroup): Notification {
        val titleText = satellites.toNotificationTitle(app)
        val summaryText = location?.toNotificationSummary(app, prefs) ?: getString(R.string.no_location_text)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(summaryText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification
        val launchActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            // NOTE: The above causes the activity/viewmodel to be recreated from scratch for Accuracy when it's already visible
            // and the notification is tapped (strangely if it's destroyed Accuracy viewmodel seems to keep it's state)
            // FLAG_ACTIVITY_REORDER_TO_FRONT seems like it should work, but if this is used then onResume() is called
            // again (and onPause() is never called). This seems to freeze up Status into a blank state because GNSS inits again.
        }
        val openActivityPendingIntent = PendingIntentCompat.getActivity(
            applicationContext,
            System.currentTimeMillis().toInt(),
            launchActivityIntent,
            0,
            false
        )

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java).apply {
            putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)
        }
        val stopServicePendingIntent = PendingIntentCompat.getService(
            applicationContext,
            System.currentTimeMillis().toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
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
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(PRIORITY_LOW) // For < API 26
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS) // For < API 26
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
    @Synchronized
    private fun initLogging() {
        // Inject time and/or PSDS to make sure timestamps and assistance are as updated as possible
        maybeInjectAssistData()

        val date = Date()
        if (!csvFileLogger.isStarted && isCsvLoggingEnabled(app, prefs)) {
            // User has granted permissions and has chosen to log at least one data type
            csvFileLogger.startLog(null, date)
        }

        if (!jsonFileLogger.isStarted && isJsonLoggingEnabled(app, prefs)) {
            jsonFileLogger.startLog(null, date)
        }
        maybeDeleteFiles()
    }

    private fun maybeInjectAssistData() {
        if (injectedAssistData) {
            // Only inject once per logging session
            return
        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (PreferenceUtil.injectTimeWhenLogging(app, prefs)) {
            forceTimeInjection(app, locationManager)
        }
        if (PreferenceUtil.injectPsdsWhenLogging(app, prefs)) {
            forcePsdsInjection(app, locationManager)
        }
        injectedAssistData = true
    }

    private fun maybeDeleteFiles() {
        if (deletedFiles) {
            // If we've already deleted files on this application execution, don't do it again
            return
        }
        if (csvFileLogger.isStarted || jsonFileLogger.isStarted) {
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
