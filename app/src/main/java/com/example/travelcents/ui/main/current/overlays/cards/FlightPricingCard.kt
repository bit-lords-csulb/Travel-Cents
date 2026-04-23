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
import com.example.travelcents.data.trip.model.TravelEvent

@Composable
fun FlightPricingCard(event: TravelEvent) {
    val totalPrice = formatPrice(event.details["total_price"]) ?: "Price unavailable"
    val carbon = event.details["carbon_diff_percent"]?.takeIf { it.isNotBlank() }?.let { "$it% vs typical" }
    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(eyebrow = "Pricing", title = totalPrice)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cabin",
                color = CardTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = event.details["cabin_class"] ?: "Economy",
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        carbon?.let {
            Spacer(modifier = Modifier.height(10.dp))
            DetailBadgeRow(badges = listOf(it), accent = CardSky)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Round-trip fare shown from the selected flight option.",
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
