package com.android.gpstest.library.util.rinex

import android.location.GnssClock
import android.location.GnssMeasurement
import android.location.GnssMeasurement.ADR_STATE_VALID
import android.location.GnssMeasurement.STATE_BIT_SYNC
import android.location.GnssMeasurement.STATE_CODE_LOCK
import android.location.GnssMeasurement.STATE_GAL_E1BC_CODE_LOCK
import android.location.GnssMeasurement.STATE_GAL_E1B_PAGE_SYNC
import android.location.GnssMeasurement.STATE_GAL_E1C_2ND_CODE_LOCK
import android.location.GnssMeasurement.STATE_GLO_STRING_SYNC
import android.location.GnssMeasurement.STATE_GLO_TOD_DECODED
import android.location.GnssMeasurement.STATE_MSEC_AMBIGUOUS
import android.location.GnssMeasurement.STATE_SBAS_SYNC
import android.location.GnssMeasurement.STATE_SUBFRAME_SYNC
import android.location.GnssMeasurement.STATE_SYMBOL_SYNC
import android.location.GnssMeasurement.STATE_TOW_DECODED
import android.util.Log
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.util.SatelliteUtil.toGnssType
import com.android.gpstest.library.util.rinex.RinexWriting.BDST_TO_GPST
import com.android.gpstest.library.util.rinex.RinexWriting.NS_TO_S
import com.android.gpstest.library.util.rinex.RinexWriting.SPEED_OF_LIGHT
import com.android.gpstest.library.util.rinex.RinexWriting.WEEK_TO_S
import com.android.gpstest.library.util.rinex.RinexWriting.formatGpst
import com.android.gpstest.library.util.rinex.RinexWriting.gpstToMicroseconds
import com.android.gpstest.library.util.rinex.RinexWriting.writeRnx3Header
import com.android.gpstest.library.util.rinex.RinexWriting.writeRnx3HeaderEnd
import com.android.gpstest.library.util.rinex.RinexWriting.writeRnx3HeaderRunBy
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.round

/**
 * A GNSS logger to store information to a RINEX OBS file. Originally from
 * https://github.com/rokubun/android_rinex, modified for GPSTest.
 */
object RinexObservationWriting {
    const val TAG = "RinexObservationWriting"

    /**
     * Epoch flag as defined by RINEX v3.03
     */
    object EpochFlag {
        const val OK = 0
        const val POWER_FAILURE = 1
        const val MOVING_ANTENNA = 2
        const val NEW_SITE = 3
        const val HEADER_INFORMATION = 4
        const val EXTERNAL_EVENT = 5
        const val CYCLE_SLIP = 6
    }

    fun generateHeader(
        obsList: Map<GnssType, List<String>>,
        firstEpoch: GnssClock,
        runAt: Calendar,
        ver: Double = 3.03,
        pgm: String = "Rokubun",
        markerName: String = "UNKN",
        markerType: String = "SMARTPHONE",
        observer: String = "unknown",
        agency: String = "unknown",
        rec: String = "unknown",
        recType: String = "unknown",
        recVersion: String = "unknown",
        antenna: String = "unknown",
        antType: String = "unknown",
        pos: List<Double> = listOf(0.0, 0.0, 0.0),
        hen: List<Double> = listOf(0.0, 0.0, 0.0),
        gloSlotFreqChns: Map<String, Int> = emptyMap(),
        gloCodPhsBis: Map<String, Double> = emptyMap()
    ): String {
        return buildString {
            append(writeRnx3Header(ver, "OBSERVATION DATA"))
            append(writeRnx3HeaderRunBy(runAt, pgm, agency))
            append(writeRnx3HeaderMarkerName(markerName))
            append(writeRnx3HeaderMarkerType(markerType))
            append(writeRnx3HeaderObsAgency(observer, agency))
            append(writeRnx3HeaderRecType(rec, recType, recVersion))
            append(writeRnx3HeaderAntType(antenna, antType))
            append(writeRnx3HeaderAntPos(pos))
            append(writeRnx3HeaderAntHen(hen))
            append(writeRnx3HeaderObsList(obsList))
            append(writeRnx3HeaderFirstObs(firstEpoch))
            append(writeRnx3HeaderGloSlotFreqChn(gloSlotFreqChns))
            append(writeRnx3HeaderGloCodPhsBis(gloCodPhsBis))
            append(writeRnx3HeaderEnd())
        }
    }

