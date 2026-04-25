package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun VenueCard(event: TravelEvent) {
    val venueName = event.detailValue(ATTR_VENUE_NAME, "location")
    val address = event.detailValue(ATTR_BUSINESS_ADDRESS, "address")
    if (venueName == null && address == null) return

    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(
            eyebrow = "Venue",
            title = venueName ?: "Venue details"
        )
        address?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = CardTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
