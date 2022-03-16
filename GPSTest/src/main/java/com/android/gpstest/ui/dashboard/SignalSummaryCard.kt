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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.components.CircleGraph
import com.android.gpstest.ui.components.CircleIcon
import com.android.gpstest.ui.components.OkDialog
import com.android.gpstest.util.PreferenceUtil.expandSignalSummary
import com.android.gpstest.util.PreferenceUtils
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SignalSummaryCard(
    satelliteMetadata: SatelliteMetadata,
) {
    var openDialog by remember { mutableStateOf(false) }
    Row {
        Text(
            modifier = Modifier.padding(5.dp),
            text = stringResource(id = R.string.dashboard_signal_summary),
            style = headingStyle,
            color = MaterialTheme.colors.onBackground
        )
        HelpIcon(
            modifier = Modifier.align(CenterVertically),
            onClick = { openDialog = true }
        )
    }
    OkDialog(
        open = openDialog,
        onDismiss = { openDialog = false },
        title = stringResource(R.string.dashboard_signal_summary),
        text = stringResource(R.string.dashboard_signal_summary_help)
    )
    var expandedState by rememberSaveable { mutableStateOf(expandSignalSummary()) }
    if (satelliteMetadata.numSignalsTotal == 0) {
        // Make the ProgressCard about the height of the CircleGraph to avoid UI quickly expanding
        ProgressCard(
            modifier = Modifier.height(150.dp),
            progressVisible = true,
            message = stringResource(id = R.string.dashboard_waiting_for_signals)
        )
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(5.dp)
                .clickable {
                    expandedState = !expandedState
                    PreferenceUtils.saveBoolean(
                        Application.app.getString(R.string.pref_key_expand_signal_summary),
                        expandedState
                    )
                },
            elevation = 2.dp,
        ) {
            // TODO - add spinning chevron for collapse/expand in bottom right corner
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CircleGraph(
                        currentValue = satelliteMetadata.numSatsUsed,
                        maxValue = satelliteMetadata.numSatsInView,
                        descriptionText = stringResource(R.string.satellites_in_use)
                    ) {
                        CircleIcon(
                            iconId = R.drawable.ic_satellite_alt_black_24dp,
                            iconSize = 35.dp
                        )
                    }
                    CircleGraph(
                        currentValue = satelliteMetadata.numSignalsUsed,
                        maxValue = satelliteMetadata.numSignalsInView,
                        descriptionText = stringResource(R.string.signals_in_use)
                    ) {
                        CircleIcon(
                            iconId = R.drawable.ic_wireless_vertical
                        )
                    }
                }
                if (expandedState) {
                    FlowRow {
                        for (numSignalsInViewByCf in satelliteMetadata.numSignalsInViewByCf.entries) {
                            // TODO - shrink the size of this CircleGraph to indicate it's a subset of the main 2 graphs
                            CircleGraph(
                                currentValue = satelliteMetadata.numSignalsUsedByCf[numSignalsInViewByCf.key] ?: 0,
                                maxValue = numSignalsInViewByCf.value,
                                descriptionText = stringResource(R.string.signals_in_use)
                            ) {
                                Chip(numSignalsInViewByCf.key)
                            }
                        }
                    }
                }
            }
        }
    }
}