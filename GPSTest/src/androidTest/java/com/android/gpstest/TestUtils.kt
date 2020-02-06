/*
 * Copyright (C) 2019 Sean J. Barbeau (sjbarbeau@gmail.com)
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

import com.android.gpstest.model.GnssType
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.model.SatelliteStatus.Companion.NO_DATA
import com.android.gpstest.model.SbasType

/**
 * Returns a status for a GPS NAVSTAR L1 signal
 */
fun gpsL1(id: Int, usedInFix: Boolean): SatelliteStatus {
    val gpsL1 = SatelliteStatus(id,
            GnssType.NAVSTAR,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    gpsL1.hasCarrierFrequency = true
    gpsL1.carrierFrequencyHz = 1575420000.0f
    return gpsL1
}

/**
 * Returns a status for a GPS NAVSTAR L1 signal, but with no C/N0 data
 */
fun gpsL1NoSignal(id: Int): SatelliteStatus {
    val gpsL1 = SatelliteStatus(id,
            GnssType.NAVSTAR,
            NO_DATA,
            false,
            false,
            false,
            NO_DATA,
            NO_DATA);
    gpsL1.hasCarrierFrequency = true
    gpsL1.carrierFrequencyHz = 1575420000.0f
    return gpsL1
}

/**
 * Returns a status for a GPS NAVSTAR L2 signal
 */
fun gpsL2(): SatelliteStatus {
    val gpsL2 = SatelliteStatus(1,
            GnssType.NAVSTAR,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    gpsL2.hasCarrierFrequency = true
    gpsL2.carrierFrequencyHz = 1227600000.0f
    return gpsL2
}

/**
 * Returns a status for a GPS NAVSTAR L3 signal
 */
fun gpsL3(): SatelliteStatus {
    val gpsL3 = SatelliteStatus(1,
            GnssType.NAVSTAR,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    gpsL3.hasCarrierFrequency = true
    gpsL3.carrierFrequencyHz = 1381050000.0f
    return gpsL3
}

/**
 * Returns a status for a GPS NAVSTAR L4 signal
 */
fun gpsL4(): SatelliteStatus {
    val gpsL4 = SatelliteStatus(1,
            GnssType.NAVSTAR,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    gpsL4.hasCarrierFrequency = true
    gpsL4.carrierFrequencyHz = 1379913000.0f
    return gpsL4
}

/**
 * Returns a status for a GPS NAVSTAR L5 signal
 */
fun gpsL5(id: Int, usedInFix: Boolean): SatelliteStatus {
    val gpsL5 = SatelliteStatus(id,
            GnssType.NAVSTAR,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    gpsL5.hasCarrierFrequency = true
    gpsL5.carrierFrequencyHz = 1176450000.0f
    return gpsL5
}

/**
 * Returns a status for a GLONASS L1 FDMA signal - one frequency
 */
fun glonassL1variant1(): SatelliteStatus {
    val glonassL1variant1 = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL1variant1.hasCarrierFrequency = true
    glonassL1variant1.carrierFrequencyHz = 1598062500.0f
    return glonassL1variant1
}

/**
 * Returns a status for a GLONASS L1 FDMA signal - second frequency
 */
fun glonassL1variant2(): SatelliteStatus {
    val glonassL1variant2 = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL1variant2.hasCarrierFrequency = true
    glonassL1variant2.carrierFrequencyHz = 1605375000.0f
    return glonassL1variant2
}

/**
 * Returns a status for a GLONASS L2 signal - one frequency
 */
fun glonassL2variant1(): SatelliteStatus {
    val glonassL2 = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL2.hasCarrierFrequency = true
    glonassL2.carrierFrequencyHz = 1242937500.0f
    return glonassL2
}

/**
 * Returns a status for a GLONASS L2 signal - second frequency
 */
fun glonassL2variant2(): SatelliteStatus {
    val glonassL2variant2 = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL2variant2.hasCarrierFrequency = true
    glonassL2variant2.carrierFrequencyHz = 1248625000.0f
    return glonassL2variant2
}

/**
 * Returns a status for a GLONASS L3 signal
 */
fun glonassL3(): SatelliteStatus {
    val glonassL3 = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL3.hasCarrierFrequency = true
    glonassL3.carrierFrequencyHz = 1207140000.0f
    return glonassL3
}

/**
 * Returns a status for a GLONASS L5 signal
 */
fun glonassL5(): SatelliteStatus {
    val glonassL5 = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL5.hasCarrierFrequency = true
    glonassL5.carrierFrequencyHz = 1176450000.0f
    return glonassL5
}

/**
 * Returns a status for a GLONASS L1 CDMA signal
 */
fun glonassL1Cdma(): SatelliteStatus {
    val glonassL1Cdma = SatelliteStatus(1,
            GnssType.GLONASS,
            30f,
            true,
            true,
            true,
            72f,
            25f);
    glonassL1Cdma.hasCarrierFrequency = true
    glonassL1Cdma.carrierFrequencyHz = 1575420000.0f
    return glonassL1Cdma
}

/**
 * Returns a status for a Galileo E1 signal
 */
fun galileoE1(id: Int, usedInFix: Boolean): SatelliteStatus {
    val galileoE1 = SatelliteStatus(id,
            GnssType.GALILEO,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galileoE1.hasCarrierFrequency = true
    galileoE1.carrierFrequencyHz = 1575420000.0f
    return galileoE1
}

/**
 * Returns a status for a Galileo E5 signal
 */
fun galileoE5(id: Int, usedInFix: Boolean): SatelliteStatus {
    val galileoE5 = SatelliteStatus(id,
            GnssType.GALILEO,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galileoE5.hasCarrierFrequency = true
    galileoE5.carrierFrequencyHz = 1191795000.0f
    return galileoE5
}

/**
 * Returns a status for a Galileo E5a signal
 */
fun galileoE5a(id: Int, usedInFix: Boolean): SatelliteStatus {
    val galileoE5a = SatelliteStatus(id,
            GnssType.GALILEO,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galileoE5a.hasCarrierFrequency = true
    galileoE5a.carrierFrequencyHz = 1176450000.0f
    return galileoE5a
}

/**
 * Returns a status for a Galileo E5b signal
 */
fun galileoE5b(id: Int, usedInFix: Boolean): SatelliteStatus {
    val galileoE5b = SatelliteStatus(id,
            GnssType.GALILEO,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galileoE5b.hasCarrierFrequency = true
    galileoE5b.carrierFrequencyHz = 1207140000.0f
    return galileoE5b
}

/**
 * Returns a status for a Galileo E6 signal
 */
fun galileoE6(id: Int, usedInFix: Boolean): SatelliteStatus {
    val galileoE6 = SatelliteStatus(id,
            GnssType.GALILEO,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galileoE6.hasCarrierFrequency = true
    galileoE6.carrierFrequencyHz = 1278750000.0f
    return galileoE6
}

/**
 * Returns a status for a WAAS Galaxy 15 ID 135 L1 signal
 */
fun galaxy15_135L1(usedInFix: Boolean): SatelliteStatus {
    val galaxy15_135L1 = SatelliteStatus(135,
            GnssType.SBAS,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galaxy15_135L1.hasCarrierFrequency = true
    galaxy15_135L1.carrierFrequencyHz = 1575420000.0f
    galaxy15_135L1.sbasType = SbasType.WAAS
    return galaxy15_135L1
}

/**
 * Returns a status for a WAAS Galaxy 15 ID 135 L5 signal
 */
fun galaxy15_135L5(usedInFix: Boolean): SatelliteStatus {
    val galaxy15_135L5 = SatelliteStatus(135,
            GnssType.SBAS,
            30f,
            true,
            true,
            usedInFix,
            72f,
            25f);
    galaxy15_135L5.hasCarrierFrequency = true
    galaxy15_135L5.carrierFrequencyHz = 1176450000.0f
    galaxy15_135L5.sbasType = SbasType.WAAS
    return galaxy15_135L5
}