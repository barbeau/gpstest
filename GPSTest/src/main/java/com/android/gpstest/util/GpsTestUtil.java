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

package com.android.gpstest.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GnssMeasurement;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import com.android.gpstest.Application;
import com.android.gpstest.model.GnssType;
import com.android.gpstest.model.SatelliteName;
import com.android.gpstest.model.SbasType;

import java.lang.reflect.InvocationTargetException;

import androidx.annotation.RequiresApi;

import static com.android.gpstest.model.GnssType.BEIDOU;
import static com.android.gpstest.model.GnssType.GALILEO;
import static com.android.gpstest.model.GnssType.GLONASS;
import static com.android.gpstest.model.GnssType.NAVSTAR;
import static com.android.gpstest.model.GnssType.QZSS;
import static com.android.gpstest.model.GnssType.SBAS;
import static com.android.gpstest.model.GnssType.UNKNOWN;

public class GpsTestUtil {

    private static final String TAG = "GpsTestUtil";

    private static final String NMEA_OUTPUT_TAG = "GpsOutputNmea";

    private static final String MEASURE_OUTPUT_TAG = "GpsOutputMeasure";

    private static final String NM_OUTPUT_TAG = "GpsOutputNav";

    private static StringBuilder mNmeaOutput = new StringBuilder();

