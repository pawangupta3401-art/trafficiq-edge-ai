package com.example.trafficiq.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TrafficIQDarkColorScheme = darkColorScheme(
    primary = TrafficCyan,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = TrafficCyan,
    secondary = TrafficEmerald,
    onSecondary = DarkBackground,
    tertiary = TrafficAmber,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

@Composable
fun TrafficIQTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrafficIQDarkColorScheme,
        typography = Typography,
        content = content
    )
}
