package com.android.gpstest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.sin

/**
 * Based on code from https://dev.to/tkuenneth/drawing-and-painting-in-jetpack-compose-1-2okl
 */
@Composable
fun Wave(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    frequencyMultiplier: Float = 1f,
    animationDurationMs: Int = 30000,
    initialDeltaX: Float = 0f
) {

    val deltaXAnim = rememberInfiniteTransition()
    val dx by deltaXAnim.animateFloat(
        initialValue = initialDeltaX,
        targetValue = 100f + initialDeltaX,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = modifier.fillMaxSize(),
        onDraw = {
            val midHeight = size.height / 2
            val doubleWidth = size.width * 2
            val points = mutableListOf<Offset>()
            for (x in -doubleWidth.toInt() until doubleWidth.toInt()) {
                val y = (sin(x * (2f * PI / (size.width * frequencyMultiplier))) * midHeight + midHeight).toFloat()
                points.add(Offset(x.toFloat(), y))
            }
            withTransform({
                translate(left = dx)
            }) {
                drawPoints(
                    points = points,
                    strokeWidth = 8f,
                    pointMode = PointMode.Points,
                    color = color,
                    cap = StrokeCap.Round,
                )
            }
        }
    )
}