    /**
     * Returns the Global Navigation Satellite System (GNSS) for a satellite given the PRN.  For
     * Android 6.0.1 (API Level 23) and lower.  Android 7.0 and higher should use getGnssConstellationType()
     *
     * @param prn PRN value provided by the GpsSatellite.getPrn() method
     * @return GnssType for the given PRN
     */
    @Deprecated
    public static GnssType getGnssType(int prn) {
        if (prn >= 1 && prn <= 32) {
            return NAVSTAR;
        } else if (prn == 33) {
            return SBAS;
        } else if (prn == 39) {
            // See Issue #205
            return SBAS;
        } else if (prn >= 40 && prn <= 41) {
            // See Issue #92
            return SBAS;
        } else if (prn == 46) {
            return SBAS;
        } else if (prn == 48) {
            return SBAS;
        } else if (prn == 49) {
            return SBAS;
        } else if (prn == 51) {
            return SBAS;
        } else if (prn >= 65 && prn <= 96) {
            // See Issue #26 for details
            return GLONASS;
        } else if (prn >= 193 && prn <= 200) {
            // See Issue #54 for details
            return QZSS;
        } else if (prn >= 201 && prn <= 235) {
            // See Issue #54 for details
            return BEIDOU;
        } else if (prn >= 301 && prn <= 336) {
            // See https://github.com/barbeau/gpstest/issues/58#issuecomment-252235124 for details
            return GALILEO;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Returns the Global Navigation Satellite System (GNSS) for a satellite given the GnssStatus
     * constellation type.  For Android 7.0 and higher.  This is basically a translation to our
     * own GnssType enumeration that we use for Android 6.0.1 and lower.  Note that
     * getSbasConstellationType() should be used to get the particular SBAS constellation type
     *
     * @param gnssConstellationType constellation type provided by the GnssStatus.getConstellationType()
     *                              method
     * @return GnssType for the given GnssStatus constellation type
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static GnssType getGnssConstellationType(int gnssConstellationType) {
        switch (gnssConstellationType) {
            case GnssStatus.CONSTELLATION_GPS:
                return NAVSTAR;
            case GnssStatus.CONSTELLATION_GLONASS:
                return GLONASS;
            case GnssStatus.CONSTELLATION_BEIDOU:
                return BEIDOU;
            case GnssStatus.CONSTELLATION_QZSS:
                return QZSS;
            case GnssStatus.CONSTELLATION_GALILEO:
                return GALILEO;
            case GnssStatus.CONSTELLATION_SBAS:
                return SBAS;
            case GnssStatus.CONSTELLATION_UNKNOWN:
                return UNKNOWN;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Returns the SBAS constellation type for a GnssStatus.CONSTELLATION_SBAS satellite given the GnssStatus
     * svid.  For Android 7.0 and higher.
     *
     * @param svid identification number provided by the GnssStatus.getSvid() method
     * @return SbasType for the given GnssStatus svid for GnssStatus.CONSTELLATION_SBAS satellites
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static SbasType getSbasConstellationType(int svid) {
        if (svid == 120 || svid == 123 || svid == 126 || svid == 136) {
            return SbasType.EGNOS;
        } else if (svid == 131 || svid == 133 || svid == 135 || svid == 138) {
            return SbasType.WAAS;
        } else if (svid == 127 || svid == 128 || svid == 139) {
            return SbasType.GAGAN;
        } else if (svid == 129 || svid == 137) {
            return SbasType.MSAS;
        }
        return SbasType.UNKNOWN;
    }

    /**
     * Returns the SBAS constellation type for a satellite for Android 6.0.1 and lower
     *
     * @param svid PRN provided by the GpsSatellite.getPrn() method method
     * @return SbasType for the given GpsSatellite.getPrn() method
     */
    @SuppressLint("NewApi")
    @Deprecated
    public static SbasType getSbasConstellationTypeLegacy(int svid) {
        return getSbasConstellationType(svid + 87);
    }

    /**
     * Returns the satellite name for a satellite given the constellation type and svid.  For
     * Android 7.0 and higher.
     *
     * @param gnssType constellation type
     * @param svid identification number
     * @return SatelliteName for the given constellation type and svid
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD &&
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
    }

    /**
     * Returns true if the app is running on a large screen device, false if it is not
     *
     * @return true if the app is running on a large screen device, false if it is not
     */
    public static boolean isLargeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Returns true if the device supports the Gnss status listener, false if it does not
     *
     * @return true if the device supports the Gnss status listener, false if it does not
     */
    public static boolean isGnssStatusListenerSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * Returns true if the platform supports providing carrier frequencies for each satellite, false if it does not
     *
     * @return true if the platform supports providing carrier frequencies for each satellite, false if it does not
     */
    public static boolean isGnssCarrierFrequenciesSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Returns true if the platform supports providing vertical accuracy values, false if it does not
     *
     * @return true if the platform supports providing vertical accuracy values, false if it does not
     */
    public static boolean isVerticalAccuracySupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Returns true if the platform supports providing speed and bearing accuracy values, false if it does not
     *
     * @return true if the platform supports providing speed and bearing accuracy values, false if it does not
     */
    public static boolean isSpeedAndBearingAccuracySupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Creates a unique key to identify this satellite using a combination of both the svid and
     * constellation type
     *
     * @return a unique key to identify this satellite using a combination of both the svid and
     * constellation type
     */
    public static String createGnssSatelliteKey(int svid, int constellationType) {
        return String.valueOf(svid) + " " + String.valueOf(constellationType);
    }


    /**
     * Outputs the provided nmea message and timestamp to log
     *
     * @param timestamp timestamp to write to the log, or Long.MIN_VALUE to not write a timestamp
     *                  to
     *                  log
     */
    public static void writeNmeaToLog(String nmea, long timestamp) {
        mNmeaOutput.setLength(0);
        if (timestamp != Long.MIN_VALUE) {
            mNmeaOutput.append(timestamp);
            mNmeaOutput.append(",");
        }
        mNmeaOutput.append(nmea);
        Log.d(NMEA_OUTPUT_TAG, mNmeaOutput.toString());
    }

    /**
     * Outputs the provided GNSS navigation message to log
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void writeNavMessageToLog(GnssNavigationMessage message) {
        Log.d(NM_OUTPUT_TAG, message.toString());
    }

    /**
     * Outputs the provided GNSS measurement to log
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void writeGnssMeasurementToLog(GnssMeasurement measurement) {
        Log.d(MEASURE_OUTPUT_TAG, measurement.toString());
    }

    /**
     * Returns the GNSS hardware year for the device, or null if the year couldn't be determined
     * @return the GNSS hardware year for the device, or null if the year couldn't be determined
     */
    public static String getGnssHardwareYear() {
        java.lang.reflect.Method method;
        LocationManager locationManager = (LocationManager) Application.get().getSystemService(Context.LOCATION_SERVICE);
        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
            int hwYear = (int) method.invoke(locationManager);
            if (hwYear == 0) {
                return "GNSS HW Year: " + "2015 or older \n";
            } else {
                return "GNSS HW Year: " + hwYear + "\n";
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "No such method exception: ", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal Access exception: ", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Invocation Target Exception: ", e);
        }
        return null;
    }
}
