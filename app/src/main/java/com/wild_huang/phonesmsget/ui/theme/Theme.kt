package com.wild_huang.phonesmsget.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.wild_huang.phonesmsget.ColorSchemeOption
import com.wild_huang.phonesmsget.DarkModeOption
import com.wild_huang.phonesmsget.MainViewModel

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_primary,
    onPrimary = md_theme_onPrimary,
    primaryContainer = md_theme_primaryContainer,
    onPrimaryContainer = md_theme_onPrimaryContainer,
    secondary = md_theme_secondary,
    onSecondary = md_theme_onSecondary,
    secondaryContainer = md_theme_secondaryContainer,
    onSecondaryContainer = md_theme_onSecondaryContainer,
    tertiary = md_theme_tertiary,
    onTertiary = md_theme_onTertiary,
    tertiaryContainer = md_theme_tertiaryContainer,
    onTertiaryContainer = md_theme_onTertiaryContainer,
    error = md_theme_error,
    onError = md_theme_onError,
    errorContainer = md_theme_errorContainer,
    onErrorContainer = md_theme_onErrorContainer,
    background = md_theme_background,
    onBackground = md_theme_onBackground,
    surface = md_theme_surface,
    onSurface = md_theme_onSurface,
    surfaceVariant = md_theme_surfaceVariant,
    onSurfaceVariant = md_theme_onSurfaceVariant,
    outline = md_theme_outline,
    outlineVariant = md_theme_outlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_primary,
    onPrimary = md_theme_onPrimary,
    primaryContainer = md_theme_primaryContainer,
    onPrimaryContainer = md_theme_onPrimaryContainer,
    secondary = md_theme_secondary,
    onSecondary = md_theme_onSecondary,
    secondaryContainer = md_theme_secondaryContainer,
    onSecondaryContainer = md_theme_onSecondaryContainer,
    tertiary = md_theme_tertiary,
    onTertiary = md_theme_onTertiary,
    tertiaryContainer = md_theme_tertiaryContainer,
    onTertiaryContainer = md_theme_onTertiaryContainer,
    error = md_theme_error,
    onError = md_theme_onError,
    errorContainer = md_theme_errorContainer,
    onErrorContainer = md_theme_onErrorContainer,
    background = md_theme_background,
    onBackground = md_theme_onBackground,
    surface = md_theme_surface,
    onSurface = md_theme_onSurface,
    surfaceVariant = md_theme_surfaceVariant,
    onSurfaceVariant = md_theme_onSurfaceVariant,
    outline = md_theme_outline,
    outlineVariant = md_theme_outlineVariant
)

private fun getOledColorScheme(baseScheme: ColorSchemeOption, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val baseColors = when (baseScheme) {
        ColorSchemeOption.DYNAMIC -> if (isDark) DarkColorScheme else LightColorScheme
        ColorSchemeOption.PURPLE -> if (isDark) PurpleDarkColorScheme else PurpleLightColorScheme
        ColorSchemeOption.BLUE -> if (isDark) BlueDarkColorScheme else BlueLightColorScheme
        ColorSchemeOption.GREEN -> if (isDark) GreenDarkColorScheme else GreenLightColorScheme
        ColorSchemeOption.ORANGE -> if (isDark) OrangeDarkColorScheme else OrangeLightColorScheme
    }
    
    return darkColorScheme(
        primary = baseColors.primary,
        onPrimary = baseColors.onPrimary,
        primaryContainer = baseColors.primaryContainer,
        onPrimaryContainer = baseColors.onPrimaryContainer,
        secondary = baseColors.secondary,
        onSecondary = baseColors.onSecondary,
        secondaryContainer = baseColors.secondaryContainer,
        onSecondaryContainer = baseColors.onSecondaryContainer,
        tertiary = baseColors.tertiary,
        onTertiary = baseColors.onTertiary,
        tertiaryContainer = baseColors.tertiaryContainer,
        onTertiaryContainer = baseColors.onTertiaryContainer,
        error = baseColors.error,
        onError = baseColors.onError,
        errorContainer = baseColors.errorContainer,
        onErrorContainer = baseColors.onErrorContainer,
        background = Color(0xFF000000),
        onBackground = baseColors.onBackground,
        surface = Color(0xFF000000),
        onSurface = baseColors.onSurface,
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = baseColors.onSurfaceVariant,
        outline = baseColors.outline,
        outlineVariant = Color(0xFF2A2A2A)
    )
}

@Composable
fun PhoneSmsGetTheme(
    viewModel: MainViewModel,
    content: @Composable () -> Unit
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()

    val isDarkTheme = when (settingsState.darkMode) {
        DarkModeOption.SYSTEM -> systemDarkTheme
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.OLED -> true
    }

    val colorScheme = when {
        settingsState.darkMode == DarkModeOption.OLED -> {
            getOledColorScheme(settingsState.colorScheme, true)
        }
        settingsState.colorScheme == ColorSchemeOption.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDarkTheme) DarkColorScheme else LightColorScheme
            }
        }
        settingsState.colorScheme == ColorSchemeOption.PURPLE -> {
            if (isDarkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        }
        settingsState.colorScheme == ColorSchemeOption.BLUE -> {
            if (isDarkTheme) BlueDarkColorScheme else BlueLightColorScheme
        }
        settingsState.colorScheme == ColorSchemeOption.GREEN -> {
            if (isDarkTheme) GreenDarkColorScheme else GreenLightColorScheme
        }
        settingsState.colorScheme == ColorSchemeOption.ORANGE -> {
            if (isDarkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        }
        else -> {
            if (isDarkTheme) DarkColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.surface.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
