package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.ATTR_AIRLINE_LOGO_URL
import com.example.travelcents.data.trip.model.ATTR_ARRIVAL_DAY_OFFSET
import com.example.travelcents.data.trip.model.ATTR_DESTINATION_TZ
import com.example.travelcents.data.trip.model.ATTR_ORIGIN_TZ
import com.example.travelcents.data.trip.model.ATTR_STOP_AIRPORTS
import com.example.travelcents.data.trip.model.ATTR_WEBSITE_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.firstNonBlank
import com.example.travelcents.ui.modules.formatDisplayTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class FlightSummaryModel(
    val originCode: String,
    val originCity: String?,
    val destinationCode: String,
    val destinationCity: String?,
    val arrivalTimeLabel: String,
    val arrivalZoneLabel: String?,
    val arrivalOffsetLabel: String?,
    val departureTimeLabel: String,
    val departureZoneLabel: String?,
    val stopsLabel: String,
    val stopCount: Int,
    val airlineLabel: String,
    val airlineName: String?,
    val flightNumber: String?,
    val airlineLogoUrl: String?,
    val bookingUrl: String?
)

internal fun TravelEvent.toFlightSummaryModel(): FlightSummaryModel? {
    if (!type.equals("flight", ignoreCase = true)) return null

    val originAirport = details["origin_airport"].orEmpty().ifBlank { "—" }
    val destinationAirport = details["destination_airport"].orEmpty().ifBlank { "—" }
    val originCity = details["origin_city"]?.takeIf { it.isNotBlank() }
    val destinationCity = details["destination_city"]?.takeIf { it.isNotBlank() }
    
    val arrivalTime = details["arrival_time"]
        ?.takeIf { it.isNotBlank() }
        ?: endTime.takeIf { it.isNotBlank() }
        ?: "Time TBD"
    val departureTime = startTime.takeIf { it.isNotBlank() }
        ?: details["departure_time"]?.substringAfterLast(" ")?.takeIf { it.isNotBlank() }
        ?: "Time TBD"
    val departureTz = timezoneAbbreviation(details[ATTR_ORIGIN_TZ].orEmpty())
    val arrivalTz = timezoneAbbreviation(details[ATTR_DESTINATION_TZ].orEmpty().ifBlank { tz })
    val dayOffset = details[ATTR_ARRIVAL_DAY_OFFSET]?.toIntOrNull() ?: 0
    val stopCount = details["stops"]?.toIntOrNull() ?: 0
    val stopAirports = details[ATTR_STOP_AIRPORTS]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    val stopsLabel = when {
        stopCount <= 0 -> "Nonstop"
        stopCount == 1 && stopAirports.isNotEmpty() -> "1 stop • ${stopAirports.first()}"
        stopAirports.isNotEmpty() -> "$stopCount stops • ${stopAirports.joinToString(", ")}"
        stopCount == 1 -> "1 stop"
        else -> "$stopCount stops"
    }
    
    val airlineName = details["airline"]?.takeIf { it.isNotBlank() }
    val flightNumber = details["flight_number"]?.takeIf { it.isNotBlank() }
    val airlineBits = listOfNotNull(airlineName, flightNumber)

    return FlightSummaryModel(
        originCode = originAirport,
        originCity = originCity,
        destinationCode = destinationAirport,
        destinationCity = destinationCity,
        arrivalTimeLabel = formatDisplayTime(arrivalTime),
        arrivalZoneLabel = arrivalTz,
        arrivalOffsetLabel = when {
            dayOffset > 0 -> "+${dayOffset}d"
            dayOffset < 0 -> "${dayOffset}d"
            else -> null
        },
        departureTimeLabel = formatDisplayTime(departureTime),
        departureZoneLabel = departureTz,
        stopsLabel = stopsLabel,
        stopCount = stopCount,
        airlineLabel = airlineBits.joinToString(" • ").ifBlank { "Flight" },
        airlineName = airlineName,
        flightNumber = flightNumber,
        airlineLogoUrl = details.firstNonBlank(ATTR_AIRLINE_LOGO_URL, "airline_logo")
            ?.takeIf { it.isNotBlank() },
        bookingUrl = details["booking_url"]?.takeIf { it.isNotBlank() }
    )
}

