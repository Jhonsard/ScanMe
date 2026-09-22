package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = PrimaryNavy,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = AccentBlue,
    background = PrimaryNavy,
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = PrimaryNavy,
    secondary = AccentBlue,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SurfaceCardLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Désactivé pour garantir le design identitaire NetWard
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
