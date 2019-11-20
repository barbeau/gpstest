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

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.android.gpstest.model.Satellite;
import com.android.gpstest.model.SatelliteStatus;
import com.android.gpstest.util.CarrierFreqUtils;
import com.android.gpstest.util.SatelliteUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.gpstest.util.CarrierFreqUtils.CF_UNKNOWN;

/**
 * View model that holds device properties
 */
public class DeviceInfoViewModel extends AndroidViewModel {

    private MutableLiveData<Map<String, Satellite>> mGnssSatellites = new MutableLiveData<>();

    private MutableLiveData<Map<String, Satellite>> mSbasSatellites = new MutableLiveData<>();

    private boolean mIsDualFrequencyInView = false;

    private boolean mIsDualFrequencyInUse = false;

    /**
     * Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     */
    private Map<String, SatelliteStatus> mDuplicateCarrierStatuses = new HashMap();

    /**
     * Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     */
    private Map<String, SatelliteStatus> mUnknownCarrierStatuses = new HashMap();

    public DeviceInfoViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Map<String, Satellite>> getGnssSatellites() {
        return mGnssSatellites;
    }

    public MutableLiveData<Map<String, Satellite>> geSbasSatellites() {
        return mSbasSatellites;
    }

    /**
     * Returns a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     *
     * @return a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     */
    public Map<String, SatelliteStatus> getDuplicateCarrierStatuses() {
        return mDuplicateCarrierStatuses;
    }

    /**
     * Returns a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     *
     * @return a map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     */
    public Map<String, SatelliteStatus> getUnknownCarrierStatuses() {
        return mUnknownCarrierStatuses;
    }

    /**
     * Returns true if this device is viewing multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is viewing multiple signals from the same satellite, false if it is not
     */
    public boolean isDualFrequencyInView() {
        return mIsDualFrequencyInView;
    }

    /**
     * Returns true if this device is using multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is using multiple signals from the same satellite, false if it is not
     */
    public boolean isDualFrequencyInUse() {
        return mIsDualFrequencyInUse;
    }

    /**
     * Adds a new set of GNSS and SBAS status objects (signals) so they can be analyzed and grouped
     * into satellites
     *
     * @param gnssStatuses a new set of GNSS and SBAS status objects (signals)
     */
    public void setStatuses(List<SatelliteStatus> gnssStatuses, List<SatelliteStatus> sbasStatuses) {
        Map<String, Satellite> gnssSatellites = getSatellitesFromStatuses(gnssStatuses);
        Map<String, Satellite> sbasSatellites = getSatellitesFromStatuses(sbasStatuses);

        mGnssSatellites.setValue(gnssSatellites);
        mSbasSatellites.setValue(sbasSatellites);
    }

    /**
     * Returns a map with the provided status grouped into satellites
     * @param allStatuses all statuses for either all GNSS or SBAS constellations
     * @return a map with the provided status grouped into satellites. The key to the map is the combination of constellation and ID
     * created using SatelliteUtils.createGnssSatelliteKey().
     */
    private Map<String, Satellite> getSatellitesFromStatuses(List<SatelliteStatus> allStatuses) {
        Map<String, Satellite> satellites = new HashMap<>();

        if (allStatuses == null) {
            return satellites;
        }

        for (SatelliteStatus s : allStatuses) {
            String key = SatelliteUtils.createGnssSatelliteKey(s);
            // Get carrier label
            String carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(s);
            if (carrierLabel.equals(CF_UNKNOWN)) {
                mUnknownCarrierStatuses.put(SatelliteUtils.createGnssStatusKey(s), s);
            }

            Map<String, SatelliteStatus> satStatuses;
            if (!satellites.containsKey(key)) {
                // Create new satellite and add signal
                satStatuses = new HashMap<>();
                satStatuses.put(carrierLabel, s);
                Satellite sat = new Satellite(key, satStatuses);
                satellites.put(key, sat);
            } else {
                // Add signal to existing satellite
                Satellite sat = satellites.get(key);
                satStatuses = sat.getStatus();
                if (!satStatuses.containsKey(carrierLabel)) {
                    // We found another frequency for this satellite
                    satStatuses.put(carrierLabel, s);
                    mIsDualFrequencyInView = true;
                    int frequenciesInUse = 0;
                    for (SatelliteStatus satelliteStatus : satStatuses.values()) {
                        if (satelliteStatus.getUsedInFix()) {
                            frequenciesInUse++;
                        }
                    }
                    if (frequenciesInUse > 1) {
                        mIsDualFrequencyInUse = true;
                    }
                } else {
                    // This shouldn't happen - we found a satellite signal with the same constellation, sat ID, and carrier frequency (including multiple "unknown" or "unsupported" frequencies) as an existing one
                    mDuplicateCarrierStatuses.put(SatelliteUtils.createGnssStatusKey(s), s);
                }
            }
        }
        return satellites;
    }

    public void reset() {
        mGnssSatellites.setValue(null);
        mSbasSatellites.setValue(null);
        mDuplicateCarrierStatuses = new HashMap<>();
        mUnknownCarrierStatuses = new HashMap<>();
        mIsDualFrequencyInView = false;
        mIsDualFrequencyInUse = false;
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
