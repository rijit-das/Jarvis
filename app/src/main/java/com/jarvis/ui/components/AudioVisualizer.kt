package com.jarvis.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarvis.ui.theme.JarvisAccent
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated audio visualization: a ring of bars pulsing while listening.
 * Purely visual — driven by a sine wave, not real mic levels, to avoid
 * implying precise metering.
 */
@Composable
fun AudioVisualizer(
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = JarvisAccent
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = modifier.size(160.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 - 16.dp.toPx()
        val barCount = 32
        for (i in 0 until barCount) {
            val angle = (i.toFloat() / barCount) * 2 * PI
            val wave = if (active) {
                (sin(phase + i * 0.6f) * 0.5f + 0.5f)
            } else {
                0.15f
            }
            val barLength = radius * (0.25f + wave * 0.55f)
            val startX = centerX + (radius * 0.55f) * kotlin.math.cos(angle).toFloat()
            val startY = centerY + (radius * 0.55f) * kotlin.math.sin(angle).toFloat()
            val endX = centerX + (radius * 0.55f + barLength) * kotlin.math.cos(angle).toFloat()
            val endY = centerY + (radius * 0.55f + barLength) * kotlin.math.sin(angle).toFloat()
            drawLine(
                color = color.copy(alpha = 0.35f + wave * 0.65f),
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = 4.dp.toPx()
            )
        }
    }
}
