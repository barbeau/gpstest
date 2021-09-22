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
package com.android.gpstest.util

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.GnssMeasurementsEvent
import androidx.core.app.ActivityCompat
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.util.SharedPreferenceUtil.METERS
import com.android.gpstest.util.SharedPreferenceUtil.prefDistanceUnits

/**
 * Returns the `location` object as a human readable string for use in a notification title
 */
fun android.location.Location?.toNotificationTitle(): String {
    return if (this != null) {
        toString(latitude, longitude)
    } else {
        "Unknown location"
    }
}

/**
 * Returns the `location` object as a human readable string for use in a notification summary
 */
fun android.location.Location?.toNotificationSummary(): String {
    return if (this != null) {
        val coordinateFormat = Application.prefs.getString(
            Application.app.getString(R.string.pref_key_coordinate_format),
            Application.app.getString(R.string.preferences_coordinate_format_dd_key)
        )
        val resources = Application.app.resources
        val lat: String
        val lon: String
        val alt: String
        when (coordinateFormat) {
            "dd" -> {
                // Decimal degrees
                lat =
                    resources.getString(
                        R.string.gps_latitude_value,
                        this.latitude
                    )
                lon =
                    resources.getString(
                        R.string.gps_longitude_value,
                        this.longitude
                    )

            }
            "dms" -> {
                // Degrees minutes seconds
               lat =
                    UIUtils.getDMSFromLocation(
                        Application.app,
                        this.latitude,
                        UIUtils.COORDINATE_LATITUDE
                    )
                lon =
                    UIUtils.getDMSFromLocation(
                        Application.app,
                        this.longitude,
                        UIUtils.COORDINATE_LONGITUDE
                    )
            }
            "ddm" -> {
                // Degrees decimal minutes
                lat =
                    UIUtils.getDDMFromLocation(
                        Application.app,
                        this.latitude,
                        UIUtils.COORDINATE_LATITUDE
                    )
                lon =
                    UIUtils.getDDMFromLocation(
                        Application.app,
                        this.longitude,
                        UIUtils.COORDINATE_LONGITUDE
                    )
            }
            else -> {
                // Decimal degrees
                lat =
                    resources.getString(
                        R.string.gps_latitude_value,
                        this.latitude
                    )
                lon =
                    resources.getString(
                        R.string.gps_longitude_value,
                        this.longitude
                    )
            }
        }
        if (this.hasAltitude()) {
            alt = if (prefDistanceUnits().equals(METERS, ignoreCase = true)) {
                resources.getString(
                    R.string.gps_altitude_value_meters,
                    this.altitude
                )
            } else {
                // Feet
                resources.getString(
                    R.string.gps_altitude_value_feet,
                    UIUtils.toFeet(this.altitude)
                )
            }
        } else {
            alt = ""
        }

        "$lat $lon $alt"
    } else {
        "Unknown location"
    }
}

fun toString(lat: Double, lon: Double): String {
    return "$lat, $lon"
}

/**
 * Helper functions to simplify permission checks/requests.
 */
fun Context.hasPermission(permission: String): Boolean {

    // Background permissions didn't exit prior to Q, so it's approved by default.
    if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION &&
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        return true
    }

    return ActivityCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

/**
 * Provides access to SharedPreferences for location to Activities and Services.
 */
internal object SharedPreferenceUtil {
    const val SECONDS_TO_MILLISECONDS = 1000

    val METERS =
        Application.app.resources.getStringArray(R.array.preferred_distance_units_values)[0]
    val METERS_PER_SECOND =
        Application.app.resources.getStringArray(R.array.preferred_speed_units_values)[0]
    val KILOMETERS_PER_HOUR =
        Application.app.resources.getStringArray(R.array.preferred_speed_units_values)[1]

    /**
     * Returns the minTime between location updates used for the LocationListener in milliseconds
     */
    fun minTimeMillis(): Long {
        val minTimeDouble: Double =
            Application.prefs
                .getString(Application.app.getString(R.string.pref_key_gps_min_time), "1")
                ?.toDouble() ?: 1.0
        return (minTimeDouble * SECONDS_TO_MILLISECONDS).toLong()
    }

    /**
     * Returns the minDistance between location updates used for the LocationLitsener in meters
     */
    fun minDistance(): Float {
        return Application.prefs
            .getString(Application.app.getString(R.string.pref_key_gps_min_distance), "0")
            ?.toFloat() ?: 0.0f
    }

