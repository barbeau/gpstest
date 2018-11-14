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
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

import androidx.constraintlayout.motion.widget.MotionLayout;

/**
 * This class encapsulates logic used for the benchmarking feature that compares a user-entered
 * ground truth value against the GPS location.
 */
public class BenchmarkControllerImpl implements BenchmarkController {

    private static final String BENCHMARK_CARD_COLLAPSED = "ground_truth_card_collapsed";

    private boolean mBenchmarkCardCollapsed = false;

    MaterialCardView mGroundTruthCardView;

    MotionLayout mMotionLayout;

    public BenchmarkControllerImpl(View v, Bundle savedInstanceState) {
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
        TextView title2 = v.findViewById(R.id.set_ground_truth_title2);
        mMotionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.expanded) {
                    title2.setText(Application.get().getString(R.string.set_ground_truth_subtitle));
                    saveGroundTruth.setText(Application.get().getString(R.string.save));
                    latText.setEnabled(true);
                    longText.setEnabled(true);
                    altText.setEnabled(true);
                    latText.setFocusable(true);
                    longText.setFocusable(true);
                    altText.setFocusable(true);
                } else {
                    // Collapsed
                    title2.setText(Application.get().getString(R.string.set_ground_truth_subtitle_collapsed));
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
                // Collapse card
                mMotionLayout.transitionToEnd();
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height_collapsed);
                mGroundTruthCardView.setLayoutParams(lp);
                mBenchmarkCardCollapsed = true;
            } else {
                // Expand card
                mMotionLayout.transitionToStart();
                lp.height = (int) Application.get().getResources().getDimension(R.dimen.ground_truth_cardview_height);
                mGroundTruthCardView.setLayoutParams(lp);
                mBenchmarkCardCollapsed = false;
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
}
