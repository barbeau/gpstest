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

import android.content.Intent;
import android.location.Location;

import static com.android.gpstest.util.LocationUtils.isValidLatitude;
import static com.android.gpstest.util.LocationUtils.isValidLongitude;

public class IOUtils {

    /**
     * Returns the ground truth location encapsulated in the Intent if the provided Intent has a
     * SHOW_RADAR action (com.google.android.radar.SHOW_RADAR) with a valid latitude and longitude, or
     * null if the Intent doesn't have a SHOW_RADAR action or the intent has an invalid latitude or longitude
     *
     * @param intent Intent possibly containing the RADAR action
     * @return the ground truth location encapsulated in the Intent if the provided Intent has a
     * SHOW_RADAR action (com.google.android.radar.SHOW_RADAR) with a valid latitude and longitude, or
     * null if the Intent doesn't have a SHOW_RADAR action or the intent has an invalid latitude or longitude
     */
    public static Location getLocationFromIntent(Intent intent) {
        Location groundTruth = null;
        if (isShowRadarIntent(intent)) {
            double lat = Double.NaN, lon = Double.NaN;
            float latFloat = intent.getFloatExtra(Application.get().getString(R.string.radar_lat_key), Float.NaN);
            float lonFloat = intent.getFloatExtra(Application.get().getString(R.string.radar_lon_key), Float.NaN);
            if (isValidLatitude(latFloat) && isValidLongitude(lonFloat)) {
                // Use the float values
                lat = (double) latFloat;
                lon = (double) lonFloat;
            } else {
                // Try parsing doubles
                double latDouble = intent.getDoubleExtra(Application.get().getString(R.string.radar_lat_key), Double.NaN);
                double lonDouble = intent.getDoubleExtra(Application.get().getString(R.string.radar_lon_key), Double.NaN);
                if (isValidLatitude(latDouble) && isValidLongitude(lonDouble)) {
                    lat = latDouble;
                    lon = lonDouble;
                }
            }

            if (isValidLatitude(lat) && isValidLongitude(lon)) {
                groundTruth = new Location("ground_truth");
                groundTruth.setLatitude(lat);
                groundTruth.setLongitude(lon);
                if (intent.hasExtra(Application.get().getString(R.string.radar_alt_key))) {
                    float altitude = intent.getFloatExtra(Application.get().getString(R.string.radar_alt_key), Float.NaN);
                    if (!Float.isNaN(altitude)) {
                        groundTruth.setAltitude(altitude);
                    } else {
                        // Try the double version
                        double altitudeDouble = intent.getDoubleExtra(Application.get().getString(R.string.radar_alt_key), Double.NaN);
                        if (!Double.isNaN(altitudeDouble)) {
                            groundTruth.setAltitude(altitudeDouble);
                        }
                    }
                }
            }
        }
        return groundTruth;
    }

    /**
     * Returns true if the provided intent has the SHOW_RADAR action (com.google.android.radar.SHOW_RADAR), or false if it does not
     *
     * @param intent
     * @return true if the provided intent has the SHOW_RADAR action (com.google.android.radar.SHOW_RADAR), or false if it does not
     */
    public static boolean isShowRadarIntent(Intent intent) {
        return intent != null &&
                intent.getAction() != null &&
                intent.getAction().equals(Application.get().getString(R.string.show_radar_intent));
    }
}
