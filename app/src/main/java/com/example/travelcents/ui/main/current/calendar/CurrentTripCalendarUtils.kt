package com.example.travelcents.ui.main.current.calendar

import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.ui.main.current.eventTitle
import com.example.travelcents.ui.modules.formatMinutes
import com.example.travelcents.ui.modules.formatTripDate
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.parseIsoDate
import com.example.travelcents.ui.modules.parseTimeToMinutes
import com.example.travelcents.ui.modules.todayIsoDate
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
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
    val continuesAfter: Boolean
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
    return events.filter { eventSpanForDate(it, date) != null }
}

fun visibleWeekDatesForSelection(
    allDates: List<String>,
    selectedDate: String
): List<String> {
    if (allDates.isEmpty()) return emptyList()

    val selectedIndex = allDates.indexOf(selectedDate).takeIf { it >= 0 } ?: 0
    val startIndex = (selectedIndex / 7) * 7
    val startDate = parseIsoDate(allDates[startIndex]) ?: return allDates.drop(startIndex).take(7)
    return List(7) { offset -> startDate.plusDays(offset.toLong()).toString() }
}

fun defaultStartMinutesForDate(
    events: List<TravelEvent>,
    date: String
): Int {
    return eventsForDate(events, date)
        .mapNotNull { eventSpanForDate(it, date)?.startMinutes }
        .minOrNull()
        ?.let { (it - 30).coerceAtLeast(0) }
        ?: 9 * 60
}

fun buildEventLayouts(
    date: String,
    events: List<TravelEvent>,
    scheduleWindow: ScheduleWindow
): List<EventLayoutInfo> {
    val windowStart = scheduleWindow.startHour * 60
    val windowEnd = scheduleWindow.endHour * 60

    val spans = events.mapNotNull { event ->
        eventSpanForDate(event, date)?.let { span ->
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
    val targetDate = parseIsoDate(date) ?: return null
    val (startDateTime, endDateTime) = eventDateTimeRange(event) ?: return null
    val dayStart = targetDate.atStartOfDay()
    val dayEnd = targetDate.plusDays(1).atStartOfDay()

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
        continuesAfter = endDateTime.isAfter(dayEnd)
    )
}

fun eventDateTimeRange(event: TravelEvent): Pair<LocalDateTime, LocalDateTime>? {
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

    return startDateTime to endDateTime
}

fun renderSpanLabel(span: EventRenderSpan): String {
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
        .mapNotNull { eventSpanForDate(it, date)?.startMinutes }
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
        events.mapNotNull { eventSpanForDate(it, normalizeDate(it.date)) }
    } else {
        dates.flatMap { date ->
            events.mapNotNull { eventSpanForDate(it, date) }
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
