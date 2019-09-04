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
import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.gpstest.map.MapViewModelController;
import com.android.gpstest.map.OnMapClickListener;
import com.android.gpstest.util.MapUtils;
import com.android.gpstest.util.MathUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.gpstest.map.MapConstants.ALLOW_GROUND_TRUTH_CHANGE;
import static com.android.gpstest.map.MapConstants.CAMERA_INITIAL_ZOOM;
import static com.android.gpstest.map.MapConstants.DRAW_LINE_THRESHOLD_METERS;
import static com.android.gpstest.map.MapConstants.GROUND_TRUTH;
import static com.android.gpstest.map.MapConstants.MODE;
import static com.android.gpstest.map.MapConstants.MODE_ACCURACY;
import static com.android.gpstest.map.MapConstants.MODE_MAP;

public class GpsMapFragment extends Fragment implements GpsTestListener, MapViewModelController.MapInterface {

    private static final String MAP_TYPE_SATELLITE = "mapbox.satellite";

    private static final String MAP_TYPE_STREETS = "barbeau/cju1g27421a0w1fmvsy13tjfv";

    private MapView mMap;

    RotationGestureOverlay mRotationGestureOverlay;

    Marker mMyLocationMarker;

    Marker mGroundTruthMarker;

    Polygon mHorAccPolygon;

    Polyline mErrorLine;

    List<Polyline> mPathLines = new ArrayList<>();

    private boolean mGotFix;

    // User preferences for map rotation based on sensors
    private boolean mRotate;

    private Location mLastLocation;

    private OnMapClickListener mOnMapClickListener;

    MapViewModelController mMapController;

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

        mLastLocation = null;

        mMapController = new MapViewModelController(getActivity(), this);
        mMapController.restoreState(savedInstanceState, getArguments(), mGroundTruthMarker == null);
        mMap.invalidate();

        addMapClickListener();

        GpsTestActivity.getInstance().addListener(this);

