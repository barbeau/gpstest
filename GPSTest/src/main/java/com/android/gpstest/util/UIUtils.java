/*
 * Copyright (C) 2015-2018 University of South  Florida, Sean J. Barbeau
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import static com.android.gpstest.view.GpsSkyView.MAX_VALUE_CN0;
import static com.android.gpstest.view.GpsSkyView.MAX_VALUE_SNR;
import static com.android.gpstest.view.GpsSkyView.MIN_VALUE_CN0;
import static com.android.gpstest.view.GpsSkyView.MIN_VALUE_SNR;

/**
 * Utilities for processing user inteface elements
 */

public class UIUtils {

    /**
     * Formats a view so it is ignored for accessible access
     */
    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    /**
     * Converts screen dimension units from dp to pixels, based on algorithm defined in
     * http://developer.android.com/guide/practices/screens_support.html#dips-pels
     *
     * @param dp value in dp
     * @return value in pixels
     */
    public static int dpToPixels(Context context, float dp) {
        // Get the screen's density scale
        final float scale = context.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (dp * scale + 0.5f);
    }

    /**
     * Returns true if the activity is still active and dialogs can be managed (i.e., displayed
     * or dismissed), or false if it is not
     *
     * @param activity Activity to check for displaying/dismissing a dialog
     * @return true if the activity is still active and dialogs can be managed, or false if it is
     * not
     */
    public static boolean canManageDialog(Activity activity) {
        if (activity == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return !activity.isFinishing() && !activity.isDestroyed();
        } else {
            return !activity.isFinishing();
        }
    }

    /**
     * Returns true if the fragment is attached to the activity, or false if it is not attached
     *
     * @param f fragment to be tested
     * @return true if the fragment is attached to the activity, or false if it is not attached
     */
    public static boolean isFragmentAttached(Fragment f) {
        return f.getActivity() != null && f.isAdded();
    }

    /**
     * Returns a human-readable description of the time-to-first-fix, such as "38 sec"
     *
     * @param ttff time-to-first fix, in milliseconds
     * @return a human-readable description of the time-to-first-fix, such as "38 sec"
     */
    public static String getTtffString(int ttff) {
        if (ttff == 0) {
            return "";
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(ttff) + " sec";
        }
    }

    /**
     * Converts the provided SNR values to a left margin value (pixels) for the avg SNR indicator ImageViews in gps_sky_signal
     * Left margin range for the SNR indicator ImageViews in gps_sky_signal is determined by dimens.xml
     * cn0_meter_width (based on device screen width) and cn0_indicator_min_left_margin values.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param snr signal-to-noise ratio of the satellite in dB (from GpsSatellite)
     * @return left margin value in pixels for the SNR indicator ImageViews
     */
    public static int snrToIndicatorLeftMarginPx(float snr, int minIndicatorMarginPx, int maxIndicatorMarginPx) {
        return (int) MathUtils.mapToRange(snr, MIN_VALUE_SNR, MAX_VALUE_SNR, minIndicatorMarginPx, maxIndicatorMarginPx);
    }

    /**
     * Converts the provided SNR values to a left margin value (pixels) for the avg SNR TextViews in gps_sky_signal
     * Left margin range for the SNR indicator TextView in gps_sky_signal is determined by dimens.xml
     * cn0_meter_width (based on device screen width) and cn0_textview_min_left_margin values.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param snr signal-to-noise ratio of the satellite in dB (from GpsSatellite)
     * @return left margin value in dp for the SNR TextViews
     */
    public static int snrToTextViewLeftMarginPx(float snr, int minTextViewMarginPx, int maxTextViewMarginPx) {
        return (int) MathUtils.mapToRange(snr, MIN_VALUE_SNR, MAX_VALUE_SNR, minTextViewMarginPx, maxTextViewMarginPx);
    }

    /**
     * Converts the provided C/N0 values to a left margin value (dp) for the avg C/N0 indicator ImageViews in gps_sky_signal
     * Left margin range for the C/N0 indicator ImageViews in gps_sky_signal is determined by dimens.xml
     * cn0_meter_width (based on device screen width) and cn0_indicator_min_left_margin values.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param cn0 carrier-to-noise density at the antenna of the satellite in dB-Hz (from GnssStatus)
     * @return left margin value in dp for the C/N0 indicator ImageViews
     */
    public static int cn0ToIndicatorLeftMarginPx(float cn0, int minIndicatorMarginPx, int maxIndicatorMarginPx) {
        return (int) MathUtils.mapToRange(cn0, MIN_VALUE_CN0, MAX_VALUE_CN0, minIndicatorMarginPx, maxIndicatorMarginPx);
    }

