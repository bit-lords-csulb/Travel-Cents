package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CLASS
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun HotelOverviewCard(
    event: TravelEvent,
    onOpenReviews: () -> Unit
) {
    val hotelClass = event.detailValue(ATTR_HOTEL_CLASS, "hotel_class")
    val classStars = hotelClass
        ?.substringBefore("-")
        ?.trim()
        ?.toIntOrNull()
        ?.coerceIn(1, 5)
    val googleRating = event.detailValue("attr_hotel_rating", "attr_average_rating", "rating")
    val reviewCount = event.detailValue(ATTR_REVIEW_COUNT, "review_count")
    if (classStars == null && googleRating == null && reviewCount == null) return

    DetailCardFrame(accent = CardLavender) {
        DetailCardHeader(
            eyebrow = "Overview",
            title = "Hotel details",
            trailing = {
                if (classStars != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < classStars) {
                                    Icons.Default.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                contentDescription = null,
                                tint = CardLavender,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        )
        if (googleRating != null || reviewCount != null) {
            Spacer(modifier = Modifier.size(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenReviews)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Google reviews",
                    color = CardTextMuted,
                    fontSize = 13.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(CardLavender.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = listOfNotNull(
                                googleRating?.let { "★$it" },
                                reviewCount?.let { "$it reviews" }
                            ).joinToString(" · "),
                            color = CardLavender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
