/*
 * Copyright (C) 2018 Sean J. Barbeau
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
import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.TimeUnit;

/**
 * Utilities for processing user inteface elements
 */

public class UIUtils {

    // Signal strength ranges
    private static final float MIN_VALUE_CN0 = 10.0f;
    private static final float MAX_VALUE_CN0 = 45.0f;
    private static final float MIN_VALUE_SNR = 0.0f;
    private static final float MAX_VALUE_SNR = 30.0f;

    // Margin ranges for signal strength indicators
    private static final float MIN_VALUE_INDICATOR_MARGIN_DP = -6.0f;
    private static final float MAX_VALUE_INDICATOR_MARGIN_DP = 140.0f;
    private static final float MIN_VALUE_TEXT_VIEW_MARGIN_DP = 3.0f;
    private static final float MAX_VALUE_TEXT_VIEW_MARGIN_DP = 149.0f;

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
     * Converts the provided SNR values to a left margin value (dp) for the avg SNR indicator ImageViews in gps_sky_signal
     * Left margin range for the SNR indicator ImageViews in gps_sky_signal is from -6dp (0 dB) to 140dp (30 dB).
     * So, based on the avg SNR for "in view" and "used" satellites the left margins need to be adjusted accordingly.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param snr signal-to-noise ratio of the satellite in dB (from GpsSatellite)
     * @return left margin value in dp for the SNR indicator ImageViews
     */
    public static float snrToIndicatorLeftMarginDp(float snr) {
        // Shift margin and SNR ranges to calculate percentages (because default min value isn't 0)
        return MathUtils.mapToRange(snr, MIN_VALUE_SNR, MAX_VALUE_SNR, MIN_VALUE_INDICATOR_MARGIN_DP, MAX_VALUE_INDICATOR_MARGIN_DP);
    }

    /**
     * Converts the provided SNR values to a left margin value (dp) for the avg SNR TextViews in gps_sky_signal
     * Left margin range for the SNR TextView in gps_sky_signal is from 3dp (0 dB) to 149dp (30 dB).
     * So, based on the avg SNR for "in view" and "used" satellites the left margins need to be adjusted accordingly.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param snr signal-to-noise ratio of the satellite in dB (from GpsSatellite)
     * @return left margin value in dp for the SNR TextViews
     */
    public static float snrToTextViewLeftMarginDp(float snr) {
        // Shift margin and CN0 ranges to calculate percentages (because default min value isn't 0)
        return MathUtils.mapToRange(snr, MIN_VALUE_SNR, MAX_VALUE_SNR, MIN_VALUE_TEXT_VIEW_MARGIN_DP, MAX_VALUE_TEXT_VIEW_MARGIN_DP);
    }

    /**
     * Converts the provided C/N0 values to a left margin value (dp) for the avg C/N0 indicator ImageViews in gps_sky_signal
     * Left margin range for the C/N0 indicator ImageViews in gps_sky_signal is from -6dp (10 dB-Hz) to 140dp (45 dB-Hz).
     * So, based on the avg C/N0 for "in view" and "used" satellites the left margins need to be adjusted accordingly.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param cn0 carrier-to-noise density at the antenna of the satellite in dB-Hz (from GnssStatus)
     * @return left margin value in dp for the C/N0 indicator ImageViews
     */
    public static float cn0ToIndicatorLeftMarginDp(float cn0) {
        // Shift margin and CN0 ranges to calculate percentages (because default min value isn't 0)
        return MathUtils.mapToRange(cn0, MIN_VALUE_CN0, MAX_VALUE_CN0, MIN_VALUE_INDICATOR_MARGIN_DP, MAX_VALUE_INDICATOR_MARGIN_DP);
    }

    /**
     * Converts the provided C/N0 values to a left margin value (dp) for the avg C/N0 TextViews in gps_sky_signal
     * Left margin range for the C/N0 TextView in gps_sky_signal is from 3dp (10 dB-Hz) to 149dp (45 dB-Hz).
     * So, based on the avg C/N0 for "in view" and "used" satellites the left margins need to be adjusted accordingly.
     *
     * This is effectively an affine transform - https://math.stackexchange.com/a/377174/554287.
     *
     * @param cn0 carrier-to-noise density at the antenna of the satellite in dB-Hz (from GnssStatus)
     * @return left margin value in dp for the C/N0 TextViews
     */
    public static float cn0ToTextViewLeftMarginDp(float cn0) {
        // Shift margin and CN0 ranges to calculate percentages (because default min value isn't 0)
        return MathUtils.mapToRange(cn0, MIN_VALUE_CN0, MAX_VALUE_CN0, MIN_VALUE_TEXT_VIEW_MARGIN_DP, MAX_VALUE_TEXT_VIEW_MARGIN_DP);
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
}
