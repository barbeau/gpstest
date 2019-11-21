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
import org.junit.Assert.assertEquals
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

        // Test GPS L1 - should be 1 satellite
        val modelGpsL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1.setStatuses(listOf(gpsL1()), null)
        assertEquals(1, modelGpsL1.gnssSatellites.value?.size)

        // Test GPS L1 + L5 - should be 1 satellite
        val modelGpsL1L5 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1L5.setStatuses(listOf(gpsL1(), gpsL5()), null)
        assertEquals(1, modelGpsL1.gnssSatellites.value?.size)

        // Test GPS L1 + GLONASS L1 - should be 2 satellites
        val modelGpsL1GlonassL1 = DeviceInfoViewModel(InstrumentationRegistry.getTargetContext().applicationContext as Application)
        modelGpsL1L5.setStatuses(listOf(gpsL1(), glonassL1variant1()), null)
        assertEquals(2, modelGpsL1.gnssSatellites.value?.size)
    }
}
