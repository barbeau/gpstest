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

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.data.FixState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockIcon(
    modifier: Modifier = Modifier,
    fixState: FixState,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    visible = fixState == FixState.Acquired
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + expandVertically(expandFrom = Alignment.CenterVertically),
        exit = scaleOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_lock_24),
            contentDescription = stringResource(id = R.string.lock),
            tint = tint,
            modifier = modifier.padding(6.dp)
        )
    }
}