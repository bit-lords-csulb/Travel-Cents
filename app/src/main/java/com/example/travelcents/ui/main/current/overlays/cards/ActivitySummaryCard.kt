package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.ui.main.current.eventTitle

@Composable
fun ActivitySummaryCard(event: TravelEvent) {
    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(eyebrow = "Summary", title = eventTitle(event))
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOf(
                event.detailValue(ATTR_CATEGORIES, "categories") ?: "",
                event.detailValue(ATTR_PRICE_TIER, "price_tier", "cost") ?: "",
                event.detailValue(ATTR_AVERAGE_RATING, "rating")?.let { "★$it" } ?: ""
            ),
            accent = CardMint
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = event.detailValue(ATTR_BUSINESS_ADDRESS, "address")
                ?: eventExperienceText(event),
            color = CardTextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
