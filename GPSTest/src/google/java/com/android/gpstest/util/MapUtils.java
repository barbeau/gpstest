/*
 * Copyright (C) 2014-2018 University of South Florida (sjbarbeau@gmail.com), Sean J. Barbeau
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

import com.google.android.gms.maps.model.LatLng;

public class MapUtils {

    /**
     * Converts a latitude/longitude to a LatLng.
     *
     * @param lat The latitude.
     * @param lon The longitude.
     * @return A LatLng representing this latitude/longitude.
     */
    public static final LatLng makeLatLng(double lat, double lon) {
        return new LatLng(lat, lon);
    }

    /**
     * Converts a Location to a LatLng.
     *
     * @param l Location to convert
     * @return A LatLng representing this Location.
     */
    public static final LatLng makeLatLng(Location l) {
        return makeLatLng(l.getLatitude(), l.getLongitude());
    }

    /**
     * Converts a LatLng to a Location.
     *
     * @param latLng LatLng to convert
     * @return A Location representing this LatLng.
     */
    public static final Location makeLocation(LatLng latLng) {
        Location l = new Location("FromLatLng");
        l.setLatitude(latLng.latitude);
        l.setLongitude(latLng.longitude);
        return l;
    }
}
