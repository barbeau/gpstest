/*
 * Copyright (C) 2008-2013 The Android Open Source Project,
 * Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gpstest;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GpsMapFragment extends SupportMapFragment
        implements GpsTestListener, View.OnClickListener, LocationSource,
        GoogleMap.OnCameraChangeListener, GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMyLocationButtonClickListener, OnMapReadyCallback {

    // Constants used to control how the camera animates to a position
    public static final float CAMERA_INITIAL_ZOOM = 18.0f;

    public static final float CAMERA_INITIAL_BEARING = 0.0f;

    public static final float CAMERA_INITIAL_TILT = 45.0f;

    public static final float CAMERA_ANCHOR_ZOOM = 19.0f;

    public static final float CAMERA_MIN_TILT = 0.0f;

    public static final float CAMERA_MAX_TILT = 90.0f;

    public static final double TARGET_OFFSET_METERS = 150;

    // Amount of time the user must not touch the map for the automatic camera movements to kick in
    public static final long MOVE_MAP_INTERACTION_THRESHOLD = 5 * 1000; // milliseconds

    private static final String PREFERENCE_SHOWED_DIALOG = "showed_google_map_install_dialog";

    private final static String TAG = "GpsMapFragment";

    Bundle mSavedInstanceState;

    private GoogleMap mMap;

    private LatLng mLatLng;

    private OnLocationChangedListener mListener; //Used to update the map with new location

    // Camera control
    private long mLastMapTouchTime = 0;

    private CameraPosition mlastCameraPosition;

    private boolean mGotFix;

    // User preferences for map rotation and tilt based on sensors
    private boolean mRotate;

    private boolean mTilt;

    /**
     * Clamps a value between the given positive min and max.  If abs(value) is less than
     * min, then min is returned.  If abs(value) is greater than max, then max is returned.
     * If abs(value) is between min and max, then abs(value) is returned.
     *
     * @param min   minimum allowed value
     * @param value value to be evaluated
     * @param max   maximum allowed value
     * @return clamped value between the min and max
     */
    private static double clamp(double min, double value, double max) {
        value = Math.abs(value);
        if (value >= min && value <= max) {
            return value;
        } else {
            return (value < min ? value : max);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (isGoogleMapsInstalled()) {
            // Save the savedInstanceState
            mSavedInstanceState = savedInstanceState;
            // Register for an async callback when the map is ready
            getMapAsync(this);
        } else {
            final SharedPreferences sp = Application.getPrefs();
            if (!sp.getBoolean(PREFERENCE_SHOWED_DIALOG, false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.please_install_google_maps));
                builder.setPositiveButton(getString(R.string.install),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sp.edit().putBoolean(PREFERENCE_SHOWED_DIALOG, true).commit();
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(
                                                "market://details?id=com.google.android.apps.maps"));
                                startActivity(intent);
                            }
                        }
                );
                builder.setNegativeButton(getString(R.string.no_thanks),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sp.edit().putBoolean(PREFERENCE_SHOWED_DIALOG, true).commit();
                            }
                        }
                );
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        return v;
    }

    @Override
    public void onResume() {
        SharedPreferences settings = Application.getPrefs();
        if (mMap != null) {
            if (mMap.getMapType() != Integer.valueOf(
                    settings.getString(getString(R.string.pref_key_map_type),
                            String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
            )) {
                mMap.setMapType(Integer.valueOf(
                        settings.getString(getString(R.string.pref_key_map_type),
                                String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
                ));
            }
        }
        mRotate = settings
                .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true);
        mTilt = settings.getBoolean(getString(R.string.pref_key_tilt_map_with_sensors), true);

        super.onResume();
    }

    public void onClick(View v) {
    }

    public void gpsStart() {
        mGotFix = false;
    }

    public void gpsStop() {
    }

    public void onLocationChanged(Location loc) {
        //Update real-time location on map
        if (mListener != null) {
            mListener.onLocationChanged(loc);
        }

        mLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

        if (mMap != null) {
            //Get bounds for detection of real-time location within bounds
            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            if (!mGotFix &&
                    (!bounds.contains(mLatLng) ||
                            mMap.getCameraPosition().zoom < (mMap.getMaxZoomLevel() / 2))) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(mLatLng)
                        .zoom(CAMERA_INITIAL_ZOOM)
                        .bearing(CAMERA_INITIAL_BEARING)
                        .tilt(CAMERA_INITIAL_TILT)
                        .build();

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            mGotFix = true;
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    @Deprecated
    public void onGpsStatusChanged(int event, GpsStatus status) {
    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
    }

    @Override
    public void onGnssStarted() {
    }

    @Override
    public void onGnssStopped() {
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        // For performance reasons, only proceed if this fragment is visible
        if (!getUserVisibleHint()) {
            return;
        }
        // Only proceed if map is not null
        if (mMap == null) {
            return;
        }

        /*
        If we have a location fix, and we have a preference to rotate the map based on sensors,
        and the user hasn't touched the map lately, then do the map camera reposition
        */
        if (mLatLng != null && mRotate
                && System.currentTimeMillis() - mLastMapTouchTime
                > MOVE_MAP_INTERACTION_THRESHOLD) {

            if (!mTilt || Double.isNaN(tilt)) {
                tilt = mlastCameraPosition != null ? mlastCameraPosition.tilt : 0;
            }

            float clampedTilt = (float) clamp(CAMERA_MIN_TILT, tilt, CAMERA_MAX_TILT);

            double offset = TARGET_OFFSET_METERS * (clampedTilt / CAMERA_MAX_TILT);

            CameraPosition cameraPosition = CameraPosition.builder().
                    tilt(clampedTilt).
                    bearing((float) orientation).
                    zoom((float) (CAMERA_ANCHOR_ZOOM + (tilt / CAMERA_MAX_TILT))).
                    target(mTilt ? SphericalUtil.computeOffset(mLatLng, offset, orientation)
                            : mLatLng).
                    build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    /**
     * Maps V2 Location updates
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
    }

    /**
     * Maps V2 Location updates
     */
    @Override
    public void deactivate() {
        mListener = null;
    }

    /**
     * Returns true if Google Maps is installed, false if it is not
     */
    @SuppressWarnings("unused")
    public boolean isGoogleMapsInstalled() {
        try {
            ApplicationInfo info = getActivity().getPackageManager()
                    .getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (System.currentTimeMillis() - mLastMapTouchTime < MOVE_MAP_INTERACTION_THRESHOLD) {
            /*
            If the user recently interacted with the map (causing a camera change), extend the
            touch time before automatic map movements based on sensors will kick in
            */
            mLastMapTouchTime = System.currentTimeMillis();
        }
        mlastCameraPosition = cameraPosition;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mLastMapTouchTime = System.currentTimeMillis();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mLastMapTouchTime = System.currentTimeMillis();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        mLastMapTouchTime = System.currentTimeMillis();
        // Return false, so button still functions as normal
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Show the location on the map
        mMap.setMyLocationEnabled(true);
        //Set location source
        mMap.setLocationSource(this);
        // Listener for camera changes
        mMap.setOnCameraChangeListener(this);
        // Listener for map / My Location button clicks, to disengage map camera control
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);

        GpsTestActivity.getInstance().addListener(this);
    }
}
