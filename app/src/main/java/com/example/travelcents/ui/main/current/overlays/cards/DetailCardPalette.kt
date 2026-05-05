package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5

internal val CardBackground: Color
    get() = DeepSea1

internal val CardSurface: Color
    get() = TripWizardColors.ContainerLow

internal val CardSurfaceHigh: Color
    get() = TripWizardColors.ContainerHigh

internal val CardSurfaceHighest: Color
    get() = TripWizardColors.ContainerHighest

internal val CardOutline: Color
    get() = TripWizardColors.OnSurfaceVariant

internal val CardText: Color
    get() = DeepSea5

internal val CardTextMuted: Color
    get() = TripWizardColors.OnSurfaceVariant

internal val CardLavender = Color(0xFFC8B9FF)
internal val CardLavenderStrong = Color(0xFF9E87F5)
internal val CardGold = Color(0xFFF1CB77)
internal val CardMint = Color(0xFF8CD7BE)
internal val CardCoral = Color(0xFFFF8E7A)
internal val CardSky = Color(0xFF7BC5FF)

internal fun accentForType(type: String): Color = when (type.lowercase()) {
    "flight" -> CardSky
    "hotel" -> CardLavender
    "restaurant", "dining", "food" -> CardCoral
    else -> CardMint
}

internal fun accentGradientForType(type: String): Brush = Brush.linearGradient(
    colors = when (type.lowercase()) {
        "flight" -> listOf(CardSky, Color(0xFF4E86D9))
        "hotel" -> listOf(CardLavender, CardLavenderStrong)
        "restaurant", "dining", "food" -> listOf(CardCoral, Color(0xFFD35E58))
        else -> listOf(CardMint, Color(0xFF4CA88C))
    }
)
