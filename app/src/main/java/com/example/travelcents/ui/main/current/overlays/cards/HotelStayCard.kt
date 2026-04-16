package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CLASS
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.ui.main.current.eventTitle

@Composable
fun HotelStayCard(event: TravelEvent) {
    DetailCardFrame(accent = CardLavender) {
        DetailCardHeader(
            eyebrow = "Stay",
            title = eventTitle(event)
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOf(
                hotelStayWindow(event),
                event.detailValue(ATTR_HOTEL_CLASS, "hotel_class")?.let { "$it-star hotel" } ?: "",
                event.detailValue("attr_hotel_rating", "attr_average_rating", "rating")?.let { "★$it" } ?: ""
            ),
            accent = CardLavender
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = eventLocationLabel(event),
            color = CardTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}
