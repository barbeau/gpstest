package com.android.gpstest;

import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.LocationListener;

/**
 * Interface used by GpsTestActivity to communicate with Gps*Fragments
 */
public interface GpsTestListener extends LocationListener {

    void gpsStart();

    void gpsStop();

    @Deprecated
    void onGpsStatusChanged(int event, GpsStatus status);

    void onGnssFirstFix(int ttffMillis);

    void onSatelliteStatusChanged(GnssStatus status);

    void onGnssStarted();

    void onGnssStopped();

    void onGnssMeasurementsReceived(GnssMeasurementsEvent event);

    void onOrientationChanged(double orientation, double tilt);

    void onNmeaMessage(String message, long timestamp);
}