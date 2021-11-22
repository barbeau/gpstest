/*
 * Copyright (C) 2021 Sean J. Barbeau
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
package com.android.gpstest.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.android.gpstest.model.*
import com.android.gpstest.ui.SignalInfoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun DashboardScreen(viewModel: SignalInfoViewModel) {
    val allSatellites: SatelliteGroup by viewModel.allSatellitesGroup.observeAsState(
        SatelliteGroup(emptyMap(), satelliteMetadata = SatelliteMetadata())
    )
    GnssList(satelliteMetadata = allSatellites.satelliteMetadata)
}

@Composable
fun GnssList(satelliteMetadata: SatelliteMetadata) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            satelliteMetadata.supportedGnss.forEach {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    elevation = 2.dp
                ) {
                    Text(it.name)
                }
            }
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(5.dp),
//                elevation = 2.dp
//            ) {
//                Text("HI")
//            }
        }
    }
}

@Preview
@Composable
fun GnssListPreview(
    @PreviewParameter(SatelliteMetadataPreviewParameterProvider::class) satelliteMetadata: SatelliteMetadata
) {
    GnssList(satelliteMetadata)
}

class SatelliteMetadataPreviewParameterProvider : PreviewParameterProvider<SatelliteMetadata> {
    override val values = sequenceOf(previewMetadata())
}

fun previewMetadata(): SatelliteMetadata {
    val numSignalsInView = 10
    val numSignalsUsed = 10
    val numSignalsTotal = 10
    val numSatsInView = 7
    val numSatsUsed = 7
    val numSatsTotal = 7
    val supportedGnss: Set<GnssType> = setOf(GnssType.NAVSTAR, GnssType.GALILEO)
    val supportedGnssCfs: Set<String> = setOf("L1", "L5", "E1")
    val supportedSbas: Set<SbasType> = emptySet()
    val supportedSbasCfs: Set<String> = emptySet()
    val unknownCarrierStatuses: Map<String, SatelliteStatus> = emptyMap()
    val duplicateCarrierStatuses: Map<String, SatelliteStatus> = emptyMap()
    val isDualFrequencyPerSatInView = true
    val isDualFrequencyPerSatInUse = true
    val isNonPrimaryCarrierFreqInView = true
    val isNonPrimaryCarrierFreqInUse = true

    return SatelliteMetadata(
        numSignalsInView,
        numSignalsUsed,
        numSignalsTotal,
        numSatsInView,
        numSatsUsed,
        numSatsTotal,
        supportedGnss,
        supportedGnssCfs,
        supportedSbas,
        supportedSbasCfs,
        unknownCarrierStatuses,
        duplicateCarrierStatuses,
        isDualFrequencyPerSatInView,
        isDualFrequencyPerSatInUse,
        isNonPrimaryCarrierFreqInView,
        isNonPrimaryCarrierFreqInUse
    )
}