    /**
     * Write observation epoch and per‐satellite measurements
     * @param epochFlag As defined by RINEX v3.03http://www.apache.org/licenses/LICENSE-2.0
     * @return the RINEX string to represent the observation or null if the flag is not supported
     */
    fun generateObservationsRinexString(
        hardwareClock: GnssClock,
        measurements: Collection<GnssMeasurement>,
        epochFlag: Int = 0
    ): String? {
        val sb = StringBuilder()

        // Epoch line
        val clockBiasNs = hardwareClock.fullBiasNanos + hardwareClock.biasNanos
        val gpsTimeNs = hardwareClock.timeNanos - clockBiasNs
        val datePart = formatGpst(gpsTimeNs.toLong(), "> yyyy MM dd HH mm ss.")
        val micro = gpstToMicroseconds(gpsTimeNs.toLong())
        sb.append(String.format(Locale.US, "%s%s %2d %2d%n", datePart, micro, epochFlag, measurements.size))

        if(epochFlag == EpochFlag.OK || epochFlag == EpochFlag.POWER_FAILURE) {
            for (measurement in measurements) {
                val satelliteCharacter = measurement.constellationType.toGnssType().toRinexChar()

                if (satelliteCharacter == null) {
                    // TODO log error
                    continue
                }

                val satelliteNumber = measurement.svid.toString()
                sb.append(
                    String.format(Locale.US, "%c%2s", satelliteCharacter, satelliteNumber)
                )

                val observations = process(hardwareClock, measurement)
                for ((id, value) in observations) {
                    if (value == null || value > 40e6) {
                        sb.append(String.format(Locale.US, "%16s", ""))
                    } else {
                        sb.append(String.format(Locale.US, "%14.3f00", value))
                    }
                }
                sb.append("\n")
            }
        } else {
            // TODO generate EVENT records
            return null
        }

        return sb.toString()
    }

    /**
     * Adapted from gnsslogger.py process()
     *
     * Process a log measurement. This method computes the pseudorange, carrier-phase (in cycles)
     * Doppler (cycles/s) as well as CN/0
     *
     * @param hardwareClock Time of measurement
     * @param measurement GNSS Logger measurement line to process
     *
     * TODO test
     */
    private fun process(hardwareClock: GnssClock, measurement: GnssMeasurement): Map<String, Double?> {
        val constellation = measurement.constellationType.toGnssType()

        val frequencyHz = if (measurement.hasCarrierFrequencyHz()) measurement.carrierFrequencyHz else null
        val band = getRnxBandFromFreq(frequencyHz) ?: return mapOf()
        val attr = getRnxAttr(band, constellation, measurement.state)

        val codeValid = checkSyncState(measurement)
        val carrierStateValid = checkAdrState(measurement)

        val resultObservations = mutableMapOf<String, Double?>()

        // Compute the GPS week number and reception time (i.e. clock epoch)
        val gpsWeekNumber = floor(-hardwareClock.fullBiasNanos * NS_TO_S / WEEK_TO_S)
        val bias = hardwareClock.fullBiasNanos + hardwareClock.biasNanos
        val gpsTime = hardwareClock.timeNanos - bias
        val gpsSecondsOfWeek = gpsTime * NS_TO_S - gpsWeekNumber * WEEK_TO_S

        // Compute the reception times
        val tRxSeconds = gpsSecondsOfWeek - measurement.timeOffsetNanos * NS_TO_S

        // Compute wavelength for metric conversion in cycles
        val wavelength = if (measurement.hasCarrierFrequencyHz()) {
            SPEED_OF_LIGHT / measurement.carrierFrequencyHz
        } else {
            SPEED_OF_LIGHT / (154 * 10.23e6)
        }

        if(codeValid) {
            // Compute transmit time (depends on constellation of origin)
            val tau = if (measurement.constellationType.toGnssType() == GnssType.GLONASS) {
                // GLOT is given as TOD, need to change to TOW
                // TODO
                0.0
            } else if (measurement.constellationType.toGnssType() == GnssType.BEIDOU) {
                // BDST uses different epoch as GPS
                val tTxSeconds = measurement.receivedSvTimeNanos * NS_TO_S + BDST_TO_GPST
                // Compute the travel time, which will be eventually the pseudorange
                checkWeekCrossover(tRxSeconds, tTxSeconds)
            } else {
                // GPS, QZSS, GAL and SBAS share the same epoch time

                val tTxSeconds = measurement.receivedSvTimeNanos * NS_TO_S
                // Compute the travel time, which will be eventually the pseudorange
                checkWeekCrossover(tRxSeconds, tTxSeconds)
            }

            // Compute the apparent range as the difference between the received time and
            // the transmitted time
            val pseudorange = tau * SPEED_OF_LIGHT

            resultObservations["C${band}${attr}"] = pseudorange
        } else {
            resultObservations["C${band}${attr}"] = null
        }

        if (carrierStateValid) {
            // Process the accumulated delta range (i.e. carrier phase). This
            // needs to be translated from meters to cycles (i.e. RINEX format specification)
            val carrierphase = measurement.accumulatedDeltaRangeMeters / wavelength
            resultObservations["L${band}${attr}"] = carrierphase
        } else {
            resultObservations["L${band}${attr}"] = null
        }

        val doppler = -measurement.pseudorangeRateMetersPerSecond / wavelength
        resultObservations["D${band}${attr}"] = doppler

        val cn0 = measurement.cn0DbHz
        resultObservations["S${band}${attr}"] = cn0

        return resultObservations
    }

