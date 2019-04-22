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

import android.content.res.Configuration;
import android.content.res.Resources;

import com.android.gpstest.util.LocationUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import androidx.test.runner.AndroidJUnit4;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LocationUtilsTest {

    /**
     * Test validate latitude
     */
    @Test
    public void testIsValidLatitude() {
        // Test English
        setLocale("en", "US");

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

        // Test German
        setLocale("de", "DE");

        // Good latitudes
        assertTrue(LocationUtils.isValidLatitude("0,0"));
        assertTrue(LocationUtils.isValidLatitude("1"));
        assertTrue(LocationUtils.isValidLatitude("-82,0"));
        assertTrue(LocationUtils.isValidLatitude("-90,0"));
        assertTrue(LocationUtils.isValidLatitude("82,0"));
        assertTrue(LocationUtils.isValidLatitude("90,0"));

        // Bad latitudes
        assertFalse(LocationUtils.isValidLatitude("NaN"));
        assertFalse(LocationUtils.isValidLatitude("abcd"));
        assertFalse(LocationUtils.isValidLatitude("-90,1"));
        assertFalse(LocationUtils.isValidLatitude("90,1"));
    }

    /**
     * Test validate longitude
     */
    @Test
    public void testIsValidLongitude() {
        // Test English
        setLocale("en", "US");

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

        // Test German
        setLocale("de", "DE");

        // Good longitudes
        assertTrue(LocationUtils.isValidLongitude("0,0"));
        assertTrue(LocationUtils.isValidLongitude("1"));
        assertTrue(LocationUtils.isValidLongitude("-82,0"));
        assertTrue(LocationUtils.isValidLongitude("-90,0"));
        assertTrue(LocationUtils.isValidLongitude("82,0"));
        assertTrue(LocationUtils.isValidLongitude("90,0"));
        assertTrue(LocationUtils.isValidLongitude("-90,1"));
        assertTrue(LocationUtils.isValidLongitude("90,1"));
        assertTrue(LocationUtils.isValidLongitude("-180,0"));
        assertTrue(LocationUtils.isValidLongitude("180,0"));

        // Bad longitudes
        assertFalse(LocationUtils.isValidLongitude("abcd"));
        assertFalse(LocationUtils.isValidLongitude("-180,1"));
        assertFalse(LocationUtils.isValidLongitude("180,1"));
    }

    /**
     * Test validate altitude
     */
    @Test
    public void testIsValidAltitude() {
        // Test English
        setLocale("en", "US");

        // Good altitudes
        assertTrue(LocationUtils.isValidAltitude("0.0"));
        assertTrue(LocationUtils.isValidAltitude("1"));
        assertTrue(LocationUtils.isValidAltitude("-10.0"));
        assertTrue(LocationUtils.isValidAltitude("-10.0"));
        assertTrue(LocationUtils.isValidAltitude("1000.0"));
        assertTrue(LocationUtils.isValidAltitude("-1000.0"));

        // Bad altitudes
        assertFalse(LocationUtils.isValidAltitude("abcd"));

        // Test German
        setLocale("de", "DE");

        // Good altitudes
        assertTrue(LocationUtils.isValidAltitude("0,0"));
        assertTrue(LocationUtils.isValidAltitude("1"));
        assertTrue(LocationUtils.isValidAltitude("-10,0"));
        assertTrue(LocationUtils.isValidAltitude("-10,0"));
        assertTrue(LocationUtils.isValidAltitude("1000,0"));
        assertTrue(LocationUtils.isValidAltitude("-1000,0"));

        // Bad altitudes
        assertFalse(LocationUtils.isValidAltitude("abcd"));
    }

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        // Update locale for date formatters
        Locale.setDefault(locale);
        // Update locale for app resources
        Resources res = getTargetContext().getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
