package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_CUISINE
import com.example.travelcents.data.trip.model.ATTR_PRICE_LEVEL_USD
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

@Composable
fun RestaurantOverviewCard(
    event: TravelEvent,
    onOpenReviews: (() -> Unit)? = null
) {
    val rating = event.detailValue(ATTR_AVERAGE_RATING, "rating")?.takeIf { it.isNotBlank() }
    val reviewCount = event.detailValue(ATTR_REVIEW_COUNT, "review_count")?.takeIf { it.isNotBlank() }
    val badges = restaurantOverviewBadges(event)
    if (rating == null && reviewCount == null && badges.isEmpty()) return

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Overview",
            title = "Restaurant details"
        )

        if (rating != null || reviewCount != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
                    .then(
                        if (onOpenReviews != null) {
                            Modifier.clickable(onClick = onOpenReviews)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (onOpenReviews != null) "Yelp reviews" else "Reviews",
                    color = CardTextMuted,
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .background(CardCoral.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = listOfNotNull(
                            rating?.let { "★$it" },
                            reviewCount?.let { "$it reviews" }
                        ).joinToString(" · "),
                        color = CardCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            DetailBadgeRow(
                badges = badges,
                accent = CardCoral
            )
        }
    }
}

private fun restaurantOverviewBadges(event: TravelEvent): List<String> {
    val badges = mutableListOf<String>()
    val seen = linkedSetOf<String>()

    addRestaurantBadge(
        badges = badges,
        seen = seen,
        value = restaurantPriceLabel(event)
    )
    addRestaurantBadge(
        badges = badges,
        seen = seen,
        value = event.detailValue(ATTR_CUISINE, "cuisine")
    )
    event.detailValue(ATTR_CATEGORIES, "categories")
        ?.split(",")
        .orEmpty()
        .forEach { category ->
            addRestaurantBadge(
                badges = badges,
                seen = seen,
                value = category
            )
        }
    return badges
}

private fun addRestaurantBadge(
    badges: MutableList<String>,
    seen: MutableSet<String>,
    value: String?
) {
    val label = value?.trim()?.takeIf { it.isNotBlank() } ?: return
    val normalized = label.lowercase(Locale.US)
    if (seen.add(normalized)) {
        badges += label
    }
}

private fun restaurantPriceLabel(event: TravelEvent): String? {
    event.detailValue(ATTR_PRICE_TIER, "price_tier")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    val priceLevel = event.detailValue(ATTR_PRICE_LEVEL_USD)
        ?.trim()
        ?.toIntOrNull()
        ?.coerceIn(1, 4)
        ?: return null
    return "$".repeat(priceLevel)
}
