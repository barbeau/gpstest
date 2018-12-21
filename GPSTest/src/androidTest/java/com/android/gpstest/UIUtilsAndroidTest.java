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

import com.android.gpstest.util.UIUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class UIUtilsAndroidTest {

    @Test
    public void testGetDMSFromLocationLat() {
        String dms = UIUtils.getDMSFromLocation(getTargetContext(), -42.853583, "lat");
        assertEquals("S 42° 51' 12.90\"", dms);
    }

    @Test
    public void testGetDMSFromLocationLon() {
        String dms = UIUtils.getDMSFromLocation(getTargetContext(),47.64896, "lon");
        assertEquals("E 47° 38' 56.26\"", dms);
    }

    @Test
    public void testGetDDMFromLocationLat() {
        String ddm = UIUtils.getDDMFromLocation(getTargetContext(),24.15346, "lat");
        assertEquals("N 24° 9.208", ddm);
    }

    @Test
    public void testGetDDMFromLocationLon() {
        String ddm = UIUtils.getDDMFromLocation(getTargetContext(),-150.94523, "lon");
        assertEquals("W 150° 56.714", ddm);
    }
}
