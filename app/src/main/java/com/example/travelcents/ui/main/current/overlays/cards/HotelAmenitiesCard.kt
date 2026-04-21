package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_AMENITIES
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun HotelAmenitiesCard(event: TravelEvent) {
    val amenities = event.detailValue(ATTR_AMENITIES, "amenities")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    if (amenities.isEmpty()) return

    DetailCardFrame(accent = CardLavender) {
        DetailCardHeader(eyebrow = "Amenities", title = "What this stay includes")
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = amenities,
            accent = CardLavender
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Amenities come from the selected hotel option and stay synced with hotel switches.",
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
