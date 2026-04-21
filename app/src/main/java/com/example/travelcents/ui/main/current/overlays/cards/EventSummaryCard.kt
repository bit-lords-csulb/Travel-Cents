package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.firstNonBlank
import com.example.travelcents.ui.main.current.eventSubtitle
import com.example.travelcents.ui.main.current.eventTitle
import com.example.travelcents.ui.modules.PhotoGalleryButton
import java.util.Locale

@Composable
fun EventSummaryCard(
    event: TravelEvent,
    heroImage: String?,
    photoCount: Int,
    timeSummary: String,
    durationSummary: String,
    onOpenGallery: (() -> Unit)? = null
) {
    val accent = accentForType(event.type)
    val title = eventTitle(event)
    if (event.type.equals("hotel", ignoreCase = true)) {
        HotelSummaryCard(
            event = event,
            heroImage = heroImage,
            photoCount = photoCount,
            title = title,
            onOpenGallery = onOpenGallery
        )
        return
    }

    DetailCardFrame(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.38f)
                    .aspectRatio(0.9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardSurfaceHighest)
                    .then(if (onOpenGallery != null) Modifier.clickable(onClick = onOpenGallery) else Modifier)
            ) {
                if (!heroImage.isNullOrBlank()) {
                    AsyncImage(
                        model = heroImage,
                        contentDescription = title,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        CardBackground.copy(alpha = 0.18f),
                                        CardBackground.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(accentGradientForType(event.type))
                    )
                }

                if (photoCount > 1 && onOpenGallery != null) {
                    PhotoGalleryButton(
                        photoCount = photoCount,
                        onClick = onOpenGallery,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }

                Surface(
                    color = CardBackground.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = event.type.uppercase(Locale.US),
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.62f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = CardText,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                DetailBadgeRow(
                    badges = listOf(timeSummary, durationSummary),
                    accent = accent
                )
                if (!event.type.equals("flight", ignoreCase = true)) {
                    val subtitle = eventSubtitle(event)
                    if (subtitle.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accent)
                            )
                            Text(
                                text = subtitle,
                                color = CardTextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotelSummaryCard(
    event: TravelEvent,
    heroImage: String?,
    photoCount: Int,
    title: String,
    onOpenGallery: (() -> Unit)?
) {
    val subtitle = event.details.firstNonBlank(ATTR_BUSINESS_ADDRESS, "address")
        ?.takeIf { it.isNotBlank() }

    DetailCardFrame(accent = CardLavender) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardSurfaceHighest)
                    .then(if (onOpenGallery != null) Modifier.clickable(onClick = onOpenGallery) else Modifier)
            ) {
                if (!heroImage.isNullOrBlank()) {
                    AsyncImage(
                        model = heroImage,
                        contentDescription = title,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        CardBackground.copy(alpha = 0.12f),
                                        CardBackground.copy(alpha = 0.42f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(accentGradientForType(event.type))
                    )
                }

                if (photoCount > 1 && onOpenGallery != null) {
                    PhotoGalleryButton(
                        photoCount = photoCount,
                        onClick = onOpenGallery,
                        containerAlpha = 0.28f,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    color = CardText,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = CardTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
