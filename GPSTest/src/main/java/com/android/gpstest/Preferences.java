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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.gpstest.util.GpsTestUtil;

public class Preferences extends PreferenceActivity {

    Preference prefAnalyzeGpsAccuracy;

    EditTextPreference txtMinTime;

    EditTextPreference txtMinDistance;

    CheckBoxPreference chkDarkTheme;

    private Toolbar mActionBar;

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

        prefAnalyzeGpsAccuracy = this
                .findPreference(getString(R.string.pref_key_analyze_gps_accuracy));

        prefAnalyzeGpsAccuracy.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                // Check to see if GPS Benchmark is already installed
                Intent intent = getPackageManager()
                        .getLaunchIntentForPackage(getString(R.string.gps_benchmark_package_name));
                if (intent != null) {
                    // Start GPS Benchmark
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    // Go to Google Play
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // Use http:// scheme here instead of market:// so it doesn't crash on devices without Google Play
                    intent.setData(Uri.parse(getString(R.string.gps_benchmark_url)));
                    startActivity(intent);
                }
                return false;
            }
        });

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

        // Remove preference for rotating map if needed
        if (!GpsTestUtil.isRotationVectorSensorSupported(this) || !BuildConfig.FLAVOR.equals("google")) {
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
}
