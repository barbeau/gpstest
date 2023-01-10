/*
 * Copyright (C) 2013 Sean J. Barbeau
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

package com.android.gpstest.library.util;

import static com.android.gpstest.library.model.GnssType.SBAS;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GnssMeasurement;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.android.gpstest.library.model.GnssType;
import com.android.gpstest.library.model.SatelliteName;
import com.android.gpstest.library.model.SatelliteStatus;

/**
 * Utilities to manage GNSS signal and satellite information
 */
public class SatelliteUtils {

    private static final String TAG = "SatelliteUtils";

    /**
     * Returns the satellite name for a satellite given the constellation type and svid.  For
     * Android 7.0 and higher.
     *
     * @param gnssType constellation type
     * @param svid identification number
     * @return SatelliteName for the given constellation type and svid
     */
    public static SatelliteName getSatelliteName(GnssType gnssType, int svid) {
        // TODO - support more satellite names
        switch (gnssType) {
            case NAVSTAR:
                return SatelliteName.UNKNOWN;
            case GLONASS:
                return SatelliteName.UNKNOWN;
            case BEIDOU:
                return SatelliteName.UNKNOWN;
            case QZSS:
                return SatelliteName.UNKNOWN;
            case GALILEO:
                return SatelliteName.UNKNOWN;
            case IRNSS:
                return SatelliteName.UNKNOWN;
            case SBAS:
                if (svid == 120) {
                    return SatelliteName.INMARSAT_3F2;
                } else if (svid == 123) {
                    return SatelliteName.ASTRA_5B;
                } else if (svid == 126) {
                    return SatelliteName.INMARSAT_3F5;
                } else if (svid == 131) {
                    return SatelliteName.GEO5;
                } else if (svid == 133) {
                    return SatelliteName.INMARSAT_4F3;
                } else if (svid == 135) {
                    return SatelliteName.GALAXY_15;
                } else if (svid == 136) {
                    return SatelliteName.SES_5;
                } else if (svid == 138) {
                    return SatelliteName.ANIK;
                }
                return SatelliteName.UNKNOWN;
            case UNKNOWN:
                return SatelliteName.UNKNOWN;
            default:
                return SatelliteName.UNKNOWN;
        }
    }

    /**
     * Returns true if this device supports the Sensor.TYPE_ROTATION_VECTOR sensor, false if it
     * doesn't
     *
     * @return true if this device supports the Sensor.TYPE_ROTATION_VECTOR sensor, false if it
     * doesn't
     */
    public static boolean isRotationVectorSensorSupported(Context context) {
        SensorManager sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
    }

    /**
     * Returns true if the platform supports providing carrier frequencies for each satellite, false if it does not
     *
     * @return true if the platform supports providing carrier frequencies for each satellite, false if it does not
     */
    public static boolean isCfSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Returns true if automatic gain control is supported for this GNSS measurement, false if it is not
     * @param gnssMeasurement
     * @return true if automatic gain control is supported for this GNSS measurement, false if it is not
     */
    public static boolean isAutomaticGainControlSupported(GnssMeasurement gnssMeasurement) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && gnssMeasurement.hasAutomaticGainControlLevelDb();
    }

    /**
     * Returns true if carrier phase is supported for this GNSS measurement, false if it is not
     * @param gnssMeasurement
     * @return true if carrier phase is supported for this GNSS measurement, false if it is not
     */
    public static boolean isCarrierPhaseSupported(GnssMeasurement gnssMeasurement) {
        return isAccumulatedDeltaRangeStateValid(gnssMeasurement.getAccumulatedDeltaRangeState())
                && gnssMeasurement.getAccumulatedDeltaRangeMeters() != 0.0d;
    }


    /**
     * Returns the result of the GnssMeasurement.ADR_STATE_VALID bitmask being applied to the
     * AccumulatedDeltaRangeState from a GnssMeasurement - true if the ADR state is valid,
     * false if it is not
     * @param accumulatedDeltaRangeState accumulatedDeltaRangeState from GnssMeasurement
     * @return the result of the GnssMeasurement.ADR_STATE_VALID bitmask being applied to the
     *      * AccumulatedDeltaRangeState of the given GnssMeasurement - true if the ADR state is valid,
     *      * false if it is not
     */
    public static boolean isAccumulatedDeltaRangeStateValid(int accumulatedDeltaRangeState) {
        return (GnssMeasurement.ADR_STATE_VALID & accumulatedDeltaRangeState) == GnssMeasurement.ADR_STATE_VALID;
    }

    /**
     * Returns true if the platform supports the Android GnssAntennaInfo (https://developer.android.com/reference/android/location/GnssAntennaInfo.Listener)
     * , false if it does not
     * @return true if the platform supports the Android GnssAntennaInfo (https://developer.android.com/reference/android/location/GnssAntennaInfo.Listener)
     *      , false if it does not
     */
    public static boolean isGnssAntennaInfoSupported(LocationManager manager) {
        if (manager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return manager.getGnssCapabilities().hasAntennaInfo();
        } else {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager.getGnssCapabilities().hasGnssAntennaInfo();
        }
    }

    /**
     * Returns true if "force full GNSS measurements" can be programmatically invoked, and false if not
     * @return true if "force full GNSS measurements" can be programmatically invoked, and false if not
     */
    public static boolean isForceFullGnssMeasurementsSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /**
     * Returns true if the platform supports GNSS measurements, false if it does not.
     * @return true if the platform supports GNSS measurements, false if it does not
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static boolean isMeasurementsSupported(LocationManager manager) {
        return manager != null && manager.getGnssCapabilities().hasMeasurements();
    }

    /**
     * Returns true if the platform supports navigation messages, false if it does not.
     * @return true if the platform supports navigation messages, false if it does not
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static boolean isNavMessagesSupported(LocationManager manager) {
        return manager != null && manager.getGnssCapabilities().hasNavigationMessages();
    }

    /**
     * Creates a unique key to identify this satellite using a combination of both the svid and
     * constellation type
     *
     * @return a unique key to identify this satellite using a combination of both the svid and
     * constellation type
     */
    public static String createGnssSatelliteKey(SatelliteStatus status) {
        if (status.getGnssType() == SBAS) {
            return status.getSvid() + " " + status.getGnssType() + " " + status.getSbasType();
        } else {
            // GNSS
            return status.getSvid() + " " + status.getGnssType();
        }
    }

    /**
     * Creates a unique key to identify a particular signal, or GnssStatus, from a satellite using a
     * combination of both the svid and constellation type and carrier frequency
     *
     * @return a unique key to identify a particular signal, or GnssStatus, from a satellite using a
     * combination of both the svid and constellation type and carrier frequency
     */
    public static String createGnssStatusKey(SatelliteStatus status) {
        String carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(status);
        if (status.getGnssType() == SBAS) {
            return status.getSvid() + " " + status.getGnssType() + " " + status.getSbasType() + " " + carrierLabel;
        } else {
            // GNSS
            return status.getSvid() + " " + status.getGnssType() + " " + carrierLabel;
        }
    }
}