@Composable
internal fun FlightOverviewCard(event: TravelEvent) {
    val summary = event.toFlightSummaryModel() ?: return
    
    DetailCardFrame(accent = CardSky) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlightOverviewMoment(
                modifier = Modifier.weight(1f),
                label = "Departure",
                code = summary.originCode,
                city = summary.originCity,
                time = summary.departureTimeLabel,
                tz = summary.departureZoneLabel,
                offset = null
            )
            FlightOverviewMoment(
                modifier = Modifier.weight(1f),
                label = "Arrival",
                code = summary.destinationCode,
                city = summary.destinationCity,
                time = summary.arrivalTimeLabel,
                tz = summary.arrivalZoneLabel,
                offset = summary.arrivalOffsetLabel
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        DetailBadgeRow(
            badges = listOfNotNull(
                summary.airlineLabel,
                summary.stopsLabel,
                event.detailValue("cabin_class")
            ),
            accent = CardSky
        )
    }
}

@Composable
private fun FlightOverviewMoment(
    modifier: Modifier,
    label: String,
    code: String,
    city: String?,
    time: String,
    tz: String?,
    offset: String?
) {
    Column(
        modifier = modifier
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = CardTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = code,
                color = CardText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (city != null) {
                Text(
                    text = city,
                    color = CardTextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = time,
                color = CardSky,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (tz != null) {
                Text(
                    text = tz,
                    color = CardTextMuted.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            if (offset != null) {
                Text(
                    text = offset,
                    color = CardSky,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
internal fun FlightActionRow(
    event: TravelEvent,
    onTrack: () -> Unit,
    onBook: () -> Unit
) {
    val summary = event.toFlightSummaryModel() ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ItineraryActionButton(
            label = "Track",
            onClick = onTrack,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Search,
            accent = CardSky
        )
        if (!summary.bookingUrl.isNullOrBlank()) {
            ItineraryActionButton(
                label = "Book",
                onClick = onBook,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ConfirmationNumber,
                emphasis = ItineraryActionEmphasis.Secondary,
                accent = CardSky
            )
        } else {
            ItineraryActionButton(
                label = "Official Site",
                onClick = onBook,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Language,
                emphasis = ItineraryActionEmphasis.Secondary,
                accent = CardSky
            )
        }
    }
}

@Composable
internal fun FlightHeroMedia(
    heroImage: String?,
    title: String,
    airlineLogoUrl: String?,
    photoCount: Int,
    onOpenGallery: (() -> Unit)?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    showFallbackTypePill: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(CardSurfaceHighest)
            .then(if (onOpenGallery != null) Modifier.clickable(onClick = onOpenGallery) else Modifier)
    ) {
        if (!heroImage.isNullOrBlank()) {
            AsyncImage(
                model = heroImage,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CardBackground.copy(alpha = 0.16f),
                                CardBackground.copy(alpha = 0.56f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentGradientForType("flight"))
            )
        }

        if (photoCount > 1 && onOpenGallery != null) {
            HeroImageCountPill(
                photoCount = photoCount,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )
        }

        when {
            !airlineLogoUrl.isNullOrBlank() -> {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .size(36.dp)
                ) {
                    AsyncImage(
                        model = airlineLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            showFallbackTypePill -> {
                Surface(
                    color = CardBackground.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "FLIGHT",
                        color = CardSky,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CompactFlightSummaryContent(
    summary: FlightSummaryModel,
    accent: Color,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top row: Airline & Stops
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (!summary.airlineLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = summary.airlineLogoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    text = summary.airlineLabel,
                    color = primaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = summary.stopsLabel,
                color = secondaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FlightRouteRow(
            summary = summary,
            primaryText = primaryText,
            accent = accent,
            codeFontSize = 18.sp,
            graphicHeight = 20.dp
        )

        FlightTimingBlock(
            summary = summary,
            primaryText = primaryText,
            secondaryText = secondaryText,
            accent = accent
        )
    }
}

@Composable
private fun FlightRouteRow(
    summary: FlightSummaryModel,
    primaryText: Color,
    accent: Color,
    codeFontSize: androidx.compose.ui.unit.TextUnit,
    graphicHeight: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = summary.originCode,
            color = primaryText,
            fontSize = codeFontSize,
            fontWeight = FontWeight.ExtraBold
        )
        FlightRouteGraphic(
            accent = accent,
            stopCount = summary.stopCount,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .height(graphicHeight)
        )
        Text(
            text = summary.destinationCode,
            color = primaryText,
            fontSize = codeFontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
internal fun FlightRouteGraphic(
    accent: Color,
    stopCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = Offset(0f, size.height * 0.5f)
            val end = Offset(size.width, size.height * 0.5f)
            val control = Offset(size.width / 2f, size.height * 0.1f)
            
            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            }
            
            drawPath(
                path = path,
                color = accent.copy(alpha = 0.85f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            if (stopCount > 0) {
                drawCircle(
                    color = accent,
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width / 2f, size.height * 0.3f)
                )
            }

            // Standard arrowhead
            drawLine(
                color = accent.copy(alpha = 0.85f),
                start = end,
                end = Offset(end.x - 6.dp.toPx(), end.y - 4.dp.toPx()),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accent.copy(alpha = 0.85f),
                start = end,
                end = Offset(end.x - 6.dp.toPx(), end.y + 4.dp.toPx()),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun FlightTimingBlock(
    summary: FlightSummaryModel,
    primaryText: Color,
    secondaryText: Color,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Departure
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DEP",
                color = secondaryText.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = summary.departureTimeLabel,
                    color = primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                summary.departureZoneLabel?.let {
                    Text(
                        text = it,
                        color = secondaryText.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }

        // Arrival
        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
            Text(
                text = "ARR",
                color = secondaryText.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = summary.arrivalTimeLabel,
                    color = primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (summary.arrivalZoneLabel != null || summary.arrivalOffsetLabel != null) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        summary.arrivalZoneLabel?.let {
                            Text(
                                text = it,
                                color = secondaryText.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 1.dp),
                                maxLines = 1
                            )
                        }
                        summary.arrivalOffsetLabel?.let {
                            Text(
                                text = it,
                                color = accent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActivityActionRow(
    event: TravelEvent,
    onBook: () -> Unit
) {
    val ticketsUrl = event.details["tickets_url"]?.takeIf { it.isNotBlank() }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ticketsUrl != null) {
            ItineraryActionButton(
                label = "Tickets",
                onClick = onBook,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ConfirmationNumber,
                accent = CardMint
            )
            ItineraryActionButton(
                label = "Official Site",
                onClick = onBook,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Language,
                emphasis = ItineraryActionEmphasis.Secondary,
                accent = CardMint
            )
        } else {
            ItineraryActionButton(
                label = "Official Site",
                onClick = onBook,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Language,
                accent = CardMint
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun timezoneAbbreviation(tz: String): String? {
    if (tz.isBlank()) return null
    return try {
        val zoneId = ZoneId.of(tz)
        val now = ZonedDateTime.now(zoneId)
        val shortName = now.format(DateTimeFormatter.ofPattern("z", Locale.US))
        
        if (shortName.startsWith("GMT") && shortName.length > 5) {
            val offset = zoneId.rules.getOffset(now.toInstant()).id
            if (offset == "Z") "UTC" else offset.replace(":00", "")
        } else {
            shortName
        }
    } catch (_: Exception) {
        tz.take(3).uppercase(Locale.US).ifBlank { null }
    }
}
