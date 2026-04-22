package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_HOURS_RAW
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RestaurantHoursCard(event: TravelEvent) {
    val rawHours = event.detailValue(ATTR_HOURS_RAW, "hours_raw")?.takeIf { it.isNotBlank() } ?: return
    val schedule = parseRestaurantHours(rawHours)
    if (schedule.isEmpty()) return

    val zoneId = restaurantZoneId(event)
    val now = ZonedDateTime.now(zoneId)
    val todayIndex = now.dayOfWeek.toYelpDayIndex()
    val rows = restaurantScheduleRows(schedule, todayIndex)
    val todayHours = rows.firstOrNull { it.isToday }?.hoursLabel ?: HOURS_CLOSED_LABEL

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Hours",
            title = if (todayHours == HOURS_CLOSED_LABEL) "Closed today" else "Today · $todayHours"
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOf(if (isRestaurantOpenNow(schedule, now)) "Open now" else "Closed"),
            accent = CardCoral
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (row.isToday) {
                                CardCoral.copy(alpha = 0.12f)
                            } else {
                                CardSurfaceHigh
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = row.dayLabel,
                        color = if (row.isToday) CardCoral else CardText,
                        fontSize = 13.sp,
                        fontWeight = if (row.isToday) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = row.hoursLabel,
                        color = if (row.hoursLabel == HOURS_CLOSED_LABEL) CardTextMuted else CardText,
                        fontSize = 13.sp,
                        fontWeight = if (row.isToday) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

private const val HOURS_CLOSED_LABEL = "Closed"

private val hoursFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private data class RestaurantHoursPeriod(
    val dayIndex: Int,
    val start: LocalTime,
    val end: LocalTime,
    val isOvernight: Boolean
)

private data class RestaurantHoursRow(
    val dayLabel: String,
    val hoursLabel: String,
    val isToday: Boolean
)

private fun parseRestaurantHours(rawHours: String): List<RestaurantHoursPeriod> {
    return rawHours.split("|")
        .mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size < 4) return@mapNotNull null

            val dayIndex = parts[0].toIntOrNull()?.takeIf { it in 0..6 } ?: return@mapNotNull null
            val start = parseHoursTime(parts[1]) ?: return@mapNotNull null
            val end = parseHoursTime(parts[2]) ?: return@mapNotNull null
            val isOvernight = parts[3].trim().toBooleanStrictOrNull() ?: false

            RestaurantHoursPeriod(
                dayIndex = dayIndex,
                start = start,
                end = end,
                isOvernight = isOvernight
            )
        }
        .sortedWith(compareBy(RestaurantHoursPeriod::dayIndex, RestaurantHoursPeriod::start))
}

private fun parseHoursTime(raw: String): LocalTime? {
    val value = raw.trim()
    if (value.length != 4 || value.any { !it.isDigit() }) return null
    val hour = value.substring(0, 2).toIntOrNull() ?: return null
    val minute = value.substring(2, 4).toIntOrNull() ?: return null
    return runCatching { LocalTime.of(hour, minute) }.getOrNull()
}

private fun restaurantZoneId(event: TravelEvent): ZoneId {
    val timeZoneId = event.tz.takeIf { it.isNotBlank() } ?: return ZoneId.systemDefault()
    return runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
}

private fun restaurantScheduleRows(
    schedule: List<RestaurantHoursPeriod>,
    todayIndex: Int
): List<RestaurantHoursRow> {
    return (0..6).map { dayIndex ->
        val periods = schedule.filter { it.dayIndex == dayIndex }
        RestaurantHoursRow(
            dayLabel = restaurantDayLabel(dayIndex),
            hoursLabel = if (periods.isEmpty()) {
                HOURS_CLOSED_LABEL
            } else {
                periods.joinToString(", ") { restaurantHoursLabel(it) }
            },
            isToday = dayIndex == todayIndex
        )
    }
}

private fun restaurantHoursLabel(period: RestaurantHoursPeriod): String {
    if (period.start == period.end && !period.isOvernight) {
        return "Open 24 hours"
    }
    return "${period.start.format(hoursFormatter)} - ${period.end.format(hoursFormatter)}"
}

private fun isRestaurantOpenNow(
    schedule: List<RestaurantHoursPeriod>,
    now: ZonedDateTime
): Boolean {
    val currentDay = now.dayOfWeek.toYelpDayIndex()
    val previousDay = (currentDay + 6) % 7
    val currentTime = now.toLocalTime()

    return schedule.any { period ->
        when {
            period.start == period.end && !period.isOvernight ->
                period.dayIndex == currentDay

            period.dayIndex == currentDay && restaurantCrossesMidnight(period) ->
                currentTime >= period.start

            period.dayIndex == previousDay && restaurantCrossesMidnight(period) ->
                currentTime < period.end

            period.dayIndex == currentDay ->
                currentTime >= period.start && currentTime < period.end

            else -> false
        }
    }
}

private fun restaurantCrossesMidnight(period: RestaurantHoursPeriod): Boolean {
    return period.isOvernight || period.end <= period.start
}

private fun DayOfWeek.toYelpDayIndex(): Int = value - 1

private fun restaurantDayLabel(dayIndex: Int): String {
    return when (dayIndex) {
        0 -> "Mon"
        1 -> "Tue"
        2 -> "Wed"
        3 -> "Thu"
        4 -> "Fri"
        5 -> "Sat"
        6 -> "Sun"
        else -> dayIndex.toString()
    }
}
