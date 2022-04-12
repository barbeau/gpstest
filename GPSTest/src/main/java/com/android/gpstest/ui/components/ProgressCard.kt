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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A card that shows a progress indicator if [progressVisible] is true, and also shows a [message]
 * as text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressCard(
    modifier: Modifier = Modifier,
    progressVisible: Boolean,
    message: String) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            if (progressVisible) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            Text(
                text = message,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterVertically),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}