package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.jarvis.ui.theme.JarvisGlass
import com.jarvis.ui.theme.JarvisGlassBorder
import com.jarvis.ui.theme.JarvisGradientEnd
import com.jarvis.ui.theme.JarvisGradientStart

/**
 * Glassmorphism card: translucent fill, gradient border, soft depth shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(JarvisGlass)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        JarvisGradientStart.copy(alpha = 0.5f),
                        JarvisGlassBorder,
                        JarvisGradientEnd.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
            .padding(16.dp),
        content = content
    )
}
