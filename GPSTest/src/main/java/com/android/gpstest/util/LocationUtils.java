/*
 * Copyright (C) 2014-2018 University of South Florida, Sean J. Barbeau (sjbarbeau@gmail.com)
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

import android.location.Location;
import android.os.SystemClock;

import java.text.NumberFormat;

public class LocationUtils {

    /**
     * Returns the human-readable details of a Location (provider, lat/long, accuracy, timestamp)
     *
     * @return the details of a Location (provider, lat/long, accuracy, timestamp) in a string
     */
    public static String printLocationDetails(Location loc) {
        if (loc == null) {
            return "";
        }

        long timeDiff;
        double timeDiffSec;

        timeDiff = SystemClock.elapsedRealtimeNanos() - loc.getElapsedRealtimeNanos();
        // Convert to seconds
        timeDiffSec = timeDiff / 1E9;

        StringBuilder sb = new StringBuilder();
        sb.append(loc.getProvider());
        sb.append(' ');
        sb.append(loc.getLatitude());
        sb.append(',');
        sb.append(loc.getLongitude());
        if (loc.hasAccuracy()) {
            sb.append(' ');
            sb.append(loc.getAccuracy());
        }
        sb.append(", ");
        sb.append(String.format("%.0f", timeDiffSec) + " second(s) ago");

        return sb.toString();
    }

    /**
     * Returns true if the provided string is a valid latitude value, false if it is not
     * @param latitude the latitude value to validate
     * @return true if the provided string is a valid latitude value, false if it is not
     */
    public static boolean isValidLatitude(String latitude) {
        double latitudeDouble;
        try {
            latitudeDouble = NumberFormat.getInstance().parse(latitude).doubleValue();
        } catch (Exception e) {
            return false;
        }
        return isValidLatitude(latitudeDouble);
    }

    /**
     * Returns true if the provided latitude is a valid latitude value, false if it is not
     *
     * @param latitude the latitude value to validate
     * @return true if the provided latitude is a valid latitude value, false if it is not
     */
    public static boolean isValidLatitude(double latitude) {
        return latitude >= -90.0d && latitude <= 90.0d;
    }

    /**
     * Returns true if the provided string is a valid longitude value, false if it is not
     * @param longitude the longitude value to validate
     * @return true if the provided string is a valid longitude value, false if it is not
     */
    public static boolean isValidLongitude(String longitude) {
        double longitudeDouble;
        try {
            longitudeDouble = NumberFormat.getInstance().parse(longitude).doubleValue();
        } catch (Exception e) {
            return false;
        }
        return isValidLongitude(longitudeDouble);
    }

    /**
     * Returns true if the provided longitude is a valid longitude value, false if it is not
     *
     * @param longitude the longitude value to validate
     * @return true if the provided longitude is a valid longitude value, false if it is not
     */
    public static boolean isValidLongitude(double longitude) {
        return longitude >= -180.0d && longitude <= 180.0d;
    }

    /**
     * Returns true if the provided string is a valid altitude value, false if it is not
     * @param altitude the altitude value to validate
     * @return true if the provided string is a valid altitude value, false if it is not
     */
    public static boolean isValidAltitude(String altitude) {
        double altitudeDouble;
        try {
            altitudeDouble = NumberFormat.getInstance().parse(altitude).doubleValue();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
