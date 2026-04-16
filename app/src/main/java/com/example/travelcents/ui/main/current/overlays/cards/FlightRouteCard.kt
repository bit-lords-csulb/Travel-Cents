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
import com.example.travelcents.data.trip.model.TravelEvent

@Composable
fun FlightRouteCard(event: TravelEvent) {
    val origin = event.details["origin_airport"] ?: "—"
    val destination = event.details["destination_airport"] ?: "—"
    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(eyebrow = "Route", title = "$origin to $destination")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AirportBlock(modifier = Modifier.weight(1f), label = "From", airport = origin, alignEnd = false)
            Text(
                text = "→",
                color = CardSky,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            AirportBlock(modifier = Modifier.weight(1f), label = "To", airport = destination, alignEnd = true)
        }
        Spacer(modifier = Modifier.height(10.dp))
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
