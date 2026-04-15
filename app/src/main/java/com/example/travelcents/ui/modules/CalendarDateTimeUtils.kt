package com.example.travelcents.ui.modules

import com.example.travelcents.data.trip.model.TravelEvent
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

fun formatDisplayTime(time: String): String {
    return parseFlexibleTime(time)?.format(displayTimeFormatter) ?: time.ifBlank { "TIME TBD" }
}

fun formatDisplayTimeRange(startTime: String, endTime: String): String {
    val formattedStart = formatDisplayTime(startTime)
    val formattedEnd = parseFlexibleTime(endTime)?.format(displayTimeFormatter)

    return when {
        formattedEnd.isNullOrBlank() -> formattedStart
        formattedStart == "TIME TBD" -> formattedEnd
        else -> "$formattedStart - $formattedEnd"
    }
}

fun formatMinutes(minutes: Int): String {
    val clamped = minutes.coerceIn(0, (23 * 60) + 59)
    return LocalTime.of(clamped / 60, clamped % 60).format(displayTimeFormatter)
}

fun parseTimeToMinutes(rawTime: String): Int? {
    return parseFlexibleTime(rawTime)?.let { it.hour * 60 + it.minute }
}

fun plusMinutes(rawTime: String, minutesToAdd: Long): String {
    return parseFlexibleTime(rawTime)
        ?.plusMinutes(minutesToAdd)
        ?.format(displayTimeFormatter)
        ?: rawTime
}

fun hourLabel(hour: Int): String {
    return LocalTime.of(hour.coerceIn(0, 23), 0).format(displayHourFormatter)
}

fun hourLabel24(hour: Int): String {
    return "%02d:00".format(Locale.US, hour.coerceIn(0, 23))
}

fun formatTripDate(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(tripDateFormatter) ?: rawDate
}

fun formatItineraryHeader(rawDate: String): String {
    return parseIsoDate(rawDate)?.format(itineraryHeaderFormatter)?.uppercase(Locale.US) ?: rawDate
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

fun formatHeroDate(rawDate: String): String {
    val date = parseIsoDate(rawDate) ?: return rawDate.ifBlank { "—" }
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

fun formatWeekRangeHero(startDate: String, endDate: String): String {
    val start = parseIsoDate(startDate) ?: return startDate.ifBlank { "—" }
    val end = parseIsoDate(endDate) ?: return startDate.ifBlank { "—" }
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

