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
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.android.gpstest.library.data.*
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.theme.SignalInfoViewModel
import kotlinx.coroutines.GlobalScope
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SignalInfoViewModelTest {

    // Required to allow LiveData to execute
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository = LocationRepository(
        SharedLocationManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope),
        SharedGnssStatusManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope),
        SharedNmeaManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope),
        SharedSensorManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope),
        SharedNavMessageManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope),
        SharedGnssMeasurementManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope),
        SharedAntennaManager(InstrumentationRegistry.getTargetContext().applicationContext, GlobalScope)
    )

    /**
     * Test aggregating signal information into satellites
     */
    @Test
    fun testDeviceInfoViewModel() {
        val modelEmpty = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelEmpty.updateStatus(emptyList())

        // Test GPS L1 - should be 1 satellite, no L5 or dual-frequency
        val modelGpsL1 = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelGpsL1.updateStatus(listOf(gpsL1(1, true)))
        assertEquals(1, modelGpsL1.filteredGnssSatellites.value?.size)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInUse)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSatsInView)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSatsUsed)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSatsTotal)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSignalsInView)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSignalsUsed)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSignalsTotal)
        assertEquals(1, modelGpsL1.getSupportedGnss().size)
        assertEquals(0, modelGpsL1.getSupportedSbas().size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(1, modelGpsL1.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1.getSupportedGnssCfs().contains("L1"))
        } else {
            assertEquals(0, modelGpsL1.getSupportedGnssCfs().size)
        }
        assertEquals(0, modelGpsL1.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1.getSupportedGnss().contains(GnssType.NAVSTAR))

        modelGpsL1.reset();

        // Test GPS L1 no signal - should be 1 satellite, no L5 or dual-frequency
        modelGpsL1.updateStatus(listOf(gpsL1NoSignal(1)))
        assertEquals(1, modelGpsL1.filteredGnssSatellites.value?.size)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1.isDualFrequencyPerSatInUse)
        assertEquals(0, modelGpsL1.filteredSatelliteMetadata.value?.numSatsInView)
        assertEquals(0, modelGpsL1.filteredSatelliteMetadata.value?.numSatsUsed)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSatsTotal)
        assertEquals(0, modelGpsL1.filteredSatelliteMetadata.value?.numSignalsInView)
        assertEquals(0, modelGpsL1.filteredSatelliteMetadata.value?.numSignalsUsed)
        assertEquals(1, modelGpsL1.filteredSatelliteMetadata.value?.numSignalsTotal)
        assertEquals(1, modelGpsL1.getSupportedGnss().size)
        assertEquals(0, modelGpsL1.getSupportedSbas().size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(1, modelGpsL1.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1.getSupportedGnssCfs().contains("L1"))
        } else {
            assertEquals(0, modelGpsL1.getSupportedGnssCfs().size)
        }
        assertEquals(0, modelGpsL1.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1.getSupportedGnss().contains(GnssType.NAVSTAR))


        // Test GPS L1 + L5 same sv - should be 1 satellite, dual frequency in view and but not in use
        val modelGpsL1L5 = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelGpsL1L5.updateStatus(listOf(gpsL1(1, false), gpsL5(1, true)))
        assertEquals(1, modelGpsL1L5.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGpsL1L5.getSupportedGnss().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbas().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1L5.getSupportedGnss().contains(GnssType.NAVSTAR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelGpsL1L5.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L1"))
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L5"))
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
            assertEquals(0, modelGpsL1L5.getSupportedGnssCfs().size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and use
        modelGpsL1L5.updateStatus(listOf(gpsL1(1, true), gpsL5(1, true)))
        assertEquals(1, modelGpsL1L5.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGpsL1L5.getSupportedGnss().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbas().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1L5.getSupportedGnss().contains(GnssType.NAVSTAR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelGpsL1L5.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L1"))
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L5"))
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
            assertEquals(0, modelGpsL1L5.getSupportedGnssCfs().size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv - should be 1 satellite, dual-frequency in view and but not used (only 1 sv in use)
        modelGpsL1L5.updateStatus(listOf(gpsL1(1, true), gpsL5(1, false)))
        assertEquals(1, modelGpsL1L5.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGpsL1L5.getSupportedGnss().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbas().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1L5.getSupportedGnss().contains(GnssType.NAVSTAR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelGpsL1L5.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L1"))
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L5"))
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
            assertEquals(0, modelGpsL1L5.getSupportedGnssCfs().size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 but different satellites - should be 2 satellites, non-primary frequency in view and in use, but not dual-frequency in view or use
        modelGpsL1L5.updateStatus(listOf(gpsL1(1, true), gpsL5(2, true)))
        assertEquals(2, modelGpsL1L5.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGpsL1L5.getSupportedGnss().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbas().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1L5.getSupportedGnss().contains(GnssType.NAVSTAR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelGpsL1L5.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L1"))
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L5"))
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(0, modelGpsL1L5.getSupportedGnssCfs().size)
        }

        modelGpsL1L5.reset();

        // Test GPS L1 + L5 same sv, but no L1 signal - should be 1 satellite, dual-frequency not in view or in use
        modelGpsL1L5.updateStatus(listOf(gpsL1NoSignal(1), gpsL5(1, true)))
        assertEquals(1, modelGpsL1L5.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGpsL1L5.getSupportedGnss().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbas().size)
        assertEquals(0, modelGpsL1L5.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1L5.getSupportedGnss().contains(GnssType.NAVSTAR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(1, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGpsL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelGpsL1L5.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L1"))
            assertTrue(modelGpsL1L5.getSupportedGnssCfs().contains("L5"))
        } else {
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGpsL1L5.duplicateCarrierStatuses.size)
            assertEquals(0, modelGpsL1L5.getSupportedGnssCfs().size)
        }

        modelGpsL1L5.reset();

        // Test GPS L5 not in use - should be 1 satellites, non-primary frequency in view, but not dual-frequency in view or use
        val modelGpsL5 = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelGpsL5.updateStatus(listOf(gpsL5(1, false)))
        assertEquals(1, modelGpsL5.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGpsL5.getSupportedGnss().size)
        assertEquals(0, modelGpsL5.getSupportedSbas().size)
        assertEquals(0, modelGpsL5.getSupportedSbasCfs().size)
        assertTrue(modelGpsL5.getSupportedGnss().contains(GnssType.NAVSTAR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGpsL5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(0, modelGpsL5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(0, modelGpsL5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(1, modelGpsL5.getSupportedGnssCfs().size)
            assertTrue(modelGpsL5.getSupportedGnssCfs().contains("L5"))
        } else {
            assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGpsL5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInView)
            assertFalse(modelGpsL5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(0, modelGpsL5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(0, modelGpsL5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(1, modelGpsL5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(0, modelGpsL5.getSupportedGnssCfs().size)
        }

        // Test GPS L1 + GLONASS L1 - should be 2 satellites, no non-primary carrier of dual-freq
        val modelGpsL1GlonassL1 = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelGpsL1GlonassL1.updateStatus(listOf(gpsL1(1, true), glonassL1variant1()))
        assertEquals(2, modelGpsL1GlonassL1.filteredGnssSatellites.value?.size)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInView)
        assertFalse(modelGpsL1GlonassL1.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyPerSatInView)
        assertFalse(modelGpsL1GlonassL1.isDualFrequencyPerSatInUse)
        assertEquals(2, modelGpsL1GlonassL1.filteredSatelliteMetadata.value?.numSatsInView)
        assertEquals(2, modelGpsL1GlonassL1.filteredSatelliteMetadata.value?.numSatsUsed)
        assertEquals(2, modelGpsL1GlonassL1.filteredSatelliteMetadata.value?.numSatsTotal)
        assertEquals(2, modelGpsL1GlonassL1.filteredSatelliteMetadata.value?.numSignalsInView)
        assertEquals(2, modelGpsL1GlonassL1.filteredSatelliteMetadata.value?.numSignalsUsed)
        assertEquals(2, modelGpsL1GlonassL1.filteredSatelliteMetadata.value?.numSignalsTotal)
        assertEquals(2, modelGpsL1GlonassL1.getSupportedGnss().size)
        assertEquals(0, modelGpsL1GlonassL1.getSupportedSbas().size)
        assertEquals(0, modelGpsL1GlonassL1.getSupportedSbasCfs().size)
        assertTrue(modelGpsL1GlonassL1.getSupportedGnss().contains(GnssType.NAVSTAR))
        assertTrue(modelGpsL1GlonassL1.getSupportedGnss().contains(GnssType.GLONASS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(1, modelGpsL1GlonassL1.getSupportedGnssCfs().size)
            assertTrue(modelGpsL1GlonassL1.getSupportedGnssCfs().contains("L1"))
        } else {
            assertEquals(0, modelGpsL1GlonassL1.getSupportedGnssCfs().size)
        }

        // Test Galileo E1 + E5a - should be 2 satellites, dual frequency not in use, non-primary carrier of dual-freq
        val modelGalileoE1E5a = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelGalileoE1E5a.updateStatus(listOf(galileoE1(1, true), galileoE5a(2, true)))
        assertEquals(2, modelGalileoE1E5a.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGalileoE1E5a.getSupportedGnss().size)
        assertEquals(0, modelGalileoE1E5a.getSupportedSbas().size)
        assertEquals(0, modelGalileoE1E5a.getSupportedSbasCfs().size)
        assertTrue(modelGalileoE1E5a.getSupportedGnss().contains(GnssType.GALILEO))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelGalileoE1E5a.getSupportedGnssCfs().size)
            assertTrue(modelGalileoE1E5a.getSupportedGnssCfs().contains("E1"))
            assertTrue(modelGalileoE1E5a.getSupportedGnssCfs().contains("E5a"))
        } else {
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(0, modelGalileoE1E5a.getSupportedGnssCfs().size)
        }

        modelGalileoE1E5a.reset()

        // Test Galileo E1 + E5a - should be 1 satellites, dual frequency in use, non-primary carrier of dual-freq
        modelGalileoE1E5a.updateStatus(listOf(galileoE1(1, true), galileoE5a(1, true)))
        assertEquals(1, modelGalileoE1E5a.filteredGnssSatellites.value?.size)
        assertEquals(1, modelGalileoE1E5a.getSupportedGnss().size)
        assertEquals(0, modelGalileoE1E5a.getSupportedSbas().size)
        assertEquals(0, modelGalileoE1E5a.getSupportedSbasCfs().size)
        assertTrue(modelGalileoE1E5a.getSupportedGnss().contains(GnssType.GALILEO))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertTrue(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertTrue(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            assertEquals(1, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelGalileoE1E5a.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertTrue(modelGalileoE1E5a.getSupportedGnssCfs().contains("E1"))
            assertTrue(modelGalileoE1E5a.getSupportedGnssCfs().contains("E5a"))
        } else {
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInView)
            assertFalse(modelGalileoE1E5a.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInView)
            assertFalse(modelGalileoE1E5a.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelGalileoE1E5a.duplicateCarrierStatuses.size)
            assertEquals(0, modelGalileoE1E5a.getSupportedGnssCfs().size)
        }

        modelGalileoE1E5a.reset()

        // Test WAAS SBAS - L1 - should be 1 satellite, dual frequency not in use, no non-primary carrier of dual-freq
        val modelWaasL1L5 = SignalInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application, repository)
        modelWaasL1L5.updateStatus(listOf(galaxy15_135L1(true)))
        assertEquals(1, modelWaasL1L5.filteredSbasSatellites.value?.size)
        assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInView)
        assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInUse)
        assertFalse(modelWaasL1L5.isDualFrequencyPerSatInView)
        assertFalse(modelWaasL1L5.isDualFrequencyPerSatInUse)
        assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSatsInView)
        assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
        assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
        assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
        assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
        assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
        assertEquals(0, modelWaasL1L5.getSupportedGnss().size)
        assertEquals(0, modelWaasL1L5.getSupportedGnssCfs().size)
        assertEquals(1, modelWaasL1L5.getSupportedSbas().size)
        assertTrue(modelWaasL1L5.getSupportedSbas().contains(SbasType.WAAS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(1, modelWaasL1L5.getSupportedSbasCfs().size)
            assertTrue(modelWaasL1L5.getSupportedSbasCfs().contains("L1"))
        } else {
            assertEquals(0, modelWaasL1L5.getSupportedSbasCfs().size)
        }

        modelWaasL1L5.reset()

        // Test WAAS SBAS - L1 + L5 - should be 1 satellites, dual frequency in use, non-primary carrier of dual-freq
        modelWaasL1L5.updateStatus(listOf(galaxy15_135L1(true), galaxy15_135L5(true)))
        assertEquals(1, modelWaasL1L5.filteredSbasSatellites.value?.size)
        assertEquals(0, modelWaasL1L5.getSupportedGnss().size)
        assertEquals(0, modelWaasL1L5.getSupportedGnssCfs().size)
        assertEquals(1, modelWaasL1L5.getSupportedSbas().size)
        assertTrue(modelWaasL1L5.getSupportedSbas().contains(SbasType.WAAS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(modelWaasL1L5.isNonPrimaryCarrierFreqInView)
            assertTrue(modelWaasL1L5.isNonPrimaryCarrierFreqInUse)
            assertTrue(modelWaasL1L5.isDualFrequencyPerSatInView)
            assertTrue(modelWaasL1L5.isDualFrequencyPerSatInUse)
            assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSatsInView)
            assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSatsUsed)
            assertEquals(1, modelWaasL1L5.filteredSatelliteMetadata.value?.numSatsTotal)
            assertEquals(2, modelWaasL1L5.filteredSatelliteMetadata.value?.numSignalsInView)
            assertEquals(2, modelWaasL1L5.filteredSatelliteMetadata.value?.numSignalsUsed)
            assertEquals(2, modelWaasL1L5.filteredSatelliteMetadata.value?.numSignalsTotal)
            assertEquals(2, modelWaasL1L5.getSupportedSbasCfs().size)
            assertTrue(modelWaasL1L5.getSupportedSbasCfs().contains("L1"))
            assertTrue(modelWaasL1L5.getSupportedSbasCfs().contains("L5"))
        } else {
            assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInView)
            assertFalse(modelWaasL1L5.isNonPrimaryCarrierFreqInUse)
            assertFalse(modelWaasL1L5.isDualFrequencyPerSatInView)
            assertFalse(modelWaasL1L5.isDualFrequencyPerSatInUse)
            // Because carrier frequency isn't considered, these signals should be detected as duplicates
            assertEquals(1, modelWaasL1L5.duplicateCarrierStatuses.size)
            assertEquals(0, modelWaasL1L5.getSupportedSbasCfs().size)
        }
    }
}
