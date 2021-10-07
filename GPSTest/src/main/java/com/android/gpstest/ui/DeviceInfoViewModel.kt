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

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.gpstest.model.*
import com.android.gpstest.util.CarrierFreqUtils
import com.android.gpstest.util.SatelliteUtils
import java.util.*

/**
 * View model that holds device properties
 */
class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {
    val gnssSatellites = MutableLiveData<Map<String, Satellite>>()
    val gnssStatuses = MutableLiveData<List<SatelliteStatus>>()
    val sbasSatellites = MutableLiveData<Map<String, Satellite>>()
    val sbasStatuses = MutableLiveData<List<SatelliteStatus>>()
    val location = MutableLiveData<Location>()

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
     * Returns the metadata about a group of satellites
     *
     * @return the metadata about a group of satellites
     */
    /**
     * A set of metadata about all satellites the device knows of
     */
    val satelliteMetadata = MutableLiveData<SatelliteMetadata>()

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
     * into satellites
     *
     * @param gnssStatuses a new set of GNSS status objects (signals)
     * @param sbasStatuses a new set of SBAS status objects (signals)
     */
    fun setStatuses(gnssStatuses: List<SatelliteStatus>, sbasStatuses: List<SatelliteStatus>) {
        this.gnssStatuses.value = gnssStatuses
        this.sbasStatuses.value = sbasStatuses

        val gnssSatellites = getSatellitesFromStatuses(gnssStatuses)
        this.gnssSatellites.value = gnssSatellites.satellites
        val sbasSatellites = getSatellitesFromStatuses(sbasStatuses)
        this.sbasSatellites.value = sbasSatellites.satellites

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

        satelliteMetadata.value = SatelliteMetadata(
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
        gnssSatellites.value = null
        sbasSatellites.value = null
        satelliteMetadata.value = null
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