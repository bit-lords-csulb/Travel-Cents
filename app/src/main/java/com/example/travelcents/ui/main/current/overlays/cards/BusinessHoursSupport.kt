package com.example.travelcents.ui.main.current.overlays.cards

import com.example.travelcents.data.trip.model.TravelEvent
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val HOURS_CLOSED_LABEL = "Closed"

internal val hoursFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.US)

internal data class BusinessHoursPeriod(
    val dayIndex: Int,
    val start: LocalTime,
    val end: LocalTime,
    val isOvernight: Boolean
)

internal data class BusinessHoursRow(
    val dayLabel: String,
    val hoursLabel: String,
    val isToday: Boolean
)

internal fun parseBusinessHours(rawHours: String): List<BusinessHoursPeriod> {
    return rawHours.split("|")
        .mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size < 4) return@mapNotNull null

            val dayIndex = parts[0].toIntOrNull()?.takeIf { it in 0..6 } ?: return@mapNotNull null
            val start = parseBusinessHoursTime(parts[1]) ?: return@mapNotNull null
            val end = parseBusinessHoursTime(parts[2]) ?: return@mapNotNull null
            val isOvernight = parts[3].trim().toBooleanStrictOrNull() ?: false

            BusinessHoursPeriod(
                dayIndex = dayIndex,
                start = start,
                end = end,
                isOvernight = isOvernight
            )
        }
        .sortedWith(compareBy(BusinessHoursPeriod::dayIndex, BusinessHoursPeriod::start))
}

internal fun businessZoneId(event: TravelEvent): ZoneId {
    val timeZoneId = event.tz.takeIf { it.isNotBlank() } ?: return ZoneId.systemDefault()
    return runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
}

internal fun businessScheduleRows(
    schedule: List<BusinessHoursPeriod>,
    todayIndex: Int
): List<BusinessHoursRow> {
    return (0..6).map { dayIndex ->
        val periods = schedule.filter { it.dayIndex == dayIndex }
        BusinessHoursRow(
            dayLabel = businessDayLabel(dayIndex),
            hoursLabel = if (periods.isEmpty()) {
                HOURS_CLOSED_LABEL
            } else {
                periods.joinToString(", ") { businessHoursLabel(it) }
            },
            isToday = dayIndex == todayIndex
        )
    }
}

internal fun isBusinessOpenNow(
    schedule: List<BusinessHoursPeriod>,
    now: ZonedDateTime
): Boolean {
    val currentDay = now.dayOfWeek.toYelpDayIndex()
    val previousDay = (currentDay + 6) % 7
    val currentTime = now.toLocalTime()

    return schedule.any { period ->
        when {
            period.start == period.end && !period.isOvernight ->
                period.dayIndex == currentDay

            period.dayIndex == currentDay && businessCrossesMidnight(period) ->
                currentTime >= period.start

            period.dayIndex == previousDay && businessCrossesMidnight(period) ->
                currentTime < period.end

            period.dayIndex == currentDay ->
                currentTime >= period.start && currentTime < period.end

            else -> false
        }
    }
}

internal fun DayOfWeek.toYelpDayIndex(): Int = value - 1

private fun parseBusinessHoursTime(raw: String): LocalTime? {
    val value = raw.trim()
    if (value.length != 4 || value.any { !it.isDigit() }) return null
    val hour = value.substring(0, 2).toIntOrNull() ?: return null
    val minute = value.substring(2, 4).toIntOrNull() ?: return null
    return runCatching { LocalTime.of(hour, minute) }.getOrNull()
}

private fun businessHoursLabel(period: BusinessHoursPeriod): String {
    if (period.start == period.end && !period.isOvernight) {
        return "Open 24 hours"
    }
    return "${period.start.format(hoursFormatter)} - ${period.end.format(hoursFormatter)}"
}

private fun businessCrossesMidnight(period: BusinessHoursPeriod): Boolean {
    return period.isOvernight || period.end <= period.start
}

private fun businessDayLabel(dayIndex: Int): String {
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
