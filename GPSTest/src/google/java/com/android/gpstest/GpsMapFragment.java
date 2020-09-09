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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.android.gpstest.map.MapViewModelController;
import com.android.gpstest.map.OnMapClickListener;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;

import static com.android.gpstest.map.MapConstants.ALLOW_GROUND_TRUTH_CHANGE;
import static com.android.gpstest.map.MapConstants.CAMERA_ANCHOR_ZOOM;
import static com.android.gpstest.map.MapConstants.CAMERA_INITIAL_BEARING;
import static com.android.gpstest.map.MapConstants.CAMERA_INITIAL_TILT_ACCURACY;
import static com.android.gpstest.map.MapConstants.CAMERA_INITIAL_TILT_MAP;
import static com.android.gpstest.map.MapConstants.CAMERA_INITIAL_ZOOM;
import static com.android.gpstest.map.MapConstants.CAMERA_MAX_TILT;
import static com.android.gpstest.map.MapConstants.CAMERA_MIN_TILT;
import static com.android.gpstest.map.MapConstants.DRAW_LINE_THRESHOLD_METERS;
import static com.android.gpstest.map.MapConstants.GROUND_TRUTH;
import static com.android.gpstest.map.MapConstants.MODE;
import static com.android.gpstest.map.MapConstants.MODE_ACCURACY;
import static com.android.gpstest.map.MapConstants.MODE_MAP;
import static com.android.gpstest.map.MapConstants.MOVE_MAP_INTERACTION_THRESHOLD;
import static com.android.gpstest.map.MapConstants.PREFERENCE_SHOWED_DIALOG;
import static com.android.gpstest.map.MapConstants.TARGET_OFFSET_METERS;

public class GpsMapFragment extends SupportMapFragment
        implements GpsTestListener, View.OnClickListener, LocationSource,
        GoogleMap.OnCameraChangeListener, GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMyLocationButtonClickListener, OnMapReadyCallback, MapViewModelController.MapInterface {

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

    private Location mLastLocation;

    private ArrayList<Polyline> mPathLines = new ArrayList<>();

    MapViewModelController mMapController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        mLastLocation = null;

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
        mMapController = new MapViewModelController(getActivity(), this);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(MODE, mMapController.getMode());
        bundle.putBoolean(ALLOW_GROUND_TRUTH_CHANGE, mMapController.allowGroundTruthChange());
        if (mMapController.getGroundTruthLocation() != null) {
            bundle.putParcelable(GROUND_TRUTH, mMapController.getGroundTruthLocation());
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
                float tilt = mMapController.getMode().equals(MODE_MAP) ? CAMERA_INITIAL_TILT_MAP : CAMERA_INITIAL_TILT_ACCURACY;
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(mLatLng)
                        .zoom(CAMERA_INITIAL_ZOOM)
                        .bearing(CAMERA_INITIAL_BEARING)
                        .tilt(tilt)
                        .build();

                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            mGotFix = true;

            if (mMapController.getMode().equals(MODE_ACCURACY) && !mMapController.allowGroundTruthChange() && mMapController.getGroundTruthLocation() != null) {
                // Draw error line between ground truth and calculated position
                LatLng gt = MapUtils.makeLatLng(mMapController.getGroundTruthLocation());
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
            if (mMapController.getMode().equals(MODE_ACCURACY) && mLastLocation != null) {
                // Draw line between this and last location
                boolean drawn = drawPathLine(mLastLocation, loc);
                if (drawn) {
                    mLastLocation = loc;
                }
            }
        }
        if (mLastLocation == null) {
            mLastLocation = loc;
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
        if (mMap == null || !mMapController.getMode().equals(MODE_MAP)) {
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
        if (!mMapController.getMode().equals(MODE_ACCURACY) || !mMapController.allowGroundTruthChange()) {
            // Don't allow changes to the ground truth location, so don't pass taps to listener
            return;
        }
        if (mMap != null) {
            addGroundTruthMarker(MapUtils.makeLocation(latLng));
        }

        if (mOnMapClickListener != null) {
            Location location = new Location("OnMapClick");
            location.setLatitude(latLng.latitude);
            location.setLongitude(latLng.longitude);
            mOnMapClickListener.onMapClick(location);
        }
    }

    @Override
    public void addGroundTruthMarker(Location location) {
        if (mMap == null) {
            return;
        }
        LatLng latLng = MapUtils.makeLatLng(location);
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

        mMapController.restoreState(mSavedInstanceState, getArguments(), mGroundTruthMarker == null);

        checkMapPreferences();

        // Show the location on the map
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e(mMapController.getMode(), "Tried to initialize my location on Google Map - " + e);
        }
        // Set location source
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
        if (mMap != null && mMapController.getMode().equals(MODE_MAP)) {
            if (mMap.getMapType() != Integer.valueOf(
                    settings.getString(getString(R.string.pref_key_map_type),
                            String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
            )) {
                mMap.setMapType(Integer.valueOf(
                        settings.getString(getString(R.string.pref_key_map_type),
                                String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
                ));
            }
        } else if (mMap != null && mMapController.getMode().equals(MODE_ACCURACY)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (mMapController.getMode().equals(MODE_MAP)) {
            mRotate = settings
                    .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true);
            mTilt = settings.getBoolean(getString(R.string.pref_key_tilt_map_with_sensors), true);
        }

        boolean useDarkTheme = Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false);
        if (mMap != null && getActivity() != null && useDarkTheme) {
            mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getActivity(), R.raw.dark_theme));
        }
    }

    /**
     * Draws a line on the map between the two locations if its greater than a threshold value defined
     * by DRAW_LINE_THRESHOLD_METERS
     * @param loc1
     * @param loc2
     */
    @Override
    public boolean drawPathLine(Location loc1, Location loc2) {
        if (loc1.distanceTo(loc2) < DRAW_LINE_THRESHOLD_METERS) {
            return false;
        }
        Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(MapUtils.makeLatLng(loc1), MapUtils.makeLatLng(loc2))
                .color(Color.RED)
                .width(2.0f)
                .geodesic(true));
        mPathLines.add(line);
        return true;
    }

    /**
     * Removes all path lines from the map
     */
    @Override
    public void removePathLines() {
        for (Polyline line : mPathLines) {
            line.remove();
        }
        mPathLines = new ArrayList<>();
    }
}
