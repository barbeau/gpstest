package com.android.gpstest.util

import android.location.GnssAntennaInfo
import android.location.GnssClock
import android.location.GnssMeasurement
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.R
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.model.Orientation
import com.android.gpstest.model.SatelliteGroup
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.util.SatelliteUtil.isBearingAccuracySupported
import com.android.gpstest.util.SatelliteUtil.isSpeedAccuracySupported
import com.android.gpstest.util.SatelliteUtil.isVerticalAccuracySupported
import com.android.gpstest.util.SatelliteUtil.toGnssStatusConstellationType
import java.util.concurrent.TimeUnit

/**
 * Provides access to formatting utilities for view in the UI
 **/
internal object FormatUtils {
    fun formatLatOrLon(latOrLong: Double, coordinateType: CoordinateType): String {
        if (latOrLong == 0.0) return "             "

        when (PreferenceUtil.coordinateFormat()) {
            "dd" -> {
                // Decimal degrees
                return app.getString(R.string.lat_or_lon, latOrLong)
            }
            "dms" -> {
                // Degrees minutes seconds
                return UIUtils.getDMSFromLocation(
                    app,
                    latOrLong,
                    coordinateType
                )
            }
            "ddm" -> {
                // Degrees decimal minutes
                return UIUtils.getDDMFromLocation(
                    app,
                    latOrLong,
                    coordinateType
                )
            }
            else -> {
                // Decimal degrees
                return app.getString(R.string.lat_or_lon, latOrLong)
            }
        }
    }

    fun formatAltitude(location: Location): String {
        if (location.hasAltitude()) {
            val text = when {
                PreferenceUtil.distanceUnits().equals(PreferenceUtil.METERS, ignoreCase = true) -> {
                    app.getString(R.string.gps_altitude_value_meters, location.altitude)
                }
                else -> {
                    // Feet
                    app.getString(
                        R.string.gps_altitude_value_feet,
                        UIUtils.toFeet(location.altitude)
                    )
                }
            }
            return text
        } else {
            return ""
        }
    }

