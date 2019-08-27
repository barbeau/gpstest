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
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.gpstest.model.MeasuredError;

import java.util.ArrayList;

/**
 * View model that holds device properties
 */
public class DeviceInfoViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> mAllowGroundTruthEdit = new MutableLiveData<>(true);

    private boolean mBenchmarkCardCollapsed = false;

    private MutableLiveData<Pair<Location, MeasuredError>> mLocationErrorPair = new MutableLiveData<>();

    private ArrayList<Pair<Location, MeasuredError>> mLocationErrorPairs = new ArrayList<>();

    public DeviceInfoViewModel(@NonNull Application application) {
        super(application);
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

    public LiveData<Pair<Location, MeasuredError>> getLocationErrorPair() {
        return mLocationErrorPair;
    }

    /**
     * Get history of all location and error pairs from the most recent test
     *
     * @return history of all location and error pairs from the most recent test
     */
    public ArrayList<Pair<Location, MeasuredError>> getLocationErrorPairs() {
        return mLocationErrorPairs;
    }

    /**
     * Adds a new location to the view model and calculates relevate errors
     *
     * @param status
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setGnssStatus(GnssStatus status) {
//        final int length = status.getSatelliteCount();
//        int svCount = 0;
//        while (svCount < length) {
//            SatelliteStatus satStatus = new SatelliteStatus(status.getSvid(svCount), GpsTestUtil.getGnssConstellationType(status.getConstellationType(svCount)),
//                    status.getCn0DbHz(svCount),
//                    status.hasAlmanacData(svCount),
//                    status.hasEphemerisData(svCount),
//                    status.usedInFix(svCount),
//                    status.getElevationDegrees(svCount),
//                    status.getAzimuthDegrees(svCount));
//            if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
//                if (status.hasCarrierFrequencyHz(svCount)) {
//                    satStatus.setHasCarrierFrequency(true);
//                    satStatus.setCarrierFrequencyHz(status.getCarrierFrequencyHz(svCount));
//                }
//            }
//
//            if (satStatus.getGnssType() == GnssType.SBAS) {
//                satStatus.setSbasType(GpsTestUtil.getSbasConstellationType(satStatus.getSvid()));
//                mSbasStatus.add(satStatus);
//            } else {
//                mGnssStatus.add(satStatus);
//            }
//
//            if (satStatus.getUsedInFix()) {
//                mUsedInFixCount++;
//            }
//
//            svCount++;
//        }
    }

    @Deprecated
    private void updateLegacyStatus(GpsStatus status) {

    }

    public void reset() {
        // TODO
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
