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

import android.annotation.SuppressLint
import android.location.GnssStatus
import android.location.Location
import android.os.Build
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.R
import com.android.gpstest.model.*
import com.android.gpstest.model.SatelliteStatus.Companion.NO_DATA
import com.android.gpstest.util.CarrierFreqUtils.*
import com.android.gpstest.util.SatelliteUtils.createGnssSatelliteKey

internal object SatelliteUtil {

    /**
     * Tranforms the Android [GnssStatus] object to a list of our [SatelliteStatus] model objects
     */
    @JvmStatic
    fun GnssStatus.toSatelliteStatus() : List<SatelliteStatus> {
        val satStatuses: MutableList<SatelliteStatus> = ArrayList()

        for (i in 0 until this.satelliteCount) {
            val satStatus = SatelliteStatus(
                this.getSvid(i),
                this.getConstellationType(i).toGnssType(),
                this.getCn0DbHz(i),
                this.hasAlmanacData(i),
                this.hasEphemerisData(i),
                this.usedInFix(i),
                this.getElevationDegrees(i),
                this.getAzimuthDegrees(i)
            )
            if (SatelliteUtils.isCfSupported() && this.hasCarrierFrequencyHz(i)) {
                satStatus.hasCarrierFrequency = true
                satStatus.carrierFrequencyHz = this.getCarrierFrequencyHz(i).toDouble()
            }
            if (isBasebandCn0DbHzSupported(i)) {
                satStatus.hasBasebandCn0DbHz = true
                satStatus.basebandCn0DbHz = this.getBasebandCn0DbHz(i)
            }
            if (satStatus.gnssType == GnssType.SBAS) {
                satStatus.sbasType = satStatus.svid.toSbasType()
            }
            satStatuses.add(satStatus)
        }
        return satStatuses
    }

