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
package com.android.gpstest.ui

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.Application
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.R
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.util.MathUtils
import com.android.gpstest.library.util.PreferenceUtil
import com.android.gpstest.map.MapConstants
import com.android.gpstest.map.MapViewModelController
import com.android.gpstest.map.MapViewModelController.MapInterface
import com.android.gpstest.map.OnMapClickListener
import com.android.gpstest.util.MapUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import com.google.maps.android.ktx.awaitMap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : SupportMapFragment(), View.OnClickListener, LocationSource,
    OnCameraChangeListener, GoogleMap.OnMapClickListener, OnMapLongClickListener,
    OnMyLocationButtonClickListener, MapInterface {
    private var savedInstanceState: Bundle? = null
    private var map: GoogleMap? = null
    private var latLng: LatLng? = null
    private var listener //Used to update the map with new location
            : OnLocationChangedListener? = null

    // Camera control
    private var lastMapTouchTime: Long = 0
    private var lastCameraPosition: CameraPosition? = null
    private var gotFix = false

    // User preferences for map rotation and tilt based on sensors
    private var rotate = false
    private var tiltEnabled = false
    private var onMapClickListener: OnMapClickListener? = null
    private var groundTruthMarker: Marker? = null
    private var errorLine: Polyline? = null
    private var lastLocation: Location? = null
    private var pathLines: MutableList<Polyline> = ArrayList()
    var mapController: MapViewModelController? = null

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null
    private var sensorFlow: Job? = null

    // Preference listener that will cancel the above flows when the user turns off tracking via UI
    private val trackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        PreferenceUtil.newStopTrackingListener ({ onGnssStopped() }, prefs)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        lastLocation = null

        Application.prefs.registerOnSharedPreferenceChangeListener(trackingListener)

        if (isGooglePlayServicesInstalled) {
            // Save the savedInstanceState
            this.savedInstanceState = savedInstanceState
            val mapFragment = this
            lifecycle.coroutineScope.launchWhenCreated {
                val googleMap = awaitMap()
                setupMap(mapFragment, googleMap)
                observeLocationUpdateStates()
            }
        } else {
            val sp = Application.prefs
            if (!sp.getBoolean(MapConstants.PREFERENCE_SHOWED_DIALOG, false)) {
                val builder = AlertDialog.Builder(
                    requireActivity()
                )
                builder.setMessage(getString(R.string.please_install_google_maps))
                builder.setPositiveButton(
                    getString(R.string.install)
                ) { _, _ ->
                    sp.edit().putBoolean(MapConstants.PREFERENCE_SHOWED_DIALOG, true).apply()
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "market://details?id=com.google.android.apps.maps"
                        )
                    )
                    startActivity(intent)
                }
                builder.setNegativeButton(
                    getString(R.string.no_thanks)
                ) { _, _ ->
                    sp.edit().putBoolean(MapConstants.PREFERENCE_SHOWED_DIALOG, true).apply()
                }
                val dialog = builder.create()
                dialog.show()
            }
        }
        mapController = MapViewModelController(activity, this)
        return v
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

    override fun onResume() {
        checkMapPreferences()
        super.onResume()
    }

    override fun onClick(v: View) {}

    private fun setupMap(mapFragment: MapFragment, googleMap: GoogleMap) {
        map = googleMap
        mapController!!.restoreState(savedInstanceState, arguments, groundTruthMarker == null)
        checkMapPreferences()

        // Show the location on the map
        try {
            googleMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Log.e(mapController!!.mode, "Tried to initialize my location on Google Map - $e")
        }
        // Set location source
        googleMap.setLocationSource(mapFragment)
        // Listener for camera changes
        googleMap.setOnCameraChangeListener(mapFragment)
        // Listener for map / My Location button clicks, to disengage map camera control
        googleMap.setOnMapClickListener(mapFragment)
        googleMap.setOnMapLongClickListener(mapFragment)
        googleMap.setOnMyLocationButtonClickListener(mapFragment)
        googleMap.uiSettings.isMapToolbarEnabled = false
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

    @ExperimentalCoroutinesApi
    private fun onGnssStarted() {
        gotFix = false
        observeFlows()
    }

    private fun onGnssStopped() {
        cancelFlows()
    }

    private fun cancelFlows() {
        // Cancel updates (Note that these are canceled via trackingListener preference listener
        // in the case where updates are stopped from the Activity UI switch.
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
        // Update real-time location on map
        listener?.onLocationChanged(loc)

        val latLng = LatLng(loc.latitude, loc.longitude)
        this.latLng = latLng
        val googleMap = map
        if (googleMap != null) {
            // Get bounds for detection of real-time location within bounds
            val bounds = googleMap.projection.visibleRegion.latLngBounds
            if (!gotFix &&
                (!bounds.contains(latLng) ||
                        googleMap.cameraPosition.zoom < googleMap.maxZoomLevel / 2)
            ) {
                val tilt =
                    if (mapController!!.mode == MapConstants.MODE_MAP) MapConstants.CAMERA_INITIAL_TILT_MAP else MapConstants.CAMERA_INITIAL_TILT_ACCURACY
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(MapConstants.CAMERA_INITIAL_ZOOM)
                    .bearing(MapConstants.CAMERA_INITIAL_BEARING)
                    .tilt(tilt)
                    .build()
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
            gotFix = true
            if (mapController!!.mode == MapConstants.MODE_ACCURACY && !mapController!!.allowGroundTruthChange() && mapController!!.groundTruthLocation != null) {
                // Draw error line between ground truth and calculated position
                val gt = MapUtils.makeLatLng(mapController!!.groundTruthLocation)
                val current = MapUtils.makeLatLng(loc)
                if (errorLine == null) {
                    errorLine = googleMap.addPolyline(
                        PolylineOptions()
                            .add(gt, current)
                            .color(Color.WHITE)
                            .geodesic(true)
                    )
                } else {
                    errorLine!!.points = listOf(gt, current)
                }
            }
            if (mapController!!.mode == MapConstants.MODE_ACCURACY && lastLocation != null) {
                // Draw line between this and last location
                val drawn = drawPathLine(lastLocation!!, loc)
                if (drawn) {
                    lastLocation = loc
                }
            }
        }
        if (lastLocation == null) {
            lastLocation = loc
        }
    }

    private fun onOrientationChanged(orientation: Double, tilt: Double) {
        // For performance reasons, only proceed if this fragment is visible
        if (!userVisibleHint) {
            return
        }
        // Only proceed if map is not null and we're in MAP mode
        if (map == null || mapController!!.mode != MapConstants.MODE_MAP) {
            return
        }
        var mutableTilt = tilt

        /*
        If we have a location fix, and we have a preference to rotate the map based on sensors,
        and the user hasn't touched the map lately, then do the map camera reposition
        */
        if (latLng != null && rotate
            && (System.currentTimeMillis() - lastMapTouchTime
                    > MapConstants.MOVE_MAP_INTERACTION_THRESHOLD)
        ) {
            if (!tiltEnabled || java.lang.Double.isNaN(mutableTilt)) {
                mutableTilt =
                    if (lastCameraPosition != null) lastCameraPosition!!.tilt.toDouble() else 0.toDouble()
            }
            val clampedTilt = MathUtils.clamp(
                MapConstants.CAMERA_MIN_TILT.toDouble(),
                mutableTilt,
                MapConstants.CAMERA_MAX_TILT.toDouble()
            ).toFloat()
            val offset =
                MapConstants.TARGET_OFFSET_METERS * (clampedTilt / MapConstants.CAMERA_MAX_TILT)
            val cameraPosition = CameraPosition.builder().tilt(clampedTilt).bearing(
                orientation.toFloat()
            )
                .zoom((MapConstants.CAMERA_ANCHOR_ZOOM + mutableTilt / MapConstants.CAMERA_MAX_TILT).toFloat())
                .target(
                    if (tiltEnabled) SphericalUtil.computeOffset(
                        latLng,
                        offset,
                        orientation
                    ) else latLng
                ).build()
            map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    /**
     * Maps V2 Location updates
     */
    override fun activate(listener: OnLocationChangedListener) {
        this.listener = listener
    }

    /**
     * Maps V2 Location updates
     */
    override fun deactivate() {
        listener = null
    }

    override fun onCameraChange(cameraPosition: CameraPosition) {
        if (System.currentTimeMillis() - lastMapTouchTime < MapConstants.MOVE_MAP_INTERACTION_THRESHOLD) {
            /*
            If the user recently interacted with the map (causing a camera change), extend the
            touch time before automatic map movements based on sensors will kick in
            */
            lastMapTouchTime = System.currentTimeMillis()
        }
        lastCameraPosition = cameraPosition
    }

    override fun onMapClick(latLng: LatLng) {
        lastMapTouchTime = System.currentTimeMillis()
        if (mapController!!.mode != MapConstants.MODE_ACCURACY || !mapController!!.allowGroundTruthChange()) {
            // Don't allow changes to the ground truth location, so don't pass taps to listener
            return
        }
        if (map != null) {
            addGroundTruthMarker(MapUtils.makeLocation(latLng))
        }
        if (onMapClickListener != null) {
            val location = Location("OnMapClick")
            location.latitude = latLng.latitude
            location.longitude = latLng.longitude
            onMapClickListener!!.onMapClick(location)
        }
    }

    override fun addGroundTruthMarker(location: Location) {
        if (map == null) {
            return
        }
        val latLng = MapUtils.makeLatLng(location)
        if (groundTruthMarker == null) {
            groundTruthMarker = map!!.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(Application.app.getString(R.string.ground_truth_marker_title))
            )
        } else {
            groundTruthMarker!!.position = latLng
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        lastMapTouchTime = System.currentTimeMillis()
    }

    override fun onMyLocationButtonClick(): Boolean {
        lastMapTouchTime = System.currentTimeMillis()
        // Return false, so button still functions as normal
        return false
    }

    /**
     * Sets the listener that should receive map click events
     * @param listener the listener that should receive map click events
     */
    fun setOnMapClickListener(listener: OnMapClickListener?) {
        onMapClickListener = listener
    }

    private fun checkMapPreferences() {
        val settings = Application.prefs
        if (map != null && mapController!!.mode == MapConstants.MODE_MAP) {
            if (map!!.mapType !=
                settings.getString(
                    getString(R.string.pref_key_map_type),
                    GoogleMap.MAP_TYPE_NORMAL.toString()
                )
                    ?.toInt() ?: GoogleMap.MAP_TYPE_NORMAL
            ) {
                map!!.mapType = settings.getString(
                    getString(R.string.pref_key_map_type),
                    GoogleMap.MAP_TYPE_NORMAL.toString()
                )
                    ?.toInt() ?: GoogleMap.MAP_TYPE_NORMAL
            }
        } else if (map != null && mapController!!.mode == MapConstants.MODE_ACCURACY) {
            map!!.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        if (mapController!!.mode == MapConstants.MODE_MAP) {
            rotate = settings
                .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), true)
            tiltEnabled = settings.getBoolean(getString(R.string.pref_key_tilt_map_with_sensors), true)
        }
        val useDarkTheme =
            Application.prefs.getBoolean(getString(R.string.pref_key_dark_theme), false)
        if (map != null && activity != null && useDarkTheme) {
            map!!.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    activity, R.raw.dark_theme
                )
            )
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
        val line = map!!.addPolyline(
            PolylineOptions()
                .add(MapUtils.makeLatLng(loc1), MapUtils.makeLatLng(loc2))
                .color(Color.RED)
                .width(2.0f)
                .geodesic(true)
        )
        pathLines.add(line)
        return true
    }

    /**
     * Removes all path lines from the map
     */
    override fun removePathLines() {
        for (line in pathLines) {
            line.remove()
        }
        pathLines = ArrayList()
    }

    companion object {
        private const val TAG = "GpsMapFragment"

        /**
         * Returns true if Google Play Services is available, false if it is not
         */
        private val isGooglePlayServicesInstalled: Boolean
            get() = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(Application.app) == ConnectionResult.SUCCESS
    }
}