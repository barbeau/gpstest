package com.android.gpstest.model

/**
 * A container class to hold the altitude above mean sea level ([altitudeMsl], or geoid altitude,
 * or H) and [heightOfGeoid] above the WGS84 ellipsoid (N) (coming from
 * https://developer.android.com/reference/android/location/OnNmeaMessageListener amd
 * {@link NmeaUtils#getAltitudeMeanSeaLevel()}
 *
 * WGS84 altitude ({@link Location#getAltitude()} is h, and H = -N + h
 */
data class GeoidAltitude(val altitudeMsl: Double = Double.NaN, val heightOfGeoid: Double = Double.NaN)