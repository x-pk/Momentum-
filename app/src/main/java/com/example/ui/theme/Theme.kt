package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PitchBlackColorScheme = darkColorScheme(
    primary = OrangeRescue,
    onPrimary = Color.Black,
    secondary = Color(0xFF111111),
    background = Color.Black,
    surface = Color(0xFF111111),
    onBackground = Color.White,
    onSurface = Color(0xFFEEEEEE)
)

private val CleanLightColorScheme = lightColorScheme(
    primary = OrangeRescue,
    onPrimary = Color.White,
    secondary = Color(0xFFF1F5F9),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) PitchBlackColorScheme else CleanLightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
