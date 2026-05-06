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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.ATTR_AIRLINE_LOGO_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.firstNonBlank
import com.example.travelcents.ui.main.current.eventSubtitle
import com.example.travelcents.ui.main.current.eventTitle
import com.example.travelcents.ui.modules.PhotoGalleryButton
import com.example.travelcents.ui.modules.formatLongTripDateWithYear

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

    val compactTime = listOfNotNull(
        timeSummary.takeIf { it.isNotBlank() },
        durationSummary.takeIf { it.isNotBlank() }
    ).joinToString(" • ").ifBlank { "Time TBD" }

    DetailCardFrame(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryHeroMedia(
                heroImage = heroImage,
                title = title,
                accentGradient = accentGradientForType(event.type),
                photoCount = photoCount,
                onOpenGallery = onOpenGallery,
                modifier = Modifier
                    .weight(0.38f)
                    .aspectRatio(0.9f)
                    .clip(RoundedCornerShape(22.dp)),
                scrim = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CardBackground.copy(alpha = 0.18f),
                        CardBackground.copy(alpha = 0.72f)
                    )
                )
            )

            Column(
                modifier = Modifier.weight(0.62f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = CardText,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                CompactSummaryMetaRow(
                    label = "Date",
                    value = formatLongTripDateWithYear(event.date).ifBlank { "Date TBD" },
                    accent = accent
                )
                CompactSummaryMetaRow(
                    label = "Time",
                    value = compactTime,
                    accent = accent,
                    maxLines = 2
                )
                eventSubtitle(event)
                    .takeIf { it.isNotBlank() }
                    ?.let { subtitle ->
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
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
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
    val (titleFontSize, titleLineHeight) = compactSummaryTitleMetrics(title)

    DetailCardFrame(accent = CardLavender) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryHeroMedia(
                heroImage = heroImage,
                title = title,
                accentGradient = accentGradientForType(event.type),
                photoCount = photoCount,
                onOpenGallery = onOpenGallery,
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(24.dp)),
                showCountPill = false,
                scrim = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CardBackground.copy(alpha = 0.1f),
                        CardBackground.copy(alpha = 0.32f)
                    )
                )
            )

            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                CompactSummaryMetaRow(
                    label = "Date",
                    value = dateSummary,
                    accent = CardLavender
                )
                CompactSummaryMetaRow(
                    label = "Time",
                    value = timeSummary,
                    accent = CardLavender,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
internal fun CompactSummaryMetaRow(
    label: String,
    value: String,
    accent: Color,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(42.dp)
        )
        Text(
            text = value,
            color = accent,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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
            SummaryHeroMedia(
                heroImage = heroImage,
                title = title,
                accentGradient = accentGradientForType(event.type),
                photoCount = photoCount,
                onOpenGallery = onOpenGallery,
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(24.dp))
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
            SummaryHeroMedia(
                heroImage = heroImage,
                title = title,
                accentGradient = accentGradientForType("restaurant"),
                photoCount = photoCount,
                onOpenGallery = onOpenGallery,
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(24.dp))
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
internal fun HeroImageCountPill(
    photoCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardBackground.copy(alpha = 0.58f),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Text(
            text = "1/$photoCount",
            color = CardText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SummaryHeroMedia(
    heroImage: String?,
    title: String,
    accentGradient: Brush,
    photoCount: Int,
    onOpenGallery: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showCountPill: Boolean = true,
    scrim: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            CardBackground.copy(alpha = 0.12f),
            CardBackground.copy(alpha = 0.42f)
        )
    )
) {
    Box(
        modifier = modifier
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
                    .background(scrim)
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(accentGradient)
            )
        }

        if (showCountPill && photoCount > 0 && onOpenGallery != null) {
            HeroImageCountPill(
                photoCount = photoCount,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            )
        }
    }
}

private fun compactSummaryTitleMetrics(title: String): Pair<TextUnit, TextUnit> {
    return when {
        title.length >= 80 -> 18.sp to 22.sp
        title.length >= 54 -> 19.sp to 23.sp
        else -> 21.sp to 25.sp
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
    val airlineLogo = event.details.firstNonBlank(ATTR_AIRLINE_LOGO_URL, "airline_logo")
        ?.takeIf { it.isNotBlank() }

    DetailCardFrame(accent = CardSky) {
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
                                        Color.Transparent,
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

                EventTypeChip(
                    type = event.type,
                    accent = CardSky,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )

                if (photoCount > 1 && onOpenGallery != null) {
                    PhotoGalleryButton(
                        photoCount = photoCount,
                        onClick = onOpenGallery,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }

                if (airlineLogo != null) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .size(36.dp)
                    ) {
                        AsyncImage(
                            model = airlineLogo,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
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

                if (summary != null) {
                    Text(
                        text = "${summary.originCode} → ${summary.destinationCode}",
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
