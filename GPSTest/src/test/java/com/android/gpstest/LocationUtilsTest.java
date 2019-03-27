/*
 * Copyright (C) 2019 Sean J. Barbeau (sjbarbeau@gmail.com)
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

import com.android.gpstest.util.LocationUtils;
import com.android.gpstest.util.MathUtils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class LocationUtilsTest {

    /**
     * Test validate latitude
     */
    @Test
    public void testIsValidLatitude() {
        // Good latitudes
        assertTrue(LocationUtils.isValidLatitude("0.0"));
        assertTrue(LocationUtils.isValidLatitude("1"));
        assertTrue(LocationUtils.isValidLatitude("-82.0"));
        assertTrue(LocationUtils.isValidLatitude("-90.0"));
        assertTrue(LocationUtils.isValidLatitude("82.0"));
        assertTrue(LocationUtils.isValidLatitude("90.0"));

        // Bad latitudes
        assertFalse(LocationUtils.isValidLatitude("NaN"));
        assertFalse(LocationUtils.isValidLatitude("abcd"));
        assertFalse(LocationUtils.isValidLatitude("-90.1"));
        assertFalse(LocationUtils.isValidLatitude("90.1"));
    }

    /**
     * Test validate longitude
     */
    @Test
    public void testIsValidLongitude() {
        // Good longitudes
        assertTrue(LocationUtils.isValidLongitude("0.0"));
        assertTrue(LocationUtils.isValidLongitude("1"));
        assertTrue(LocationUtils.isValidLongitude("-82.0"));
        assertTrue(LocationUtils.isValidLongitude("-90.0"));
        assertTrue(LocationUtils.isValidLongitude("82.0"));
        assertTrue(LocationUtils.isValidLongitude("90.0"));
        assertTrue(LocationUtils.isValidLongitude("-90.1"));
        assertTrue(LocationUtils.isValidLongitude("90.1"));
        assertTrue(LocationUtils.isValidLongitude("-180.0"));
        assertTrue(LocationUtils.isValidLongitude("180.0"));

        // Bad longitudes
        assertFalse(LocationUtils.isValidLongitude("abcd"));
        assertFalse(LocationUtils.isValidLongitude("-180.1"));
        assertFalse(LocationUtils.isValidLongitude("180.1"));
    }

    /**
     * Test validate altitude
     */
    @Test
    public void testIsValidAltitude() {
        // Good altitudes
        assertTrue(LocationUtils.isValidAltitude("0.0"));
        assertTrue(LocationUtils.isValidAltitude("1"));
        assertTrue(LocationUtils.isValidAltitude("-10.0"));
        assertTrue(LocationUtils.isValidAltitude("-10.0"));
        assertTrue(LocationUtils.isValidAltitude("1000.0"));
        assertTrue(LocationUtils.isValidAltitude("-1000.0"));

        // Bad altitudes
        assertFalse(LocationUtils.isValidAltitude("abcd"));
    }
}
