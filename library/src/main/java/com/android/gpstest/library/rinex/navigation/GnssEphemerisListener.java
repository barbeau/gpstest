package com.android.gpstest.library.rinex.navigation;

import androidx.annotation.NonNull;

/**
 * Informs when a new ephemeris message was fully decoded
 */
public interface GnssEphemerisListener {
    void onGpsEphemerisDecoded(@NonNull GpsEphemeris ephemeris);
}
