package com.android.gpstest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun StrikeThrough(
    color: Color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1.0f),
    shadowColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 1.0f)
) {
    // Strike-through line with shadows (top and bottom)
    Canvas(
        Modifier
            .fillMaxSize()
    ) {
        val angleDegrees = 45f
        val lineSize = Size(width = size.width, height = size.height / 16)
        val yOffset = (this.size.height - lineSize.height) / 2
        val line = Rect(
            offset = Offset(x = 0.0f, y = yOffset),
            size = lineSize,
        )
        val shadowTop = Rect(
            offset = Offset(x = 12.0f, y = yOffset),
            size = lineSize,
        )
        val shadowBottom = Rect(
            offset = Offset(x = -12.0f, y = yOffset),
            size = lineSize,
        )
        // Shadow of line - top
        rotate(angleDegrees, pivot = shadowTop.center) {
            drawRect(
                shadowColor,
                topLeft = shadowTop.topLeft,
                size = shadowTop.size,
            )
        }
        // Shadow of line - bottom
        rotate(angleDegrees, pivot = shadowBottom.center) {
            drawRect(
                shadowColor,
                topLeft = shadowBottom.topLeft,
                size = shadowBottom.size,
            )
        }
        // Line
        rotate(angleDegrees, pivot = line.center) {
            drawRect(
                color,
                topLeft = line.topLeft,
                size = line.size,
            )
        }
    }
}