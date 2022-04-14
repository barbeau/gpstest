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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.data.FixState
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.components.*
import com.android.gpstest.util.PreferenceUtil.expandSignalSummary
import com.android.gpstest.util.PreferenceUtils
import com.google.accompanist.flowlayout.FlowRow

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun SignalSummaryCard(
    satelliteMetadata: SatelliteMetadata,
    fixState: FixState,
) {
    if (satelliteMetadata.numSignalsTotal == 0) {
        // Make the ProgressCard about the height of the CircleGraph to avoid UI quickly expanding
        ProgressCard(
            modifier = Modifier.height(138.dp),
            progressVisible = true,
            message = stringResource(id = R.string.dashboard_waiting_for_signals)
        )
    } else {
        CollapsibleCard(
            initialExpandedState = expandSignalSummary(),
            backgroundColor = MaterialTheme.colorScheme.primary,
            onClick = {
                PreferenceUtils.saveBoolean(
                    Application.app.getString(R.string.pref_key_expand_signal_summary),
                    it
                )
            },
            topContent = {
                TopContent(
                    satelliteMetadata = satelliteMetadata,
                    fixState = fixState
                )
            },
            expandedContent = {
                ExpandedContent(satelliteMetadata = satelliteMetadata)
            }
        )
    }
}

@Composable
fun TopContent(
    satelliteMetadata: SatelliteMetadata,
    fixState: FixState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = leftColumnMargin, bottom = 5.dp, end = leftColumnMargin)
    ) {
        LockIcon(
            modifier = Modifier
                .padding(top = 5.dp)
                .align(Alignment.TopStart),
            fixState = fixState,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            CircleGraph(
                currentValue = satelliteMetadata.numSatsUsed,
                maxValue = satelliteMetadata.numSatsInView,
                descriptionText = stringResource(R.string.satellites_in_use)
            ) {
                CircleIcon(
                    iconId = R.drawable.ic_satellite_alt_black_24dp,
                    iconSize = 35.dp,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            CircleGraph(
                currentValue = satelliteMetadata.numSignalsUsed,
                maxValue = satelliteMetadata.numSignalsInView,
                descriptionText = stringResource(R.string.signals_in_use)
            ) {
                CircleIcon(
                    iconId = R.drawable.ic_wireless_vertical,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ExpandedContent(satelliteMetadata: SatelliteMetadata) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.frequency_bands),
            style = titleStyle.copy(fontWeight = Bold),
            modifier = Modifier.padding(bottom = 5.dp)
        )
        FlowRow {
            for (numSignalsInViewByCf in satelliteMetadata.numSignalsInViewByCf.entries.toList()
                .sortedBy { it.key }) {
                CircleGraph(
                    currentValue = satelliteMetadata.numSignalsUsedByCf[numSignalsInViewByCf.key]
                        ?: 0,
                    maxValue = numSignalsInViewByCf.value,
                    descriptionText = stringResource(R.string.signals_in_use),
                    size = 88.dp,
                    largeTextStyle = titleStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    smallTextStyle = subtitleStyle.copy(fontSize = 10.sp),
                    topIconPadding = 24.dp
                ) {
                    Chip(
                        text = numSignalsInViewByCf.key,
                        textStyle = chipStyle.copy(fontSize = 12.sp),
                        width = 30.dp,
                        textColor = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        LastUpdatedText(
            currentTimeMillis = satelliteMetadata.systemCurrentTimeMillis,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}