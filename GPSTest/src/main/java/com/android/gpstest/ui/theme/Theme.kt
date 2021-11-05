/*
 * Copyright (C) 2021 Sean J. Barbeau (sjbarbeau@gmail.com)
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
package com.android.gpstest.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TODO - For more theme colors and dark mode see:
// * https://material.io/design/color/the-color-system.html#tools-for-picking-colors
// * https://material.io/resources/color/#!/?view.left=0&view.right=1&primary.color=3f50b5&primary.text.color=ffffff
// * https://material.io/blog/android-material-theme-color
// * https://material.io/blog/android-dark-theme-tutorial
// Issue #277

private val Purple500 = Color(0xFF3F51B5)
private val Purple700 = Color(0xFF303F9F)

private val lightColors = lightColors(
    primary = Purple500,
    primaryVariant = Purple700
)

private val darkColors = darkColors(
    primary = Purple500,
    primaryVariant = Purple700
)

@Composable
fun AppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (darkTheme) darkColors else lightColors,
    ) {
        content()
    }
}