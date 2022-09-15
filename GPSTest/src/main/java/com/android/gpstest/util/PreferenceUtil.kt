package com.android.gpstest.util

import android.content.Context
import android.content.SharedPreferences
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import android.os.Build
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.util.SatelliteUtils.*

/**
 * Provides access to SharedPreferences to Activities and Services.
 */
internal object PreferenceUtil {
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
        return Application.prefs.getString(Application.app.getString(R.string.pref_key_gps_min_distance), "0") ?.toFloat() ?: 0.0f
    }

    /**
     * Returns true if the user has selected to write locations to file output, false if they have not
     */
    fun writeLocationToFile(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_location_output), false)
    }

    fun writeMeasurementToLogcat(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_as_measurement_output), false)
    }

    fun writeMeasurementsToFile(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_measurement_output), false)
    }

    fun writeNmeaToAndroidMonitor(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_as_nmea_output), true)
    }

    fun writeNmeaTimestampToLogcat(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_as_nmea_timestamp_output), true)
    }

    fun writeNmeaToFile(): Boolean {
      return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_nmea_output), false)
    }

    fun writeAntennaInfoToFileJson(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_antenna_output_json), false);
    }

    fun writeAntennaInfoToFileCsv(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_antenna_output_csv), false)
    }

    fun writeNavMessageToLogcat(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_as_navigation_message_output), false);
    }

    fun writeNavMessageToFile(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_navigation_message_output), false);
    }

    fun writeStatusToFile(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_gnss_status_output), false);
    }

    fun writeOrientationToFile(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_file_orientation_output), false);
    }

    fun injectTimeWhenLogging(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_inject_time_when_logging), true);
    }

    fun injectPsdsWhenLogging(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_inject_psds_when_logging), true);
    }

    fun isFileLoggingEnabled(): Boolean {
        return isCsvLoggingEnabled() || isJsonLoggingEnabled()
    }

    fun isCsvLoggingEnabled(): Boolean {
        return writeNmeaToFile() || writeMeasurementsToFile() || writeNavMessageToFile() || writeLocationToFile() || writeAntennaInfoToFileCsv() || writeStatusToFile() || writeOrientationToFile()
    }

    fun isJsonLoggingEnabled(): Boolean {
        return writeAntennaInfoToFileJson()
    }

    fun distanceUnits(): String {
        return Application.prefs.getString(Application.app.getString(R.string.pref_key_preferred_distance_units_v2), METERS) ?: METERS
    }

    fun speedUnits(): String {
        return Application.prefs.getString(Application.app.getString(R.string.pref_key_preferred_speed_units_v2), METERS_PER_SECOND) ?: METERS_PER_SECOND
    }

    fun coordinateFormat(): String {
        return Application.prefs.getString(
            Application.app.getString(R.string.pref_key_coordinate_format),
            Application.app.getString(R.string.preferences_coordinate_format_dd_key)
        ) ?: Application.app.getString(R.string.preferences_coordinate_format_dd_key)
    }

    fun runInBackground(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_gnss_background), false)
    }

    fun darkTheme(): Boolean {
        return Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_dark_theme), false)
    }

    fun shareIncludeAltitude(): Boolean {
        return Application.prefs.getBoolean(
            Application.app.getString(R.string.pref_key_share_include_altitude), false
        )
    }

    fun expandSignalSummary(): Boolean {
        return Application.prefs.getBoolean(
            Application.app.getString(R.string.pref_key_expand_signal_summary), false
        )
    }

    /**
     * Returns true if preferences related to raw measurements should be enabled,
     * false if they should be disabled
     */
    fun enableMeasurementsPref(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return isMeasurementsSupported(manager)
        }
        // Legacy versions before Android S
        val capabilityMeasurementsInt = Application.prefs.getInt(
            Application.app.getString(R.string.capability_key_raw_measurements),
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
            val manager = Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return isNavMessagesSupported(manager)
        }
        // Legacy versions before Android S
        val capabilityNavMessagesInt = Application.prefs.getInt(
            Application.app.getString(R.string.capability_key_nav_messages),
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isAutomaticGainControlSupported(event)) {
                agcSupport = PreferenceUtils.CAPABILITY_SUPPORTED
            } else if (agcSupport == PreferenceUtils.CAPABILITY_UNKNOWN) {
                agcSupport = PreferenceUtils.CAPABILITY_NOT_SUPPORTED
            }
        }
        // Loop through all measurements - if at least one supports, then mark as supported
        for (measurement in event.measurements) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (isAutomaticGainControlSupported(measurement)) {
                    agcSupport = PreferenceUtils.CAPABILITY_SUPPORTED
                } else if (agcSupport == PreferenceUtils.CAPABILITY_UNKNOWN) {
                    agcSupport = PreferenceUtils.CAPABILITY_NOT_SUPPORTED
                }
            }
            if (isCarrierPhaseSupported(measurement)) {
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