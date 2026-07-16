package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun OrionTheme(
    themeName: String = "space_dark",
    content: @Composable () -> Unit
) {
    currentThemeName = themeName

    val dynamicScheme = if (themeName == "light_starlight") {
        lightColorScheme(
            primary = OrionPrimary,
            secondary = OrionSecondary,
            tertiary = OrionTertiary,
            background = OrionBackground,
            surface = OrionSurface,
            surfaceVariant = OrionSurfaceVariant,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = OrionTextPrimary,
            onSurface = OrionTextPrimary,
            onSurfaceVariant = OrionTextSecondary
        )
    } else {
        darkColorScheme(
            primary = OrionPrimary,
            secondary = OrionSecondary,
            tertiary = OrionTertiary,
            background = OrionBackground,
            surface = OrionSurface,
            surfaceVariant = OrionSurfaceVariant,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = OrionTextPrimary,
            onSurface = OrionTextPrimary,
            onSurfaceVariant = OrionTextSecondary
        )
    }

    MaterialTheme(
        colorScheme = dynamicScheme,
        typography = Typography,
        content = content
    )
}
