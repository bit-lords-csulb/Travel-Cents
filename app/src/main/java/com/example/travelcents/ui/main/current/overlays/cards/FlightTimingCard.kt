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

@Composable
fun FlightTimingCard(event: TravelEvent) {
    val departureDate = event.date.takeIf { it.isNotBlank() } ?: "Date TBD"
    val arrivalDate = event.details["arrival_date"]?.takeIf { it.isNotBlank() } ?: departureDate
    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(eyebrow = "Timing", title = "Departure and arrival")
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FlightTimeCell(
                modifier = Modifier.weight(1f),
                label = "Depart",
                time = formatDisplayTime(event.startTime),
                date = departureDate,
                alignEnd = false
            )
            FlightTimeCell(
                modifier = Modifier.weight(1f),
                label = "Arrive",
                time = formatDisplayTime(event.endTime),
                date = arrivalDate,
                alignEnd = true
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        DetailBadgeRow(
            badges = listOf(
                eventDurationSummary(event),
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
    date: String,
    alignEnd: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = CardTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = time,
            color = CardText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = date,
            color = CardTextMuted,
            fontSize = 12.sp,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
