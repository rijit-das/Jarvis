package com.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jarvis.ui.theme.JarvisAccent
import com.jarvis.ui.theme.JarvisBlue
import com.jarvis.ui.theme.JarvisError
import com.jarvis.ui.theme.JarvisGlow
import com.jarvis.ui.theme.JarvisSuccess
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Visual state of the JARVIS core orb. */
enum class CoreState { IDLE, LISTENING, SPEAKING, ERROR }

/**
 * The JARVIS core: a 3D-feel orb rendered on Canvas.
 *
 * - Layered radial gradients give the sphere volume (highlight offset to the
 *   top-left, dark rim).
 * - Three tilted orbit rings with satellites simulate 3D orbits.
 * - A pulsing inner core + outer glow.
 * - Drag to tilt in 3D (rotationX/rotationY with perspective).
 *
 * Pure Canvas — no images, cheap to render, battery-friendly.
 */
@Composable
fun JarvisCore(
    state: CoreState,
    modifier: Modifier = Modifier,
    interactive: Boolean = true
) {
    val accent = when (state) {
        CoreState.IDLE -> JarvisAccent
        CoreState.LISTENING -> JarvisSuccess
        CoreState.SPEAKING -> JarvisBlue
        CoreState.ERROR -> JarvisError
    }
    val speed = when (state) {
        CoreState.LISTENING -> 6000
        CoreState.SPEAKING -> 9000
        else -> 16000
    }

    val transition = rememberInfiniteTransition(label = "core")
    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    val tiltModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change ->
                change.consume()
                tiltY = (tiltY + change.positionChange().x * 0.25f).coerceIn(-28f, 28f)
                tiltX = (tiltX - change.positionChange().y * 0.25f).coerceIn(-28f, 28f)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(260.dp)
            .graphicsLayer {
                rotationX = tiltX
                rotationY = tiltY
            }
            .then(tiltModifier)
    ) {
        Canvas(modifier = Modifier.size(260.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // --- Outer glow ---
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.28f + pulse * 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                radius = radius * 1.05f,
                center = center
            )

            // --- Orbit rings (3D feel via tilted ellipses + satellites) ---
            val ringAngles = listOf(0f, 60f, 120f)
            ringAngles.forEachIndexed { index, baseAngle ->
                rotate(degrees = baseAngle) {
                    drawOval(
                        color = accent.copy(alpha = 0.35f),
                        topLeft = Offset(center.x - radius * 0.98f, center.y - radius * 0.34f),
                        size = androidx.compose.ui.geometry.Size(
                            radius * 1.96f,
                            radius * 0.68f
                        ),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Satellite on the orbit.
                    val satelliteAngle = (orbitRotation + index * 120f) * PI / 180f
                    val satelliteX = center.x + cos(satelliteAngle).toFloat() * radius * 0.98f
                    val satelliteY = center.y + sin(satelliteAngle).toFloat() * radius * 0.34f
                    drawCircle(
                        color = JarvisGlow.copy(alpha = 0.9f),
                        radius = 3.5.dp.toPx(),
                        center = Offset(satelliteX, satelliteY)
                    )
                }
            }

            // --- Sphere body (volume via offset radial gradient) ---
            val sphereRadius = radius * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        accent.copy(alpha = 0.95f),
                        accent.copy(alpha = 0.55f),
                        Color(0xFF0B1220)
                    ),
                    center = Offset(center.x - sphereRadius * 0.35f, center.y - sphereRadius * 0.4f),
                    radius = sphereRadius * 1.6f
                ),
                radius = sphereRadius,
                center = center
            )

            // --- Rim light ---
            drawCircle(
                color = accent.copy(alpha = 0.5f),
                radius = sphereRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // --- Specular highlight (top-left) ---
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = sphereRadius * 0.22f,
                center = Offset(center.x - sphereRadius * 0.38f, center.y - sphereRadius * 0.45f)
            )

            // --- Pulsing inner core ---
            val coreRadius = sphereRadius * (0.30f + pulse * 0.10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        JarvisGlow.copy(alpha = 0.9f),
                        accent.copy(alpha = 0.0f)
                    ),
                    center = center,
                    radius = coreRadius * 1.8f
                ),
                radius = coreRadius * 1.8f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = coreRadius,
                center = center
            )
        }
    }
}
