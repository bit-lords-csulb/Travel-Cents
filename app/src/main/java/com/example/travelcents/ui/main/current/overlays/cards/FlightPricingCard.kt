package com.example.travelcents.ui.main.current.overlays.cards

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
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun FlightPricingCard(event: TravelEvent) {
    val totalPrice = formatPrice(event.detailValue("total_price")) ?: return
    val cabin = event.detailValue("cabin_class") ?: "Economy"
    val carbon = event.detailValue("carbon_diff_percent")
        ?.toIntOrNull()
        ?.let { pct -> if (pct < 0) "${-pct}% less CO₂ than typical" else "$pct% more CO₂ than typical" }
    val priceLevel = event.detailValue("price_level")
        ?.replaceFirstChar { it.uppercase() }
        ?.takeIf { it.isNotBlank() }

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
                text = cabin,
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        val badges = listOfNotNull(priceLevel, carbon)
        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            DetailBadgeRow(badges = badges, accent = CardSky)
        }
    }
}