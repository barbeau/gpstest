package com.android.gpstest;

import android.location.Location;

/**
 * General map click listener that can be used on both Google and OSM Droid flavors
 */
public interface OnMapClickListener {

    void onMapClick(Location location);
}
