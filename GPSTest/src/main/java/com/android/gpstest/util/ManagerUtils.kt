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
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.R
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.util.SharedPreferenceUtil.METERS
import com.android.gpstest.util.SharedPreferenceUtil.coordinateFormat
import com.android.gpstest.util.SharedPreferenceUtil.distanceUnits
import com.android.gpstest.util.SharedPreferenceUtil.speedUnits
import java.util.concurrent.TimeUnit

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
// FIXME - Collapse this into other formatting functions in StatusScreen
fun android.location.Location?.toNotificationSummary(): String {
    return if (this != null) {
        val resources = app.resources
        val lat: String
        val lon: String
        val alt: String
        when (coordinateFormat()) {
            "dd" -> {
                // Decimal degrees
                lat =
                    resources.getString(
                        R.string.lat_or_lon,
                        this.latitude
                    )
                lon =
                    resources.getString(
                        R.string.lat_or_lon,
                        this.longitude
                    )

            }
            "dms" -> {
                // Degrees minutes seconds
               lat =
                    UIUtils.getDMSFromLocation(
                        app,
                        this.latitude,
                        CoordinateType.LATITUDE
                    )
                lon =
                    UIUtils.getDMSFromLocation(
                        app,
                        this.longitude,
                        CoordinateType.LONGITUDE
                    )
            }
            "ddm" -> {
                // Degrees decimal minutes
                lat =
                    UIUtils.getDDMFromLocation(
                        app,
                        this.latitude,
                        CoordinateType.LATITUDE
                    )
                lon =
                    UIUtils.getDDMFromLocation(
                        app,
                        this.longitude,
                        CoordinateType.LONGITUDE
                    )
            }
            else -> {
                // Decimal degrees
                lat =
                    resources.getString(
                        R.string.lat_or_lon,
                        this.latitude
                    )
                lon =
                    resources.getString(
                        R.string.lat_or_lon,
                        this.longitude
                    )
            }
        }
        if (this.hasAltitude()) {
            alt = if (distanceUnits().equals(METERS, ignoreCase = true)) {
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
        app.resources.getStringArray(R.array.preferred_distance_units_values)[0]
    val METERS_PER_SECOND =
        app.resources.getStringArray(R.array.preferred_speed_units_values)[0]
    val KILOMETERS_PER_HOUR =
        app.resources.getStringArray(R.array.preferred_speed_units_values)[1]

    /**
     * Returns the minTime between location updates used for the LocationListener in milliseconds
     */
    fun minTimeMillis(): Long {
        val minTimeDouble: Double =
            prefs
                .getString(app.getString(R.string.pref_key_gps_min_time), "1")
                ?.toDouble() ?: 1.0
        return (minTimeDouble * SECONDS_TO_MILLISECONDS).toLong()
    }

    /**
     * Returns the minDistance between location updates used for the LocationLitsener in meters
     */
    fun minDistance(): Float {
        return prefs.getString(app.getString(R.string.pref_key_gps_min_distance), "0") ?.toFloat() ?: 0.0f
    }

    /**
     * Returns true if the user has selected to write locations to file output, false if they have not
     */
    fun writeLocationToFile(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_file_location_output), false)
    }

    fun writeMeasurementToLogcat(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_as_measurement_output), false)
    }

    fun writeMeasurementsToFile(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_file_measurement_output), false)
    }

    fun writeNmeaToAndroidMonitor(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_as_nmea_output), true)
    }

    fun writeNmeaTimestampToLogcat(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_as_nmea_timestamp_output), true)
    }

    fun writeNmeaToFile(): Boolean {
      return prefs.getBoolean(app.getString(R.string.pref_key_file_nmea_output), false)
    }

    fun writeAntennaInfoToFileJson(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_file_antenna_output_json), false);
    }

    fun writeAntennaInfoToFileCsv(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_file_antenna_output_csv), false)
    }

    fun writeNavMessageToLogcat(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_as_navigation_message_output), false);
    }

    fun writeNavMessageToFile(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_file_navigation_message_output), false);
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

    fun distanceUnits(): String {
        return prefs.getString(app.getString(R.string.pref_key_preferred_distance_units_v2), METERS) ?: METERS
    }

    fun speedUnits(): String {
        return prefs.getString(app.getString(R.string.pref_key_preferred_speed_units_v2), METERS_PER_SECOND) ?: METERS_PER_SECOND
    }

    fun coordinateFormat(): String {
        return prefs.getString(
            app.getString(R.string.pref_key_coordinate_format),
            app.getString(R.string.preferences_coordinate_format_dd_key)
        ) ?: app.getString(R.string.preferences_coordinate_format_dd_key)
    }

    fun runInBackground(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_gnss_background), false)
    }

    fun darkTheme(): Boolean {
        return prefs.getBoolean(app.getString(R.string.pref_key_dark_theme), false)
    }

    fun shareIncludeAltitude(): Boolean {
        return prefs.getBoolean(
            app.getString(R.string.pref_key_share_include_altitude), false
        )
    }

    /**
     * Returns true if preferences related to raw measurements should be enabled,
     * false if they should be disabled
     */
    fun enableMeasurementsPref(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return SatelliteUtils.isMeasurementsSupported(manager)
        }
        // Legacy versions before Android S
        val capabilityMeasurementsInt = prefs.getInt(
            app.getString(R.string.capability_key_raw_measurements),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        return capabilityMeasurementsInt != PreferenceUtils.CAPABILITY_NOT_SUPPORTED
    }

    /**
     * Returns true if preferences related to navigation messages should be enabled,
     * false if they should be disabled
     */
    fun enableNavMessagesPref(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return SatelliteUtils.isNavMessagesSupported(manager)
        }
        // Legacy versions before Android S
        val capabilityNavMessagesInt = prefs.getInt(
            app.getString(R.string.capability_key_nav_messages),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        return capabilityNavMessagesInt != PreferenceUtils.CAPABILITY_NOT_SUPPORTED
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
            app
                .getString(R.string.capability_key_measurement_automatic_gain_control),
            agcSupport
        )
        PreferenceUtils.saveInt(
            app
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
            if (key == app.getString(R.string.pref_key_file_location_output) ||
                key == app.getString(R.string.pref_key_file_measurement_output) ||
                key == app.getString(R.string.pref_key_file_nmea_output) ||
                key == app.getString(R.string.pref_key_file_navigation_message_output) ||
                key == app.getString(R.string.pref_key_file_antenna_output_csv) ||
                key == app.getString(R.string.pref_key_file_antenna_output_json)) {

                // Count number of file logging preferences that are enabled
                val loggingEnabled = arrayOf(writeLocationToFile(), writeMeasurementsToFile(), writeNmeaToFile(), writeNavMessageToFile(), writeAntennaInfoToFileCsv(), writeAntennaInfoToFileJson())
                val enabledCount = loggingEnabled.count { it }

                if (enabledCount == 1) {
                    if (key == app.getString(R.string.pref_key_file_location_output) &&
                        writeLocationToFile()
                    ) {
                        // Location file logging was just enabled
                        initLogging()
                    }
                    if (key == app.getString(R.string.pref_key_file_measurement_output) &&
                        writeMeasurementsToFile()
                    ) {
                        // Measurement file logging was just enabled
                        initLogging()
                    }
                    if (key == app.getString(R.string.pref_key_file_nmea_output) &&
                        writeNmeaToFile()
                    ) {
                        // NMEA file logging was just enabled
                        initLogging()
                    }
                    if (key == app.getString(R.string.pref_key_file_navigation_message_output) &&
                        writeNavMessageToFile()
                    ) {
                        // Nav message file logging was just enabled
                        initLogging()
                    }
                    if (key == app.getString(R.string.pref_key_file_antenna_output_csv) &&
                        writeAntennaInfoToFileCsv()
                    ) {
                        // Antenna CSV file logging was just enabled
                        initLogging()
                    }
                    if (key == app.getString(R.string.pref_key_file_antenna_output_json) &&
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

/**
 * Provides access to formatting utilities for view in the UI
 **/
internal object FormatUtils {
    fun formatLatOrLon(latOrLong: Double, coordinateType: CoordinateType): String {
        if (latOrLong == 0.0) return "             "

        when (coordinateFormat()) {
            "dd" -> {
                // Decimal degrees
                return app.getString(R.string.lat_or_lon, latOrLong)
            }
            "dms" -> {
                // Degrees minutes seconds
                return UIUtils.getDMSFromLocation(
                    app,
                    latOrLong,
                    coordinateType
                )
            }
            "ddm" -> {
                // Degrees decimal minutes
                return UIUtils.getDDMFromLocation(
                    app,
                    latOrLong,
                    coordinateType
                )
            }
            else -> {
                // Decimal degrees
                return app.getString(R.string.lat_or_lon, latOrLong)
            }
        }
    }

    fun formatAltitude(location: Location): String {
        if (location.hasAltitude()) {
            val text = when {
                distanceUnits().equals(METERS, ignoreCase = true) -> {
                    app.getString(R.string.gps_altitude_value_meters, location.altitude)
                }
                else -> {
                    // Feet
                    app.getString(
                        R.string.gps_altitude_value_feet,
                        UIUtils.toFeet(location.altitude)
                    )
                }
            }
            return text
        } else {
            return ""
        }
    }

    fun formatSpeed(location: Location): String {
        if (location.hasSpeed()) {
            val text = when {
                speedUnits()
                    .equals(SharedPreferenceUtil.METERS_PER_SECOND, ignoreCase = true) -> {
                    app.getString(R.string.gps_speed_value_meters_sec, location.speed)
                }
                speedUnits()
                    .equals(SharedPreferenceUtil.KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    app.getString(
                        R.string.gps_speed_value_kilometers_hour,
                        UIUtils.toKilometersPerHour(location.speed)
                    )
                }
                else -> {
                    // Miles per hour
                    app.getString(
                        R.string.gps_speed_value_miles_hour,
                        UIUtils.toMilesPerHour(location.speed)
                    )
                }
            }
            return text
        } else {
            return ""
        }
    }

    /**
     * Returns a human-readable description of the time-to-first-fix, such as "38 sec"
     *
     * @param ttff time-to-first fix, in milliseconds
     * @return a human-readable description of the time-to-first-fix, such as "38 sec"
     */
    fun formatTtff(ttff: Int): String {
        return if (ttff == 0) {
            ""
        } else {
            TimeUnit.MILLISECONDS.toSeconds(ttff.toLong()).toString() + " sec"
        }
    }

    fun formatAccuracy(location: Location): String {
        if (SatelliteUtils.isVerticalAccuracySupported(location)) {
            if (distanceUnits().equals(METERS, ignoreCase = true)) {
                return app.getString(
                        R.string.gps_hor_and_vert_accuracy_value_meters,
                        location.accuracy,
                        location.verticalAccuracyMeters
                    )
            } else {
                // Feet
                return app.getString(
                        R.string.gps_hor_and_vert_accuracy_value_feet,
                        UIUtils.toFeet(location.accuracy.toDouble()),
                        UIUtils.toFeet(
                            location.verticalAccuracyMeters.toDouble()
                        )
                    )
            }
        } else {
            if (location.hasAccuracy()) {
                return if (distanceUnits().equals(METERS, ignoreCase = true)) {
                    app.getString(
                        R.string.gps_accuracy_value_meters, location.accuracy
                    )
                } else {
                    // Feet
                    app.getString(
                        R.string.gps_accuracy_value_feet,
                        UIUtils.toFeet(location.accuracy.toDouble())
                    )
                }
            }
        }
        return ""
    }

    fun formatAltitudeMsl(altitudeMsl: Double): String {
        if (altitudeMsl.isNaN()) return ""

        return if (distanceUnits().equals(METERS, ignoreCase = true)) {
            app.getString(
                R.string.gps_altitude_msl_value_meters,
                altitudeMsl)
        } else {
            app.getString(
                R.string.gps_altitude_msl_value_feet,
                UIUtils.toFeet(altitudeMsl)
            )
        }
    }

    fun formatSpeedAccuracy(location: Location): String {
        if (SatelliteUtils.isSpeedAndBearingAccuracySupported() && location.hasSpeedAccuracy()) {
            when {
                speedUnits()
                    .equals(SharedPreferenceUtil.METERS_PER_SECOND, ignoreCase = true) -> {
                    return app.getString(
                        R.string.gps_speed_acc_value_meters_sec,
                        location.speedAccuracyMetersPerSecond
                    )
                }
                speedUnits()
                    .equals(SharedPreferenceUtil.KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    return app.getString(
                        R.string.gps_speed_acc_value_km_hour,
                        UIUtils.toKilometersPerHour(location.speedAccuracyMetersPerSecond)
                    )
                }
                else -> {
                    // Miles per hour
                    return app.getString(
                        R.string.gps_speed_acc_value_miles_hour,
                        UIUtils.toMilesPerHour(location.speedAccuracyMetersPerSecond)
                    )
                }
            }
        }
        return ""
    }

    fun formatBearingAccuracy(location: Location): String {
        return if (SatelliteUtils.isSpeedAndBearingAccuracySupported() && location.hasBearingAccuracy()) {
            app.getString(
                R.string.gps_bearing_acc_value,
                location.bearingAccuracyDegrees
            )
        } else {
            ""
        }
    }
}