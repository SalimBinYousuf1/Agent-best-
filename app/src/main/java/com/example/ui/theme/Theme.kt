package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Premium White Theme (Default — ChatGPT / Claude aesthetic)
private val LightColorScheme = lightColorScheme(
    primary = BrandAccent,
    onPrimary = TextInverse,
    primaryContainer = SurfaceMuted,
    onPrimaryContainer = TextPrimary,
    secondary = BrandAccentHighlight,
    onSecondary = Color.White,
    secondaryContainer = BrandAccentSubtle,
    onSecondaryContainer = BrandAccentHighlight,
    tertiary = StatusSuccess,
    onTertiary = Color.White,
    background = BackgroundWhite,
    onBackground = TextPrimary,
    surface = BackgroundWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCanvas,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderSubtle,
    error = StatusDestructive,
    onError = Color.White,
    errorContainer = StatusDestructiveSubtle,
    onErrorContainer = StatusDestructive
)

// Polished Dark Mode for equal quality
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF1F5F9),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF8FAFC),
    secondary = BrandAccentHighlight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFF6EE7B7),
    tertiary = StatusSuccess,
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F232B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2E3440),
    outlineVariant = Color(0xFF222630),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun SalimAssistantTheme(
    darkTheme: Boolean = false, // Default is premium white as required
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SalimTypography,
        shapes = SalimShapes,
        content = content
    )
}
