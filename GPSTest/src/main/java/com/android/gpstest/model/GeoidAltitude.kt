package com.android.gpstest.model

/**
 * A container class to hold the altitude above mean sea level ([altitudeMsl], or geoid altitude,
 * or H) and [heightOfGeoid] above the WGS84 ellipsoid (N) (coming from
 * https://developer.android.com/reference/android/location/OnNmeaMessageListener amd
 * {@link NmeaUtils#getAltitudeMeanSeaLevel()}.
 *
 * [timestamp] is the date and time of the location fix, as reported by the GNSS chipset. The value
 * is specified in milliseconds since 0:00 UTC 1 January 1970.
 *
 * WGS84 altitude ({@link Location#getAltitude()} is h, and H = -N + h
 */
data class GeoidAltitude(val timestamp: Long = 0, val altitudeMsl: Double = Double.NaN, val heightOfGeoid: Double = Double.NaN)