    /**
     * Checks if measurement is valid or not based on the Sync bits
     */
    private fun checkSyncState(measurement: GnssMeasurement): Boolean {
        val state = measurement.state
        val constellation = measurement.constellationType.toGnssType()
        val frequencyHz = if (measurement.hasCarrierFrequencyHz()) measurement.carrierFrequencyHz else null
        val band = getRnxBandFromFreq(frequencyHz)

        fun fmt(v: Int): Pair<String, String> =
            v.toString(16).padStart(2, '0') to v.toString(2).padStart(8, '0')

        fun err(mask: Int, name: String): Boolean {
            val (sHex, sBin) = fmt(state)
            val (mHex, mBin) = fmt(mask)
            Log.w(
                TAG,
                "State [ 0x${sHex} ${sBin} ] has $name [ 0x${mHex} ${mBin} ] not valid"
            )
            return false
        }

        when (constellation) {
            GnssType.NAVSTAR -> {
                if (state and STATE_CODE_LOCK == 0)      return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                if (state and STATE_TOW_DECODED == 0)    return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                if (state and STATE_BIT_SYNC == 0)       return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                if (state and STATE_SUBFRAME_SYNC == 0)  return err(STATE_SUBFRAME_SYNC, "STATE_SUBFRAME_SYNC")
                if (state and STATE_MSEC_AMBIGUOUS != 0) return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
            }

            GnssType.SBAS -> {
                if (state and STATE_CODE_LOCK == 0)      return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                if (state and STATE_TOW_DECODED == 0)    return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                if (state and STATE_BIT_SYNC == 0)       return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                if (state and STATE_SYMBOL_SYNC == 0)    return err(STATE_SYMBOL_SYNC, "STATE_SYMBOL_SYNC")
                if (state and STATE_SBAS_SYNC == 0)      return err(STATE_SBAS_SYNC, "STATE_SBAS_SYNC")
                if (state and STATE_MSEC_AMBIGUOUS != 0) return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
            }

            GnssType.GLONASS -> {
                if (state and STATE_CODE_LOCK == 0)        return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                if (state and STATE_SYMBOL_SYNC == 0)      return err(STATE_SYMBOL_SYNC, "STATE_SYMBOL_SYNC")
                if (state and STATE_BIT_SYNC == 0)         return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                if (state and STATE_GLO_TOD_DECODED == 0)  return err(STATE_GLO_TOD_DECODED, "STATE_GLO_TOD_DECODED")
                if (state and STATE_GLO_STRING_SYNC == 0)  return err(STATE_GLO_STRING_SYNC, "STATE_GLO_STRING_SYNC")
                if (state and STATE_MSEC_AMBIGUOUS != 0)   return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
            }

            GnssType.QZSS -> {
                if (state and STATE_CODE_LOCK == 0)      return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                if (state and STATE_TOW_DECODED == 0)    return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                if (state and STATE_BIT_SYNC == 0)       return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                if (state and STATE_SUBFRAME_SYNC == 0)  return err(STATE_SUBFRAME_SYNC, "STATE_SUBFRAME_SYNC")
                if (state and STATE_MSEC_AMBIGUOUS != 0) return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
            }

            GnssType.BEIDOU -> {
                if (state and STATE_CODE_LOCK == 0)      return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                if (state and STATE_TOW_DECODED == 0)    return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                if (state and STATE_BIT_SYNC == 0)       return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                if (state and STATE_SUBFRAME_SYNC == 0)  return err(STATE_SUBFRAME_SYNC, "STATE_SUBFRAME_SYNC")
                if (state and STATE_MSEC_AMBIGUOUS != 0) return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
            }

            GnssType.GALILEO -> {
                when (band) {
                    1 -> {
                        if (state and STATE_GAL_E1BC_CODE_LOCK == 0) return err(STATE_GAL_E1BC_CODE_LOCK, "STATE_GAL_E1BC_CODE_LOCK")

                        if (state and STATE_GAL_E1C_2ND_CODE_LOCK == 0) {
                            if (state and STATE_TOW_DECODED == 0)   return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                            if (state and STATE_BIT_SYNC == 0)      return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                            if (state and STATE_GAL_E1B_PAGE_SYNC == 0)
                                return err(STATE_GAL_E1B_PAGE_SYNC, "STATE_GAL_E1B_PAGE_SYNC")
                            if (state and STATE_MSEC_AMBIGUOUS != 0)
                                return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
                        } else {
                            if (state and STATE_GAL_E1C_2ND_CODE_LOCK == 0)
                                return err(STATE_GAL_E1C_2ND_CODE_LOCK, "STATE_GAL_E1C_2ND_CODE_LOCK")
                            if (state and STATE_MSEC_AMBIGUOUS != 0)
                                return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
                        }
                    }
                    5 -> {
                        if (state and STATE_CODE_LOCK == 0)      return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                        if (state and STATE_TOW_DECODED == 0)    return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                        if (state and STATE_BIT_SYNC == 0)       return err(STATE_BIT_SYNC, "STATE_BIT_SYNC")
                        if (state and STATE_SUBFRAME_SYNC == 0)  return err(STATE_SUBFRAME_SYNC, "STATE_SUBFRAME_SYNC")
                        if (state and STATE_MSEC_AMBIGUOUS != 0) return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
                    }
                    else -> {
                        // you could choose to ignore or error on unexpected bands
                    }
                }
            }

            GnssType.UNKNOWN -> {
                if (state and STATE_CODE_LOCK == 0)      return err(STATE_CODE_LOCK, "STATE_CODE_LOCK")
                if (state and STATE_TOW_DECODED == 0)    return err(STATE_TOW_DECODED, "STATE_TOW_DECODED")
                if (state and STATE_MSEC_AMBIGUOUS != 0) return err(STATE_MSEC_AMBIGUOUS, "STATE_MSEC_AMBIGUOUS")
            }

            else -> {
                val (cHex, cBin) = fmt(measurement.constellationType)
                Log.w(RinexObservationWriting.javaClass.name,
                    "ConstellationType [ 0x${cHex} ${cBin} ] is not valid")
                return false
            }
        }

        return true
    }

