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

import android.location.Location
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.model.*
import com.android.gpstest.ui.SignalInfoViewModel
import com.android.gpstest.util.PreferenceUtils.isTrackingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun DashboardScreen(viewModel: SignalInfoViewModel) {
    val allSatellites: SatelliteGroup by viewModel.allSatellitesGroup.observeAsState(
        SatelliteGroup(emptyMap(), satelliteMetadata = SatelliteMetadata())
    )
    val finishedScanningCfs: Boolean by viewModel.finishedScanningCfs.observeAsState(false)
    val timeUntilScanCompleteMs: Long by viewModel.timeUntilScanCompleteMs.observeAsState(viewModel.scanDurationMs)
    val location: Location by viewModel.location.observeAsState(Location("default"))

    Dashboard(
        satelliteMetadata = allSatellites.satelliteMetadata,
        scanStatus = ScanStatus(
            finishedScanningCfs = finishedScanningCfs,
            timeUntilScanCompleteMs = timeUntilScanCompleteMs,
            scanDurationMs = viewModel.scanDurationMs
        ),
        location = location
    )
}

@Composable
fun Dashboard(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
    location: Location,
) {
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
            GnssList(satelliteMetadata.gnssToCf, scanStatus)
            Spacer(modifier = Modifier.padding(5.dp))
            SbasList(satelliteMetadata.sbasToCf, scanStatus, satelliteMetadata.numSignalsUsed)
            Spacer(modifier = Modifier.padding(5.dp))
            FeaturesAccuracyList(satelliteMetadata, scanStatus)
            Spacer(modifier = Modifier.padding(5.dp))
            FeaturesInfoList(satelliteMetadata, scanStatus, location)
            Spacer(modifier = Modifier.padding(5.dp))
            FeaturesAssistDataList(satelliteMetadata)
        }
    }
}

@Composable
fun GnssList(
    gnssToCf: MutableMap<GnssType, MutableSet<String>>,
    scanStatus: ScanStatus,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_supported_gnss),
        style = MaterialTheme.typography.h6,
        color = MaterialTheme.colors.onBackground
    )
    if (gnssToCf.entries.isEmpty()) {
        ProgressCard(true, stringResource(id = R.string.dashboard_waiting_for_fix))
    } else {
        gnssToCf.entries.forEach {
            GnssOrSbasCard(
                gnssType = it.key,
                cfs = it.value,
                scanStatus = scanStatus
            )
        }
    }
}

@Composable
fun SbasList(
    sbasToCf: MutableMap<SbasType, MutableSet<String>>,
    scanStatus: ScanStatus,
    numSignalsUsed: Int
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_supported_sbas),
        style = MaterialTheme.typography.h6,
        color = MaterialTheme.colors.onBackground
    )
    if (sbasToCf.entries.isEmpty()) {
        if (numSignalsUsed == 0) {
            // We don't have a fix yet, just show waiting card
            ProgressCard(
                true,
                stringResource(R.string.dashboard_waiting_for_fix)
            )
        } else {
            // Show "no SBAS" card
            ProgressCard(
                false,
                stringResource(R.string.sbas_not_available)
            )
        }
    } else {
        sbasToCf.entries.forEach {
            SbasCard(
                sbasType = it.key,
                cfs = it.value,
                scanStatus = scanStatus
            )
        }
    }
}

