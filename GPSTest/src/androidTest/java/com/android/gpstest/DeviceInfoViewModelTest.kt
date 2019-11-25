/*
 * Copyright (C) 2019 Sean J. Barbeau (sjbarbeau@gmail.com)
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

package com.android.gpstest

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceInfoViewModelTest {

    // Required to allow LiveData to execute
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * Test aggregating signal information into satellites
     */
    @Test
    fun testDeviceInfoViewModel() {
        val modelNull = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelNull.setStatuses(null, null)

        // Test GPS L1 - should be 1 satellite, no dual-frequency
        val modelGpsL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1.setStatuses(listOf(gpsL1(1, true)), null)
        assertEquals(1, modelGpsL1.gnssSatellites.value?.size)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1.isDualFrequencyInView)
        assertFalse(modelGpsL1.isDualFrequencyInUse)

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual frequency in view and but not in use
        val modelGpsL1L5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, false), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertTrue(modelGpsL1L5.isDualFrequencyInView)
        assertFalse(modelGpsL1L5.isDualFrequencyInUse)

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and use
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertTrue(modelGpsL1L5.isDualFrequencyInView)
        assertTrue(modelGpsL1L5.isDualFrequencyInUse)

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 but different satellites - should be 2 satellites, non-primary frequency in view and in use, but not dual-frequency in view or use
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(2, true)), null)
        assertEquals(2, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1L5.isDualFrequencyInView)
        assertFalse(modelGpsL1L5.isDualFrequencyInUse)

        modelGpsL1L5.reset();

        // Test GPS L1 + GLONASS L1 - should be 2 satellites, no non-primary carrier of dual-freq
        val modelGpsL1GlonassL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1GlonassL1.setStatuses(listOf(gpsL1(1, true), glonassL1variant1()), null)
        assertEquals(2, modelGpsL1GlonassL1.gnssSatellites.value?.size)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyInView)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyInUse)

        // TODO - test dual-frequency with Galileo

        // TODO - test SBAS
    }
}
