package com.android.gpstest.util

import android.location.Location
import com.android.gpstest.Application
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.R
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.model.SatelliteGroup
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
                return Application.app.getString(R.string.lat_or_lon, latOrLong)
            }
            "dms" -> {
                // Degrees minutes seconds
                return UIUtils.getDMSFromLocation(
                    Application.app,
                    latOrLong,
                    coordinateType
                )
            }
            "ddm" -> {
                // Degrees decimal minutes
                return UIUtils.getDDMFromLocation(
                    Application.app,
                    latOrLong,
                    coordinateType
                )
            }
            else -> {
                // Decimal degrees
                return Application.app.getString(R.string.lat_or_lon, latOrLong)
            }
        }
    }

    fun formatAltitude(location: Location): String {
        if (location.hasAltitude()) {
            val text = when {
                PreferenceUtil.distanceUnits().equals(PreferenceUtil.METERS, ignoreCase = true) -> {
                    Application.app.getString(R.string.gps_altitude_value_meters, location.altitude)
                }
                else -> {
                    // Feet
                    Application.app.getString(
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
                    Application.app.getString(R.string.gps_speed_value_meters_sec, location.speed)
                }
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    Application.app.getString(
                        R.string.gps_speed_value_kilometers_hour,
                        UIUtils.toKilometersPerHour(location.speed)
                    )
                }
                else -> {
                    // Miles per hour
                    Application.app.getString(
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
        if (SatelliteUtils.isVerticalAccuracySupported(location)) {
            if (PreferenceUtil.distanceUnits().equals(PreferenceUtil.METERS, ignoreCase = true)) {
                return Application.app.getString(
                    R.string.gps_hor_and_vert_accuracy_value_meters,
                        location.accuracy,
                        location.verticalAccuracyMeters
                    )
            } else {
                // Feet
                return Application.app.getString(
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
                    Application.app.getString(
                        R.string.gps_accuracy_value_meters, location.accuracy
                    )
                } else {
                    // Feet
                    Application.app.getString(
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
            Application.app.getString(
                R.string.gps_altitude_msl_value_meters,
                altitudeMsl)
        } else {
            Application.app.getString(
                R.string.gps_altitude_msl_value_feet,
                UIUtils.toFeet(altitudeMsl)
            )
        }
    }

    fun formatSpeedAccuracy(location: Location): String {
        if (SatelliteUtils.isSpeedAndBearingAccuracySupported() && location.hasSpeedAccuracy()) {
            when {
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.METERS_PER_SECOND, ignoreCase = true) -> {
                    return Application.app.getString(
                        R.string.gps_speed_acc_value_meters_sec,
                        location.speedAccuracyMetersPerSecond
                    )
                }
                PreferenceUtil.speedUnits()
                    .equals(PreferenceUtil.KILOMETERS_PER_HOUR, ignoreCase = true) -> {
                    return Application.app.getString(
                        R.string.gps_speed_acc_value_km_hour,
                        UIUtils.toKilometersPerHour(location.speedAccuracyMetersPerSecond)
                    )
                }
                else -> {
                    // Miles per hour
                    return Application.app.getString(
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
        return if (SatelliteUtils.isSpeedAndBearingAccuracySupported() && location.hasBearingAccuracy()) {
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
            if (meta.supportedGnssCfs.isNotEmpty()) " (" + IOUtils.trimEnds(
                meta.supportedGnssCfs.sorted().toString()
            ) + ")" else ""
    }
}