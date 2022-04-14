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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.model.ScanStatus
import com.android.gpstest.ui.dashboard.chipStyle
import com.android.gpstest.ui.theme.Green500

/**
 * A chip designed after the Material Design 3 chip - https://m3.material.io/components/chips/overview
 */
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

/**
 * A chip indicating that an error check has passed
 */
@Composable
fun PassChip() {
    Chip(
        stringResource(R.string.dashboard_pass),
        backgroundColor = Green500
    )
}

/**
 * A chip indicating that an error check has failed
 */
@Composable
fun FailChip() {
    Chip(
        stringResource(R.string.dashboard_fail),
        backgroundColor = MaterialTheme.colorScheme.error
    )
}

/**
 * A circular progress bar roughly the same size as a chip that disappears when [scanStatus] is
 * within the time threshold following a first fix
 */
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