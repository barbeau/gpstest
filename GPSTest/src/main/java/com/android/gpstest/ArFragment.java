/*
 * Copyright (C) 2008-2019 The Android Open Source Project,
 * Sean J. Barbeau.  Derived from StarDroid, (c) 2010 Google
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
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.gpstest.ar.ArRenderer;
import com.android.gpstest.ar.AstronomerModel;
import com.android.gpstest.ar.ButtonLayerView;
import com.android.gpstest.ar.ControllerGroup;
import com.android.gpstest.ar.FullscreenControlsManager;
import com.android.gpstest.ar.LayerManager;
import com.android.gpstest.ar.RendererController;
import com.android.gpstest.ar.Vector3;
import com.android.gpstest.ar.touch.DragRotateZoomGestureDetector;
import com.android.gpstest.ar.touch.GestureInterpreter;
import com.android.gpstest.ar.touch.MapMover;
import com.android.gpstest.ar.util.AbstractUpdateClosure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

public class ArFragment extends Fragment implements GpsTestListener {

    public final static String TAG = "ArFragment";

    private GLSurfaceView skyView;
    AstronomerModel model;
    private RendererController rendererController;
    LayerManager layerManager;
    ControllerGroup controller;
    private ImageButton cancelSearchButton;
    private FullscreenControlsManager fullscreenControlsManager;
    private GestureDetector gestureDetector;
    private DragRotateZoomGestureDetector dragZoomRotateDetector;
    private boolean searchMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ar_renderer, container, false);

        skyView = v.findViewById(R.id.ar_renderer_view);
        // We don't want a depth buffer.
        skyView.setEGLConfigChooser(false);
        ArRenderer renderer = new ArRenderer(getActivity().getResources());
        skyView.setRenderer(renderer);

        rendererController = new RendererController(renderer, skyView);
        // The renderer will now call back every frame to get model updates.
        rendererController.addUpdateClosure(
                new RendererModelUpdateClosure(model, rendererController, Application.getPrefs()));

        Log.i(TAG, "Setting layers @ " + System.currentTimeMillis());
        layerManager.registerWithRenderer(rendererController);
        Log.i(TAG, "Set up controllers @ " + System.currentTimeMillis());
        controller.setModel(model);
        wireUpScreenControls(v); // TODO(johntaylor) move these?

        GpsTestActivity.getInstance().addListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void onLocationChanged(Location loc) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public void gpsStart() {
    }

    public void gpsStop() {
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
        //mSkyView.setStarted();
    }

    @Override
    public void onGnssStopped() {
        //mSkyView.setStopped();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        //mSkyView.setGnssMeasurementEvent(event);
    }

    @Deprecated
    public void onGpsStatusChanged(int event, GpsStatus status) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                //mSkyView.setStarted();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                //mSkyView.setStopped();
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                //mSkyView.setSats(status);
                break;
        }
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        // For performance reasons, only proceed if this fragment is visible
        if (!getUserVisibleHint()) {
            return;
        }

//        if (mSkyView != null) {
//            mSkyView.onOrientationChanged(orientation, tilt);
//        }
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
    }

    /**
     * Passed to the renderer to get per-frame updates from the model.
     *
     * @author John Taylor
     */
    private static final class RendererModelUpdateClosure extends AbstractUpdateClosure {
        private RendererController rendererController;
        private AstronomerModel model;
        private boolean horizontalRotation;

        public RendererModelUpdateClosure(AstronomerModel model,
                                          RendererController rendererController, SharedPreferences sharedPreferences) {
            this.model = model;
            this.rendererController = rendererController;
            this.horizontalRotation = sharedPreferences.getBoolean(Application.get().getString(R.string.pref_key_rotate_horizon), false);
            model.setHorizontalRotation(this.horizontalRotation);
        }

        @Override
        public void run() {
            AstronomerModel.Pointing pointing = model.getPointing();
            float directionX = pointing.getLineOfSightX();
            float directionY = pointing.getLineOfSightY();
            float directionZ = pointing.getLineOfSightZ();

            float upX = pointing.getPerpendicularX();
            float upY = pointing.getPerpendicularY();
            float upZ = pointing.getPerpendicularZ();

            rendererController.queueSetViewOrientation(directionX, directionY, directionZ, upX, upY, upZ);

            Vector3 up = model.getPhoneUpDirection();
            rendererController.queueTextAngle((float) Math.atan2(up.x, up.y));
            rendererController.queueViewerUpDirection(model.getZenith().copy());

            float fieldOfView = model.getFieldOfView();
            rendererController.queueFieldOfView(fieldOfView);
        }
    }

    private void wireUpScreenControls(View v) {
//        cancelSearchButton = (ImageButton) v.findViewById(R.id.cancel_search_button);
//        // TODO(johntaylor): move to set this in the XML once we don't support 1.5
//        cancelSearchButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cancelSearch(v);
//            }
//        });

        ButtonLayerView providerButtons = (ButtonLayerView) v.findViewById(R.id.layer_buttons_control);

        int numChildren = providerButtons.getChildCount();
        List<View> buttonViews = new ArrayList<>();
        for (int i = 0; i < numChildren; ++i) {
            ImageButton button = (ImageButton) providerButtons.getChildAt(i);
            buttonViews.add(button);
        }
        buttonViews.add(v.findViewById(R.id.manual_auto_toggle));
        ButtonLayerView manualButtonLayer = (ButtonLayerView) v.findViewById(
                R.id.layer_manual_auto_toggle);

        fullscreenControlsManager = new FullscreenControlsManager(
                getActivity(),
                v.findViewById(R.id.main_ar_view),
                Arrays.asList(manualButtonLayer, providerButtons),
                buttonViews);

        MapMover mapMover = new MapMover(model, controller, getActivity());

        gestureDetector = new GestureDetector(getActivity(), new GestureInterpreter(
                fullscreenControlsManager, mapMover));
        dragZoomRotateDetector = new DragRotateZoomGestureDetector(mapMover);
    }

    private void cancelSearch(View v) {
//        View searchControlBar = v.findViewById(R.id.search_control_bar);
//        searchControlBar.setVisibility(View.INVISIBLE);
//        rendererController.queueDisableSearchOverlay();
//        searchMode = false;
    }
}
