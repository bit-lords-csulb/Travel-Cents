package com.example.travelcents.ui.modules

import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.firstNonBlank
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

private val storageTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val defaultDisplayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val defaultDisplayHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)
private val tripDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val itineraryHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
private val longDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.US)
private val longDateWithYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.US)
private val fullDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.US)
private val shortDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

private val flexibleTimeFormatters: List<DateTimeFormatter> = listOf(
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.HOUR_OF_DAY)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .toFormatter(Locale.US),
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalEnd()
        .appendLiteral(' ')
        .appendText(ChronoField.AMPM_OF_DAY)
        .toFormatter(Locale.US)
)

fun parseIsoDate(rawDate: String): LocalDate? {
    if (rawDate.isBlank()) return null
    return runCatching { LocalDate.parse(rawDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}

fun normalizeDate(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: rawDate.trim()
}

fun todayIsoDate(zoneId: ZoneId = ZoneId.systemDefault()): String {
    return LocalDate.now(zoneId).format(DateTimeFormatter.ISO_LOCAL_DATE)
}

fun parseFlexibleTime(rawTime: String): LocalTime? {
    if (rawTime.isBlank()) return null

    val normalized = rawTime
        .trim()
        .replace(".", "")
        .replace(Regex("(?i)(\\d)(am|pm)$"), "$1 $2")
        .uppercase(Locale.US)

    return flexibleTimeFormatters.firstNotNullOfOrNull { formatter ->
        runCatching { LocalTime.parse(normalized, formatter) }.getOrNull()
    }
}

fun normalizeTime(rawTime: String): String {
    return parseFlexibleTime(rawTime)?.format(storageTimeFormatter) ?: rawTime.trim()
}

fun formatDisplayTime(time: String, pattern: String? = null): String {
    val formatter = if (pattern != null) DateTimeFormatter.ofPattern(pattern, Locale.US) else defaultDisplayTimeFormatter
    return parseFlexibleTime(time)?.format(formatter) ?: time.ifBlank { "TIME TBD" }
}

fun formatDisplayTimeRange(startTime: String, endTime: String, pattern: String? = null): String {
    val formattedStart = formatDisplayTime(startTime, pattern)
    val formatter = if (pattern != null) DateTimeFormatter.ofPattern(pattern, Locale.US) else defaultDisplayTimeFormatter
    val formattedEnd = parseFlexibleTime(endTime)?.format(formatter)

    return when {
        formattedEnd.isNullOrBlank() -> formattedStart
        formattedStart == "TIME TBD" -> formattedEnd
        else -> "$formattedStart - $formattedEnd"
    }
}

fun formatMinutes(minutes: Int, pattern: String? = null): String {
    val clamped = minutes.coerceIn(0, (23 * 60) + 59)
    val formatter = if (pattern != null) DateTimeFormatter.ofPattern(pattern, Locale.US) else defaultDisplayTimeFormatter
    return LocalTime.of(clamped / 60, clamped % 60).format(formatter)
}

fun parseTimeToMinutes(rawTime: String): Int? {
    return parseFlexibleTime(rawTime)?.let { it.hour * 60 + it.minute }
}

fun plusMinutes(rawTime: String, minutesToAdd: Long, pattern: String? = null): String {
    val formatter = if (pattern != null) DateTimeFormatter.ofPattern(pattern, Locale.US) else defaultDisplayTimeFormatter
    return parseFlexibleTime(rawTime)
        ?.plusMinutes(minutesToAdd)
        ?.format(formatter)
        ?: rawTime
}

fun hourLabel(hour: Int, pattern: String? = null): String {
    val formatter = if (pattern != null) {
        // If pattern is "HH:mm" or similar, we want just the hour part
        val hourPattern = if (pattern.contains("H")) "HH:00" else "h a"
        DateTimeFormatter.ofPattern(hourPattern, Locale.US)
    } else {
        defaultDisplayHourFormatter
    }
    return LocalTime.of(hour.coerceIn(0, 23), 0).format(formatter)
}

fun hourLabel24(hour: Int): String {
    return "%02d:00".format(Locale.US, hour.coerceIn(0, 23))
}

fun formatTripDate(rawDate: String, pattern: String? = null): String {
    val formatter = if (pattern != null) DateTimeFormatter.ofPattern(pattern, Locale.US) else tripDateFormatter
    return parseIsoDate(rawDate)?.format(formatter) ?: rawDate
}

fun formatLongTripDateWithYear(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(longDateWithYearFormatter) ?: rawDate
}

fun formatLongDuration(totalMinutes: Long): String {
    if (totalMinutes <= 0) return ""
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return buildString {
        if (hours > 0) append("$hours ${if (hours == 1L) "hour" else "hours"}")
        if (minutes > 0) {
            if (isNotEmpty()) append(" ")
            append("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
        }
    }
}

fun formatItineraryHeader(rawDate: String, pattern: String? = null): String {
    return if (pattern != null) {
        parseIsoDate(rawDate)?.format(DateTimeFormatter.ofPattern("EEEE, $pattern", Locale.US))?.uppercase(Locale.US) ?: rawDate
    } else {
        parseIsoDate(rawDate)?.format(itineraryHeaderFormatter)?.uppercase(Locale.US) ?: rawDate
    }
}

fun formatLongDayLabel(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(longDayFormatter) ?: rawDate
}

fun formatDayOfWeekShort(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(shortDayFormatter)?.uppercase(Locale.US) ?: rawDate
}

fun formatDayOfWeekFull(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(fullDayFormatter)?.uppercase(Locale.US) ?: rawDate
}

fun formatMonthDayCompact(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(tripDateFormatter)?.uppercase(Locale.US) ?: rawDate
}

fun formatHeroDate(rawDate: String, pattern: String? = null): String {
    val date = parseIsoDate(rawDate) ?: return rawDate.ifBlank { "—" }
    if (pattern != null) {
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.US)).uppercase(Locale.US)
    }
    val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).uppercase(Locale.US)
    val day = date.dayOfMonth
    val suffix = when {
        day in 11..13 -> "TH"
        day % 10 == 1 -> "ST"
        day % 10 == 2 -> "ND"
        day % 10 == 3 -> "RD"
        else -> "TH"
    }
    return "$month $day$suffix"
}

