package com.example.travelcents.ui.main.current.calendar

import com.example.travelcents.data.trip.model.ATTR_ARRIVAL_DAY_OFFSET
import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_CHECK_OUT_TIME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.ui.main.current.eventTitle
import com.example.travelcents.ui.modules.formatMinutes
import com.example.travelcents.ui.modules.formatTripDate
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.parseIsoDate
import com.example.travelcents.ui.modules.parseTimeToMinutes
import com.example.travelcents.ui.modules.todayIsoDate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class ScheduleWindow(
    val startHour: Int,
    val endHour: Int
)

data class EventRenderSpan(
    val event: TravelEvent,
    val startMinutes: Int,
    val endMinutes: Int,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean,
    val titleOverride: String? = null,
    val labelOverride: String? = null
)

data class EventLayoutInfo(
    val span: EventRenderSpan,
    val columnIndex: Int,
    val columnCount: Int
)

fun buildTripDateRange(
    dateFrom: String,
    dateTo: String,
    eventDates: List<String>
): String {
    val start = dateFrom.ifBlank { eventDates.firstOrNull().orEmpty() }
    val end = dateTo.ifBlank { eventDates.lastOrNull().orEmpty() }

    if (start.isBlank() && end.isBlank()) {
        return "DATES TBD"
    }

    if (start == end || end.isBlank()) {
        return formatTripDate(start).uppercase(Locale.US)
    }

    return "${formatTripDate(start).uppercase(Locale.US)} - ${formatTripDate(end).uppercase(Locale.US)}"
}

fun eventsForDate(
    events: List<TravelEvent>,
    date: String
): List<TravelEvent> {
    return eventSpansForDate(events, date).map(EventRenderSpan::event)
}

fun visibleWeekDatesForSelection(
    allDates: List<String>,
    selectedDate: String
): List<String> {
    val startDate = when {
        selectedDate.isNotBlank() -> parseIsoDate(selectedDate)
        allDates.isNotEmpty() -> parseIsoDate(allDates.first())
        else -> parseIsoDate(todayIsoDate())
    } ?: return emptyList()
    return List(7) { offset -> startDate.plusDays(offset.toLong()).toString() }
}

fun defaultStartMinutesForDate(
    events: List<TravelEvent>,
    date: String
): Int {
    return eventSpansForDate(events, date)
        .map(EventRenderSpan::startMinutes)
        .minOrNull()
        ?.let { (it - 30).coerceAtLeast(0) }
        ?: 9 * 60
}

fun eventSpansForDate(
    events: List<TravelEvent>,
    date: String
): List<EventRenderSpan> {
    return events.flatMap { event -> eventSpansForDate(event, date) }
}

