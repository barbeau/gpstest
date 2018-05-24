package com.android.gpstest;

import com.android.gpstest.util.UIUtils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class UIUtilsTest {

    /**
     * Left margin range for the C/N0 indicator ImageViews in gps_sky_signal_meter is from -6dp (10 dB-Hz) to 140dp (45 dB-Hz).
     */
    @Test
    public void testCn0ToLeftMargin() {
        float marginDp;

        // FIX ME with new offset values

        // CN0 value of 27.5 dB-Hz is 50% between 10 and 45, so output should be halfway between -6dp and 140dp, which is 70dp
        marginDp = UIUtils.cn0ToIndicatorLeftMarginDp(27.5f);
        assertEquals(67.0f, marginDp);

        // CN0 value of 45.0 dB-Hz is 100% of 45, so output should be 100% of 140dp, which is 140dp
        marginDp = UIUtils.cn0ToIndicatorLeftMarginDp(45.0f);
        assertEquals(140.0f, marginDp);

        // CN0 value of 10.0 dB-Hz is 0% (min value of CN0), so output should be 0% of dp range, which is -6
        marginDp = UIUtils.cn0ToIndicatorLeftMarginDp(10.0f);
        assertEquals(-6.0f, marginDp);
    }
}
