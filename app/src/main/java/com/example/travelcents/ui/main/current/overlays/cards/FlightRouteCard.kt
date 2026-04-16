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

@Composable
fun FlightRouteCard(event: TravelEvent) {
    val origin = event.details["origin_airport"] ?: "—"
    val destination = event.details["destination_airport"] ?: "—"
    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(eyebrow = "Route", title = "$origin to $destination")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AirportBlock(label = "From", airport = origin, alignEnd = false)
            Text(
                text = "→",
                color = CardSky,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            AirportBlock(label = "To", airport = destination, alignEnd = true)
        }
        Spacer(modifier = Modifier.height(14.dp))
        DetailBadgeRow(
            badges = listOf(
                event.details["airline"] ?: "",
                event.details["flight_number"] ?: "",
                event.details["cabin_class"] ?: "Economy"
            ),
            accent = CardSky
        )
    }
}

@Composable
private fun AirportBlock(
    label: String,
    airport: String,
    alignEnd: Boolean
) {
    Column {
        Text(
            text = label.uppercase(),
            color = CardTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = airport,
            color = CardText,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
