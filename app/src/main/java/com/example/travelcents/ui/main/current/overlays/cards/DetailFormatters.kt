package com.example.travelcents.ui.main.current.overlays.cards

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

fun formatPrice(amount: Double, currencyCode: String = "USD"): String {
    val rounded = round(amount)
    val symbol = getCurrencySymbol(currencyCode)
    return if (abs(amount - rounded) < 0.01) {
        "$symbol${String.format(Locale.US, "%,d", rounded.toLong())}"
    } else {
        "$symbol${String.format(Locale.US, "%,.2f", amount)}"
    }
}

fun formatPrice(amountText: String?, currencyCode: String = "USD"): String? {
    val normalized = amountText
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.removePrefix("$")
        ?.replace(",", "")
        ?: return null
    return normalized.toDoubleOrNull()?.let { formatPrice(it, currencyCode) }
}

private fun getCurrencySymbol(code: String): String {
    return when (code.uppercase(Locale.US)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "CAD" -> "CA$"
        "AUD" -> "A$"
        "NZD" -> "NZ$"
        "SGD" -> "S$"
        "HKD" -> "HK$"
        "MXN" -> "MX$"
        "CNY" -> "CN¥"
        "KRW" -> "₩"
        "INR" -> "₹"
        "BRL" -> "R$"
        "TRY" -> "₺"
        "ILS" -> "₪"
        "PHP" -> "₱"
        "THB" -> "฿"
        "PLN" -> "zł"
        "ZAR" -> "R"
        "IDR" -> "Rp"
        "MYR" -> "RM"
        "CHF" -> "CHF"
        "SEK" -> "kr"
        "NOK" -> "kr"
        "DKK" -> "kr"
        else -> "$code "
    }
}
