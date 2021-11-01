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
import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.gpstest.data.FirstFixState
import com.android.gpstest.data.FixState
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.data.toSatelliteStatus
import com.android.gpstest.model.*
import com.android.gpstest.util.*
import com.android.gpstest.util.CarrierFreqUtils.getCarrierFrequencyLabel
import com.android.gpstest.util.FormatUtils.formatTtff
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.set

/**
 * View model that holds GNSS signal information
 */
@ExperimentalCoroutinesApi
@HiltViewModel
class SignalInfoViewModel @Inject constructor(
    application: Application,
    private val repository: LocationRepository
) : AndroidViewModel(application) {
    //
    // Flows from the repository
    //
    private var locationFlow: Job? = null
    private var gnssFlow: Job? = null
    private var nmeaFlow: Job? = null

    //
    // LiveData observed by Composables
    //

    // All statuses BEFORE filtering
    private val _allStatuses = MutableLiveData<List<SatelliteStatus>>()
    val allStatuses: LiveData<List<SatelliteStatus>> = _allStatuses

    // GNSS Statuses AFTER applying filter
    private val _filteredGnssStatuses = MutableLiveData<List<SatelliteStatus>>()
    val filteredGnssStatuses: LiveData<List<SatelliteStatus>> = _filteredGnssStatuses

    // SBAS Statuses AFTER applying filter
    private val _filteredSbasStatuses = MutableLiveData<List<SatelliteStatus>>()
    val filteredSbasStatuses: LiveData<List<SatelliteStatus>> = _filteredSbasStatuses

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

    private val _ttff = MutableLiveData("")
    val ttff: LiveData<String> = _ttff

    private val _altitudeMsl = MutableLiveData<Double>()
    val altitudeMsl: LiveData<Double> = _altitudeMsl

    private val _dop = MutableLiveData<DilutionOfPrecision>()
    val dop: LiveData<DilutionOfPrecision> = _dop

    private val _fixState = MutableLiveData<FixState>(FixState.NotAcquired)
    val fixState: LiveData<FixState> = _fixState

    private var started = false

    // Preference listener that will cancel the above flows when the user turns off tracking via UI
    private val trackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferenceUtil.newStopTrackingListener { setStarted(false) }

    init {
        viewModelScope.launch {
            observeLocationUpdateStates()
            observeGnssStates()
            com.android.gpstest.Application.prefs.registerOnSharedPreferenceChangeListener(trackingListener)
        }
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates() {
        repository.receivingLocationUpdates
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
        locationFlow = repository.getLocations()
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel location: ${it.toNotificationTitle()}")
                _location.value = it
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
        gnssFlow = repository.getGnssStatus()
            .map { it.toSatelliteStatus() }
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel gnssStatus: ${it}")
                updateStatus(it)
            }
            .launchIn(viewModelScope)
    }

    private fun observeGnssStates() {
        repository.firstFixState
            .onEach {
                when (it) {
                    is FirstFixState.Acquired -> {
                        // FIXME - if the service is running but the activity is destroyed and then created
                        // we never see TTFF in the Status fragment.
                        onGnssFirstFix(it.ttffMillis)
                    }
                    is FirstFixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted()) onGnssFixLost()
                }
            }
            .launchIn(viewModelScope)
        repository.fixState
            .onEach {
                when (it) {
                    is FixState.Acquired -> onGnssFixAcquired()
                    is FixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted()) onGnssFixLost()
                }
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeNmeaFlow() {
        if (nmeaFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        nmeaFlow = repository.getNmea()
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel NMEA: ${it}")
                onNmeaMessage(it.message, it.timestamp)
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun updateStatus(status: List<SatelliteStatus>) {
        _allStatuses.value = status

        // Count number of sats shown to user
        val filter = PreferenceUtils.gnssFilter()

        // Split list into GNSS and SBAS statuses, apply "shown" filter, and update view model
        val (gnssStatus, sbasStatus) = status
            .filter {
                filter.isEmpty() || filter.contains(it.gnssType)
            }
            .partition {
                it.gnssType != GnssType.SBAS
            }

        setStatuses(sort(gnssStatus, true), sort(sbasStatus, false))
    }

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

    private fun onGnssFirstFix(ttffMillis: Int) {
        _ttff.value = formatTtff(ttffMillis)
        setGotFirstFix(true)
    }

    private fun onGnssFixAcquired() {
        _fixState.value = FixState.Acquired
    }

    private fun onGnssFixLost() {
        _fixState.value = FixState.NotAcquired
    }

    private fun onNmeaMessage(message: String, timestamp: Long) {
        if (message.startsWith("\$GPGGA") || message.startsWith("\$GNGNS") || message.startsWith("\$GNGGA")) {
            val altitudeMsl = NmeaUtils.getAltitudeMeanSeaLevel(message)
            if (altitudeMsl != null && started) {
                _altitudeMsl.value = altitudeMsl
            }
        }
        if (message.startsWith("\$GNGSA") || message.startsWith("\$GPGSA")) {
            val dop = NmeaUtils.getDop(message)
            if (dop != null && started) {
                _dop.value = dop
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
    }

    /**
     * Returns true if this device is viewing multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is viewing multiple signals from the same satellite, false if it is not
     */
    var isDualFrequencyPerSatInView = false
        private set

    /**
     * Returns true if this device is using multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is using multiple signals from the same satellite, false if it is not
     */
    var isDualFrequencyPerSatInUse = false
        private set

    /**
     * Returns true if a non-primary carrier frequency is in view by at least one satellite, or false if
     * only primary carrier frequencies are in view
     *
     * @return true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in view
     */
    var isNonPrimaryCarrierFreqInView = false
        private set

    /**
     * Returns true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in use
     *
     * @return true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in use
     */
    var isNonPrimaryCarrierFreqInUse = false
        private set
    private var gotFirstFix = false

    private var supportedGnss: MutableSet<GnssType> = HashSet()
    private var supportedSbas: MutableSet<SbasType> = HashSet()
    private var supportedGnssCfs: MutableSet<String> = HashSet()
    private var supportedSbasCfs: MutableSet<String> = HashSet()

    /**
     * Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     */
    private var mDuplicateCarrierStatuses: MutableMap<String, SatelliteStatus> = HashMap()

    /**
     * Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     */
    private var mUnknownCarrierStatuses: MutableMap<String, SatelliteStatus> = HashMap()

    /**
     * Returns a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     *
     * @return a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     */
    val duplicateCarrierStatuses: Map<String, SatelliteStatus>
        get() = mDuplicateCarrierStatuses

    /**
     * Returns a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     *
     * @return a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     */
    val unknownCarrierStatuses: Map<String, SatelliteStatus>
        get() = mUnknownCarrierStatuses

    /**
     * Returns a set of GNSS types that are supported by the device
     * @return a set of GNSS types that are supported by the device
     */
    fun getSupportedGnss(): Set<GnssType> {
        return supportedGnss
    }

    /**
     * Returns a set of SBAS types that are supported by the device
     * @return a set of SBAS types that are supported by the device
     */
    fun getSupportedSbas(): Set<SbasType> {
        return supportedSbas
    }

    /**
     * Returns a set of GNSS carrier frequency labels that are supported by the device
     * @return a set of GNSS carrier frequency labels that are supported by the device
     */
    fun getSupportedGnssCfs(): Set<String> {
        return supportedGnssCfs
    }

    /**
     * Returns a set of SBAS carrier frequency labels that are supported by the device
     * @return a set of SBAS carrier frequency labels that are supported by the device
     */
    fun getSupportedSbasCfs(): Set<String> {
        return supportedSbasCfs
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
    fun setGotFirstFix(value: Boolean) {
        gotFirstFix = value
    }

    /**
     * Adds a new set of GNSS and SBAS status objects (signals) so they can be analyzed and grouped
     * into satellites. Filter should have been applied before calling this method so only signals
     * and satellites that will be shown to the user are included.
     *
     * @param gnssStatuses a new set of GNSS status objects (signals)
     * @param sbasStatuses a new set of SBAS status objects (signals)
     */
    @VisibleForTesting
    fun setStatuses(gnssStatuses: List<SatelliteStatus>, sbasStatuses: List<SatelliteStatus>) {
        this._filteredGnssStatuses.value = gnssStatuses
        this._filteredSbasStatuses.value = sbasStatuses

        val gnssSatellites = getSatellitesFromStatuses(gnssStatuses)
        this._filteredGnssSatellites.value = gnssSatellites.satellites
        val sbasSatellites = getSatellitesFromStatuses(sbasStatuses)
        this._filteredSbasSatellites.value = sbasSatellites.satellites

        val numSignalsUsed =
            gnssSatellites.satelliteMetadata.numSignalsUsed + sbasSatellites.satelliteMetadata.numSignalsUsed
        val numSignalsInView =
            gnssSatellites.satelliteMetadata.numSignalsInView + sbasSatellites.satelliteMetadata.numSignalsInView
        val numSignalsTotal =
            gnssSatellites.satelliteMetadata.numSignalsTotal + sbasSatellites.satelliteMetadata.numSignalsTotal
        val numSatsUsed =
            gnssSatellites.satelliteMetadata.numSatsUsed + sbasSatellites.satelliteMetadata.numSatsUsed
        val numSatsInView =
            gnssSatellites.satelliteMetadata.numSatsInView + sbasSatellites.satelliteMetadata.numSatsInView
        val numSatsTotal =
            gnssSatellites.satelliteMetadata.numSatsTotal + sbasSatellites.satelliteMetadata.numSatsTotal

        _filteredSatelliteMetadata.value = SatelliteMetadata(
            numSignalsInView,
            numSignalsUsed,
            numSignalsTotal,
            numSatsInView,
            numSatsUsed,
            numSatsTotal
        )
    }

    /**
     * Returns a map with the provided status grouped into satellites
     * @param allStatuses all statuses for either all GNSS or SBAS constellations
     * @return a map with the provided status grouped into satellites. The key to the map is the combination of constellation and ID
     * created using SatelliteUtils.createGnssSatelliteKey().
     */
    private fun getSatellitesFromStatuses(allStatuses: List<SatelliteStatus>): SatelliteGroup {
        val satellites: MutableMap<String, Satellite> = HashMap()
        var numSignalsUsed = 0
        var numSignalsInView = 0
        var numSatsUsed = 0
        var numSatsInView = 0
        if (allStatuses.isEmpty()) {
            return SatelliteGroup(satellites, SatelliteMetadata(0, 0, 0, 0, 0, 0))
        }
        for (s in allStatuses) {
            if (s.usedInFix) {
                numSignalsUsed++
            }
            if (s.cn0DbHz != SatelliteStatus.NO_DATA) {
                numSignalsInView++
            }

            // Save the supported GNSS or SBAS type
            val key = SatelliteUtils.createGnssSatelliteKey(s)
            if (s.gnssType != GnssType.UNKNOWN) {
                if (s.gnssType != GnssType.SBAS) {
                    supportedGnss.add(s.gnssType)
                } else {
                    if (s.sbasType != SbasType.UNKNOWN) {
                        supportedSbas.add(s.sbasType)
                    }
                }
            }

            // Get carrier label
            val carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(s)
            if (carrierLabel == CarrierFreqUtils.CF_UNKNOWN) {
                mUnknownCarrierStatuses[SatelliteUtils.createGnssStatusKey(s)] = s
            }
            if (carrierLabel != CarrierFreqUtils.CF_UNKNOWN && carrierLabel != CarrierFreqUtils.CF_UNSUPPORTED) {
                // Save the supported GNSS or SBAS CF
                if (s.gnssType != GnssType.UNKNOWN) {
                    if (s.gnssType != GnssType.SBAS) {
                        supportedGnssCfs.add(carrierLabel)
                    } else {
                        if (s.sbasType != SbasType.UNKNOWN) {
                            supportedSbasCfs.add(carrierLabel)
                        }
                    }
                }
                // Check if this is a non-primary carrier frequency
                if (!CarrierFreqUtils.isPrimaryCarrier(carrierLabel)) {
                    isNonPrimaryCarrierFreqInView = true
                    if (s.usedInFix) {
                        isNonPrimaryCarrierFreqInUse = true
                    }
                }
            }
            var satStatuses: MutableMap<String, SatelliteStatus>
            if (!satellites.containsKey(key)) {
                // Create new satellite and add signal
                satStatuses = HashMap()
                satStatuses[carrierLabel] = s
                val sat = Satellite(key, satStatuses)
                satellites[key] = sat
                if (s.usedInFix) {
                    numSatsUsed++
                }
                if (s.cn0DbHz != SatelliteStatus.NO_DATA) {
                    numSatsInView++
                }
            } else {
                // Add signal to existing satellite
                val sat = satellites[key]
                satStatuses = sat!!.status as MutableMap<String, SatelliteStatus>
                if (!satStatuses.containsKey(carrierLabel)) {
                    // We found another frequency for this satellite
                    satStatuses[carrierLabel] = s
                    var frequenciesInUse = 0
                    var frequenciesInView = 0
                    for ((_, _, cn0DbHz, _, _, usedInFix) in satStatuses.values) {
                        if (usedInFix) {
                            frequenciesInUse++
                        }
                        if (cn0DbHz != SatelliteStatus.NO_DATA) {
                            frequenciesInView++
                        }
                    }
                    if (frequenciesInUse > 1) {
                        isDualFrequencyPerSatInUse = true
                    }
                    if (frequenciesInUse == 1 && s.usedInFix) {
                        // The new frequency we just added was the first in use for this satellite
                        numSatsUsed++
                    }
                    if (frequenciesInView > 1) {
                        isDualFrequencyPerSatInView = true
                    }
                    if (frequenciesInView == 1 && s.cn0DbHz != SatelliteStatus.NO_DATA) {
                        // The new frequency we just added was the first in view for this satellite
                        numSatsInView++
                    }
                } else {
                    // This shouldn't happen - we found a satellite signal with the same constellation, sat ID, and carrier frequency (including multiple "unknown" or "unsupported" frequencies) as an existing one
                    mDuplicateCarrierStatuses[SatelliteUtils.createGnssStatusKey(s)] = s
                }
            }
        }
        return SatelliteGroup(
            satellites,
            SatelliteMetadata(
                numSignalsInView,
                numSignalsUsed,
                allStatuses.size,
                numSatsInView,
                numSatsUsed,
                satellites.size
            )
        )
    }

    fun reset() {
        _filteredGnssStatuses.value = emptyList()
        _filteredSbasStatuses.value = emptyList()
        _filteredGnssSatellites.value = emptyMap()
        _filteredSbasSatellites.value = emptyMap()
        _location.value = Location("reset")
        _ttff.value = ""
        _altitudeMsl.value = Double.NaN
        _dop.value = DilutionOfPrecision(Double.NaN, Double.NaN, Double.NaN)
        _filteredSatelliteMetadata.value = SatelliteMetadata(0,0,0,0,0,0)
        _fixState.value = FixState.NotAcquired
        mDuplicateCarrierStatuses = HashMap()
        mUnknownCarrierStatuses = HashMap()
        supportedGnss = HashSet()
        supportedSbas = HashSet()
        supportedGnssCfs = HashSet()
        supportedSbasCfs = HashSet()
        isDualFrequencyPerSatInView = false
        isDualFrequencyPerSatInUse = false
        isNonPrimaryCarrierFreqInView = false
        isNonPrimaryCarrierFreqInUse = false
        gotFirstFix = false
    }

    /**
     * Called when the lifecycle of the observer is ended
     */
    override fun onCleared() {
        super.onCleared()
        reset()
    }
}