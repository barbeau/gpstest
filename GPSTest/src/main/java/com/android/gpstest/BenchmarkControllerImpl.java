/*
 * Copyright (C) 2018 Sean J. Barbeau
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

import android.animation.LayoutTransition;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import androidx.constraintlayout.motion.widget.MotionLayout;

import static android.text.TextUtils.isEmpty;

/**
 * This class encapsulates logic used for the benchmarking feature that compares a user-entered
 * ground truth value against the GPS location.
 */
public class BenchmarkControllerImpl implements BenchmarkController {

    private static final String BENCHMARK_CARD_COLLAPSED = "ground_truth_card_collapsed";

    private boolean mBenchmarkCardCollapsed = false;

    MaterialCardView mGroundTruthCardView;

    MotionLayout mMotionLayout;

    SlidingUpPanelLayout mSlidingPanel;

    Location mGroundTruthLocation;

    public BenchmarkControllerImpl(View v, Bundle savedInstanceState) {
        mSlidingPanel = v.findViewById(R.id.bottom_sliding_layout);
        mGroundTruthCardView = v.findViewById(R.id.benchmark_card);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mGroundTruthCardView.getLayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mGroundTruthCardView.getLayoutTransition()
                    .enableTransitionType(LayoutTransition.CHANGING);
        }

        if (savedInstanceState != null) {
            // Activity is being restarted and has previous state (e.g., user rotated device)
            mBenchmarkCardCollapsed = savedInstanceState.getBoolean(BENCHMARK_CARD_COLLAPSED, false);
        }

        mMotionLayout = v.findViewById(R.id.motion_layout);
        Button saveGroundTruth = v.findViewById(R.id.save);
        TextInputLayout latText = v.findViewById(R.id.ground_truth_lat);
        TextInputLayout longText = v.findViewById(R.id.ground_truth_long);
        TextInputLayout altText = v.findViewById(R.id.ground_truth_alt);
        mMotionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.expanded) {
                    saveGroundTruth.setText(Application.get().getString(R.string.save));
                    latText.setEnabled(true);
                    longText.setEnabled(true);
                    altText.setEnabled(true);
                    latText.setFocusable(true);
                    longText.setFocusable(true);
                    altText.setFocusable(true);
                } else {
                    // Collapsed
                    saveGroundTruth.setText(Application.get().getString(R.string.edit));
                    latText.setEnabled(false);
                    longText.setEnabled(false);
                    altText.setEnabled(false);
                    latText.setFocusable(false);
                    longText.setFocusable(false);
                    altText.setFocusable(false);
                }
            }
        });

        // TODO - set initial state of card and motion layout depending on savedInstanceState

        saveGroundTruth.setOnClickListener(view -> {
            if (!mBenchmarkCardCollapsed) {
                // Save Ground Truth
                if (mGroundTruthLocation == null) {
                    mGroundTruthLocation = new Location("ground_truth");
                }
                if (!isEmpty(latText.getEditText().getText().toString()) && !isEmpty(longText.getEditText().getText().toString())) {
                    mGroundTruthLocation.setLatitude(Double.valueOf(latText.getEditText().getText().toString()));
                    mGroundTruthLocation.setLongitude(Double.valueOf(longText.getEditText().getText().toString()));
                }
                if (!isEmpty(altText.getEditText().getText().toString())) {
                    mGroundTruthLocation.setAltitude(Double.valueOf(altText.getEditText().getText().toString()));
                }

                // Collapse card
                mMotionLayout.transitionToEnd();
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height_collapsed);
                mGroundTruthCardView.setLayoutParams(lp);
                mBenchmarkCardCollapsed = true;

                // Show sliding panel if it's not visible
                if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            } else {
                // Expand card
                mMotionLayout.transitionToStart();
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height);
                mGroundTruthCardView.setLayoutParams(lp);
                mBenchmarkCardCollapsed = false;

                // Collapse sliding panel if it's anchored so there is room
                if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save current benchmark card state
        outState.putBoolean(BENCHMARK_CARD_COLLAPSED, mBenchmarkCardCollapsed);
    }

    public void show() {
        if (mGroundTruthCardView != null) {
            mGroundTruthCardView.setVisibility(View.VISIBLE);
        }
        if (mMotionLayout != null) {
            mMotionLayout.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (mGroundTruthCardView != null) {
            mGroundTruthCardView.setVisibility(View.GONE);
        }
        if (mMotionLayout != null) {
            mMotionLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void gpsStart() {

    }

    @Override
    public void gpsStop() {

    }

    @Override
    public void onGpsStatusChanged(int event, GpsStatus status) {

    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

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
    public void onOrientationChanged(double orientation, double tilt) {

    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
