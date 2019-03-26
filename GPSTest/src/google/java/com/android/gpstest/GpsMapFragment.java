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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.gpstest.util.MapUtils;
import com.android.gpstest.util.MathUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class GpsMapFragment extends SupportMapFragment
        implements GpsTestListener, View.OnClickListener, LocationSource,
        GoogleMap.OnCameraChangeListener, GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMyLocationButtonClickListener, OnMapReadyCallback {

    // Constants used to control how the camera animates to a position
    private static final float CAMERA_INITIAL_ZOOM = 18.0f;

    private static final float CAMERA_INITIAL_BEARING = 0.0f;

    private static final float CAMERA_INITIAL_TILT_MAP = 45.0f;

    private static final float CAMERA_INITIAL_TILT_ACCURACY = 0.0f;

    private static final float CAMERA_ANCHOR_ZOOM = 19.0f;

    private static final float CAMERA_MIN_TILT = 0.0f;

    private static final float CAMERA_MAX_TILT = 90.0f;

    private static final double TARGET_OFFSET_METERS = 150;

    // Amount of time the user must not touch the map for the automatic camera movements to kick in
    private static final long MOVE_MAP_INTERACTION_THRESHOLD = 5 * 1000; // milliseconds

    private static final String PREFERENCE_SHOWED_DIALOG = "showed_google_map_install_dialog";

    static final String MODE = "mode";

    static final String MODE_MAP = "mode_map";

    static final String MODE_ACCURACY = "mode_accuracy";

    static final String GROUND_TRUTH = "ground_truth";

    static final String ALLOW_GROUND_TRUTH_CHANGE = "allow_ground_truth_change";

    private String mMode = MODE_MAP;

    private Bundle mSavedInstanceState;

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

    private OnMapClickListener mOnMapClickListener;

    private Marker mGroundTruthMarker;

    private Polyline mErrorLine;

    private boolean mAllowGroundTruthChange = true;

    private Location mGroundTruthLocation;

    BenchmarkViewModel mViewModel;

    private final Observer<Location> mGroundTruthLocationObserver = new Observer<Location>() {
        @Override
        public void onChanged(@Nullable final Location newValue) {
            mGroundTruthLocation = newValue;
            if (mMap != null) {
                addMapMarker(MapUtils.makeLatLng(mGroundTruthLocation));
            }
        }
    };

    private final Observer<Boolean> mAllowGroundTruthEditObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable final Boolean newValue) {
            mAllowGroundTruthChange = newValue;
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (isGooglePlayServicesInstalled()) {
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

        mViewModel = ViewModelProviders.of(getActivity()).get(BenchmarkViewModel.class);
        mViewModel.getGroundTruthLocation().observe(getActivity(), mGroundTruthLocationObserver);
        mViewModel.getAllowGroundTruthEdit().observe(getActivity(), mAllowGroundTruthEditObserver);


        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(MODE, mMode);
        bundle.putBoolean(ALLOW_GROUND_TRUTH_CHANGE, mAllowGroundTruthChange);
        if (mGroundTruthLocation != null) {
            bundle.putParcelable(GROUND_TRUTH, mGroundTruthLocation);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onResume() {
        checkMapPreferences();

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
                float tilt = mMode.equals(MODE_MAP) ? CAMERA_INITIAL_TILT_MAP : CAMERA_INITIAL_TILT_ACCURACY;
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(mLatLng)
                        .zoom(CAMERA_INITIAL_ZOOM)
                        .bearing(CAMERA_INITIAL_BEARING)
                        .tilt(tilt)
                        .build();

                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            mGotFix = true;

            if (!mAllowGroundTruthChange && mGroundTruthLocation != null) {
                // Draw error line between ground truth and calculated position
                LatLng gt = MapUtils.makeLatLng(mGroundTruthLocation);
                LatLng current = MapUtils.makeLatLng(loc);

                if (mErrorLine == null) {
                    mErrorLine = mMap.addPolyline(new PolylineOptions()
                        .add(gt, current)
                        .color(Color.WHITE)
                        .geodesic(true));
                } else {
                    mErrorLine.setPoints(Arrays.asList(gt, current));
                };
            }
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
        // Only proceed if map is not null and we're in MAP mode
        if (mMap == null || !mMode.equals(MODE_MAP)) {
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

            float clampedTilt = (float) MathUtils.clamp(CAMERA_MIN_TILT, tilt, CAMERA_MAX_TILT);

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
        if (!mMode.equals(MODE_ACCURACY) || !mAllowGroundTruthChange) {
            // Don't allow changes to the ground truth location, so don't pass taps to listener
            return;
        }
        if (mMap != null) {
            addMapMarker(latLng);
        }

        if (mOnMapClickListener != null) {
            Location location = new Location("OnMapClick");
            location.setLatitude(latLng.latitude);
            location.setLongitude(latLng.longitude);
            mOnMapClickListener.onMapClick(location);
        }
    }

    private void addMapMarker(LatLng latLng) {
        if (mGroundTruthMarker == null) {
            mGroundTruthMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(Application.get().getString(R.string.ground_truth_marker_title)));
        } else {
            mGroundTruthMarker.setPosition(latLng);
        }
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

        restoreState(mSavedInstanceState);

        checkMapPreferences();

        //Show the location on the map
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e(MODE_MAP, "Tried to initialize my location on Google Map - " + e);
        }
        //Set location source
        mMap.setLocationSource(this);
        // Listener for camera changes
        mMap.setOnCameraChangeListener(this);
        // Listener for map / My Location button clicks, to disengage map camera control
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        GpsTestActivity.getInstance().addListener(this);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restore an existing state (e.g., from device rotation)
            mMode = savedInstanceState.getString(MODE);
            mAllowGroundTruthChange = savedInstanceState.getBoolean(ALLOW_GROUND_TRUTH_CHANGE);
            Location groundTruth = savedInstanceState.getParcelable(GROUND_TRUTH);
            if (groundTruth != null) {
                mGroundTruthLocation = groundTruth;
                addMapMarker(MapUtils.makeLatLng(mGroundTruthLocation));
            }
        } else {
            // Not restoring existing state - see what was provided as arguments
            Bundle arguments = getArguments();
            if (arguments != null) {
                mMode = arguments.getString(MODE, MODE_MAP);
            }
            // If we have a ground truth location but no marker, we're starting using a ground truth
            // location from a previous execution but map wasn't initialized when we got the ViewModel
            // callback to mGroundTruthLocationObserver.  So, add the marker now to restore state.
            if (mGroundTruthLocation != null && mGroundTruthMarker == null) {
                addMapMarker(MapUtils.makeLatLng(mGroundTruthLocation));
            }
        }
    }


    /**
     * Returns true if Google Play Services is available, false if it is not
     */
    private static boolean isGooglePlayServicesInstalled() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(Application.get()) == ConnectionResult.SUCCESS;
    }

    /**
     * Sets the listener that should receive map click events
     * @param listener the listener that should receive map click events
     */
    public void setOnMapClickListener(OnMapClickListener listener) {
        mOnMapClickListener = listener;
    }

    private void checkMapPreferences() {
        SharedPreferences settings = Application.getPrefs();
        if (mMap != null && mMode.equals(MODE_MAP)) {
            if (mMap.getMapType() != Integer.valueOf(
                    settings.getString(getString(R.string.pref_key_map_type),
                            String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
            )) {
                mMap.setMapType(Integer.valueOf(
                        settings.getString(getString(R.string.pref_key_map_type),
                                String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
                ));
            }
        } else if (mMap != null && mMode.equals(MODE_ACCURACY)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (mMode.equals(MODE_MAP)) {
            mRotate = settings
                    .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true);
            mTilt = settings.getBoolean(getString(R.string.pref_key_tilt_map_with_sensors), true);
        }
    }
}
