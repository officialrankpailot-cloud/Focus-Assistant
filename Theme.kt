package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DisciplineDarkColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = NeonCrimson,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF881337),
    onTertiaryContainer = Color(0xFFFFD1DC),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun DisciplineTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DisciplineDarkColorScheme,
        typography = Typography,
        content = content
    )
}
