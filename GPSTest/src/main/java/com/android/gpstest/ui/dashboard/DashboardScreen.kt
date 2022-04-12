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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.data.FixState
import com.android.gpstest.model.*
import com.android.gpstest.ui.SignalInfoViewModel
import com.android.gpstest.ui.components.ListHeader
import com.android.gpstest.ui.components.ProgressCard
import com.android.gpstest.ui.theme.Green500
import com.android.gpstest.util.PreferenceUtils
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
    val location: Location by viewModel.location.observeAsState(Location(dummyProvider))
    val fixState: FixState by viewModel.fixState.observeAsState(FixState.NotAcquired)
    val geoidAltitude: GeoidAltitude by viewModel.geoidAltitude.observeAsState(GeoidAltitude())
    val datum: Datum by viewModel.datum.observeAsState(Datum())
    val adrStates: Set<String> by viewModel.adrStates.observeAsState(emptySet())
    val timeBetweenLocationUpdatesSeconds: Double by viewModel.timeBetweenLocationUpdatesSeconds.observeAsState(
        Double.NaN
    )
    val timeBetweenGnssSystemTimeSeconds: Double by viewModel.timeBetweenGnssSystemTimeSeconds.observeAsState(
        Double.NaN
    )
    val userCountry: UserCountry by viewModel.userCountry.observeAsState(UserCountry())

    Dashboard(
        satelliteMetadata = allSatellites.satelliteMetadata,
        scanStatus = ScanStatus(
            finishedScanningCfs = finishedScanningCfs,
            timeUntilScanCompleteMs = timeUntilScanCompleteMs,
            scanDurationMs = viewModel.scanDurationMs
        ),
        location = location,
        fixState = fixState,
        geoidAltitude = geoidAltitude,
        datum = datum,
        adrStates = adrStates,
        timeBetweenLocationUpdatesSeconds = timeBetweenLocationUpdatesSeconds,
        timeBetweenGnssSystemTimeSeconds = timeBetweenGnssSystemTimeSeconds,
        userCountry = userCountry
    )
}

@Composable
fun Dashboard(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
    location: Location,
    fixState: FixState,
    geoidAltitude: GeoidAltitude,
    datum: Datum,
    adrStates: Set<String>,
    timeBetweenLocationUpdatesSeconds: Double,
    timeBetweenGnssSystemTimeSeconds: Double,
    userCountry: UserCountry,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (PreferenceUtils.isTrackingStarted()) {
                Spacer(modifier = Modifier.padding(5.dp))
                SignalSummaryCard(satelliteMetadata, fixState)
                Spacer(modifier = Modifier.padding(5.dp))
                GnssList(satelliteMetadata.supportedGnss, satelliteMetadata.gnssToCf, scanStatus)
                Spacer(modifier = Modifier.padding(5.dp))
                SbasList(
                    satelliteMetadata.supportedGnss,
                    satelliteMetadata.supportedSbas,
                    satelliteMetadata.sbasToCf,
                    scanStatus
                )
                Spacer(modifier = Modifier.padding(5.dp))
                FeaturesAccuracyList(satelliteMetadata, scanStatus, adrStates)
                Spacer(modifier = Modifier.padding(5.dp))
                FeaturesInfoList(satelliteMetadata, scanStatus, location)
                Spacer(modifier = Modifier.padding(5.dp))
                FeaturesAssistDataList(satelliteMetadata)
                Spacer(modifier = Modifier.padding(5.dp))
                ErrorCheckList(
                    satelliteMetadata,
                    location,
                    fixState,
                    geoidAltitude,
                    datum,
                    timeBetweenLocationUpdatesSeconds,
                    timeBetweenGnssSystemTimeSeconds
                )
                Spacer(modifier = Modifier.padding(5.dp))
                DevicePropertiesList(userCountry)
                Spacer(modifier = Modifier.padding(5.dp))
            } else {
                // Show message to turn on GNSS
                ProgressCard(
                    progressVisible = false,
                    message = stringResource(id = R.string.dashboard_turn_on_gnss)
                )
            }
        }
    }
}