fun formatWeekRangeHero(startDate: String, endDate: String, pattern: String? = null): String {
    val start = parseIsoDate(startDate) ?: return startDate.ifBlank { "—" }
    val end = parseIsoDate(endDate) ?: return startDate.ifBlank { "—" }
    
    if (pattern != null) {
        return "${start.format(DateTimeFormatter.ofPattern(pattern, Locale.US))} – ${end.format(DateTimeFormatter.ofPattern(pattern, Locale.US))}".uppercase(Locale.US)
    }

    val startMonth = start.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).uppercase(Locale.US)
    val endMonth = end.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).uppercase(Locale.US)
    return if (start.month == end.month) {
        "$startMonth ${start.dayOfMonth} – ${end.dayOfMonth}"
    } else {
        "$startMonth ${start.dayOfMonth} – $endMonth ${end.dayOfMonth}"
    }
}

fun buildCalendarDates(
    dateFrom: String,
    dateTo: String,
    eventDates: List<String>
): List<String> {
    val start = parseIsoDate(dateFrom)
    val end = parseIsoDate(dateTo)

    if (start != null && end != null && !end.isBefore(start)) {
        return generateSequence(start) { current ->
            current.plusDays(1).takeIf { !it.isAfter(end) }
        }.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }.toList()
    }

    return eventDates
        .mapNotNull(::parseIsoDate)
        .distinct()
        .sorted()
        .map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
}

fun zoneIdOrDefault(rawZoneId: String, fallback: ZoneId = ZoneId.systemDefault()): ZoneId {
    return runCatching { ZoneId.of(rawZoneId.trim()) }.getOrDefault(fallback)
}

fun defaultPlanTimeZoneId(): String = ZoneId.systemDefault().id

fun formatTimeZoneLabel(timeZoneId: String, date: String, time: String): String {
    val zoneId = zoneIdOrDefault(timeZoneId)
    val localDate = parseIsoDate(date) ?: LocalDate.now(zoneId)
    val localTime = parseFlexibleTime(time) ?: LocalTime.NOON
    val zonedDateTime = ZonedDateTime.of(localDate, localTime, zoneId)
    val abbreviation = DateTimeFormatter.ofPattern("zzz", Locale.US).format(zonedDateTime)
    val regionName = zoneId.getDisplayName(TextStyle.SHORT, Locale.US)
    return if (abbreviation.equals(regionName, ignoreCase = true)) abbreviation else "$abbreviation (${zoneId.id})"
}

fun sortEventsForCalendar(events: List<TravelEvent>): List<TravelEvent> {
    return events.sortedWith(
        compareBy<TravelEvent>(
            { normalizeDate(it.date) },
            { parseTimeToMinutes(it.startTime) ?: Int.MAX_VALUE },
            { parseTimeToMinutes(it.endTime) ?: Int.MAX_VALUE },
            {
                it.details.firstNonBlank(
                    "title",
                    ATTR_BUSINESS_NAME,
                    ATTR_HOTEL_NAME,
                    "activity_name",
                    "restaurant_name",
                    "hotel_name",
                    "name"
                )
                    ?: it.type
            }
        )
    )
}
