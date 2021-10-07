/*
 * Copyright (C) 2008-2021 The Android Open Source Project,
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
package com.android.gpstest.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.data.FirstFixState
import com.android.gpstest.data.FixState
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.data.toSatelliteStatus
import com.android.gpstest.databinding.GpsStatusBinding
import com.android.gpstest.model.*
import com.android.gpstest.util.*
import com.android.gpstest.util.DateTimeUtils.Companion.NUM_DAYS_TIME_VALID
import com.android.gpstest.util.DateTimeUtils.Companion.isTimeValid
import com.android.gpstest.util.SharedPreferenceUtil.KILOMETERS_PER_HOUR
import com.android.gpstest.util.SharedPreferenceUtil.METERS
import com.android.gpstest.util.SharedPreferenceUtil.METERS_PER_SECOND
import com.android.gpstest.util.SharedPreferenceUtil.distanceUnits
import com.android.gpstest.util.SharedPreferenceUtil.speedUnits
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class StatusFragment : Fragment() {
    @SuppressLint("SimpleDateFormat")
    var timeFormat // See #117
            = SimpleDateFormat(
        if (DateFormat.is24HourFormat(Application.app.applicationContext)) SDF_TIME_24_HOUR else SDF_TIME_12_HOUR
    )
    @SuppressLint("SimpleDateFormat")
    var timeAndDateFormat = SimpleDateFormat(
        if (DateFormat.is24HourFormat(Application.app.applicationContext)) SDF_DATE_24_HOUR else SDF_DATE_12_HOUR
    )

    private var location: Location? = null
    private var gnssAdapter: SatelliteStatusAdapter? = null
    private var sbasAdapter: SatelliteStatusAdapter? = null
    private var gnssStatus: MutableList<SatelliteStatus> = ArrayList()
    private var sbasStatus: MutableList<SatelliteStatus> = ArrayList()
    private var svCount = 0
    private var svShownCount = 0
    private var fixTime: Long = 0
    private var started = false
    private var flagUsa: Drawable? = null
    private var flagRussia: Drawable? = null
    private var flagJapan: Drawable? = null
    private var flagChina: Drawable? = null
    private var flagIndia: Drawable? = null
    private var flagEU: Drawable? = null
    private var flagICAO: Drawable? = null
    private var ttff = ""
    private var viewModel: DeviceInfoViewModel? = null

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null
    private var gnssFlow: Job? = null
    private var nmeaFlow: Job? = null

    // Preference listener that will cancel the above flows when the user turns off tracking via UI
    private val trackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferenceUtil.newStopTrackingListener { setStarted(false) }

    // Observers of view model
    private val satelliteMetadataObserver: Observer<SatelliteMetadata> =
        Observer { satelliteMetadata ->
            if (satelliteMetadata != null) {
                binding.numSats.text = resources.getString(
                    R.string.gps_num_sats_value,
                    satelliteMetadata.numSatsUsed,
                    satelliteMetadata.numSatsInView,
                    satelliteMetadata.numSatsTotal
                )
            }
        }

    private var _binding: GpsStatusBinding? = null
    private val binding get() = _binding!!

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GpsStatusBinding.inflate(inflater, container, false)
        val view = binding.root

        setupUnitPreferences()

        observeLocationUpdateStates()
        observeGnssStates()

        Application.prefs.registerOnSharedPreferenceChangeListener(trackingListener)

        binding.latitude.text = EMPTY_LAT_LONG
        binding.longitude.text = EMPTY_LAT_LONG
        binding.fixTimeError.setOnClickListener {
            showTimeErrorDialog(fixTime)
        }

        flagUsa = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_usa)
        flagRussia = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_russia)
        flagJapan = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_japan)
        flagChina = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_china)
        flagIndia = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_india)
        flagEU = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_european_union)
        flagICAO = ContextCompat.getDrawable(Application.app, R.drawable.ic_flag_icao)

        val detector = GestureDetectorCompat(context, object : SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val location = location
                // Copy location to clipboard
                if (location != null) {
                    val includeAltitude = Application.prefs.getBoolean(
                        Application.app.getString(R.string.pref_key_share_include_altitude), false
                    )
                    val coordinateFormat = Application.prefs.getString(
                        Application.app.getString(R.string.pref_key_coordinate_format),
                        Application.app.getString(R.string.preferences_coordinate_format_dd_key)
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
        binding.statusLocationScrollview.setOnTouchListener { view: View?, motionEvent: MotionEvent? ->
            detector.onTouchEvent(motionEvent)
            false
        }

        // Remove any previous clickable spans to avoid issues with recycling views
        UIUtils.removeAllClickableSpans(binding.filterShowAll)
        // Save an empty set to preferences to show all satellites
        val filterShowAllClick: ClickableSpan = object : ClickableSpan() {
            override fun onClick(v: View) {
                // Save an empty set to preferences to show all satellites
                PreferenceUtils.saveGnssFilter(LinkedHashSet())
            }
        }
        UIUtils.setClickableSpan(binding.filterShowAll, filterShowAllClick)

        // GNSS
        val llmGnss = LinearLayoutManager(context)
        llmGnss.isAutoMeasureEnabled = true
        llmGnss.orientation = RecyclerView.VERTICAL
        gnssAdapter = SatelliteStatusAdapter(ConstellationType.GNSS)
        binding.gnssStatusList.adapter = gnssAdapter
        binding.gnssStatusList.isFocusable = false
        binding.gnssStatusList.isFocusableInTouchMode = false
        binding.gnssStatusList.layoutManager = llmGnss
        binding.gnssStatusList.isNestedScrollingEnabled = false

        // SBAS
        val llmSbas = LinearLayoutManager(context)
        llmSbas.isAutoMeasureEnabled = true
        llmSbas.orientation = RecyclerView.VERTICAL
        sbasAdapter = SatelliteStatusAdapter(ConstellationType.SBAS)
        binding.sbasStatusList.adapter = sbasAdapter
        binding.sbasStatusList.isFocusable = false
        binding.sbasStatusList.isFocusableInTouchMode = false
        binding.sbasStatusList.layoutManager = llmSbas
        binding.sbasStatusList.isNestedScrollingEnabled = false
        viewModel = ViewModelProviders.of(requireActivity()).get(
            DeviceInfoViewModel::class.java
        )
        viewModel!!.satelliteMetadata.observe(requireActivity(), satelliteMetadataObserver)

        return view
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Status location: ${it.toNotificationTitle()}")
                onLocationChanged(it)
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates() {
        repository.receivingLocationUpdates
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    true -> onGnssStarted()
                    false -> onGnssStopped()
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeGnssFlow() {
        if (gnssFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        gnssFlow = repository.getGnssStatus()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .map { it.toSatelliteStatus() }
            .onEach {
                //Log.d(TAG, "Status gnssStatus: ${it}")
                updateStatus(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun observeGnssStates() {
        repository.firstFixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FirstFixState.Acquired -> {
                        // FIXME - if the service is running but the activity is destroyed and then created
                        // we never see TTFF in the Status fragment.
                        onGnssFirstFix(it.ttffMillis)
                        onGnssFixAcquired()
                    }
                    is FirstFixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted()) onGnssFixLost()
                }
            }
            .launchIn(lifecycleScope)
        repository.fixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FixState.Acquired -> onGnssFixAcquired()
                    is FixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted()) onGnssFixLost()
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeNmeaFlow() {
        if (nmeaFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        nmeaFlow = repository.getNmea()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Status NMEA: ${it}")
                onNmeaMessage(it.message, it.timestamp)
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("NotifyDataSetChanged")
    private fun setStarted(started: Boolean) {
        if (started == this.started) {
            // State hasn't changed - no op and return
            return
        }
        if (started) {
            // Activity or service is observing updates, so observe here too
            observeLocationFlow()
            observeGnssFlow()
            observeNmeaFlow()
        } else {
            // Cancel updates (Note that these are canceled via trackingListener preference listener
            // in the case where updates are stopped from the Activity UI switch)
            cancelFlows()

            // Reset views
            viewModel!!.reset()
            binding.latitude.text = EMPTY_LAT_LONG
            binding.longitude.text = EMPTY_LAT_LONG
            fixTime = 0
            ttff = ""
            updateFixTime()
            updateFilterView()
            binding.ttff.text = ""
            binding.altitude.text = ""
            binding.altitudeMsl.text = ""
            binding.horVertAccuracy.text = ""
            binding.speed.text = ""
            binding.speedAcc.text = ""
            binding.bearing.text = ""
            binding.bearingAcc.text = ""
            binding.numSats.text = ""
            binding.pdop.text = ""
            binding.hvdop.text = ""
            binding.statusLock.visibility = View.GONE
            svCount = 0
            svShownCount = 0
            gnssStatus.clear()
            sbasStatus.clear()
            gnssAdapter!!.notifyDataSetChanged()
            sbasAdapter!!.notifyDataSetChanged()
        }
        this.started = started
    }

    private fun cancelFlows() {
        locationFlow?.cancel()
        gnssFlow?.cancel()
        nmeaFlow?.cancel()
    }

    private fun updateFixTime() {
        if (fixTime == 0L || !PreferenceUtils.isTrackingStarted()) {
            binding.fixTime.text = ""
            binding.fixTimeError.text = ""
            binding.fixTimeError.visibility = View.GONE
        } else {
            if (isTimeValid(fixTime)) {
                binding.fixTimeError.visibility = View.GONE
                binding.fixTime.visibility = View.VISIBLE
                binding.fixTime.text = formatFixTimeDate(fixTime)
            } else {
                // Error in fix time
                binding.fixTimeError.visibility = View.VISIBLE
                binding.fixTimeError.text = formatFixTimeDate(fixTime)
                binding.fixTime.visibility = View.GONE
            }
        }
    }

    /**
     * Returns a formatted version of the provided fixTime based on the width of the current display
     * @return a formatted version of the provided fixTime based on the width of the current display
     */
    private fun formatFixTimeDate(fixTime: Long): String {
        return if (UIUtils.isWideEnoughForDate(context)) timeAndDateFormat.format(fixTime)
        else timeFormat.format(fixTime)
    }

    /**
     * Update views for speed and bearing location accuracies based on the provided location
     * @param location
     */
    private fun updateSpeedAndBearingAccuracies(location: Location) {
        if (SatelliteUtils.isSpeedAndBearingAccuracySupported()) {
            binding.speedBearingAccRow.visibility = View.VISIBLE
            if (location.hasSpeedAccuracy()) {
                when {
                    speedUnits().equals(METERS_PER_SECOND, ignoreCase = true) -> {
                        binding.speedAcc.text = resources.getString(
                            R.string.gps_speed_acc_value_meters_sec,
                            location.speedAccuracyMetersPerSecond
                        )
                    }
                    speedUnits().equals(KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                        binding.speedAcc.text = resources.getString(
                            R.string.gps_speed_acc_value_km_hour,
                            UIUtils.toKilometersPerHour(location.speedAccuracyMetersPerSecond)
                        )
                    }
                    else -> {
                        // Miles per hour
                        binding.speedAcc.text = resources.getString(
                            R.string.gps_speed_acc_value_miles_hour,
                            UIUtils.toMilesPerHour(location.speedAccuracyMetersPerSecond)
                        )
                    }
                }
            } else {
                binding.speedAcc.text = ""
            }
            if (location.hasBearingAccuracy()) {
                binding.bearingAcc.text = resources.getString(
                    R.string.gps_bearing_acc_value,
                    location.bearingAccuracyDegrees
                )
            } else {
                binding.bearingAcc.text = ""
            }
        } else {
            binding.speedBearingAccRow.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
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

    private fun onLocationChanged(location: Location) {
        if (!UIUtils.isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return
        }

        // Cache location for copy to clipboard operation
        this.location = location

        // Make sure TTFF is shown/set, if the TTFF is acquired before the mTTFFView/this fragment is initialized
        binding.ttff.text = ttff
        if (viewModel != null) {
            viewModel!!.setGotFirstFix(true)
        }
        val coordinateFormat = Application.prefs.getString(
            getString(R.string.pref_key_coordinate_format),
            getString(R.string.preferences_coordinate_format_dd_key)
        )
        when (coordinateFormat) {
            "dd" -> {
                // Decimal degrees
                binding.latitude.text =
                    getString(R.string.gps_latitude_value, location.latitude)
                binding.longitude.text =
                    getString(R.string.gps_longitude_value, location.longitude)
            }
            "dms" -> {
                // Degrees minutes seconds
                binding.latitude.text = UIUtils.getDMSFromLocation(
                    Application.app,
                    location.latitude,
                    UIUtils.COORDINATE_LATITUDE
                )
                binding.longitude.text = UIUtils.getDMSFromLocation(
                    Application.app,
                    location.longitude,
                    UIUtils.COORDINATE_LONGITUDE
                )
            }
            "ddm" -> {
                // Degrees decimal minutes
                binding.latitude.text = UIUtils.getDDMFromLocation(
                    Application.app,
                    location.latitude,
                    UIUtils.COORDINATE_LATITUDE
                )
                binding.longitude.text = UIUtils.getDDMFromLocation(
                    Application.app,
                    location.longitude,
                    UIUtils.COORDINATE_LONGITUDE
                )
            }
            else -> {
                // Decimal degrees
                binding.latitude.text =
                    getString(R.string.gps_latitude_value, location.latitude)
                binding.longitude.text =
                    getString(R.string.gps_longitude_value, location.longitude)
            }
        }
        fixTime = location.time
        if (location.hasAltitude()) {
            when {
                distanceUnits().equals(METERS, ignoreCase = true) -> {
                    binding.altitude.text =
                        getString(R.string.gps_altitude_value_meters, location.altitude)
                }
                else -> {
                    // Feet
                    binding.altitude.text = getString(
                        R.string.gps_altitude_value_feet,
                        UIUtils.toFeet(location.altitude)
                    )
                }
            }
        } else {
            binding.altitude.text = ""
        }
        if (location.hasSpeed()) {
            when {
                speedUnits().equals(METERS_PER_SECOND, ignoreCase = true) -> {
                    binding.speed.text =
                        getString(R.string.gps_speed_value_meters_sec, location.speed)
                }
                speedUnits().equals(KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    binding.speed.text = resources.getString(
                        R.string.gps_speed_value_kilometers_hour,
                        UIUtils.toKilometersPerHour(location.speed)
                    )
                }
                else -> {
                    // Miles per hour
                    binding.speed.text = resources.getString(
                        R.string.gps_speed_value_miles_hour,
                        UIUtils.toMilesPerHour(location.speed)
                    )
                }
            }
        } else {
            binding.speed.text = ""
        }
        if (location.hasBearing()) {
            binding.bearing.text = getString(R.string.gps_bearing_value, location.bearing)
        } else {
            binding.bearing.text = ""
        }
        updateSpeedAndBearingAccuracies(location)
        updateFixTime()
    }

    private fun onGnssFirstFix(ttffMillis: Int) {
        ttff = UIUtils.getTtffString(ttffMillis)
        binding.ttff.text = ttff
        if (viewModel != null) {
            viewModel!!.setGotFirstFix(true)
        }
    }

    private fun onGnssFixAcquired() {
        showHaveFix()
    }

    private fun onGnssFixLost() {
        showLostFix()
    }

    @ExperimentalCoroutinesApi
    fun onGnssStarted() {
        setStarted(true)
    }

    @ExperimentalCoroutinesApi
    private fun onGnssStopped() {
        setStarted(false)
    }

    private fun onNmeaMessage(message: String, timestamp: Long) {
        if (!isAdded) {
            // Do nothing if the Fragment isn't added
            return
        }
        if (message.startsWith("\$GPGGA") || message.startsWith("\$GNGNS") || message.startsWith("\$GNGGA")) {
            val altitudeMsl = NmeaUtils.getAltitudeMeanSeaLevel(message)
            if (altitudeMsl != null && started) {
                if (distanceUnits().equals(METERS, ignoreCase = true)) {
                    binding.altitudeMsl.text =
                        getString(R.string.gps_altitude_msl_value_meters, altitudeMsl)
                } else {
                    binding.altitudeMsl.text =
                        getString(
                            R.string.gps_altitude_msl_value_feet,
                            UIUtils.toFeet(altitudeMsl)
                        )
                }
            }
        }
        if (message.startsWith("\$GNGSA") || message.startsWith("\$GPGSA")) {
            val dop = NmeaUtils.getDop(message)
            if (dop != null && started) {
                showDopViews()
                binding.pdop.text = getString(R.string.pdop_value, dop.positionDop)
                binding.hvdop.text = getString(
                    R.string.hvdop_value, dop.horizontalDop,
                    dop.verticalDop
                )
            }
        }
    }

    private fun showDopViews() {
        binding.pdopLabel.visibility = View.VISIBLE
        binding.pdop.visibility = View.VISIBLE
        binding.hvdopLabel.visibility = View.VISIBLE
        binding.hvdop.visibility = View.VISIBLE
    }

    @ExperimentalCoroutinesApi
    private fun updateStatus(status: List<SatelliteStatus>) {
        updateFixTime()
        if (!UIUtils.isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return
        }
        gnssStatus.clear()
        sbasStatus.clear()
        viewModel!!.reset()

        svCount = status.size
        // Count number of sats shown to user
        val filter = PreferenceUtils.getGnssFilter()

        // FIXME - this is actually counting number of signals shown, not number of satellites shown
        svShownCount = status.count { filter.isEmpty() || filter.contains(it.gnssType) }

        // Split list into GNSS and SBAS statuses, apply shown filter, and update view model
        // FIXME - use .partition when moving to ViewModel - val (gnssStatus, sbasStatus) = status.partition { it.gnssType != GnssType.SBAS }
        gnssStatus = status.filter {
            it.gnssType != GnssType.SBAS &&
                    (filter.isEmpty() || filter.contains(it.gnssType))
        } as MutableList<SatelliteStatus>
        sbasStatus = status.filter {
            it.gnssType == GnssType.SBAS &&
                    (filter.isEmpty() || filter.contains(it.gnssType))
        } as MutableList<SatelliteStatus>
        viewModel!!.setStatuses(gnssStatus, sbasStatus)
        refreshViews()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshViews() {
        sortLists()
        updateFilterView()
        updateListVisibility()
        gnssAdapter!!.notifyDataSetChanged()
        sbasAdapter!!.notifyDataSetChanged()
    }

    private fun sortLists() {
        when (PreferenceUtils.getSatSortOrderFromPreferences()) {
            0 -> {
                // Sort by Constellation
                gnssStatus = sortByGnssThenId(gnssStatus)
                sbasStatus = sortBySbasThenId(sbasStatus)
            }
            1 -> {
                // Sort by Carrier Frequency
                gnssStatus = sortByCarrierFrequencyThenId(gnssStatus)
                sbasStatus = sortByCarrierFrequencyThenId(sbasStatus)
            }
            2 -> {
                // Sort by Signal Strength
                gnssStatus = sortByCn0(gnssStatus)
                sbasStatus = sortByCn0(sbasStatus)
            }
            3 -> {
                // Sort by Used in Fix
                gnssStatus = sortByUsedThenId(gnssStatus)
                sbasStatus = sortByUsedThenId(sbasStatus)
            }
            4 -> {
                // Sort by Constellation, Carrier Frequency
                gnssStatus = sortByGnssThenCarrierFrequencyThenId(gnssStatus)
                sbasStatus = sortBySbasThenCarrierFrequencyThenId(sbasStatus)
            }
            5 -> {
                // Sort by Constellation, Signal Strength
                gnssStatus = sortByGnssThenCn0ThenId(gnssStatus)
                sbasStatus = sortBySbasThenCn0ThenId(sbasStatus)
            }
            6 -> {
                // Sort by Constellation, Used in Fix
                gnssStatus = sortByGnssThenUsedThenId(gnssStatus)
                sbasStatus = sortBySbasThenUsedThenId(sbasStatus)
            }
        }
    }

    private fun updateFilterView() {
        val c = context ?: return
        val filter = PreferenceUtils.getGnssFilter()
        if (!PreferenceUtils.isTrackingStarted() || filter.isEmpty()) {
            binding.statusFilterGroup.visibility = View.GONE
            // Set num sats view back to normal
            binding.numSats.setTypeface(null, Typeface.NORMAL)
        } else {
            // Show filter text
            binding.statusFilterGroup.visibility = View.VISIBLE
            binding.filterText.text = c.getString(R.string.filter_text, svShownCount, svCount)
            // Set num sats view to italics to match filter text
            binding.numSats.setTypeface(binding.numSats.typeface, Typeface.ITALIC)
        }
    }

    private fun setupUnitPreferences() {
        val settings = Application.prefs
        val app = Application.app
    }

    /**
     * Sets the visibility of the lists
     */
    private fun updateListVisibility() {
        if (gnssStatus.isNotEmpty()) {
            binding.gnssNotAvailable.visibility = View.GONE
            binding.gnssStatusList.visibility = View.VISIBLE
        } else {
            binding.gnssNotAvailable.visibility = View.VISIBLE
            binding.gnssStatusList.visibility = View.GONE
        }
        if (sbasStatus.isNotEmpty()) {
            binding.sbasNotAvailable.visibility = View.GONE
            binding.sbasStatusList.visibility = View.VISIBLE
        } else {
            binding.sbasNotAvailable.visibility = View.VISIBLE
            binding.sbasStatusList.visibility = View.GONE
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
            ContextCompat.getDrawable(Application.app, android.R.drawable.ic_dialog_alert)
        DrawableCompat.setTint(
            drawable!!,
            ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
        )
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
        val sortOptions = Application.app.resources.getStringArray(R.array.sort_sats)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                gnssStatus.size + 1
            } else {
                sbasStatus.size + 1
            }
        }

        override fun onBindViewHolder(v: ViewHolder, position: Int) {
            if (position == 0) {
                // Show the header field for the GNSS flag and hide the ImageView
                v.flagHeader.visibility = View.VISIBLE
                v.flag.visibility = View.GONE
                v.flagLayout.visibility = View.GONE

                // Populate the header fields
                v.svId.text = getString(R.string.id_column_label)
                v.svId.setTypeface(v.svId.typeface, Typeface.BOLD)
                if (mConstellationType == ConstellationType.GNSS) {
                    v.flagHeader.text = getString(R.string.gnss_flag_image_label)
                } else {
                    v.flagHeader.text = getString(R.string.sbas_flag_image_label)
                }
                if (SatelliteUtils.isCfSupported()) {
                    v.carrierFrequency.visibility = View.VISIBLE
                    v.carrierFrequency.text = getString(R.string.cf_column_label)
                    v.carrierFrequency.setTypeface(v.carrierFrequency.typeface, Typeface.BOLD)
                } else {
                    v.carrierFrequency.visibility = View.GONE
                }
                v.signal.text = getString(R.string.gps_cn0_column_label)
                v.signal.setTypeface(v.signal.typeface, Typeface.BOLD)
                v.elevation.text = resources.getString(R.string.elevation_column_label)
                v.elevation.setTypeface(v.elevation.typeface, Typeface.BOLD)
                v.azimuth.text = resources.getString(R.string.azimuth_column_label)
                v.azimuth.setTypeface(v.azimuth.typeface, Typeface.BOLD)
                v.statusFlags.text = resources.getString(R.string.flags_aeu_column_label)
                v.statusFlags.setTypeface(v.statusFlags.typeface, Typeface.BOLD)
            } else {
                // There is a header at 0, so the first data row will be at position - 1, etc.
                val dataRow = position - 1
                val sats: List<SatelliteStatus> =
                    if (mConstellationType == ConstellationType.GNSS) {
                        gnssStatus
                    } else {
                        sbasStatus
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
                        v.flag.setImageDrawable(flagUsa)
                        v.flag.contentDescription =
                            getString(R.string.gps_content_description)
                    }
                    GnssType.GLONASS -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(flagRussia)
                        v.flag.contentDescription =
                            getString(R.string.glonass_content_description)
                    }
                    GnssType.QZSS -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(flagJapan)
                        v.flag.contentDescription =
                            getString(R.string.qzss_content_description)
                    }
                    GnssType.BEIDOU -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(flagChina)
                        v.flag.contentDescription =
                            getString(R.string.beidou_content_description)
                    }
                    GnssType.GALILEO -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(flagEU)
                        v.flag.contentDescription =
                            getString(R.string.galileo_content_description)
                    }
                    GnssType.IRNSS -> {
                        v.flag.visibility = View.VISIBLE
                        v.flag.setImageDrawable(flagIndia)
                        v.flag.contentDescription =
                            getString(R.string.irnss_content_description)
                    }
                    GnssType.SBAS -> setSbasFlag(sats[dataRow], v.flag)
                    GnssType.UNKNOWN -> {
                        v.flag.visibility = View.INVISIBLE
                        v.flag.contentDescription = getString(R.string.unknown)
                    }
                }
                if (SatelliteUtils.isCfSupported()) {
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
                    flag.setImageDrawable(flagUsa)
                    flag.contentDescription =
                        getString(R.string.waas_content_description)
                }
                SbasType.EGNOS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(flagEU)
                    flag.contentDescription =
                        getString(R.string.egnos_content_description)
                }
                SbasType.GAGAN -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(flagIndia)
                    flag.contentDescription =
                        getString(R.string.gagan_content_description)
                }
                SbasType.MSAS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(flagJapan)
                    flag.contentDescription =
                        getString(R.string.msas_content_description)
                }
                SbasType.SDCM -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(flagRussia)
                    flag.contentDescription =
                        getString(R.string.sdcm_content_description)
                }
                SbasType.SNAS -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(flagChina)
                    flag.contentDescription =
                        getString(R.string.snas_content_description)
                }
                SbasType.SACCSA -> {
                    flag.visibility = View.VISIBLE
                    flag.setImageDrawable(flagICAO)
                    flag.contentDescription =
                        getString(R.string.saccsa_content_description)
                }
                SbasType.UNKNOWN -> {
                    flag.visibility = View.INVISIBLE
                    flag.contentDescription = getString(R.string.unknown)
                }
            }
        }
    }

    private fun showHaveFix() {
        UIUtils.showViewWithAnimation(binding.statusLock, UIUtils.ANIMATION_DURATION_SHORT_MS)
    }

    private fun showLostFix() {
        UIUtils.hideViewWithAnimation(binding.statusLock, UIUtils.ANIMATION_DURATION_SHORT_MS)
    }

    companion object {
        private const val TAG = "GpsStatusFragment"
        private const val EMPTY_LAT_LONG = "             "

        // SimpleDateFormat can only do 3 digits of fractional seconds (.SSS)
        private const val SDF_TIME_24_HOUR = "HH:mm:ss.SSS"
        private const val SDF_TIME_12_HOUR = "hh:mm:ss.SSS a"
        private const val SDF_DATE_24_HOUR = "HH:mm:ss.SSS MMM d, yyyy z"
        private const val SDF_DATE_12_HOUR = "hh:mm:ss.SSS a MMM d, yyyy z"
    }
}