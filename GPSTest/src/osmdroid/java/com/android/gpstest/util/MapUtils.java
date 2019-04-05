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

import org.osmdroid.util.GeoPoint;

public class MapUtils {

    /**
     * Converts a latitude/longitude to a GeoPoint.
     *
     * @param lat The latitude.
     * @param lon The longitude.
     * @return A GeoPoint representing this latitude/longitude.
     */
    public static GeoPoint makeGeoPoint(double lat, double lon) {
        return new GeoPoint(lat, lon);
    }

    /**
     * Converts a Location to a GeoPoint.
     *
     * @param l Location to convert
     * @return A GeoPoint representing this Location.
     */
    public static GeoPoint makeGeoPoint(Location l) {
        return makeGeoPoint(l.getLatitude(), l.getLongitude());
    }

    /**
     * Converts a GeoPoint to a Location.
     *
     * @param geoPoint LatLng to convert
     * @return A Location representing this LatLng.
     */
    public static Location makeLocation(GeoPoint geoPoint) {
        Location l = new Location("FromLatLng");
        l.setLatitude(geoPoint.getLatitude());
        l.setLongitude(geoPoint.getLongitude());
        return l;
    }
}
