package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_ESTIMATED_HOME_COST
import com.example.travelcents.data.trip.model.ATTR_ESTIMATED_LOCAL_COST
import com.example.travelcents.data.trip.model.ATTR_FX_HISTORY_30D
import com.example.travelcents.data.trip.model.ATTR_HOME_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_LOCAL_CURRENCY
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

private data class FxSample(
    val date: String,
    val rate: Double
)

@Composable
fun CurrencyCostCard(event: TravelEvent) {
    val localCurrency = event.detailValue(ATTR_LOCAL_CURRENCY)?.takeIf { it.isNotBlank() } ?: return
    val homeCurrency = event.detailValue(ATTR_HOME_CURRENCY)?.takeIf { it.isNotBlank() } ?: return
    if (localCurrency.equals(homeCurrency, ignoreCase = true)) return

    val localCost = event.detailValue(ATTR_ESTIMATED_LOCAL_COST)?.toDoubleOrNull() ?: return
    val homeCost = event.detailValue(ATTR_ESTIMATED_HOME_COST)?.toDoubleOrNull() ?: return
    val fxHistory = remember(event.eventId, event.details[ATTR_FX_HISTORY_30D]) {
        parseFxSamples(event.detailValue(ATTR_FX_HISTORY_30D))
    }

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Cost preview",
            title = "Est. ${formatCurrency(homeCost, homeCurrency)} (${formatCurrency(localCost, localCurrency)})"
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOf(
                "$homeCurrency home",
                "$localCurrency local"
            ),
            accent = CardCoral
        )

        if (fxHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "30-DAY FX",
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            FxHistoryChart(samples = sampleFxHistory(fxHistory))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Estimated from the restaurant's price tier and recent exchange rates.",
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun FxHistoryChart(samples: List<FxSample>) {
    if (samples.isEmpty()) return
    val minRate = samples.minOf { it.rate }
    val maxRate = samples.maxOf { it.rate }
    val range = (maxRate - minRate).takeIf { it > 0.000001 } ?: 1.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        samples.forEach { sample ->
            val normalized = ((sample.rate - minRate) / range).toFloat()
            val barHeight = (18 + (normalized * 42f)).dp
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(68.dp)
                    .background(CardSurfaceHigh, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(barHeight)
                        .background(CardCoral, RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

private fun parseFxSamples(rawHistory: String?): List<FxSample> {
    return rawHistory
        ?.split('|')
        .orEmpty()
        .mapNotNull { rawEntry ->
            val entry = rawEntry.trim()
            if (entry.isBlank()) return@mapNotNull null
            val date = entry.substringBefore(',').trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val rate = entry.substringAfter(',', "").trim().toDoubleOrNull() ?: return@mapNotNull null
            FxSample(date = date, rate = rate)
        }
}

private fun sampleFxHistory(samples: List<FxSample>): List<FxSample> {
    if (samples.size <= 10) return samples
    return List(10) { index ->
        val sampleIndex = ((index.toDouble() / 9.0) * (samples.lastIndex)).toInt()
        samples[sampleIndex]
    }
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    val safeCode = currencyCode.uppercase(Locale.US)
    val roundedAmount = when (safeCode) {
        "JPY", "KRW", "VND", "IDR" -> String.format(Locale.US, "%.0f", amount)
        else -> String.format(Locale.US, "%.2f", amount).trimEnd('0').trimEnd('.')
    }
    val symbol = currencySymbol(safeCode).ifBlank { "$safeCode " }
    return "$symbol$roundedAmount"
}

private fun currencySymbol(code: String): String {
    return when (code) {
        "USD" -> "$"
        "CAD" -> "CA$"
        "AUD" -> "A$"
        "NZD" -> "NZ$"
        "SGD" -> "S$"
        "HKD" -> "HK$"
        "MXN" -> "MX$"
        "EUR" -> "EUR "
        "GBP" -> "GBP "
        "JPY" -> "JPY "
        "CNY" -> "CNY "
        "KRW" -> "KRW "
        "INR" -> "INR "
        "BRL" -> "BRL "
        "TRY" -> "TRY "
        "ILS" -> "ILS "
        "PHP" -> "PHP "
        "THB" -> "THB "
        "PLN" -> "PLN "
        "ZAR" -> "ZAR "
        "IDR" -> "IDR "
        "MYR" -> "MYR "
        "CHF" -> "CHF "
        "SEK" -> "SEK "
        "NOK" -> "NOK "
        "DKK" -> "DKK "
        else -> "$code "
    }
}
