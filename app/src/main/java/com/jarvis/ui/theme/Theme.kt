package com.jarvis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisDarkColors = darkColorScheme(
    primary = JarvisAccent,
    onPrimary = JarvisBackground,
    secondary = JarvisAccentDim,
    background = JarvisBackground,
    onBackground = JarvisOnSurface,
    surface = JarvisSurface,
    onSurface = JarvisOnSurface,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisOnSurfaceDim,
    error = JarvisError,
    onError = JarvisBackground
)

/**
 * JARVIS theme: deep black/charcoal with subtle cyan accents.
 * Always dark — the futuristic identity is part of the product.
 */
@Composable
fun JarvisTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColors,
        typography = JarvisTypography,
        content = content
    )
}
