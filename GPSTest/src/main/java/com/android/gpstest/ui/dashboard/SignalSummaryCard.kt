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

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.components.CircleGraph
import com.android.gpstest.ui.components.OkDialog

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
                .wrapContentHeight()
                .padding(5.dp),
            elevation = 2.dp,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                CircleGraph(
                    currentValue = satelliteMetadata.numSatsUsed,
                    maxValue = satelliteMetadata.numSatsInView,
                    iconId = R.drawable.ic_satellite_alt_black_24dp,
                    iconSize = 35.dp,
                    descriptionText = stringResource(R.string.satellites_in_use)
                )
                CircleGraph(
                    currentValue = satelliteMetadata.numSignalsUsed,
                    maxValue = satelliteMetadata.numSignalsInView,
                    iconId = R.drawable.ic_wireless_vertical,
                    descriptionText = stringResource(R.string.signals_in_use)
                )
            }
        }
    }
}