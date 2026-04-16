package com.example.travelcents.ui.main

import com.example.travelcents.data.model.TravelEvent
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
private val displayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val displayHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)
private val tripDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val itineraryHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
private val longDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.US)
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

fun formatDisplayTime(time: String, locale: Locale = Locale.US): String {
    val formatter = if (locale == Locale.US) displayTimeFormatter else DateTimeFormatter.ofPattern("HH:mm", locale)
    return parseFlexibleTime(time)?.format(formatter) ?: time.ifBlank { "TIME TBD" }
}

fun formatDisplayTimeRange(startTime: String, endTime: String, locale: Locale = Locale.US): String {
    val formattedStart = formatDisplayTime(startTime, locale)
    val formattedEnd = parseFlexibleTime(endTime)?.let { 
        val formatter = if (locale == Locale.US) displayTimeFormatter else DateTimeFormatter.ofPattern("HH:mm", locale)
        it.format(formatter)
    }

    return when {
        formattedEnd.isNullOrBlank() -> formattedStart
        formattedStart == "TIME TBD" -> formattedEnd
        else -> "$formattedStart - $formattedEnd"
    }
}

fun formatMinutes(minutes: Int, locale: Locale = Locale.US): String {
    val clamped = minutes.coerceIn(0, (23 * 60) + 59)
    val formatter = if (locale == Locale.US) displayTimeFormatter else DateTimeFormatter.ofPattern("HH:mm", locale)
    return LocalTime.of(clamped / 60, clamped % 60).format(formatter)
}

fun parseTimeToMinutes(rawTime: String): Int? {
    return parseFlexibleTime(rawTime)?.let { it.hour * 60 + it.minute }
}

fun plusMinutes(rawTime: String, minutesToAdd: Long, locale: Locale = Locale.US): String {
    val formatter = if (locale == Locale.US) displayTimeFormatter else DateTimeFormatter.ofPattern("HH:mm", locale)
    return parseFlexibleTime(rawTime)
        ?.plusMinutes(minutesToAdd)
        ?.format(formatter)
        ?: rawTime
}

fun hourLabel(hour: Int, locale: Locale = Locale.US): String {
    val formatter = if (locale == Locale.US) displayHourFormatter else DateTimeFormatter.ofPattern("HH:00", locale)
    return LocalTime.of(hour.coerceIn(0, 23), 0).format(formatter)
}

fun hourLabel24(hour: Int): String {
    return "%02d:00".format(Locale.US, hour.coerceIn(0, 23))
}

fun formatTripDate(rawDate: String, pattern: String = "MMM d"): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
    return parseIsoDate(rawDate)?.format(formatter) ?: rawDate
}

fun formatItineraryHeader(rawDate: String, pattern: String = "EEEE, MMMM d"): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
    return parseIsoDate(rawDate)?.format(formatter)?.uppercase(Locale.US) ?: rawDate
}

fun formatLongDayLabel(rawDate: String, pattern: String = "MMMM d"): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
    return parseIsoDate(rawDate)?.format(formatter) ?: rawDate
}

fun formatDayOfWeekShort(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(shortDayFormatter)?.uppercase(Locale.US) ?: rawDate
}

fun formatMonthDayCompact(rawDate: String, pattern: String = "MMM d"): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
    return parseIsoDate(rawDate)?.format(formatter)?.uppercase(Locale.US) ?: rawDate
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
                it.details["title"]
                    ?: it.details["activity_name"]
                    ?: it.details["restaurant_name"]
                    ?: it.details["hotel_name"]
                    ?: it.details["name"]
                    ?: it.type
            }
        )
    )
}
