package com.android.gpstest.library.util

import android.content.Context
import android.content.SharedPreferences
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import android.os.Build
import com.android.gpstest.library.R

/**
 * Provides access to SharedPreferences to Activities and Services.
 */
object PreferenceUtil {
    const val SECONDS_TO_MILLISECONDS = 1000

    val METERS = "1"
    val METERS_PER_SECOND = "1"
    val KILOMETERS_PER_HOUR = "2"
    val KNOTS = "4"

    /**
     * Returns the minTime between location updates used for the LocationListener in milliseconds
     */
    fun minTimeMillis(context: Context, prefs: SharedPreferences): Long {
        val minTimeDouble: Double =
            prefs.getString(context.getString(R.string.pref_key_gps_min_time), "1")
                ?.toDouble() ?: 1.0
        return (minTimeDouble * SECONDS_TO_MILLISECONDS).toLong()
    }

    /**
     * Returns the minDistance between location updates used for the LocationLitsener in meters
     */
    fun minDistance(context: Context, prefs: SharedPreferences): Float {
        return prefs.getString(context.getString(R.string.pref_key_gps_min_distance), "0") ?.toFloat() ?: 0.0f
    }

    /**
     * Returns true if the user has selected to write locations to file output, false if they have not
     */
    fun writeLocationToFile(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_location_output), false)
    }

    fun writeMeasurementToLogcat(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_as_measurement_output), false)
    }

    fun writeMeasurementsToFile(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_measurement_output), false)
    }

    fun writeNmeaToAndroidMonitor(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_as_nmea_output), true)
    }

    fun writeNmeaTimestampToLogcat(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_as_nmea_timestamp_output), true)
    }

    fun writeNmeaToFile(context: Context, prefs: SharedPreferences): Boolean {
      return prefs.getBoolean(context.getString(R.string.pref_key_file_nmea_output), false)
    }

    fun writeAntennaInfoToFileJson(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_antenna_output_json), false);
    }

    fun writeAntennaInfoToFileCsv(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_antenna_output_csv), false)
    }

    fun writeNavMessageToLogcat(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_as_navigation_message_output), false);
    }

    fun writeNavMessageToFile(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_navigation_message_output), false);
    }

    fun writeStatusToFile(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_gnss_status_output), false);
    }

    fun writeOrientationToFile(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_file_orientation_output), false);
    }

    fun injectTimeWhenLogging(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_inject_time_when_logging), true);
    }

    fun injectPsdsWhenLogging(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_inject_psds_when_logging), true);
    }

    fun isFileLoggingEnabled(context: Context, prefs: SharedPreferences): Boolean {
        return isCsvLoggingEnabled(context, prefs) || isJsonLoggingEnabled(context, prefs)
    }

    fun isCsvLoggingEnabled(context: Context, prefs: SharedPreferences): Boolean {
        return writeNmeaToFile(context, prefs) || writeMeasurementsToFile(context, prefs) || writeNavMessageToFile(context, prefs) || writeLocationToFile(context, prefs) || writeAntennaInfoToFileCsv(context, prefs) || writeStatusToFile(context, prefs) || writeOrientationToFile(context, prefs)
    }

    fun isJsonLoggingEnabled(context: Context, prefs: SharedPreferences): Boolean {
        return writeAntennaInfoToFileJson(context, prefs)
    }

    fun distanceUnits(context: Context, prefs: SharedPreferences): String {
        return prefs.getString(context.getString(R.string.pref_key_preferred_distance_units_v2), METERS) ?: METERS
    }

    fun speedUnits(context: Context, prefs: SharedPreferences): String {
        return prefs.getString(context.getString(R.string.pref_key_preferred_speed_units_v2), METERS_PER_SECOND) ?: METERS_PER_SECOND
    }

    fun coordinateFormat(context: Context, prefs: SharedPreferences): String {
        return prefs.getString(
            context.getString(R.string.pref_key_coordinate_format),
            context.getString(R.string.preferences_coordinate_format_dd_key)
        ) ?: context.getString(R.string.preferences_coordinate_format_dd_key)
    }

    fun runInBackground(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_gnss_background), false)
    }

    private fun saveRunInBackground(context: Context, prefs: SharedPreferences, value: Boolean) {
        PreferenceUtils.saveBoolean(context.getString(R.string.pref_key_gnss_background),
                                    value,
                                    prefs)
    }

    fun darkTheme(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_dark_theme), false)
    }

    fun shareIncludeAltitude(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_share_include_altitude), false
        )
    }

    /**
     * Returns true if preferences related to raw measurements should be enabled,
     * false if they should be disabled
     */
    fun enableMeasurementsPref(context: Context, prefs: SharedPreferences): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return SatelliteUtils.isMeasurementsSupported(manager)
        }
        // Legacy versions before Android S
        val capabilityMeasurementsInt = prefs.getInt(
            context.getString(R.string.capability_key_raw_measurements),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        return capabilityMeasurementsInt != PreferenceUtils.CAPABILITY_NOT_SUPPORTED
    }

    /**
     * Returns true if preferences related to navigation messages should be enabled,
     * false if they should be disabled
     */
    fun enableNavMessagesPref(context: Context, prefs: SharedPreferences): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return SatelliteUtils.isNavMessagesSupported(manager)
        }
        // Legacy versions before Android S
        val capabilityNavMessagesInt = prefs.getInt(
            context.getString(R.string.capability_key_nav_messages),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        return capabilityNavMessagesInt != PreferenceUtils.CAPABILITY_NOT_SUPPORTED
    }

    /**
     * Saves device capabilities for GNSS measurements and related information from the given [event]
     */
    fun saveMeasurementCapabilities(context: Context, event: GnssMeasurementsEvent, prefs: SharedPreferences) {
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
            context.getString(R.string.capability_key_measurement_automatic_gain_control),
            agcSupport,
            prefs
        )
        PreferenceUtils.saveInt(
            context.getString(R.string.capability_key_measurement_delta_range),
            carrierPhaseSupport,
            prefs
        )
    }

    /**
     * Creates a new preference listener that will invoke the provide [cancelFlows] function (e.g., to cancel jobs)
     * when the user turns off tracking via the UI.
     *
     * Returns a reference to the OnSharedPreferenceChangeListener so it can be held by the calling class, as
     * anonymous preference listeners tend to get GC'd by Android.
     */
    fun newStopTrackingListener(cancelFlows: () -> Unit, prefs: SharedPreferences): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PreferenceUtils.KEY_SERVICE_TRACKING_ENABLED) {
                if (!PreferenceUtils.isTrackingStarted(prefs)) {
                    cancelFlows()
                }
            }
        }
    }

    /**
     * Creates a new preference listener that will invoke the provide [initLogging] function
     * when the user turns on file logging via any of the Settings options and all of the other file
     * logging settings were previously off
     * @param context
     * @param initLogging
     * @param prefs
     * Returns a reference to the OnSharedPreferenceChangeListener so it can be held by the calling class, as
     * anonymous preference listeners tend to get GC'd by Android.
     */
    fun newFileLoggingListener(context: Context, initLogging: () -> Unit, prefs: SharedPreferences): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == context.getString(R.string.pref_key_file_location_output) ||
                key == context.getString(R.string.pref_key_file_measurement_output) ||
                key == context.getString(R.string.pref_key_file_nmea_output) ||
                key == context.getString(R.string.pref_key_file_navigation_message_output) ||
                key == context.getString(R.string.pref_key_file_antenna_output_csv) ||
                key == context.getString(R.string.pref_key_file_antenna_output_json)) {

                // Enable background execution for file logging
                saveRunInBackground(context, prefs, true)

                // Count number of file logging preferences that are enabled
                val loggingEnabled = arrayOf(writeLocationToFile(context, prefs), writeMeasurementsToFile(context, prefs), writeNmeaToFile(context, prefs), writeNavMessageToFile(context, prefs), writeAntennaInfoToFileCsv(context, prefs), writeAntennaInfoToFileJson(context, prefs))
                val enabledCount = loggingEnabled.count { it }

                if (enabledCount == 1) {
                    if (key == context.getString(R.string.pref_key_file_location_output) &&
                        writeLocationToFile(context, prefs)
                    ) {
                        // Location file logging was just enabled
                        initLogging()
                    }
                    if (key == context.getString(R.string.pref_key_file_measurement_output) &&
                        writeMeasurementsToFile(context, prefs)
                    ) {
                        // Measurement file logging was just enabled
                        initLogging()
                    }
                    if (key == context.getString(R.string.pref_key_file_nmea_output) &&
                        writeNmeaToFile(context, prefs)
                    ) {
                        // NMEA file logging was just enabled
                        initLogging()
                    }
                    if (key == context.getString(R.string.pref_key_file_navigation_message_output) &&
                        writeNavMessageToFile(context, prefs)
                    ) {
                        // Nav message file logging was just enabled
                        initLogging()
                    }
                    if (key == context.getString(R.string.pref_key_file_antenna_output_csv) &&
                        writeAntennaInfoToFileCsv(context, prefs)
                    ) {
                        // Antenna CSV file logging was just enabled
                        initLogging()
                    }
                    if (key == context.getString(R.string.pref_key_file_antenna_output_json) &&
                        writeAntennaInfoToFileJson(context,prefs)
                    ) {
                        // Antenna JSON file logging was just enabled
                        initLogging()
                    }
                }
            }
        }
    }
}