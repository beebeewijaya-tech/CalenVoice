package com.bee.calenvoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Maroon,
    onPrimary = OffWhite,
    secondary = Cream,
    onSecondary = Ink,
    secondaryContainer = Cream,
    onSecondaryContainer = Ink,
    tertiary = Rose,
    onTertiary = OffWhite,
    background = OffWhite,
    onBackground = Ink,
    surface = OffWhite,
    onSurface = Ink,
)

private val DarkColorScheme = darkColorScheme(
    primary = Rose,
    onPrimary = MaroonDark,
    secondary = Cream,
    onSecondary = MaroonDark,
    tertiary = Rose,
    onTertiary = MaroonDark,
    background = MaroonDark,
    onBackground = OffWhite,
    surface = MaroonDark,
    onSurface = OffWhite,
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
