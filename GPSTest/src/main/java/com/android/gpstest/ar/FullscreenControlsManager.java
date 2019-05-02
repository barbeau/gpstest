package com.android.gpstest.ar;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.android.gpstest.util.MiscUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the showing and hiding of controls and system UI in full screen mode.
 * <p>
 * Created by johntaylor on 2/21/16.
 */
public class FullscreenControlsManager {
    private static final String TAG = MiscUtils.getTag(FullscreenControlsManager.class);
    /**
     * number of milliseconds to wait after
     * user interaction before hiding the system UI
     * if autoHide is true in shared preferences.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 1000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private Activity mActivity;
    private View mContentView;
    private List<View> mViewsToHide;
    private boolean mVisible;

    public FullscreenControlsManager(
            Activity parentActivity, View contentView,
            List<View> viewsToHide, List<View> viewsToTriggerHide) {
        mActivity = parentActivity;
        mVisible = true;
        mViewsToHide = new ArrayList(viewsToHide);
        mContentView = contentView;

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        for (View buttonView : viewsToTriggerHide) {
            buttonView.setOnTouchListener(mDelayHideTouchListener);
        }
    }

    public void toggleControls() {
        Log.d(TAG, "Toggling the UI");
        toggle();
    }

    /**
     * Quickly exposes the controls so that the user knows they're there.
     */
    public void flashTheControls() {

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(1000);
    }

    public void delayHideTheControls() {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //Note this function should not be executed when hide_show_toolbar button is clicked
            if (autoHideEnabled()) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        if (autoHideEnabled()) {
            if (mVisible) {
                hide();
            } else {
                show();
            }
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = mActivity.getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        for (View view : mViewsToHide) {
            view.setVisibility(View.GONE);
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = mActivity.getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            for (View view : mViewsToHide) {
                view.setVisibility(View.VISIBLE);
            }
        }
    };

    private final Handler mHideHandler = new Handler();

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private boolean autoHideEnabled() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(mActivity.getApplicationContext());
        return preferences.getBoolean("auto_hide", true);
    }
}
