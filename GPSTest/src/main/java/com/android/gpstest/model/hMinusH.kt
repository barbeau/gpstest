package com.android.gpstest.model

/**
 * A container class to hold the calculation of N = (h - H) where:
 * * H = [geoidAltitude.altitudeMsl], or geoid altitude
 * * N = [geoidAltitude.heightOfGeoid] above the WGS84 ellipsoid
 * * h = [this.altitude], or the location WGS84 altitude (height above the WGS84 ellipsoid)
 *
 * [hMinusH] is the calculated value of N, from within {@link SatelliteUtil#altitudeComparedTo()),
 * and [isSame] is true if the calculated value of N within this class is approximately the same as
 * the N that it was compared to (for example, {@link GeoidAltitude#heightOfGeoid}).
 *
 * See https://issuetracker.google.com/issues/191674805 for details.
 *
 */
data class hMinusH(val hMinusH: Double = Double.NaN, val isSame: Boolean = false)