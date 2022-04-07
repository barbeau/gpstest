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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.android.gpstest.ui.dashboard.HelpIcon
import com.android.gpstest.ui.dashboard.headingStyle
import com.android.gpstest.ui.dashboard.leftColumnMargin

/**
 * A title and help icon that's used as a header for each section in Dashboard. A dialog is shown
 * when the help icon is tapped. [titleTextId] is the main title header, [helpTitleId] is the title
 * of the help dialog, and [helpTextId] is the text shown in the help dialog.
 */
@Composable
fun TitleWithHelp(
    @StringRes titleTextId: Int,
    @StringRes helpTitleId: Int,
    @StringRes helpTextId: Int,
) {
    var openDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {
        ListHeader(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(
                    start = leftColumnMargin,
                    end = leftColumnMargin,
                    top = 15.dp,
                    bottom = leftColumnMargin
                ),
            text = stringResource(id = titleTextId)
        )
        HelpIcon(
            modifier = Modifier
                .padding(5.dp)
                .align(Alignment.CenterVertically),
            onClick = { openDialog = true }
        )
    }
    OkDialog(
        open = openDialog,
        onDismiss = { openDialog = false },
        title = stringResource(helpTitleId),
        text = stringResource(helpTextId)
    )
}

@Composable
fun ListHeader(
    modifier: Modifier = Modifier.padding(
        start = leftColumnMargin,
        end = leftColumnMargin,
        top = 15.dp,
        bottom = leftColumnMargin
    ),
    style: TextStyle = headingStyle,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        style = style,
    )
}


