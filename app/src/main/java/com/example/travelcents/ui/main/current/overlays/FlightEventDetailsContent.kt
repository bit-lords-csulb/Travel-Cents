package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.modules.formatDisplayTime

private val FlightSurfaceLow = Color(0xFF131B2E)
private val FlightSurfaceHighest = Color(0xFF2D3449)
private val FlightOnSurface = Color(0xFFDAE2FD)
private val FlightOnSurfaceVariant = Color(0xFFCAC4D4)
private val FlightPrimary = Color(0xFFCEBDFF)
private val FlightOutline = Color(0xFF494552)

private fun formatFlightDuration(minStr: String?): String? {
    val min = minStr?.toIntOrNull() ?: return null
    if (min <= 0) return null
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

// Route card showing FROM/TO airport codes and departure/arrival times
@Composable
fun FlightRouteCard(event: TravelEvent) {
    val d = event.details
    val origin = d["origin_airport"] ?: "—"
    val destination = d["destination_airport"] ?: "—"
    val departure = formatDisplayTime(event.startTime).ifBlank { d["departure_time"] ?: "—" }
    val arrivalRaw = d["arrival_time"] ?: event.endTime.ifBlank { "" }
    val arrival = formatDisplayTime(arrivalRaw).ifBlank { arrivalRaw.ifBlank { "—" } }
    val departureDate = event.date.takeIf { it.isNotBlank() }
    val arrivalDate = d["arrival_date"]?.takeIf { it.isNotBlank() && it != event.date }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FlightSurfaceLow)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FROM",
                    color = FlightOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = origin,
                    color = FlightOnSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "→",
                color = FlightPrimary,
                fontSize = 28.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "TO",
                    color = FlightOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = destination,
                    color = FlightOnSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }

        HorizontalDivider(color = FlightOutline.copy(alpha = 0.4f))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DEPARTS",
                    color = FlightOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = departure,
                    color = FlightPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (departureDate != null) {
                    Text(
                        text = departureDate,
                        color = FlightOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "ARRIVES",
                    color = FlightOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = arrival,
                    color = FlightPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                if (arrivalDate != null) {
                    Text(
                        text = arrivalDate,
                        color = FlightOnSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// Info grid showing airline, duration, and price highlight cards
@Composable
fun FlightInfoGrid(event: TravelEvent) {
    val d = event.details
    val airline = d["airline"] ?: "—"
    val flightNum = d["flight_number"] ?: "—"
    val duration = formatFlightDuration(d["flight_duration_min"] ?: d["total_duration"] ?: d["duration"])
    val cabin = d["cabin_class"] ?: "Economy"
    val price = d["total_price"]?.let { "$$it" }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlightHighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FlightTakeoff,
                    eyebrow = "Airline",
                    title = airline,
                    supporting = "Flight $flightNum"
                )
                FlightHighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    eyebrow = "Duration",
                    title = duration ?: "—",
                    supporting = cabin
                )
            }
            if (price != null) {
                FlightHighlightCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.AttachMoney,
                    eyebrow = "Total Price",
                    title = price,
                    supporting = "Round-trip"
                )
            }
        }
    }
}

@Composable
private fun FlightHighlightCard(
    modifier: Modifier,
    icon: ImageVector,
    eyebrow: String,
    title: String,
    supporting: String
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FlightSurfaceLow)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(FlightSurfaceHighest)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlightPrimary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = eyebrow.uppercase(),
                color = FlightOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = title,
                color = FlightOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supporting,
                color = FlightOnSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}