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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * An animating orbit image
 */
@Composable
fun Orbit(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onPrimary.copy(alpha = 1.0f),
    animationDurationMs: Int = 30000,
) {
    val rotateX1 = rememberInfiniteTransition()
    val dx1 by rotateX1.animateFloat(
        initialValue = 45f,
        targetValue = 360f + 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val rotateX2 = rememberInfiniteTransition()
    val dx2 by rotateX2.animateFloat(
        initialValue = -45f,
        targetValue = 360f - 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val padding = 8.dp
    val height = 14.dp
    val stroke = 3.dp

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
                center = middle,
                radius = size.minDimension / 24
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = dx1
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val middle = Offset(x = canvasWidth / 2, y = canvasHeight / 2)

            // FIXME - try below instead of rotate() but with transform(Matrix) - https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/drawscope/DrawTransform#transform(androidx.compose.ui.graphics.Matrix). See https://developer.android.com/jetpack/compose/graphics
            // Or just animate height instead for Y "translation"
//            withTransform({
//                translate(left = canvasWidth / 5F)
//                rotate(degrees = 45F)
//            }) {
            rotate(
                degrees = dx1,
                pivot = middle
            ) {
                drawOval(
                    color = color,
                    style = Stroke(
                        width = stroke.toPx()
                    ),
                    topLeft = Offset(x = padding.toPx(), y = (canvasHeight / 2) - (height.toPx() / 2)),
                    size = Size(
                        width = canvasWidth - (padding.toPx() * 2),
                        height = height.toPx()
                    )
                )
            }
            rotate(
                dx2,
                pivot = middle
            ) {
                drawOval(
                    color = color,
                    style = Stroke(
                        width = stroke.toPx()
                    ),
                    topLeft = Offset(x = padding.toPx(), y = (canvasHeight / 2) - (height.toPx() / 2)),
                    size = Size(
                        width = canvasWidth - (padding.toPx() * 2),
                        height = height.toPx()
                    )
                )
            }
        }
    }
}