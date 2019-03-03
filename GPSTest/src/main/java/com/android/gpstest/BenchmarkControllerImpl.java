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
import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.gpstest.chart.DistanceValueFormatter;
import com.android.gpstest.model.AvgError;
import com.android.gpstest.model.MeasuredError;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

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

    private static final String BENCHMARK_CARD_COLLAPSED = "ground_truth_card_collapsed";

    private static final int ERROR_SET = 0;

    private static final int ESTIMATED_ACCURACY_SET = 1;

    private static final float UNIT_VERT_BIAS_HOR_ERROR_ONLY = 0.582f;

    private static final float UNIT_VERT_BIAS_INCL_VERT_ERROR = 0.25f;

    MaterialCardView mGroundTruthCardView, mVerticalErrorCardView;

    MotionLayout mMotionLayout;

    TextView mErrorView, mVertErrorView, mAvgErrorView, mAvgVertErrorView, mErrorLabel, mAvgErrorLabel, mLeftDivider, mRightDivider, mErrorUnit, mAvgErrorUnit;
    TextInputLayout mLatText, mLongText, mAltText;

    SlidingUpPanelLayout mSlidingPanel;

    SlidingUpPanelLayout.PanelState mLastPanelState;

    LineChart mErrorChart, mVertErrorChart;

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

                resetError();

                // Show sliding panel if it's not visible
                if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
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
                mErrorView.setText(Application.get().getString(R.string.benchmark_error, error.getError()));
            }
            if (mVertErrorView != null && !Double.isNaN(error.getVertError())) {
                // Vertical errors
                mErrorLabel.setText(R.string.horizontal_vertical_error_label);
                mLeftDivider.setVisibility(VISIBLE);
                mRightDivider.setVisibility(VISIBLE);
                mVertErrorView.setVisibility(VISIBLE);
                mVertErrorView.setText(Application.get().getString(R.string.benchmark_error, error.getVertError()));
                mVerticalErrorCardView.setVisibility(VISIBLE);
            } else {
                // Hide any vertical error indication
                mErrorLabel.setText(R.string.horizontal_error_label);
                mLeftDivider.setVisibility(GONE);
                mRightDivider.setVisibility(GONE);
                mVertErrorView.setVisibility(GONE);
                mVerticalErrorCardView.setVisibility(GONE);
            }
            addErrorToGraphs(error, location);
        }
    };

    private final Observer<AvgError> mAvgErrorObserver = new Observer<AvgError>() {
        @Override
        public void onChanged(@Nullable final AvgError avgError) {
            if (mAvgErrorView != null) {
                mAvgErrorUnit.setVisibility(VISIBLE);
                mAvgErrorView.setVisibility(VISIBLE);
                mAvgErrorView.setText(Application.get().getString(R.string.benchmark_error, avgError.getAvgError()));
                mAvgErrorLabel.setText(Application.get().getString(R.string.avg_error_label, avgError.getCount()));
            }
            if (mAvgVertErrorView != null && !Double.isNaN(avgError.getAvgVertError())) {
                // Vertical errors
                mAvgVertErrorView.setVisibility(VISIBLE);
                mAvgVertErrorView.setText(Application.get().getString(R.string.benchmark_error, avgError.getAvgVertError()));
            } else {
                // Hide any vertical error indication
                mAvgVertErrorView.setVisibility(GONE);
            }
        }
    };

    public BenchmarkControllerImpl(AppCompatActivity activity, View v, Bundle savedInstanceState) {
        mSlidingPanel = v.findViewById(R.id.bottom_sliding_layout);
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
        mVerticalErrorCardView = v.findViewById(R.id.vert_error_layout);
        mGroundTruthCardView = v.findViewById(R.id.benchmark_card);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mGroundTruthCardView.getLayoutTransition()
                    .enableTransitionType(LayoutTransition.CHANGING);
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
        // TODO - set initial state of sliding panel depending on savedInstanceState

        saveGroundTruth.setOnClickListener(view -> {
            if (!mViewModel.getBenchmarkCardCollapsed()) {
                // TODO - if lat and long aren't filled, show error
                saveGroundTruth();
            } else {
                editGroundTruth();
            }
        });

        mViewModel = ViewModelProviders.of(activity).get(BenchmarkViewModel.class);
        mViewModel.getAllowGroundTruthEdit().observe(activity, mAllowGroundTruthEditObserver);
        mViewModel.getLocationErrorPair().observe(activity, mLocationErrorPairObserver);
        mViewModel.getAvgError().observe(activity, mAvgErrorObserver);
        if (mViewModel.getBenchmarkCardCollapsed()) {
            updateGroundTruthEditTexts(mViewModel.getGroundTruthLocation().getValue());
            // TODO Instead of saving ground truth again (which starts test back at 1), resume existing test
            saveGroundTruth();
        }
    }

    /**
     * Initialize the ground truth value from the values in the lat/long/alt text boxes
     */
    private void saveGroundTruth() {
        // Save Ground Truth
        Location groundTruthLocation = new Location("ground_truth");

        if (!isEmpty(mLatText.getEditText().getText().toString()) && !isEmpty(mLongText.getEditText().getText().toString())) {
            groundTruthLocation.setLatitude(Double.valueOf(mLatText.getEditText().getText().toString()));
            groundTruthLocation.setLongitude(Double.valueOf(mLongText.getEditText().getText().toString()));
        }
        if (!isEmpty(mAltText.getEditText().getText().toString())) {
            // Use altitude for measuring vertical error
            groundTruthLocation.setAltitude(Double.valueOf(mAltText.getEditText().getText().toString()));
        }
        mViewModel.setGroundTruthLocation(groundTruthLocation);
        mViewModel.setBenchmarkCardCollapsed(true);
        mViewModel.setAllowGroundTruthEdit(false);
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

//        // Modify the legend ...
//        l.setForm(Legend.LegendForm.LINE);
//        //l.setTypeface(tfLight);
//        l.setTextColor(Color.WHITE);

        XAxis xAxis = errorChart.getXAxis();
        //xAxis.setTypeface(tfLight);
        //xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        DistanceValueFormatter formatter = new DistanceValueFormatter("m");

        YAxis leftAxis = errorChart.getAxisLeft();
        //leftAxis.setTypeface(tfLight);
        //leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setValueFormatter(formatter);

        YAxis rightAxis = errorChart.getAxisRight();
        rightAxis.setEnabled(false);
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
        mViewModel.addLocation(location);
    }

    private void addErrorToGraphs(MeasuredError error, Location location) {
        addErrorToGraph(mErrorChart, error.getError(), location.getAccuracy());

        if (!Double.isNaN(error.getVertError())) {
            float vertAccuracy = Float.NaN;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vertAccuracy = location.getVerticalAccuracyMeters();
            }

            addErrorToGraph(mVertErrorChart, error.getVertError(), vertAccuracy);
        }
    }

    private void addErrorToGraph(LineChart chart, double error, float estimatedAccuracy) {
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

            data.addEntry(new Entry(mViewModel.getAvgError().getValue().getCount(), (float) error), ERROR_SET);
            if (!Float.isNaN(estimatedAccuracy)) {
                data.addEntry(new Entry(mViewModel.getAvgError().getValue().getCount(), estimatedAccuracy), ESTIMATED_ACCURACY_SET);
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
        set.setValueTextColor(Color.WHITE);
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
}