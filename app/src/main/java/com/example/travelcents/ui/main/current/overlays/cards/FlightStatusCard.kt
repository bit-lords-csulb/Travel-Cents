package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_ORIGIN_TZ
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.FlightStatusRepository
import java.time.Instant
import java.util.Locale

@Composable
fun FlightStatusCard(event: TravelEvent) {
    val flightNumber = event.detailValue("flight_number").orEmpty()
    val originIata = event.detailValue("origin_airport").orEmpty()
    val originTz = event.detailValue(ATTR_ORIGIN_TZ).orEmpty().ifBlank { event.tz }
    val scheduledLocal = remember(event.date, event.startTime) {
        val date = event.date.takeIf { it.isNotBlank() } ?: return@remember ""
        val time = event.startTime.takeIf { it.isNotBlank() } ?: return@remember ""
        "$date $time"
    }
    if (flightNumber.isBlank() || originIata.isBlank() || scheduledLocal.isBlank()) return

    var snapshot by remember(flightNumber, scheduledLocal) {
        mutableStateOf<FlightStatusRepository.Snapshot?>(null)
    }
    var settled by remember(flightNumber, scheduledLocal) { mutableStateOf(false) }

    LaunchedEffect(flightNumber, scheduledLocal, originIata, originTz) {
        snapshot = FlightStatusRepository.fetchStatus(
            flightNumber = flightNumber,
            scheduledDepartureLocal = scheduledLocal,
            originIata = originIata,
            scheduledZone = originTz
        )
        settled = true
    }

    if (!settled || snapshot == null) return
    val s = snapshot ?: return

    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(
            eyebrow = "Live",
            title = statusLabel(s)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val grid = buildList {
            s.delayMinutes?.takeIf { it > 0 }?.let { add("Delay" to "${it} min") }
            s.callsign?.let { add("Callsign" to it) }
            s.icao24?.let { add("Aircraft" to it.uppercase(Locale.US)) }
            if (s.latitude != null && s.longitude != null) {
                add(
                    "Position" to String.format(
                        Locale.US,
                        "%.2f, %.2f",
                        s.latitude,
                        s.longitude
                    )
                )
            }
            s.onGround?.let { add("Phase" to if (it) "On ground" else "In air") }
        }

        if (grid.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                grid.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { (label, value) ->
                            StatusCell(
                                modifier = Modifier.background(
                                    CardSky.copy(alpha = 0.10f),
                                    RoundedCornerShape(12.dp)
                                ),
                                label = label,
                                value = value
                            )
                        }
                        if (row.size == 1) Box(modifier = Modifier.background(androidx.compose.ui.graphics.Color.Transparent)) {}
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        val lastUpdate = relativeAgo(s.updatedAtUnix)
        if (lastUpdate != null) {
            Text(
                text = "Updated $lastUpdate",
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusCell(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(Locale.US),
            color = CardTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Text(
            text = value,
            color = CardText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun statusLabel(s: FlightStatusRepository.Snapshot): String {
    return when (s.status) {
        FlightStatusRepository.Status.SCHEDULED -> {
            val delay = s.delayMinutes ?: 0
            if (delay > 0) "Scheduled · delayed $delay min" else "On time"
        }
        FlightStatusRepository.Status.IN_AIR -> "In air"
        FlightStatusRepository.Status.LANDED -> "Landed"
        FlightStatusRepository.Status.ON_GROUND_AT_ORIGIN -> "On ground"
        FlightStatusRepository.Status.UNKNOWN -> "Status unavailable"
    }
}

private fun relativeAgo(epochSeconds: Long): String? {
    if (epochSeconds <= 0) return null
    val deltaSec = Instant.now().epochSecond - epochSeconds
    return when {
        deltaSec < 60 -> "just now"
        deltaSec < 3600 -> "${deltaSec / 60} min ago"
        deltaSec < 86400 -> "${deltaSec / 3600} h ago"
        else -> null
    }
}
