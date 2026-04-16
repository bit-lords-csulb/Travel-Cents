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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_GROUP_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_ROOMS_NEEDED
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun HotelPricingCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    val nightly = event.detailValue(ATTR_RATE_PER_NIGHT, "rate_per_night")?.let { "$$it / night" } ?: "Price unavailable"
    val group = event.detailValue(ATTR_GROUP_RATE_PER_NIGHT, "group_rate_per_night")?.let { "$$it / night" }
    val rooms = event.detailValue(ATTR_ROOMS_NEEDED, "rooms_needed")
    val bookingUrl = event.detailValue(ATTR_BOOKING_URL, "booking_url")

    DetailCardFrame(accent = CardLavender) {
        DetailCardHeader(eyebrow = "Pricing", title = nightly)
        group?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Group total",
                    color = CardTextMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = it,
                    color = CardText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        rooms?.let {
            Spacer(modifier = Modifier.height(10.dp))
            DetailBadgeRow(badges = listOf("$it rooms needed"), accent = CardLavender)
        }
        bookingUrl?.let {
            Spacer(modifier = Modifier.height(12.dp))
            DetailLinkRow(
                label = "Booking",
                value = "Open hotel offer",
                onClick = { uriHandler.openUri(it) },
                accent = CardLavender
            )
        }
    }
}
