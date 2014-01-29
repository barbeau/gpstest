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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.text.InputType;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.android.gpstest.util.GpsTestUtil;

public class Preferences extends SherlockPreferenceActivity {
	
	Preference prefShowTutorial;
	EditTextPreference txtMinTime;
	EditTextPreference txtMinDistance;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		prefShowTutorial = this.findPreference(getString(R.string.pref_key_showed_v2_tutorial));
		
		prefShowTutorial.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				// If the user taps on the tutorial preference, reset the flag to false
				// and finish to go back to the GpsTestActivity
				SharedPreferences.Editor editor = Application.getPrefs().edit();
	    	    editor.putBoolean(getString(R.string.pref_key_showed_v2_tutorial), false);
	    	    editor.commit();
	    	    finish();
				return false;
			}						
		});
		
		txtMinTime = (EditTextPreference) this.findPreference(getString(R.string.pref_key_gps_min_time));
		txtMinTime.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
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
		
		txtMinDistance = (EditTextPreference) this.findPreference(getString(R.string.pref_key_gps_min_distance));
		txtMinDistance.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
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

        // Remove preferences that shouldn't be shown based on this device
        if (!GpsTestUtil.isRotationVectorSensorSupported()) {
            // We don't have tilt info, so remove this preference
            CheckBoxPreference mCheckBoxTiltMap = (CheckBoxPreference) findPreference(getString(R.string.pref_key_tilt_map_with_sensors));
            PreferenceCategory mMapCategory = (PreferenceCategory) findPreference(getString(R.string.pref_key_map_category));
            mMapCategory.removePreference(mCheckBoxTiltMap);
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
}
