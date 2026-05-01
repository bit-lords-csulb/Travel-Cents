package com.example.travelcents.ui.main.current.overlays.cards

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

fun formatPrice(amount: Double): String {
    val rounded = round(amount)
    return if (abs(amount - rounded) < 0.01) {
        "$${String.format(Locale.US, "%,d", rounded.toLong())}"
    } else {
        "$${String.format(Locale.US, "%,.2f", amount)}"
    }
}

fun formatPrice(amountText: String?): String? {
    val normalized = amountText
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.removePrefix("$")
        ?.replace(",", "")
        ?: return null
    return normalized.toDoubleOrNull()?.let(::formatPrice)
}
