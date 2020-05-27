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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.android.gpstest.chart.DistanceValueFormatter;
import com.android.gpstest.model.AvgError;
import com.android.gpstest.model.MeasuredError;
import com.android.gpstest.util.IOUtils;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.PreferenceUtils;
import com.android.gpstest.util.UIUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import static android.text.TextUtils.isEmpty;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * This class encapsulates logic used for the benchmarking feature that compares a user-entered
 * ground truth value against the GPS location.
 */
public class BenchmarkControllerImpl implements BenchmarkController {

    private static final String TAG = "BenchmarkCntlrImpl";

    private static final String GROUND_TRUTH_LAT = "ground_truth_card_lat";

    private static final String GROUND_TRUTH_LONG = "ground_truth_card_long";

    private static final String GROUND_TRUTH_ALT = "ground_truth_card_alt";

    private static final int ERROR_SET = 0;

    private static final int ESTIMATED_ACCURACY_SET = 1;

    private static final float UNIT_VERT_BIAS_HOR_ERROR_ONLY = 0.582f;

    private static final float UNIT_VERT_BIAS_INCL_VERT_ERROR = 0.25f;

    MaterialCardView mGroundTruthCardView, mVerticalErrorCardView;

    MotionLayout mMotionLayout;

    TextView mErrorView, mVertErrorView, mAvgErrorView, mAvgVertErrorView, mErrorLabel, mAvgErrorLabel, mLeftDivider, mRightDivider, mErrorUnit, mAvgErrorUnit;
    TextInputLayout mLatText, mLongText, mAltText;
    Button mSaveGroundTruth;
    MaterialButton mQrCode;

    SlidingUpPanelLayout mSlidingPanel;

    SlidingUpPanelLayout.PanelState mLastPanelState;

    ViewGroup mSlidingPanelHeader;

    LineChart mErrorChart, mVertErrorChart;

    int mChartTextColor;

    String mPrefDistanceUnits;

    private static final String METERS = Application.get().getResources().getStringArray(R.array.preferred_distance_units_values)[0];

    BenchmarkViewModel mViewModel;

