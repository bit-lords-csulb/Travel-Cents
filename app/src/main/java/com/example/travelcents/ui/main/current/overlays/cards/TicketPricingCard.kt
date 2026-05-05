package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_TICKET_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MAX
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MIN
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

enum class TicketPricingMode {
    STANDARD,
    TICKETMASTER
}

@Composable
fun TicketPricingCard(
    event: TravelEvent,
    mode: TicketPricingMode = if (!event.detailValue(ATTR_TICKETMASTER_EVENT_ID).isNullOrBlank()) {
        TicketPricingMode.TICKETMASTER
    } else {
        TicketPricingMode.STANDARD
    }
) {
    val minimum = event.detailValue(ATTR_TICKET_PRICE_MIN)?.toDoubleOrNull()
    val maximum = event.detailValue(ATTR_TICKET_PRICE_MAX)?.toDoubleOrNull()
    val currency = event.detailValue(ATTR_TICKET_CURRENCY)?.takeIf { it.isNotBlank() }
    val title = formatTicketPriceRange(minimum, maximum, currency) ?: return
    val minimumLabel = if (mode == TicketPricingMode.TICKETMASTER) "General admission" else "Lowest listed"
    val maximumLabel = if (mode == TicketPricingMode.TICKETMASTER) "VIP" else "Highest listed"

    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(
            eyebrow = "Ticket pricing",
            title = title
        )
        currency?.let {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(badges = listOf(it.uppercase(Locale.US)), accent = CardMint)
        }
        minimum?.let {
            Spacer(modifier = Modifier.height(12.dp))
            TicketPriceRow(
                label = minimumLabel,
                value = formatTicketAmount(it, currency)
            )
        }
        maximum?.let {
            Spacer(modifier = Modifier.height(10.dp))
            TicketPriceRow(
                label = maximumLabel,
                value = formatTicketAmount(it, currency)
            )
        }
    }
}

@Composable
private fun TicketPriceRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CardTextMuted,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = CardText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTicketPriceRange(
    minimum: Double?,
    maximum: Double?,
    currency: String?
): String? {
    val formattedMinimum = minimum?.let { formatTicketAmount(it, currency) }
    val formattedMaximum = maximum?.let { formatTicketAmount(it, currency) }
    return when {
        formattedMinimum != null && formattedMaximum != null && minimum == maximum -> formattedMinimum
        formattedMinimum != null && formattedMaximum != null -> "$formattedMinimum - $formattedMaximum"
        formattedMinimum != null -> "From $formattedMinimum"
        formattedMaximum != null -> "Up to $formattedMaximum"
        else -> null
    }
}

private fun formatTicketAmount(
    amount: Double,
    currency: String?
): String {
    val trimmedAmount = if (amount % 1.0 == 0.0) {
        amount.toInt().toString()
    } else {
        "%.2f".format(Locale.US, amount)
            .trimEnd('0')
            .trimEnd('.')
    }
    return currency?.uppercase(Locale.US)?.let { "$it $trimmedAmount" } ?: trimmedAmount
}
