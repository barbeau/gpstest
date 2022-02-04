package com.android.gpstest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
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
    color: Color = MaterialTheme.colors.onPrimary.copy(alpha = 1.0f),
    animationDurationMs: Int = 40000,
) {
    val padding = 8.dp
    val height = 12.dp
    val stroke = 3.dp

    // Transitions used to flatten oval height to give impression of Z rotation
    val height1 = rememberInfiniteTransition()
    val hx1 by height1.animateValue(
        initialValue = 0.dp,
        targetValue = height,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs / 9, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary)
            .border(
                BorderStroke(1.dp, MaterialTheme.colors.primary),
                CircleShape
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val middle = Offset(x = size.width / 2, y = size.height / 2)
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
                start = Offset(x = size.width / 4, size.height / 2.4f),
                end = Offset(x = size.width - (size.width / 4), y = size.height / 2.4f)
            )
            // Bottom line
            drawLine(
                color = color,
                strokeWidth = stroke.toPx(),
                start = Offset(x = size.width / 4, size.height - (size.height / 2.4f)),
                end = Offset(
                    x = size.width - (size.width / 4),
                    y = size.height - (size.height / 2.4f)
                )
            )
        }
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
                drawOval(
                    color = color,
                    style = Stroke(
                        width = stroke.toPx()
                    ),
                    topLeft = Offset(
                        x = (size.minDimension / 2) - (size.minDimension / 4),
                        y = (canvasHeight / 2) - (hx1.toPx() / 2)
                    ),
                    size = Size(
                        width = canvasWidth - ((size.minDimension / 4) * 2),
                        height = hx1.toPx()
                    )
                )
            }
        }
    }
}