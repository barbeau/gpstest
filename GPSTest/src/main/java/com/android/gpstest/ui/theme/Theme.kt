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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TODO - For more theme colors and dark mode see:
// * https://material.io/design/color/the-color-system.html#tools-for-picking-colors
// * https://material.io/resources/color/#!/?view.left=0&view.right=1&primary.color=3f50b5&primary.text.color=ffffff
// * https://material.io/blog/android-material-theme-color
// * https://material.io/blog/android-dark-theme-tutorial
// * https://material.io/blog/material-theme-builder
// Issue #277

private val Indigo500 = Color(0xFF3F51B5)
private val primaryDark = Color(0xFF002984)
//val Purple200 = Color(0xFFBB86FC)
val Indigo050 = Color(0xFFE8EAF6)
val Indigo100 = Color(0xFFc5cae9)
val Indigo200 = Color(0xFF9fa8da)
val Indigo300 = Color(0xFF7986cb)
val Indigo400 = Color(0xFF5c6bc0)
val Indigo600 = Color(0xFF3949ab)
val Indigo700 = Color(0xFF303f9f)
val Indigo800 = Color(0xFF283593)
val Indigo900 = Color(0xFF1a237e)
val IndigoLight = Color(0xFF757DE8)
val LightGray = Color(0xFFE3E1EC)
val Green500 = Color(0xFF4caf50)
val DarkGray = Color(0xFF232323)

private val lightColors = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = LightGray
    //secondary = primaryDark
)

private val darkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color.Black,
    primaryContainer = DarkGray
    //secondary = primaryDark
)

@Composable
fun AppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        // isSystemInDarkTheme()
        colorScheme = if (darkTheme) darkColors else lightColors,
    ) {
        content()
    }
}