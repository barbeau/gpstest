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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.model.GnssType
import com.android.gpstest.model.SatelliteGroup
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.SignalInfoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun DashboardScreen(viewModel: SignalInfoViewModel) {
    val allSatellites: SatelliteGroup by viewModel.allSatellitesGroup.observeAsState(
        SatelliteGroup(emptyMap(), satelliteMetadata = SatelliteMetadata())
    )
    val finishedScanningCfs: Boolean by viewModel.finishedScanningCfs.observeAsState(false)

    GnssList(satelliteMetadata = allSatellites.satelliteMetadata, finishedScanningCfs)
}

@Composable
fun GnssList(satelliteMetadata: SatelliteMetadata, finishedScanningCfs: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxSize()
        ) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(id = R.string.dashboard_supported_gnss),
                style = MaterialTheme.typography.h6
            )
            if (satelliteMetadata.gnssToCf.entries.isEmpty()) {
                ProgressCard()
            } else {
                satelliteMetadata.gnssToCf.entries.forEach {
                    GnssCard(
                        gnssType = it.key,
                        cfs = it.value,
                        finishedScanningCfs = finishedScanningCfs
                    )
                }
            }
        }
    }
}

@Composable
fun GnssCard(gnssType: GnssType, cfs: Set<String>, finishedScanningCfs: Boolean) {
    when (gnssType) {
        GnssType.NAVSTAR -> {
            GnssCard(
                R.drawable.ic_us_flag_round,
                R.string.dashboard_gps,
                R.string.dashboard_usa,
                R.string.usa_flag,
                cfs,
                finishedScanningCfs
            )
        }
        GnssType.GALILEO -> {
            GnssCard(
                R.drawable.ic_eu_flag_round,
                R.string.dashboard_galileo,
                R.string.dashboard_eu,
                R.string.eu_flag,
                cfs,
                finishedScanningCfs
            )
        }
        GnssType.GLONASS -> {
            GnssCard(
                R.drawable.ic_russia_flag_round,
                R.string.dashboard_glonass,
                R.string.dashboard_russia,
                R.string.russia_flag,
                cfs,
                finishedScanningCfs
            )
        }
        GnssType.QZSS -> {
            GnssCard(
                R.drawable.ic_japan_flag_round,
                R.string.dashboard_qzss,
                R.string.dashboard_japan,
                R.string.japan_flag,
                cfs,
                finishedScanningCfs
            )
        }
        GnssType.BEIDOU -> {
            GnssCard(
                R.drawable.ic_china_flag_round,
                R.string.dashboard_beidou,
                R.string.dashboard_china,
                R.string.china_flag,
                cfs,
                finishedScanningCfs
            )
        }
        GnssType.IRNSS -> {
            GnssCard(
                R.drawable.ic_india_flag_round,
                R.string.dashboard_irnss,
                R.string.dashboard_india,
                R.string.india_flag,
                cfs,
                finishedScanningCfs
            )
        }
        GnssType.SBAS -> return // No-op
        GnssType.UNKNOWN -> return // No-op
    }
}

@Composable
fun GnssCard(
    @DrawableRes flagId: Int,
    @StringRes nameId: Int,
    @StringRes countryId: Int,
    @StringRes contentDescriptionId: Int,
    cfs: Set<String>,
    finishedScanningCfs: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Row {
            Column {
                Image(
                    painterResource(
                        id = flagId
                    ),
                    contentDescription = stringResource(id = contentDescriptionId),
                    modifier = Modifier
                        .size(75.dp)
                        .padding(10.dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = CircleShape,
                            clip = true
                        )
                )
            }
            Column(modifier = Modifier.align(CenterVertically)) {
                Text(
                    modifier = Modifier.padding(start = 5.dp),
                    text = stringResource(id = nameId),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    modifier = Modifier.padding(start = 5.dp),
                    text = stringResource(id = countryId),
                    style = MaterialTheme.typography.body2
                )
            }
            Column(
                modifier = Modifier
                    .align(Bottom)
                    .fillMaxSize()
                    .padding(bottom = 5.dp, end = 5.dp),
                horizontalAlignment = End
            ) {
                Row {
                    if (!finishedScanningCfs) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 5.dp, top = 4.dp, bottom = 4.dp)
                                .align(CenterVertically)
                                .size(20.dp)
                        )
                    }
                    cfs.forEach {
                        Chip(it)
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        modifier = Modifier
            .padding(end = 5.dp, top = 4.dp, bottom = 4.dp)
            .width(58.dp),
        shape = MaterialTheme.shapes.small,
        color = colorResource(id = R.color.colorPrimary),
        elevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )
    }
}

@Composable
fun ProgressCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(15.dp)
            )
        }
    }
}

//@Preview
//@Composable
//fun GnssListPreview(
//    @PreviewParameter(SatelliteMetadataPreviewParameterProvider::class) satelliteMetadata: SatelliteMetadata
//) {
//    GnssList(satelliteMetadata)
//}
//
//class SatelliteMetadataPreviewParameterProvider : PreviewParameterProvider<SatelliteMetadata> {
//    override val values = sequenceOf(previewMetadata())
//}
//
//fun previewMetadata(): SatelliteMetadata {
//    val numSignalsInView = 10
//    val numSignalsUsed = 10
//    val numSignalsTotal = 10
//    val numSatsInView = 7
//    val numSatsUsed = 7
//    val numSatsTotal = 7
//    val supportedGnss: Set<GnssType> = setOf(GnssType.NAVSTAR, GnssType.GALILEO)
//    val supportedGnssCfs: Set<String> = setOf("L1", "L5", "E1")
//    val supportedSbas: Set<SbasType> = emptySet()
//    val supportedSbasCfs: Set<String> = emptySet()
//    val unknownCarrierStatuses: Map<String, SatelliteStatus> = emptyMap()
//    val duplicateCarrierStatuses: Map<String, SatelliteStatus> = emptyMap()
//    val isDualFrequencyPerSatInView = true
//    val isDualFrequencyPerSatInUse = true
//    val isNonPrimaryCarrierFreqInView = true
//    val isNonPrimaryCarrierFreqInUse = true
//
//    return SatelliteMetadata(
//        numSignalsInView,
//        numSignalsUsed,
//        numSignalsTotal,
//        numSatsInView,
//        numSatsUsed,
//        numSatsTotal,
//        supportedGnss,
//        supportedGnssCfs,
//        supportedSbas,
//        supportedSbasCfs,
//        unknownCarrierStatuses,
//        duplicateCarrierStatuses,
//        isDualFrequencyPerSatInView,
//        isDualFrequencyPerSatInUse,
//        isNonPrimaryCarrierFreqInView,
//        isNonPrimaryCarrierFreqInUse
//    )
//}