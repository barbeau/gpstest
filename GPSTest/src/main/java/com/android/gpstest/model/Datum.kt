package com.android.gpstest.model

/**
 * A container class to hold a DTM Local datum code and NMEA DTM datum from a $GNDTM NMEA sentence
 * coming from https://developer.android.com/reference/android/location/OnNmeaMessageListener amd
 * {@link NmeaUtils#getDatum()}.
 *
 * [timestamp] is the date and time of the location fix, as reported by the GNSS chipset. The value
 * is specified in milliseconds since 0:00 UTC 1 January 1970.
 */
data class Datum(val timestamp: Long = 0, val localDatumCode: String = "", val datum: String = "")