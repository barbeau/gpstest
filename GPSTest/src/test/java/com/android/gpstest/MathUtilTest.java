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

import com.android.gpstest.util.MathUtils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class MathUtilTest {

    /**
     * Test converting Hz to MHz
     */
    @Test
    public void testToMhz() {
        float mhz = MathUtils.toMhz(1000000.0f);
        assertEquals(1.0f, mhz);
    }

    /**
     * Test mapping a value on one range to the equivalent value on another range
     */
    @Test
    public void testMapToRange() {
        float a = 50;
        final float minA = 0.0f;
        final float maxA = 100.0f;
        final float minB = 0.0f;
        final float maxB = 200.0f;

        float mappedB = MathUtils.mapToRange(a, minA, maxA, minB, maxB);

        // mappedB should be 100, because a = 50 is halfway between 0 and 100, and b = 100 is halfway between 0 and 200
        assertEquals(100.0f, mappedB);

        // Check below min range
        a = -10;
        mappedB = MathUtils.mapToRange(a, minA, maxA, minB, maxB);
        assertEquals(0.0f, mappedB);

        // Check above max range
        a = 105;
        mappedB = MathUtils.mapToRange(a, minA, maxA, minB, maxB);
        assertEquals(200.0f, mappedB);
    }

    @Test
    public void testIsValidFloat() {
        assertTrue(MathUtils.isValidFloat(15.0f));
        assertFalse(MathUtils.isValidFloat(0.0f));
        assertFalse(MathUtils.isValidFloat(Float.NaN));
    }
}
