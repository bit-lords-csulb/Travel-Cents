package com.example.travelcents.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

internal data class TravelCentsPalette(
    val deepSea1: Color,
    val deepSea2: Color,
    val deepSea3: Color,
    val deepSea4: Color,
    val deepSea5: Color,
    val blue: Color,
    val primaryContainer: Color,
    val containerHigh: Color,
    val containerHighest: Color,
    val containerLow: Color,
    val surfaceBright: Color,
    val onSurfaceVariant: Color,
    val secondaryContainer: Color
)

internal val DarkTravelCentsPalette = TravelCentsPalette(
    deepSea1 = Color(0xFF0D1B2A),
    deepSea2 = Color(0xFF1B263B),
    deepSea3 = Color(0xFF415A77),
    deepSea4 = Color(0xFF778DA9),
    deepSea5 = Color(0xFFE0E1DD),
    blue = Color(0xFF64B5F6),
    primaryContainer = Color(0xFF54A7E7),
    containerHigh = Color(0xFF0B203D),
    containerHighest = Color(0xFF102645),
    containerLow = Color(0xFF02132B),
    surfaceBright = Color(0xFF152C4E),
    onSurfaceVariant = Color(0xFF9EABC8),
    secondaryContainer = Color(0xFF3A485B)
)

internal val LightTravelCentsPalette = TravelCentsPalette(
    deepSea1 = Color(0xFFF6F8FC),
    deepSea2 = Color(0xFFFFFFFF),
    deepSea3 = Color(0xFFD8E2EF),
    deepSea4 = Color(0xFF607089),
    deepSea5 = Color(0xFF132238),
    blue = Color(0xFF1E6FE8),
    primaryContainer = Color(0xFF5FA3FF),
    containerHigh = Color(0xFFEAF1FA),
    containerHighest = Color(0xFFDDE7F4),
    containerLow = Color(0xFFF3F7FD),
    surfaceBright = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF607089),
    secondaryContainer = Color(0xFFD5E4F7)
)

internal val LocalTravelCentsPalette = staticCompositionLocalOf { DarkTravelCentsPalette }

internal object TravelCentsPaletteState {
    var currentPalette by mutableStateOf(DarkTravelCentsPalette)
}

val DeepSea1: Color
    get() = TravelCentsPaletteState.currentPalette.deepSea1

val DeepSea2: Color
    get() = TravelCentsPaletteState.currentPalette.deepSea2

val DeepSea3: Color
    get() = TravelCentsPaletteState.currentPalette.deepSea3

val DeepSea4: Color
    get() = TravelCentsPaletteState.currentPalette.deepSea4

val DeepSea5: Color
    get() = TravelCentsPaletteState.currentPalette.deepSea5
