package com.android.gpstest.map;

import android.location.Location;
import android.os.Bundle;
import android.util.Pair;

import com.android.gpstest.BenchmarkViewModel;
import com.android.gpstest.model.MeasuredError;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import static com.android.gpstest.map.MapConstants.ALLOW_GROUND_TRUTH_CHANGE;
import static com.android.gpstest.map.MapConstants.GROUND_TRUTH;
import static com.android.gpstest.map.MapConstants.MODE;
import static com.android.gpstest.map.MapConstants.MODE_ACCURACY;
import static com.android.gpstest.map.MapConstants.MODE_MAP;

public class BenchmarkMapController {

    /**
     * An interface implemented by the map to allow other classes to manipulate the map
     */
    public interface MapInterface {

        void addGroundTruthMarker(Location location);

        void drawPathLine(Location loc1, Location loc2);

        void removePathLines();
    }

    private String mMode = MODE_MAP;

    private boolean mAllowGroundTruthChange = true;

    private Location mGroundTruthLocation;

    BenchmarkViewModel mViewModel;

    MapInterface mMap;

    public BenchmarkMapController(FragmentActivity activity, MapInterface map) {
        mMap = map;

        mViewModel = ViewModelProviders.of(activity).get(BenchmarkViewModel.class);
        mViewModel.getGroundTruthLocation().observe(activity, mGroundTruthLocationObserver);
        mViewModel.getAllowGroundTruthEdit().observe(activity, mAllowGroundTruthEditObserver);
    }

    private final Observer<Location> mGroundTruthLocationObserver = new Observer<Location>() {
        @Override
        public void onChanged(@Nullable final Location newValue) {
            mGroundTruthLocation = newValue;
            mMap.addGroundTruthMarker(mGroundTruthLocation);
            mMap.removePathLines();
        }
    };

    private final Observer<Boolean> mAllowGroundTruthEditObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable final Boolean newValue) {
            mAllowGroundTruthChange = newValue;
        }
    };

    /**
     * Restore the controller state in priority order, first from savedInstanceState (e.g., on rotation),
     * then from the passed in arguments
     * @param savedInstanceState savedInstanceState from the host fragment
     * @param arguments arguments passed into the host fragment
     * @param isGroundTruthMarkerNull true if the ground truth map marker was null, false if it was not
     */
    public void restoreState(Bundle savedInstanceState, Bundle arguments, boolean isGroundTruthMarkerNull) {
        if (savedInstanceState != null) {
            // Restore an existing state (e.g., from device rotation)
            mMode = savedInstanceState.getString(MODE);
            mAllowGroundTruthChange = savedInstanceState.getBoolean(ALLOW_GROUND_TRUTH_CHANGE);
            Location groundTruth = savedInstanceState.getParcelable(GROUND_TRUTH);
            if (groundTruth != null) {
                mGroundTruthLocation = groundTruth;
                mMap.addGroundTruthMarker(mGroundTruthLocation);
            }
        } else {
            // Not restoring existing state - see what was provided as arguments
            if (arguments != null) {
                mMode = arguments.getString(MODE, MODE_MAP);
            }
            // If we have a ground truth location but no marker, we're starting using a ground truth
            // location from a previous execution but map wasn't initialized when we got the ViewModel
            // callback to mGroundTruthLocationObserver.  So, add the marker now to restore state.
            if (mGroundTruthLocation != null && isGroundTruthMarkerNull) {
                mMap.addGroundTruthMarker(mGroundTruthLocation);
            }
        }
        if (mMode.equals(MODE_ACCURACY) && isTestInProgress()) {
            Location lastLocation = null;
            // Restore the path lines on the map
            for (Pair<Location, MeasuredError> pair : mViewModel.getLocationErrorPairs()) {
                if (lastLocation != null) {
                    mMap.drawPathLine(lastLocation, pair.first);
                }
                lastLocation = pair.first;
            }
        }
    }

    public String getMode() {
        return mMode;
    }

    public boolean allowGroundTruthChange() {
        return mAllowGroundTruthChange;
    }

    public Location getGroundTruthLocation() {
        return mGroundTruthLocation;
    }

    /**
     * Returns true if there is a test in progress to measure accuracy, and false if there is not
     * @return true if there is a test in progress to measure accuracy, and false if there is not
     */
    public boolean isTestInProgress() {
        return mViewModel.getBenchmarkCardCollapsed();
    }
}