package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal val CardBackground = Color(0xFF091121)
internal val CardSurface = Color(0xFF121D31)
internal val CardSurfaceHigh = Color(0xFF1A2740)
internal val CardSurfaceHighest = Color(0xFF233555)
internal val CardOutline = Color(0xFF33415F)
internal val CardText = Color(0xFFE8EEFF)
internal val CardTextMuted = Color(0xFFAFBDD9)
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
