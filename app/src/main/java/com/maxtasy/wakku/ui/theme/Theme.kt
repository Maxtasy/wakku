package com.maxtasy.wakku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Every role is set explicitly: anything left out falls back to Material3's
// baseline purple, which has leaked into Switch thumbs / FABs before.
private val WakkuColors = darkColorScheme(
    primary = WakkuAccentHover,
    onPrimary = WakkuAccentFg,
    primaryContainer = WakkuAccent,
    onPrimaryContainer = WakkuAccentFg,
    inversePrimary = WakkuAccent,
    secondary = WakkuAccentSoft,
    onSecondary = WakkuBackground,
    secondaryContainer = WakkuAccentContainer,
    onSecondaryContainer = WakkuOnAccentContainer,
    tertiary = WakkuAmber,
    onTertiary = WakkuAmberFg,
    tertiaryContainer = WakkuAmberContainer,
    onTertiaryContainer = WakkuAmberHover,
    error = WakkuDanger,
    onError = WakkuOnDanger,
    errorContainer = WakkuDangerContainer,
    onErrorContainer = WakkuDangerHover,
    background = WakkuBackground,
    onBackground = WakkuFg,
    surface = WakkuBackground,
    onSurface = WakkuFg,
    surfaceVariant = WakkuBorder,
    onSurfaceVariant = WakkuFgMuted,
    surfaceTint = WakkuAccentHover,
    surfaceBright = WakkuBorder,
    surfaceDim = WakkuBackground,
    surfaceContainerLowest = WakkuBackground,
    surfaceContainerLow = WakkuSurface,
    surfaceContainer = WakkuSurface,
    surfaceContainerHigh = WakkuSurfaceHover,
    surfaceContainerHighest = WakkuBorder,
    inverseSurface = WakkuFg,
    inverseOnSurface = WakkuSurface,
    outline = WakkuOutline,
    outlineVariant = WakkuBorder,
)

@Composable
fun WakkuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WakkuColors,
        typography = Typography,
        content = content
    )
}
