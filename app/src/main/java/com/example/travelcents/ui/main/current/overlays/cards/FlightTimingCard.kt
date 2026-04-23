package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.modules.formatDisplayTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FlightTimingCard(event: TravelEvent) {
    val departureDate = event.date.takeIf { it.isNotBlank() } ?: "Date TBD"
    val arrivalDate = event.details["arrival_date"]?.takeIf { it.isNotBlank() } ?: departureDate
    val tzAbbr = timezoneAbbr(event.tz)
    DetailCardFrame(accent = CardSky) {
        Text(
            text = "TIMING",
            color = CardTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FlightTimeCell(
                modifier = Modifier.weight(1f),
                label = "Depart",
                time = formatDisplayTime(event.startTime),
                tzAbbr = tzAbbr,
                date = departureDate,
                alignEnd = false
            )
            FlightTimeCell(
                modifier = Modifier.weight(1f),
                label = "Arrive",
                time = formatDisplayTime(event.endTime),
                tzAbbr = tzAbbr,
                date = arrivalDate,
                alignEnd = true
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        DetailBadgeRow(
            badges = listOf(
                event.details["trip_type"] ?: "",
                event.details["stops"]?.takeIf { it.isNotBlank() }?.let { "$it stops" } ?: ""
            ),
            accent = CardSky
        )
    }
}

@Composable
private fun FlightTimeCell(
    modifier: Modifier,
    label: String,
    time: String,
    tzAbbr: String?,
    date: String,
    alignEnd: Boolean
) {
    val align = if (alignEnd) TextAlign.End else TextAlign.Start
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = CardTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = time,
            color = CardText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        if (tzAbbr != null) {
            Text(
                text = tzAbbr,
                color = CardTextMuted.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = date,
            color = CardTextMuted,
            fontSize = 11.sp,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun timezoneAbbr(tz: String): String? {
    if (tz.isBlank()) return null
    return try {
        val zoneId = ZoneId.of(tz)
        ZonedDateTime.now(zoneId)
            .format(DateTimeFormatter.ofPattern("zzz", Locale.US))
    } catch (e: Exception) {
        tz.take(3).uppercase(Locale.US).ifBlank { null }
    }
}
