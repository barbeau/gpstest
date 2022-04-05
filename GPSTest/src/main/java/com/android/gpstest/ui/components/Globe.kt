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

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An globe image that appears to be turning
 */
@Composable
fun Globe(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1.0f),
    animationDurationMs: Int = 40000,
) {
    val height = 21.dp
    val stroke = 2.5.dp

    // Transitions used to flatten oval height to give impression of Z rotation
    val height1 = rememberInfiniteTransition()
    val hx1 by height1.animateValue(
        initialValue = 0.dp,
        targetValue = height,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs / 9, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                CircleShape
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val middle = Offset(x = size.width / 2, y = size.height / 2)
            val quarterWidth = size.width / 4
            val topLineHeight = size.height / 2.4f
            drawCircle(
                color = color,
                style = Stroke(
                    width = stroke.toPx()
                ),
                center = middle,
                radius = size.minDimension / 4
            )
            // Top line
            drawLine(
                color = color,
                strokeWidth = stroke.toPx(),
                start = Offset(x = size.width / 4, y = topLineHeight),
                end = Offset(x = size.width - quarterWidth, y = topLineHeight)
            )
            // Bottom line
            drawLine(
                color = color,
                strokeWidth = stroke.toPx(),
                start = Offset(x = quarterWidth, y = size.height - topLineHeight),
                end = Offset(x = size.width - quarterWidth, y = size.height - topLineHeight)
            )
        }
        // Rotating middle ovals
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val middle = Offset(x = canvasWidth / 2, y = canvasHeight / 2)

            rotate(
                degrees = 90.0f,
                pivot = middle
            ) {
                val halfMin = size.minDimension / 2
                val quarterMin = size.minDimension / 4
                val halfHeight = canvasHeight / 2
                drawOval(
                    color = color,
                    style = Stroke(
                        width = stroke.toPx()
                    ),
                    topLeft = Offset(
                        x = halfMin - quarterMin,
                        y = halfHeight - (hx1.toPx() / 2)
                    ),
                    size = Size(
                        width = canvasWidth - quarterMin * 2,
                        height = hx1.toPx()
                    )
                )
            }
        }
    }
}