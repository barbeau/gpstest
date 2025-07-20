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
package com.android.gpstest.library.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.gpstest.library.data.FirstFixState
import com.android.gpstest.library.data.FixState
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.model.DilutionOfPrecision
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.Satellite
import com.android.gpstest.library.model.SatelliteGroup
import com.android.gpstest.library.model.SatelliteMetadata
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.util.CarrierFreqUtils.getCarrierFrequencyLabel
import com.android.gpstest.library.util.FormatUtils.formatTtff
import com.android.gpstest.library.util.NmeaUtils
import com.android.gpstest.library.util.PreferenceUtil
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.SatelliteUtil.toSatelliteGroup
import com.android.gpstest.library.util.SatelliteUtil.toSatelliteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model that holds GNSS signal information
 */
@ExperimentalCoroutinesApi
@HiltViewModel
class SignalInfoViewModel @Inject constructor(
    context: Context,
    application: Application,
    private val repository: LocationRepository,
    prefs: SharedPreferences
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
        PreferenceUtil.newStopTrackingListener({setStarted(context, false, prefs)}, prefs)

    init {
        viewModelScope.launch {
            observeLocationUpdateStates(context, prefs)
            observeGnssStates(prefs)
            prefs.registerOnSharedPreferenceChangeListener(trackingListener)
        }
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates(context: Context, prefs: SharedPreferences) {
        repository.receivingLocationUpdates
            .onEach {
                setStarted(context, it, prefs)
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
                setGotFirstFix(true)
            }
            .launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeGnssFlow(context: Context, prefs: SharedPreferences) {
        if (gnssFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        gnssFlow = repository.getGnssStatus()
            .map { it.toSatelliteStatus() }
            .onEach {
                //Log.d(TAG, "SignalInfoViewModel gnssStatus: ${it}")
                updateStatus(context, it, prefs)
            }
            .launchIn(viewModelScope)
    }

    private fun observeGnssStates(prefs: SharedPreferences) {
        repository.firstFixState
            .onEach {
                when (it) {
                    is FirstFixState.Acquired -> {
                        onGnssFirstFix(it.ttffMillis)
                    }
                    is FirstFixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted(prefs)) onGnssFixLost()
                }
            }
            .launchIn(viewModelScope)
        repository.fixState
            .onEach {
                when (it) {
                    is FixState.Acquired -> onGnssFixAcquired()
                    is FixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted(prefs)) onGnssFixLost()
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
    @VisibleForTesting
    fun updateStatus(context: Context, status: List<SatelliteStatus>, prefs: SharedPreferences) {
        _allStatuses.value = status
        _allSatellitesGroup.value = status.toSatelliteGroup()

        // Get filter set by user in UI
        val filter = PreferenceUtils.gnssFilter(context, prefs)

        // Split list into GNSS and SBAS statuses, apply "shown" filter, and update view model
        val (gnssStatus, sbasStatus) = status
            .filter {
                filter.isEmpty() || filter.contains(it.gnssType)
            }
            .partition {
                it.gnssType != GnssType.SBAS
            }

        _filteredStatuses.value = gnssStatus + sbasStatus
        setFilteredAndSortedStatuses(sort(context, gnssStatus, true, prefs), sort(context, sbasStatus, false, prefs))
    }

    /**
     * Returns a sorted version of the provided [status] list according to the sort preference of
     * the user, with [isGnss] set to true if the list contains all GNSS signals and false if
     * it contains all SBAS signals
     */
    private fun sort(context: Context, status: List<SatelliteStatus>, isGnss: Boolean, prefs: SharedPreferences): List<SatelliteStatus> {
        return when (PreferenceUtils.getSatSortOrderFromPreferences(context, prefs)) {
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
            gnssSatellites.satelliteMetadata.numSignalsInView + sbasSatellites.satelliteMetadata.numSignalsInView,
            gnssSatellites.satelliteMetadata.numSignalsUsed + sbasSatellites.satelliteMetadata.numSignalsUsed,
            gnssSatellites.satelliteMetadata.numSignalsTotal + sbasSatellites.satelliteMetadata.numSignalsTotal,
            gnssSatellites.satelliteMetadata.numSatsInView + sbasSatellites.satelliteMetadata.numSatsInView,
            gnssSatellites.satelliteMetadata.numSatsUsed + sbasSatellites.satelliteMetadata.numSatsUsed,
            gnssSatellites.satelliteMetadata.numSatsTotal + sbasSatellites.satelliteMetadata.numSatsTotal,
            gnssSatellites.satelliteMetadata.supportedGnss,
            gnssSatellites.satelliteMetadata.supportedGnssCfs,
            sbasSatellites.satelliteMetadata.supportedSbas,
            sbasSatellites.satelliteMetadata.supportedSbasCfs,
            gnssSatellites.satelliteMetadata.unknownCarrierStatuses + sbasSatellites.satelliteMetadata.unknownCarrierStatuses,
            gnssSatellites.satelliteMetadata.duplicateCarrierStatuses + sbasSatellites.satelliteMetadata.duplicateCarrierStatuses,
            gnssSatellites.satelliteMetadata.isDualFrequencyPerSatInView or sbasSatellites.satelliteMetadata.isDualFrequencyPerSatInView,
            gnssSatellites.satelliteMetadata.isDualFrequencyPerSatInUse or sbasSatellites.satelliteMetadata.isDualFrequencyPerSatInUse,
            gnssSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInView or sbasSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInView,
            gnssSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInUse or sbasSatellites.satelliteMetadata.isNonPrimaryCarrierFreqInUse
        )
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
    private fun setStarted(context: Context, started: Boolean, prefs: SharedPreferences) {
        if (started == this.started) {
            // State hasn't changed - no op and return
            return
        }
        if (started) {
            // Activity or service is observing updates, so observe here too
            observeLocationFlow()
            observeGnssFlow(context, prefs)
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
        _filteredGnssStatuses.value = emptyList()
        _filteredSbasStatuses.value = emptyList()
        _filteredGnssSatellites.value = emptyMap()
        _filteredSbasSatellites.value = emptyMap()
        _location.value = Location("reset")
        _ttff.value = ""
        _altitudeMsl.value = Double.NaN
        _dop.value = DilutionOfPrecision(Double.NaN, Double.NaN, Double.NaN)
        _filteredSatelliteMetadata.value = SatelliteMetadata()
        _fixState.value = FixState.NotAcquired
        _allSatellitesGroup.value = SatelliteGroup(emptyMap(), SatelliteMetadata())
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