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

import com.android.gpstest.model.ConstellationFamily;
import com.android.gpstest.model.Satellite;
import com.android.gpstest.model.SatelliteMetadata;
import com.android.gpstest.model.SatelliteStatus;
import com.android.gpstest.util.CarrierFreqUtils;
import com.android.gpstest.util.SatelliteUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.gpstest.model.SatelliteStatus.NO_DATA;
import static com.android.gpstest.util.CarrierFreqUtils.CF_UNKNOWN;
import static com.android.gpstest.util.CarrierFreqUtils.CF_UNSUPPORTED;

/**
 * View model that holds device properties
 */
public class DeviceInfoViewModel extends AndroidViewModel {

    private MutableLiveData<Map<String, Satellite>> mGnssSatellites = new MutableLiveData<>();

    private MutableLiveData<Map<String, Satellite>> mSbasSatellites = new MutableLiveData<>();

    private boolean mIsDualFrequencyPerSatInView = false;

    private boolean mIsDualFrequencyPerSatInUse = false;

    private boolean mIsNonPrimaryCarrierFreqInView = false;

    private boolean mIsNonPrimaryCarrierFreqInUse = false;

    /**
     * A set of metadata about all satellites the device knows of
     */
    private MutableLiveData<SatelliteMetadata> mSatelliteMetadata = new MutableLiveData<>();

    /**
     * Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected as having duplicate carrier frequency data with another signal
     */
    private Map<String, SatelliteStatus> mDuplicateCarrierStatuses = new HashMap<>();

    /**
     * Map of status keys (created using SatelliteUtils.createGnssStatusKey()) to the status that
     * has been detected with an unknown GNSS frequency
     */
    private Map<String, SatelliteStatus> mUnknownCarrierStatuses = new HashMap<>();

    public DeviceInfoViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Map<String, Satellite>> getGnssSatellites() {
        return mGnssSatellites;
    }

