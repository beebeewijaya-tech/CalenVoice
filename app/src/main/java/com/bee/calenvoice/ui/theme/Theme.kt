package com.bee.calenvoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Maroon,
    onPrimary = OffWhite,
    primaryContainer = Cream,
    onPrimaryContainer = MaroonDark,
    secondary = Rose,
    onSecondary = OffWhite,
    secondaryContainer = Cream,
    onSecondaryContainer = Ink,
    tertiary = Rose,
    onTertiary = OffWhite,
    background = OffWhite,
    onBackground = Ink,
    surface = OffWhite,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = InkMuted,
    outline = InkMuted,
    outlineVariant = Hairline,
    error = Danger,
    onError = OffWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = Rose,
    onPrimary = MaroonDark,
    primaryContainer = MaroonDark,
    onPrimaryContainer = Cream,
    secondary = Rose,
    onSecondary = MaroonDark,
    secondaryContainer = NightRaised,
    onSecondaryContainer = Cream,
    tertiary = Cream,
    onTertiary = MaroonDark,
    background = Night,
    onBackground = OffWhite,
    surface = Night,
    onSurface = OffWhite,
    surfaceVariant = NightRaised,
    onSurfaceVariant = Cream,
    outline = Cream,
    outlineVariant = NightRaised,
    error = Rose,
    onError = MaroonDark,
)

// dynamic color sengaja dimatikan: palet merek harus tetap sama di semua perangkat.
@Composable
fun CalenVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    typography = Typography,
    content = content,
)
