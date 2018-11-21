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
import android.text.TextUtils;
import android.util.Log;

import com.android.gpstest.Application;
import com.android.gpstest.DilutionOfPrecision;
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
        } else if (prn >= 301 && prn <= 330) {
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
     * Given a $GPGGA or $GNGNS NMEA sentence, return the altitude above mean sea level (geoid
     * altitude),
     * or null if the altitude can't be parsed.
     *
     * Example inputs are:
     * $GPGGA,032739.0,2804.732835,N,08224.639709,W,1,08,0.8,19.2,M,-24.0,M,,*5B
     * $GNGNS,015002.0,2804.733672,N,08224.631117,W,AAN,09,1.1,78.9,-24.0,,*23
     *
     * Example outputs would be:
     * 19.2
     * 78.9
     *
     * @param nmeaSentence a $GPGGA or $GNGNS NMEA sentence
     * @return the altitude above mean sea level (geoid altitude), or null if altitude can't be
     * parsed
     */
    public static Double getAltitudeMeanSeaLevel(String nmeaSentence) {
        final int ALTITUDE_INDEX = 9;
        String[] tokens = nmeaSentence.split(",");

        if (nmeaSentence.startsWith("$GPGGA") || nmeaSentence.startsWith("$GNGNS")) {
            String altitude;
            try {
                altitude = tokens[ALTITUDE_INDEX];
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Bad NMEA sentence for geoid altitude - " + nmeaSentence + " :" + e);
                return null;
            }
            if (!TextUtils.isEmpty(altitude)) {
                Double altitudeParsed = null;
                try {
                    altitudeParsed = Double.parseDouble(altitude);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Bad geoid altitude value of '" + altitude + "' in NMEA sentence " + nmeaSentence + " :" + e);
                }
                return altitudeParsed;
            } else {
                Log.w(TAG, "Couldn't parse geoid altitude from NMEA: " + nmeaSentence);
                return null;
            }
        } else {
            Log.w(TAG, "Input must be a $GPGGA or $GNGNS NMEA: " + nmeaSentence);
            return null;
        }
    }

    /**
     * Given a $GNGSA or $GPGSA NMEA sentence, return the dilution of precision, or null if dilution of
     * precision can't be parsed.
     *
     * Example inputs are:
     * $GPGSA,A,3,03,14,16,22,23,26,,,,,,,3.6,1.8,3.1*38
     * $GNGSA,A,3,03,14,16,22,23,26,,,,,,,3.6,1.8,3.1,1*3B
     *
     * Example output is:
     * PDOP is 3.6, HDOP is 1.8, and VDOP is 3.1
     *
     * @param nmeaSentence a $GNGSA or $GPGSA NMEA sentence
     * @return the dilution of precision, or null if dilution of precision can't be parsed
     */
    public static DilutionOfPrecision getDop(String nmeaSentence) {
        final int PDOP_INDEX = 15;
        final int HDOP_INDEX = 16;
        final int VDOP_INDEX = 17;
        String[] tokens = nmeaSentence.split(",");

        if (nmeaSentence.startsWith("$GNGSA") || nmeaSentence.startsWith("$GPGSA")) {
            String pdop, hdop, vdop;
            try {
                pdop = tokens[PDOP_INDEX];
                hdop = tokens[HDOP_INDEX];
                vdop = tokens[VDOP_INDEX];
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Bad NMEA message for parsing DOP - " + nmeaSentence + " :" + e);
                return null;
            }

            // See https://github.com/barbeau/gpstest/issues/71#issuecomment-263169174
            if (vdop.contains("*")) {
                vdop = vdop.split("\\*")[0];
            }

            if (!TextUtils.isEmpty(pdop) && !TextUtils.isEmpty(hdop) && !TextUtils.isEmpty(vdop)) {
                DilutionOfPrecision dop = null;
                try {
                    dop = new DilutionOfPrecision(Double.valueOf(pdop), Double.valueOf(hdop),
                            Double.valueOf(vdop));
                } catch (NumberFormatException e) {
                    // See https://github.com/barbeau/gpstest/issues/71#issuecomment-263169174
                    Log.e(TAG, "Invalid DOP values in NMEA: " + nmeaSentence);
                }
                return dop;
            } else {
                Log.w(TAG, "Empty DOP values in NMEA: " + nmeaSentence);
                return null;
            }
        } else {
            Log.w(TAG, "Input must be a $GNGSA NMEA: " + nmeaSentence);
            return null;
        }
    }

    /**
     * Returns the label that should be displayed for a given GNSS constellation, svid, and carrier
     * frequency in MHz, or null if no carrier frequency label is found
     *
     * @param gnssType constellation type defined in GnssType
     * @param svid identification number provided by the GnssStatus.getSvid() method
     * @param carrierFrequencyMhz carrier frequency for the signal in MHz
     * @return the label that should be displayed for a given GNSS constellation, svid, and carrier
     * frequency in MHz or null if no carrier frequency label is found
     */
    public static String getCarrierFrequencyLabel(GnssType gnssType, int svid, float carrierFrequencyMhz) {
        final float TOLERANCE_MHZ = 1f;
        switch (gnssType) {
            case NAVSTAR:
                if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                    return "L1";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1227.6f, TOLERANCE_MHZ)) {
                    return "L2";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1381.05f, TOLERANCE_MHZ)) {
                    return "L3";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1379.913f, TOLERANCE_MHZ)) {
                    return "L4";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                    return "L5";
                }
                break;
            case GLONASS:
                if (carrierFrequencyMhz >= 1598.0000f && carrierFrequencyMhz <= 1610.000f) {
                    // Actual range is 1598.0625 MHz to 1609.3125, but allow padding for float comparisons - #103
                    return "L1";
                } else if (carrierFrequencyMhz >= 1242.0000f && carrierFrequencyMhz <= 1252.000f) {
                    // Actual range is 1242.9375 - 1251.6875, but allow padding for float comparisons - #103
                    return "L2";
                } else if (carrierFrequencyMhz >= 1200.0000f && carrierFrequencyMhz <= 1210.000f) {
                    // Exact range is unclear - appears to be 1202.025 - 1207.14 - #103
                    return "L3";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                    return "L5";
                }
                break;
            case BEIDOU:
                if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1561.098f, TOLERANCE_MHZ)) {
                    return "B1";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1589.742f, TOLERANCE_MHZ)) {
                    return "B1-2";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                    return "B1C";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1207.14f, TOLERANCE_MHZ)) {
                    return "B2";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                    return "B2a";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1268.52f, TOLERANCE_MHZ)) {
                    return "B3";
                }
                break;
            case QZSS:
                if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                    return "L1";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1227.6f, TOLERANCE_MHZ)) {
                    return "L2";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                    return "L5";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1278.75f, TOLERANCE_MHZ)) {
                    return "L6";
                }
                break;
            case GALILEO:
                if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                    return "E1";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1191.795f, TOLERANCE_MHZ)) {
                    return "E5";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                    return "E5a";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1207.14f, TOLERANCE_MHZ)) {
                    return "E5b";
                } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1278.75f, TOLERANCE_MHZ)) {
                    return "E6";
                }
                break;
            case SBAS:
                if (svid == 120) {
                    // GnssType.INMARSAT_3F2
                    if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                        return "L1";
                    }
                } else if (svid == 127 || svid == 128 || svid == 139) {
                    // GnssType.GAGAN
                    if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                        return "L1";
                    }
                } else if (svid == 133) {
                    // GnssType.INMARSAT_4F3;
                    if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                        return "L1";
                    } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                        return "L5";
                    }
                } else if (svid == 135) {
                    // GnssType.GALAXY_15;
                    if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                        return "L1";
                    } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                        return "L5";
                    }
                }  else if (svid == 136) {
                    // GnssType.SES_5;
                    if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                        return "L1";
                    } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                        return "L5";
                    }
                } else if (svid == 138) {
                    // GnssType.ANIK;
                    if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42f, TOLERANCE_MHZ)) {
                        return "L1";
                    } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45f, TOLERANCE_MHZ)) {
                        return "L5";
                    }
                }
                break;
            case UNKNOWN:
                break;
            default:
                break;
        }
        // Unknown carrier frequency for given constellation and svid
        return null;
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