    fun formatSpeed(location: Location): String {
        if (location.hasSpeed()) {
            val text = when {
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.METERS_PER_SECOND, ignoreCase = true) -> {
                    app.getString(R.string.gps_speed_value_meters_sec, location.speed)
                }
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    app.getString(
                        R.string.gps_speed_value_kilometers_hour,
                        UIUtils.toKilometersPerHour(location.speed)
                    )
                }
                else -> {
                    // Miles per hour
                    app.getString(
                        R.string.gps_speed_value_miles_hour,
                        UIUtils.toMilesPerHour(location.speed)
                    )
                }
            }
            return text
        } else {
            return ""
        }
    }

    /**
     * Returns a human-readable description of the time-to-first-fix, such as "38 sec"
     *
     * @param ttff time-to-first fix, in milliseconds
     * @return a human-readable description of the time-to-first-fix, such as "38 sec"
     */
    fun formatTtff(ttff: Int): String {
        return if (ttff == 0) {
            ""
        } else {
            TimeUnit.MILLISECONDS.toSeconds(ttff.toLong()).toString() + " sec"
        }
    }

    fun formatAccuracy(location: Location): String {
        if (location.isVerticalAccuracySupported()) {
            if (PreferenceUtil.distanceUnits().equals(PreferenceUtil.METERS, ignoreCase = true)) {
                return app.getString(
                    R.string.gps_hor_and_vert_accuracy_value_meters,
                        location.accuracy,
                        location.verticalAccuracyMeters
                    )
            } else {
                // Feet
                return app.getString(
                    R.string.gps_hor_and_vert_accuracy_value_feet,
                    UIUtils.toFeet(location.accuracy.toDouble()),
                    UIUtils.toFeet(
                        location.verticalAccuracyMeters.toDouble()
                    )
                    )
            }
        } else {
            if (location.hasAccuracy()) {
                return if (PreferenceUtil.distanceUnits()
                        .equals(PreferenceUtil.METERS, ignoreCase = true)) {
                    app.getString(
                        R.string.gps_accuracy_value_meters, location.accuracy
                    )
                } else {
                    // Feet
                    app.getString(
                        R.string.gps_accuracy_value_feet,
                        UIUtils.toFeet(location.accuracy.toDouble())
                    )
                }
            }
        }
        return ""
    }

    fun formatAltitudeMsl(altitudeMsl: Double): String {
        if (altitudeMsl.isNaN()) return ""

        return if (PreferenceUtil.distanceUnits()
                .equals(PreferenceUtil.METERS, ignoreCase = true)) {
            app.getString(
                R.string.gps_altitude_msl_value_meters,
                altitudeMsl)
        } else {
            app.getString(
                R.string.gps_altitude_msl_value_feet,
                UIUtils.toFeet(altitudeMsl)
            )
        }
    }

    fun formatSpeedAccuracy(location: Location): String {
        if (location.isSpeedAccuracySupported()) {
            when {
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.METERS_PER_SECOND, ignoreCase = true) -> {
                    return app.getString(
                        R.string.gps_speed_acc_value_meters_sec,
                        location.speedAccuracyMetersPerSecond
                    )
                }
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    return app.getString(
                        R.string.gps_speed_acc_value_km_hour,
                        UIUtils.toKilometersPerHour(location.speedAccuracyMetersPerSecond)
                    )
                }
                else -> {
                    // Miles per hour
                    return app.getString(
                        R.string.gps_speed_acc_value_miles_hour,
                        UIUtils.toMilesPerHour(location.speedAccuracyMetersPerSecond)
                    )
                }
            }
        }
        return ""
    }

    fun formatBearing(location: Location): String {
        return app.getString(R.string.gps_bearing_value, location.bearing)
    }

    fun formatBearingAccuracy(location: Location): String {
        return if (location.isBearingAccuracySupported()) {
            app.getString(
                R.string.gps_bearing_acc_value,
                location.bearingAccuracyDegrees
            )
        } else {
            ""
        }
    }

    /**
     * Returns metadata about the satellite group formatted for a notification title, like
     * "1/3 sats | 2/4 signals (E1, E5a, L1)"
     */
    fun SatelliteGroup.toNotificationTitle(): String {
        val meta = this.satelliteMetadata
        return app.getString(
            R.string.notification_title,
            meta.numSatsUsed,
            meta.numSatsInView,
            meta.numSignalsUsed,
            meta.numSignalsInView
        ) +
            if (meta.supportedGnssCfs.isNotEmpty())
                " (" + IOUtils.trimEnds(meta.supportedGnssCfs.sorted().toString()) + ")"
            else ""
    }

    /**
     * Returns location data formatted to CSV for logging in the following format:
     * Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters
     */
    @JvmStatic
    fun Location.toLog(): String {
        return "Fix,${provider},${latitude.toLog()},${longitude.toLog()},${altitude.toLog()},${speed.toLog()},${accuracy.toLog()},${bearing.toLog()},$time," +
                "${if (isSpeedAccuracySupported()) speedAccuracyMetersPerSecond.toLog() else ""}," +
                "${if (isBearingAccuracySupported()) bearingAccuracyDegrees.toLog() else ""}," +
                "$elapsedRealtimeNanos," +
                if (isVerticalAccuracySupported()) verticalAccuracyMeters.toLog() else ""
    }

    /**
     * Returns status data formatted to CSV for logging in the following format
     * ([unixTimeMillis] comes from the last location fix from GPS provider, or 0 if location hasn't been obtained yet):
     * Status,UnixTimeMillis,SignalCount,SignalIndex,ConstellationType,Svid,CarrierFrequencyHz,Cn0DbHz,AzimuthDegrees,ElevationDegrees,UsedInFix,HasAlmanacData,HasEphemerisData,BasebandCn0DbHz
     *
     * Sample data:
     * Status,0,25,0,1,10,1575420032,35.0,136.0,57.0,1,1,1,30.0
     */
    @JvmStatic
    fun SatelliteStatus.toLog(unixTimeMillis: Long, signalCount: Int, signalIndex: Int): String {
        return "Status," +
                "$unixTimeMillis," +
                "$signalCount,$signalIndex,${gnssType.toGnssStatusConstellationType()},$svid," +
                "${carrierFrequencyHz.toLog()},${cn0DbHz.toLog()},${azimuthDegrees.toLog()},${elevationDegrees.toLog()}," +
                "${if (usedInFix) "1" else "0"}," +
                "${if (hasAlmanac) "1" else "0"}," +
                "${if (hasEphemeris) "1" else "0"}," +
                if (hasBasebandCn0DbHz) basebandCn0DbHz.toLog() else ""
    }

    /**
     * Converts the provided SystemClock.elapsedRealtime() as [elapsedRealtime] and
     * [elapsedRealtimeNanos] from SystemClock.elapsedRealtimeNanos(), [clock] and
     * [measurement] to a CSV format:
     * Raw,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
     */
    @JvmStatic
    fun toLog(elapsedRealtime: Long, elapsedRealtimeNanos: Long, clock: GnssClock, measurement: GnssMeasurement): String {
        return clock.toLog(elapsedRealtime) + "," + measurement.toLog(elapsedRealtimeNanos)
    }

    /**
     * Returns the following format:
     * Raw,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount
     */
    @JvmStatic
    private fun GnssClock.toLog(elapsedRealtime: Long): String {
        return "Raw,$elapsedRealtime,$timeNanos," +
                "${if (hasLeapSecond()) leapSecond else ""}," +
                "${if (hasTimeUncertaintyNanos()) timeUncertaintyNanos.toLog() else ""}," +
                "$fullBiasNanos" +
                "${if (hasBiasNanos()) biasNanos.toLog() else ""}," +
                "${if (hasBiasUncertaintyNanos()) biasUncertaintyNanos.toLog() else ""}," +
                "${if (hasDriftNanosPerSecond()) driftNanosPerSecond.toLog() else ""}," +
                "${if (hasDriftUncertaintyNanosPerSecond()) driftUncertaintyNanosPerSecond.toLog() else ""}," +
                "$hardwareClockDiscontinuityCount"
    }

    /**
     * Returns the following format:
     * Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
     */
    @JvmStatic
    private fun GnssMeasurement.toLog(elapsedRealtimeNanos: Long): String {
        // Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,
        return "$svid,${timeOffsetNanos.toLog()},$state,$receivedSvTimeNanos,$receivedSvTimeUncertaintyNanos," +
                "${cn0DbHz.toLog()},${pseudorangeRateMetersPerSecond.toLog()},${pseudorangeRateUncertaintyMetersPerSecond.toLog()}," +
                "$accumulatedDeltaRangeState,${accumulatedDeltaRangeMeters.toLog()}," +
                "${accumulatedDeltaRangeUncertaintyMeters.toLog()}," +
                // CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,
                "${if (hasCarrierFrequencyHz()) carrierFrequencyHz.toLog() else ""}," +
                "${if (hasCarrierCycles()) carrierCycles else ""}," +
                "${if (hasCarrierPhase()) carrierPhase.toLog() else ""}," +
                "${if (hasCarrierPhaseUncertainty()) carrierPhaseUncertainty.toLog() else ""}," +
                "$multipathIndicator," +
                "${if (hasSnrInDb()) snrInDb.toLog() else ""}," +
                "$constellationType," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAutomaticGainControlLevelDb()) automaticGainControlLevelDb.toLog() else ""}," +
                // BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasBasebandCn0DbHz()) basebandCn0DbHz.toLog() else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasFullInterSignalBiasNanos()) fullInterSignalBiasNanos.toLog() else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasFullInterSignalBiasUncertaintyNanos()) fullInterSignalBiasUncertaintyNanos.toLog() else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasSatelliteInterSignalBiasNanos()) satelliteInterSignalBiasNanos.toLog() else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasSatelliteInterSignalBiasUncertaintyNanos()) satelliteInterSignalBiasUncertaintyNanos.toLog() else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasCodeType()) codeType else ""}," +
                "$elapsedRealtimeNanos"
    }

    /**
     * Returns a CSV representation of the data as:
     * GnssAntennaInfo,CarrierFrequencyMHz,PhaseCenterOffsetXOffsetMm,PhaseCenterOffsetXOffsetUncertaintyMm,PhaseCenterOffsetYOffsetMm,PhaseCenterOffsetYOffsetUncertaintyMm,PhaseCenterOffsetZOffsetMm,PhaseCenterOffsetZOffsetUncertaintyMm,PhaseCenterVariationCorrectionsArray,PhaseCenterVariationCorrectionUncertaintiesArray,PhaseCenterVariationCorrectionsDeltaPhi,PhaseCenterVariationCorrectionsDeltaTheta,SignalGainCorrectionsArray,SignalGainCorrectionUncertaintiesArray,SignalGainCorrectionsDeltaPhi,SignalGainCorrectionsDeltaTheta
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    @JvmStatic
    fun GnssAntennaInfo.toLog(): String {
        return "GnssAntennaInfo,${carrierFrequencyMHz.toLog()},${phaseCenterOffset.xOffsetMm.toLog()}," +
                "${phaseCenterOffset.xOffsetUncertaintyMm.toLog()},${phaseCenterOffset.yOffsetMm.toLog()}," +
                "${phaseCenterOffset.yOffsetUncertaintyMm.toLog()},${phaseCenterOffset.zOffsetMm.toLog()}," +
                "${phaseCenterOffset.zOffsetUncertaintyMm.toLog()},${
            IOUtils.serialize(
                phaseCenterVariationCorrections!!.correctionsArray
            )
        },${IOUtils.serialize(phaseCenterVariationCorrections!!.correctionUncertaintiesArray)}," +
                "${phaseCenterVariationCorrections!!.deltaPhi.toLog()},${phaseCenterVariationCorrections!!.deltaTheta.toLog()},${
            IOUtils.serialize(
                signalGainCorrections!!.correctionsArray
            )
        },${IOUtils.serialize(signalGainCorrections!!.correctionUncertaintiesArray)}," +
                "${signalGainCorrections!!.deltaPhi.toLog()},${signalGainCorrections!!.deltaTheta.toLog()}"
    }

    /**
     * Formats orientations as follows:
     * OrientationDeg,1637087900313,1131752852726298,200,0,0
     *
     * given [currentTimeMs] as System.currentTimeMillis(), and [millisSinceBootMs] as SystemClock.elapsedRealtime()
     *
     * where:
     * utcTimeMillis - The sum of elapsedRealtimeNanos below and the estimated device boot time at UTC, after a recent NTP (Network Time Protocol) sync.
     * elapsedRealtimeNanos - The time in nanoseconds at which the event happened.
     * yawDeg - If the screen is in portrait mode, this value equals the Azimuth degree (modulus to 0°~360°). If the screen is in landscape mode, it equals the sum (modulus to 0°~360°) of the screen rotation angle (either 90° or 270°) and the Azimuth degree. Azimuth, refers to the angle of rotation about the -z axis. This value represents the angle between the device's y axis and the magnetic north pole.
     * rollDeg - Roll, angle of rotation about the y axis. This value represents the angle between a plane perpendicular to the device's screen and a plane perpendicular to the ground.
     * pitchDeg - Pitch, angle of rotation about the x axis. This value represents the angle between a plane parallel to the device's screen and a plane parallel to the ground.
     */
    @JvmStatic
    fun Orientation.toLog(currentTimeMs: Long, millisSinceBootMs: Long): String {
        val timeAtBootMs = currentTimeMs - millisSinceBootMs
        return "OrientationDeg,${TimeUnit.NANOSECONDS.toMillis(elapsedRealtimeNanos) + timeAtBootMs}," +
                "$elapsedRealtimeNanos," +
                "${values[0].toLog()},${values[1].toLog()},${values[2].toLog()}"
    }

    /**
     * Formats [this] value to it's full numeric value in String format, NOT using scientific notation.
     *
     * For example, `0.00000000000000000001` will be output as `0.00000000000000000001`, not `1.0E-20`
     */
    @JvmStatic
    fun Float.toLog(): String {
        return this.toBigDecimal().toPlainString()
    }

    /**
     * Formats [this] value to it's full numeric value in String format, NOT using scientific notation.
     *
     * For example, `0.00000000000000000001` will be output as `0.00000000000000000001`, not `1.0E-20`
     */
    @JvmStatic
    fun Double.toLog(): String {
        return this.toBigDecimal().toPlainString()
    }
}