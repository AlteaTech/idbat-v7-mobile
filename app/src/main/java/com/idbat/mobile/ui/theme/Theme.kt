package com.idbat.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
/*
private val DarkColorScheme = darkColorScheme(
    primary            = VeoliaPrincipal,
    secondary          = VeoliaLightGray,
    tertiary           = VeoliaLightGray,
    background         = VeoliaGray,
    surface            = Color(0xFF1E1E1E),
    surfaceVariant     = Color(0xFF2A2A2A),
    onPrimary          = White,
    onSecondary        = Black,
    onTertiary         = White,
    onBackground       = White,
    onSurface          = White,
    onSurfaceVariant   = Color(0xFFCCCCCC),
    outline            = Color(0xFF444444),
)*/

private val DarkColorScheme = lightColorScheme(
    primary            = VeoliaPrincipal,
    secondary          = VeoliaGray,
    tertiary           = VeoliaLightGray,
    background         = VeoliaLightGray,
    surface            = White,
    surfaceVariant     = VeoliaLightGray,
    onPrimary          = White,
    onSecondary        = White,
    onTertiary         = White,
    onBackground       = Black,
    onSurface          = Black,
    onSurfaceVariant   = VeoliaGray,
)

private val LightColorScheme = lightColorScheme(
    primary            = VeoliaPrincipal,
    secondary          = VeoliaGray,
    tertiary           = VeoliaLightGray,
    background         = VeoliaLightGray,
    surface            = White,
    surfaceVariant     = VeoliaLightGray,
    onPrimary          = White,
    onSecondary        = White,
    onTertiary         = White,
    onBackground       = Black,
    onSurface          = Black,
    onSurfaceVariant   = VeoliaGray,
)

@Composable
fun IdbatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
