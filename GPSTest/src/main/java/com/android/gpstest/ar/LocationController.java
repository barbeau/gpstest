// Copyright 2008 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.android.gpstest.util.MiscUtils;

/**
 * Sets the AstronomerModel's (and thus the user's) position using one of the
 * network, GPS or user-set preferences.
 *
 * @author John Taylor
 */
public class LocationController extends AbstractController implements LocationListener {
    // Must match the key in the preferences file.
    public static final String NO_AUTO_LOCATE = "no_auto_locate";
    // Must match the key in the preferences file.
    private static final String FORCE_GPS = "force_gps";
    private static final int MINIMUM_DISTANCE_BEFORE_UPDATE_METRES = 2000;
    private static final int LOCATION_UPDATE_TIME_MILLISECONDS = 600000;
    private static final String TAG = MiscUtils.getTag(LocationController.class);
    private static final float MIN_DIST_TO_SHOW_TOAST_DEGS = 0.01f;

    private Context context;
    private LocationManager locationManager;

    public LocationController(Context context, LocationManager locationManager) {
        this.context = context;
        if (locationManager != null) {
            Log.d(TAG, "Got location Manager");
        } else {
            Log.d(TAG, "Didn't get location manager");
        }
        this.locationManager = locationManager;
    }

    @Override
    public void start() {
        Log.d(TAG, "LocationController start");

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME_MILLISECONDS,
                    MINIMUM_DISTANCE_BEFORE_UPDATE_METRES,
                    this);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                LatLong myLocation = new LatLong(location.getLatitude(), location.getLongitude());
                setLocationInModel(myLocation, location.getProvider());
            }

        } catch (SecurityException securityException) {
            Log.d(TAG, "Caught " + securityException);
            Log.d(TAG, "Most likely user has not enabled this permission");
        }

        Log.d(TAG, "LocationController -start");
    }

    private void setLocationInModel(LatLong location, String provider) {
        LatLong oldLocation = model.getLocation();
        currentProvider = provider;
        model.setLocation(location);
    }

    /**
     * Last known provider;
     */
    private String currentProvider = "unknown";

    public String getCurrentProvider() {
        return currentProvider;
    }

    public LatLong getCurrentLocation() {
        return model.getLocation();
    }

    @Override
    public void stop() {
        Log.d(TAG, "LocationController stop");

        if (locationManager == null) {
            return;
        }
        locationManager.removeUpdates(this);

        Log.d(TAG, "LocationController -stop");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "LocationController onLocationChanged");

        LatLong newLocation = new LatLong(location.getLatitude(), location.getLongitude());

        Log.d(TAG, "Latitude " + newLocation.getLatitude());
        Log.d(TAG, "Longitude " + newLocation.getLongitude());
        setLocationInModel(newLocation, location.getProvider());

        // Only need get the location once.
        locationManager.removeUpdates(this);

        Log.d(TAG, "LocationController -onLocationChanged");
    }

    @Override
    public void onProviderDisabled(String provider) {
        // No action.
    }

    @Override
    public void onProviderEnabled(String provider) {
        // No action.
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // No action.
    }
}
