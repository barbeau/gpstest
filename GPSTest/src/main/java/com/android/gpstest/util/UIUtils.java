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
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;
import com.android.gpstest.io.FileLogger;
import com.android.gpstest.model.GnssType;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.createChooser;
import static android.content.pm.PackageManager.GET_META_DATA;
import static android.text.TextUtils.isEmpty;
import static com.android.gpstest.view.GpsSkyView.MAX_VALUE_CN0;
import static com.android.gpstest.view.GpsSkyView.MAX_VALUE_SNR;
import static com.android.gpstest.view.GpsSkyView.MIN_VALUE_CN0;
import static com.android.gpstest.view.GpsSkyView.MIN_VALUE_SNR;

/**
 * Utilities for processing user inteface elements
 */

public class UIUtils {

    public static final String COORDINATE_LATITUDE = "lat";
    public static final String COORDINATE_LONGITUDE = "lon";

    public static int PICKFILE_REQUEST_CODE = 101;

    /**
     * Formats a view so it is ignored for accessible access
     */
    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
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
     * Returns true if the current display is wide enough to show the GPS date on the Status screen,
     * false if the current display is too narrow to fit the GPS date
     * @param context
     * @return true if the current display is wide enough to show the GPS date on the Status screen,
     *      false if the current display is too narrow to fit the GPS date
     */
    public static boolean isWideEnoughForDate(Context context) {
        // 450dp is a little larger than the width of a Samsung Galaxy S8+
        final int WIDTH_THRESHOLD = dpToPixels(context, 450);
        return context.getResources().getDisplayMetrics().widthPixels > WIDTH_THRESHOLD;
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
        return !activity.isFinishing() && !activity.isDestroyed();
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

        body.append(IOUtils.getGnssHardwareYear() + "\n");

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

        // Inject PSDS capability
        capability = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_inject_psds), PreferenceUtils.CAPABILITY_UNKNOWN);
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(Application.get().getString(R.string.capability_title_inject_psds, PreferenceUtils.getCapabilityDescription(capability)));
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

        Intent send = new Intent(Intent.ACTION_SENDTO);
        send.setData(Uri.parse("mailto:"));
        send.putExtra(Intent.EXTRA_EMAIL, new String[]{email});

        String subject = context.getString(R.string.feedback_subject);

        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.putExtra(Intent.EXTRA_TEXT, body.toString());

        try {
            context.startActivity(createChooser(send, subject));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.feedback_error, Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Returns the provided latitude or longitude value in Degrees Minutes Seconds (DMS) format
     * @param coordinate latitude or longitude to convert to DMS format
     * @return the provided latitude or longitude value in Degrees Minutes Seconds (DMS) format
     */
    public static String getDMSFromLocation(Context context, double coordinate, String latOrLon) {
        BigDecimal loc = new BigDecimal(coordinate);
        BigDecimal degrees = loc.setScale(0, RoundingMode.DOWN);
        BigDecimal minTemp = loc.subtract(degrees).multiply((new BigDecimal(60))).abs();
        BigDecimal minutes = minTemp.setScale(0, RoundingMode.DOWN);
        BigDecimal seconds = minTemp.subtract(minutes).multiply(new BigDecimal(60)).setScale(2, RoundingMode.HALF_UP);

        String hemisphere;
        int output_string;
        if (latOrLon.equals(UIUtils.COORDINATE_LATITUDE)) {
            hemisphere = (coordinate < 0 ? "S" : "N");
            output_string = R.string.gps_lat_dms_value;
        } else {
            hemisphere = (coordinate < 0 ? "W" : "E");
            output_string = R.string.gps_lon_dms_value;
        }

        return context.getString(output_string, hemisphere, degrees.abs().intValue(), minutes.intValue(), seconds.floatValue());
    }

    /**
     * Returns the provided latitude or longitude value in Decimal Degree Minutes (DDM) format
     *
     * @param coordinate latitude or longitude to convert to DDM format
     * @param latOrLon   lat or lon to format hemisphere
     * @return the provided latitude or longitude value in Decimal Degree Minutes (DDM) format
     */
    public static String getDDMFromLocation(Context context, double coordinate, String latOrLon) {
        BigDecimal loc = new BigDecimal(coordinate);
        BigDecimal degrees = loc.setScale(0, RoundingMode.DOWN);
        BigDecimal minutes = loc.subtract(degrees).multiply((new BigDecimal(60))).abs().setScale(3, RoundingMode.HALF_UP);
        String hemisphere;
        int output_string;
        if (latOrLon.equals(COORDINATE_LATITUDE)) {
            hemisphere = (coordinate < 0 ? "S" : "N");
            output_string = R.string.gps_lat_ddm_value;
        } else {
            hemisphere = (coordinate < 0 ? "W" : "E");
            output_string = R.string.gps_lon_ddm_value;
        }
        return context.getString(output_string, hemisphere, degrees.abs().intValue(), minutes.floatValue());
    }

    /**
     * Converts the provide value in meters to the corresponding value in feet
     * @param meters value in meters to convert to feet
     * @return the provided meters value converted to feet
     */
    public static double toFeet(double meters) {
        return meters * 1000d / 25.4d / 12d;
    }

    /**
     * Converts the provide value in meters per second to the corresponding value in kilometers per hour
     * @param metersPerSecond value in meters per second to convert to kilometers per hour
     * @return the provided meters per second value converted to kilometers per hour
     */
    public static float toKilometersPerHour(float metersPerSecond) {
        return metersPerSecond * 3600f / 1000f ;
    }

    /**
     * Converts the provide value in meters per second to the corresponding value in miles per hour
     * @param metersPerSecond value in meters per second to convert to miles per hour
     * @return the provided meters per second value converted to miles per hour
     */
    public static float toMilesPerHour(float metersPerSecond) {
        return toKilometersPerHour(metersPerSecond) / 1.6093440f;
    }

    /**
     * Sets the vertical bias for a provided view that is within a ConstraintLayout
     * @param view view within a ConstraintLayout
     * @param bias vertical bias to be used
     */
    public static void setVerticalBias(View view, float bias) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        params.verticalBias = bias;
        view.setLayoutParams(params);
    }

    /**
     * Tests to see if the provided text latitude, longitude, and altitude values are valid, and if
     * not shows an error dialog and returns false, or if yes then returns true
     * @param activity
     * @param lat latitude to validate
     * @param lon longitude to validate
     * @param alt altitude to validate
     * @return true if the latitude, longitude, and latitude are valid, false if any of them are not
     */
    public static boolean isValidLocationWithErrorDialog(AppCompatActivity activity, String lat, String lon, String alt) {
        String dialogTitle = Application.get().getString(R.string.ground_truth_invalid_location_title);
        String dialogMessage;

        if (!LocationUtils.isValidLatitude(lat)) {
            dialogMessage = Application.get().getString(R.string.ground_truth_invalid_lat);
            UIUtils.showLocationErrorDialog(activity, dialogTitle, dialogMessage);
            return false;
        }
        if (!LocationUtils.isValidLongitude(lon)) {
            dialogMessage = Application.get().getString(R.string.ground_truth_invalid_long);
            UIUtils.showLocationErrorDialog(activity, dialogTitle, dialogMessage);
            return false;
        }
        if (!isEmpty(alt) && !LocationUtils.isValidAltitude(alt)) {
            dialogMessage = Application.get().getString(R.string.ground_truth_invalid_alt);
            UIUtils.showLocationErrorDialog(activity, dialogTitle, dialogMessage);
            return false;
        }
        return true;
    }

    /**
     * Shows an error dialog for an incorrectly entered latitude, longitude, or altitude
     * @param activity
     * @param title title of the error dialog
     * @param message message body of the error dialog
     */
    private static void showLocationErrorDialog(AppCompatActivity activity, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, id) -> { })
                .create()
                .show();
    }

    public static Dialog createQrCodeDialog(AppCompatActivity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.qr_code_instructions, null);
        CheckBox neverShowDialog = view.findViewById(R.id.qr_code_never_show_again);

        neverShowDialog.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // Save the preference
            PreferenceUtils.saveBoolean(Application.get().getString(R.string.pref_key_never_show_qr_code_instructions), isChecked);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.qr_code_instructions_title)
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> {
                            IOUtils.openQrCodeReader(activity);
                        }
                ).setNegativeButton(R.string.not_now,
                        (dialog, which) -> {
                            // No op
                        }
                );
        return builder.create();
    }

    /**
     * Creates a dialog for sharing location and files
     *
     * @param activity
     * @param location
     * @param loggingEnabled true if logging is enabled, false if it is not
     * @param fileLogger the file logger being used to log files
     * @param alternateFileUri The URI for a file if a file other than the one current used by the FileLogger should be used (e.g., one previously picked from the folder browse button), or null if no alternate file is chosen and the file from the file logger should be shared.
     * @return a dialog for sharing location and files
     */
    public static Dialog createShareDialog(AppCompatActivity activity, final Location location,
                                           boolean loggingEnabled, FileLogger fileLogger,
                                           Uri alternateFileUri) {
        View view = activity.getLayoutInflater().inflate(R.layout.share, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.share)
                .setView(view)
                .setNeutralButton(R.string.main_help_close,
                        (dialog, which) -> {
                            // No-op
                        }
                );
        AlertDialog dialog = builder.create();
        TextView locationValue = view.findViewById(R.id.location_value);
        TextView fileName = view.findViewById(R.id.log_file_name);
        CheckBox includeAltitude = view.findViewById(R.id.include_altitude);
        TextView noLocation = view.findViewById(R.id.no_location);
        TextView logInstructions = view.findViewById(R.id.log_instructions);
        MaterialButton locationCopy = view.findViewById(R.id.location_copy);
        MaterialButton locationGeohack = view.findViewById(R.id.location_geohack);
        MaterialButton locationLaunchApp = view.findViewById(R.id.location_launch_app);
        MaterialButton locationShare = view.findViewById(R.id.location_share);
        MaterialButton logBrowse = view.findViewById(R.id.log_browse);
        MaterialButton logShare = view.findViewById(R.id.log_share);
        ChipGroup chipGroup = view.findViewById(R.id.coordinate_format_group);
        Chip chipDecimalDegrees = view.findViewById(R.id.chip_decimal_degrees);
        Chip chipDMS = view.findViewById(R.id.chip_dms);
        Chip chipDegreesDecimalMin = view.findViewById(R.id.chip_degrees_decimal_minutes);

        if (location == null) {
            // No location - Hide the location info
            locationValue.setVisibility(View.GONE);
            includeAltitude.setVisibility(View.GONE);
            chipGroup.setVisibility(View.GONE);
            locationCopy.setVisibility(View.GONE);
            locationGeohack.setVisibility(View.GONE);
            locationLaunchApp.setVisibility(View.GONE);
            locationShare.setVisibility(View.GONE);
        } else {
            // We have a location - Hide the "no location" message
            noLocation.setVisibility(View.GONE);
        }

        // Set default state of include altitude view
        boolean includeAltitudePref = Application.getPrefs().getBoolean(Application.get().getString(R.string.pref_key_share_include_altitude), false);
        includeAltitude.setChecked(includeAltitudePref);

        // Check selected coordinate format and show in UI
        String coordinateFormat = Application.getPrefs().getString(Application.get().getString(R.string.pref_key_coordinate_format), Application.get().getString(R.string.preferences_coordinate_format_dd_key));
        formatLocationForDisplay(location, locationValue, includeAltitude.isChecked(), chipDecimalDegrees, chipDMS, chipDegreesDecimalMin, coordinateFormat);

        // Change the location text when the user toggles the altitude checkbox
        includeAltitude.setOnCheckedChangeListener((view1, isChecked) -> {
            String format = "dd";
            if (chipDecimalDegrees.isChecked()) {
                format = "dd";
            } else if (chipDMS.isChecked()) {
                format = "dms";
            } else if (chipDegreesDecimalMin.isChecked()) {
                format = "ddm";
            }
            formatLocationForDisplay(location, locationValue, isChecked, chipDecimalDegrees, chipDMS, chipDegreesDecimalMin, format);
            PreferenceUtils.saveBoolean(Application.get().getString(R.string.pref_key_share_include_altitude), isChecked);
        });

        chipDecimalDegrees.setOnCheckedChangeListener((view1, isChecked) -> {
            if (isChecked) {
                locationValue.setText(IOUtils.createLocationShare(location, includeAltitude.isChecked()));
            }
        });
        chipDMS.setOnCheckedChangeListener((view1, isChecked) -> {
            if (isChecked) {
                if (location != null) {
                    locationValue.setText(IOUtils.createLocationShare(UIUtils.getDMSFromLocation(Application.get(), location.getLatitude(), UIUtils.COORDINATE_LATITUDE),
                            UIUtils.getDMSFromLocation(Application.get(), location.getLongitude(), UIUtils.COORDINATE_LONGITUDE),
                            location.hasAltitude() && includeAltitude.isChecked() ? Double.toString(location.getAltitude()) : null));
                }
            }
        });
        chipDegreesDecimalMin.setOnCheckedChangeListener((view1, isChecked) -> {
            if (isChecked) {
                if (location != null) {
                    locationValue.setText(IOUtils.createLocationShare(UIUtils.getDDMFromLocation(Application.get(), location.getLatitude(), UIUtils.COORDINATE_LATITUDE),
                            UIUtils.getDDMFromLocation(Application.get(), location.getLongitude(), UIUtils.COORDINATE_LONGITUDE),
                            location.hasAltitude() && includeAltitude.isChecked() ? Double.toString(location.getAltitude()) : null));
                }
            }
        });

        locationCopy.setOnClickListener(v -> {
            // Copy to clipboard
            if (location != null) {
                String locationString = locationValue.getText().toString();
                IOUtils.copyToClipboard(locationString);
                Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
            }
        });
        locationGeohack.setOnClickListener(v -> {
            // Open the browser to the GeoHack site with lots of coordinate conversions
            if (location != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String geohackUrl = Application.get().getString(R.string.geohack_url) +
                        location.getLatitude() + ";" +
                        location.getLongitude();
                intent.setData(Uri.parse(geohackUrl));
                activity.startActivity(intent);
            }
        });
        locationLaunchApp.setOnClickListener(v -> {
            // Open the location in another app
            if (location != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(IOUtils.createGeoUri(location, includeAltitude.isChecked())));
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(intent);
                }
            }
        });
        locationShare.setOnClickListener(v -> {
            // Send the location as a Geo URI (e.g., in an email) if the user has decimal degrees
            // selected, otherwise send plain text version
            if (location != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                String text;
                if (chipDecimalDegrees.isChecked()) {
                    text = IOUtils.createGeoUri(location, includeAltitude.isChecked());
                } else {
                    text = locationValue.getText().toString();
                }
                intent.putExtra(Intent.EXTRA_TEXT, text);
                intent.setType("text/plain");
                activity.startActivity(createChooser(intent, Application.get().getString(R.string.share)));
            }
        });

        final File file = fileLogger.getFile();

        if (loggingEnabled && file != null) {
            // Hide the logging instructions - logging is enabled and working
            logInstructions.setVisibility(View.GONE);
        } else {
            // Hide the logging and file views so the user can see the instructions
            fileName.setVisibility(View.GONE);
            logBrowse.setVisibility(View.GONE);
            logShare.setVisibility(View.GONE);
        }

        // Set the log file name
        if (loggingEnabled) {
            if (file != null) {
                if (alternateFileUri == null) {
                    // Set the log file currently being logged to by the FileLogger
                    fileName.setText(file.getName());
                } else {
                    // Set the log file selected by the user using the File Browse button
                    String lastPathSegment = alternateFileUri.getLastPathSegment();
                    // Parse file name from string like "primary:gnss_log/gnss_log_2019..."
                    String[] parts = lastPathSegment.split("/");
                    fileName.setText(parts[parts.length - 1]);
                }
            } else {
                // Something went wrong - did user allow file/storage permissions when prompted when they enabled logging in Settings?
                logInstructions.setText(R.string.log_error);
            }
        }

        logBrowse.setOnClickListener(v -> {
            // File browse
            Uri uri = IOUtils.getUriFromFile(activity, fileLogger.getFile());
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setData(uri);
            activity.startActivityForResult(intent, PICKFILE_REQUEST_CODE);
            // Dismiss the dialog - it will be re-created in the callback to GpsTestActivity
            dialog.dismiss();
        });

        logShare.setOnClickListener(v -> {
            // Send the log file
            if (alternateFileUri == null) {
                // Send the log file currently being logged to by the FileLogger
                fileLogger.send(activity);
            } else {
                // Send the log file selected by the user using the File Browse button
                IOUtils.sendLogFile(activity, alternateFileUri);
            }
        });

        return dialog;
    }

    /**
     * Returns the provided location based on the provided coordinate format, and sets the provided
     * Views (textView, chips) accordingly if views are provided, and returns the string value.
     *
     * @param location              location to be formatted
     * @param textView              View to be set with the selected coordinateFormat
     * @param includeAltitude       true if altitude should be included, false if it should not
     * @param chipDecimalDegrees    View to be set as checked if "dd" is the coordinateFormat
     * @param chipDMS               View to be set as checked if "dms" is the coordinateFormat
     * @param chipDegreesDecimalMin View to be set as checked if "ddm" is the coordinateFormat
     * @param coordinateFormat      dd, dms, or ddm
     * @return the provided location based on the provided coordinate format
     */
    public static String formatLocationForDisplay(Location location, TextView textView, boolean includeAltitude, Chip chipDecimalDegrees, Chip chipDMS, Chip chipDegreesDecimalMin, String coordinateFormat) {
        String formattedLocation = "";
        switch (coordinateFormat) {
            // Constants below must match string values in do_not_translate.xml
            case "dd":
                // Decimal degrees
                formattedLocation = IOUtils.createLocationShare(location, includeAltitude);
                if (chipDecimalDegrees != null) {
                    chipDecimalDegrees.setChecked(true);
                }
                break;
            case "dms":
                // Degrees minutes seconds
                if (location != null) {
                    formattedLocation = IOUtils.createLocationShare(UIUtils.getDMSFromLocation(Application.get(), location.getLatitude(), UIUtils.COORDINATE_LATITUDE),
                            UIUtils.getDMSFromLocation(Application.get(), location.getLongitude(), UIUtils.COORDINATE_LONGITUDE),
                            (location.hasAltitude() && includeAltitude) ? Double.toString(location.getAltitude()) : null);
                }
                if (chipDMS != null) {
                    chipDMS.setChecked(true);
                }
                break;
            case "ddm":
                // Degrees decimal minutes
                if (location != null) {
                    formattedLocation = IOUtils.createLocationShare(UIUtils.getDDMFromLocation(Application.get(), location.getLatitude(), UIUtils.COORDINATE_LATITUDE),
                            UIUtils.getDDMFromLocation(Application.get(), location.getLongitude(), UIUtils.COORDINATE_LONGITUDE),
                            (location.hasAltitude() && includeAltitude) ? Double.toString(location.getAltitude()) : null);
                }
                if (chipDegreesDecimalMin != null) {
                    chipDegreesDecimalMin.setChecked(true);
                }
                break;
            default:
                // Decimal degrees
                formattedLocation = IOUtils.createLocationShare(location, includeAltitude);
                if (chipDecimalDegrees != null) {
                    chipDecimalDegrees.setChecked(true);
                }
                break;
        }
        if (textView != null) {
            textView.setText(formattedLocation);
        }
        return formattedLocation;
    }

    /**
     * Resets the activity title so the locale is updated
     *
     * @param a the activity to reset the title for
     */
    public static void resetActivityTitle(Activity a) {
        try {
            ActivityInfo info = a.getPackageManager().getActivityInfo(a.getComponentName(), GET_META_DATA);
            if (info.labelRes != 0) {
                a.setTitle(info.labelRes);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns true if the app is running on a large screen device, false if it is not
     *
     * @return true if the app is running on a large screen device, false if it is not
     */
    public static boolean isLargeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Returns the display name for the given GnssType
     * @param context
     * @param gnssType
     * @return the display name for the given GnssType
     */
    public static String getGnssDisplayName(Context context, GnssType gnssType) {
        switch(gnssType) {
            case NAVSTAR:
                return context.getResources().getString(R.string.sky_legend_shape_navstar);
            case GALILEO:
                return context.getResources().getString(R.string.sky_legend_shape_galileo);
            case GLONASS:
                return context.getResources().getString(R.string.sky_legend_shape_glonass);
            case BEIDOU:
                return context.getResources().getString(R.string.sky_legend_shape_beidou);
            case QZSS:
                return context.getResources().getString(R.string.sky_legend_shape_qzss);
            case IRNSS:
                return context.getResources().getString(R.string.sky_legend_shape_irnss);
            case SBAS:
                return context.getResources().getString(R.string.sbas);
            case UNKNOWN:
            default:
                return context.getResources().getString(R.string.unknown);
        }
    }

    public static void setClickableSpan(TextView v, ClickableSpan span) {
        Spannable text = (Spannable) v.getText();
        text.setSpan(span, 0, text.length(), 0);
        v.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void removeAllClickableSpans(TextView v) {
        Spannable text = (Spannable) v.getText();
        ClickableSpan[] spans = text.getSpans(0, text.length(), ClickableSpan.class);
        for (ClickableSpan cs : spans) {
            text.removeSpan(cs);
        }
    }
}
