package com.android.gpstest.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class UIUtilsTest {

    /**
     * Example left margin range for the avg C/N0 indicator ImageViews in gps_sky_signal_meter is from -6px (10 dB-Hz) to 140px (45 dB-Hz).
     */
    @Test
    public void testCn0ToIndicatorLeftMarginPx() {
        int marginPx;
        final int MIN_PX = -6;
        final int MAX_PX = 140;

        // CN0 value of 27.5 dB-Hz is 50% between 10 and 45, so output should be halfway between -6px and 140px, which is 70px
        marginPx = UIUtils.cn0ToIndicatorLeftMarginPx(27.5f, MIN_PX, MAX_PX);
        assertEquals(67, marginPx);

        // CN0 value of 45.0 dB-Hz is 100% of 45, so output should be 100% of 140px, which is 140px
        marginPx = UIUtils.cn0ToIndicatorLeftMarginPx(45.0f, MIN_PX, MAX_PX);
        assertEquals(140, marginPx);

        // CN0 value of 10.0 dB-Hz is 0% (min value of CN0), so output should be 0% of px range, which is -6
        marginPx = UIUtils.cn0ToIndicatorLeftMarginPx(10.0f, MIN_PX, MAX_PX);
        assertEquals(-6, marginPx);
    }

    /**
     * Example left margin range for the avg SNR indicator ImageViews in gps_sky_signal_meter is from -6px (0 dB) to 140px (30 dB).
     */
    @Test
    public void testSnrToIndicatorLeftMarginPx() {
        int marginPx;
        final int MIN_PX = -6;
        final int MAX_PX = 140;

        // SNR value of 15 dB is 50% between 0 dB and 30 dB, so output should be halfway between -6px and 140px, which is 70px
        marginPx = UIUtils.snrToIndicatorLeftMarginPx(15.0f, MIN_PX, MAX_PX);
        assertEquals(67, marginPx);

        // SNR value of 30.0 dB is 100% of 45, so output should be 100% of 140px, which is 140px
        marginPx = UIUtils.snrToIndicatorLeftMarginPx(30.0f, MIN_PX, MAX_PX);
        assertEquals(140, marginPx);

        // SNR value of 0.0 dB is 0% (min value of SNR), so output should be 0% of px range, which is -6
        marginPx = UIUtils.snrToIndicatorLeftMarginPx(0.0f, MIN_PX, MAX_PX);
        assertEquals(-6, marginPx);
    }

    /**
     * Example feft margin range for the avg C/N0 indicator TextViews in gps_sky_signal_meter is from 3px (10 dB-Hz) to 149px (45 dB-Hz).
     */
    @Test
    public void testCn0ToTextViewLeftMargin() {
        int marginPx;
        final int MIN_PX = 3;
        final int MAX_PX = 149;

        // CN0 value of 27.5 dB-Hz is 50% between 10 and 45, so output should be halfway between 3px and 149px, which is 76px
        marginPx = UIUtils.cn0ToTextViewLeftMarginPx(27.5f, MIN_PX, MAX_PX);
        assertEquals(76, marginPx);

        // CN0 value of 45.0 dB-Hz is 100% of 149, so output should be 100% of 149px, which is 149px
        marginPx = UIUtils.cn0ToTextViewLeftMarginPx(45.0f, MIN_PX, MAX_PX);
        assertEquals(149, marginPx);

        // CN0 value of 10.0 dB-Hz is 0% (min value of CN0), so output should be 0% of px range, which is 3px
        marginPx = UIUtils.cn0ToTextViewLeftMarginPx(10.0f, MIN_PX, MAX_PX);
        assertEquals(3, marginPx);
    }

    /**
     * Example left margin range for the avg SNR indicator TextViews in gps_sky_signal_meter is from 3px (0 dB) to 149px (30 dB).
     */
    @Test
    public void testSnrToTextViewLeftMargin() {
        int marginPx;
        final int MIN_PX = 3;
        final int MAX_PX = 149;

        // SNR value of 15 dB is 50% between 0 and 30, so output should be halfway between 3px and 149px, which is 76px
        marginPx = UIUtils.snrToTextViewLeftMarginPx(15.0f, MIN_PX, MAX_PX);
        assertEquals(76, marginPx);

        // SNR value of 30.0 dB is 100% of 149, so output should be 100% of 149px, which is 149px
        marginPx = UIUtils.snrToTextViewLeftMarginPx(30.0f, MIN_PX, MAX_PX);
        assertEquals(149, marginPx);

        // SNR value of 0.0 dB is 0% (min value of SNR), so output should be 0% of px range, which is 3px
        marginPx = UIUtils.snrToTextViewLeftMarginPx(0.0f, MIN_PX, MAX_PX);
        assertEquals(3, marginPx);
    }

    /**
     * Tests converting from meters to feet
     */
    @Test
    public void testToFeet() {
        double meters = 1.0d;

        assertEquals(3.2808398950131235d, UIUtils.toFeet(meters));

        meters = 30.0d;

        assertEquals(98.4251968503937d, UIUtils.toFeet(meters));
    }

    /**
     * Tests converting from meters per second to kilometers per hour
     */
    @Test
    public void testToKilometersPerHour() {
        float metersPerSecond = 1.0f;

        assertEquals(3.6f, UIUtils.toKilometersPerHour(metersPerSecond));

        metersPerSecond = 30.0f;

        assertEquals(108.0f, UIUtils.toKilometersPerHour(metersPerSecond));
    }

    /**
     * Tests converting from meters per second to miles per hour
     */
    @Test
    public void testToMilesPerHour() {
        float metersPerSecond = 1.0f;

        assertEquals(2.2369363f, UIUtils.toMilesPerHour(metersPerSecond));

        metersPerSecond = 60.0f;

        assertEquals(134.21617f, UIUtils.toMilesPerHour(metersPerSecond));
    }
}
