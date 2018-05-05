package com.android.gpstest;

import com.android.gpstest.util.UIUtils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class UIUtilsTest {

    /**
     * Left margin range for the C/N0 indicator ImageViews in gps_sky_signal is from -5dp (10 dB-Hz) to 155dp (45 dB-Hz).
     */
    @Test
    public void testCn0ToLeftMargin() {
        float marginDp;

        // CN0 value of 27.5 dB-Hz is 50% between 10 and 45, so output should be halfway between -5dp and 155dp, which is 75dp
        marginDp = UIUtils.cn0ToLeftMarginDp(27.5f);
        assertEquals(75.0f, marginDp);

        // CN0 value of 45.0 dB-Hz is 100% of 45, so output should be 100% of 155dp, which is 155dp
        marginDp = UIUtils.cn0ToLeftMarginDp(45.0f);
        assertEquals(155.0f, marginDp);

        // CN0 value of 10.0 dB-Hz is 0% (min value of CN0), so output should be 0% of dp range, which is -5
        marginDp = UIUtils.cn0ToLeftMarginDp(10.0f);
        assertEquals(-5.0f, marginDp);
    }
}