    /**
     * Converts the provided C/N0 values to a left margin value (dp) for the avg C/N0 TextViews in gps_sky_signal
     * Left margin range for the C/N0 indicator TextView in gps_sky_signal is determined by dimens.xml
     * cn0_meter_width (based on device screen width) and cn0_textview_min_left_margin values.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param cn0 carrier-to-noise density at the antenna of the satellite in dB-Hz (from GnssStatus)
     * @return left margin value in dp for the C/N0 TextViews
     */
    public static int cn0ToTextViewLeftMarginPx(float cn0, int minTextViewMarginPx, int maxTextViewMarginPx) {
        return (int) MathUtils.mapToRange(cn0, MIN_VALUE_CN0, MAX_VALUE_CN0, minTextViewMarginPx, maxTextViewMarginPx);
    }

    /**
     * Sets the margins for a given view
     *
     * @param v View to set the margin for
     * @param l left margin, in pixels
     * @param t top margin, in pixels
     * @param r right margin, in pixels
     * @param b bottom margin, in pixels
     */
    public static void setMargins(View v, int l, int t, int r, int b) {
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        p.setMargins(l, t, r, b);
        v.setLayoutParams(p);
    }

    /**
     * Opens email apps based on the given email address
     * @param email address
     * @param location string that shows the current location
     */
    public static void sendEmail(Context context, String email, String location) {
        PackageManager pm = context.getPackageManager();
        PackageInfo appInfo;

        StringBuilder body = new StringBuilder();
        body.append(context.getString(R.string.feedback_body));

        String versionName = "";
        int versionCode = 0;

        try {
            appInfo = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            versionName = appInfo.versionName;
            versionCode = appInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Leave version as empty string
        }

        // App version
        body.append("App version: v")
                .append(versionName)
                .append(" (")
                .append(versionCode)
                .append("-" + BuildConfig.FLAVOR + ")\n");

        // Device properties
        body.append("Model: " + Build.MODEL + "\n");
        body.append("Android version: " + Build.VERSION.RELEASE + " / " + Build.VERSION.SDK_INT + "\n");

        if (!TextUtils.isEmpty(location)) {
            body.append("Location: " + location + "\n");
        }
        
        body.append(GpsTestUtil.getGnssHardwareYear());

        // Raw GNSS measurement capability
        int capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_raw_measurements), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_raw_measurements, PreferenceUtils.getCapabilityDescription(capability)));
        }

        // Navigation messages capability
        capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_nav_messages), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_nav_messages, PreferenceUtils.getCapabilityDescription(capability)));
        }

        // NMEA capability
        capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_nmea), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_nmea, PreferenceUtils.getCapabilityDescription(capability)));
        }

        // Inject XTRA capability
        capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_inject_xtra), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_inject_xtra, PreferenceUtils.getCapabilityDescription(capability)));
        }

        // Inject time capability
        capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_inject_time), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_inject_time, PreferenceUtils.getCapabilityDescription(capability)));
        }

        // Delete assist capability
        capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_delete_assist), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_delete_assist, PreferenceUtils.getCapabilityDescription(capability)));
        }


        if (!TextUtils.isEmpty(BuildUtils.getPlayServicesVersion())) {
            body.append("\n" + BuildUtils.getPlayServicesVersion());
        }

        body.append("\n\n\n");

        Intent send = new Intent(Intent.ACTION_SEND);
        send.putExtra(Intent.EXTRA_EMAIL, new String[]{email});

        String subject = context.getString(R.string.feedback_subject);

        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.putExtra(Intent.EXTRA_TEXT, body.toString());
        send.setType("message/rfc822");
        try {
            context.startActivity(Intent.createChooser(send, subject));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.feedback_error, Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Returns the provided latitude or longitude value in Degrees Minutes Seconds (DMS) format
     * @param latOrLon latitude or longitude to convert to DMS format
     * @return the provided latitude or longitude value in Degrees Minutes Seconds (DMS) format
     */
    public static String getDMSFromLocation(Context context, double latOrLon) {
        BigDecimal loc = new BigDecimal(latOrLon);
        BigDecimal degrees = loc.setScale(0, RoundingMode.DOWN);
        BigDecimal minTemp = loc.subtract(degrees).multiply((new BigDecimal(60))).abs();
        BigDecimal minutes = minTemp.setScale(0, RoundingMode.DOWN);
        BigDecimal seconds = minTemp.subtract(minutes).multiply(new BigDecimal(60)).setScale(0, RoundingMode.DOWN);

        return context.getString(R.string.gps_lat_lon_dms_value, degrees.intValue(), minutes.intValue(), seconds.intValue());
    }
}
