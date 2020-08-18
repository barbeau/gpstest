/*
 * Copyright (C) 2012-2018 Paul Watts (paulcwatts@gmail.com), Sean J. Barbeau (sjbarbeau@gmail.com)
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
package com.android.gpstest.util;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.android.gpstest.Application;
import com.android.gpstest.R;
import com.android.gpstest.model.GnssType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A class containing utility methods related to preferences
 */
public class PreferenceUtils {

    public static final int CAPABILITY_UNKNOWN = -1;
    public static final int CAPABILITY_NOT_SUPPORTED = 0;
    public static final int CAPABILITY_SUPPORTED = 1;
    public static final int CAPABILITY_LOCATION_DISABLED = 2;

    /**
     * Gets the string description of a CAPABILITY_* constant
     * @param capability CAPABILITY_* constant defined in this class
     * @return a string description of the CAPABILITY_* constant
     */
    public static String getCapabilityDescription(int capability) {
        switch (capability) {
            case CAPABILITY_UNKNOWN:
                return Application.get().getString(R.string.capability_value_unknown);
            case CAPABILITY_NOT_SUPPORTED:
                return Application.get().getString(R.string.capability_value_not_supported);
            case CAPABILITY_SUPPORTED:
                return Application.get().getString(R.string.capability_value_supported);
            case CAPABILITY_LOCATION_DISABLED:
                return Application.get().getString(R.string.capability_value_location_disabled);
            default:
                return Application.get().getString(R.string.capability_value_unknown);
        }
    }

    @TargetApi(9)
    public static void saveString(SharedPreferences prefs, String key, String value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, value);
        edit.apply();
    }

    public static void saveString(String key, String value) {
        saveString(Application.getPrefs(), key, value);
    }

    @TargetApi(9)
    public static void saveInt(SharedPreferences prefs, String key, int value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(key, value);
        edit.apply();
    }

    public static void saveInt(String key, int value) {
        saveInt(Application.getPrefs(), key, value);
    }

    @TargetApi(9)
    public static void saveLong(SharedPreferences prefs, String key, long value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, value);
        edit.apply();
    }

    public static void saveLong(String key, long value) {
        saveLong(Application.getPrefs(), key, value);
    }

    @TargetApi(9)
    public static void saveBoolean(SharedPreferences prefs, String key, boolean value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(key, value);
        edit.apply();
    }

    public static void saveBoolean(String key, boolean value) {
        saveBoolean(Application.getPrefs(), key, value);
    }

    @TargetApi(9)
    public static void saveFloat(SharedPreferences prefs, String key, float value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(key, value);
        edit.apply();
    }

    public static void saveFloat(String key, float value) {
        saveFloat(Application.getPrefs(), key, value);
    }

    @TargetApi(9)
    public static void saveDouble(SharedPreferences prefs, String key, double value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, Double.doubleToRawLongBits(value));
        edit.apply();
    }

    @TargetApi(9)
    public static void saveDouble(String key, double value) {
        saveDouble(Application.getPrefs(), key, value);
    }

    /**
     * Gets a double for the provided key from preferences, or the default value if the preference
     * doesn't currently have a value
     *
     * @param key          key for the preference
     * @param defaultValue the default value to return if the key doesn't have a value
     * @return a double from preferences, or the default value if it doesn't exist
     */
    public static Double getDouble(String key, double defaultValue) {
        if (!Application.getPrefs().contains(key)) {
            return defaultValue;
        }
        return Double.longBitsToDouble(Application.getPrefs().getLong(key, 0));
    }

    public static String getString(String key) {
        return Application.getPrefs().getString(key, null);
    }

    public static long getLong(String key, long defaultValue) {
        return Application.getPrefs().getLong(key, defaultValue);
    }

    public static float getFloat(String key, float defaultValue) {
        return Application.getPrefs().getFloat(key, defaultValue);
    }

    /**
     * Returns the currently selected satellite sort order as the index in R.array.sort_sats
     *
     * @return the currently selected satellite sort order as the index in R.array.sort_sats
     */
    public static int getSatSortOrderFromPreferences() {
        Resources r = Application.get().getResources();
        SharedPreferences settings = Application.getPrefs();
        String[] sortOptions = r.getStringArray(R.array.sort_sats);
        String sortPref = settings.getString(r.getString(
                R.string.pref_key_default_sat_sort), sortOptions[0]);
        for (int i = 0; i < sortOptions.length; i++) {
            if (sortPref.equalsIgnoreCase(sortOptions[i])) {
                return i;
            }
        }
        return 0;  // Default to the first option
    }

    /**
     * Gets a set of GnssTypes that should have their satellites displayed that has been saved to preferences. (All are shown if empty or null)
     * @return a set of GnssTypes that should have their satellites displayed that has been saved to preferences. (All are shown if empty or null)
     */
    public static Set<GnssType> getGnssFilter() {
        Set<GnssType> filter = new LinkedHashSet<>();
        Resources r = Application.get().getResources();
        String filterString = getString(r.getString(R.string.pref_key_default_sat_filter));
        if (filterString == null) {
            return filter;
        }
        String[] parsedFilter = filterString.split(",");
        for (String s : parsedFilter) {
            GnssType gnssType = GnssType.fromString(s);
            if (gnssType != null) {
                filter.add(gnssType);
            }
        }
        return filter;
    }

    /**
     * Saves a set of GnssTypes that should have their satellites displayed to preferences. (All are shown if empty or null)
     * Values are persisted as string of comma-separated values, with each of the enum values .toString() called
     * @param filter a set of GnssTypes that should have their satellites displayed. (All are shown if empty or null)
     */
    public static void saveGnssFilter(Set<GnssType> filter) {
        Resources r = Application.get().getResources();
        StringBuilder filterString = new StringBuilder();
        for (GnssType gnssType : filter) {
            filterString.append(gnssType.toString() + ",");
        }
        // Remove the last comma (if there was at least one entry)
        if (filter.size() >= 1) {
            filterString.deleteCharAt(filterString.length() - 1);
        }
        saveString(r.getString(R.string.pref_key_default_sat_filter), filterString.toString());
    }

    /**
     * Removes the specified preference by deleting it
     * @param key
     */
    public static void remove(String key) {
        SharedPreferences.Editor edit = Application.getPrefs().edit();
        edit.remove(key).apply();
    }
}