fun buildEventLayouts(
    date: String,
    events: List<TravelEvent>,
    scheduleWindow: ScheduleWindow
): List<EventLayoutInfo> {
    val windowStart = scheduleWindow.startHour * 60
    val windowEnd = scheduleWindow.endHour * 60

    val spans = eventSpansForDate(events, date).mapNotNull { span ->
        span.let {
            if (span.endMinutes <= windowStart || span.startMinutes >= windowEnd) {
                null
            } else {
                span.copy(
                    startMinutes = max(span.startMinutes, windowStart),
                    endMinutes = min(span.endMinutes, windowEnd)
                )
            }
        }
    }.sortedWith(
        compareBy<EventRenderSpan>(
            { it.startMinutes },
            { it.endMinutes },
            { eventTitle(it.event) }
        )
    )

    if (spans.isEmpty()) return emptyList()

    val clusters = mutableListOf<MutableList<EventRenderSpan>>()
    var currentCluster = mutableListOf<EventRenderSpan>()
    var currentClusterEnd = -1

    spans.forEach { span ->
        if (currentCluster.isEmpty() || span.startMinutes < currentClusterEnd) {
            currentCluster.add(span)
            currentClusterEnd = max(currentClusterEnd, span.endMinutes)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(span)
            currentClusterEnd = span.endMinutes
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    return clusters.flatMap(::layoutCluster)
}

private fun layoutCluster(cluster: List<EventRenderSpan>): List<EventLayoutInfo> {
    val assignments = linkedMapOf<EventRenderSpan, Int>()
    val active = mutableListOf<Pair<EventRenderSpan, Int>>()
    var maxColumns = 1

    cluster.forEach { span ->
        active.removeAll { (activeSpan, _) -> activeSpan.endMinutes <= span.startMinutes }
        val usedColumns = active.map { it.second }.toSet()
        val nextColumn = generateSequence(0) { it + 1 }.first { it !in usedColumns }
        assignments[span] = nextColumn
        active.add(span to nextColumn)
        maxColumns = max(maxColumns, active.size)
    }

    return cluster.map { span ->
        EventLayoutInfo(
            span = span,
            columnIndex = assignments.getValue(span),
            columnCount = maxColumns
        )
    }
}

fun eventSpanForDate(
    event: TravelEvent,
    date: String
): EventRenderSpan? {
    return eventSpansForDate(event, date).firstOrNull()
}

private fun eventSpansForDate(
    event: TravelEvent,
    date: String
): List<EventRenderSpan> {
    val targetDate = parseIsoDate(date) ?: return emptyList()
    val spans = eventDateTimeRanges(event)
    if (spans.isEmpty()) return emptyList()
    return spans.mapNotNull { timing ->
        eventSpanForDate(event, targetDate, timing)
    }
}

private fun eventSpanForDate(
    event: TravelEvent,
    targetDate: LocalDate,
    timing: CalendarEventTiming
): EventRenderSpan? {
    val dayStart = targetDate.atStartOfDay()
    val dayEnd = targetDate.plusDays(1).atStartOfDay()
    val startDateTime = timing.startDateTime
    val endDateTime = timing.endDateTime

    if (!endDateTime.isAfter(dayStart) || !startDateTime.isBefore(dayEnd)) {
        return null
    }

    val clippedStart = if (startDateTime.isBefore(dayStart)) dayStart else startDateTime
    val clippedEnd = if (endDateTime.isAfter(dayEnd)) dayEnd else endDateTime
    val startMinutes = Duration.between(dayStart, clippedStart).toMinutes().toInt()
    val endMinutes = Duration.between(dayStart, clippedEnd).toMinutes().toInt()

    return EventRenderSpan(
        event = event,
        startMinutes = startMinutes.coerceIn(0, 24 * 60),
        endMinutes = endMinutes.coerceAtLeast(startMinutes + 15).coerceAtMost(24 * 60),
        continuesBefore = startDateTime.isBefore(dayStart),
        continuesAfter = endDateTime.isAfter(dayEnd),
        titleOverride = timing.titleOverride,
        labelOverride = timing.labelOverride
    )
}

fun eventDateTimeRange(event: TravelEvent): Pair<LocalDateTime, LocalDateTime>? {
    return eventDateTimeRanges(event).firstOrNull()?.let { it.startDateTime to it.endDateTime }
}

fun renderSpanLabel(span: EventRenderSpan): String {
    span.labelOverride?.let { return it }
    val safeEndMinutes = span.endMinutes.coerceAtMost((23 * 60) + 59)
    return when {
        span.continuesBefore && span.continuesAfter -> "Continues all day"
        span.continuesBefore -> "Until ${formatMinutes(safeEndMinutes)}"
        span.continuesAfter -> "${formatMinutes(span.startMinutes)} onward"
        else -> "${formatMinutes(span.startMinutes)} - ${formatMinutes(safeEndMinutes)}"
    }
}

fun preferredDayScrollMinutes(
    date: String,
    events: List<TravelEvent>
): Int {
    val todayMinutes = currentTimeMinutes(date)
    val firstEventMinute = events
        .flatMap { eventSpansForDate(it, date) }
        .map(EventRenderSpan::startMinutes)
        .minOrNull()

    val anchorMinutes = todayMinutes ?: firstEventMinute ?: (8 * 60)
    return (anchorMinutes - 60).coerceIn(0, 22 * 60)
}

fun currentTimeMinutes(date: String): Int? {
    if (date != todayIsoDate()) return null
    val now = LocalTime.now()
    return (now.hour * 60) + now.minute
}

fun buildScheduleWindow(
    events: List<TravelEvent>,
    dates: List<String> = emptyList()
): ScheduleWindow {
    val spans = if (dates.isEmpty()) {
        events.flatMap { event ->
            val anchorDate = eventDateTimeRanges(event)
                .firstOrNull()
                ?.startDateTime
                ?.toLocalDate()
                ?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                ?: normalizeDate(event.date)
            eventSpansForDate(event, anchorDate)
        }
    } else {
        dates.flatMap { date ->
            eventSpansForDate(events, date)
        }
    }

    val startMinutes = spans.map { it.startMinutes }
    val endMinutes = spans.map { it.endMinutes }

    if (startMinutes.isEmpty()) {
        return ScheduleWindow(startHour = 8, endHour = 20)
    }

    val earliest = startMinutes.minOrNull() ?: 8 * 60
    val latest = max(
        endMinutes.maxOrNull() ?: (startMinutes.maxOrNull() ?: 18 * 60) + 90,
        earliest + 120
    )

    val startHour = max(6, (earliest / 60) - 1)
    val endHour = min(24, ceil((latest + 60) / 60f).toInt())

    return ScheduleWindow(
        startHour = startHour,
        endHour = max(endHour, startHour + 6)
    )
}

private data class CalendarEventTiming(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val titleOverride: String? = null,
    val labelOverride: String? = null
)

private fun eventDateTimeRanges(event: TravelEvent): List<CalendarEventTiming> {
    return when (event.type.lowercase(Locale.US)) {
        "flight" -> flightEventDateTimeRange(event)?.let(::listOf).orEmpty()
        "hotel" -> hotelEventDateTimeRanges(event)
        else -> defaultEventDateTimeRange(event)?.let(::listOf).orEmpty()
    }
}

private fun flightEventDateTimeRange(event: TravelEvent): CalendarEventTiming? {
    val fallback = defaultEventDateTimeRange(event)
    val departure = resolveFlightDepartureDateTime(event) ?: return fallback
    val arrival = resolveFlightArrivalDateTime(event, departure) ?: fallback?.endDateTime ?: return null
    val safeArrival = if (arrival.isAfter(departure)) arrival else departure.plusMinutes(90)
    return CalendarEventTiming(
        startDateTime = departure,
        endDateTime = safeArrival
    )
}

private fun hotelEventDateTimeRanges(event: TravelEvent): List<CalendarEventTiming> {
    val stay = resolveHotelStayDateTimeRange(event) ?: return defaultEventDateTimeRange(event)?.let(::listOf).orEmpty()
    val title = event.detailValue(ATTR_HOTEL_NAME, "hotel_name", "title", "name")
    val checkInTitle = title ?: "Hotel check-in"
    val checkoutTitle = title?.let { "$it checkout" } ?: "Hotel checkout"
    val checkInStart = stay.startDateTime.minusHours(2)
    val checkoutStart = stay.endDateTime.minusHours(2)
    return listOf(
        CalendarEventTiming(
            startDateTime = checkInStart,
            endDateTime = stay.startDateTime,
            titleOverride = checkInTitle
        ),
        CalendarEventTiming(
            startDateTime = checkoutStart,
            endDateTime = stay.endDateTime,
            titleOverride = checkoutTitle
        )
    )
}

private fun resolveHotelStayDateTimeRange(event: TravelEvent): CalendarEventTiming? {
    val checkInDate = parseBestDate(
        event.details["check_in_date"],
        event.date
    ) ?: parseIsoDate(event.date) ?: return null
    val checkOutDate = parseBestDate(
        event.details["check_out_date"],
        event.details["check_out_time"]
    )
    val checkInTime = parseBestTime(
        event.detailValue(ATTR_CHECK_IN_TIME, "check_in_time", "check_in"),
        event.startTime
    ) ?: LocalTime.of(15, 0)
    val checkOutTime = parseBestTime(
        event.detailValue(ATTR_CHECK_OUT_TIME, "check_out_time", "check_out"),
        event.endTime
    ) ?: LocalTime.of(11, 0)
    val start = LocalDateTime.of(checkInDate, checkInTime)
    val candidateEndDate = checkOutDate ?: checkInDate
    var end = LocalDateTime.of(candidateEndDate, checkOutTime)
    if (!end.isAfter(start)) {
        end = LocalDateTime.of(candidateEndDate.plusDays(1), checkOutTime)
    }
    return CalendarEventTiming(start, end)
}

private fun defaultEventDateTimeRange(event: TravelEvent): CalendarEventTiming? {
    val startDate = parseIsoDate(event.date) ?: return null
    val isAllDay = event.details["is_all_day"]?.equals("true", ignoreCase = true) == true
    val startMinutes = parseTimeToMinutes(event.startTime) ?: if (isAllDay) 0 else 9 * 60
    val startDateTime = startDate.atStartOfDay().plusMinutes(startMinutes.toLong())

    val checkOutDate = event.details["check_out_date"]?.let(::parseIsoDate)
    val baseEndDate = when {
        checkOutDate != null && !checkOutDate.isBefore(startDate) -> checkOutDate
        else -> startDate
    }
    val parsedEndMinutes = parseTimeToMinutes(event.endTime)
    var endDateTime = when {
        isAllDay && baseEndDate.isAfter(startDate) -> baseEndDate.atStartOfDay()
        isAllDay -> startDate.plusDays(1).atStartOfDay()
        parsedEndMinutes != null -> baseEndDate.atStartOfDay().plusMinutes(parsedEndMinutes.toLong())
        else -> startDateTime.plusMinutes(90)
    }

    if (!endDateTime.isAfter(startDateTime)) {
        endDateTime = when {
            baseEndDate.isAfter(startDate) -> baseEndDate.atStartOfDay().plusMinutes((parsedEndMinutes ?: 0).toLong())
            parsedEndMinutes != null -> startDate.plusDays(1).atStartOfDay().plusMinutes(parsedEndMinutes.toLong())
            else -> startDateTime.plusMinutes(90)
        }
    }

    return CalendarEventTiming(startDateTime, endDateTime)
}

private fun resolveFlightDepartureDateTime(event: TravelEvent): LocalDateTime? {
    parseDateTimeCandidate(
        dateCandidate = event.details["departure_date"],
        timeCandidate = event.details["departure_time"]
    )?.let { return it }
    parseDateTimeFromRaw(event.details["departure_time"])?.let { return it }
    val departureDate = parseBestDate(event.details["departure_date"], event.date) ?: parseIsoDate(event.date)
    val departureTime = parseBestTime(event.details["departure_time"], event.startTime) ?: parseBestTime(event.startTime)
    return if (departureDate != null && departureTime != null) {
        LocalDateTime.of(departureDate, departureTime)
    } else {
        null
    }
}

private fun resolveFlightArrivalDateTime(
    event: TravelEvent,
    departure: LocalDateTime
): LocalDateTime? {
    parseDateTimeCandidate(
        dateCandidate = event.details["arrival_date"],
        timeCandidate = event.details["arrival_time"]
    )?.let { return it }
    parseDateTimeFromRaw(event.details["arrival_time"])?.let { return it }
    val arrivalDate = parseBestDate(event.details["arrival_date"], event.details["arrival_time"])
    val arrivalTime = parseBestTime(event.details["arrival_time"], event.endTime) ?: parseBestTime(event.endTime)
    if (arrivalTime == null) return null

    val dayOffset = event.detailValue(ATTR_ARRIVAL_DAY_OFFSET, "arrival_day_offset")?.toLongOrNull() ?: 0L
    val baseDate = arrivalDate ?: departure.toLocalDate().plusDays(dayOffset)
    var arrival = LocalDateTime.of(baseDate, arrivalTime)
    if (dayOffset == 0L && arrivalDate == null && !arrival.isAfter(departure)) {
        arrival = arrival.plusDays(1)
    }
    return arrival
}

private fun parseDateTimeCandidate(
    dateCandidate: String?,
    timeCandidate: String?
): LocalDateTime? {
    parseDateTimeFromRaw(timeCandidate)?.let { return it }
    val date = parseBestDate(dateCandidate, timeCandidate) ?: return null
    val time = parseBestTime(timeCandidate) ?: return null
    return LocalDateTime.of(date, time)
}

private fun parseDateTimeFromRaw(raw: String?): LocalDateTime? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val date = parseBestDate(value) ?: return null
    val time = parseBestTime(value) ?: return null
    return LocalDateTime.of(date, time)
}

private fun parseBestDate(vararg candidates: String?): LocalDate? {
    return candidates.asSequence()
        .mapNotNull { candidate ->
            candidate?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { raw ->
                    ISO_DATE_REGEX.find(raw)?.value?.let(::parseIsoDate)
                        ?: parseIsoDate(raw)
                }
        }
        .firstOrNull()
}

private fun parseBestTime(vararg candidates: String?): LocalTime? {
    return candidates.asSequence()
        .mapNotNull { candidate ->
            candidate?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let(::extractTime)
        }
        .firstOrNull()
}

private fun extractTime(raw: String): LocalTime? {
    parseTimeToMinutes(raw)?.let { minutes ->
        return LocalTime.of(minutes / 60, minutes % 60)
    }
    val match = TIME_REGEX.find(raw) ?: return null
    return parseTimeToMinutes(match.value)?.let { minutes ->
        LocalTime.of(minutes / 60, minutes % 60)
    }
}

private val ISO_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
private val TIME_REGEX = Regex("\\b\\d{1,2}:\\d{2}(?:\\s?[APMapm]{2})?\\b")

