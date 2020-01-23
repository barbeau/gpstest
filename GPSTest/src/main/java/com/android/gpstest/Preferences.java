/*
 * Copyright (C) 2013 Sean J. Barbeau
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

package com.android.gpstest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.android.gpstest.util.PermissionUtils;
import com.android.gpstest.util.SatelliteUtils;
import com.android.gpstest.util.UIUtils;

public class Preferences extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    EditTextPreference txtMinTime;

    EditTextPreference txtMinDistance;

    CheckBoxPreference chkDarkTheme;

    private Toolbar mActionBar;

    ListPreference preferredDistanceUnits;

    ListPreference preferredSpeedUnits;

    ListPreference language;

    CheckBoxPreference chkLogFileNmea;
    CheckBoxPreference chkLogFileNavMessages;
    CheckBoxPreference chkLogFileMeasurements;
    CheckBoxPreference chkLogFileLocation;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Set theme
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mActionBar.setTitle(getTitle());

        UIUtils.resetActivityTitle(this);

        txtMinTime = (EditTextPreference) this
                .findPreference(getString(R.string.pref_key_gps_min_time));
        txtMinTime.getEditText()
                .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Verify minTime entry
        txtMinTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!verifyFloat(newValue)) {
                    // Tell user that entry must be valid decimal
                    Toast.makeText(
                            Preferences.this,
                            getString(R.string.pref_gps_min_time_invalid_entry),
                            Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    return true;
                }
            }
        });

        txtMinDistance = (EditTextPreference) this
                .findPreference(getString(R.string.pref_key_gps_min_distance));
        txtMinDistance.getEditText()
                .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Verify minDistance entry
        txtMinDistance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!verifyFloat(newValue)) {
                    // Tell user that entry must be valid decimal
                    Toast.makeText(
                            Preferences.this,
                            getString(R.string.pref_gps_min_distance_invalid_entry),
                            Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    return true;
                }
            }
        });

        // Check Dark Theme
        chkDarkTheme = (CheckBoxPreference) this
                .findPreference(getString(R.string.pref_key_dark_theme));

        chkDarkTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Destroy and recreate Activity
                recreate();
                return true;
            }
        });

        preferredDistanceUnits = (ListPreference) findPreference(
                getString(R.string.pref_key_preferred_distance_units_v2));

        preferredSpeedUnits = (ListPreference) findPreference(
                getString(R.string.pref_key_preferred_speed_units_v2));

        language = (ListPreference) findPreference(getString(R.string.pref_key_language));
        language.setOnPreferenceChangeListener((preference, newValue) -> {
            Application.getLocaleManager().setNewLocale(Application.get(), newValue.toString());
            // Destroy and recreate Activity
            recreate();
            return true;
        });

        // Remove preference for rotating map if needed
        if (!SatelliteUtils.isRotationVectorSensorSupported(this) || !BuildConfig.FLAVOR.equals("google")) {
            // We don't have tilt info or it's the OSM Droid flavor, so remove this preference
            CheckBoxPreference checkBoxTiltMap = (CheckBoxPreference) findPreference(
                    getString(R.string.pref_key_tilt_map_with_sensors));
            PreferenceCategory mMapCategory = (PreferenceCategory) findPreference(
                    getString(R.string.pref_key_map_category));
            mMapCategory.removePreference(checkBoxTiltMap);
        }

        // Remove preference for setting map type if needed
        if (!BuildConfig.FLAVOR.equals("google")) {
            // We don't have tilt info or it's the OSM Droid flavor, so remove this preference
            ListPreference checkBoxMapType = (ListPreference) findPreference(
                    getString(R.string.pref_key_map_type));
            PreferenceCategory mMapCategory = (PreferenceCategory) findPreference(
                    getString(R.string.pref_key_map_category));
            mMapCategory.removePreference(checkBoxMapType);
        }

        // If the user chooses to enable any of the file writing preferences, request permission
        chkLogFileNmea = (CheckBoxPreference) findPreference(getString(R.string.pref_key_file_nmea_output));
        chkLogFileNmea.setOnPreferenceChangeListener((preference, newValue) -> {
            PermissionUtils.requestFileWritePermission(Preferences.this);
            return true;
        });
        chkLogFileNavMessages = (CheckBoxPreference) findPreference(getString(R.string.pref_key_file_navigation_message_output));
        chkLogFileNavMessages.setOnPreferenceChangeListener((preference, newValue) -> {
            PermissionUtils.requestFileWritePermission(Preferences.this);
            return true;
        });
        chkLogFileMeasurements = (CheckBoxPreference) findPreference(getString(R.string.pref_key_file_measurement_output));
        chkLogFileMeasurements.setOnPreferenceChangeListener((preference, newValue) -> {
            PermissionUtils.requestFileWritePermission(Preferences.this);
            return true;
        });
        chkLogFileLocation = (CheckBoxPreference) findPreference(getString(R.string.pref_key_file_location_output));
        chkLogFileLocation.setOnPreferenceChangeListener((preference, newValue) -> {
            PermissionUtils.requestFileWritePermission(Preferences.this);
            return true;
        });

        Application.getPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        changePreferenceSummary(getString(R.string.pref_key_preferred_distance_units_v2));
        changePreferenceSummary(getString(R.string.pref_key_preferred_speed_units_v2));
        changePreferenceSummary(getString(R.string.pref_key_language));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(getString(R.string.pref_key_preferred_distance_units_v2))) {
            // Change the preferred distance units description
            changePreferenceSummary(key);
        } else {
            if (key.equalsIgnoreCase(getString(R.string.pref_key_preferred_speed_units_v2))) {
                // Change the preferred speed units description
                changePreferenceSummary(key);
            } else {
                if (key.equalsIgnoreCase(getString(R.string.pref_key_language))) {
                    // Change the preferred language description
                    changePreferenceSummary(key);
                }
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        // For dynamically changing the locale
        super.attachBaseContext(Application.getLocaleManager().setLocale(base));
    }

    /**
     * Verify that the value is a valid float
     *
     * @param newValue entered value
     * @return true if its a valid float, false if its not
     */
    private boolean verifyFloat(Object newValue) {
        try {
            Float.parseFloat(newValue.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.settings_activity, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

    /**
     * Changes the summary of a preference based on a given preference key
     *
     * @param prefKey preference key that triggers a change in summary
     */
    private void changePreferenceSummary(String prefKey) {
        if (prefKey.equalsIgnoreCase(getString(R.string.pref_key_preferred_distance_units_v2))) {
            String[] values = Application.get().getResources().getStringArray(R.array.preferred_distance_units_values);
            String[] entries = Application.get().getResources().getStringArray(R.array.preferred_distance_units_entries);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(preferredDistanceUnits.getValue())) {
                    preferredDistanceUnits.setSummary(entries[i]);
                }
            }
        } else if (prefKey.equalsIgnoreCase(getString(R.string.pref_key_preferred_speed_units_v2))) {
            String[] values = Application.get().getResources().getStringArray(R.array.preferred_speed_units_values);
            String[] entries = Application.get().getResources().getStringArray(R.array.preferred_speed_units_entries);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(preferredSpeedUnits.getValue())) {
                    preferredSpeedUnits.setSummary(entries[i]);
                }
            }
        } else if (prefKey.equalsIgnoreCase(getString(R.string.pref_key_language))) {
            String[] values = Application.get().getResources().getStringArray(R.array.language_values);
            String[] entries = Application.get().getResources().getStringArray(R.array.language_entries);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(language.getValue())) {
                    language.setSummary(entries[i]);
                }
            }
        }
    }
}
