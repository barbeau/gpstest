/*
 * Copyright (C) 2013-2021 Sean J. Barbeau
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
package com.android.gpstest.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.localeManager
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.BuildConfig
import com.android.gpstest.R
import com.android.gpstest.library.util.LibUIUtils.resetActivityTitle
import com.android.gpstest.library.util.PermissionUtils
import com.android.gpstest.library.util.PreferenceUtil
import com.android.gpstest.library.util.PreferenceUtil.enableMeasurementsPref
import com.android.gpstest.library.util.PreferenceUtil.enableNavMessagesPref
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.SatelliteUtils
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Preferences : PreferenceActivity(), OnSharedPreferenceChangeListener {
    var forceFullGnssMeasurements: CheckBoxPreference? = null
    var txtMinTime: EditTextPreference? = null
    var txtMinDistance: EditTextPreference? = null
    var chkDarkTheme: CheckBoxPreference? = null

    private var actionBar: Toolbar? = null

    var preferredDistanceUnits: ListPreference? = null
    var preferredSpeedUnits: ListPreference? = null

    var language: ListPreference? = null

    var chkShowNotification: CheckBoxPreference? = null
    var chkRunInBackground: CheckBoxPreference? = null
    var chkLogFileNmea: CheckBoxPreference? = null
    var chkLogFileNavMessages: CheckBoxPreference? = null
    var chkLogFileMeasurements: CheckBoxPreference? = null
    var chkLogFileLocation: CheckBoxPreference? = null
    var chkLogFileAntennaJson: CheckBoxPreference? = null
    var chkLogFileAntennaCsv: CheckBoxPreference? = null

    var chkAsMeasurements: CheckBoxPreference? = null
    var chkAsNavMessages: CheckBoxPreference? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme
        if (prefs.getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark)
        }
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        actionBar?.title = title
        resetActivityTitle(this)
        forceFullGnssMeasurements =
            findPreference(getString(R.string.pref_key_force_full_gnss_measurements)) as CheckBoxPreference
        if (!SatelliteUtils.isForceFullGnssMeasurementsSupported()) {
            forceFullGnssMeasurements!!.isEnabled = false
        }
        txtMinTime = findPreference(getString(R.string.pref_key_gps_min_time)) as EditTextPreference
        txtMinTime?.editText?.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Verify minTime entry
        txtMinTime?.onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                if (!verifyFloat(newValue)) {
                    // Tell user that entry must be valid decimal
                    Toast.makeText(
                        this@Preferences,
                        getString(R.string.pref_gps_min_time_invalid_entry),
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                } else {
                    true
                }
            }

        txtMinDistance =
            findPreference(getString(R.string.pref_key_gps_min_distance)) as EditTextPreference
        txtMinDistance?.editText?.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Verify minDistance entry
        txtMinDistance?.onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                if (!verifyFloat(newValue)) {
                    // Tell user that entry must be valid decimal
                    Toast.makeText(
                        this@Preferences,
                        getString(R.string.pref_gps_min_distance_invalid_entry),
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                } else {
                    true
                }
            }

        // Check Dark Theme
        chkDarkTheme = findPreference(getString(R.string.pref_key_dark_theme)) as CheckBoxPreference
        chkDarkTheme?.onPreferenceChangeListener =
            OnPreferenceChangeListener { _: Preference?, _: Any? ->
                // Destroy and recreate Activity
                recreate()
                true
            }
        preferredDistanceUnits = findPreference(
            getString(R.string.pref_key_preferred_distance_units_v2)
        ) as ListPreference
        preferredSpeedUnits = findPreference(
            getString(R.string.pref_key_preferred_speed_units_v2)
        ) as ListPreference
        language = findPreference(getString(R.string.pref_key_language)) as ListPreference
        language?.onPreferenceChangeListener =
            OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                localeManager.setNewLocale(app, newValue.toString())
                // Destroy and recreate Activity
                recreate()
                true
            }

        // Remove preference for rotating map if needed
        if (!SatelliteUtils.isRotationVectorSensorSupported(this) || BuildConfig.FLAVOR != "google") {
            // We don't have tilt info or it's the OSM Droid flavor, so remove this preference
            val checkBoxTiltMap = findPreference(
                getString(R.string.pref_key_tilt_map_with_sensors)
            ) as CheckBoxPreference
            val mMapCategory = findPreference(
                getString(R.string.pref_key_map_category)
            ) as PreferenceCategory
            mMapCategory.removePreference(checkBoxTiltMap)
        }

        // Remove preference for setting map type if needed
        if (BuildConfig.FLAVOR != "google") {
            // We don't have tilt info or it's the OSM Droid flavor, so remove this preference
            val checkBoxMapType = findPreference(
                getString(R.string.pref_key_map_type)
            ) as ListPreference
            val mMapCategory = findPreference(
                getString(R.string.pref_key_map_category)
            ) as PreferenceCategory
            mMapCategory.removePreference(checkBoxMapType)
        }

        // Disable preferences for antenna info logging if it's not supported
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        chkLogFileAntennaJson =
            findPreference(getString(R.string.pref_key_file_antenna_output_json)) as CheckBoxPreference
        chkLogFileAntennaCsv =
            findPreference(getString(R.string.pref_key_file_antenna_output_csv)) as CheckBoxPreference
        if(!SatelliteUtils.isGnssAntennaInfoSupported(manager)) {
            chkLogFileAntennaJson!!.isEnabled = false
            chkLogFileAntennaCsv!!.isEnabled = false
        }

        // Disable Android Studio logging if not supported by platform
        chkAsMeasurements = findPreference(getString(R.string.pref_key_as_measurement_output)) as CheckBoxPreference
        chkAsMeasurements?.isEnabled = enableMeasurementsPref(app, prefs)
        chkAsNavMessages = findPreference(getString(R.string.pref_key_as_navigation_message_output)) as CheckBoxPreference
        chkAsNavMessages?.isEnabled = enableNavMessagesPref(app, prefs)

        initNotificationPermissionDialog()

        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        changePreferenceSummary(getString(R.string.pref_key_preferred_distance_units_v2))
        changePreferenceSummary(getString(R.string.pref_key_preferred_speed_units_v2))
        changePreferenceSummary(getString(R.string.pref_key_language))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key.equals(
                getString(R.string.pref_key_preferred_distance_units_v2),
                ignoreCase = true
            )
        ) {
            // Change the preferred distance units description
            if (key != null) {
                changePreferenceSummary(key)
            }
        } else {
            if (key.equals(
                    getString(R.string.pref_key_preferred_speed_units_v2),
                    ignoreCase = true
                )
            ) {
                // Change the preferred speed units description
                if (key != null) {
                    changePreferenceSummary(key)
                }
            } else {
                if (key.equals(getString(R.string.pref_key_language), ignoreCase = true)) {
                    // Change the preferred language description
                    if (key != null) {
                        changePreferenceSummary(key)
                    }
                }
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        // For dynamically changing the locale
        super.attachBaseContext(localeManager.setLocale(base))
    }

    /**
     * Verify that the value is a valid float
     *
     * @param newValue entered value
     * @return true if its a valid float, false if its not
     */
    private fun verifyFloat(newValue: Any): Boolean {
        return try {
            newValue.toString().toFloat()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setContentView(layoutResID: Int) {
        val contentView = LayoutInflater.from(this).inflate(
            R.layout.settings_activity, LinearLayout(this), false
        ) as ViewGroup
        actionBar = contentView.findViewById(R.id.action_bar)
        actionBar!!.setNavigationOnClickListener { finish() }
        val contentWrapper = contentView.findViewById<ViewGroup>(R.id.content_wrapper)
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true)
        window.setContentView(contentView)
    }

    /**
     * Changes the summary of a preference based on a given preference key
     *
     * @param prefKey preference key that triggers a change in summary
     */
    private fun changePreferenceSummary(prefKey: String) {
        if (prefKey.equals(
                getString(R.string.pref_key_preferred_distance_units_v2),
                ignoreCase = true
            )
        ) {
            val values = app.resources.getStringArray(R.array.preferred_distance_units_values)
            val entries = app.resources.getStringArray(R.array.preferred_distance_units_entries)
            for (i in values.indices) {
                if (values[i] == preferredDistanceUnits!!.value) {
                    preferredDistanceUnits!!.summary = entries[i]
                }
            }
        } else if (prefKey.equals(
                getString(R.string.pref_key_preferred_speed_units_v2),
                ignoreCase = true
            )
        ) {
            val values = app.resources.getStringArray(R.array.preferred_speed_units_values)
            val entries = app.resources.getStringArray(R.array.preferred_speed_units_entries)
            for (i in values.indices) {
                if (values[i] == preferredSpeedUnits!!.value) {
                    preferredSpeedUnits!!.summary = entries[i]
                }
            }
        } else if (prefKey.equals(getString(R.string.pref_key_language), ignoreCase = true)) {
            val values = app.resources.getStringArray(R.array.language_values)
            val entries = app.resources.getStringArray(R.array.language_entries)
            for (i in values.indices) {
                if (values[i] == language!!.value) {
                    language!!.summary = entries[i]
                }
            }
        }
    }

    /**
     * Initializes the dialog for notification permissions, which is required for
     * notifications, background execution and logging.
     */
    @SuppressLint("NewApi")
    private fun initNotificationPermissionDialog() {
        chkShowNotification =
            findPreference(getString(R.string.pref_key_show_notification)) as CheckBoxPreference
        if (VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
            // Notifications are always shown on Android 12 and lower
            chkShowNotification?.isEnabled = false
            PreferenceUtils.saveBoolean(
                getString(R.string.pref_key_show_notification),
                true,
                prefs
            )
            return
        }

        // Permissions for notifications are used in place of a user-defined setting. This workflow
        // supports users that have installed an update, who will already have permissions granted.
        // Additionally, revoking notification permissions seems to be the only way to disable
        // user-facing notifications for the foreground service, because they are required.
        PreferenceUtils.saveBoolean(
            getString(R.string.pref_key_show_notification),
            PermissionUtils.hasGrantedNotificationPermissions(this),
            prefs
        )

        chkRunInBackground =
            findPreference(getString(R.string.pref_key_gnss_background)) as CheckBoxPreference
        chkLogFileNmea =
            findPreference(getString(R.string.pref_key_file_nmea_output)) as CheckBoxPreference
        chkLogFileNavMessages =
            findPreference(getString(R.string.pref_key_file_navigation_message_output)) as CheckBoxPreference
        chkLogFileMeasurements =
            findPreference(getString(R.string.pref_key_file_measurement_output)) as CheckBoxPreference
        chkLogFileLocation =
            findPreference(getString(R.string.pref_key_file_location_output)) as CheckBoxPreference
        chkLogFileAntennaJson =
            findPreference(getString(R.string.pref_key_file_antenna_output_json)) as CheckBoxPreference
        chkLogFileAntennaCsv =
            findPreference(getString(R.string.pref_key_file_antenna_output_csv)) as CheckBoxPreference
        val prefsThatNeedNotificationPermissions = listOf(
            chkShowNotification,
            chkRunInBackground,
            chkLogFileNmea,
            chkLogFileNavMessages,
            chkLogFileMeasurements,
            chkLogFileLocation,
            chkLogFileAntennaJson,
            chkLogFileAntennaCsv
        )
        prefsThatNeedNotificationPermissions.forEach {
            it?.onPreferenceChangeListener =
                OnPreferenceChangeListener { preference, newValue ->
                    if (newValue as Boolean && !PermissionUtils.hasGrantedNotificationPermissions(
                            this
                        )
                    ) {
                        // User must have granted notification permissions first
                        createNotificationPermissionDialog(this).show()
                        // Reject change to setting by returning false
                        return@OnPreferenceChangeListener false
                    } else {
                        if (preference == chkShowNotification && !newValue &&
                            (PreferenceUtil.runInBackground(
                                this,
                                prefs
                            ) || PreferenceUtil.isFileLoggingEnabled(this, prefs))
                        ) {
                            // Don't let the user disable notifications if background execution or logging is enabled
                            createCanNotDisableSettingDialog(this).show()
                            // Reject change to setting by returning false
                            return@OnPreferenceChangeListener false
                        }

                        if (preference == chkShowNotification && !newValue) {
                            // If the user disabled the notification setting prompt them to restart app
                            createRestartApplicationDialog(this).show()
                            return@OnPreferenceChangeListener false
                        }

                        // Accept change to setting by returning true
                        return@OnPreferenceChangeListener true
                    }
                }
        }
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun createNotificationPermissionDialog(activity: Activity): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.notification_permissions_dialog, null)
        val textView = view.findViewById<TextView>(R.id.notification_permission_instructions)
        textView.text = getString(R.string.notification_permission_required_dialog_text)
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.notification_permission_required_dialog_title)
            .setCancelable(false)
            .setView(view)
            .setPositiveButton(
                R.string.ok
            ) { _: DialogInterface?, _: Int -> requestNotificationPermission() }
            .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        return builder.create()
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun createCanNotDisableSettingDialog(activity: Activity): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.notification_permissions_dialog, null)
        val textView = view.findViewById<TextView>(R.id.notification_permission_instructions)
        textView.text = getString(R.string.can_not_disable_setting_dialog_text)
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.notification_permission_required_dialog_title)
            .setCancelable(false)
            .setView(view)
            .setPositiveButton(
                R.string.ok
            ) { _: DialogInterface?, _: Int -> requestNotificationPermission() }
        return builder.create()
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun createRestartApplicationDialog(activity: Activity): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.notification_permissions_dialog, null)
        val textView = view.findViewById<TextView>(R.id.notification_permission_instructions)
        textView.text = getString(R.string.need_to_restart_application_dialog_text)
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.need_to_restart_application_dialog_title)
            .setCancelable(false)
            .setView(view)
            .setPositiveButton(
                R.string.ok
            ) { _: DialogInterface?, _: Int -> revokeNotificationPermissionAndRestartApplication() }
            .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        return builder.create()
    }

    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(PermissionUtils.getNotificationPermission()),
            PermissionUtils.NOTIFICATION_PERMISSION_REQUEST
        )
    }

    @SuppressLint("NewApi")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notification permission granted - change the setting in the Preferences UI
                // The notification will automatically be posted by the service
                PreferenceUtils.saveBoolean(
                    getString(R.string.pref_key_show_notification),
                    true,
                    prefs
                )
                recreate()
            } else {
                // Prompt the user to grant permissions again
                createNotificationPermissionDialog(this).show();
            }
        }
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private fun Context.revokeNotificationPermissionAndRestartApplication() {
        revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS)
        PreferenceUtils.saveBoolean(getString(R.string.pref_key_show_notification), false, prefs)

        Executors.newSingleThreadScheduledExecutor().schedule({
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }, 200, TimeUnit.MILLISECONDS)
    }
}