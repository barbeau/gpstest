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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.util.MathUtils
import com.android.gpstest.util.SatelliteUtil.constellationName
import com.android.gpstest.util.SortUtil.Companion.sortByGnssThenId

@Composable
fun ErrorCheck(
    satelliteMetadata: SatelliteMetadata,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_error_check),
        style = headingStyle,
        color = MaterialTheme.colors.onBackground
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Column {
            ValidCfs(satelliteMetadata)
        }
    }
}

@Composable
fun ValidCfs(satelliteMetadata: SatelliteMetadata) {
    val pass = satelliteMetadata.unknownCarrierStatuses.isEmpty()
    Error(
        featureTitleId = R.string.dashboard_valid_cfs_title,
        featureDescriptionId = if (pass) R.string.dashboard_valid_cfs_description_pass else R.string.dashboard_valid_cfs_description_fail,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.unknownCarrierStatuses.values.toList()),
        pass = pass
    )
}

@Composable
fun Error(
    @StringRes featureTitleId: Int,
    @StringRes featureDescriptionId: Int,
    badSatelliteStatus: List<SatelliteStatus>,
    pass: Boolean,
) {
    Row {
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
                .padding(start = 10.dp, end = 10.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 5.dp, top = 10.dp),
                text = stringResource(id = featureTitleId),
                style = titleStyle
            )
            Text(
                modifier = Modifier.padding(start = 5.dp, bottom = if (pass) 10.dp else 1.dp),
                text = stringResource(id = featureDescriptionId),
                style = subtitleStyle
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row {
                if (pass) {
                    PassChip()
                } else {
                    FailChip()
                }
            }
        }
    }
    badSatelliteStatus.forEachIndexed { index, status ->
        val bottomPadding = if (index == badSatelliteStatus.size - 1) 10.dp else 0.dp
        Row(modifier = Modifier.padding(start = 20.dp, bottom = bottomPadding)) {
            val carrierMhz = MathUtils.toMhz(status.carrierFrequencyHz)
            val cf = String.format("%.3f MHz", carrierMhz)
            Text(
                text = "ID ${status.svid}, ${status.constellationName()}, $cf",
                modifier = Modifier.padding(start = 3.dp, end = 2.dp),
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}