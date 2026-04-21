package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.YelpReview
import java.util.Locale

@Composable
fun ReviewsCard(
    ratingLabel: String,
    reviewCountLabel: String,
    reviews: List<YelpReview>,
    reviewsLoading: Boolean,
    onReadAll: (() -> Unit)?
) {
    DetailCardFrame(accent = CardGold) {
        DetailCardHeader(
            eyebrow = "Reviews",
            title = if (reviewCountLabel.isBlank()) ratingLabel else "$ratingLabel average"
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOfNotNull(
                reviewCountLabel.takeIf { it.isNotBlank() }?.let { "$it reviews" },
                if (reviewsLoading) "Loading reviews" else null
            ),
            accent = CardGold
        )
        Spacer(modifier = Modifier.height(14.dp))
        when {
            reviewsLoading -> ReviewState("Loading guest reviews...")
            reviews.isEmpty() -> ReviewState("No guest reviews are available for this event yet.")
            else -> reviews.take(2).forEach { review ->
                ReviewSnippet(review)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        if (onReadAll != null) {
            DetailLinkRow(
                label = "Reviews",
                value = if (reviewCountLabel.isBlank()) "Read all reviews" else "Read all $reviewCountLabel reviews",
                onClick = onReadAll,
                accent = CardGold
            )
        }
    }
}

@Composable
private fun ReviewState(message: String) {
    Text(
        text = message,
        color = CardTextMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun ReviewSnippet(review: YelpReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurfaceHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!review.user?.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = review.user!!.imageUrl,
                        contentDescription = review.user!!.name,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CardGold.copy(alpha = 0.16f), CircleShape),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = review.user?.name?.take(1)?.uppercase(Locale.US) ?: "R",
                            color = CardGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(
                        text = review.user?.name ?: "Traveler",
                        color = CardText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = review.timeCreated.take(10),
                        color = CardTextMuted,
                        fontSize = 10.sp
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(review.rating.coerceIn(0, 5)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CardGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Text(
            text = review.text,
            color = CardTextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
