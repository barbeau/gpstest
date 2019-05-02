// Copyright 2008 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utilities for working with Dates and times.
 *
 * @author Kevin Serafini
 * @author Brent Bryan
 */
public class TimeUtils {
    private TimeUtils() {
    }

    /**
     * Calculate the number of Julian Centuries from the epoch 2000.0
     * (equivalent to Julian Day 2451545.0).
     */
    public static double julianCenturies(Date date) {
        double jd = calculateJulianDay(date);
        double delta = jd - 2451545.0;
        return delta / 36525.0;
    }

    /**
     * Calculate the Julian Day for a given date using the following formula:
     * JD = 367 * Y - INT(7 * (Y + INT((M + 9)/12))/4) + INT(275 * M / 9)
     * + D + 1721013.5 + UT/24
     * <p>
     * Note that this is only valid for the year range 1900 - 2099.
     */
    public static double calculateJulianDay(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(date);

        double hour = cal.get(Calendar.HOUR_OF_DAY)
                + cal.get(Calendar.MINUTE) / 60.0f
                + cal.get(Calendar.SECOND) / 3600.0f;

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        double jd = 367.0 * year - Math.floor(7.0 * (year
                + Math.floor((month + 9.0) / 12.0)) / 4.0)
                + Math.floor(275.0 * month / 9.0) + day
                + 1721013.5 + hour / 24.0;
        return jd;
    }

    /**
     * Convert the given Julian Day to Gregorian Date (in UT time zone).
     * Based on the formula given in the Explanitory Supplement to the
     * Astronomical Almanac, pg 604.
     */
    public static Date calculateGregorianDate(double jd) {
        int l = (int) jd + 68569;
        int n = (4 * l) / 146097;
        l = l - (146097 * n + 3) / 4;
        int i = (4000 * (l + 1)) / 1461001;
        l = l - (1461 * i) / 4 + 31;
        int j = (80 * l) / 2447;
        int d = l - (2447 * j) / 80;
        l = j / 11;
        int m = j + 2 - 12 * l;
        int y = 100 * (n - 49) + i + l;

        double fraction = jd - Math.floor(jd);
        double dHours = fraction * 24.0;
        int hours = (int) dHours;
        double dMinutes = (dHours - hours) * 60.0;
        int minutes = (int) dMinutes;
        int seconds = (int) ((dMinutes - minutes) * 60.0);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UT"));
        cal.set(y, m - 1, d, hours + 12, minutes, seconds);
        return cal.getTime();
    }

    /**
     * Calculate local mean sidereal time in degrees. Note that longitude is
     * negative for western longitude values.
     */
    public static float meanSiderealTime(Date date, float longitude) {
        // First, calculate number of Julian days since J2000.0.
        double jd = calculateJulianDay(date);
        double delta = jd - 2451545.0f;

        // Calculate the global and local sidereal times
        double gst = 280.461f + 360.98564737f * delta;
        double lst = normalizeAngle(gst + longitude);

        return (float) lst;
    }

    /**
     * Normalize the angle to the range 0 <= value < 360.
     */
    public static double normalizeAngle(double angle) {
        double remainder = angle % 360;
        if (remainder < 0) remainder += 360;
        return remainder;
    }

    /**
     * Normalize the time to the range 0 <= value < 24.
     */
    public static double normalizeHours(double time) {
        double remainder = time % 24;
        if (remainder < 0) remainder += 24;
        return remainder;
    }

    /**
     * Take a universal time between 0 and 24 and return a triple
     * [hours, minutes, seconds].
     *
     * @param ut Universal time - presumed to be between 0 and 24.
     * @return [hours, minutes, seconds]
     */
    public static int[] clockTimeFromHrs(double ut) {
        int[] hms = new int[3];
        hms[0] = (int) Math.floor(ut);
        double remainderMins = 60 * (ut - hms[0]);
        hms[1] = (int) Math.floor(remainderMins);
        hms[2] = (int) Math.floor(remainderMins - hms[1]);
        return hms;
    }
}
