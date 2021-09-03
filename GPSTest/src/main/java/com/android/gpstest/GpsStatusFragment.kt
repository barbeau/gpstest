/*
 * Copyright (C) 2008-2018 The Android Open Source Project,
 * Sean J. Barbeau (sjbarbeau@gmail.com)
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
package com.android.gpstest

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.GnssStatus
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.gpstest.model.*
import com.android.gpstest.ui.GnssFilterDialog
import com.android.gpstest.util.*
import com.android.gpstest.util.DateTimeUtils.Companion.NUM_DAYS_TIME_VALID
import com.android.gpstest.util.DateTimeUtils.Companion.isTimeValid
import com.android.gpstest.util.SortUtil.Companion.sortByCarrierFrequencyThenId
import com.android.gpstest.util.SortUtil.Companion.sortByCn0
import com.android.gpstest.util.SortUtil.Companion.sortByGnssThenCarrierFrequencyThenId
import com.android.gpstest.util.SortUtil.Companion.sortByGnssThenCn0ThenId
import com.android.gpstest.util.SortUtil.Companion.sortByGnssThenId
import com.android.gpstest.util.SortUtil.Companion.sortByGnssThenUsedThenId
import com.android.gpstest.util.SortUtil.Companion.sortBySbasThenCarrierFrequencyThenId
import com.android.gpstest.util.SortUtil.Companion.sortBySbasThenCn0ThenId
import com.android.gpstest.util.SortUtil.Companion.sortBySbasThenId
import com.android.gpstest.util.SortUtil.Companion.sortBySbasThenUsedThenId
import com.android.gpstest.util.SortUtil.Companion.sortByUsedThenId
import java.text.SimpleDateFormat
import java.util.*

class GpsStatusFragment : Fragment() {
    @SuppressLint("SimpleDateFormat")
    var mTimeFormat// See #117
            = SimpleDateFormat(
        if (DateFormat.is24HourFormat(Application.get().applicationContext)) "HH:mm:ss" else "hh:mm:ss a"
    )
    var mTimeAndDateFormat = SimpleDateFormat(
        if (DateFormat.is24HourFormat(Application.get().applicationContext)) "HH:mm:ss MMM d, yyyy z" else "hh:mm:ss a MMM d, yyyy z"
    )
    private var mLatitudeView: TextView? = null
    private var mLongitudeView: TextView? = null
    private var mFixTimeView: TextView? = null
    private var mTTFFView: TextView? = null
    private var mAltitudeView: TextView? = null
    private var mAltitudeMslView: TextView? = null
    private var mHorVertAccuracyLabelView: TextView? = null
    private var mHorVertAccuracyView: TextView? = null
    private var mSpeedView: TextView? = null
    private var mSpeedAccuracyView: TextView? = null
    private var mBearingView: TextView? = null
    private var mBearingAccuracyView: TextView? = null
    private var mNumSats: TextView? = null
    private var mPdopLabelView: TextView? = null
    private var mPdopView: TextView? = null
    private var mHvdopLabelView: TextView? = null
    private var mHvdopView: TextView? = null
    private var mGnssNotAvailableView: TextView? = null
    private var mSbasNotAvailableView: TextView? = null
    private var mFixTimeErrorView: TextView? = null
    private var filterGroup: ViewGroup? = null
    private var filterTextView: TextView? = null
    private var mLocation: Location? = null
    private var mSpeedBearingAccuracyRow: TableRow? = null
    private var mGnssStatusList: RecyclerView? = null
    private var mSbasStatusList: RecyclerView? = null
    private var mGnssAdapter: SatelliteStatusAdapter? = null
    private var mSbasAdapter: SatelliteStatusAdapter? = null
    private var mGnssStatus: MutableList<SatelliteStatus> = ArrayList()
    private var mSbasStatus: MutableList<SatelliteStatus> = ArrayList()
    private var svCount = 0
    private var svVisibleCount = 0
    private var mSnrCn0Title: String? = null
    private var mFixTime: Long = 0
    private var mNavigating = false
    private var mFlagUsa: Drawable? = null
    private var mFlagRussia: Drawable? = null
    private var mFlagJapan: Drawable? = null
    private var mFlagChina: Drawable? = null
    private var mFlagIndia: Drawable? = null
    private var mFlagEU: Drawable? = null
    private var mFlagICAO: Drawable? = null
    private var mTtff = ""
    var mPrefDistanceUnits: String? = null
    var mPrefSpeedUnits: String? = null
    var mViewModel: DeviceInfoViewModel? = null
    var lock: ImageView? = null
    private val mSatelliteMetadataObserver: Observer<SatelliteMetadata> =
        Observer { satelliteMetadata ->
            if (satelliteMetadata != null) {
                mNumSats!!.text = resources!!.getString(
                    R.string.gps_num_sats_value,
                    satelliteMetadata.numSatsUsed,
                    satelliteMetadata.numSatsInView,
                    satelliteMetadata.numSatsTotal
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupUnitPreferences()
        val v = inflater.inflate(R.layout.gps_status, container, false)
        lock = v.findViewById(R.id.status_lock)
        mLatitudeView = v.findViewById(R.id.latitude)
        mLongitudeView = v.findViewById(R.id.longitude)
        mLatitudeView?.text = EMPTY_LAT_LONG
        mLongitudeView?.text = EMPTY_LAT_LONG

        mFixTimeView = v.findViewById(R.id.fix_time)
        mFixTimeErrorView = v.findViewById(R.id.fix_time_error)
        mFixTimeErrorView!!.setOnClickListener(View.OnClickListener {
            showTimeErrorDialog(
                mFixTime
            )
        })

        mTTFFView = v.findViewById(R.id.ttff)
        mAltitudeView = v.findViewById(R.id.altitude)
        mAltitudeMslView = v.findViewById(R.id.altitude_msl)
        mHorVertAccuracyLabelView = v.findViewById(R.id.hor_vert_accuracy_label)
        mHorVertAccuracyView = v.findViewById(R.id.hor_vert_accuracy)
        mSpeedView = v.findViewById(R.id.speed)
        mSpeedAccuracyView = v.findViewById(R.id.speed_acc)
        mBearingView = v.findViewById(R.id.bearing)
        mBearingAccuracyView = v.findViewById(R.id.bearing_acc)
        mNumSats = v.findViewById(R.id.num_sats)
        mPdopLabelView = v.findViewById(R.id.pdop_label)
        mPdopView = v.findViewById(R.id.pdop)
        mHvdopLabelView = v.findViewById(R.id.hvdop_label)
        mHvdopView = v.findViewById(R.id.hvdop)
        mSpeedBearingAccuracyRow = v.findViewById(R.id.speed_bearing_acc_row)
        mGnssNotAvailableView = v.findViewById(R.id.gnss_not_available)
        mSbasNotAvailableView = v.findViewById(R.id.sbas_not_available)
        mFlagUsa = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_usa)
        mFlagRussia = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_russia)
        mFlagJapan = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_japan)
        mFlagChina = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_china)
        mFlagIndia = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_india)
        mFlagEU = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_european_union)
        mFlagICAO = ContextCompat.getDrawable(Application.get(), R.drawable.ic_flag_icao)
        val detector = GestureDetectorCompat(context, object : SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val location = mLocation
                // Copy location to clipboard
                if (location != null) {
                    val includeAltitude = Application.getPrefs().getBoolean(
                        Application.get().getString(R.string.pref_key_share_include_altitude), false
                    )
                    val coordinateFormat = Application.getPrefs().getString(
                        Application.get().getString(R.string.pref_key_coordinate_format),
                        Application.get().getString(R.string.preferences_coordinate_format_dd_key)
                    )
                    val formattedLocation = UIUtils.formatLocationForDisplay(
                        location, null, includeAltitude,
                        null, null, null, coordinateFormat
                    )
                    if (!TextUtils.isEmpty(formattedLocation)) {
                        IOUtils.copyToClipboard(formattedLocation)
                        Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_LONG)
                            .show()
                    }
                }
                return false
            }
        })
        val locationScrollView =
            v.findViewById<HorizontalScrollView>(R.id.status_location_scrollview)
        locationScrollView.setOnTouchListener { view: View?, motionEvent: MotionEvent? ->
            detector.onTouchEvent(motionEvent)
            false
        }

        // GNSS filter
        filterGroup = v.findViewById(R.id.status_filter_group)
        filterTextView = v.findViewById(R.id.filter_text)
        val filterShowAllView = v.findViewById<TextView>(R.id.filter_show_all)

        // Remove any previous clickable spans to avoid issues with recycling views
        UIUtils.removeAllClickableSpans(filterShowAllView)
        // Save an empty set to preferences to show all satellites
        val filterShowAllClick: ClickableSpan = object : ClickableSpan() {
            override fun onClick(v: View) {
                // Save an empty set to preferences to show all satellites
                PreferenceUtils.saveGnssFilter(LinkedHashSet())
            }
        }
        UIUtils.setClickableSpan(filterShowAllView, filterShowAllClick)

        // GNSS
        val llmGnss = LinearLayoutManager(context)
        llmGnss.isAutoMeasureEnabled = true
        llmGnss.orientation = RecyclerView.VERTICAL
        mGnssStatusList = v.findViewById(R.id.gnss_status_list)
        mGnssAdapter = SatelliteStatusAdapter(ConstellationType.GNSS)
        mGnssStatusList?.adapter = mGnssAdapter
        mGnssStatusList?.isFocusable = false
        mGnssStatusList?.isFocusableInTouchMode = false
        mGnssStatusList?.layoutManager = llmGnss
        mGnssStatusList?.isNestedScrollingEnabled = false

        // SBAS
        val llmSbas = LinearLayoutManager(context)
        llmSbas.isAutoMeasureEnabled = true
        llmSbas.orientation = RecyclerView.VERTICAL
        mSbasStatusList = v.findViewById(R.id.sbas_status_list)
        mSbasAdapter = SatelliteStatusAdapter(ConstellationType.SBAS)
        mSbasStatusList?.adapter = mSbasAdapter
        mSbasStatusList?.isFocusable = false
        mSbasStatusList?.isFocusableInTouchMode = false
        mSbasStatusList?.layoutManager = llmSbas
        mSbasStatusList?.isNestedScrollingEnabled = false
        mViewModel = ViewModelProviders.of(requireActivity()).get(
            DeviceInfoViewModel::class.java
        )
        mViewModel!!.satelliteMetadata.observe(requireActivity(), mSatelliteMetadataObserver)
        return v
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setStarted(navigating: Boolean) {
        if (navigating != mNavigating) {
            if (!navigating) {
                mViewModel!!.reset()
                mLatitudeView!!.text = EMPTY_LAT_LONG
                mLongitudeView!!.text = EMPTY_LAT_LONG
                mFixTime = 0
                updateFixTime()
                updateFilterView()
                mTTFFView!!.text = ""
                mAltitudeView!!.text = ""
                mAltitudeMslView!!.text = ""
                mHorVertAccuracyView!!.text = ""
                mSpeedView!!.text = ""
                mSpeedAccuracyView!!.text = ""
                mBearingView!!.text = ""
                mBearingAccuracyView!!.text = ""
                mNumSats!!.text = ""
                mPdopView!!.text = ""
                mHvdopView!!.text = ""
                if (lock != null) {
                    lock!!.visibility = View.GONE
                }
                svCount = 0
                svVisibleCount = 0
                mGnssStatus.clear()
                mSbasStatus.clear()
                mGnssAdapter!!.notifyDataSetChanged()
                mSbasAdapter!!.notifyDataSetChanged()
            }
            mNavigating = navigating
        }
    }

    private fun updateFixTime() {
        if (mFixTime == 0L || !PreferenceUtils.isTrackingStarted()) {
            mFixTimeView?.text = ""
            mFixTimeErrorView?.text = ""
            mFixTimeErrorView?.visibility = View.GONE
        } else {
            if (isTimeValid(mFixTime)) {
                mFixTimeErrorView?.visibility = View.GONE
                mFixTimeView?.visibility = View.VISIBLE
                mFixTimeView?.text = formatFixTimeDate(mFixTime)
            } else {
                // Error in fix time
                mFixTimeErrorView?.visibility = View.VISIBLE
                mFixTimeView?.visibility = View.GONE
                mFixTimeErrorView?.text = formatFixTimeDate(mFixTime)
            }
        }
    }

    /**
     * Returns a formatted version of the provided fixTime based on the width of the current display
     * @return a formatted version of the provided fixTime based on the width of the current display
     */
    private fun formatFixTimeDate(fixTime: Long): String {
        val context = context ?: return ""
        return if (UIUtils.isWideEnoughForDate(context)) mTimeAndDateFormat.format(fixTime) else mTimeFormat.format(
            fixTime
        )
    }

    /**
     * Update views for horizontal and vertical location accuracies based on the provided location
     * @param location
     */
    private fun updateLocationAccuracies(location: Location) {
        if (SatelliteUtils.isVerticalAccuracySupported(location)) {
            mHorVertAccuracyLabelView!!.setText(R.string.gps_hor_and_vert_accuracy_label)
            if (mPrefDistanceUnits.equals(METERS, ignoreCase = true)) {
                mHorVertAccuracyView!!.text = resources.getString(
                    R.string.gps_hor_and_vert_accuracy_value_meters,
                    location.accuracy,
                    location.verticalAccuracyMeters
                )
            } else {
                // Feet
                mHorVertAccuracyView!!.text = resources.getString(
                    R.string.gps_hor_and_vert_accuracy_value_feet,
                    UIUtils.toFeet(location.accuracy.toDouble()),
                    UIUtils.toFeet(location.verticalAccuracyMeters.toDouble())
                )
            }
        } else {
            if (location.hasAccuracy()) {
                if (mPrefDistanceUnits.equals(METERS, ignoreCase = true)) {
                    mHorVertAccuracyView!!.text =
                        resources.getString(R.string.gps_accuracy_value_meters, location.accuracy)
                } else {
                    // Feet
                    mHorVertAccuracyView!!.text = resources.getString(
                        R.string.gps_accuracy_value_feet,
                        UIUtils.toFeet(location.accuracy.toDouble())
                    )
                }
            } else {
                mHorVertAccuracyView!!.text = ""
            }
        }
    }

    /**
     * Update views for speed and bearing location accuracies based on the provided location
     * @param location
     */
    private fun updateSpeedAndBearingAccuracies(location: Location) {
        if (SatelliteUtils.isSpeedAndBearingAccuracySupported()) {
            mSpeedBearingAccuracyRow!!.visibility = View.VISIBLE
            if (location.hasSpeedAccuracy()) {
                if (mPrefSpeedUnits.equals(METERS_PER_SECOND, ignoreCase = true)) {
                    mSpeedAccuracyView!!.text = resources!!.getString(
                        R.string.gps_speed_acc_value_meters_sec,
                        location.speedAccuracyMetersPerSecond
                    )
                } else if (mPrefSpeedUnits.equals(KILOMETERS_PER_HOUR, ignoreCase = true)) {
                    mSpeedAccuracyView!!.text = resources!!.getString(
                        R.string.gps_speed_acc_value_km_hour,
                        UIUtils.toKilometersPerHour(location.speedAccuracyMetersPerSecond)
                    )
                } else {
                    // Miles per hour
                    mSpeedAccuracyView!!.text = resources!!.getString(
                        R.string.gps_speed_acc_value_miles_hour,
                        UIUtils.toMilesPerHour(location.speedAccuracyMetersPerSecond)
                    )
                }
            } else {
                mSpeedAccuracyView!!.text = ""
            }
            if (location.hasBearingAccuracy()) {
                mBearingAccuracyView!!.text = resources!!.getString(
                    R.string.gps_bearing_acc_value,
                    location.bearingAccuracyDegrees
                )
            } else {
                mBearingAccuracyView!!.text = ""
            }
        } else {
            mSpeedBearingAccuracyRow!!.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        setStarted(PreferenceUtils.isTrackingStarted())
        setupUnitPreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.status_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.sort_sats) {
            showSortByDialog()
        } else if (id == R.id.filter_sats) {
            showFilterDialog()
        }
        return false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    fun onGpsStarted() {
        setStarted(true)
    }

    fun onGpsStopped() {
        setStarted(false)
    }

    @SuppressLint("NewApi")
    fun gpsStart() {
    }

    fun gpsStop() {}
    fun onLocationChanged(location: Location) {
        if (!UIUtils.isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return
        }

        // Cache location for copy to clipboard operation
        mLocation = location

        // Make sure TTFF is shown/set, if the TTFF is acquired before the mTTFFView/this fragment is initialized
        mTTFFView!!.text = mTtff
        if (mViewModel != null) {
            mViewModel!!.setGotFirstFix(true)
        }
        val coordinateFormat = Application.getPrefs().getString(
            getString(R.string.pref_key_coordinate_format),
            getString(R.string.preferences_coordinate_format_dd_key)
        )
        when (coordinateFormat) {
            "dd" -> {
                // Decimal degrees
                mLatitudeView!!.text =
                    getString(R.string.gps_latitude_value, location.latitude)
                mLongitudeView!!.text =
                    getString(R.string.gps_longitude_value, location.longitude)
            }
            "dms" -> {
                // Degrees minutes seconds
                mLatitudeView!!.text = UIUtils.getDMSFromLocation(
                    Application.get(),
                    location.latitude,
                    UIUtils.COORDINATE_LATITUDE
                )
                mLongitudeView!!.text = UIUtils.getDMSFromLocation(
                    Application.get(),
                    location.longitude,
                    UIUtils.COORDINATE_LONGITUDE
                )
            }
            "ddm" -> {
                // Degrees decimal minutes
                mLatitudeView!!.text = UIUtils.getDDMFromLocation(
                    Application.get(),
                    location.latitude,
                    UIUtils.COORDINATE_LATITUDE
                )
                mLongitudeView!!.text = UIUtils.getDDMFromLocation(
                    Application.get(),
                    location.longitude,
                    UIUtils.COORDINATE_LONGITUDE
                )
            }
            else -> {
                // Decimal degrees
                mLatitudeView!!.text =
                    getString(R.string.gps_latitude_value, location.latitude)
                mLongitudeView!!.text =
                    getString(R.string.gps_longitude_value, location.longitude)
            }
        }
        mFixTime = location.time
        if (location.hasAltitude()) {
            if (mPrefDistanceUnits.equals(METERS, ignoreCase = true)) {
                mAltitudeView!!.text =
                    getString(R.string.gps_altitude_value_meters, location.altitude)
            } else {
                // Feet
                mAltitudeView!!.text = getString(
                    R.string.gps_altitude_value_feet,
                    UIUtils.toFeet(location.altitude)
                )
            }
        } else {
            mAltitudeView!!.text = ""
        }
        if (location.hasSpeed()) {
            if (mPrefSpeedUnits.equals(METERS_PER_SECOND, ignoreCase = true)) {
                mSpeedView!!.text =
                    getString(R.string.gps_speed_value_meters_sec, location.speed)
            } else if (mPrefSpeedUnits.equals(KILOMETERS_PER_HOUR, ignoreCase = true)) {
                mSpeedView!!.text = resources.getString(
                    R.string.gps_speed_value_kilometers_hour,
                    UIUtils.toKilometersPerHour(location.speed)
                )
            } else {
                // Miles per hour
                mSpeedView!!.text = resources.getString(
                    R.string.gps_speed_value_miles_hour,
                    UIUtils.toMilesPerHour(location.speed)
                )
            }
        } else {
            mSpeedView!!.text = ""
        }
        if (location.hasBearing()) {
            mBearingView!!.text = getString(R.string.gps_bearing_value, location.bearing)
        } else {
            mBearingView!!.text = ""
        }
        updateLocationAccuracies(location)
        updateSpeedAndBearingAccuracies(location)
        updateFixTime()
    }

    fun onGnssFirstFix(ttffMillis: Int) {
        mTtff = UIUtils.getTtffString(ttffMillis)
        if (mTTFFView != null) {
            mTTFFView!!.text = mTtff
        }
        if (mViewModel != null) {
            mViewModel!!.setGotFirstFix(true)
        }
    }

    fun onGnssFixAcquired() {
        showHaveFix()
    }

    fun onGnssFixLost() {
        showLostFix()
    }

    fun onSatelliteStatusChanged(status: GnssStatus) {
        updateGnssStatus(status)
    }

    fun onGnssStarted() {
        setStarted(true)
    }

    fun onGnssStopped() {
        setStarted(false)
    }

    fun onNmeaMessage(message: String, timestamp: Long) {
        if (!isAdded) {
            // Do nothing if the Fragment isn't added
            return
        }
        if (message.startsWith("\$GPGGA") || message.startsWith("\$GNGNS") || message.startsWith("\$GNGGA")) {
            val altitudeMsl = NmeaUtils.getAltitudeMeanSeaLevel(message)
            if (altitudeMsl != null && mNavigating) {
                if (mPrefDistanceUnits.equals(METERS, ignoreCase = true)) {
                    mAltitudeMslView!!.text =
                        getString(R.string.gps_altitude_msl_value_meters, altitudeMsl)
                } else {
                    mAltitudeMslView!!.text =
                        getString(
                            R.string.gps_altitude_msl_value_feet,
                            UIUtils.toFeet(altitudeMsl)
                        )
                }
            }
        }
        if (message.startsWith("\$GNGSA") || message.startsWith("\$GPGSA")) {
            val dop = NmeaUtils.getDop(message)
            if (dop != null && mNavigating) {
                showDopViews()
                mPdopView!!.text = getString(R.string.pdop_value, dop.positionDop)
                mHvdopView!!.text = getString(
                    R.string.hvdop_value, dop.horizontalDop,
                    dop.verticalDop
                )
            }
        }
    }

    private fun showDopViews() {
        mPdopLabelView!!.visibility = View.VISIBLE
        mPdopView!!.visibility = View.VISIBLE
        mHvdopLabelView!!.visibility = View.VISIBLE
        mHvdopView!!.visibility = View.VISIBLE
    }

    private fun updateGnssStatus(status: GnssStatus) {
        setStarted(true)
        updateFixTime()
        if (!UIUtils.isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return
        }
        mSnrCn0Title = getString(R.string.gps_cn0_column_label)
        val length = status.satelliteCount
        svCount = 0
        svVisibleCount = 0
        mGnssStatus.clear()
        mSbasStatus.clear()
        mViewModel!!.reset()
        val filter = PreferenceUtils.getGnssFilter()
        while (svCount < length) {
            val satStatus = SatelliteStatus(
                status.getSvid(svCount),
                SatelliteUtils.getGnssConstellationType(status.getConstellationType(svCount)),
                status.getCn0DbHz(svCount),
                status.hasAlmanacData(svCount),
                status.hasEphemerisData(svCount),
                status.usedInFix(svCount),
                status.getElevationDegrees(svCount),
                status.getAzimuthDegrees(svCount)
            )
            if (SatelliteUtils.isGnssCarrierFrequenciesSupported()) {
                if (status.hasCarrierFrequencyHz(svCount)) {
                    satStatus.hasCarrierFrequency = true
                    satStatus.carrierFrequencyHz = status.getCarrierFrequencyHz(svCount)
                }
            }
            if (filter.isEmpty() || filter.contains(satStatus.gnssType)) {
                svVisibleCount++
                if (satStatus.gnssType == GnssType.SBAS) {
                    satStatus.sbasType = SatelliteUtils.getSbasConstellationType(satStatus.svid)
                    mSbasStatus.add(satStatus)
                } else {
                    mGnssStatus.add(satStatus)
                }
            }
            svCount++
        }
        mViewModel!!.setStatuses(mGnssStatus, mSbasStatus)
        refreshViews()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshViews() {
        sortLists()
        updateFilterView()
        updateListVisibility()
        mGnssAdapter!!.notifyDataSetChanged()
        mSbasAdapter!!.notifyDataSetChanged()
    }

    private fun sortLists() {
        when (PreferenceUtils.getSatSortOrderFromPreferences()) {
            0 -> {
                // Sort by Constellation
                mGnssStatus = sortByGnssThenId(mGnssStatus)
                mSbasStatus = sortBySbasThenId(mSbasStatus)
            }
            1 -> {
                // Sort by Carrier Frequency
                mGnssStatus = sortByCarrierFrequencyThenId(mGnssStatus)
                mSbasStatus = sortByCarrierFrequencyThenId(mSbasStatus)
            }
            2 -> {
                // Sort by Signal Strength
                mGnssStatus = sortByCn0(mGnssStatus)
                mSbasStatus = sortByCn0(mSbasStatus)
            }
            3 -> {
                // Sort by Used in Fix
                mGnssStatus = sortByUsedThenId(mGnssStatus)
                mSbasStatus = sortByUsedThenId(mSbasStatus)
            }
            4 -> {
                // Sort by Constellation, Carrier Frequency
                mGnssStatus = sortByGnssThenCarrierFrequencyThenId(mGnssStatus)
                mSbasStatus = sortBySbasThenCarrierFrequencyThenId(mSbasStatus)
            }
            5 -> {
                // Sort by Constellation, Signal Strength
                mGnssStatus = sortByGnssThenCn0ThenId(mGnssStatus)
                mSbasStatus = sortBySbasThenCn0ThenId(mSbasStatus)
            }
            6 -> {
                // Sort by Constellation, Used in Fix
                mGnssStatus = sortByGnssThenUsedThenId(mGnssStatus)
                mSbasStatus = sortBySbasThenUsedThenId(mSbasStatus)
            }
        }
    }

    private fun updateFilterView() {
        val c = context ?: return
        val filter = PreferenceUtils.getGnssFilter()
        if (PreferenceUtils.isTrackingStarted() || filter.isEmpty()) {
            filterGroup!!.visibility = View.GONE
            // Set num sats view back to normal
            mNumSats!!.setTypeface(null, Typeface.NORMAL)
        } else {
            // Show filter text
            filterGroup!!.visibility = View.VISIBLE
            filterTextView!!.text = c.getString(R.string.filter_text, svVisibleCount, svCount)
            // Set num sats view to italics to match filter text
            mNumSats!!.setTypeface(mNumSats!!.typeface, Typeface.ITALIC)
        }
    }

    private fun setupUnitPreferences() {
        val settings = Application.getPrefs()
        val app = Application.get()
        mPrefDistanceUnits = settings
            .getString(app.getString(R.string.pref_key_preferred_distance_units_v2), METERS)
        mPrefSpeedUnits = settings
            .getString(app.getString(R.string.pref_key_preferred_speed_units_v2), METERS_PER_SECOND)
    }

    /**
     * Sets the visibility of the lists
     */
    private fun updateListVisibility() {
        if (mGnssStatus.isNotEmpty()) {
            mGnssNotAvailableView!!.visibility = View.GONE
            mGnssStatusList!!.visibility = View.VISIBLE
        } else {
            mGnssNotAvailableView!!.visibility = View.VISIBLE
            mGnssStatusList!!.visibility = View.GONE
        }
        if (mSbasStatus.isNotEmpty()) {
            mSbasNotAvailableView!!.visibility = View.GONE
            mSbasStatusList!!.visibility = View.VISIBLE
        } else {
            mSbasNotAvailableView!!.visibility = View.VISIBLE
            mSbasStatusList!!.visibility = View.GONE
        }
    }

    private fun showSortByDialog() {
        val builder = AlertDialog.Builder(
            requireActivity()
        )
        builder.setTitle(R.string.menu_option_sort_by)
        val currentSatOrder = PreferenceUtils.getSatSortOrderFromPreferences()
        builder.setSingleChoiceItems(
            R.array.sort_sats, currentSatOrder
        ) { dialog: DialogInterface, index: Int ->
            setSortByClause(index)
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setOwnerActivity(requireActivity())
        dialog.show()
    }

    private fun showTimeErrorDialog(time: Long) {
        val format = SimpleDateFormat.getDateTimeInstance(
            java.text.DateFormat.LONG,
            java.text.DateFormat.LONG
        )
        val textView = layoutInflater.inflate(R.layout.error_text_dialog, null) as TextView
        textView.text =
            getString(R.string.error_time_message, format.format(time), NUM_DAYS_TIME_VALID)
        val builder = AlertDialog.Builder(
            requireActivity()
        )
        builder.setTitle(R.string.error_time_title)
        builder.setView(textView)
        val drawable =
            ContextCompat.getDrawable(Application.get(), android.R.drawable.ic_dialog_alert)
        DrawableCompat.setTint(drawable!!, ContextCompat.getColor(requireActivity(), R.color.colorPrimary))
        builder.setIcon(drawable)
        builder.setNeutralButton(
            R.string.main_help_close
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.setOwnerActivity(requireActivity())
        dialog.show()
    }

    /**
     * Saves the "sort by" order to preferences
     *
     * @param index the index of R.array.sort_sats that should be set
     */
    private fun setSortByClause(index: Int) {
        val sortOptions = Application.get().resources.getStringArray(R.array.sort_sats)
        PreferenceUtils.saveString(
            resources
                .getString(R.string.pref_key_default_sat_sort),
            sortOptions[index]
        )
    }

    private fun showFilterDialog() {
        val gnssTypes = GnssType.values()
        val len = gnssTypes.size
        val filter = PreferenceUtils.getGnssFilter()
        val items = arrayOfNulls<String>(len)
        val checks = BooleanArray(len)

        // For each GnssType, if it is in the enabled list, mark it as checked.
        for (i in 0 until len) {
            val gnssType = gnssTypes[i]
            items[i] = UIUtils.getGnssDisplayName(context, gnssType)
            if (filter.contains(gnssType)) {
                checks[i] = true
            }
        }

        // Arguments
        val args = Bundle()
        args.putStringArray(GnssFilterDialog.ITEMS, items)
        args.putBooleanArray(GnssFilterDialog.CHECKS, checks)
        val frag = GnssFilterDialog()
        frag.arguments = args
        frag.show(requireActivity().supportFragmentManager, ".GnssFilterDialog")
    }

    private inner class SatelliteStatusAdapter(var mConstellationType: ConstellationType) :
        RecyclerView.Adapter<SatelliteStatusAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val svId: TextView = v.findViewById(R.id.sv_id)
            val flagHeader: TextView = v.findViewById(R.id.gnss_flag_header)
            val flag: ImageView = v.findViewById(R.id.gnss_flag)
            val flagLayout: LinearLayout = v.findViewById(R.id.gnss_flag_layout)
            val carrierFrequency: TextView = v.findViewById(R.id.carrier_frequency)
            val signal: TextView = v.findViewById(R.id.signal)
            val elevation: TextView = v.findViewById(R.id.elevation)
            val azimuth: TextView = v.findViewById(R.id.azimuth)
            val statusFlags: TextView = v.findViewById(R.id.status_flags)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.status_row_item, viewGroup, false)
            return ViewHolder(v)
        }

        override fun getItemCount(): Int {
            // Add 1 for header row
            return if (mConstellationType == ConstellationType.GNSS) {
                mGnssStatus.size + 1
            } else {
                mSbasStatus.size + 1
            }
        }

        override fun onBindViewHolder(v: ViewHolder, position: Int) {
            if (position == 0) {
                // Show the header field for the GNSS flag and hide the ImageView
                v.flagHeader.visibility = View.VISIBLE
                v.flag.visibility = View.GONE
                v.flagLayout.visibility = View.GONE

                // Populate the header fields
                v.svId.text = getString(R.string.gps_prn_column_label)
                v.svId.setTypeface(v.svId.typeface, Typeface.BOLD)
                if (mConstellationType == ConstellationType.GNSS) {
                    v.flagHeader.text = getString(R.string.gnss_flag_image_label)
                } else {
                    v.flagHeader.text = getString(R.string.sbas_flag_image_label)
                }
                if (SatelliteUtils.isGnssCarrierFrequenciesSupported()) {
                    v.carrierFrequency.visibility = View.VISIBLE
                    v.carrierFrequency.text = getString(R.string.gps_carrier_column_label)
                    v.carrierFrequency.setTypeface(v.carrierFrequency.typeface, Typeface.BOLD)
                } else {
                    v.carrierFrequency.visibility = View.GONE
                }
                v.signal.text = mSnrCn0Title
                v.signal.setTypeface(v.signal.typeface, Typeface.BOLD)
                v.elevation.text = resources!!.getString(R.string.gps_elevation_column_label)
                v.elevation.setTypeface(v.elevation.typeface, Typeface.BOLD)
                v.azimuth.text = resources!!.getString(R.string.gps_azimuth_column_label)
                v.azimuth.setTypeface(v.azimuth.typeface, Typeface.BOLD)
                v.statusFlags.text = resources!!.getString(R.string.gps_flags_column_label)
                v.statusFlags.setTypeface(v.statusFlags.typeface, Typeface.BOLD)
            } else {
                // There is a header at 0, so the first data row will be at position - 1, etc.
                val dataRow = position - 1
                val sats: List<SatelliteStatus> = if (mConstellationType == ConstellationType.GNSS) {
                    mGnssStatus
                } else {
                    mSbasStatus
                }

                // Show the row field for the GNSS flag mImage and hide the header
                v.flagHeader.visibility = View.GONE
                v.flag.visibility = View.VISIBLE
                v.flagLayout.visibility = View.VISIBLE

                // Populate status data for this row
                v.svId.text = Integer.toString(sats[dataRow].svid)
                v.flag.scaleType = ImageView.ScaleType.FIT_START
                when (sats[dataRow].gnssType) {
                    GnssType.NAVSTAR -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(mFlagUsa)
                        v.flag.contentDescription =
                            getString(R.string.gps_content_description)
                    }
                    GnssType.GLONASS -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(mFlagRussia)
                        v.flag.contentDescription =
                            getString(R.string.glonass_content_description)
                    }
                    GnssType.QZSS -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(mFlagJapan)
                        v.flag.contentDescription =
                            getString(R.string.qzss_content_description)
                    }
                    GnssType.BEIDOU -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(mFlagChina)
                        v.flag.contentDescription =
                            getString(R.string.beidou_content_description)
                    }
                    GnssType.GALILEO -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(mFlagEU)
                        v.flag.contentDescription =
                            getString(R.string.galileo_content_description)
                    }
                    GnssType.IRNSS -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(mFlagIndia)
                        v.flag.contentDescription =
                            getString(R.string.irnss_content_description)
                    }
                    GnssType.SBAS -> setSbasFlag(sats[dataRow], v.flag)
                    GnssType.UNKNOWN -> {
                        v.flag.visibility = View.INVISIBLE
                        v.flag.contentDescription = getString(R.string.unknown)
                    }
                }
                if (SatelliteUtils.isGnssCarrierFrequenciesSupported()) {
                    if (sats[dataRow].hasCarrierFrequency) {
                        val carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(sats[dataRow])
                        if (carrierLabel != CarrierFreqUtils.CF_UNKNOWN) {
                            // Make sure it's the normal text size (in case it's previously been
                            // resized to show raw number).  Use another TextView for default text size.
                            v.carrierFrequency.setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                v.svId.textSize
                            )
                            // Show label such as "L1"
                            v.carrierFrequency.text = carrierLabel
                        } else {
                            // Shrink the size so we can show raw number
                            v.carrierFrequency.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
                            // Show raw number for carrier frequency - Convert Hz to MHz
                            val carrierMhz = MathUtils.toMhz(sats[dataRow].carrierFrequencyHz)
                            v.carrierFrequency.text = String.format("%.3f", carrierMhz)
                        }
                    } else {
                        v.carrierFrequency.text = ""
                    }
                } else {
                    v.carrierFrequency.visibility = View.GONE
                }
                if (sats[dataRow].cn0DbHz != SatelliteStatus.NO_DATA) {
                    v.signal.text = String.format("%.1f", sats[dataRow].cn0DbHz)
                } else {
                    v.signal.text = ""
                }
                if (sats[dataRow].elevationDegrees != SatelliteStatus.NO_DATA) {
                    v.elevation.text = getString(
                        R.string.gps_elevation_column_value,
                        sats[dataRow].elevationDegrees
                    ).replace(".0", "").replace(",0", "")
                } else {
                    v.elevation.text = ""
                }
                if (sats[dataRow].azimuthDegrees != SatelliteStatus.NO_DATA) {
                    v.azimuth.text = getString(
                        R.string.gps_azimuth_column_value,
                        sats[dataRow].azimuthDegrees
                    ).replace(".0", "").replace(",0", "")
                } else {
                    v.azimuth.text = ""
                }
                val flags = CharArray(3)
                flags[0] = if (!sats[dataRow].hasAlmanac) ' ' else 'A'
                flags[1] = if (!sats[dataRow].hasEphemeris) ' ' else 'E'
                flags[2] = if (!sats[dataRow].usedInFix) ' ' else 'U'
                v.statusFlags.text = String(flags)
            }
        }

        private fun setSbasFlag(status: SatelliteStatus, flag: ImageView) {
            when (status.sbasType) {
                SbasType.WAAS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagUsa)
                    flag.contentDescription =
                        getString(R.string.waas_content_description)
                }
                SbasType.EGNOS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagEU)
                    flag.contentDescription =
                        getString(R.string.egnos_content_description)
                }
                SbasType.GAGAN -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagIndia)
                    flag.contentDescription =
                        getString(R.string.gagan_content_description)
                }
                SbasType.MSAS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagJapan)
                    flag.contentDescription =
                        getString(R.string.msas_content_description)
                }
                SbasType.SDCM -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagRussia)
                    flag.contentDescription =
                        getString(R.string.sdcm_content_description)
                }
                SbasType.SNAS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagChina)
                    flag.contentDescription =
                        getString(R.string.snas_content_description)
                }
                SbasType.SACCSA -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(mFlagICAO)
                    flag.contentDescription =
                        getString(R.string.saccsa_content_description)
                }
                SbasType.UNKNOWN -> {
                    flag.visibility = View.INVISIBLE
                    flag.contentDescription = getString(R.string.unknown)
                }
                else -> {
                    flag.visibility = View.INVISIBLE
                    flag.contentDescription = getString(R.string.unknown)
                }
            }
        }
    }

    private fun showHaveFix() {
        if (lock != null) {
            UIUtils.showViewWithAnimation(lock, UIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun showLostFix() {
        if (lock != null) {
            UIUtils.hideViewWithAnimation(lock, UIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    companion object {
        const val TAG = "GpsStatusFragment"
        private const val EMPTY_LAT_LONG = "             "
        private val METERS =
            Application.get().resources.getStringArray(R.array.preferred_distance_units_values)[0]
        private val METERS_PER_SECOND =
            Application.get().resources.getStringArray(R.array.preferred_speed_units_values)[0]
        private val KILOMETERS_PER_HOUR =
            Application.get().resources.getStringArray(R.array.preferred_speed_units_values)[1]
    }
}