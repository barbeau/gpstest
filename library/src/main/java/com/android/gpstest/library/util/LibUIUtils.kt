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
package com.android.gpstest.library.util

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.text.Spannable
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android.gpstest.library.R
import com.android.gpstest.library.model.CoordinateType
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.google.android.material.chip.Chip
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Collections
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.isGone
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Utilities for processing user interface elements
 */
object LibUIUtils {
    const val TAG = "UIUtils"
    var PICKFILE_REQUEST_CODE = 101
    const val ANIMATION_DURATION_SHORT_MS = 200
    private const val MIN_VALUE_CN0 = 10.0f
    private const val MAX_VALUE_CN0 = 45.0f

    // Dialogs
    const val WHATSNEW_DIALOG = 1
    const val HELP_DIALOG = 2
    const val CLEAR_ASSIST_WARNING_DIALOG = 3
    private const val WHATS_NEW_VER = "whatsNewVer"

    /**
     * Formats a view so it is ignored for accessible access
     */
    @JvmStatic
    fun setAccessibilityIgnore(view: View) {
        view.isClickable = false
        view.isFocusable = false
        view.contentDescription = ""
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Converts screen dimension units from dp to pixels, based on algorithm defined in
     * http://developer.android.com/guide/practices/screens_support.html#dips-pels
     *
     * @param dp value in dp
     * @return value in pixels
     */
    @JvmStatic
    fun dpToPixels(context: Context, dp: Float): Int {
        // Get the screen's density scale
        val scale = context.resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        return (dp * scale + 0.5f).toInt()
    }

    /**
     * Returns true if the current display is wide enough to show the GPS date on the Status screen,
     * false if the current display is too narrow to fit the GPS date
     * @param context
     * @return true if the current display is wide enough to show the GPS date on the Status screen,
     * false if the current display is too narrow to fit the GPS date
     */
    fun isWideEnoughForDate(context: Context): Boolean {
        // 450dp is a little larger than the width of a Samsung Galaxy S8+
        val WIDTH_THRESHOLD = dpToPixels(context, 450f)
        return context.resources.displayMetrics.widthPixels > WIDTH_THRESHOLD
    }

    /**
     * Returns true if the activity is still active and dialogs can be managed (i.e., displayed
     * or dismissed), or false if it is not
     *
     * @param activity Activity to check for displaying/dismissing a dialog
     * @return true if the activity is still active and dialogs can be managed, or false if it is
     * not
     */
    fun canManageDialog(activity: Activity?): Boolean {
        return if (activity == null) {
            false
        } else !activity.isFinishing && !activity.isDestroyed
    }

    /**
     * Returns true if the fragment is attached to the activity, or false if it is not attached
     *
     * @param f fragment to be tested
     * @return true if the fragment is attached to the activity, or false if it is not attached
     */
    fun isFragmentAttached(f: Fragment): Boolean {
        return f.activity != null && f.isAdded
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
    @JvmStatic
    fun cn0ToIndicatorLeftMarginPx(
        cn0: Float,
        minIndicatorMarginPx: Int,
        maxIndicatorMarginPx: Int
    ): Int {
        return MathUtils.mapToRange(
            cn0,
            MIN_VALUE_CN0,
            MAX_VALUE_CN0,
            minIndicatorMarginPx.toFloat(),
            maxIndicatorMarginPx.toFloat()
        ).toInt()
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
    @JvmStatic
    fun cn0ToTextViewLeftMarginPx(
        cn0: Float,
        minTextViewMarginPx: Int,
        maxTextViewMarginPx: Int
    ): Int {
        return MathUtils.mapToRange(
            cn0,
            MIN_VALUE_CN0,
            MAX_VALUE_CN0,
            minTextViewMarginPx.toFloat(),
            maxTextViewMarginPx.toFloat()
        ).toInt()
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
    fun setMargins(v: View, l: Int, t: Int, r: Int, b: Int) {
        val p = v.layoutParams as MarginLayoutParams
        p.setMargins(l, t, r, b)
        v.layoutParams = p
    }

    /**
     * Opens email apps based on the given email address
     * @param email address
     * @param location string that shows the current location
     * @param signalInfoViewModel view model that contains state of GNSS
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun sendEmail(
        context: Context,
        email: String,
        location: String?,
        signalInfoViewModel: SignalInfoViewModel,
        playServicesVersion: String,
        prefs: SharedPreferences,
        buildFlavor: String,
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val pm = context.packageManager
        val appInfo: PackageInfo
        val body = StringBuilder()
        body.append(context.getString(R.string.feedback_body))
        var versionName: String? = ""
        var versionCode = 0
        try {
            appInfo = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            versionName = appInfo.versionName
            versionCode = appInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            // Leave version as empty string
        }

        // App version
        body.append("App version: v")
            .append(versionName)
            .append(" (")
            .append(versionCode)
            .append(
                """
    -${buildFlavor})
    
    """.trimIndent()
            )

        // Device properties
        body.append(
            """
    Model: ${Build.MODEL}
    
    """.trimIndent()
        )
        body.append(
            """Android version: ${Build.VERSION.RELEASE} / ${Build.VERSION.SDK_INT}
"""
        )
        if (!TextUtils.isEmpty(location)) {
            body.append("Location: $location\n")
        }
        body.append(
            """
    GNSS HW year: ${IOUtils.getGnssHardwareYear(context)}
    
    """.trimIndent()
        )
        if (IOUtils.getGnssHardwareModelName(context).trim { it <= ' ' }.isNotEmpty()) {
            body.append(
                """
    GNSS HW name: ${IOUtils.getGnssHardwareModelName(context)}
    
    """.trimIndent()
            )
        }

        // Raw GNSS measurement capability
        var capability = prefs.getInt(
            context.getString(R.string.capability_key_raw_measurements),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(
                context.getString(
                    R.string.capability_title_raw_measurements,
                    PreferenceUtils.getCapabilityDescription(context, capability)
                )
            )
        }

        // Navigation messages capability
        capability = prefs.getInt(
            context.getString(R.string.capability_key_nav_messages),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(
                context.getString(
                    R.string.capability_title_nav_messages,
                    PreferenceUtils.getCapabilityDescription(context, capability)
                )
            )
        }

        // NMEA capability
        capability = prefs.getInt(
            context.getString(R.string.capability_key_nmea),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(
                context.getString(
                    R.string.capability_title_nmea,
                    PreferenceUtils.getCapabilityDescription(context, capability)
                )
            )
        }

        // Inject PSDS capability
        capability = prefs.getInt(
            context.getString(R.string.capability_key_inject_psds),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(
                context.getString(
                    R.string.capability_title_inject_psds,
                    PreferenceUtils.getCapabilityDescription(context, capability)
                )
            )
        }

        // Inject time capability
        capability = prefs.getInt(
            context.getString(R.string.capability_key_inject_time),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(
                context.getString(
                    R.string.capability_title_inject_time,
                    PreferenceUtils.getCapabilityDescription(context, capability)
                )
            )
        }

        // Delete assist capability
        capability = prefs.getInt(
            context.getString(R.string.capability_key_delete_assist),
            PreferenceUtils.CAPABILITY_UNKNOWN
        )
        if (capability != PreferenceUtils.CAPABILITY_UNKNOWN) {
            body.append(
                context.getString(
                    R.string.capability_title_delete_assist,
                    PreferenceUtils.getCapabilityDescription(context, capability)
                )
            )
        }

        // Got fix
        body.append(
            context.getString(
                R.string.capability_title_got_fix,
                location != null && signalInfoViewModel.gotFirstFix()
            )
        )

        // We need a fix to determine these attributes reliably
        if (location != null && signalInfoViewModel.gotFirstFix()) {
            // Dual frequency
            body.append(
                context.getString(
                    R.string.capability_title_dual_frequency,
                    PreferenceUtils.getCapabilityDescription(context, signalInfoViewModel.isNonPrimaryCarrierFreqInView)
                )
            )
            // Supported GNSS
            val gnss: List<GnssType> = ArrayList(signalInfoViewModel.getSupportedGnss())
            Collections.sort(gnss)
            body.append(
                context.getString(
                    R.string.capability_title_supported_gnss, IOUtils.trimEnds(
                        IOUtils.replaceNavstar(gnss.toString())
                    )
                )
            )
            // GNSS CF
            val gnssCfs: List<String> = ArrayList(signalInfoViewModel.getSupportedGnssCfs())
            if (gnssCfs.isNotEmpty()) {
                Collections.sort(gnssCfs)
                body.append(
                    context.getString(
                        R.string.capability_title_gnss_cf,
                        IOUtils.trimEnds(gnssCfs.toString())
                    )
                )
            }
            // Supported SBAS
            val sbas: List<SbasType> = ArrayList(signalInfoViewModel.getSupportedSbas())
            if (sbas.isNotEmpty()) {
                Collections.sort(sbas)
                body.append(
                    context.getString(
                        R.string.capability_title_supported_sbas,
                        IOUtils.trimEnds(sbas.toString())
                    )
                )
            }
            // SBAS CF
            val sbasCfs: List<String> = ArrayList(signalInfoViewModel.getSupportedSbasCfs())
            if (sbasCfs.isNotEmpty()) {
                Collections.sort(sbasCfs)
                body.append(
                    context.getString(
                        R.string.capability_title_sbas_cf,
                        IOUtils.trimEnds(sbasCfs.toString())
                    )
                )
            }
            // Accumulated delta range
            body.append(
                context.getString(
                    R.string.capability_title_accumulated_delta_range,
                    PreferenceUtils.getCapabilityDescription(
                        context,
                        prefs.getInt(
                            context.getString(R.string.capability_key_measurement_delta_range),
                            PreferenceUtils.CAPABILITY_UNKNOWN
                        )
                    )
                )
            )
            // Automatic gain control
            body.append(
                context.getString(
                  R.string.capability_title_automatic_gain_control,
                    PreferenceUtils.getCapabilityDescription(
                        context,
                        prefs.getInt(
                            context.getString(R.string.capability_key_measurement_automatic_gain_control),
                            PreferenceUtils.CAPABILITY_UNKNOWN
                        )
                    )
                )
            )
        }

        // GNSS Antenna Info
        val gnssAntennaInfo = context.getString(
            R.string.capability_title_gnss_antenna_info,
            PreferenceUtils.getCapabilityDescription(
                context,
                SatelliteUtils.isGnssAntennaInfoSupported(
                    locationManager
                )
            )
        )
        body.append(gnssAntennaInfo)
        if (gnssAntennaInfo == context.getString(R.string.capability_value_supported)) {
            body.append(
                context.getString(
                    R.string.capability_title_num_antennas, PreferenceUtils.getInt(
                        context.getString(R.string.capability_key_num_antenna), -1, prefs
                    )
                )
            )
            body.append(
                context.getString(
                    R.string.capability_title_antenna_cfs, PreferenceUtils.getString(
                        context.getString(R.string.capability_key_antenna_cf), prefs
                    )
                )
            )
        }
        if (!TextUtils.isEmpty(playServicesVersion)) {
            body.append(
                """
    
    $playServicesVersion
    """.trimIndent()
            )
        }
        body.append("\n\n\n")
        val send = Intent(Intent.ACTION_SENDTO)
        send.data = "mailto:".toUri()
        send.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        val subject = context.getString(R.string.feedback_subject)
        send.putExtra(Intent.EXTRA_SUBJECT, subject)
        send.putExtra(Intent.EXTRA_TEXT, body.toString())
        try {
            context.startActivity(Intent.createChooser(send, subject))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.feedback_error, Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Returns the provided latitude or longitude value in Degrees Minutes Seconds (DMS) format
     * @param coordinate latitude or longitude to convert to DMS format
     * @param coordinateType whether the coordinate is latitude or longitude
     * @return the provided latitude or longitude value in Degrees Minutes Seconds (DMS) format
     */
    @JvmStatic
    fun getDMSFromLocation(
        context: Context,
        coordinate: Double,
        coordinateType: CoordinateType
    ): String {
        val loc = BigDecimal(coordinate)
        val degrees = loc.setScale(0, RoundingMode.DOWN)
        val minTemp = loc.subtract(degrees).multiply(BigDecimal(60)).abs()
        val minutes = minTemp.setScale(0, RoundingMode.DOWN)
        val seconds =
            minTemp.subtract(minutes).multiply(BigDecimal(60)).setScale(2, RoundingMode.HALF_UP)
        val hemisphere: String
        val output_string: Int
        if (coordinateType == CoordinateType.LATITUDE) {
            hemisphere = if (coordinate < 0) "S" else "N"
            output_string = R.string.gps_lat_dms_value
        } else {
            hemisphere = if (coordinate < 0) "W" else "E"
            output_string = R.string.gps_lon_dms_value
        }
        return context.getString(
            output_string,
            hemisphere,
            degrees.abs().toInt(),
            minutes.toInt(),
            seconds.toFloat()
        )
    }

    /**
     * Returns the provided latitude or longitude value in Decimal Degree Minutes (DDM) format
     *
     * @param coordinate latitude or longitude to convert to DDM format
     * @param coordinateType lat or lon to format hemisphere
     * @return the provided latitude or longitude value in Decimal Degree Minutes (DDM) format
     */
    @JvmStatic
    fun getDDMFromLocation(
        context: Context,
        coordinate: Double,
        coordinateType: CoordinateType
    ): String {
        val loc = BigDecimal(coordinate)
        val degrees = loc.setScale(0, RoundingMode.DOWN)
        val minutes =
            loc.subtract(degrees).multiply(BigDecimal(60)).abs().setScale(3, RoundingMode.HALF_UP)
        val hemisphere: String
        val output_string: Int
        if (coordinateType == CoordinateType.LATITUDE) {
            hemisphere = if (coordinate < 0) "S" else "N"
            output_string = R.string.gps_lat_ddm_value
        } else {
            hemisphere = if (coordinate < 0) "W" else "E"
            output_string = R.string.gps_lon_ddm_value
        }
        return context.getString(
            output_string,
            hemisphere,
            degrees.abs().toInt(),
            minutes.toFloat()
        )
    }

    /**
     * Converts the provide value in meters to the corresponding value in feet
     * @param meters value in meters to convert to feet
     * @return the provided meters value converted to feet
     */
    @JvmStatic
    fun toFeet(meters: Double): Double {
        return meters * 1000.0 / 25.4 / 12.0
    }

    /**
     * Converts the provide value in meters per second to the corresponding value in kilometers per hour
     * @param metersPerSecond value in meters per second to convert to kilometers per hour
     * @return the provided meters per second value converted to kilometers per hour
     */
    @JvmStatic
    fun toKilometersPerHour(metersPerSecond: Float): Float {
        return metersPerSecond * 3600f / 1000f
    }

    /**
     * Converts the provide value in meters per second to the corresponding value in miles per hour
     * @param metersPerSecond value in meters per second to convert to miles per hour
     * @return the provided meters per second value converted to miles per hour
     */
    @JvmStatic
    fun toMilesPerHour(metersPerSecond: Float): Float {
        return toKilometersPerHour(metersPerSecond) / 1.6093440f
    }

    /**
     * Sets the vertical bias for a provided view that is within a ConstraintLayout
     * @param view view within a ConstraintLayout
     * @param bias vertical bias to be used
     */
    @JvmStatic
    fun setVerticalBias(view: View, bias: Float) {
        val params = view.layoutParams as ConstraintLayout.LayoutParams
        params.verticalBias = bias
        view.layoutParams = params
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
    fun formatLocationForDisplay(
        context: Context,
        location: Location?,
        textView: TextView?,
        includeAltitude: Boolean,
        chipDecimalDegrees: Chip?,
        chipDMS: Chip?,
        chipDegreesDecimalMin: Chip?,
        coordinateFormat: String?
    ): String {
        var formattedLocation = ""
        when (coordinateFormat) {
            "dd" -> {
                // Decimal degrees
                if (location != null) {
                    formattedLocation = IOUtils.createLocationShare(location, includeAltitude)
                }
                if (chipDecimalDegrees != null) {
                    chipDecimalDegrees.isChecked = true
                }
            }
            "dms" -> {
                // Degrees minutes seconds
                if (location != null) {
                    formattedLocation = IOUtils.createLocationShare(
                        getDMSFromLocation(context, location.latitude, CoordinateType.LATITUDE),
                        getDMSFromLocation(context, location.longitude, CoordinateType.LONGITUDE),
                        if (location.hasAltitude() && includeAltitude) location.altitude.toString() else null
                    )
                }
                if (chipDMS != null) {
                    chipDMS.isChecked = true
                }
            }
            "ddm" -> {
                // Degrees decimal minutes
                if (location != null) {
                    formattedLocation = IOUtils.createLocationShare(
                        getDDMFromLocation(context, location.latitude, CoordinateType.LATITUDE),
                        getDDMFromLocation(context, location.longitude, CoordinateType.LONGITUDE),
                        if (location.hasAltitude() && includeAltitude) location.altitude.toString() else null
                    )
                }
                if (chipDegreesDecimalMin != null) {
                    chipDegreesDecimalMin.isChecked = true
                }
            }
            else -> {
                // Decimal degrees
                formattedLocation = IOUtils.createLocationShare(location, includeAltitude)
                if (chipDecimalDegrees != null) {
                    chipDecimalDegrees.isChecked = true
                }
            }
        }
        if (textView != null) {
            textView.text = formattedLocation
        }
        return formattedLocation
    }

    /**
     * Resets the activity title so the locale is updated
     *
     * @param a the activity to reset the title for
     */
    @JvmStatic
    fun resetActivityTitle(a: Activity) {
        try {
            val info =
                a.packageManager.getActivityInfo(a.componentName, PackageManager.GET_META_DATA)
            if (info.labelRes != 0) {
                a.setTitle(info.labelRes)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * Returns true if the app is running on a large screen device, false if it is not
     *
     * @return true if the app is running on a large screen device, false if it is not
     */
    fun isLargeScreen(context: Context): Boolean {
        return (context.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    /**
     * Returns the display name for the given GnssType
     * @param context
     * @param gnssType
     * @return the display name for the given GnssType
     */
    fun getGnssDisplayName(context: Context, gnssType: GnssType?): String {
        return when (gnssType) {
            GnssType.NAVSTAR -> context.resources.getString(R.string.sky_legend_shape_navstar)
            GnssType.GALILEO -> context.resources.getString(R.string.sky_legend_shape_galileo)
            GnssType.GLONASS -> context.resources.getString(R.string.sky_legend_shape_glonass)
            GnssType.BEIDOU -> context.resources.getString(R.string.sky_legend_shape_beidou)
            GnssType.QZSS -> context.resources.getString(R.string.sky_legend_shape_qzss)
            GnssType.IRNSS -> context.resources.getString(R.string.sky_legend_shape_irnss)
            GnssType.SBAS -> context.resources.getString(R.string.sbas)
            GnssType.UNKNOWN -> context.resources.getString(R.string.unknown)
            else -> context.resources.getString(R.string.unknown)
        }
    }

    fun setClickableSpan(v: TextView, span: ClickableSpan?) {
        val text = v.text as Spannable
        text.setSpan(span, 0, text.length, 0)
        v.movementMethod = LinkMovementMethod.getInstance()
    }

    fun removeAllClickableSpans(v: TextView) {
        val text = v.text as Spannable
        val spans = text.getSpans(0, text.length, ClickableSpan::class.java)
        for (cs in spans) {
            text.removeSpan(cs)
        }
    }

    /**
     * Shows a view using animation
     *
     * @param v                 View to show
     * @param animationDuration duration of animation
     */
    fun showViewWithAnimation(v: View, animationDuration: Int) {
        if (v.isVisible && v.alpha == 1f) {
            // View is already visible and not transparent, return without doing anything
            return
        }
        v.clearAnimation()
        v.animate().cancel()
        if (v.visibility != View.VISIBLE) {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            v.alpha = 0f
            v.visibility = View.VISIBLE
        }

        // Animate the content view to 100% opacity, and clear any animation listener set on the view.
        v.animate()
            .alpha(1f)
            .setDuration(animationDuration.toLong())
            .setListener(null)
    }

    /**
     * Hides a view using animation
     *
     * @param v                 View to hide
     * @param animationDuration duration of animation
     */
    fun hideViewWithAnimation(v: View, animationDuration: Int) {
        if (v.isGone) {
            // View is already gone, return without doing anything
            return
        }
        v.clearAnimation()
        v.animate().cancel()

        // Animate the view to 0% opacity. After the animation ends, set its visibility to GONE as
        // an optimization step (it won't participate in layout passes, etc.)
        v.animate()
            .alpha(0f)
            .setDuration(animationDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                }
            })
    }

    /**
     * Shows the dialog to explain why location permissions are needed
     *
     * NOTE - this dialog can't be managed under the old dialog framework as the method
     * ActivityCompat.shouldShowRequestPermissionRationale() always returns false.
     */
    fun showLocationPermissionDialog(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.title_location_permission)
            .setMessage(R.string.text_location_permission)
            .setCancelable(false)
            .setPositiveButton(
                R.string.ok
            ) { dialog: DialogInterface?, which: Int ->
                // Request permissions from the user
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    PermissionUtils.LOCATION_PERMISSION_REQUEST
                )
            }
            .setNegativeButton(
                R.string.exit
            ) { dialog: DialogInterface?, which: Int ->
                // Exit app
                activity.finish()
            }
        builder.create().show()
    }

    /**
     * Ask the user if they want to enable GPS, and if so, show them system settings
     */
    fun promptEnableGps(context: Context,activity: Activity) {
        AlertDialog.Builder(activity)
            .setMessage(context.getString(R.string.enable_gps_message))
            .setPositiveButton(
                context.getString(R.string.enable_gps_positive_button)
            ) { dialog: DialogInterface?, which: Int ->
                val intent = Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
                activity.startActivity(intent)
            }
            .setNegativeButton(
                context.getString(R.string.enable_gps_negative_button)
            ) { dialog: DialogInterface?, which: Int -> }
            .show()
    }


    /**
     * Saves the "sort by" order to preferences
     *
     * @param index the index of R.array.sort_sats that should be set
     */
    fun setSortByClause(context: Context, index: Int, prefs: SharedPreferences) {
        val sortOptions = context.resources.getStringArray(R.array.sort_sats)
        PreferenceUtils.saveString(
            context.resources.getString(R.string.pref_key_default_sat_sort),
            sortOptions[index],
            prefs
        )
    }

    /**
     * Show the "What's New" message if a new version was just installed
     */
    fun autoShowWhatsNew(prefs: SharedPreferences, context: Context, activity: Activity) {
        // Get the current app version.
        val appInfo: PackageInfo = try {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Do nothing
            return
        }
        val oldVer = prefs.getInt(WHATS_NEW_VER, 0)
        val newVer = appInfo.versionCode
        if (oldVer < newVer) {
            activity.showDialog(WHATSNEW_DIALOG)
            PreferenceUtils.saveInt(WHATS_NEW_VER, appInfo.versionCode, prefs)
        }
    }

    /**
     * Returns the `location` object as a human readable string for use in a notification summary
     */
    fun Location?.toNotificationSummary(context: Context, prefs: SharedPreferences): String {
        return if (this != null) {
            val lat = FormatUtils.formatLatOrLon(context, latitude, CoordinateType.LATITUDE, prefs)
            val lon = FormatUtils.formatLatOrLon(context, longitude, CoordinateType.LONGITUDE, prefs)
            val alt = FormatUtils.formatAltitude(context, this, prefs)
            val speed = FormatUtils.formatSpeed(context,this, prefs)
            val bearing = FormatUtils.formatBearing(context,this)
            "$lat $lon $alt | $speed | $bearing"
        } else {
            "Unknown location"
        }
    }
}