/*
 * Copyright (C) 2019 Sean J. Barbeau
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

import android.app.Application;
import android.location.Location;
import android.util.Pair;

import com.android.gpstest.model.AvgError;
import com.android.gpstest.model.MeasuredError;
import com.android.gpstest.util.BenchmarkUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * View model that holds GNSS benchmarking (ground truth and error measurement) information
 */
public class BenchmarkViewModel extends AndroidViewModel {

    private MutableLiveData<Location> mGroundTruthLocation = new MutableLiveData<>();

    private MutableLiveData<AvgError> mAvgError = new MutableLiveData<>();

    private MutableLiveData<Boolean> mAllowGroundTruthEdit = new MutableLiveData<>(true);

    private boolean mBenchmarkCardCollapsed = false;

    private MutableLiveData<Pair<Location, MeasuredError>> mLocationErrorPair = new MutableLiveData<>();

    private ArrayList<Pair<Location, MeasuredError>> mLocationErrorPairs = new ArrayList<>();

    public BenchmarkViewModel(@NonNull Application application) {
        super(application);
    }

    public void setGroundTruthLocation(Location groundTruthLocation) {
        mGroundTruthLocation.setValue(groundTruthLocation);
    }

    public LiveData<Location> getGroundTruthLocation() {
        return mGroundTruthLocation;
    }

    public void setAllowGroundTruthEdit(boolean allowGroundTruthEdit) {
        mAllowGroundTruthEdit.setValue(allowGroundTruthEdit);
    }

    public LiveData<Boolean> getAllowGroundTruthEdit() {
        return mAllowGroundTruthEdit;
    }

    public void setBenchmarkCardCollapsed(boolean cardCollapsed) {
        mBenchmarkCardCollapsed = cardCollapsed;
    }

    public boolean getBenchmarkCardCollapsed() {
        return mBenchmarkCardCollapsed;
    }

    public LiveData<AvgError> getAvgError() {
        return mAvgError;
    }

    public LiveData<Pair<Location, MeasuredError>> getLocationErrorPair() {
        return mLocationErrorPair;
    }

    /**
     * Get history of all location and error pairs from the most recent test
     * @return history of all location and error pairs from the most recent test
     */
    public ArrayList<Pair<Location, MeasuredError>> getLocationErrorPairs() {
        return mLocationErrorPairs;
    }

    /**
     * Adds a new location to the view model and calculates relevate errors
     * @param location
     */
    public void addLocation(Location location) {
        if (mGroundTruthLocation.getValue() == null || !mBenchmarkCardCollapsed) {
            // If we don't have a ground truth location yet, or if the user is editing the location,
            // don't update the errors
            return;
        }
        // Calculate and update error
        MeasuredError error = BenchmarkUtils.Companion.measureError(location, mGroundTruthLocation.getValue());

        // Update avg error
        AvgError avgError = mAvgError.getValue();
        if (avgError == null) {
            avgError = new AvgError();
        }
        avgError.addMeasurement(error);
        mAvgError.setValue(avgError);

        // Set location and error pairs
        Pair<Location, MeasuredError> pair = new Pair<>(location, error);
        mLocationErrorPair.setValue(pair);
        mLocationErrorPairs.add(pair);
    }

    public void reset() {
        // Reset error measurements
        AvgError avgError = mAvgError.getValue();
        if (avgError != null) {
            avgError.reset();
            mAvgError.setValue(avgError);
        }

        // Reset location and error pair and pair list
        mLocationErrorPair.setValue(null);
        mLocationErrorPairs = new ArrayList<>();
    }

    /**
     * Called when the lifecycle of the observer is ended
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        reset();
    }
}