    /**
     * Checks if measurement is valid or not based on the Sync bits
     */
    private fun checkAdrState(measurement: GnssMeasurement): Boolean {
        // Obtain state, constellation type and frquency value to apply proper sync state
        val state = measurement.accumulatedDeltaRangeState

        fun fmt(v: Int): Pair<String, String> =
            v.toString(16).padStart(2, '0') to v.toString(2).padStart(8, '0')

        fun err(mask: Int, name: String): Boolean {
            val (sHex, sBin) = fmt(state)
            val (mHex, mBin) = fmt(mask)
            Log.w(
                TAG,
                "ADR State [ 0x${sHex} ${sBin} ] has $name [ 0x${mHex} ${mBin} ] not valid"
            )
            return false
        }

        if ((state and ADR_STATE_VALID) == 0) {
            return err(ADR_STATE_VALID, "ADR_STATE_VALID")
        }

        return true
    }

    /**
     * Generate the RINEX 3 attribute character for a given band.
     * Defaults to 'C' (L1/E1). Uses 'Q' for L5/E5a and 'I' for BDS B1I.
     * For Galileo E1, distinguishes between E1C and E1B based on tracking state.
     *
     * @param band         RINEX band number (e.g. 1, 2, 5)
     * @param constellation Constellation code: 'G' = GPS, 'E' = Galileo, 'C' = BDS, etc.
     * @param state        Tracking state bitmask (use STATE_* constants)
     * @return The RINEX attribute character.
     */
    fun getRnxAttr(
        band: Int,
        constellation: GnssType,
        state: Int
    ): Char {
        //TODO support IRNSS
        //TODO support SBAS C1X

        var attr = 'C'

        // Distinguish Galileo E1C vs E1B
        if (band == 1 && constellation == GnssType.GALILEO) {
            // if E1C second code lock is NOT set, but E1B page sync IS set → use 'B'
            if (state and STATE_GAL_E1C_2ND_CODE_LOCK == 0 &&
                state and STATE_GAL_E1B_PAGE_SYNC != 0
            ) {
                attr = 'B'
            }
        }

        // L5/E5a (GPS L5, QZSS L5, Galileo E5a)
        if (band == 5) {
            attr = 'Q'
        }

        // BDS B1I
        if (band == 2 && constellation == GnssType.BEIDOU) {
            attr = 'I'
        }

        return attr
    }

