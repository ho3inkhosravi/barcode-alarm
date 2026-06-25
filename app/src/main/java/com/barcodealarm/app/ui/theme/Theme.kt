package com.barcodealarm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE94560),
    secondary = Color(0xFF4CC9F0),
    tertiary = Color(0xFFFF6B35),
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF0F3460),
    onSurfaceVariant = Color(0xFFB0B0C0),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE94560),
    secondary = Color(0xFF4CC9F0),
    tertiary = Color(0xFFFF6B35),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFF666680),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

@Composable
fun BarcodeAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            // Default typography with RTL support
            bodyLarge = androidx.compose.ui.text.TextStyle()
        ),
        content = content
    )
}