    /**
     * Returns a map with the provided status list grouped into satellites
     * @return a [SatelliteGroup] with the provided status list grouped into satellites in a Map. The key
     * to the map is the combination of constellation and ID created using
     * [SatelliteUtils.createGnssSatelliteKey()]. Various other metadata is also included.
     */
    fun List<SatelliteStatus>.toSatelliteGroup(): SatelliteGroup {
        val satellites: MutableMap<String, Satellite> = LinkedHashMap()
        var numSignalsUsed = 0
        var numSignalsInView = 0
        var numSatsUsed = 0
        var numSatsInView = 0
        val supportedGnss: MutableSet<GnssType> = LinkedHashSet()
        val supportedGnssCfs: MutableSet<String> = LinkedHashSet()
        val supportedSbas: MutableSet<SbasType> = LinkedHashSet()
        val supportedSbasCfs: MutableSet<String> = LinkedHashSet()
        val unknownCarrierStatuses: MutableMap<String, SatelliteStatus> = LinkedHashMap()
        val duplicateCarrierStatuses: MutableMap<String, SatelliteStatus> = LinkedHashMap()
        val mismatchAzimuthElevationSameSatStatuses: MutableMap<String, SatelliteStatus> = LinkedHashMap()
        val mismatchAlmanacEphemerisSameSatStatuses: MutableMap<String, SatelliteStatus> = LinkedHashMap()
        val missingAlmanacEphemerisButHaveAzimuthElevation: MutableMap<String, SatelliteStatus> = LinkedHashMap()
        var isDualFrequencyPerSatInView = false
        var isDualFrequencyPerSatInUse = false
        var isNonPrimaryCarrierFreqInView = false
        var isNonPrimaryCarrierFreqInUse = false
        val gnssToCf: MutableMap<GnssType, MutableSet<String>> = LinkedHashMap()
        val sbasToCf: MutableMap<SbasType, MutableSet<String>> = LinkedHashMap()

        if (this.isEmpty()) {
            return SatelliteGroup(satellites, SatelliteMetadata())
        }
        for (s in this) {
            if (s.usedInFix) {
                numSignalsUsed++
            }
            if (s.cn0DbHz != NO_DATA) {
                numSignalsInView++
            }
            if ((s.elevationDegrees != NO_DATA || s.azimuthDegrees != NO_DATA) &&
                (!s.hasAlmanac && !s.hasEphemeris)) {
                // Signal has elevation or azimuth data but no almanac or ephemeris (which it needs for elevation or azimuth)
                missingAlmanacEphemerisButHaveAzimuthElevation[SatelliteUtils.createGnssStatusKey(s)] = s
            }

            // Save the supported GNSS or SBAS type
            val key = createGnssSatelliteKey(s)
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
            val carrierLabel = getCarrierFrequencyLabel(s)
            if (carrierLabel == CF_UNKNOWN) {
                unknownCarrierStatuses[SatelliteUtils.createGnssStatusKey(s)] = s
            }
            if (carrierLabel != CF_UNKNOWN && carrierLabel != CF_UNSUPPORTED) {
                // Save the supported GNSS or SBAS CF
                if (s.gnssType != GnssType.UNKNOWN) {
                    if (s.gnssType != GnssType.SBAS) {
                        supportedGnssCfs.add(carrierLabel)
                        addToMap(gnssToCf, s.gnssType, carrierLabel)
                    } else {
                        if (s.sbasType != SbasType.UNKNOWN) {
                            supportedSbasCfs.add(carrierLabel)
                            addToMap(sbasToCf, s.sbasType, carrierLabel)
                        }
                    }
                }
                // Check if this is a non-primary carrier frequency
                if (!isPrimaryCarrier(carrierLabel)) {
                    isNonPrimaryCarrierFreqInView = true
                    if (s.usedInFix) {
                        isNonPrimaryCarrierFreqInUse = true
                    }
                }
            }
            var satStatuses: MutableMap<String, SatelliteStatus>
            if (!satellites.containsKey(key)) {
                // Create new satellite and add signal
                satStatuses = LinkedHashMap()
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
                    satStatuses.values.forEach { status ->
                        if (status.usedInFix) {
                            frequenciesInUse++
                        }
                        if (status.cn0DbHz != SatelliteStatus.NO_DATA) {
                            frequenciesInView++
                        }
                        if (s.azimuthDegrees != status.azimuthDegrees ||
                                s.elevationDegrees != status.elevationDegrees) {
                            // Found disagreement on azimuth and elevation on signals from same satellite
                            mismatchAzimuthElevationSameSatStatuses[SatelliteUtils.createGnssStatusKey(s)] =
                                s
                            mismatchAzimuthElevationSameSatStatuses[SatelliteUtils.createGnssStatusKey(status)] =
                                status
                        }
                        if (s.hasAlmanac != status.hasAlmanac ||
                                s.hasEphemeris != status.hasEphemeris) {
                            // Found disagreement on almanac and ephemeris on signals from same satellite
                            mismatchAlmanacEphemerisSameSatStatuses[SatelliteUtils.createGnssStatusKey(s)] =
                                s
                            mismatchAlmanacEphemerisSameSatStatuses[SatelliteUtils.createGnssStatusKey(status)] =
                                status
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
                this.size,
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
                isNonPrimaryCarrierFreqInUse,
                gnssToCf,
                sbasToCf,
                mismatchAzimuthElevationSameSatStatuses,
                mismatchAlmanacEphemerisSameSatStatuses,
                missingAlmanacEphemerisButHaveAzimuthElevation
            )
        )
    }

    private fun <T> addToMap(map: MutableMap<T, MutableSet<String>>, key: T, cf: String) {
        val cfs: MutableSet<String> = map.getOrDefault(key, mutableSetOf(cf))
        cfs.add(cf)
        map[key] = cfs
    }

    /**
     * Returns true if the speed accuracy is supported for this location, false if it does not
     *
     * @return true if the speed accuracy is supported for this location, false if it does not
     */
    fun Location.isSpeedAccuracySupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasSpeedAccuracy()
    }

    /**
     * Returns true if the bearing accuracy is supported for this location, false if it does not
     *
     * @return true if the bearing accuracy is supported for this location, false if it does not
     */
    fun Location.isBearingAccuracySupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasBearingAccuracy()
    }

