package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.TravelEvent

@Composable
fun RestaurantHoursCard(event: TravelEvent) {
    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(eyebrow = "Hours", title = hoursSummary(event))
        isClosedLabel(event)?.let {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(badges = listOf(it), accent = CardCoral)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Use these hours as guidance before making changes or sharing the plan.",
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
