package com.example.travelcents.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkTravelCentsPalette.blue,
    onPrimary = DarkTravelCentsPalette.deepSea1,
    primaryContainer = DarkTravelCentsPalette.primaryContainer,
    onPrimaryContainer = DarkTravelCentsPalette.deepSea5,
    secondary = DarkTravelCentsPalette.deepSea4,
    onSecondary = DarkTravelCentsPalette.deepSea1,
    secondaryContainer = DarkTravelCentsPalette.secondaryContainer,
    onSecondaryContainer = DarkTravelCentsPalette.deepSea5,
    tertiary = DarkTravelCentsPalette.blue,
    background = DarkTravelCentsPalette.deepSea1,
    onBackground = DarkTravelCentsPalette.deepSea5,
    surface = DarkTravelCentsPalette.deepSea2,
    onSurface = DarkTravelCentsPalette.deepSea5,
    surfaceVariant = DarkTravelCentsPalette.deepSea3,
    onSurfaceVariant = DarkTravelCentsPalette.deepSea4,
    outline = DarkTravelCentsPalette.deepSea3,
    error = Color(0xFFE57373),
    onError = Color(0xFF2B0B0B),
    errorContainer = Color(0xFF6D1F1F),
    onErrorContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightTravelCentsPalette.blue,
    onPrimary = Color.White,
    primaryContainer = LightTravelCentsPalette.primaryContainer,
    onPrimaryContainer = LightTravelCentsPalette.deepSea5,
    secondary = LightTravelCentsPalette.deepSea4,
    onSecondary = Color.White,
    secondaryContainer = LightTravelCentsPalette.secondaryContainer,
    onSecondaryContainer = LightTravelCentsPalette.deepSea5,
    tertiary = LightTravelCentsPalette.blue,
    background = LightTravelCentsPalette.deepSea1,
    onBackground = LightTravelCentsPalette.deepSea5,
    surface = LightTravelCentsPalette.deepSea2,
    onSurface = LightTravelCentsPalette.deepSea5,
    surfaceVariant = LightTravelCentsPalette.deepSea3,
    onSurfaceVariant = LightTravelCentsPalette.deepSea4,
    outline = LightTravelCentsPalette.deepSea3,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun TravelCentsTheme(
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

    val palette = if (darkTheme) DarkTravelCentsPalette else LightTravelCentsPalette

    SideEffect {
        TravelCentsPaletteState.currentPalette = palette
    }

    CompositionLocalProvider(LocalTravelCentsPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
