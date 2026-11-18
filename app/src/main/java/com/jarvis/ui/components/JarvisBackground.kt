package com.jarvis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jarvis.ui.theme.JarvisAccent
import com.jarvis.ui.theme.JarvisBackground
import com.jarvis.ui.theme.JarvisDeepBlue
import kotlin.random.Random

/**
 * Deep-space background: vertical gradient, soft top glow, and a static
 * starfield. Static (no per-frame animation) to stay battery-friendly.
 */
@Composable
fun JarvisBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val stars = remember {
        val random = Random(42)
        List(70) {
            Triple(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        JarvisBackground,
                        Color(0xFF070D1A),
                        JarvisDeepBlue.copy(alpha = 0.35f),
                        JarvisBackground
                    )
                )
            )
    ) {
        // Soft top glow.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            JarvisAccent.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.0f),
                        radius = 900f
                    )
                )
        )
        // Starfield.
        Canvas(modifier = Modifier.fillMaxSize()) {
            for ((x, y, size) in stars) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f + size * 0.25f),
                    radius = 0.8f + size * 1.6f,
                    center = Offset(x * this.size.width, y * this.size.height)
                )
            }
        }
        content()
    }
}
