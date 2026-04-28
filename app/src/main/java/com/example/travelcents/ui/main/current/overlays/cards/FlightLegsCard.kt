package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.modules.formatDisplayTime
import java.util.Locale

@Composable
fun FlightLegsCard(event: TravelEvent) {
    val stops = event.details["stops"]?.toIntOrNull() ?: 0
    if (stops <= 0) return
    val legs = readLegs(event)
    if (legs.size < 2) return

    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(
            eyebrow = "Legs",
            title = "${legs.size} segments"
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            legs.forEachIndexed { index, leg ->
                LegRow(leg = leg, isLast = index == legs.lastIndex)
                if (index < legs.lastIndex) {
                    val layover = layoverMinutes(leg, legs[index + 1])
                    val nextFrom = legs[index + 1].fromIata
                    LayoverChip(layoverMin = layover, atIata = nextFrom)
                }
            }
        }
    }
}

@Composable
private fun LegRow(leg: LegData, isLast: Boolean) {
    val depTime = formatDisplayTime(leg.departureTime)
    val arrTime = formatDisplayTime(leg.arrivalTime)
    val durationLabel = formatDuration(leg.durationMinutes)
    val badges = buildList {
        if (leg.overnight) add("Overnight")
        if (leg.oftenDelayed) add("Often delayed")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CardSky)
            )
            if (!isLast) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(CardSky.copy(alpha = 0.35f))
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = listOfNotNull(
                    leg.airline.takeIf { it.isNotBlank() },
                    leg.flightNumber.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${leg.fromIata} $depTime  →  ${leg.toIata} $arrTime",
                color = CardTextMuted,
                fontSize = 12.sp
            )
            durationLabel?.let {
                Text(
                    text = it,
                    color = CardTextMuted.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            if (badges.isNotEmpty()) {
                DetailBadgeRow(badges = badges, accent = CardSky)
            }
        }
    }
}

@Composable
private fun LayoverChip(layoverMin: Long?, atIata: String) {
    val label = when {
        layoverMin == null || layoverMin <= 0 -> "Layover · $atIata"
        else -> "${formatDuration(layoverMin)} layover · $atIata"
    }
    Row(
        modifier = Modifier.padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CardSky,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(CardSky.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private data class LegData(
    val airline: String,
    val flightNumber: String,
    val fromIata: String,
    val toIata: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Long?,
    val overnight: Boolean,
    val oftenDelayed: Boolean
) {
    val arrivalEpochOrNull: Long? = parseEpoch(arrivalTime)
    val departureEpochOrNull: Long? = parseEpoch(departureTime)
}

private fun readLegs(event: TravelEvent): List<LegData> {
    val legs = mutableListOf<LegData>()
    var i = 0
    while (true) {
        val from = event.details["leg_${i}_from"]
        val to = event.details["leg_${i}_to"]
        if (from.isNullOrBlank() || to.isNullOrBlank()) break
        legs += LegData(
            airline = event.details["leg_${i}_airline"].orEmpty(),
            flightNumber = event.details["leg_${i}_flight_number"].orEmpty(),
            fromIata = from,
            toIata = to,
            departureTime = event.details["leg_${i}_departure"].orEmpty(),
            arrivalTime = event.details["leg_${i}_arrival"].orEmpty(),
            durationMinutes = event.details["leg_${i}_duration_min"]?.toLongOrNull(),
            overnight = event.details["leg_${i}_overnight"] == "true",
            oftenDelayed = event.details["leg_${i}_often_delayed"] == "true"
        )
        i++
    }
    return legs
}

private fun layoverMinutes(prevLeg: LegData, nextLeg: LegData): Long? {
    val arrival = prevLeg.arrivalEpochOrNull ?: return null
    val departure = nextLeg.departureEpochOrNull ?: return null
    val delta = (departure - arrival) / 60
    return delta.takeIf { it > 0 }
}

private fun parseEpoch(timestamp: String): Long? {
    if (timestamp.isBlank()) return null
    return runCatching {
        java.time.LocalDateTime
            .parse(timestamp.trim(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            .toEpochSecond(java.time.ZoneOffset.UTC)
    }.getOrNull()
}

private fun formatDuration(minutes: Long?): String? {
    val m = minutes ?: return null
    if (m <= 0) return null
    val h = m / 60
    val r = m % 60
    return when {
        h > 0 && r > 0 -> String.format(Locale.US, "%dh %dm", h, r)
        h > 0 -> String.format(Locale.US, "%dh", h)
        else -> String.format(Locale.US, "%dm", r)
    }
}
