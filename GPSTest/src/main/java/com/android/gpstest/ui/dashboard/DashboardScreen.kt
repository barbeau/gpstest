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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.data.FixState
import com.android.gpstest.model.*
import com.android.gpstest.ui.SignalInfoViewModel
import com.android.gpstest.ui.components.GnssList
import com.android.gpstest.ui.components.ProgressCard
import com.android.gpstest.ui.components.SbasList
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun DashboardScreen(viewModel: SignalInfoViewModel) {
    val prefs: AppPreferences by viewModel.prefs.observeAsState(AppPreferences(true))
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
        userCountry = userCountry,
        prefs = prefs
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
    prefs: AppPreferences,
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
            if (prefs.isTrackingStarted) {
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
                Spacer(modifier = Modifier.padding(5.dp))
                ProgressCard(
                    progressVisible = false,
                    message = stringResource(id = R.string.dashboard_turn_on_gnss)
                )
            }
        }
    }
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