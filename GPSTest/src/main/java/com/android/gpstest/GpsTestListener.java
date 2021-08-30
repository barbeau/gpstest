package com.android.gpstest;

import android.location.LocationListener;

/**
 * Interface used by GpsTestActivity to communicate with Gps*Fragments
 */
public interface GpsTestListener extends LocationListener {

    void gpsStart();

    void gpsStop();

    void onOrientationChanged(double orientation, double tilt);
}