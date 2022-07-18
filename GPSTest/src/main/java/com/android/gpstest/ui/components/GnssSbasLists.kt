/*
 * Copyright (C) 2022 Sean J. Barbeau
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
package com.android.gpstest.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.model.GnssType
import com.android.gpstest.model.SbasType
import com.android.gpstest.model.ScanStatus
import com.android.gpstest.ui.dashboard.*

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
        // Make the ProgressCard about the height of GNSS card with 3 records to avoid UI quickly expanding
        ProgressCard(
            modifier = Modifier.height(280.dp),
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
            modifier = Modifier.height(65.dp),
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
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
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
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
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
                    .align(Alignment.Bottom)
                    .fillMaxSize()
                    .padding(bottom = 5.dp, end = 5.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row {
                    if (cfs.size < 2) {
                        ChipProgress(
                            Modifier
                                .align(Alignment.CenterVertically)
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
