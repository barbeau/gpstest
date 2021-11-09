/*
 * Copyright (C) 2021 Sean J. Barbeau
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
package com.android.gpstest.util

import com.android.gpstest.model.*

internal object SatelliteUtil {

    /**
     * Returns a map with the provided status grouped into satellites
     * @param allStatuses all statuses for either all GNSS or SBAS constellations
     * @return a SatelliteGroup with the provided status grouped into satellites in a Map. The key
     * to the map is the combination of constellation and ID created using
     * SatelliteUtils.createGnssSatelliteKey(). Various other metadata is also included.
     */
    fun getSatellitesFromStatuses(allStatuses: List<SatelliteStatus>): SatelliteGroup {
        val satellites: MutableMap<String, Satellite> = HashMap()
        var numSignalsUsed = 0
        var numSignalsInView = 0
        var numSatsUsed = 0
        var numSatsInView = 0
        val supportedGnss: MutableSet<GnssType> = HashSet()
        val supportedGnssCfs: MutableSet<String> = HashSet()
        val supportedSbas: MutableSet<SbasType> = HashSet()
        val supportedSbasCfs: MutableSet<String> = HashSet()
        val unknownCarrierStatuses: MutableMap<String, SatelliteStatus> = HashMap()
        val duplicateCarrierStatuses: MutableMap<String, SatelliteStatus> = HashMap()
        var isDualFrequencyPerSatInView: Boolean = false
        var isDualFrequencyPerSatInUse: Boolean = false
        var isNonPrimaryCarrierFreqInView: Boolean = false
        var isNonPrimaryCarrierFreqInUse: Boolean = false

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
                unknownCarrierStatuses[SatelliteUtils.createGnssStatusKey(s)] = s
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
                    duplicateCarrierStatuses[SatelliteUtils.createGnssStatusKey(s)] = s
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
                satellites.size,
                supportedGnss,
                supportedGnssCfs,
                supportedSbas,
                supportedSbasCfs,
                unknownCarrierStatuses,
                duplicateCarrierStatuses,
                isDualFrequencyPerSatInView,
                isDualFrequencyPerSatInUse,
                isNonPrimaryCarrierFreqInView,
                isNonPrimaryCarrierFreqInUse
            )
        )
    }
}