    /**
     * Returns true if the platform supports providing vertical accuracy values and this location
     * has vertical accuracy information, false if it does not
     *
     * @return true if the platform supports providing vertical accuracy values and this location
     * has vertical accuracy information, false if it does not
     */
    fun Location.isVerticalAccuracySupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasVerticalAccuracy()
    }

    /**
     * Returns true if the platform supports providing basebandCn0DbHz values and the status at the
     * provided [index] has basebandCn0DbHz information, false if it does not
     *
     * @return true if the platform supports providing basebandCn0DbHz values and the status at the
     * provided [index] has basebandCn0DbHz information, false if it does not
     */
    fun GnssStatus.isBasebandCn0DbHzSupported(index: Int): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasBasebandCn0DbHz(index)
    }

    /**
     * Returns the Global Navigation Satellite System (GNSS) for a satellite given the GnssStatus
     * constellation type.  Note that getSbasConstellationType() should be used to get the particular
     * SBAS constellation type
     *
     * @return GnssType for the given GnssStatus constellation type
     */
    private fun Int.toGnssType(): GnssType {
        return when (this) {
            GnssStatus.CONSTELLATION_GPS -> GnssType.NAVSTAR
            GnssStatus.CONSTELLATION_GLONASS -> GnssType.GLONASS
            GnssStatus.CONSTELLATION_BEIDOU -> GnssType.BEIDOU
            GnssStatus.CONSTELLATION_QZSS -> GnssType.QZSS
            GnssStatus.CONSTELLATION_GALILEO -> GnssType.GALILEO
            GnssStatus.CONSTELLATION_IRNSS -> GnssType.IRNSS
            GnssStatus.CONSTELLATION_SBAS -> GnssType.SBAS
            GnssStatus.CONSTELLATION_UNKNOWN -> GnssType.UNKNOWN
            else -> GnssType.UNKNOWN
        }
    }

    /**
     * Returns the Android system GnssStatus.getConstellationType() for our GnssType enumeration
     * @return GnssStatus.getConstellationType() for a given GnssType
     */
    @SuppressLint("InlinedApi")
    fun GnssType.toGnssStatusConstellationType(): Int {
        return when (this) {
            GnssType.NAVSTAR -> GnssStatus.CONSTELLATION_GPS
            GnssType.GLONASS -> GnssStatus.CONSTELLATION_GLONASS
            GnssType.BEIDOU -> GnssStatus.CONSTELLATION_BEIDOU
            GnssType.QZSS -> GnssStatus.CONSTELLATION_QZSS
            GnssType.GALILEO -> GnssStatus.CONSTELLATION_GALILEO
            GnssType.IRNSS -> GnssStatus.CONSTELLATION_IRNSS
            GnssType.SBAS -> GnssStatus.CONSTELLATION_SBAS
            GnssType.UNKNOWN -> GnssStatus.CONSTELLATION_UNKNOWN
        }
    }

    /**
     * Returns the SBAS constellation type for a GnssStatus.CONSTELLATION_SBAS satellite given the GnssStatus
     * svid.  For Android 7.0 and higher.
     *
     * [this] is the identification number provided by the GnssStatus.getSvid() method
     * @return SbasType for the given GnssStatus svid for GnssStatus.CONSTELLATION_SBAS satellites
     */
    fun Int.toSbasType(): SbasType {
        if (this == 120 || this == 123 || this == 126 || this == 136) {
            return SbasType.EGNOS
        } else if (this == 125 || this == 140 || this == 141) {
            return SbasType.SDCM
        } else if (this == 130 || this == 143 || this == 144) {
            // Also referred to as BDSBAS
            return SbasType.SNAS
        } else if (this == 131 || this == 133 || this == 135 || this == 138) {
            return SbasType.WAAS
        } else if (this == 127 || this == 128 || this == 139) {
            return SbasType.GAGAN
        } else if (this == 129 || this == 137) {
            return SbasType.MSAS
        }
        return SbasType.UNKNOWN
    }

    fun SatelliteStatus.constellationName(): String {
        return when (this.gnssType) {
            GnssType.NAVSTAR -> {
                app.getString(R.string.gps_content_description)
            }
            GnssType.GALILEO -> {
                app.getString(R.string.galileo_content_description)
            }
            GnssType.GLONASS -> {
                app.getString(R.string.glonass_content_description)
            }
            GnssType.QZSS -> {
                app.getString(R.string.qzss_content_description)
            }
            GnssType.BEIDOU -> {
                app.getString(R.string.beidou_content_description)
            }
            GnssType.IRNSS -> {
                app.getString(R.string.irnss_content_description)
            }
            GnssType.SBAS -> {
                this.sbasName()
            }
            GnssType.UNKNOWN -> {
                app.getString(R.string.unknown)
            }
        }
    }

    fun SatelliteStatus.sbasName(): String {
        return when (sbasType) {
            SbasType.WAAS -> {
                app.getString(R.string.waas_content_description)
            }
            SbasType.EGNOS -> {
                app.getString(R.string.egnos_content_description)
            }
            SbasType.SDCM -> {
                app.getString(R.string.sdcm_content_description)
            }
            SbasType.MSAS -> {
                app.getString(R.string.msas_content_description)
            }
            SbasType.SNAS -> {
                app.getString(R.string.snas_content_description)
            }
            SbasType.GAGAN -> {
                app.getString(R.string.gagan_content_description)
            }
            SbasType.SACCSA -> {
                app.getString(R.string.saccsa_content_description)
            }
            SbasType.UNKNOWN -> {
                app.getString(R.string.unknown)
            }
        }
    }
}