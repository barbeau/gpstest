package com.android.gpstest.util

import android.location.GnssClock
import android.location.GnssMeasurement
import android.location.Location
import android.os.Build
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.R
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.model.SatelliteGroup
import com.android.gpstest.util.SatelliteUtil.isBearingAccuracySupported
import com.android.gpstest.util.SatelliteUtil.isSpeedAccuracySupported
import com.android.gpstest.util.SatelliteUtil.isVerticalAccuracySupported
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
        return "Fix,$provider,$latitude,$longitude,$altitude,$speed,$accuracy,$bearing,$time," +
                "${if (isSpeedAccuracySupported()) speedAccuracyMetersPerSecond else ""}," +
                "${if (isBearingAccuracySupported()) bearingAccuracyDegrees else ""}," +
                "$elapsedRealtimeNanos," +
                "${if (isVerticalAccuracySupported()) verticalAccuracyMeters else ""}"
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
                "${if (hasTimeUncertaintyNanos()) timeUncertaintyNanos else ""}," +
                "$fullBiasNanos" +
                "${if (hasBiasNanos()) biasNanos else ""}," +
                "${if (hasBiasUncertaintyNanos()) biasUncertaintyNanos else ""}," +
                "${if (hasDriftNanosPerSecond()) driftNanosPerSecond else ""}," +
                "${ if (hasDriftUncertaintyNanosPerSecond()) driftUncertaintyNanosPerSecond else ""}," +
                "$hardwareClockDiscontinuityCount"
    }

    /**
     * Returns the following format:
     * Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
     */
    @JvmStatic
    private fun GnssMeasurement.toLog(elapsedRealtimeNanos: Long): String {
        // Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,
        return "$svid,$timeOffsetNanos,$state,$receivedSvTimeNanos,$receivedSvTimeUncertaintyNanos," +
                "$cn0DbHz,$pseudorangeRateMetersPerSecond,$pseudorangeRateUncertaintyMetersPerSecond," +
                "$accumulatedDeltaRangeState,$accumulatedDeltaRangeMeters," +
                "$accumulatedDeltaRangeUncertaintyMeters," +
                // CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,
                "${if (hasCarrierFrequencyHz()) carrierFrequencyHz else ""}," +
                "${if (hasCarrierCycles()) carrierCycles else ""}," +
                "${if (hasCarrierPhase()) carrierPhase else ""}," +
                "${if (hasCarrierPhaseUncertainty()) carrierPhaseUncertainty else ""}," +
                "$multipathIndicator," +
                "${if (hasSnrInDb()) snrInDb else ""}," +
                "$constellationType," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAutomaticGainControlLevelDb()) automaticGainControlLevelDb else ""}," +
                // BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasBasebandCn0DbHz()) basebandCn0DbHz else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasFullInterSignalBiasNanos()) fullInterSignalBiasNanos else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasFullInterSignalBiasUncertaintyNanos()) fullInterSignalBiasUncertaintyNanos else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasSatelliteInterSignalBiasNanos()) satelliteInterSignalBiasNanos else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasSatelliteInterSignalBiasUncertaintyNanos()) satelliteInterSignalBiasUncertaintyNanos else ""}," +
                "${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasCodeType()) codeType else ""}," +
                "$elapsedRealtimeNanos"
    }
}