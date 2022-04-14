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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.ui.dashboard.Support
import com.android.gpstest.ui.theme.Green500

/**
 * Shows a green check circle when [supported] is [Support.YES], a red cross circle when [supported]
 * is [Support.NO], and a gray question mark when [supported] is [Support.UNKNOWN]
 */
@Composable
fun Check(
    modifier: Modifier = Modifier,
    supported: Support
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .padding(end = 5.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
                .align(Alignment.Center)
        )
        Icon(
            modifier = modifier
                .size(34.dp)
                .align(Alignment.Center),
            imageVector = ImageVector.vectorResource(
                id = when (supported) {
                    Support.YES -> R.drawable.ic_baseline_check_circle_24
                    Support.NO -> R.drawable.ic_baseline_cancel_24
                    Support.UNKNOWN -> R.drawable.ic_baseline_question_24
                }
            ),
            contentDescription =
            when (supported) {
                Support.YES -> stringResource(R.string.dashboard_supported)
                Support.NO -> stringResource(R.string.dashboard_not_supported)
                Support.UNKNOWN -> stringResource(R.string.unknown)
            },
            tint = when (supported) {
                Support.YES -> Green500
                Support.NO -> MaterialTheme.colorScheme.error
                Support.UNKNOWN -> Color.DarkGray
            }
        )
    }
}