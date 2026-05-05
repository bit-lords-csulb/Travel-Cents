package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.firstNonBlank
import com.example.travelcents.data.trip.model.detailValue
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
    val isTicketmasterBacked = !event.detailValue(ATTR_TICKETMASTER_EVENT_ID).isNullOrBlank()
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
    if (isTicketmasterBacked) {
        TicketmasterSummaryCard(
            event = event,
            heroImage = heroImage,
            photoCount = photoCount,
            title = title,
            onOpenGallery = onOpenGallery
        )
        return
    }
    if (event.type.equals("restaurant", ignoreCase = true) ||
        event.type.equals("dining", ignoreCase = true) ||
        event.type.equals("food", ignoreCase = true)
    ) {
        RestaurantSummaryVariant(
            event = event,
            heroImage = heroImage,
            photoCount = photoCount,
            title = title,
            onOpenGallery = onOpenGallery
        )
        return
    }
    if (event.type.equals("flight", ignoreCase = true)) {
        FlightSummaryVariant(
            event = event,
            heroImage = heroImage,
            photoCount = photoCount,
            title = title,
            timeSummary = timeSummary,
            durationSummary = durationSummary,
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
private fun TicketmasterSummaryCard(
    event: TravelEvent,
    heroImage: String?,
    photoCount: Int,
    title: String,
    onOpenGallery: (() -> Unit)?
) {
    val dateSummary = ticketmasterEventDateSummary(event) ?: event.date.ifBlank { "Date TBD" }
    val timeSummary = ticketmasterEventTimeSummary(event)
    val titleFontSize = when {
        title.length >= 80 -> 20.sp
        title.length >= 54 -> 22.sp
        else -> 25.sp
    }
    val titleLineHeight = when {
        title.length >= 80 -> 24.sp
        title.length >= 54 -> 27.sp
        else -> 29.sp
    }

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
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                TicketmasterSummaryMetaRow(label = "Date", value = dateSummary)
                TicketmasterSummaryMetaRow(label = "Time", value = timeSummary)
            }
        }
    }
}

@Composable
private fun TicketmasterSummaryMetaRow(
    label: String,
    value: String
) {
    Text(
        text = "$label: $value",
        color = CardText,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
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
                        text = "HOTEL",
                        color = CardLavender,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = CardTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantSummaryVariant(
    event: TravelEvent,
    heroImage: String?,
    photoCount: Int,
    title: String,
    onOpenGallery: (() -> Unit)?
) {
    val cuisine = event.details.firstNonBlank("cuisine", "category")
        ?.takeIf { it.isNotBlank() }
    val priceLevel = event.details["price_level"]?.takeIf { it.isNotBlank() }
    val rating = event.details["rating"]?.takeIf { it.isNotBlank() }
    val subtitle = listOfNotNull(cuisine, priceLevel, rating?.let { "★$it" }).joinToString(" • ")

    DetailCardFrame(accent = CardCoral) {
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
                            .background(accentGradientForType("restaurant"))
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
                        text = "RESTAURANT",
                        color = CardCoral,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = CardTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightSummaryVariant(
    event: TravelEvent,
    heroImage: String?,
    photoCount: Int,
    title: String,
    timeSummary: String,
    durationSummary: String,
    onOpenGallery: (() -> Unit)?
) {
    val summary = event.toFlightSummaryModel()

    DetailCardFrame(accent = CardSky) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlightHeroMedia(
                heroImage = heroImage,
                title = title,
                airlineLogoUrl = summary?.airlineLogoUrl,
                photoCount = photoCount,
                onOpenGallery = onOpenGallery,
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp)
            )

            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    color = CardText,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                if (summary != null) {
                    Text(
                        text = "${summary.originCode} \u2192 ${summary.destinationCode}",
                        color = CardSky,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DetailBadgeRow(
                    badges = listOfNotNull(
                        timeSummary.takeIf { it.isNotBlank() },
                        durationSummary.takeIf { it.isNotBlank() }
                    ),
                    accent = CardSky
                )
            }
        }
    }
}
