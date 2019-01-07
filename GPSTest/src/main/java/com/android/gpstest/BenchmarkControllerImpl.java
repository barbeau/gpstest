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
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.gpstest.model.AvgError;
import com.android.gpstest.model.MeasuredError;
import com.android.gpstest.util.BenchmarkUtils;
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

import androidx.constraintlayout.motion.widget.MotionLayout;

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

    private boolean mBenchmarkCardCollapsed = false;

    MaterialCardView mGroundTruthCardView;

    MotionLayout mMotionLayout;

    TextView mErrorView, mVertErrorView, mAvgErrorView, mAvgVertErrorView, mErrorLabel, mAvgErrorLabel, mLeftDivider, mRightDivider, mErrorUnit, mAvgErrorUnit;
    TextInputLayout mLatText, mLongText, mAltText;

    SlidingUpPanelLayout mSlidingPanel;

    SlidingUpPanelLayout.PanelState mLastPanelState;

    LineChart mErrorChart;

    Location mGroundTruthLocation;

    AvgError mAvgError = new AvgError();

    private Listener mListener;

    public BenchmarkControllerImpl(View v, Bundle savedInstanceState) {
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
        initChart(mErrorChart);
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
        // TODO - set initial state of sliding panel depending on savedInstanceState

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

                resetError();

                // Show sliding panel if it's not visible
                if (mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
                if (mListener != null) {
                    mListener.onAllowGroundTruthEditChanged(false);
                    mListener.onGroundTruthLocationSaved(mGroundTruthLocation);
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
        l.setEnabled(false);

//        // Modify the legend ...
//        l.setForm(Legend.LegendForm.LINE);
//        //l.setTypeface(tfLight);
//        l.setTextColor(Color.WHITE);

        XAxis xAxis = errorChart.getXAxis();
        //xAxis.setTypeface(tfLight);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(1f);

        YAxis leftAxis = errorChart.getAxisLeft();
        //leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = errorChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void resetError() {
        mAvgError.reset();
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
        if (mGroundTruthLocation == null || !mBenchmarkCardCollapsed) {
            // If we don't have a ground truth location yet, or if the user is editing the location,
            // don't update the errors
            return;
        }
        MeasuredError error = BenchmarkUtils.Companion.measureError(location, mGroundTruthLocation);
        mAvgError.addMeasurement(error);
        if (mErrorView != null && mAvgErrorView != null) {
            mErrorUnit.setVisibility(VISIBLE);
            mErrorView.setVisibility(VISIBLE);
            mErrorView.setText(Application.get().getString(R.string.benchmark_error, error.getError()));
            mAvgErrorUnit.setVisibility(VISIBLE);
            mAvgErrorView.setVisibility(VISIBLE);
            mAvgErrorView.setText(Application.get().getString(R.string.benchmark_error, mAvgError.getAvgError()));
            mAvgErrorLabel.setText(Application.get().getString(R.string.avg_error_label, mAvgError.getCount()));
        }
        if (mVertErrorView != null && !Double.isNaN(error.getVertError())) {
            // Vertical errors
            mErrorLabel.setText(R.string.horizontal_vertical_error_label);
            mLeftDivider.setVisibility(VISIBLE);
            mRightDivider.setVisibility(VISIBLE);
            mVertErrorView.setVisibility(VISIBLE);
            mVertErrorView.setText(Application.get().getString(R.string.benchmark_error, error.getVertError()));
            mAvgVertErrorView.setVisibility(VISIBLE);
            mAvgVertErrorView.setText(Application.get().getString(R.string.benchmark_error, mAvgError.getAvgVertError()));
        } else {
            // Hide any vertical error indication
            mErrorLabel.setText(R.string.horizontal_error_label);
            mLeftDivider.setVisibility(GONE);
            mRightDivider.setVisibility(GONE);
            mVertErrorView.setVisibility(GONE);
            mAvgVertErrorView.setVisibility(GONE);
        }
        addErrorToGraph(error);
    }

    private void addErrorToGraph(MeasuredError error) {
        LineData data = mErrorChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createGraphDataSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), error.getError()), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mErrorChart.notifyDataSetChanged();

            // limit the number of visible entries
            mErrorChart.setVisibleXRangeMaximum(40);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mErrorChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createGraphDataSet() {
        LineDataSet set = new LineDataSet(null, Application.get().getResources().getString(R.string.horizontal_error_label));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
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
        if (!mBenchmarkCardCollapsed) {
            mLatText.getEditText().setText(Application.get().getString(R.string.benchmark_lat_long, location.getLatitude()));
            mLongText.getEditText().setText(Application.get().getString(R.string.benchmark_lat_long, location.getLongitude()));

            if (location.hasAltitude()) {
                mAltText.getEditText().setText(Application.get().getString(R.string.benchmark_alt, location.getAltitude()));
            }
        }
    }
}
