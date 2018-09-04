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

import android.location.GnssStatus;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.android.gpstest.util.UIUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class UIUtilsAndroidTest {

    private static final String TAG = "DFTest";

    @Test
    public void testGetDMSFromLocation() {
        String dms = UIUtils.getDMSFromLocation(getTargetContext(), -42.853583);
        assertEquals("-42° 51' 12\"", dms);
    }

    /**
     * To validate the fields in GnssStatus class, the value is got from device
     * @param status, GnssStatus
     * @param softAssert, customized assert class.
     */
    private void validateGnssStatus(GnssStatus status, SoftAssert softAssert) {
        int sCount = status.getSatelliteCount();
        Log.i(TAG, "Total satellite:" + sCount);
        // total number of satellites for all constellation is less than 200
        softAssert.assertTrue("Satellite count test sCount : " + sCount , sCount < 200);
        for (int i = 0; i < sCount; ++i) {
            softAssert.assertTrue("azimuth_degrees: Azimuth in degrees: ",
                    "0.0 <= X <= 360.0",
                    String.valueOf(status.getAzimuthDegrees(i)),
                    status.getAzimuthDegrees(i) >= 0.0 && status.getAzimuthDegrees(i) <= 360.0);
            TestMeasurementUtil.verifyGnssCarrierFrequency(softAssert, mTestLocationManager,
                    status.hasCarrierFrequencyHz(i),
                    status.hasCarrierFrequencyHz(i) ? status.getCarrierFrequencyHz(i) : 0F);
            softAssert.assertTrue("c_n0_dbhz: Carrier-to-noise density",
                    "0.0 <= X <= 63",
                    String.valueOf(status.getCn0DbHz(i)),
                    status.getCn0DbHz(i) >= 0.0 &&
                            status.getCn0DbHz(i) <= 63.0);
            softAssert.assertTrue("elevation_degrees: Elevation in Degrees :",
                    "0.0 <= X <= 90.0",
                    String.valueOf(status.getElevationDegrees(i)),
                    status.getElevationDegrees(i) >= 0.0 && status.getElevationDegrees(i) <= 90.0);
            // in validateSvidSub, it will validate ConstellationType, svid
            // however, we don't have the event time in the current scope, pass in "-1" instead
            TestMeasurementUtil.validateSvidSub(softAssert, null,
                    status.getConstellationType(i),status.getSvid(i));
            // For those function with boolean type return, just simply call the function
            // to make sure those function won't crash, also increase the test coverage.
            Log.i(TAG, "hasAlmanacData: " + status.hasAlmanacData(i));
            Log.i(TAG, "hasEphemerisData: " + status.hasEphemerisData(i));
            Log.i(TAG, "usedInFix: " + status.usedInFix(i));
        }
    }
}
