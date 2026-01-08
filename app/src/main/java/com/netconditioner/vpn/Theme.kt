package com.netconditioner.vpn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val NeonDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EE7FF),
    secondary = Color(0xFFB388FF),
    tertiary = Color(0xFF45F5D0),
    background = Color(0xFF05070A),
    surface = Color(0xFF0E1118),
    surfaceVariant = Color(0xFF141824),
    onPrimary = Color(0xFF001015),
    onSecondary = Color(0xFF13081D),
    onBackground = Color(0xFFDAE2FF),
    onSurface = Color(0xFFDAE2FF),
)

val GlassSurfaceBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xAA1A2030),
        Color(0x66111820),
    ),
)

@Composable
fun NetConditionerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = if (useDarkTheme) NeonDarkColorScheme else NeonDarkColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
