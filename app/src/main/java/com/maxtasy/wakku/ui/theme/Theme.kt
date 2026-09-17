package com.maxtasy.wakku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = WakkuOrangeLight,
    onPrimary = WakkuOnOrangeLight,
    primaryContainer = WakkuOrangeContainerLight,
    onPrimaryContainer = WakkuOnOrangeContainerLight,
    secondary = WakkuGreenLight,
    onSecondary = WakkuOnGreenLight,
    secondaryContainer = WakkuGreenContainerLight,
    onSecondaryContainer = WakkuOnGreenContainerLight,
    background = WakkuBackgroundLight,
    onBackground = WakkuInkLight,
    surface = WakkuSurfaceLight,
    onSurface = WakkuInkLight,
    surfaceVariant = WakkuSurfaceVariantLight,
    onSurfaceVariant = WakkuInkMutedLight,
    outline = WakkuInkFaintLight,
)

private val DarkColors = darkColorScheme(
    primary = WakkuOrangeDark,
    onPrimary = WakkuOnOrangeDark,
    primaryContainer = WakkuOrangeContainerDark,
    onPrimaryContainer = WakkuOnOrangeContainerDark,
    secondary = WakkuGreenDark,
    onSecondary = WakkuOnGreenDark,
    secondaryContainer = WakkuGreenContainerDark,
    onSecondaryContainer = WakkuOnGreenContainerDark,
    background = WakkuBackgroundDark,
    onBackground = WakkuInkDark,
    surface = WakkuSurfaceDark,
    onSurface = WakkuInkDark,
    surfaceVariant = WakkuSurfaceVariantDark,
    onSurfaceVariant = WakkuInkMutedDark,
    outline = WakkuInkFaintDark,
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
