package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OceanBlueLight,
    onPrimary = OceanBlueDark,
    primaryContainer = OceanBlue,
    onPrimaryContainer = Color.White,
    secondary = SunsetCoralLight,
    onSecondary = SunsetCoralDark,
    secondaryContainer = SunsetCoral,
    onSecondaryContainer = Color.White,
    tertiary = ForestTealLight,
    onTertiary = ForestTealDark,
    tertiaryContainer = ForestTeal,
    onTertiaryContainer = Color.White,
    background = CharcoalDark,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CharcoalBorderDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = OceanBlueContainer,
    onPrimaryContainer = OceanBlueDark,
    secondary = SunsetCoral,
    onSecondary = Color.White,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = SunsetCoralDark,
    tertiary = ForestTeal,
    onTertiary = Color.White,
    tertiaryContainer = ForestTealLight,
    onTertiaryContainer = ForestTealDark,
    background = WarmSandLight,
    onBackground = TextPrimaryLight,
    surface = WarmSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = WarmBorderLight,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive travel branding colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

