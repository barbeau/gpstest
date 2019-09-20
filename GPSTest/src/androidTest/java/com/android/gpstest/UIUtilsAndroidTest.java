/*
 * Copyright (C) 2018 Sean J. Barbeau (sjbarbeau@gmail.com)
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

import androidx.test.runner.AndroidJUnit4;

import com.android.gpstest.util.UIUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class UIUtilsAndroidTest {

    @Test
    public void testGetDMSFromLocationLat() {
        // Test German
        setLocale("de", "DE");

        String dms = UIUtils.getDMSFromLocation(getTargetContext(), -42.853583, UIUtils.COORDINATE_LATITUDE);
        assertEquals("S\t\u200742° 51' 12,90\"", dms);

        // Test English
        setLocale("en", "US");

        dms = UIUtils.getDMSFromLocation(getTargetContext(), -42.853583, UIUtils.COORDINATE_LATITUDE);
        assertEquals("S\t\u200742° 51' 12.90\"", dms);
    }

    @Test
    public void testGetDMSFromLocationLon() {
        // Test German
        setLocale("de", "DE");

        String dms = UIUtils.getDMSFromLocation(getTargetContext(), 47.64896, UIUtils.COORDINATE_LONGITUDE);
        assertEquals("E\t047° 38' 56,26\"", dms);

        // Test English
        setLocale("en", "US");

        dms = UIUtils.getDMSFromLocation(getTargetContext(), 47.64896, UIUtils.COORDINATE_LONGITUDE);
        assertEquals("E\t047° 38' 56.26\"", dms);
    }

    @Test
    public void testGetDDMFromLocationLat() {
        // Test German
        setLocale("de", "DE");

        String ddm = UIUtils.getDDMFromLocation(getTargetContext(), 24.15346, UIUtils.COORDINATE_LATITUDE);
        assertEquals("N\t\u200724° 09,208", ddm);

        // Test English
        setLocale("en", "US");

        ddm = UIUtils.getDDMFromLocation(getTargetContext(), 24.15346, UIUtils.COORDINATE_LATITUDE);
        assertEquals("N\t\u200724° 09.208", ddm);
    }

    @Test
    public void testGetDDMFromLocationLon() {
        // Test English
        setLocale("en", "US");

        String ddm = UIUtils.getDDMFromLocation(getTargetContext(), -150.94523, UIUtils.COORDINATE_LONGITUDE);
        assertEquals("W\t150° 56.714", ddm);

        // Test German
        setLocale("de", "DE");

        ddm = UIUtils.getDDMFromLocation(getTargetContext(), -150.94523, UIUtils.COORDINATE_LONGITUDE);
        assertEquals("W\t150° 56,714", ddm);
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
