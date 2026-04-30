package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_STOP_AIRPORTS
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun FlightRouteCard(event: TravelEvent) {
    val origin = event.detailValue("origin_airport") ?: "—"
    val destination = event.detailValue("destination_airport") ?: "—"
    val stopCount = event.detailValue("stops")?.toIntOrNull() ?: 0
    val stopAirports = event.detailValue(ATTR_STOP_AIRPORTS)
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?: emptyList()
    val stopsLabel = when {
        stopCount == 0 -> "Nonstop"
        stopCount == 1 && stopAirports.isNotEmpty() -> "1 stop · ${stopAirports.first()}"
        stopAirports.isNotEmpty() -> "$stopCount stops · ${stopAirports.joinToString(", ")}"
        stopCount == 1 -> "1 stop"
        else -> "$stopCount stops"
    }
    val durationMin = event.detailValue("flight_duration_min")?.toIntOrNull()
    val durationLabel = durationMin?.let { "${it / 60}h ${it % 60}m" }

    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(eyebrow = "Route", title = "$origin → $destination")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AirportBlock(modifier = Modifier.weight(1f), label = "From", airport = origin, alignEnd = false)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "→",
                    color = CardSky,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (durationLabel != null) {
                    Text(
                        text = durationLabel,
                        color = CardTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            AirportBlock(modifier = Modifier.weight(1f), label = "To", airport = destination, alignEnd = true)
        }
        Spacer(modifier = Modifier.height(10.dp))
        DetailBadgeRow(
            badges = listOfNotNull(
                event.detailValue("airline"),
                event.detailValue("flight_number"),
                event.detailValue("cabin_class") ?: "Economy",
                stopsLabel
            ),
            accent = CardSky
        )
    }
}

@Composable
private fun AirportBlock(
    modifier: Modifier = Modifier,
    label: String,
    airport: String,
    alignEnd: Boolean
) {
    val align = if (alignEnd) TextAlign.End else TextAlign.Start
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = CardTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = airport,
            color = CardText,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}