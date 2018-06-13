package com.android.gpstest;

import com.android.gpstest.util.UIUtils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class UIUtilsTest {

    /**
     * Left margin range for the avg C/N0 indicator ImageViews in gps_sky_signal_meter is from -6dp (10 dB-Hz) to 140dp (45 dB-Hz).
     */
    @Test
    public void testCn0ToIndicatorLeftMarginDp() {
        float marginDp;

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

    /**
     * Left margin range for the avg SNR indicator ImageViews in gps_sky_signal_meter is from -6dp (0 dB) to 140dp (30 dB).
     */
    @Test
    public void testSnrToIndicatorLeftMarginDp() {
        float marginDp;

        // SNR value of 15 dB is 50% between 0 dB and 30 dB, so output should be halfway between -6dp and 140dp, which is 70dp
        marginDp = UIUtils.snrToIndicatorLeftMarginDp(15.0f);
        assertEquals(67.0f, marginDp);

        // SNR value of 30.0 dB is 100% of 45, so output should be 100% of 140dp, which is 140dp
        marginDp = UIUtils.snrToIndicatorLeftMarginDp(30.0f);
        assertEquals(140.0f, marginDp);

        // SNR value of 0.0 dB is 0% (min value of SNR), so output should be 0% of dp range, which is -6
        marginDp = UIUtils.snrToIndicatorLeftMarginDp(0.0f);
        assertEquals(-6.0f, marginDp);
    }

    /**
     * Left margin range for the avg C/N0 indicator TextViews in gps_sky_signal_meter is from 3dp (10 dB-Hz) to 149dp (45 dB-Hz).
     */
    @Test
    public void testCn0ToTextViewLeftMargin() {
        float marginDp;

        // CN0 value of 27.5 dB-Hz is 50% between 10 and 45, so output should be halfway between 3dp and 149dp, which is 76dp
        marginDp = UIUtils.cn0ToTextViewLeftMarginDp(27.5f);
        assertEquals(76.0f, marginDp);

        // CN0 value of 45.0 dB-Hz is 100% of 149, so output should be 100% of 149dp, which is 149dp
        marginDp = UIUtils.cn0ToTextViewLeftMarginDp(45.0f);
        assertEquals(149.0f, marginDp);

        // CN0 value of 10.0 dB-Hz is 0% (min value of CN0), so output should be 0% of dp range, which is 3dp
        marginDp = UIUtils.cn0ToTextViewLeftMarginDp(10.0f);
        assertEquals(3.0f, marginDp);
    }

    /**
     * Left margin range for the avg SNR indicator TextViews in gps_sky_signal_meter is from 3dp (0 dB) to 149dp (30 dB).
     */
    @Test
    public void testSnrToTextViewLeftMargin() {
        float marginDp;

        // SNR value of 15 dB is 50% between 0 and 30, so output should be halfway between 3dp and 149dp, which is 76dp
        marginDp = UIUtils.snrToTextViewLeftMarginDp(15.0f);
        assertEquals(76.0f, marginDp);

        // SNR value of 30.0 dB is 100% of 149, so output should be 100% of 149dp, which is 149dp
        marginDp = UIUtils.snrToTextViewLeftMarginDp(30.0f);
        assertEquals(149.0f, marginDp);

        // SNR value of 0.0 dB is 0% (min value of SNR), so output should be 0% of dp range, which is 3dp
        marginDp = UIUtils.snrToTextViewLeftMarginDp(0.0f);
        assertEquals(3.0f, marginDp);
    }
}
