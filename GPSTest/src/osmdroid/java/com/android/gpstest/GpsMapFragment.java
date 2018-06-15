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

import android.content.SharedPreferences;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;

public class GpsMapFragment extends Fragment implements GpsTestListener {

    public final static String TAG = "GpsMapFragment";

    private MapView mMap;

    RotationGestureOverlay mRotationGestureOverlay;

    Marker mMarker;

    Polygon mHorAccPolygon;

    // User preferences for map rotation based on sensors
    private boolean mRotate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Configuration.getInstance().load(Application.get(), PreferenceManager.getDefaultSharedPreferences(Application.get()));
        mMap = new MapView(inflater.getContext());
        mMap.setMultiTouchControls(true);
        mMap.setBuiltInZoomControls(false);
        mMap.getController().setZoom(3.0f);

        mRotationGestureOverlay = new RotationGestureOverlay(mMap);
        mRotationGestureOverlay.setEnabled(true);
        mMap.getOverlays().add(mRotationGestureOverlay);

        GpsTestActivity.getInstance().addListener(this);

        return mMap;
    }

    @Override
    public void onPause() {
        super.onPause();
        mMap.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = Application.getPrefs();
        mRotate = settings
                .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true);
        mMap.onResume();
    }

    public void onClick(View v) {
    }

    public void gpsStart() {
    }

    public void gpsStop() {
    }

    public void onLocationChanged(Location loc) {
        GeoPoint startPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        mMap.getController().setCenter(startPoint);
        mMap.getController().setZoom(20.0f);

        if (loc.hasAccuracy()) {
            // Add horizontal accuracy uncertainty as polygon
            if (mHorAccPolygon == null) {
                mHorAccPolygon = new Polygon();
            }
            ArrayList<GeoPoint> circle = Polygon.pointsAsCircle(startPoint, loc.getAccuracy());
            if (circle != null) {
                mHorAccPolygon.setPoints(circle);
            }

            if (!mMap.getOverlays().contains(mHorAccPolygon)) {
                mHorAccPolygon.setStrokeWidth(0.5f);
                mHorAccPolygon.setFillColor(ContextCompat.getColor(Application.get(), R.color.horizontal_accuracy));
                mMap.getOverlays().add(mHorAccPolygon);
            }
        }

        if (mMarker == null) {
            mMarker = new Marker(mMap);
        }

        mMarker.setPosition(startPoint);
        mMarker.setTitle(String.format("%.6f\u00B0, %.6f\u00B0, %.1f m", loc.getLatitude(), loc.getLongitude(), loc.getAltitude()));

        if (!mMap.getOverlays().contains(mMarker)) {
            // This is the first fix when this fragment is active
            mMarker.setIcon(ContextCompat.getDrawable(Application.get(), R.drawable.ic_marker));
            mMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mMap.getOverlays().add(mMarker);
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
        if (mMap == null) {
            return;
        }

        /*
        If we have a location fix, and we have a preference to rotate the map based on sensors,
        then do the map camera reposition
        */
        if (mMarker != null && mRotate) {
            mMap.setMapOrientation((float) -orientation);
        }
    }
}