        return mMap;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = Application.getPrefs();
        if (mMap != null && mMapController.getMode().equals(MODE_MAP)) {
            try {
                setMapBoxTileSource(MAP_TYPE_STREETS);
            } catch (UnsupportedEncodingException e) {
                Log.e(mMapController.getMode(), "Error setting tile source: " + e);
            }
        } else if (mMap != null && mMapController.getMode().equals(MODE_ACCURACY)) {
            try {
                setMapBoxTileSource(MAP_TYPE_SATELLITE);
            } catch (UnsupportedEncodingException e) {
                Log.e(mMapController.getMode(), "Error setting tile source: " + e);
            }
        }
        if (mMapController.getMode().equals(MODE_MAP)) {
            mRotate = settings
                    .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true);
        }

        mMap.onResume();
    }

    /**
     * Sets the listener that should receive map click events
     * @param listener the listener that should receive map click events
     */
    public void setOnMapClickListener(OnMapClickListener listener) {
        mOnMapClickListener = listener;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        bundle.putString(MODE, mMapController.getMode());
        bundle.putBoolean(ALLOW_GROUND_TRUTH_CHANGE, mMapController.allowGroundTruthChange());
        if (mMapController.getGroundTruthLocation() != null) {
            bundle.putParcelable(GROUND_TRUTH, mMapController.getGroundTruthLocation());
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMap.onPause();
    }

    private void addMapClickListener() {
        final MapEventsReceiver mReceive = new MapEventsReceiver(){
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (!mMapController.getMode().equals(MODE_ACCURACY) || !mMapController.allowGroundTruthChange()) {
                    // Don't allow changes to the ground truth location, so don't pass taps to listener
                    return false;
                }

                if (mMap != null) {
                    addGroundTruthMarker(MapUtils.makeLocation(p));
                    mMap.invalidate();
                }

                if (mOnMapClickListener != null) {
                    Location location = new Location("OnMapClick");
                    location.setLatitude(p.getLatitude());
                    location.setLongitude(p.getLongitude());
                    mOnMapClickListener.onMapClick(location);
                }

                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        mMap.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void setMapBoxTileSource(String mapType) throws UnsupportedEncodingException {
        // To prevent web scrapers from easily finding the key, we store it encoded
        final String keyBase64 = "amdXY2VockFndXc2R1R1U3dQTmk=";
        final String key = MathUtils.fromBase64(keyBase64);

        final ITileSource tileSource;
        if (mapType.equals(MAP_TYPE_SATELLITE)) {
            // Use the Maptiler format
            tileSource = new OnlineTileSourceBase("Maptiler Satellite Hybrid", 1, 19, 256, "",
                    new String[]{"https://api.maptiler.com/maps/hybrid/"}) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl()
                            + MapTileIndex.getZoom(pMapTileIndex)
                            + "/" + MapTileIndex.getX(pMapTileIndex)
                            + "/" + MapTileIndex.getY(pMapTileIndex)
                            + "@2x.jpg?key=" + key;
                }
            };
            mMap.setTileSource(tileSource);
        } else {
            // Below is commented out due to Mapbox billing - until this is resolved, use default OSMDroid tiles

            // We're using a Mapbox style, which isn't directly supported by OSMDroid due to a different URL format than Map IDs, so build the URL ourselves
//            tileSource = new OnlineTileSourceBase("MapBox Streets", 1, 19, 256, "",
//                    new String[] { "https://api.mapbox.com/styles/v1/" + MAP_TYPE_STREETS + "/tiles/256/"}) {
//                @Override
//                public String getTileURLString(long pMapTileIndex) {
//                    return getBaseUrl()
//                            + MapTileIndex.getZoom(pMapTileIndex)
//                            + "/" + MapTileIndex.getX(pMapTileIndex)
//                            + "/" + MapTileIndex.getY(pMapTileIndex)
//                            + "@2x?access_token=" + key;
//                }
//            };
//            mMap.setTileSource(tileSource);
        }
    }

    public void gpsStart() {
        mGotFix = false;
    }

    public void gpsStop() {
    }

    public void onLocationChanged(Location loc) {
        GeoPoint startPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        if (!mGotFix) {
            // Zoom levels are a little different than Google Maps, so add 2 to our Google default to get the same view
            mMap.getController().setZoom(CAMERA_INITIAL_ZOOM + 2);
            mMap.getController().setCenter(startPoint);
            mGotFix = true;
        }

        if (loc.hasAccuracy()) {
            // Add horizontal accuracy uncertainty as polygon
            if (mHorAccPolygon == null) {
                mHorAccPolygon = new Polygon();
            }
            ArrayList<GeoPoint> circle = Polygon.pointsAsCircle(startPoint, loc.getAccuracy());
            mHorAccPolygon.setPoints(circle);

            if (!mMap.getOverlays().contains(mHorAccPolygon)) {
                mHorAccPolygon.setStrokeWidth(0.5f);
                mHorAccPolygon.setOnClickListener((polygon, mapView, eventPos) -> {
                    // Disable clicks
                    return false;
                });
                mHorAccPolygon.setFillColor(ContextCompat.getColor(Application.get(), R.color.horizontal_accuracy));
                mMap.getOverlays().add(mHorAccPolygon);
            }
        }

        if (mMapController.getMode().equals(MODE_ACCURACY) && mLastLocation != null) {
            // Draw line between this and last location
            boolean drawn = drawPathLine(mLastLocation, loc);
            if (drawn) {
                mLastLocation = loc;
            }
        }
        if (mMapController.getMode().equals(MODE_ACCURACY) && !mMapController.allowGroundTruthChange() && mMapController.getGroundTruthLocation() != null) {
            // Draw error line between ground truth and calculated position
            GeoPoint gt = MapUtils.makeGeoPoint(mMapController.getGroundTruthLocation());
            GeoPoint current = MapUtils.makeGeoPoint(loc);
            List<GeoPoint> points = new ArrayList<>(Arrays.asList(gt, current));

            if (mErrorLine == null) {
                mErrorLine = new Polyline();
                mErrorLine.setColor(Color.WHITE);
                mErrorLine.setPoints(points);
                mMap.getOverlayManager().add(mErrorLine);
            } else {
                mErrorLine.setPoints(points);
            }
        }
        // Draw my location marker last so it's on top
        if (mMyLocationMarker == null) {
            mMyLocationMarker = new Marker(mMap);
        }

        mMyLocationMarker.setPosition(startPoint);

        if (!mMap.getOverlays().contains(mMyLocationMarker)) {
            // This is the first fix when this fragment is active
            mMyLocationMarker.setIcon(ContextCompat.getDrawable(Application.get(), R.drawable.my_location));
            mMyLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mMap.getOverlays().remove(mMyLocationMarker);
            mMap.getOverlays().add(mMyLocationMarker);
        }
        if (mLastLocation == null) {
            mLastLocation = loc;
        }
        mMap.invalidate();
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
        If we're in map mode, we have a location fix, and we have a preference to rotate the map based on sensors,
        then do the map camera reposition
        */
        if (mMapController.getMode().equals(MODE_MAP) && mMyLocationMarker != null && mRotate) {
            mMap.setMapOrientation((float) -orientation);
        }
        mMap.invalidate();
    }

    @Override
    public void addGroundTruthMarker(Location location) {
        if (mMap == null) {
            return;
        }

        if (mGroundTruthMarker == null) {
            mGroundTruthMarker = new Marker(mMap);
        }

        mGroundTruthMarker.setPosition(MapUtils.makeGeoPoint(location));
        mGroundTruthMarker.setIcon(ContextCompat.getDrawable(Application.get(), R.drawable.ic_ground_truth));
        mGroundTruthMarker.setTitle(Application.get().getString(R.string.ground_truth_marker_title));
        mGroundTruthMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        if (!mMap.getOverlays().contains(mGroundTruthMarker)) {
            mMap.getOverlays().add(mGroundTruthMarker);
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
        Polyline line = new Polyline();
        List<GeoPoint> points = Arrays.asList(MapUtils.makeGeoPoint(loc1), MapUtils.makeGeoPoint(loc2));
        line.setPoints(points);
        line.setColor(Color.RED);
        line.setWidth(2.0f);
        mMap.getOverlayManager().add(line);

        mPathLines.add(line);
        return true;
    }

    /**
     * Removes all path lines from the map
     */
    @Override
    public void removePathLines() {
        for (Polyline line : mPathLines) {
            mMap.getOverlayManager().remove(line);
        }
        mPathLines = new ArrayList<>();
    }
}
