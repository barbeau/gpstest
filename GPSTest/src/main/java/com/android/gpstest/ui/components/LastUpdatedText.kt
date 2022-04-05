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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.ui.dashboard.subtitleStyle
import com.android.gpstest.ui.status.formatTime

/**
 * A text that shows the last updated time based on the provided timestamp [currentTimeMillis].
 */
@Composable
fun LastUpdatedText(currentTimeMillis: Long) {
    Text(
        text = stringResource(
            R.string.last_updated,
            formatTime(currentTimeMillis)
        ),
        style = subtitleStyle.copy(fontSize = 12.sp),
        modifier = Modifier.padding(10.dp)
    )
}

