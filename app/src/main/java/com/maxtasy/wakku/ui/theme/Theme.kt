package com.maxtasy.wakku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = WakkuOrangeLight,
    background = WakkuBackgroundLight,
    onBackground = WakkuInkLight,
    surface = WakkuSurfaceLight,
    onSurface = WakkuInkLight,
)

private val DarkColors = darkColorScheme(
    primary = WakkuOrangeDark,
    background = WakkuBackgroundDark,
    onBackground = WakkuInkDark,
    surface = WakkuSurfaceDark,
    onSurface = WakkuInkDark,
)

@Composable
fun WakkuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