    private final Observer<Boolean> mAllowGroundTruthEditObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable final Boolean allowEdit) {
            if (!allowEdit) {
                if (mViewModel.getGroundTruthLocation().getValue().hasAltitude()) {
                    // Set default text size and align units properly
                    mErrorView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Application.get().getResources().getDimension(R.dimen.ground_truth_sliding_header_vert_text_size));
                    mAvgErrorView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Application.get().getResources().getDimension(R.dimen.ground_truth_sliding_header_vert_text_size));
                    UIUtils.setVerticalBias(mErrorUnit, UNIT_VERT_BIAS_INCL_VERT_ERROR);
                    UIUtils.setVerticalBias(mAvgErrorUnit, UNIT_VERT_BIAS_INCL_VERT_ERROR);
                } else {
                    // No altitude provided - Hide vertical error chart card
                    mVerticalErrorCardView.setVisibility(GONE);
                    // Set default text size and align units properly
                    mErrorView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Application.get().getResources().getDimension(R.dimen.ground_truth_sliding_header_error_text_size));
                    mAvgErrorView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Application.get().getResources().getDimension(R.dimen.ground_truth_sliding_header_error_text_size));
                    UIUtils.setVerticalBias(mErrorUnit, UNIT_VERT_BIAS_HOR_ERROR_ONLY);
                    UIUtils.setVerticalBias(mAvgErrorUnit, UNIT_VERT_BIAS_HOR_ERROR_ONLY);
                }

                // Collapse card - we have to set height on card manually because card doesn't auto-collapse right when views are within card container
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mGroundTruthCardView.getLayoutParams();
                mMotionLayout.transitionToEnd();
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height_collapsed);
                mGroundTruthCardView.setLayoutParams(lp);

                // Show sliding panel if we're showing the Accuracy fragment and the sliding panel isn't visible
                if (mGroundTruthCardView.getVisibility() == VISIBLE && mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                } else if (mGroundTruthCardView.getVisibility() == GONE) {
                    // A test was started using a saved ground truth, but the user started the app at another fragment other than Accuracy.  Set the last
                    // sliding panel state to COLLAPSED so it becomes visible when the user switches to the Accuracy fragment
                    mLastPanelState = SlidingUpPanelLayout.PanelState.COLLAPSED;
                }
            } else {
                // Edits are allowed
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mGroundTruthCardView.getLayoutParams();
                // Expand card to allow editing ground truth
                mMotionLayout.transitionToStart();
                // We have to set height on card manually because it doesn't auto-expand right when views are within card container
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height);
                mGroundTruthCardView.setLayoutParams(lp);

                // Collapse sliding panel if it's anchored so there is room
                if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }
        }
    };

    private final Observer<Pair<Location, MeasuredError>> mLocationErrorPairObserver = new Observer<Pair<Location, MeasuredError>>() {
        @Override
        public void onChanged(@Nullable final Pair<Location, MeasuredError> locationErrorPair) {
            if (locationErrorPair == null || locationErrorPair.first == null || locationErrorPair.second == null) {
                return;
            }
            Location location = locationErrorPair.first;
            MeasuredError error = locationErrorPair.second;
            if (mErrorView != null) {
                mErrorUnit.setVisibility(VISIBLE);
                mErrorView.setVisibility(VISIBLE);
                if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
                    mErrorView.setText(Application.get().getString(R.string.benchmark_error, error.getError()));
                    mErrorUnit.setText(Application.get().getString(R.string.meters_abbreviation));
                } else {
                    // Feet
                    mErrorView.setText(Application.get().getString(R.string.benchmark_error, UIUtils.toFeet(error.getError())));
                    mErrorUnit.setText(Application.get().getString(R.string.feet_abbreviation));
                }
            }
            if (mVertErrorView != null && !Double.isNaN(error.getVertError())) {
                // Vertical errors
                mErrorLabel.setText(R.string.horizontal_vertical_error_label);
                mLeftDivider.setVisibility(VISIBLE);
                mRightDivider.setVisibility(VISIBLE);
                mVertErrorView.setVisibility(VISIBLE);
                if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
                    mVertErrorView.setText(Application.get().getString(R.string.benchmark_error, Math.abs(error.getVertError())));
                } else {
                    // Feet
                    mVertErrorView.setText(Application.get().getString(R.string.benchmark_error, UIUtils.toFeet(Math.abs(error.getVertError()))));
                }
                mVerticalErrorCardView.setVisibility(VISIBLE);
            } else {
                // Hide any vertical error indication
                mErrorLabel.setText(R.string.horizontal_error_label);
                mLeftDivider.setVisibility(GONE);
                mRightDivider.setVisibility(GONE);
                mVertErrorView.setVisibility(GONE);
                mVerticalErrorCardView.setVisibility(GONE);
            }
            addErrorToGraphs(mViewModel.getAvgError().getValue().getCount(), error, location);
        }
    };

    private final Observer<AvgError> mAvgErrorObserver = new Observer<AvgError>() {
        @Override
        public void onChanged(@Nullable final AvgError avgError) {
            if (mAvgErrorView != null) {
                mAvgErrorUnit.setVisibility(VISIBLE);
                mAvgErrorView.setVisibility(VISIBLE);
                if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
                    mAvgErrorView.setText(Application.get().getString(R.string.benchmark_error, avgError.getAvgError()));
                    mAvgErrorUnit.setText(Application.get().getString(R.string.meters_abbreviation));
                } else {
                    // Feet
                    mAvgErrorView.setText(Application.get().getString(R.string.benchmark_error, UIUtils.toFeet(avgError.getAvgError())));
                    mAvgErrorUnit.setText(Application.get().getString(R.string.feet_abbreviation));
                }
                mAvgErrorLabel.setText(Application.get().getString(R.string.avg_error_label, avgError.getCount()));
            }
            if (mAvgVertErrorView != null && !Double.isNaN(avgError.getAvgVertAbsError())) {
                // Vertical errors
                mAvgVertErrorView.setVisibility(VISIBLE);
                if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
                    mAvgVertErrorView.setText(Application.get().getString(R.string.benchmark_error, avgError.getAvgVertAbsError()));
                } else {
                    mAvgVertErrorView.setText(Application.get().getString(R.string.benchmark_error, UIUtils.toFeet(avgError.getAvgVertAbsError())));
                }
            } else {
                // Hide any vertical error indication
                mAvgVertErrorView.setVisibility(GONE);
            }
        }
    };

    public BenchmarkControllerImpl(AppCompatActivity activity, View v) {
        if (Application.getPrefs().getBoolean(activity.getString(R.string.pref_key_dark_theme), false)) {
            // Dark theme
            mChartTextColor = ContextCompat.getColor(activity, R.color.body_text_1_dark);
        } else {
            // Light theme
            mChartTextColor = ContextCompat.getColor(activity, R.color.body_text_1_light);
        }
        mSlidingPanel = v.findViewById(R.id.bottom_sliding_layout);
        mSlidingPanelHeader = v.findViewById(R.id.sliding_panel_header);
        mErrorView = v.findViewById(R.id.error);
        mVertErrorView = v.findViewById(R.id.vert_error);
        mAvgErrorView = v.findViewById(R.id.avg_error);
        mAvgVertErrorView = v.findViewById(R.id.avg_vert_error);
        mErrorLabel = v.findViewById(R.id.error_label);
        mAvgErrorLabel = v.findViewById(R.id.avg_error_label);
        mAvgErrorLabel.setText(Application.get().getString(R.string.avg_error_label, 0));
        mLeftDivider = v.findViewById(R.id.divider_left);
        mRightDivider = v.findViewById(R.id.divider_right);
        mErrorUnit = v.findViewById(R.id.error_unit);
        mAvgErrorUnit = v.findViewById(R.id.avg_error_unit);
        mErrorChart = v.findViewById(R.id.error_chart);
        mVertErrorChart = v.findViewById(R.id.vert_error_chart);
        initChart(mErrorChart);
        initChart(mVertErrorChart);
        setupUnitPreferences();
        mVerticalErrorCardView = v.findViewById(R.id.vert_error_layout);
        mGroundTruthCardView = v.findViewById(R.id.benchmark_card);

        mGroundTruthCardView.getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        mMotionLayout = v.findViewById(R.id.motion_layout);
        mSaveGroundTruth = v.findViewById(R.id.save);
        mQrCode = v.findViewById(R.id.qr_code);
        mLatText = v.findViewById(R.id.ground_truth_lat);
        mLongText = v.findViewById(R.id.ground_truth_long);
        mAltText = v.findViewById(R.id.ground_truth_alt);
        mMotionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int i, boolean b, float v) {
            }

            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int i, int i1) {
            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.expanded) {
                    onCardExpanded();
                } else {
                    onCardCollapsed();
                }
            }
        });

        mSaveGroundTruth.setOnClickListener(view -> {
            if (!mViewModel.getBenchmarkCardCollapsed()) {
                if (!UIUtils.isValidLocationWithErrorDialog(activity, mLatText.getEditText().getText().toString(), mLongText.getEditText().getText().toString(), mAltText.getEditText().getText().toString())) {
                    return;
                }
                resetError();
                saveGroundTruth();
            } else {
                editGroundTruth();
            }
        });

        mQrCode.setOnClickListener(view -> {
            if (!Application.getPrefs().getBoolean(
                    Application.get().getString(R.string.pref_key_never_show_qr_code_instructions), false)) {
                UIUtils.createQrCodeDialog(activity).show();
            } else {
                IOUtils.openQrCodeReader(activity);
            }
        });

        setupSlidingPanel();

        mViewModel = ViewModelProviders.of(activity).get(BenchmarkViewModel.class);
        mViewModel.getAllowGroundTruthEdit().observe(activity, mAllowGroundTruthEditObserver);
        mViewModel.getLocationErrorPair().observe(activity, mLocationErrorPairObserver);
        mViewModel.getAvgError().observe(activity, mAvgErrorObserver);
        if (isTestInProgress()) {
            // Test is already in progress (e.g., due to device rotation), restore model to views
            updateGroundTruthEditTexts(mViewModel.getGroundTruthLocation().getValue());
            saveGroundTruth();
            restoreGraphData();
            onCardCollapsed();
        } else {
            Location groundTruth;
            // If a SHOW_RADAR intent was passed via an Intent (e.g., from BenchMap app), use that as ground truth
            if (IOUtils.isShowRadarIntent(activity.getIntent())) {
                groundTruth = IOUtils.getLocationFromIntent(activity.getIntent());
                if (groundTruth != null) {
                    Toast.makeText(activity, Application.get().getString(R.string.show_radar_valid_location), Toast.LENGTH_LONG).show();
                    restoreGroundTruth(groundTruth);
                } else {
                    Toast.makeText(activity, Application.get().getString(R.string.show_radar_invalid_location), Toast.LENGTH_LONG).show();
                }
            } else if (Application.getPrefs().contains(GROUND_TRUTH_LAT)) {
                // If there is a saved ground truth value from previous executions, start test using that
                groundTruth = new Location("ground_truth");
                groundTruth.setLatitude(PreferenceUtils.getDouble(GROUND_TRUTH_LAT, Double.NaN));
                groundTruth.setLongitude(PreferenceUtils.getDouble(GROUND_TRUTH_LONG, Double.NaN));
                if (Application.getPrefs().contains(GROUND_TRUTH_ALT)) {
                    groundTruth.setAltitude(PreferenceUtils.getDouble(GROUND_TRUTH_ALT, Double.NaN));
                }
                restoreGroundTruth(groundTruth);
            }
        }
    }

    /**
     * Initializes a test with a pre-existing ground truth location (e.g., from an Intent or preferences)
     *
     * @param location location to use as the ground truth location
     */
    private void restoreGroundTruth(Location location) {
        updateGroundTruthEditTexts(location);
        resetError();
        saveGroundTruth();
        onCardCollapsed();
    }

    /**
     * Should be called when the ground truth card state is fully expanded
     */
    private void onCardExpanded() {
        mSaveGroundTruth.setText(Application.get().getString(R.string.save));
        mLatText.setEnabled(true);
        mLongText.setEnabled(true);
        mAltText.setEnabled(true);
        mLatText.setFocusable(true);
        mLongText.setFocusable(true);
        mAltText.setFocusable(true);
    }

    /**
     * Should be called when the ground truth card state is fully collapsed
     */
    private void onCardCollapsed() {
        mSaveGroundTruth.setText(Application.get().getString(R.string.edit));
        mLatText.setEnabled(false);
        mLongText.setEnabled(false);
        mAltText.setEnabled(false);
        mLatText.setFocusable(false);
        mLongText.setFocusable(false);
        mAltText.setFocusable(false);
    }

    /**
     * Initialize the ground truth value from the values in the lat/long/alt text boxes
     */
    private void saveGroundTruth() {
        // Save Ground Truth
        Location groundTruthLocation = new Location("ground_truth");

        if (!isEmpty(mLatText.getEditText().getText().toString()) && !isEmpty(mLongText.getEditText().getText().toString())) {
            groundTruthLocation.setLatitude(MathUtils.toDouble(mLatText.getEditText().getText().toString()));
            groundTruthLocation.setLongitude(MathUtils.toDouble(mLongText.getEditText().getText().toString()));
        }
        if (!isEmpty(mAltText.getEditText().getText().toString())) {
            // Use altitude for measuring vertical error
            groundTruthLocation.setAltitude(MathUtils.toDouble(mAltText.getEditText().getText().toString()));
        }
        mViewModel.setGroundTruthLocation(groundTruthLocation);
        mViewModel.setBenchmarkCardCollapsed(true);
        mViewModel.setAllowGroundTruthEdit(false);

        PreferenceUtils.saveDouble(GROUND_TRUTH_LAT, groundTruthLocation.getLatitude());
        PreferenceUtils.saveDouble(GROUND_TRUTH_LONG, groundTruthLocation.getLongitude());
        if (groundTruthLocation.hasAltitude()) {
            PreferenceUtils.saveDouble(GROUND_TRUTH_ALT, groundTruthLocation.getAltitude());
        } else {
            PreferenceUtils.remove(GROUND_TRUTH_ALT);
        }
    }

    /**
     * Expands the ground truth card to allow the user to enter a new ground truth value
     */
    private void editGroundTruth() {
        mViewModel.setBenchmarkCardCollapsed(false);
        mViewModel.setAllowGroundTruthEdit(true);
    }

    private void initChart(LineChart errorChart) {
        errorChart.getDescription().setEnabled(false);
        errorChart.setTouchEnabled(true);
        errorChart.setDragEnabled(true);
        errorChart.setScaleEnabled(true);
        errorChart.setDrawGridBackground(false);
        // If disabled, scaling can be done on x- and y-axis separately
        errorChart.setPinchZoom(true);

        // Set an alternate background color
        //mErrorChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        //data.setValueTextColor(Color.WHITE);

        // Add empty data
        errorChart.setData(data);

        // Get the legend (only possible after setting data)
        Legend l = errorChart.getLegend();
        l.setEnabled(true);
        l.setTextColor(mChartTextColor);

//        // Modify the legend ...
//        l.setForm(Legend.LegendForm.LINE);
//        //l.setTypeface(tfLight);
//        l.setTextColor(Color.WHITE);

        XAxis xAxis = errorChart.getXAxis();
        //xAxis.setTypeface(tfLight);
        xAxis.setTextColor(mChartTextColor);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        YAxis leftAxis = errorChart.getAxisLeft();
        //leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(mChartTextColor);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = errorChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void initChartUnits(LineChart errorChart) {
        String unit;
        if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
            unit = Application.get().getString(R.string.meters_abbreviation);
        } else {
            unit = Application.get().getString(R.string.feet_abbreviation);
        }

        DistanceValueFormatter formatter = new DistanceValueFormatter(unit);
        errorChart.getAxisLeft().setValueFormatter(formatter);
    }

    private void resetError() {
        mViewModel.reset();
        mErrorView.setVisibility(INVISIBLE);
        mVertErrorView.setVisibility(INVISIBLE);
        mAvgErrorView.setVisibility(INVISIBLE);
        mAvgVertErrorView.setVisibility(INVISIBLE);
        mLeftDivider.setVisibility(INVISIBLE);
        mRightDivider.setVisibility(INVISIBLE);
        mErrorUnit.setVisibility(INVISIBLE);
        mAvgErrorUnit.setVisibility(INVISIBLE);
        mAvgErrorLabel.setText(Application.get().getString(R.string.avg_error_label, 0));

        mErrorChart.clearValues();
        mVertErrorChart.clearValues();
    }

    /**
     * Load data from the view model into the graphs, for example after rotation
     */
    private void restoreGraphData() {
        mErrorChart.clearValues();
        mVertErrorChart.clearValues();
        int i = 1;
        for (Pair<Location, MeasuredError> pair : mViewModel.getLocationErrorPairs()) {
            addErrorToGraphs(i, pair.second, pair.first);
            i++;
        }
    }

    /**
     * Called from the hosting Activity when onBackPressed() is called (i.e., when the user
     * presses the back button)
     * @return true if the controller handled in the click and super.onBackPressed() should not be
     * called by the hosting Activity, or false if the controller didn't handle the click and
     * super.onBackPressed() should be called
     */
    @Override
    public boolean onBackPressed() {
        // Collapse the panel when the user presses the back button
        if (mSlidingPanel != null) {
            // Collapse the sliding panel if its anchored or expanded
            if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED
                    || mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
                mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        setupUnitPreferences();
    }

    public void show() {
        if (mGroundTruthCardView != null) {
            mGroundTruthCardView.setVisibility(VISIBLE);
        }
        if (mMotionLayout != null) {
            mMotionLayout.setVisibility(VISIBLE);
        }
        if (mSlidingPanel != null && mLastPanelState != null) {
            mSlidingPanel.setPanelState(mLastPanelState);
        }
    }

    public void hide() {
        if (mGroundTruthCardView != null) {
            mGroundTruthCardView.setVisibility(GONE);
        }
        if (mMotionLayout != null) {
            mMotionLayout.setVisibility(GONE);
        }
        if (mSlidingPanel != null) {
            if (mSlidingPanel.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
                // Save the last visible panel state
                mLastPanelState = mSlidingPanel.getPanelState();
            }
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
        mViewModel.addLocation(location);
    }

    /**
     * Add the provided error and estimated accuracy info (from Location) to the graph with the given x-axis index
     * @param index x-axis index
     * @param error
     * @param location
     */
    private void addErrorToGraphs(int index, MeasuredError error, Location location) {
        float horError;
        float horAccuracy;
        if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
            horError = error.getError();
            horAccuracy = location.getAccuracy();
        } else {
            // Feet
            horError = (float) UIUtils.toFeet(error.getError());
            horAccuracy = (float) UIUtils.toFeet(location.getAccuracy());
        }
        addErrorToGraph(index, mErrorChart, horError, horAccuracy);

        if (!Double.isNaN(error.getVertError())) {
            double vertError;
            float vertAccuracy = Float.NaN;
            if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
                vertError = Math.abs(error.getVertError());
            } else {
                // Feet
                vertError = UIUtils.toFeet(Math.abs(error.getVertError()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mPrefDistanceUnits.equalsIgnoreCase(METERS)) {
                    vertAccuracy = location.getVerticalAccuracyMeters();
                } else {
                    // Feet
                    vertAccuracy = (float) UIUtils.toFeet(location.getVerticalAccuracyMeters());
                }
            }

            addErrorToGraph(index, mVertErrorChart, vertError, vertAccuracy);
        }
    }

    private void addErrorToGraph(int index, LineChart chart, double error, float estimatedAccuracy) {
        LineData data = chart.getData();

        if (data != null) {
            ILineDataSet errorSet = data.getDataSetByIndex(ERROR_SET);
            ILineDataSet estimatedSet = data.getDataSetByIndex(ESTIMATED_ACCURACY_SET);
            // errorSet.addEntry(...); // can be called as well

            if (errorSet == null) {
                errorSet = createGraphDataSet(ERROR_SET);
                data.addDataSet(errorSet);
            }
            if (estimatedSet == null && !Float.isNaN(estimatedAccuracy)) {
                estimatedSet = createGraphDataSet(ESTIMATED_ACCURACY_SET);
                data.addDataSet(estimatedSet);
            }

            data.addEntry(new Entry(index, (float) error), ERROR_SET);
            if (!Float.isNaN(estimatedAccuracy)) {
                data.addEntry(new Entry(index, estimatedAccuracy), ESTIMATED_ACCURACY_SET);
            }
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(40);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    /**
     * Creates a graph dataset, for error if set is ERROR_SET, or for estimated accuracy if ESTIMATED_ACCURACY_SET
     * @param setType creates a data set for error if set is ERROR_SET, and for estimated accuracy if ESTIMATED_ACCURACY_SET
     * @return a graph dataset
     */
    private LineDataSet createGraphDataSet(int setType) {
        String label;
        if (setType == ERROR_SET) {
            label = Application.get().getResources().getString(R.string.measured_error_graph_label);
        } else {
            label = Application.get().getResources().getString(R.string.estimated_accuracy_graph_label);
        }

        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        if (setType == ERROR_SET) {
            set.setColor(Color.RED);
        } else {
            set.setColor(ColorTemplate.getHoloBlue());
        }
        set.setCircleColor(Color.BLACK);
        set.setLineWidth(2f);
        set.setCircleRadius(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
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
        if (!mViewModel.getBenchmarkCardCollapsed()) {
            updateGroundTruthEditTexts(location);
        }
    }

    private void updateGroundTruthEditTexts(Location location) {
        mLatText.getEditText().setText(Application.get().getString(R.string.benchmark_lat_long, location.getLatitude()));
        mLongText.getEditText().setText(Application.get().getString(R.string.benchmark_lat_long, location.getLongitude()));

        if (location.hasAltitude()) {
            mAltText.getEditText().setText(Application.get().getString(R.string.benchmark_alt, location.getAltitude()));
        }
    }

    /**
     * Returns true if there is a test in progress to measure accuracy, and false if there is not
     * @return true if there is a test in progress to measure accuracy, and false if there is not
     */
    private boolean isTestInProgress() {
        return mViewModel.getBenchmarkCardCollapsed();
    }

    private void setupUnitPreferences() {
        SharedPreferences settings = Application.getPrefs();
        Application app = Application.get();

        String prefDistanceUnits = settings
                .getString(app.getString(R.string.pref_key_preferred_distance_units_v2), METERS);
        if (mPrefDistanceUnits != null && !prefDistanceUnits.equals(mPrefDistanceUnits)) {
            // Units have changed since the graphs were originally initialized - reload data
            mPrefDistanceUnits = prefDistanceUnits;
            restoreGraphData();
        } else {
            mPrefDistanceUnits = prefDistanceUnits;
        }
        initChartUnits(mErrorChart);
        initChartUnits(mVertErrorChart);
    }

    private void setupSlidingPanel() {
        mSlidingPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (previousState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    return;
                }

                switch (newState) {
                    case EXPANDED:
                        onPanelExpanded(panel);
                        break;
                    case COLLAPSED:
                        onPanelCollapsed(panel);
                        break;
                    case ANCHORED:
                        onPanelAnchored(panel);
                        break;
                    case HIDDEN:
                        onPanelHidden(panel);
                        break;
                }
            }

            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.d(TAG, "onPanelSlide, offset " + slideOffset);
                setPanelHeaderRadius(slideOffset);
            }

            public void onPanelExpanded(View panel) {
                Log.d(TAG, "onPanelExpanded");
            }

            public void onPanelCollapsed(View panel) {
                Log.d(TAG, "onPanelCollapsed");
            }

            public void onPanelAnchored(View panel) {
                Log.d(TAG, "onPanelAnchored");
            }

            public void onPanelHidden(View panel) {
                Log.d(TAG, "onPanelHidden");
            }
        });
    }

    private void setPanelHeaderRadius(float slideOffset) {
        final float ANIMATE_THRESHOLD_PERCENT = 0.5f;
        // Only animate the corner radius if the panel is over ANIMATE_THRESHOLD_PERCENT expanded
        if (slideOffset < ANIMATE_THRESHOLD_PERCENT) {
            return;
        }
        float newOffset = MathUtils.mapToRange(slideOffset, ANIMATE_THRESHOLD_PERCENT, 1.0f, 0f, 1.0f);
        GradientDrawable shape =  new GradientDrawable();
        float[] corners = new float[8];
        float radius = (1 - newOffset) * Application.get().getResources().getDimensionPixelSize(R.dimen.ground_truth_sliding_header_corner_radius);
        corners[0] = radius;
        corners[1] = radius;
        corners[2] = radius;
        corners[3] = radius;
        corners[4] = 0;
        corners[5] = 0;
        corners[6] = 0;
        corners[7] = 0;
        shape.setCornerRadii(corners);
        shape.setColor(Application.get().getResources().getColor(R.color.colorPrimary));
        mSlidingPanelHeader.setBackground(shape);
    }
}
