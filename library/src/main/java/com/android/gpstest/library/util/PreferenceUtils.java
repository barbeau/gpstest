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
package com.android.gpstest.library.util;

import static java.util.Collections.emptySet;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.android.gpstest.library.R;
import com.android.gpstest.library.model.GnssType;

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

    public static final String KEY_SERVICE_TRACKING_ENABLED = "tracking_foreground_location";

    /**
     * Gets the string description of a CAPABILITY_* constant
     * @param capability CAPABILITY_* constant defined in this class
     * @return a string description of the CAPABILITY_* constant
     */
    public static String getCapabilityDescription(Context context, int capability) {
        switch (capability) {
            case CAPABILITY_UNKNOWN:
                return context.getString(R.string.capability_value_unknown);
            case CAPABILITY_NOT_SUPPORTED:
                return context.getString(R.string.capability_value_not_supported);
            case CAPABILITY_SUPPORTED:
                return context.getString(R.string.capability_value_supported);
            case CAPABILITY_LOCATION_DISABLED:
                return context.getString(R.string.capability_value_location_disabled);
            default:
                return context.getString(R.string.capability_value_unknown);
        }
    }

    /**
     * Returns a simple SUPPORTED or UNSUPPORTED value for the given boolean value
     * @param supported
     * @return a simple SUPPORTED or UNSUPPORTED value for the given boolean value
     */
    public static String getCapabilityDescription(Context context, boolean supported) {
        if (supported) {
            return context.getString(R.string.capability_value_supported);
        } else {
            return context.getString(R.string.capability_value_not_supported);
        }
    }

    @TargetApi(9)
    public static void saveString(String key, String value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveInt(String key, int value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(key, value);
        edit.apply();
    }

    public static int getInt(String key, int defaultValue, SharedPreferences prefs) {
        return prefs.getInt(key, defaultValue);
    }

    @TargetApi(9)
    public static void saveLong(String key, long value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveBoolean(String key, boolean value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveFloat(String key, float value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveDouble(String key, double value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, Double.doubleToRawLongBits(value));
        edit.apply();
    }

    /**
     * Gets a double for the provided key from preferences, or the default value if the preference
     * doesn't currently have a value
     *
     * @param key          key for the preference
     * @param defaultValue the default value to return if the key doesn't have a value
     * @return a double from preferences, or the default value if it doesn't exist
     */
    public static Double getDouble(String key, double defaultValue, SharedPreferences prefs) {
        if (!prefs.contains(key)) {
            return defaultValue;
        }
        return Double.longBitsToDouble(prefs.getLong(key, 0));
    }

    public static String getString(String key, SharedPreferences prefs) {
        return prefs.getString(key, null);
    }

    public static long getLong(SharedPreferences prefs, String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }

    public static float getFloat(SharedPreferences prefs, String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }

    /**
     * Returns the currently selected satellite sort order as the index in R.array.sort_sats
     *
     * @return the currently selected satellite sort order as the index in R.array.sort_sats
     */
    public static int getSatSortOrderFromPreferences(Context context, SharedPreferences prefs) {
        Resources r = context.getResources();
        SharedPreferences settings = prefs;
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
    public static Set<GnssType> gnssFilter(Context context, SharedPreferences prefs) {
        Set<GnssType> filter = new LinkedHashSet<>();
        Resources r = context.getResources();
        String filterString = getString(r.getString(R.string.pref_key_default_sat_filter), prefs);
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
    public static void saveGnssFilter(Context context, Set<GnssType> filter, SharedPreferences prefs) {
        Resources r = context.getResources();
        StringBuilder filterString = new StringBuilder();
        for (GnssType gnssType : filter) {
            filterString.append(gnssType.toString() + ",");
        }
        // Remove the last comma (if there was at least one entry)
        if (filter.size() >= 1) {
            filterString.deleteCharAt(filterString.length() - 1);
        }
        saveString(r.getString(R.string.pref_key_default_sat_filter), filterString.toString(), prefs);
    }

    /**
     * Clears any active GNSS filter so all satellites are displayed
     */
    public static void clearGnssFilter(Context context, SharedPreferences prefs) {
        saveGnssFilter(context, emptySet(), prefs);
    }

    /**
     * Removes the specified preference by deleting it
     * @param key
     */
    public static void remove(String key, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(key).apply();
    }

    /**
     * Returns true if service location tracking is active, and false if it is not
     * @return true if service location tracking is active, and false if it is not
     */
    public static boolean isTrackingStarted(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_SERVICE_TRACKING_ENABLED, false);
    }

    /**
     * Saves the provided value as the current service location tracking state
     * @param value true if service location tracking is active, and false if it is not
     */
    public static void saveTrackingStarted(boolean value, SharedPreferences prefs) {
        saveBoolean(KEY_SERVICE_TRACKING_ENABLED, value, prefs);
    }
}
