package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_FREQUENT_FLYER_NUMBER
import com.example.travelcents.data.trip.model.ATTR_LOYALTY_PROGRAM
import com.example.travelcents.data.trip.model.ATTR_POINTS_EARNED
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LoyaltyCard(event: TravelEvent) {
    val program = event.detailValue(ATTR_LOYALTY_PROGRAM)?.takeIf { it.isNotBlank() }
    val points = event.detailValue(ATTR_POINTS_EARNED)?.toIntOrNull()?.takeIf { it > 0 }
    val ffNumber = event.detailValue(ATTR_FREQUENT_FLYER_NUMBER)?.takeIf { it.isNotBlank() }

    if (program == null && points == null) return

    val title = points
        ?.let { "Earn ~${NumberFormat.getInstance(Locale.US).format(it)} miles" }
        ?: "Loyalty program linked"

    DetailCardFrame(accent = CardGold) {
        DetailCardHeader(eyebrow = "Points", title = title)
        val badges = listOfNotNull(program, ffNumber?.let { "FF# $it" })
        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            DetailBadgeRow(badges = badges, accent = CardGold)
        }
        if (program == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Link a program in settings to track these miles.",
                color = CardTextMuted,
                fontSize = 12.sp,
                modifier = Modifier
            )
        }
    }
}