@Composable
fun GnssOrSbasCard(
    gnssType: GnssType,
    cfs: Set<String>,
    scanStatus: ScanStatus
) {
    when (gnssType) {
        GnssType.NAVSTAR -> {
            GnssOrSbasCard(
                R.drawable.ic_us_flag_round,
                R.string.gps_content_description,
                R.string.dashboard_usa,
                R.string.usa_flag,
                cfs,
                scanStatus
            )
        }
        GnssType.GALILEO -> {
            GnssOrSbasCard(
                R.drawable.ic_eu_flag_round,
                R.string.galileo_content_description,
                R.string.dashboard_eu,
                R.string.eu_flag,
                cfs,
                scanStatus
            )
        }
        GnssType.GLONASS -> {
            GnssOrSbasCard(
                R.drawable.ic_russia_flag_round,
                R.string.glonass_content_description,
                R.string.dashboard_russia,
                R.string.russia_flag,
                cfs,
                scanStatus
            )
        }
        GnssType.QZSS -> {
            GnssOrSbasCard(
                R.drawable.ic_japan_flag_round,
                R.string.qzss_content_description,
                R.string.dashboard_japan,
                R.string.japan_flag,
                cfs,
                scanStatus
            )
        }
        GnssType.BEIDOU -> {
            GnssOrSbasCard(
                R.drawable.ic_china_flag_round,
                R.string.beidou_content_description,
                R.string.dashboard_china,
                R.string.china_flag,
                cfs,
                scanStatus
            )
        }
        GnssType.IRNSS -> {
            GnssOrSbasCard(
                R.drawable.ic_india_flag_round,
                R.string.irnss_content_description,
                R.string.dashboard_india,
                R.string.india_flag,
                cfs,
                scanStatus
            )
        }
        GnssType.SBAS -> return // No-op
        GnssType.UNKNOWN -> return // No-op
    }
}

@Composable
fun SbasCard(
    sbasType: SbasType,
    cfs: Set<String>,
    scanStatus: ScanStatus,
) {
    when (sbasType) {
        SbasType.WAAS -> {
            GnssOrSbasCard(
                R.drawable.ic_us_flag_round,
                R.string.waas_content_description,
                R.string.dashboard_usa,
                R.string.usa_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.EGNOS -> {
            GnssOrSbasCard(
                R.drawable.ic_eu_flag_round,
                R.string.egnos_content_description,
                R.string.dashboard_eu,
                R.string.eu_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.SDCM -> {
            GnssOrSbasCard(
                R.drawable.ic_russia_flag_round,
                R.string.sdcm_content_description,
                R.string.dashboard_russia,
                R.string.russia_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.MSAS -> {
            GnssOrSbasCard(
                R.drawable.ic_japan_flag_round,
                R.string.msas_content_description,
                R.string.dashboard_japan,
                R.string.japan_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.SNAS -> {
            GnssOrSbasCard(
                R.drawable.ic_china_flag_round,
                R.string.snas_content_description,
                R.string.dashboard_china,
                R.string.china_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.GAGAN -> {
            GnssOrSbasCard(
                R.drawable.ic_india_flag_round,
                R.string.gagan_content_description,
                R.string.dashboard_india,
                R.string.india_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.SACCSA -> {
            GnssOrSbasCard(
                R.drawable.ic_flag_icao,
                R.string.saccsa_content_description,
                R.string.dashboard_icao,
                R.string.japan_flag,
                cfs,
                scanStatus
            )
        }
        SbasType.UNKNOWN -> {
            // No-op
        }
    }
}

@Composable
fun GnssOrSbasCard(
    @DrawableRes flagId: Int,
    @StringRes nameId: Int,
    @StringRes countryId: Int,
    @StringRes contentDescriptionId: Int,
    cfs: Set<String>,
    scanStatus: ScanStatus
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
                    if (cfs.size < 2) {
                        ChipProgress(
                            Modifier
                                .align(CenterVertically)
                                .padding(end = 5.dp, top = 4.dp, bottom = 4.dp),
                            scanStatus
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
fun ChipProgress(
    modifier: Modifier = Modifier,
    scanStatus: ScanStatus
) {
    var progress by remember { mutableStateOf(1.0f) }
    // Only show the "scanning" mini progress circle if it's within the time threshold
    // following the first fix
    if (!scanStatus.finishedScanningCfs && scanStatus.timeUntilScanCompleteMs >= 0) {
        progress = scanStatus.timeUntilScanCompleteMs.toFloat() / scanStatus.scanDurationMs.toFloat()
        val animatedProgress = animateFloatAsState(
            targetValue = progress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        ).value

        CircularProgressIndicator(
            modifier = modifier
                .size(20.dp),
            progress = animatedProgress,
        )
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
fun ProgressCard(progressVisible: Boolean, message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            if (progressVisible && isTrackingStarted()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(15.dp)
                )
            }
            Text(
                text = if (isTrackingStarted()) message else stringResource(id = R.string.dashboard_turn_on_gnss),
                modifier = Modifier
                    .padding(10.dp)
                    .align(CenterVertically),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
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