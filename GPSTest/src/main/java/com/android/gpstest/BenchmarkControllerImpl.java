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
import android.widget.TextView;

import com.android.gpstest.model.MeasuredError;
import com.android.gpstest.util.BenchmarkUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import androidx.constraintlayout.motion.widget.MotionLayout;

import static android.text.TextUtils.isEmpty;

/**
 * This class encapsulates logic used for the benchmarking feature that compares a user-entered
 * ground truth value against the GPS location.
 */
public class BenchmarkControllerImpl implements BenchmarkController, OnMapClickListener {

    private static final String TAG = "BenchmarkCntlrImpl";

    private static final String BENCHMARK_CARD_COLLAPSED = "ground_truth_card_collapsed";

    private boolean mBenchmarkCardCollapsed = false;

    MaterialCardView mGroundTruthCardView;

    MotionLayout mMotionLayout;

    SlidingUpPanelLayout mSlidingPanel;

    SlidingUpPanelLayout.PanelState mLastPanelState;

    Location mGroundTruthLocation;

    TextView mError, mVertError, mAvgError, mAvgVertError, mErrorLabel, mLeftDivider, mRightDivider;
    TextInputLayout mLatText, mLongText, mAltText;

    private Listener mListener;

    public BenchmarkControllerImpl(View v, Bundle savedInstanceState) {
        mSlidingPanel = v.findViewById(R.id.bottom_sliding_layout);
        mError = v.findViewById(R.id.error);
        mVertError = v.findViewById(R.id.vert_error);
        mAvgError = v.findViewById(R.id.avg_error);
        mAvgVertError = v.findViewById(R.id.avg_vert_error);
        mErrorLabel = v.findViewById(R.id.error_label);
        mLeftDivider = v.findViewById(R.id.divider_left);
        mRightDivider = v.findViewById(R.id.divider_right);
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
        mLatText = v.findViewById(R.id.ground_truth_lat);
        mLongText = v.findViewById(R.id.ground_truth_long);
        mAltText = v.findViewById(R.id.ground_truth_alt);
        mMotionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.expanded) {
                    saveGroundTruth.setText(Application.get().getString(R.string.save));
                    mLatText.setEnabled(true);
                    mLongText.setEnabled(true);
                    mAltText.setEnabled(true);
                    mLatText.setFocusable(true);
                    mLongText.setFocusable(true);
                    mAltText.setFocusable(true);
                } else {
                    // Collapsed
                    saveGroundTruth.setText(Application.get().getString(R.string.edit));
                    mLatText.setEnabled(false);
                    mLongText.setEnabled(false);
                    mAltText.setEnabled(false);
                    mLatText.setFocusable(false);
                    mLongText.setFocusable(false);
                    mAltText.setFocusable(false);
                }
            }
        });

        // TODO - set initial state of card and motion layout depending on savedInstanceState

        saveGroundTruth.setOnClickListener(view -> {
            if (!mBenchmarkCardCollapsed) {
                // TODO - if lat and long aren't filled, show error

                // Save Ground Truth
                if (mGroundTruthLocation == null) {
                    mGroundTruthLocation = new Location("ground_truth");
                }
                if (!isEmpty(mLatText.getEditText().getText().toString()) && !isEmpty(mLongText.getEditText().getText().toString())) {
                    mGroundTruthLocation.setLatitude(Double.valueOf(mLatText.getEditText().getText().toString()));
                    mGroundTruthLocation.setLongitude(Double.valueOf(mLongText.getEditText().getText().toString()));
                }
                if (!isEmpty(mAltText.getEditText().getText().toString())) {
                    mGroundTruthLocation.setAltitude(Double.valueOf(mAltText.getEditText().getText().toString()));
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
                if (mListener != null) {
                    mListener.onAllowGroundTruthEditChanged(false);
                }
            } else {
                // Expand card to allow editing ground truth
                mMotionLayout.transitionToStart();
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height);
                mGroundTruthCardView.setLayoutParams(lp);
                mBenchmarkCardCollapsed = false;

                // Collapse sliding panel if it's anchored so there is room
                if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }

                if (mListener != null) {
                    mListener.onAllowGroundTruthEditChanged(true);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save current benchmark card state
        outState.putBoolean(BENCHMARK_CARD_COLLAPSED, mBenchmarkCardCollapsed);
    }

    /**
     * Sets a lister that will be updated when benchmark controller events are fired
     * @param listener a lister that will be updated when benchmark controller events are fired
     */
    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void show() {
        if (mGroundTruthCardView != null) {
            mGroundTruthCardView.setVisibility(View.VISIBLE);
        }
        if (mMotionLayout != null) {
            mMotionLayout.setVisibility(View.VISIBLE);
        }
        if (mSlidingPanel != null && mLastPanelState != null) {
            mSlidingPanel.setPanelState(mLastPanelState);
        }
    }

    public void hide() {
        if (mGroundTruthCardView != null) {
            mGroundTruthCardView.setVisibility(View.GONE);
        }
        if (mMotionLayout != null) {
            mMotionLayout.setVisibility(View.GONE);
        }
        if (mSlidingPanel != null) {
            mLastPanelState = mSlidingPanel.getPanelState();
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
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
        if (mGroundTruthLocation == null) {
            return;
        }
        MeasuredError error = BenchmarkUtils.Companion.measureError(location, mGroundTruthLocation);
        if (mError != null) {
            mError.setText(Application.get().getString(R.string.benchmark_error, error.getError()));
        }
        if (mVertError != null && !Double.isNaN(error.getVertError())) {
            mErrorLabel.setText(R.string.horizontal_vertical_error_label);
            mLeftDivider.setVisibility(View.VISIBLE);
            mRightDivider.setVisibility(View.VISIBLE);
            mVertError.setVisibility(View.VISIBLE);
            mVertError.setText(Application.get().getString(R.string.benchmark_error, error.getVertError()));
        } else {
            mErrorLabel.setText(R.string.horizontal_error_label);
            mLeftDivider.setVisibility(View.GONE);
            mRightDivider.setVisibility(View.GONE);
            mVertError.setVisibility(View.GONE);
        }
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

    @Override
    public void onMapClick(Location location) {
        mGroundTruthLocation = location;
        if (!mBenchmarkCardCollapsed) {
            mLatText.getEditText().setText(Application.get().getString(R.string.benchmark_lat_long, location.getLatitude()));
            mLongText.getEditText().setText(Application.get().getString(R.string.benchmark_lat_long, location.getLongitude()));

            if (location.hasAltitude()) {
                mAltText.getEditText().setText(Application.get().getString(R.string.benchmark_alt, location.getAltitude()));
            }
        }
    }
}
