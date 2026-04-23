package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_BIKE_SCORE
import com.example.travelcents.data.trip.model.ATTR_NEAR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_NEIGHBORHOOD_NOTE
import com.example.travelcents.data.trip.model.ATTR_TRANSIT_SCORE
import com.example.travelcents.data.trip.model.ATTR_WALK_SCORE
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun NeighborhoodCard(event: TravelEvent) {
    val walkScore = event.detailValue(ATTR_WALK_SCORE)?.toIntOrNull()
    val transitScore = event.detailValue(ATTR_TRANSIT_SCORE)?.toIntOrNull()
    val bikeScore = event.detailValue(ATTR_BIKE_SCORE)?.toIntOrNull()
    val nearCategories = event.detailValue(ATTR_NEAR_CATEGORIES)
        ?.split(',')
        .orEmpty()
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val note = event.detailValue(ATTR_NEIGHBORHOOD_NOTE)?.takeIf { it.isNotBlank() }

    if (walkScore == null && transitScore == null && bikeScore == null && nearCategories.isEmpty() && note == null) {
        return
    }

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Neighborhood",
            title = neighborhoodTitle(walkScore, transitScore)
        )

        val scoreBadges = listOfNotNull(
            walkScore?.let { "Walk $it" },
            transitScore?.let { "Transit $it" },
            bikeScore?.let { "Bike $it" }
        )
        if (scoreBadges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(
                badges = scoreBadges,
                accent = CardCoral
            )
        }

        if (nearCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "NEARBY",
                color = CardTextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailBadgeRow(
                badges = nearCategories,
                accent = CardCoral
            )
        }

        note?.let {
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

private fun neighborhoodTitle(
    walkScore: Int?,
    transitScore: Int?
): String {
    return when {
        (walkScore ?: 0) >= 90 -> "Very walkable around this stop"
        (transitScore ?: 0) >= 80 -> "Easy to reach without a car"
        walkScore != null || transitScore != null -> "What the area feels like"
        else -> "Around this stop"
    }
}
