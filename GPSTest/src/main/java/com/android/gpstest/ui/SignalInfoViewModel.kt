/*
 * Copyright (C) 2019 Sean J. Barbeau
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
import android.app.Application
import android.content.SharedPreferences
import android.location.GnssMeasurement
import android.location.Location
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.gpstest.data.FirstFixState
import com.android.gpstest.data.FixState
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.data.PreferencesRepository
import com.android.gpstest.model.*
import com.android.gpstest.util.CarrierFreqUtils.getCarrierFrequencyLabel
import com.android.gpstest.util.FormatUtils.formatTtff
import com.android.gpstest.util.GeocodeUtils.geocode
import com.android.gpstest.util.NmeaUtils
import com.android.gpstest.util.PreferenceUtil
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.SatelliteUtil.accumulatedDeltaRangeStateString
import com.android.gpstest.util.SatelliteUtil.toSatelliteGroup
import com.android.gpstest.util.SatelliteUtil.toSatelliteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * View model that holds GNSS signal information
 */
@ExperimentalCoroutinesApi
@HiltViewModel
class SignalInfoViewModel @Inject constructor(
    application: Application,
    private val locationRepo: LocationRepository,
    private val prefsRepo: PreferencesRepository
) : AndroidViewModel(application) {
    //
    // Flows from the repository
    //
    private var locationFlow: Job? = null
    private var gnssFlow: Job? = null
    private var nmeaFlow: Job? = null
    private var measurementFlow: Job? = null

    //
    // LiveData observed by Composables
    //

    // Preferences
    private val _prefs = MutableLiveData<AppPreferences>()
    val prefs: LiveData<AppPreferences> = _prefs

    // All statuses BEFORE filtering
    private val _allStatuses = MutableLiveData<List<SatelliteStatus>>()
    val allStatuses: LiveData<List<SatelliteStatus>> = _allStatuses

    // Statuses AFTER filtering
    private val _filteredStatuses = MutableLiveData<List<SatelliteStatus>>()
    val filteredStatuses: LiveData<List<SatelliteStatus>> = _filteredStatuses

    // GNSS Statuses AFTER applying filter
    private val _filteredGnssStatuses = MutableLiveData<List<SatelliteStatus>>()
    val filteredGnssStatuses: LiveData<List<SatelliteStatus>> = _filteredGnssStatuses

    // SBAS Statuses AFTER applying filter
    private val _filteredSbasStatuses = MutableLiveData<List<SatelliteStatus>>()
    val filteredSbasStatuses: LiveData<List<SatelliteStatus>> = _filteredSbasStatuses

    // All satellites BEFORE filtering
    private val _allSatellitesGroup = MutableLiveData<SatelliteGroup>()
    val allSatellitesGroup: LiveData<SatelliteGroup> = _allSatellitesGroup

    // GNSS Satellites AFTER applying filter
    private val _filteredGnssSatellites = MutableLiveData<Map<String, Satellite>>()
    val filteredGnssSatellites : LiveData<Map<String, Satellite>> = _filteredGnssSatellites

    // SBAS Satellites AFTER applying filter
    private val _filteredSbasSatellites = MutableLiveData<Map<String, Satellite>>()
    val filteredSbasSatellites : LiveData<Map<String, Satellite>> = _filteredSbasSatellites

    // Satellite metadata AFTER applying filter
    private val _filteredSatelliteMetadata = MutableLiveData<SatelliteMetadata>()
    val filteredSatelliteMetadata: LiveData<SatelliteMetadata> = _filteredSatelliteMetadata

    private val _location = MutableLiveData<Location>()
    val location: LiveData<Location> = _location

    private val _timeBetweenLocationUpdatesSeconds = MutableLiveData<Double>()
    val timeBetweenLocationUpdatesSeconds: LiveData<Double> = _timeBetweenLocationUpdatesSeconds

    private val _timeBetweenGnssSystemTimeSeconds = MutableLiveData<Double>()
    val timeBetweenGnssSystemTimeSeconds: LiveData<Double> = _timeBetweenGnssSystemTimeSeconds

    private val _ttff = MutableLiveData("")
    val ttff: LiveData<String> = _ttff

    private val _geoidAltitude = MutableLiveData<GeoidAltitude>()
    val geoidAltitude: LiveData<GeoidAltitude> = _geoidAltitude

    private val _dop = MutableLiveData<DilutionOfPrecision>()
    val dop: LiveData<DilutionOfPrecision> = _dop

    private val _datum = MutableLiveData<Datum>()
    val datum: LiveData<Datum> = _datum

    private val _fixState = MutableLiveData<FixState>(FixState.NotAcquired)
    val fixState: LiveData<FixState> = _fixState

    private var scanningJob: Job? = null
    val scanDurationMs = TimeUnit.SECONDS.toMillis(15)

    private val _finishedScanningCfs = MutableLiveData(false)
    val finishedScanningCfs: LiveData<Boolean> = _finishedScanningCfs

    private val _timeUntilScanCompleteMs = MutableLiveData(scanDurationMs)
    val timeUntilScanCompleteMs: LiveData<Long> = _timeUntilScanCompleteMs

    // Human-readable set of observed ADR states for GNSS measurements
    private val _adrStates = MutableLiveData<Set<String>>(emptySet())
    val adrStates: LiveData<Set<String>> = _adrStates

    private val _userCountry = MutableLiveData(UserCountry())
    val userCountry: LiveData<UserCountry> = _userCountry

    private var started = false

    init {
        viewModelScope.launch {
            // Observe state changes here, NOT flows. Observe flows in setStarted().
            observeLocationUpdateStates()
            observeGnssStates()
            observePrefs()
        }
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates() {
        locationRepo.receivingLocationUpdates
            .onEach {
                setStarted(it)
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        locationFlow = locationRepo.getLocations()
            .onEach { location ->
                //Log.d(TAG, "SignalInfoViewModel location: ${location.toNotificationTitle()}")
                // Save time between location updates
                val previousLocation = _location.value
                previousLocation?.let {
                    _timeBetweenLocationUpdatesSeconds.value =  ((location.time - it.time).toDouble() / 1000f)
                }
                // Store the latest location and time diff
                _location.value = location
                _timeBetweenGnssSystemTimeSeconds.value = ((System.currentTimeMillis() - location.time).toDouble() / 1000f)
                // Try to get user country
                _userCountry.value = geocode(location)
                setGotFirstFix(true)
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeGnssFlow() {
        if (gnssFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        gnssFlow = locationRepo.getGnssStatus()
            .map { it.toSatelliteStatus() }
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel gnssStatus: ${it}")
                updateStatus(it)
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeMeasurementsFlow() {
        if (measurementFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        measurementFlow = locationRepo.getMeasurements()
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel measurement: $it")
                setAdrStates(it.measurements)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Assemble a human-readable set of current accumulated delta range states to show to the user
     */
    private fun setAdrStates(gnssMeasurements: Collection<GnssMeasurement>) {
        val adrStates: MutableSet<String> = LinkedHashSet()
        for (m in gnssMeasurements) {
            val description = m.accumulatedDeltaRangeStateString()
            if (description.isNotEmpty()) {
                adrStates.add(description)
            }
        }
        _adrStates.value = adrStates
    }

    private fun observeGnssStates() {
        locationRepo.firstFixState
            .onEach {
                when (it) {
                    is FirstFixState.Acquired -> {
                        onGnssFirstFix(it.ttffMillis)
                    }
                    is FirstFixState.NotAcquired -> if (prefsRepo.prefs().isTrackingStarted) onGnssFixLost()
                }
            }
            .launchIn(viewModelScope)
        locationRepo.fixState
            .onEach {
                when (it) {
                    is FixState.Acquired -> onGnssFixAcquired()
                    is FixState.NotAcquired -> if (prefsRepo.prefs().isTrackingStarted) onGnssFixLost()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observePrefs() {
        prefsRepo.userPreferencesFlow
            .onEach {
                Log.d(TAG, "Tracking foreground location: ${it.isTrackingStarted}")
                // Cancel the above flows when the user turns off tracking via UI
                if (!it.isTrackingStarted) {
                    setStarted(false)
                }
                _prefs.value = it
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeNmeaFlow() {
        if (nmeaFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe NMEA sentences via Flow as they are generated by the repository
        nmeaFlow = locationRepo.getNmea()
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel NMEA: ${it}")
                onNmeaMessage(it.message, it.timestamp)
            }
            .launchIn(viewModelScope)
    }



    @ExperimentalCoroutinesApi
    @VisibleForTesting
    fun updateStatus(status: List<SatelliteStatus>) {
        _allStatuses.value = status
        _allSatellitesGroup.value = status.toSatelliteGroup()

        // Get filter set by user in UI
        val filter = PreferenceUtils.gnssFilter()

        // Split list into GNSS and SBAS statuses, apply "shown" filter, and update view model
        val (gnssStatus, sbasStatus) = status
            .filter {
                filter.isEmpty() || filter.contains(it.gnssType)
            }
            .partition {
                it.gnssType != GnssType.SBAS
            }

        _filteredStatuses.value = gnssStatus + sbasStatus
        setFilteredAndSortedStatuses(sort(gnssStatus, true), sort(sbasStatus, false))
    }

    /**
     * Returns a sorted version of the provided [status] list according to the sort preference of
     * the user, with [isGnss] set to true if the list contains all GNSS signals and false if
     * it contains all SBAS signals
     */
    private fun sort(status: List<SatelliteStatus>, isGnss: Boolean): List<SatelliteStatus> {
        return when (PreferenceUtils.getSatSortOrderFromPreferences()) {
            0 -> {
                // Sort by Constellation
                if (isGnss) {
                    status.sortedWith(compareBy(SatelliteStatus::gnssType, SatelliteStatus::svid))
                } else {
                    status.sortedWith(compareBy(SatelliteStatus::sbasType, SatelliteStatus::svid))
                }
            }
            1 -> {
                // Sort by Carrier Frequency (raw CF, then label to group L5s, E5a, etc.)
                status.sortedWith(
                    compareBy<SatelliteStatus> {
                        it.carrierFrequencyHz
                    }.thenBy {
                        getCarrierFrequencyLabel(it)
                    }.thenBy {
                        it.svid
                    }
                )
            }
            2 -> {
                // Sort by Signal Strength
                status.sortedWith(compareByDescending(SatelliteStatus::cn0DbHz))
            }
            3 -> {
                // Sort by Used in Fix
                status.sortedWith(
                    compareByDescending(SatelliteStatus::usedInFix).thenComparing(
                        SatelliteStatus::svid
                    )
                )
            }
            4 -> {
                // Sort by Constellation, Carrier Frequency
                if (isGnss) {
                    status.sortedWith(
                        compareBy(
                            SatelliteStatus::gnssType,
                            SatelliteStatus::carrierFrequencyHz,
                            SatelliteStatus::svid
                        )
                    )
                } else {
                    status.sortedWith(
                        compareBy(
                            SatelliteStatus::sbasType,
                            SatelliteStatus::carrierFrequencyHz,
                            SatelliteStatus::svid
                        )
                    )
                }
            }
            5 -> {
                // Sort by Constellation, Signal Strength
                if (isGnss) {
                    status.sortedWith(
                        compareBy(SatelliteStatus::gnssType).thenByDescending(
                            SatelliteStatus::cn0DbHz
                        )
                    )
                } else {
                    status.sortedWith(
                        compareBy(SatelliteStatus::sbasType).thenByDescending(
                            SatelliteStatus::cn0DbHz
                        )
                    )
                }
            }
            6 -> {
                // Sort by Constellation, Used in Fix
                if (isGnss) {
                    status.sortedWith(
                        compareBy(SatelliteStatus::gnssType).thenByDescending(
                            SatelliteStatus::usedInFix
                        ).thenComparing(SatelliteStatus::svid)
                    )
                } else {
                    status.sortedWith(
                        compareBy(SatelliteStatus::sbasType).thenByDescending(
                            SatelliteStatus::usedInFix
                        ).thenComparing(SatelliteStatus::svid)
                    )
                }
            }
            else -> status
        }
    }


    /**
     * Adds a new set of GNSS and SBAS status objects (signals) so they can be analyzed and grouped
     * into satellites. Filter and sorting should have been applied before calling this method so
     * only signals and satellites that will be shown to the user are included.
     *
     * @param gnssStatuses a new set of GNSS status objects (signals)
     * @param sbasStatuses a new set of SBAS status objects (signals)
     */
    private fun setFilteredAndSortedStatuses(gnssStatuses: List<SatelliteStatus>, sbasStatuses: List<SatelliteStatus>) {
        this._filteredGnssStatuses.value = gnssStatuses
        this._filteredSbasStatuses.value = sbasStatuses

        val gnssSatellites = gnssStatuses.toSatelliteGroup()
        this._filteredGnssSatellites.value = gnssSatellites.satellites
        val sbasSatellites = sbasStatuses.toSatelliteGroup()
        this._filteredSbasSatellites.value = sbasSatellites.satellites

        _filteredSatelliteMetadata.value = SatelliteMetadata(
            numSignalsInView = gnssSatellites.satelliteMetadata.numSignalsInView + sbasSatellites.satelliteMetadata.numSignalsInView,
            numSignalsUsed = gnssSatellites.satelliteMetadata.numSignalsUsed + sbasSatellites.satelliteMetadata.numSignalsUsed,
            numSignalsTotal = gnssSatellites.satelliteMetadata.numSignalsTotal + sbasSatellites.satelliteMetadata.numSignalsTotal,
            numSatsInView = gnssSatellites.satelliteMetadata.numSatsInView + sbasSatellites.satelliteMetadata.numSatsInView,
            numSatsUsed = gnssSatellites.satelliteMetadata.numSatsUsed + sbasSatellites.satelliteMetadata.numSatsUsed,
            numSatsTotal = gnssSatellites.satelliteMetadata.numSatsTotal + sbasSatellites.satelliteMetadata.numSatsTotal,
            supportedGnss = gnssSatellites.satelliteMetadata.supportedGnss,
            supportedGnssCfs = gnssSatellites.satelliteMetadata.supportedGnssCfs,
            supportedSbas = sbasSatellites.satelliteMetadata.supportedSbas,
            supportedSbasCfs = sbasSatellites.satelliteMetadata.supportedSbasCfs,
            unknownCarrierStatuses = gnssSatellites.satelliteMetadata.unknownCarrierStatuses + sbasSatellites.satelliteMetadata.unknownCarrierStatuses,
            duplicateCarrierStatuses = gnssSatellites.satelliteMetadata.duplicateCarrierStatuses + sbasSatellites.satelliteMetadata.duplicateCarrierStatuses,
            isDualFrequencyPerSatInView = gnssSatellites.satelliteMetadata.isDualFrequencyPerSatInView or sbasSatellites.satelliteMetadata.isDualFrequencyPerSatInView,
            isDualFrequencyPerSatInUse = gnssSatellites.satelliteMetadata.isDualFrequencyPerSatInUse or sbasSatellites.satelliteMetadata.isDualFrequencyPerSatInUse,
            isNonPrimaryCarrierFreqInView = gnssSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInView or sbasSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInView,
            isNonPrimaryCarrierFreqInUse = gnssSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInUse or sbasSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInUse,
            mismatchAlmanacEphemerisSameSatStatuses = gnssSatellites.satelliteMetadata.mismatchAlmanacEphemerisSameSatStatuses + sbasSatellites.satelliteMetadata.mismatchAlmanacEphemerisSameSatStatuses,
            mismatchAzimuthElevationSameSatStatuses = gnssSatellites.satelliteMetadata.mismatchAzimuthElevationSameSatStatuses + sbasSatellites.satelliteMetadata.mismatchAzimuthElevationSameSatStatuses,
            missingAlmanacEphemerisButHaveAzimuthElevation = gnssSatellites.satelliteMetadata.missingAlmanacEphemerisButHaveAzimuthElevation + sbasSatellites.satelliteMetadata.missingAlmanacEphemerisButHaveAzimuthElevation,
            signalsWithoutData = gnssSatellites.satelliteMetadata.signalsWithoutData + sbasSatellites.satelliteMetadata.signalsWithoutData
        )
    }

    private fun onGnssFirstFix(ttffMillis: Int) {
        _ttff.value = formatTtff(ttffMillis)
        setGotFirstFix(true)
    }

    private fun onGnssFixAcquired() {
        _fixState.value = FixState.Acquired
        scanningJob = viewModelScope.launch {
            val endTime = System.currentTimeMillis() + scanDurationMs
            while (_timeUntilScanCompleteMs.value!! >= 0) {
                _timeUntilScanCompleteMs.value = endTime - System.currentTimeMillis()
                delay(10)
            }

            // If we still have a fix after scanDurationMs, consider the scan complete
            if (_fixState.value == FixState.Acquired) {
                _finishedScanningCfs.value = true
            }
        }
    }

    private fun onGnssFixLost() {
        _fixState.value = FixState.NotAcquired
        _finishedScanningCfs.value = false
        if (scanningJob?.isActive == true) {
            // Cancel any existing scan
            scanningJob?.cancel()
        }
    }

    private fun onNmeaMessage(message: String, timestamp: Long) {
        if (message.startsWith("\$GPGGA") || message.startsWith("\$GNGNS") || message.startsWith("\$GNGGA")) {
            val geoidAltitude = NmeaUtils.getAltitudeMeanSeaLevel(timestamp, message)
            if (geoidAltitude != null && started) {
                _geoidAltitude.value = geoidAltitude
            }
        }
        if (message.startsWith("\$GNGSA") || message.startsWith("\$GPGSA")) {
            val dop = NmeaUtils.getDop(message)
            if (dop != null && started) {
                _dop.value = dop
            }
        }
        if (message.startsWith("\$GNDTM")) {
            val datum = NmeaUtils.getDatum(timestamp, message)
            if (datum != null && started) {
                _datum.value = datum
            }
        }

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
            observeMeasurementsFlow()
        } else {
            // Cancel updates (Note that these are canceled via trackingListener preference listener
            // in the case where updates are stopped from the Activity UI switch)
            cancelFlows()

            // Reset views
            reset()
        }
        this.started = started
    }

    private fun cancelFlows() {
        locationFlow?.cancel()
        gnssFlow?.cancel()
        nmeaFlow?.cancel()
        measurementFlow?.cancel()
    }

    /**
     * Returns true if this device is viewing multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is viewing multiple signals from the same satellite, false if it is not
     */
    val isDualFrequencyPerSatInView: Boolean get() = allSatellitesGroup.value?.satelliteMetadata?.isDualFrequencyPerSatInView ?: false

    /**
     * Returns true if this device is using multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is using multiple signals from the same satellite, false if it is not
     */
    val isDualFrequencyPerSatInUse: Boolean get() = allSatellitesGroup.value?.satelliteMetadata?.isDualFrequencyPerSatInUse ?: false

    /**
     * Returns true if a non-primary carrier frequency is in view by at least one satellite, or false if
     * only primary carrier frequencies are in view
     *
     * @return true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in view
     */
    val isNonPrimaryCarrierFreqInView: Boolean get() = allSatellitesGroup.value?.satelliteMetadata?.isNonPrimaryCarrierFreqInView ?: false

    /**
     * Returns true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in use
     *
     * @return true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in use
     */
    val isNonPrimaryCarrierFreqInUse: Boolean get() = allSatellitesGroup.value?.satelliteMetadata?.isNonPrimaryCarrierFreqInUse ?: false

    private var gotFirstFix = false

    /**
     * Returns a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     *
     * @return a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     */
    val duplicateCarrierStatuses: Map<String, SatelliteStatus>
        get() = allSatellitesGroup.value?.satelliteMetadata?.duplicateCarrierStatuses ?: emptyMap()

    /**
     * Returns a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     *
     * @return a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     */
    val unknownCarrierStatuses: Map<String, SatelliteStatus>
        get() = allSatellitesGroup.value?.satelliteMetadata?.unknownCarrierStatuses ?: emptyMap()

    /**
     * Returns a set of GNSS types that are supported by the device
     * @return a set of GNSS types that are supported by the device
     */
    fun getSupportedGnss(): Set<GnssType> {
        return allSatellitesGroup.value?.satelliteMetadata?.supportedGnss ?: emptySet()
    }

    /**
     * Returns a set of SBAS types that are supported by the device
     * @return a set of SBAS types that are supported by the device
     */
    fun getSupportedSbas(): Set<SbasType> {
        return allSatellitesGroup.value?.satelliteMetadata?.supportedSbas ?: emptySet()
    }

    /**
     * Returns a set of GNSS carrier frequency labels that are supported by the device
     * @return a set of GNSS carrier frequency labels that are supported by the device
     */
    fun getSupportedGnssCfs(): Set<String> {
        return allSatellitesGroup.value?.satelliteMetadata?.supportedGnssCfs ?: emptySet()
    }

    /**
     * Returns a set of SBAS carrier frequency labels that are supported by the device
     * @return a set of SBAS carrier frequency labels that are supported by the device
     */
    fun getSupportedSbasCfs(): Set<String> {
        return allSatellitesGroup.value?.satelliteMetadata?.supportedSbasCfs ?: emptySet()
    }

    /**
     * Returns true if this view model has observed a GNSS fix first, false if it has not
     * @return true if this view model has observed a GNSS fix first, false if it has not
     */
    fun gotFirstFix(): Boolean {
        return gotFirstFix
    }

    /**
     * Sets if the view model has observed a first GNSS fix during this execution
     * @param value true if the model has observed a first GNSS fix during this execution, false if it has not
     */
    private fun setGotFirstFix(value: Boolean) {
        gotFirstFix = value
    }

    fun reset() {
        _prefs.value = prefsRepo.prefs()
        _allStatuses.value = emptyList()
        _filteredStatuses.value = emptyList()
        _filteredGnssStatuses.value = emptyList()
        _filteredSbasStatuses.value = emptyList()
        _filteredGnssSatellites.value = emptyMap()
        _filteredSbasSatellites.value = emptyMap()
        _location.value = Location("reset")
        _ttff.value = ""
        _geoidAltitude.value = GeoidAltitude()
        _dop.value = DilutionOfPrecision(Double.NaN, Double.NaN, Double.NaN)
        _filteredSatelliteMetadata.value = SatelliteMetadata()
        _fixState.value = FixState.NotAcquired
        _allSatellitesGroup.value = SatelliteGroup(emptyMap(),SatelliteMetadata())
        gotFirstFix = false
        _finishedScanningCfs.value = false
        _timeUntilScanCompleteMs.value = scanDurationMs
        _adrStates.value = emptySet()
        _timeBetweenLocationUpdatesSeconds.value = Double.NaN
    }

    /**
     * Called when the lifecycle of the observer is ended
     */
    override fun onCleared() {
        super.onCleared()
        reset()
    }

    companion object {
        private const val TAG = "SignalInfoViewModel"
    }
}