@Composable
fun GnssListHeader() {
    ListHeader(
        text = stringResource(id = R.string.dashboard_supported_gnss),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GnssList(
    supportedGnss: Set<GnssType>,
    gnssToCf: MutableMap<GnssType, MutableSet<String>>,
    scanStatus: ScanStatus,
) {
    if (supportedGnss.isEmpty()) {
        // Make the ProgressCard about the height of GNSS card to avoid UI quickly expanding
        ProgressCard(
            modifier = Modifier.height(240.dp),
            progressVisible = true,
            message = stringResource(id = R.string.dashboard_waiting_for_signals)
        )
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(leftColumnMargin)) {
                GnssListHeader()
                if (gnssToCf.isNotEmpty()) {
                    gnssToCf.entries.forEachIndexed { index, entry ->
                        GnssOrSbasRecord(
                            gnssType = entry.key,
                            cfs = entry.value,
                            scanStatus = scanStatus
                        )
                        maybeDivider(index, gnssToCf.entries.size)
                    }
                } else {
                    // Some devices don't support CF values, so loop through supported GNSS instead
                    supportedGnss.forEachIndexed { index, entry ->
                        GnssOrSbasRecord(
                            gnssType = entry,
                            cfs = emptySet(),
                            scanStatus = scanStatus
                        )
                        maybeDivider(index, supportedGnss.size)
                    }
                }
            }
        }
    }
}

    /**
     * Shows a divider if the [index] element is not the last in the collection ([index] != [size] - 1)
     */
    @Composable
    fun maybeDivider(index: Int, size: Int) {
        if (index != size - 1) {
            Divider(
                modifier = Modifier
                    .padding(start = leftColumnMargin, end = leftColumnMargin)
                    .fillMaxWidth(),
                thickness = Dp.Hairline,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = helpIconAlpha)
            )
        }
    }

    @Composable
    fun SbasListHeader() {
        ListHeader(
            text = stringResource(id = R.string.dashboard_supported_sbas),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SbasList(
        supportedGnss: Set<GnssType>,
        supportedSbas: Set<SbasType>,
        sbasToCf: MutableMap<SbasType, MutableSet<String>>,
        scanStatus: ScanStatus,
    ) {
        // SBAS usually show up after GNSS, so wait for GNSS to show up before potentially saying "No SBAS"
        if (supportedGnss.isEmpty()) {
            ProgressCard(
                progressVisible = true,
                message = stringResource(R.string.dashboard_waiting_for_signals)
            )
        } else {
            when {
                sbasToCf.isNotEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(modifier = Modifier.padding(leftColumnMargin)) {
                            SbasListHeader()
                            sbasToCf.entries.forEachIndexed { index, entry ->
                                SbasCard(
                                    sbasType = entry.key,
                                    cfs = entry.value,
                                    scanStatus = scanStatus
                                )
                                maybeDivider(index = index, size = sbasToCf.entries.size)
                            }
                        }
                    }
                }
                supportedSbas.isNotEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(modifier = Modifier.padding(leftColumnMargin)) {
                            SbasListHeader()
                            // Some devices don't support CF values, so loop through supported SBAS instead
                            supportedSbas.forEachIndexed { index, entry ->
                                SbasCard(
                                    sbasType = entry,
                                    cfs = emptySet(),
                                    scanStatus = scanStatus
                                )
                                maybeDivider(index = index, size = supportedSbas.size)
                            }
                        }
                    }
                }
                else -> {
                    // Show "no SBAS" card
                    ProgressCard(
                        progressVisible = false,
                        message = stringResource(R.string.sbas_not_available)
                    )
                }
            }
        }
    }

    @Composable
    fun GnssOrSbasRecord(
        gnssType: GnssType,
        cfs: Set<String>,
        scanStatus: ScanStatus
    ) {
        when (gnssType) {
            GnssType.NAVSTAR -> {
                GnssOrSbasRecord(
                    R.drawable.ic_us_flag_round,
                    R.string.gps_content_description,
                    R.string.dashboard_usa,
                    R.string.usa_flag,
                    cfs,
                    scanStatus
                )
            }
            GnssType.GALILEO -> {
                GnssOrSbasRecord(
                    R.drawable.ic_eu_flag_round,
                    R.string.galileo_content_description,
                    R.string.dashboard_eu,
                    R.string.eu_flag,
                    cfs,
                    scanStatus
                )
            }
            GnssType.GLONASS -> {
                GnssOrSbasRecord(
                    R.drawable.ic_russia_flag_round,
                    R.string.glonass_content_description,
                    R.string.dashboard_russia,
                    R.string.russia_flag,
                    cfs,
                    scanStatus
                )
            }
            GnssType.QZSS -> {
                GnssOrSbasRecord(
                    R.drawable.ic_japan_flag_round,
                    R.string.qzss_content_description,
                    R.string.dashboard_japan,
                    R.string.japan_flag,
                    cfs,
                    scanStatus
                )
            }
            GnssType.BEIDOU -> {
                GnssOrSbasRecord(
                    R.drawable.ic_china_flag_round,
                    R.string.beidou_content_description,
                    R.string.dashboard_china,
                    R.string.china_flag,
                    cfs,
                    scanStatus
                )
            }
            GnssType.IRNSS -> {
                GnssOrSbasRecord(
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
                GnssOrSbasRecord(
                    R.drawable.ic_us_flag_round,
                    R.string.waas_content_description,
                    R.string.dashboard_usa,
                    R.string.usa_flag,
                    cfs,
                    scanStatus
                )
            }
            SbasType.EGNOS -> {
                GnssOrSbasRecord(
                    R.drawable.ic_eu_flag_round,
                    R.string.egnos_content_description,
                    R.string.dashboard_eu,
                    R.string.eu_flag,
                    cfs,
                    scanStatus
                )
            }
            SbasType.SDCM -> {
                GnssOrSbasRecord(
                    R.drawable.ic_russia_flag_round,
                    R.string.sdcm_content_description,
                    R.string.dashboard_russia,
                    R.string.russia_flag,
                    cfs,
                    scanStatus
                )
            }
            SbasType.MSAS -> {
                GnssOrSbasRecord(
                    R.drawable.ic_japan_flag_round,
                    R.string.msas_content_description,
                    R.string.dashboard_japan,
                    R.string.japan_flag,
                    cfs,
                    scanStatus
                )
            }
            SbasType.SNAS -> {
                GnssOrSbasRecord(
                    R.drawable.ic_china_flag_round,
                    R.string.snas_content_description,
                    R.string.dashboard_china,
                    R.string.china_flag,
                    cfs,
                    scanStatus
                )
            }
            SbasType.GAGAN -> {
                GnssOrSbasRecord(
                    R.drawable.ic_india_flag_round,
                    R.string.gagan_content_description,
                    R.string.dashboard_india,
                    R.string.india_flag,
                    cfs,
                    scanStatus
                )
            }
            SbasType.SACCSA -> {
                GnssOrSbasRecord(
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GnssOrSbasRecord(
        @DrawableRes flagId: Int,
        @StringRes nameId: Int,
        @StringRes countryId: Int,
        @StringRes contentDescriptionId: Int,
        cfs: Set<String>,
        scanStatus: ScanStatus
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row {
                Column(modifier = Modifier.align(CenterVertically)) {
                    Image(
                        painterResource(
                            id = flagId
                        ),
                        contentDescription = stringResource(id = contentDescriptionId),
                        modifier = Modifier
                            .size(iconSize)
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
                        modifier = Modifier.padding(start = 5.dp, top = 10.dp),
                        text = stringResource(id = nameId),
                        style = titleStyle
                    )
                    Text(
                        modifier = Modifier.padding(start = 5.dp, bottom = 10.dp),
                        text = stringResource(id = countryId),
                        style = subtitleStyle
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
            progress =
                scanStatus.timeUntilScanCompleteMs.toFloat() / scanStatus.scanDurationMs.toFloat()
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
    fun Chip(
        text: String,
        textColor: Color = MaterialTheme.colorScheme.onPrimary,
        textStyle: TextStyle = chipStyle,
        backgroundColor: Color = colorResource(id = R.color.colorPrimary),
        width: Dp = 54.dp,
    ) {
        Surface(
            modifier = Modifier
                .padding(start = 5.dp, end = 5.dp, top = 4.dp, bottom = 4.dp)
                .width(width),
            shape = RoundedCornerShape(4.dp, 4.dp, 4.dp, 4.dp),
            color = backgroundColor
        ) {
            Text(
                text = text,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
    }

    @Composable
    fun PassChip() {
        Chip(
            stringResource(R.string.dashboard_pass),
            backgroundColor = Green500
        )
    }

    @Composable
    fun FailChip() {
        Chip(
            stringResource(R.string.dashboard_fail),
            backgroundColor = MaterialTheme.colorScheme.error
        )
    }

    val headingStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    )

    val titleStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp
    )

    val subtitleStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )

    val chipStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )

    val smallTitleStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    )
    val smallSubtitleStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )

    val iconSize = 70.dp
    const val helpIconAlpha = 0.4f
    val helpIconSize = 20.dp
    val helpIconStartPadding = 2.dp
    const val circleGraphAlpha = 0.9f
    val leftColumnMargin = 10.dp

    const val dummyProvider = "dummy"

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