    /**
     * Returns true if the user has selected to write locations to file output, false if they have not
     */
    fun writeLocationToFile(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_file_location_output), false)
    }

    fun writeMeasurementToLogcat(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_as_measurement_output), false)
    }

    fun writeMeasurementsToFile(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_file_measurement_output), false)
    }

    fun writeNmeaToAndroidMonitor(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_as_nmea_output), true)
    }

    fun writeNmeaTimestampToLogcat(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_as_nmea_timestamp_output), true)
    }

    fun writeNmeaToFile(): Boolean {
      return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_file_nmea_output), false)
    }

    fun writeAntennaInfoToFileJson(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_file_antenna_output_json), false);
    }

    fun writeAntennaInfoToFileCsv(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_file_antenna_output_csv), false)
    }

    fun writeNavMessageToLogcat(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_as_navigation_message_output), false);
    }

    fun writeNavMessageToFile(): Boolean {
        return Application.prefs
            .getBoolean(Application.app.getString(R.string.pref_key_file_navigation_message_output), false);
    }

    fun isFileLoggingEnabled(): Boolean {
        return isCsvLoggingEnabled() || isJsonLoggingEnabled()
    }

    fun isCsvLoggingEnabled(): Boolean {
        return writeNmeaToFile() || writeMeasurementsToFile() || writeNavMessageToFile() || writeLocationToFile() || writeAntennaInfoToFileCsv()
    }

    fun isJsonLoggingEnabled(): Boolean {
        return writeAntennaInfoToFileJson()
    }

    fun prefDistanceUnits(): String? {
        return Application.prefs
            .getString(Application.app.getString(R.string.pref_key_preferred_distance_units_v2), METERS);
    }


    /**
     * Saves device capabilities for GNSS measurements and related information from the given [event]
     */
    fun saveMeasurementCapabilities(event: GnssMeasurementsEvent) {
        var agcSupport = PreferenceUtils.CAPABILITY_UNKNOWN
        var carrierPhaseSupport = PreferenceUtils.CAPABILITY_UNKNOWN
        // Loop through all measurements - if at least one supports, then mark as supported
        for (measurement in event.measurements) {
            if (SatelliteUtils.isAutomaticGainControlSupported(measurement)) {
                agcSupport = PreferenceUtils.CAPABILITY_SUPPORTED
            } else if (agcSupport == PreferenceUtils.CAPABILITY_UNKNOWN) {
                agcSupport = PreferenceUtils.CAPABILITY_NOT_SUPPORTED
            }
            if (SatelliteUtils.isCarrierPhaseSupported(measurement)) {
                carrierPhaseSupport = PreferenceUtils.CAPABILITY_SUPPORTED
            } else if (carrierPhaseSupport == PreferenceUtils.CAPABILITY_UNKNOWN) {
                carrierPhaseSupport = PreferenceUtils.CAPABILITY_NOT_SUPPORTED
            }
        }
        PreferenceUtils.saveInt(
            Application.app
                .getString(R.string.capability_key_measurement_automatic_gain_control),
            agcSupport
        )
        PreferenceUtils.saveInt(
            Application.app
                .getString(R.string.capability_key_measurement_delta_range),
            carrierPhaseSupport
        )
    }

    /**
     * Creates a new preference listener that will invoke the provide [cancelFlows] function (e.g., to cancel jobs)
     * when the user turns off tracking via the UI.
     *
     * Returns a reference to the OnSharedPreferenceChangeListener so it can be held by the calling class, as
     * anonymous preference listeners tend to get GC'd by Android.
     */
    fun newStopTrackingListener(cancelFlows: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PreferenceUtils.KEY_SERVICE_TRACKING_ENABLED) {
                if (!PreferenceUtils.isTrackingStarted()) {
                    cancelFlows()
                }
            }
        }
    }

    /**
     * Creates a new preference listener that will invoke the provide [initLogging] function
     * when the user turns on file logging via any of the Settings options and all of the other file
     * logging settings were previously off
     *
     * Returns a reference to the OnSharedPreferenceChangeListener so it can be held by the calling class, as
     * anonymous preference listeners tend to get GC'd by Android.
     */
    fun newFileLoggingListener(initLogging: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Application.app.getString(R.string.pref_key_file_location_output) ||
                key == Application.app.getString(R.string.pref_key_file_measurement_output) ||
                key == Application.app.getString(R.string.pref_key_file_nmea_output) ||
                key == Application.app.getString(R.string.pref_key_file_navigation_message_output) ||
                key == Application.app.getString(R.string.pref_key_file_antenna_output_csv) ||
                key == Application.app.getString(R.string.pref_key_file_antenna_output_json)) {

                // Count number of file logging preferences that are enabled
                val loggingEnabled = arrayOf(writeLocationToFile(), writeMeasurementsToFile(), writeNmeaToFile(), writeNavMessageToFile(), writeAntennaInfoToFileCsv(), writeAntennaInfoToFileJson())
                val enabledCount = loggingEnabled.count { it }

                if (enabledCount == 1) {
                    if (key == Application.app.getString(R.string.pref_key_file_location_output) &&
                        writeLocationToFile()
                    ) {
                        // Location file logging was just enabled
                        initLogging()
                    }
                    if (key == Application.app.getString(R.string.pref_key_file_measurement_output) &&
                        writeMeasurementsToFile()
                    ) {
                        // Measurement file logging was just enabled
                        initLogging()
                    }
                    if (key == Application.app.getString(R.string.pref_key_file_nmea_output) &&
                        writeNmeaToFile()
                    ) {
                        // NMEA file logging was just enabled
                        initLogging()
                    }
                    if (key == Application.app.getString(R.string.pref_key_file_navigation_message_output) &&
                        writeNavMessageToFile()
                    ) {
                        // Nav message file logging was just enabled
                        initLogging()
                    }
                    if (key == Application.app.getString(R.string.pref_key_file_antenna_output_csv) &&
                        writeAntennaInfoToFileCsv()
                    ) {
                        // Antenna CSV file logging was just enabled
                        initLogging()
                    }
                    if (key == Application.app.getString(R.string.pref_key_file_antenna_output_json) &&
                        writeAntennaInfoToFileJson()
                    ) {
                        // Antenna JSON file logging was just enabled
                        initLogging()
                    }
                }
            }
        }
    }
}