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
import android.os.Build
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
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSatsInView)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSatsUsed)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSatsTotal)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSignalsInView)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSignalsUsed)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSignalsTotal)

        modelGpsL1.reset();

        // Test GPS L1 no signal - should be 1 satellite, no L5 or dual-frequency
        modelGpsL1.setStatuses(listOf(gpsL1NoSignal(1)), null)
        assertEquals(1, modelGpsL1.gnssSatellites.value?.size)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInUse)
        assertEquals(0, modelGpsL1.satelliteMetadata.value?.numSatsInView)
        assertEquals(0, modelGpsL1.satelliteMetadata.value?.numSatsUsed)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSatsTotal)
        assertEquals(0, modelGpsL1.satelliteMetadata.value?.numSignalsInView)
        assertEquals(0, modelGpsL1.satelliteMetadata.value?.numSignalsUsed)
        assertEquals(1, modelGpsL1.satelliteMetadata.value?.numSignalsTotal)

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual frequency in view and but not in use
        val modelGpsL1L5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, false), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and use
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and but not used (only 1 sv in use)
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(1, false)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 but different satellites - should be 2 satellites, non-primary frequency in view and in use, but not dual-frequency in view or use
        modelGpsL1L5.setStatuses(listOf(gpsL1(1, true), gpsL5(2, true)), null)
        assertEquals(2, modelGpsL1L5.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsTotal)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv, but no L1 signal - should be 1 satellite, dual-frequency not in view or in use
        modelGpsL1L5.setStatuses(listOf(gpsL1NoSignal(1), gpsL5(1, true)), null)
        assertEquals(1, modelGpsL1L5.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(1, modelGpsL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
        }

        modelGpsL1L5.reset();

        // Test GPS L5 not in use - should be 1 satellites, non-primary frequency in view, but not dual-frequency in view or use
        val modelGpsL5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL5.setStatuses(listOf(gpsL5(1, false)), null)
        assertEquals(1, modelGpsL5.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSatsInView)
            assertEquals(0, modelGpsL5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(0, modelGpsL5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSatsInView)
            assertEquals(0, modelGpsL5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(0, modelGpsL5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(1, modelGpsL5.satelliteMetadata.value?.numSignalsTotal)
        }

        // Test GPS L1 + GLONASS L1 - should be 2 satellites, no non-primary carrier of dual-freq
        val modelGpsL1GlonassL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1GlonassL1.setStatuses(listOf(gpsL1(1, true), glonassL1variant1()), null)
        assertEquals(2, modelGpsL1GlonassL1.gnssSatellites.value?.size)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyPerSatInUse)
        assertEquals(2, modelGpsL1GlonassL1.satelliteMetadata.value?.numSatsInView)
        assertEquals(2, modelGpsL1GlonassL1.satelliteMetadata.value?.numSatsUsed)
        assertEquals(2, modelGpsL1GlonassL1.satelliteMetadata.value?.numSatsTotal)
        assertEquals(2, modelGpsL1GlonassL1.satelliteMetadata.value?.numSignalsInView)
        assertEquals(2, modelGpsL1GlonassL1.satelliteMetadata.value?.numSignalsUsed)
        assertEquals(2, modelGpsL1GlonassL1.satelliteMetadata.value?.numSignalsTotal)

        // Test Galileo E1 + E5a - should be 2 satellites, dual frequency not in use, non-primary carrier of dual-freq
        val modelGalileoE1E5a = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGalileoE1E5a.setStatuses(listOf(galileoE1(1, true), galileoE5a(2, true)), null)
        assertEquals(2, modelGalileoE1E5a.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsTotal)
        }

        modelGalileoE1E5a.reset()

        // Test Galileo E1 + E5a - should be 1 satellites, dual frequency in use, non-primary carrier of dual-freq
        modelGalileoE1E5a.setStatuses(listOf(galileoE1(1, true), galileoE5a(1, true)), null)
        assertEquals(1, modelGalileoE1E5a.gnssSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertTrue(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGalileoE1E5a.satelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGalileoE1E5a.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGalileoE1E5a.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGalileoE1E5a.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGalileoE1E5a.duplicateCarrierStatuses.size)
        }

        modelGalileoE1E5a.reset()

        // Test WAAS SBAS - L1 - should be 1 satellite, dual frequency not in use, no non-primary carrier of dual-freq
        val modelWaasL1L5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelWaasL1L5.setStatuses(null, listOf(galaxy15_135L1(true)))
        assertEquals(1, modelWaasL1L5.sbasSatellites.value?.size)
        assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInView)
        assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelWaasL1L5.isDualFrequencyPerSatInView)
        assertFalse(modelWaasL1L5.isDualFrequencyPerSatInUse)
        assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSatsInView)
        assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSatsUsed)
        assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSatsTotal)
        assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSignalsInView)
        assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSignalsUsed)
        assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSignalsTotal)

        modelWaasL1L5.reset()

        // Test WAAS SBAS - L1 + L5 - should be 1 satellites, dual frequency in use, non-primary carrier of dual-freq
        modelWaasL1L5.setStatuses(null, listOf(galaxy15_135L1(true), galaxy15_135L5(true)))
        assertEquals(1, modelWaasL1L5.sbasSatellites.value?.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelWaasL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelWaasL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelWaasL1L5.isDualFrequencyPerSatInView)
            assertTrue(modelWaasL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelWaasL1L5.satelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelWaasL1L5.satelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelWaasL1L5.satelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelWaasL1L5.satelliteMetadata.value?.numSignalsTotal)
        } else {
            assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelWaasL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelWaasL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelWaasL1L5.duplicateCarrierStatuses.size)
        }
    }
}
