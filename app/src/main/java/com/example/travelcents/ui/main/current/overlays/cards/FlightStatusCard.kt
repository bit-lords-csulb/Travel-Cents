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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_ORIGIN_TZ
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.FlightStatusRepository
import com.example.travelcents.data.trip.remote.FlightStatusRepository.Endpoint
import com.example.travelcents.data.trip.remote.FlightStatusRepository.LivePosition
import com.example.travelcents.data.trip.remote.FlightStatusRepository.Snapshot
import com.example.travelcents.data.trip.remote.FlightStatusRepository.Status
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    if (flightNumber.isBlank() || scheduledLocal.isBlank()) return

    var snapshot by remember(flightNumber, scheduledLocal) {
        mutableStateOf<Snapshot?>(null)
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

    val (label, badgeColor) = statusLabelAndColor(s)

    DetailCardFrame(accent = CardSky) {
        StatusHeader(snapshot = s, label = label, badgeColor = badgeColor)
        Spacer(modifier = Modifier.height(14.dp))

        EndpointSection(eyebrow = "Departure", endpoint = s.departure, isArrival = false)

        if (hasEndpointContent(s.arrival)) {
            SectionDivider()
            EndpointSection(eyebrow = "Arrival", endpoint = s.arrival, isArrival = true)
        }

        s.live?.takeIf { it.hasAnySignal && s.status == Status.ACTIVE }?.let { live ->
            SectionDivider()
            LiveSection(live = live)
        }

        if (hasAircraftContent(s)) {
            SectionDivider()
            AircraftSection(snapshot = s)
        }

        val updatedAgo = relativeAgo(s.updatedAtUnix)
        if (updatedAgo != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Updated $updatedAgo · AviationStack",
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusHeader(snapshot: Snapshot, label: String, badgeColor: Color) {
    val identityLine = listOfNotNull(
        snapshot.flightIata?.takeIf { it.isNotBlank() }
            ?: snapshot.flightNumber?.let { num ->
                snapshot.airlineIata?.let { iata -> "$iata $num" } ?: num
            },
        snapshot.airlineName
    ).joinToString(" · ")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LIVE",
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = CardText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (identityLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = identityLine,
                    color = CardTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        StatusBadge(label = badgeText(snapshot), color = badgeColor)
    }
}

@Composable
private fun EndpointSection(eyebrow: String, endpoint: Endpoint, isArrival: Boolean) {
    val airportLine = formatAirportLine(endpoint)
    val timeLine = formatTimeLine(endpoint)
    val delayLine = formatDelayLine(endpoint)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = eyebrow.uppercase(Locale.US),
            color = CardTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        if (airportLine.isNotBlank()) {
            Text(
                text = airportLine,
                color = CardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (timeLine.isNotBlank()) {
            Text(
                text = timeLine,
                color = CardText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (delayLine != null) {
            Text(
                text = delayLine.first,
                color = delayLine.second,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        val cells = buildList {
            endpoint.terminal?.let { add(if (isArrival) "Arr Terminal" to it else "Terminal" to it) }
            endpoint.gate?.let { add(if (isArrival) "Arr Gate" to it else "Gate" to it) }
            endpoint.baggageBelt?.let { add("Baggage Belt" to it) }
        }
        if (cells.isNotEmpty()) {
            CellGrid(cells = cells)
        }
    }
}

@Composable
private fun LiveSection(live: LivePosition) {
    val cells = buildList {
        live.altitudeMeters?.let {
            val feet = (it * 3.28084).roundToInt()
            add("Altitude" to "${formatThousands(feet)} ft")
        }
        live.speedKph?.let {
            val mph = (it * 0.621371).roundToInt()
            add("Ground Speed" to "$mph mph")
        }
        live.headingDeg?.let {
            add("Heading" to "${it.roundToInt()}° ${compassDirection(it)}")
        }
        if (live.latitude != null && live.longitude != null) {
            add(
                "Position" to String.format(
                    Locale.US, "%.2f, %.2f", live.latitude, live.longitude
                )
            )
        }
        live.verticalSpeedKph?.takeIf { kotlin.math.abs(it) > 0.5 }?.let {
            val fpm = ((it * 1000.0 / 60.0) * 3.28084).roundToInt()
            val sign = if (fpm >= 0) "+" else ""
            add("Vertical" to "$sign$fpm fpm")
        }
        live.isGround?.let { add("Phase" to if (it) "On ground" else "In air") }
    }
    if (cells.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "LIVE POSITION",
            color = CardTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        CellGrid(cells = cells)
    }
}

@Composable
private fun AircraftSection(snapshot: Snapshot) {
    val cells = buildList {
        snapshot.aircraftType?.let { add("Type" to it.uppercase(Locale.US)) }
        snapshot.aircraftRegistration?.let { add("Tail #" to it.uppercase(Locale.US)) }
        snapshot.aircraftIcao24?.let { add("ICAO24" to it.uppercase(Locale.US)) }
    }
    if (cells.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "AIRCRAFT",
            color = CardTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        CellGrid(cells = cells)
    }
}

@Composable
private fun CellGrid(cells: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    StatusCell(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                CardSky.copy(alpha = 0.10f),
                                RoundedCornerShape(12.dp)
                            ),
                        label = label,
                        value = value
                    )
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CardSky.copy(alpha = 0.18f))
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    if (label.isBlank()) return
    Text(
        text = label.uppercase(Locale.US),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun StatusCell(modifier: Modifier, label: String, value: String) {
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

private fun hasEndpointContent(endpoint: Endpoint): Boolean {
    return !endpoint.airportName.isNullOrBlank() ||
        !endpoint.iata.isNullOrBlank() ||
        !endpoint.terminal.isNullOrBlank() ||
        !endpoint.gate.isNullOrBlank() ||
        !endpoint.baggageBelt.isNullOrBlank() ||
        endpoint.scheduledUnix != null ||
        endpoint.estimatedUnix != null ||
        endpoint.actualUnix != null
}

private fun hasAircraftContent(snapshot: Snapshot): Boolean {
    return !snapshot.aircraftType.isNullOrBlank() ||
        !snapshot.aircraftRegistration.isNullOrBlank() ||
        !snapshot.aircraftIcao24.isNullOrBlank()
}

private fun formatAirportLine(endpoint: Endpoint): String {
    val iata = endpoint.iata?.uppercase(Locale.US)
    val name = endpoint.airportName
    return when {
        !iata.isNullOrBlank() && !name.isNullOrBlank() -> "$iata · $name"
        !iata.isNullOrBlank() -> iata
        !name.isNullOrBlank() -> name
        else -> ""
    }
}

private fun formatTimeLine(endpoint: Endpoint): String {
    val zone = endpoint.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
        ?: ZoneOffset.UTC
    val scheduled = endpoint.scheduledUnix?.let { formatTime(it, zone) }
    val actual = endpoint.actualUnix?.let { formatTime(it, zone) }
    val estimated = endpoint.estimatedUnix?.let { formatTime(it, zone) }
    val current = actual ?: estimated
    return when {
        scheduled != null && current != null && current != scheduled ->
            "$current  (sched $scheduled)"
        current != null -> current
        scheduled != null -> scheduled
        else -> ""
    }
}

private fun formatDelayLine(endpoint: Endpoint): Pair<String, Color>? {
    val delay = endpoint.delayMinutes ?: return null
    if (delay <= 0) return null
    val color = when {
        delay >= 60 -> Color(0xFFFF8E7A)
        delay >= 15 -> Color(0xFFF1CB77)
        else -> CardTextMuted
    }
    return "Delayed ${delay} min" to color
}

private fun formatTime(epochSeconds: Long, zone: ZoneId): String {
    return Instant.ofEpochSecond(epochSeconds)
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
}

private fun compassDirection(degrees: Double): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = ((degrees % 360 + 360) % 360 / 45.0).roundToInt() % 8
    return dirs[idx]
}

private fun formatThousands(value: Int): String {
    return String.format(Locale.US, "%,d", value)
}

private fun statusLabelAndColor(s: Snapshot): Pair<String, Color> {
    return when (s.status) {
        Status.SCHEDULED -> {
            val delay = s.departure.delayMinutes ?: 0
            if (delay > 0) "Delayed ${delay} min" to StatusYellow
            else "Scheduled · on time" to StatusGreen
        }
        Status.ACTIVE -> "In flight" to StatusBlue
        Status.LANDED -> "Landed" to StatusGreen
        Status.CANCELLED -> "Cancelled" to StatusRed
        Status.DIVERTED -> "Diverted" to StatusYellow
        Status.INCIDENT -> "Incident reported" to StatusRed
        Status.UNKNOWN -> "Status unavailable" to CardTextMuted
    }
}

private fun badgeText(s: Snapshot): String = when (s.status) {
    Status.SCHEDULED -> if ((s.departure.delayMinutes ?: 0) > 0) "Delayed" else "On time"
    Status.ACTIVE -> "Active"
    Status.LANDED -> "Landed"
    Status.CANCELLED -> "Cancelled"
    Status.DIVERTED -> "Diverted"
    Status.INCIDENT -> "Incident"
    Status.UNKNOWN -> ""
}

private val StatusGreen = Color(0xFF7BE0A1)
private val StatusYellow = Color(0xFFF1CB77)
private val StatusRed = Color(0xFFFF8E7A)
private val StatusBlue = Color(0xFF7BC5FF)

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