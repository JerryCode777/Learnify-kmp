package org.example.learnify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light color scheme - Kotlin Multiplatform + Antigravity style
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,

    secondary = Secondary,
    onSecondary = OnPrimary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,

    tertiary = Tertiary,
    onTertiary = OnPrimary,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = TertiaryDark,

    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    outline = Outline,
)

@Composable
fun LearnifyTheme(
    darkTheme: Boolean = false,  // Always light theme - Antigravity style
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
