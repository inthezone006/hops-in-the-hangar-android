package com.rahul.hopsinthehangar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeoLightColorScheme = lightColorScheme(
    primary = NeoBlack,
    onPrimary = NeoYellow,
    secondary = NeoPink,
    onSecondary = NeoBlack,
    tertiary = NeoBlue,
    onTertiary = NeoBlack,
    background = NeoCream,
    onBackground = NeoBlack,
    surface = NeoWhite,
    onSurface = NeoBlack,
    surfaceVariant = NeoYellow,
    onSurfaceVariant = NeoBlack,
    outline = NeoBlack,
    error = Color(0xFFFF5757),
    onError = NeoWhite
)

@Composable
fun HopsInTheHangarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeoLightColorScheme,
        typography = Typography,
        content = content
    )
}
