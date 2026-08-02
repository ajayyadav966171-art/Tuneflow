package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TuneFlowColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = BackgroundVoid,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldLight,
    onSecondary = BackgroundVoid,
    tertiary = AccentPurple,
    background = BackgroundVoid,
    onBackground = TextPrimary,
    surface = SurfaceGlassDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGlassCard,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceGlassBorder
)

@Composable
fun TuneFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TuneFlowColorScheme,
        typography = Typography,
        content = content
    )
}

