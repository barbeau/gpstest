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

        // Test GPS L1 - should be 1 satellite, no L5 or dual-frequency
        val modelGpsL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1.setStatuses(listOf(gpsL1(1, true)), null)
        assertEquals(1, modelGpsL1.gnssSatellites.value?.size)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInUse)

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual frequency in view and but not in use
        val modelGpsL1L5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, false), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and use
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
        assertTrue(modelGpsL1L5.isDualFrequencyPerSatInUse)

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and but not used (only 1 sv in use)
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(1, false)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 but different satellites - should be 2 satellites, non-primary frequency in view and in use, but not dual-frequency in view or use
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(2, true)), null)
        assertEquals(2, modelGpsL1L5.gnssSatellites.value?.size)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)

        modelGpsL1L5.reset();

        // Test GPS L5 not in use - should be 1 satellites, non-primary frequency in view, but not dual-frequency in view or use
        val modelGpsL5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL5.setStatuses(listOf(gpsL5(1, false)), null)
        assertEquals(1, modelGpsL5.gnssSatellites.value?.size)
        assertTrue(modelGpsL5.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL5.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL5.isDualFrequencyPerSatInUse)

        // Test GPS L1 + GLONASS L1 - should be 2 satellites, no non-primary carrier of dual-freq
        val modelGpsL1GlonassL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1GlonassL1.setStatuses(listOf(gpsL1(1, true), glonassL1variant1()), null)
        assertEquals(2, modelGpsL1GlonassL1.gnssSatellites.value?.size)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyPerSatInUse)

        // Test Galileo E1 + E5 - should be 2 satellites, dual frequency not in use, non-primary carrier of dual-freq
        val modelGalileoE1E5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGalileoE1E5.setStatuses(listOf(galileoE1(1, true), galileoE5a(2, true)), null)
        assertEquals(2, modelGalileoE1E5.gnssSatellites.value?.size)
        assertTrue(modelGalileoE1E5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGalileoE1E5.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGalileoE1E5.isDualFrequencyPerSatInView)
        assertFalse(modelGalileoE1E5.isDualFrequencyPerSatInUse)

        modelGalileoE1E5.reset()

        // Test Galileo E1 + E5 - should be 1 satellites, dual frequency in use, non-primary carrier of dual-freq
        modelGalileoE1E5.setStatuses(listOf(galileoE1(1, true), galileoE5a(1, true)), null)
        assertEquals(1, modelGalileoE1E5.gnssSatellites.value?.size)
        assertTrue(modelGalileoE1E5.isNonPrimaryCarrierFreqInView)
        assertTrue(modelGalileoE1E5.isNonPrimaryCarrierFreqInUse)
        assertTrue(modelGalileoE1E5.isDualFrequencyPerSatInView)
        assertTrue(modelGalileoE1E5.isDualFrequencyPerSatInUse)

        modelGalileoE1E5.reset()

        // TODO - test SBAS
    }
}