    public MutableLiveData<Map<String, Satellite>> getSbasSatellites() {
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
    public boolean isDualFrequencyPerSatInView() {
        return mIsDualFrequencyPerSatInView;
    }

    /**
     * Returns true if this device is using multiple signals from the same satellite, false if it is not
     *
     * @return true if this device is using multiple signals from the same satellite, false if it is not
     */
    public boolean isDualFrequencyPerSatInUse() {
        return mIsDualFrequencyPerSatInUse;
    }

    /**
     * Returns true if a non-primary carrier frequency is in view by at least one satellite, or false if
     * only primary carrier frequencies are in view
     *
     * @return true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in view
     */
    public boolean isNonPrimaryCarrierFreqInView() {
        return mIsNonPrimaryCarrierFreqInView;
    }

    /**
     * Returns true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in use
     *
     * @return true if a non-primary carrier frequency is in use by at least one satellite, or false if
     * only primary carrier frequencies are in use
     */
    public boolean isNonPrimaryCarrierFreqInUse() {
        return mIsNonPrimaryCarrierFreqInUse;
    }

    /**
     * Returns the metadata about a group of satellites
     *
     * @return the metadata about a group of satellites
     */
    public MutableLiveData<SatelliteMetadata> getSatelliteMetadata() {
        return mSatelliteMetadata;
    }


    /**
     * Adds a new set of GNSS and SBAS status objects (signals) so they can be analyzed and grouped
     * into satellites
     *
     * @param gnssStatuses a new set of GNSS status objects (signals)
     * @param sbasStatuses a new set of SBAS status objects (signals)
     */
    public void setStatuses(List<SatelliteStatus> gnssStatuses, List<SatelliteStatus> sbasStatuses) {
        ConstellationFamily gnssSatellites = getSatellitesFromStatuses(gnssStatuses);
        ConstellationFamily sbasSatellites = getSatellitesFromStatuses(sbasStatuses);

        mGnssSatellites.setValue(gnssSatellites.getSatellites());
        mSbasSatellites.setValue(sbasSatellites.getSatellites());

        int numSignalsUsed = gnssSatellites.getSatelliteMetadata().getNumSignalsUsed() + sbasSatellites.getSatelliteMetadata().getNumSignalsUsed();
        int numSignalsInView = gnssSatellites.getSatelliteMetadata().getNumSignalsInView() + sbasSatellites.getSatelliteMetadata().getNumSignalsInView();
        int numSignalsTotal = gnssSatellites.getSatelliteMetadata().getNumSignalsTotal() + sbasSatellites.getSatelliteMetadata().getNumSignalsTotal();


        int numSatsUsed = gnssSatellites.getSatelliteMetadata().getNumSatsUsed() + sbasSatellites.getSatelliteMetadata().getNumSatsUsed();
        int numSatsInView = gnssSatellites.getSatelliteMetadata().getNumSatsInView() + sbasSatellites.getSatelliteMetadata().getNumSatsInView();
        int numSatsTotal = gnssSatellites.getSatelliteMetadata().getNumSatsTotal() + sbasSatellites.getSatelliteMetadata().getNumSatsTotal();
        mSatelliteMetadata.setValue(new SatelliteMetadata(numSignalsInView, numSignalsUsed, numSignalsTotal, numSatsInView, numSatsUsed, numSatsTotal));
    }

    /**
     * Returns a map with the provided status grouped into satellites
     * @param allStatuses all statuses for either all GNSS or SBAS constellations
     * @return a map with the provided status grouped into satellites. The key to the map is the combination of constellation and ID
     * created using SatelliteUtils.createGnssSatelliteKey().
     */
    private ConstellationFamily getSatellitesFromStatuses(List<SatelliteStatus> allStatuses) {
        Map<String, Satellite> satellites = new HashMap<>();
        int numSignalsUsed = 0;
        int numSignalsInView = 0;
        int numSatsUsed = 0;
        int numSatsInView = 0;

        if (allStatuses == null) {
            return new ConstellationFamily(satellites, new SatelliteMetadata(0, 0, 0, 0, 0, 0));
        }

        for (SatelliteStatus s : allStatuses) {
            if (s.getUsedInFix()) {
                numSignalsUsed++;
            }
            if (s.getCn0DbHz() != NO_DATA) {
                numSignalsInView++;
            }

            String key = SatelliteUtils.createGnssSatelliteKey(s);
            // Get carrier label
            String carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(s);
            if (carrierLabel.equals(CF_UNKNOWN)) {
                mUnknownCarrierStatuses.put(SatelliteUtils.createGnssStatusKey(s), s);
            }
            // Check if this is a non-primary carrier frequency
            if (!carrierLabel.equals(CF_UNKNOWN) && !carrierLabel.equals(CF_UNSUPPORTED)
                    && !CarrierFreqUtils.isPrimaryCarrier(carrierLabel)) {
                mIsNonPrimaryCarrierFreqInView = true;
                if (s.getUsedInFix()) {
                    mIsNonPrimaryCarrierFreqInUse = true;
                }
            }

            Map<String, SatelliteStatus> satStatuses;
            if (!satellites.containsKey(key)) {
                // Create new satellite and add signal
                satStatuses = new HashMap<>();
                satStatuses.put(carrierLabel, s);
                Satellite sat = new Satellite(key, satStatuses);
                satellites.put(key, sat);
                if (s.getUsedInFix()) {
                    numSatsUsed++;
                }
                if (s.getCn0DbHz() != NO_DATA) {
                    numSatsInView++;
                }
            } else {
                // Add signal to existing satellite
                Satellite sat = satellites.get(key);
                satStatuses = sat.getStatus();
                if (!satStatuses.containsKey(carrierLabel)) {
                    // We found another frequency for this satellite
                    satStatuses.put(carrierLabel, s);
                    int frequenciesInUse = 0;
                    int frequenciesInView = 0;
                    for (SatelliteStatus satelliteStatus : satStatuses.values()) {
                        if (satelliteStatus.getUsedInFix()) {
                            frequenciesInUse++;
                        }
                        if (satelliteStatus.getCn0DbHz() != NO_DATA) {
                            frequenciesInView++;
                        }
                    }
                    if (frequenciesInUse > 1) {
                        mIsDualFrequencyPerSatInUse = true;
                    }
                    if (frequenciesInUse == 1 && s.getUsedInFix()) {
                        // The new frequency we just added was the first in use for this satellite
                        numSatsUsed++;
                    }
                    if (frequenciesInView > 1) {
                        mIsDualFrequencyPerSatInView = true;
                    }
                    if (frequenciesInView == 1 && s.getCn0DbHz() != NO_DATA) {
                        // The new frequency we just added was the first in view for this satellite
                        numSatsInView++;
                    }
                } else {
                    // This shouldn't happen - we found a satellite signal with the same constellation, sat ID, and carrier frequency (including multiple "unknown" or "unsupported" frequencies) as an existing one
                    mDuplicateCarrierStatuses.put(SatelliteUtils.createGnssStatusKey(s), s);
                }
            }
        }
        return new ConstellationFamily(satellites, new SatelliteMetadata(numSignalsInView, numSignalsUsed, allStatuses.size(), numSatsInView, numSatsUsed, satellites.size()));
    }

    public void reset() {
        mGnssSatellites.setValue(null);
        mSbasSatellites.setValue(null);
        mSatelliteMetadata.setValue(null);
        mDuplicateCarrierStatuses = new HashMap<>();
        mUnknownCarrierStatuses = new HashMap<>();
        mIsDualFrequencyPerSatInView = false;
        mIsDualFrequencyPerSatInUse = false;
        mIsNonPrimaryCarrierFreqInView = false;
        mIsNonPrimaryCarrierFreqInUse = false;
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
