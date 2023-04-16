/*
 * Copyright (C) 2008-2021 The Android Open Source Project,
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
package com.android.gpstest.ui

import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.util.MathUtils
import com.android.gpstest.library.util.PreferenceUtil
import com.android.gpstest.library.util.PreferenceUtil.newStopTrackingListener
import com.android.gpstest.map.MapConstants
import com.android.gpstest.map.MapViewModelController
import com.android.gpstest.map.MapViewModelController.MapInterface
import com.android.gpstest.map.OnMapClickListener
import com.android.gpstest.util.MapUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.UnsupportedEncodingException
import javax.inject.Inject
import org.osmdroid.views.overlay.CopyrightOverlay

@AndroidEntryPoint
class MapFragment : Fragment(), MapInterface {
    private var map: MapView? = null
    var rotationGestureOverlay: RotationGestureOverlay? = null
    var myLocationMarker: Marker? = null
    var groundTruthMarker: Marker? = null
    var horAccPolygon: Polygon? = null
    var errorLine: Polyline? = null
    var pathLines: MutableList<Polyline> = ArrayList()
    private var gotFix = false

    // User preferences for map rotation based on sensors
    private var rotate = false
    private var lastLocation: Location? = null
    private var onMapClickListener: OnMapClickListener? = null
    var mapController: MapViewModelController? = null

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null
    private var sensorFlow: Job? = null

    // Preference listener that will cancel the above flows when the user turns off tracking via UI
    private val trackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        newStopTrackingListener ({ onGnssStopped() }, Application.prefs)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            Application.app, PreferenceManager.getDefaultSharedPreferences(
                Application.app
            )
        )
        val map = MapView(inflater.context)
        this.map = map
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        map.controller.setZoom(3.0)
        rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay!!.isEnabled = true
        map.overlays.apply {
            add(rotationGestureOverlay)
            add(CopyrightOverlay(inflater.context))
        }
        lastLocation = null
        mapController = MapViewModelController(activity, this)
        mapController!!.restoreState(savedInstanceState, arguments, groundTruthMarker == null)
        map.invalidate()

        Application.prefs.registerOnSharedPreferenceChangeListener(trackingListener)

        addMapClickListener()
        observeLocationUpdateStates()
        return map
    }

    override fun onResume() {
        super.onResume()
        val settings = Application.prefs
        if (map != null && mapController!!.mode == MapConstants.MODE_MAP) {
            try {
                setMapBoxTileSource(MAP_TYPE_STREETS)
            } catch (e: UnsupportedEncodingException) {
                Log.e(mapController!!.mode, "Error setting tile source: $e")
            }
        } else if (map != null && mapController!!.mode == MapConstants.MODE_ACCURACY) {
            try {
                setMapBoxTileSource(MAP_TYPE_SATELLITE)
            } catch (e: UnsupportedEncodingException) {
                Log.e(mapController!!.mode, "Error setting tile source: $e")
            }
        }
        if (mapController!!.mode == MapConstants.MODE_MAP) {
            rotate = settings
                .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true)
        }
        map!!.onResume()
    }

    /**
     * Sets the listener that should receive map click events
     * @param listener the listener that should receive map click events
     */
    fun setOnMapClickListener(listener: OnMapClickListener?) {
        onMapClickListener = listener
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putString(MapConstants.MODE, mapController!!.mode)
        bundle.putBoolean(
            MapConstants.ALLOW_GROUND_TRUTH_CHANGE,
            mapController!!.allowGroundTruthChange()
        )
        if (mapController!!.groundTruthLocation != null) {
            bundle.putParcelable(MapConstants.GROUND_TRUTH, mapController!!.groundTruthLocation)
        }
        super.onSaveInstanceState(bundle)
    }

    override fun onPause() {
        super.onPause()
        map!!.onPause()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates() {
        repository.receivingLocationUpdates
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    true -> onGnssStarted()
                    false -> onGnssStopped()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun addMapClickListener() {
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (mapController!!.mode != MapConstants.MODE_ACCURACY || !mapController!!.allowGroundTruthChange()) {
                    // Don't allow changes to the ground truth location, so don't pass taps to listener
                    return false
                }
                if (map != null) {
                    addGroundTruthMarker(MapUtils.makeLocation(p))
                    map!!.invalidate()
                }
                if (onMapClickListener != null) {
                    val location = Location("OnMapClick")
                    location.latitude = p.latitude
                    location.longitude = p.longitude
                    onMapClickListener!!.onMapClick(location)
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        map!!.overlays.add(MapEventsOverlay(mReceive))
    }

    @Throws(UnsupportedEncodingException::class)
    private fun setMapBoxTileSource(mapType: String) {
        // To prevent web scrapers from easily finding the key, we store it encoded
        val keyBase64 = "amdXY2VockFndXc2R1R1U3dQTmk="
        val key = MathUtils.fromBase64(keyBase64)
        val tileSource: ITileSource
        if (mapType == MAP_TYPE_SATELLITE) {
            // Use the Maptiler format
            tileSource = object : OnlineTileSourceBase(
                "Maptiler Satellite Hybrid",
                1,
                19,
                256,
                "",
                arrayOf("https://api.maptiler.com/maps/hybrid/")
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    return (baseUrl
                            + MapTileIndex.getZoom(pMapTileIndex)
                            + "/" + MapTileIndex.getX(pMapTileIndex)
                            + "/" + MapTileIndex.getY(pMapTileIndex)
                            + "@2x.jpg?key=" + key)
                }

                override fun getCopyrightNotice(): String {
                    return "© MapTiler © OpenStreetMap contributors"
                }
            }
            map!!.setTileSource(tileSource)
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

    private fun onGnssStarted() {
        gotFix = false
        observeFlows()
    }

    private fun onGnssStopped() {
        // Cancel updates (Note that these are canceled via scope in main Activity too,
        // otherwise updates won't stop because this Fragment doesn't get the switch UI event.
        // But cancel() here too for good practice)
        locationFlow?.cancel()
        sensorFlow?.cancel()
    }

    @ExperimentalCoroutinesApi
    private fun observeFlows() {
        observeLocationFlow()
        observeSensorFlow()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(GpsStatusFragment.TAG, "Map location: ${it.toNotificationTitle()}")
                onLocationChanged(it)
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeSensorFlow() {
        if (sensorFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        sensorFlow = repository.getSensorUpdates()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Map sensor: orientation ${it.values[0]}, tilt ${it.values[1]}")
                onOrientationChanged(it.values[0], it.values[1])
            }
            .launchIn(lifecycleScope)
    }

    private fun onLocationChanged(loc: Location) {
        if (map == null || !map!!.isLayoutOccurred || !map!!.isLaidOut) {
            return
        }
        val startPoint = GeoPoint(loc.latitude, loc.longitude)
        if (!gotFix) {
            // Zoom levels are a little different than Google Maps, so add 2 to our Google default to get the same view
            map!!.controller.setZoom((MapConstants.CAMERA_INITIAL_ZOOM + 2).toDouble())
            map!!.controller.setCenter(startPoint)
            gotFix = true
        }
        if (loc.hasAccuracy()) {
            // Add horizontal accuracy uncertainty as polygon
            if (horAccPolygon == null) {
                horAccPolygon = Polygon()
            }
            val circle = Polygon.pointsAsCircle(startPoint, loc.accuracy.toDouble())
            if (circle != null) {
                horAccPolygon!!.points = circle
                if (!map!!.overlays.contains(horAccPolygon)) {
                    horAccPolygon!!.strokeWidth = 0.5f
                    horAccPolygon!!.setOnClickListener { _: Polygon?, _: MapView?, _: GeoPoint? -> false }
                    horAccPolygon!!.fillColor =
                        ContextCompat.getColor(Application.app, R.color.horizontal_accuracy)
                    map!!.overlays.add(horAccPolygon)
                }
            }
        }
        if (mapController!!.mode == MapConstants.MODE_ACCURACY && lastLocation != null) {
            // Draw line between this and last location
            val drawn = drawPathLine(lastLocation!!, loc)
            if (drawn) {
                lastLocation = loc
            }
        }
        if (mapController!!.mode == MapConstants.MODE_ACCURACY && !mapController!!.allowGroundTruthChange() && mapController!!.groundTruthLocation != null) {
            // Draw error line between ground truth and calculated position
            val gt = MapUtils.makeGeoPoint(mapController!!.groundTruthLocation)
            val current = MapUtils.makeGeoPoint(loc)
            val points: List<GeoPoint> = listOf(gt, current)
            if (errorLine == null) {
                errorLine = Polyline()
                errorLine!!.color = Color.WHITE
                errorLine!!.setPoints(points)
                map!!.overlayManager.add(errorLine)
            } else {
                errorLine!!.setPoints(points)
            }
        }
        // Draw my location marker last so it's on top
        if (myLocationMarker == null) {
            myLocationMarker = Marker(map)
        }
        myLocationMarker!!.position = startPoint
        if (!map!!.overlays.contains(myLocationMarker)) {
            // This is the first fix when this fragment is active
            myLocationMarker!!.icon =
                ContextCompat.getDrawable(Application.app, R.drawable.my_location)
            myLocationMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            map!!.overlays.remove(myLocationMarker)
            map!!.overlays.add(myLocationMarker)
        }
        if (lastLocation == null) {
            lastLocation = loc
        }
        map!!.invalidate()
    }

    private fun onOrientationChanged(orientation: Double, tilt: Double) {
        // For performance reasons, only proceed if this fragment is visible
        if (!userVisibleHint) {
            return
        }
        if (map == null) {
            return
        }

        /*
        If we're in map mode, we have a location fix, and we have a preference to rotate the map based on sensors,
        then do the map camera reposition
        */if (mapController!!.mode == MapConstants.MODE_MAP && myLocationMarker != null && rotate) {
            map!!.mapOrientation = (-orientation).toFloat()
        }
        map!!.invalidate()
    }

    override fun addGroundTruthMarker(location: Location) {
        if (map == null) {
            return
        }
        if (groundTruthMarker == null) {
            groundTruthMarker = Marker(map)
        }
        groundTruthMarker!!.position = MapUtils.makeGeoPoint(location)
        groundTruthMarker!!.icon =
            ContextCompat.getDrawable(Application.app, R.drawable.ic_ground_truth)
        groundTruthMarker!!.title = Application.app.getString(R.string.ground_truth_marker_title)
        groundTruthMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        if (!map!!.overlays.contains(groundTruthMarker)) {
            map!!.overlays.add(groundTruthMarker)
        }
    }

    /**
     * Draws a line on the map between the two locations if its greater than a threshold value defined
     * by DRAW_LINE_THRESHOLD_METERS
     * @param loc1
     * @param loc2
     */
    override fun drawPathLine(loc1: Location, loc2: Location): Boolean {
        if (loc1.distanceTo(loc2) < MapConstants.DRAW_LINE_THRESHOLD_METERS) {
            return false
        }
        val line = Polyline()
        val points = listOf(MapUtils.makeGeoPoint(loc1), MapUtils.makeGeoPoint(loc2))
        line.setPoints(points)
        line.color = Color.RED
        line.width = 2.0f
        map!!.overlayManager.add(line)
        pathLines.add(line)
        return true
    }

    /**
     * Removes all path lines from the map
     */
    override fun removePathLines() {
        for (line in pathLines) {
            map!!.overlayManager.remove(line)
        }
        pathLines = ArrayList()
    }

    companion object {
        private const val TAG = "GpsMapFragment"
        private const val MAP_TYPE_SATELLITE = "mapbox.satellite"
        private const val MAP_TYPE_STREETS = "barbeau/cju1g27421a0w1fmvsy13tjfv"
    }
}