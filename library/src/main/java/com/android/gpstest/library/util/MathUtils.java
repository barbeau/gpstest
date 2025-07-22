/*
 * Copyright (C) 2013-2018 The Android Open Source Project, Sean J. Barbeau (sjbarbeau@gmail.com),
 * Google (fuzzyEquals() is from Guava)
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
package com.android.gpstest.library.util;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * A utility class containing arithmetic and geometry helper methods.
 *
 */
public class MathUtils {

    /**
     * Calculates {@code a mod b} in a way that respects negative values (for example,
     * {@code mod(-1, 5) == 4}, rather than {@code -1}).
     *
     * (from Glass Compass sample)
     *
     * @param a the dividend
     * @param b the divisor
     * @return {@code a mod b}
     */
    public static double mod(double a, double b) {
        return (a % b + b) % b;
    }

    /**
     * Converts the provided number in Hz to MHz
     * @param hertz value to be converted
     * @return value converted to MHz
     */
    public static double toMhz(double hertz) {
        return hertz / 1000000.00;
    }

    /**
     * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
     *
     * <p>Technically speaking, this is equivalent to {@code Math.abs(a - b) <= tolerance ||
     * Double.valueOf(a).equals(Double.valueOf(b))}.
     *
     * <p>Notable special cases include:
     *
     * <ul>
     *   <li>All NaNs are fuzzily equal.
     *   <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
     *   <li>Positive and negative zero are always fuzzily equal.
     *   <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then {@code a}
     *       and {@code b} are fuzzily equal if and only if {@code a == b}.
     *   <li>With {@link Double#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
     *   <li>With finite tolerance, {@code Double.POSITIVE_INFINITY} and {@code
     *       Double.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
     * </ul>
     *
     * <p>This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
     * equivalence relation and <em>not</em> suitable for use in {@link Object#equals}
     * implementations.
     *
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
     * @since 13.0
     */
    public static boolean fuzzyEquals(double a, double b, double tolerance) {
        checkNonNegative("tolerance", tolerance);
        return Math.copySign(a - b, 1.0) <= tolerance
                // copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
                || (a == b) // needed to ensure that infinities equal themselves
                || (Double.isNaN(a) && Double.isNaN(b));
    }

    static double checkNonNegative(String role, double x) {
        if (!(x >= 0)) { // not x < 0, to work with NaN.
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Converts the provided value "a" value to a value "b" given a range of possible values for "a"
     * ("minA" and "maxA") and a range of possible values for b ("minB" and "maxB").
     *
     * If a is less than minA, then minB is returned.  If a is more than maxA, then maxB is returned.
     *
     * @param a the value to be mapped to the range between minB and maxB
     * @param minA the minimum value of the range of a
     * @param maxA the maximum value of the range of a
     * @param minB the minimum value of the range of b
     * @param maxB the maximum value of the range of b
     * @return the value of b as it relates to the values minB and maxB, based on the provided value a
     *         and it's range of minA and maxA
     */
    public static float mapToRange(float a, float minA, float maxA,
                                   float minB, float maxB) {
        // If the value is outside the range return the min or max accordingly
        if (a < minA) {
            return minB;
        }
        if (a > maxA) {
            return maxB;
        }

        // Shift ranges to calculate percentages (because default min value may not be 0)
        final float maxBshifted = maxB - minB;
        final float maxAshifted = maxA - minA;

        // Calculate percentage of given a value to the a range
        final float aPercent = (100 * (a - minA)) / maxAshifted;

        // Apply percentage to adjusted b range
        final float bShifted = (maxBshifted * aPercent) / 100;

        // Shift b value back using original b range offset and return
        return bShifted + minB;
    }

    /**
     * Returns true if the provided value is a valid floating point value for signal strength, or false if it's not
     * @return true if the provided value is a valid floating point value for signal strength, or false if it's not
     */
    public static boolean isValidFloat(float value) {
        return value != 0.0f && !Float.isNaN(value);
    }

    /**
     * Clamps a value between the given positive min and max.  If abs(value) is less than
     * min, then min is returned.  If abs(value) is greater than max, then max is returned.
     * If abs(value) is between min and max, then abs(value) is returned.
     *
     * @param min   minimum allowed value
     * @param value value to be evaluated
     * @param max   maximum allowed value
     * @return clamped value between the min and max
     */
    public static double clamp(double min, double value, double max) {
        value = Math.abs(value);
        if (value >= min && value <= max) {
            return value;
        } else {
            return (value < min ? value : max);
        }
    }

    /**
     * Converts the provided base 64 string to UTF-8
     * @param base64 the base 64 string to convert to UTF-8
     * @return the input string converted to UTF-8
     */
    public static String fromBase64(String base64) throws UnsupportedEncodingException {
        byte[] data = Base64.decode(base64, Base64.DEFAULT);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Converts the provided string input to a double, and handles locale issues such as commas
     * instead of periods.  Does NOT validate input.
     * @param input string version of double to be converted
     * @return double value of the input string, or null if there is a parsing error
     */
    public static Double toDouble(String input) {
        try {
            return NumberFormat.getInstance().parse(input).doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