    /**
     * Obtain the RINEX frequency band from a carrier frequency.
     *
     * Backwards‐compatibility: if the frequency string is empty, we assume GPS L1.
     *
     * @param frequencyHz The frequency in Hz, or null for default GPS L1.
     * @return The RINEX frequency band (1, 2, or 5) or null if it cannot be determined
     */
    fun getRnxBandFromFreq(frequencyHz: Float?): Int? {
        // Convert to the integer multiplier (round(f / 10.23 MHz))
        val ifreq = if (frequencyHz == null) {
            154  // default for GPS/QZSS L1 and GAL E1
        } else {
            round(frequencyHz / 10.23e6).toInt()
        }

        return when {
            // QZSS L1 (154), GPS L1 (154), GAL E1 (154), and GLO L1 (156)
            ifreq >= 154 -> 1
            // QZSS L5 (115), GPS L5 (115), GAL E5a (115), IRNSS (115)
            ifreq == 115 -> 5
            // BDS B1I (153)
            ifreq == 153 -> 2
            else         -> null
        }
    }

    /**
     * Split an input list into sub‐lists of maximum length n
      */
    @JvmStatic
    fun <T> splitArray(arr: List<T>, n: Int): List<List<T>> = arr.chunked(n)

    @JvmStatic
    private fun writeRnx3HeaderMarkerName(markerName: String = "UNKN"): String {
        return String.format("%-60s%s%n", markerName, "MARKER NAME")
    }

    @JvmStatic
    private fun writeRnx3HeaderMarkerType(markerType: String = "SMARTPHONE"): String {
        return String.format("%-60s%s%n", markerType, "MARKER TYPE")
    }

    @JvmStatic
    private fun writeRnx3HeaderObsAgency(observer: String = "unknown", agency: String = "unknown"): String {
        return String.format("%-20s%-40s%s%n", observer, agency, "OBSERVER / AGENCY")
    }

    @JvmStatic
    private fun writeRnx3HeaderRecType(rec: String = "unknown", typ: String = "unknown", version: String = "unknown"): String {
        return String.format("%-20s%-20s%-20s%s%n", rec, typ, version, "REC # / TYPE / VERS")
    }

    @JvmStatic
    private fun writeRnx3HeaderAntType(antenna: String = "unknown", typ: String = "unknown"): String {
        return String.format("%-20s%-40s%s%n", antenna, typ, "ANT # / TYPE")
    }

    @JvmStatic
    private fun writeRnx3HeaderObsList(obsList: Map<GnssType, List<String>>): String {
        val tail = "SYS / # / OBS TYPES"
        val sb = StringBuilder()
        for ((sys, list) in obsList) {
            val lines = splitArray(list, 13)
            for ((i, line) in lines.withIndex()) {
                val prefix = if (i == 0) "%c  %3d".format(sys.toRinexChar(), list.size) else "      "
                val body = line.joinToString("") { " %3s".format(it) }
                sb.append(String.format("%-60s%s%n", prefix + body, tail))
            }
        }
        return sb.toString()
    }

    @JvmStatic
    private fun writeRnx3HeaderAntPos(pos: List<Double> = listOf(0.0, 0.0, 0.0)): String {
        val tail = "APPROX POSITION XYZ"
        return String.format(
            Locale.US,
            "%14.4f%14.4f%14.4f%18s%s%n",
            pos[0], pos[1], pos[2], "", tail
        )
    }

    @JvmStatic
    private fun writeRnx3HeaderAntHen(hen: List<Double> = listOf(0.0, 0.0, 0.0)): String {
        val tail = "ANTENNA: DELTA H/E/N"
        return String.format(
            Locale.US,
            "%14.4f%14.4f%14.4f%18s%s%n",
            hen[0], hen[1], hen[2], "", tail
        )
    }

    @JvmStatic
    private fun writeRnx3HeaderFirstObs(hardwareClock: GnssClock): String {
        val tail = "TIME OF FIRST OBS"
        val clockBiasNs = hardwareClock.fullBiasNanos + hardwareClock.biasNanos
        val gpsTimeNs = hardwareClock.timeNanos - clockBiasNs
        val datePart = formatGpst(gpsTimeNs.toLong(), "  yyyy    MM    dd    HH    mm    ss.")
        val micro = gpstToMicroseconds(gpsTimeNs.toLong())
        val full = "$datePart$micro"
        return String.format("%-60s%s%n", full, tail)
    }

    @JvmStatic
    private fun writeRnx3HeaderGloSlotFreqChn(glo: Map<String, Int>): String {
        if (glo.isEmpty()) return ""
        val tail = "GLONASS SLOT / FRQ #"
        val sb = StringBuilder()
        sb.append("%3d ".format(glo.size))
        var count = sb.length
        for ((sat, freq) in glo) {
            sb.append("%3s %2d ".format(sat, freq))
            // If we’re nearing 60 chars, flush a line
            if (sb.length >= 60) {
                sb.append(String.format("%-60s%s%n", sb.toString(), tail))
                sb.setLength(0)
                sb.append("    ")
            }
        }
        sb.append(String.format("%-60s%s%n", sb.toString(), tail))
        return sb.toString()
    }

    @JvmStatic
    private fun writeRnx3HeaderGloCodPhsBis(glo: Map<String, Double>): String {
        val tail = "GLONASS COD/PHS/BIS#"
        val sb = StringBuilder()
        if (glo.isEmpty()) {
            sb.append(String.format("%-60s%s%n", "", tail))
        } else {
            for ((sat, bias) in glo) {
                sb.append("%3s%8.3f".format(sat, bias))
            }
            sb.append(String.format("%-60s%s%n", sb.toString(), tail))
        }
        return sb.toString()
    }

    /**
     * Checks time propagation time for week crossover
     *
     * @param tRxSeconds Received time in seconds of week
     * @param tTxSeconds Transmitted time in seconds of week
     * @return Corrected propagation time
     *
     * TODO validate
     */
    @JvmStatic
    private fun checkWeekCrossover(tRxSeconds: Double, tTxSeconds: Double): Double {
        var tau = tRxSeconds - tTxSeconds
        if (tau > WEEK_TO_S / 2) {
            val delSec = round(tau / WEEK_TO_S) * WEEK_TO_S
            val rhoSec = tau - delSec

            tau = if (rhoSec > 10.0) {
                0.0
            } else {
                rhoSec
            }
        }
        return tau
    }

}