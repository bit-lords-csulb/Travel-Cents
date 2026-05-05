package com.example.travelcents.ui.main.home

import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_CHECK_OUT_TIME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_WEATHER_CONDITION
import com.example.travelcents.data.trip.model.ATTR_WEATHER_PRECIP_PCT
import com.example.travelcents.data.trip.model.ATTR_WEATHER_TEMP_C
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.data.trip.model.firstNonBlank
import com.example.travelcents.data.trip.model.Itinerary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal enum class HomeTripStatusLevel {
    LOADING,
    EMPTY,
    UPCOMING,
    READY,
    IN_PROGRESS,
    NEEDS_ATTENTION
}

data class HomeTripInfoPill(
    val title: String,
    val detail: String
)

internal data class HomeTripStatusSummary(
    val trip: Itinerary?,
    val level: HomeTripStatusLevel,
    val badgeText: String,
    val routeLine: String,
    val dateLine: String,
    val nextEventTitle: String,
    val nextEventSubtitle: String,
    val infoPills: List<HomeTripInfoPill>
)

internal fun resolveHomeStatusTrip(
    trips: List<Itinerary>,
    selectedTrip: Itinerary?,
    today: LocalDate = LocalDate.now()
): Itinerary? {
    fun isEligible(trip: Itinerary): Boolean {
        if (trip.status.equals("archived", ignoreCase = true)) return false
        val endDate = parseTripDate(trip.dateTo)
        return endDate == null || !endDate.isBefore(today)
    }

    selectedTrip?.takeIf(::isEligible)?.let { return it }

    return trips
        .asSequence()
        .filter(::isEligible)
        .sortedWith(
            compareBy<Itinerary> { parseTripDate(it.dateFrom) ?: LocalDate.MAX }
                .thenByDescending { it.createdAt }
        )
        .firstOrNull()
}

internal fun buildHomeTripStatusSummary(
    trip: Itinerary?,
    events: List<TravelEvent>,
    isTripLoading: Boolean,
    isEventDataLoading: Boolean,
    destinationWeather: HomeTripInfoPill? = null,
    today: LocalDate = LocalDate.now()
): HomeTripStatusSummary {
    if (trip == null) {
        return if (isTripLoading) {
            HomeTripStatusSummary(
                trip = null,
                level = HomeTripStatusLevel.LOADING,
                badgeText = "Updating",
                routeLine = "Loading trip status",
                dateLine = "Checking your upcoming trips",
                nextEventTitle = "Syncing latest details",
                nextEventSubtitle = "",
                infoPills = emptyList()
            )
        } else {
            HomeTripStatusSummary(
                trip = null,
                level = HomeTripStatusLevel.EMPTY,
                badgeText = "No upcoming trips",
                routeLine = "Nothing scheduled",
                dateLine = "Create a trip to see updates here",
                nextEventTitle = "No itinerary updates",
                nextEventSubtitle = "",
                infoPills = emptyList()
            )
        }
    }

    val startDate = parseTripDate(trip.dateFrom)
    val endDate = parseTripDate(trip.dateTo)
    val visibleEvents = events
        .filter { event -> event.itineraryId.isBlank() || event.itineraryId == trip.itineraryId }
        .sortedWith(
            compareBy<TravelEvent> { eventSortDate(it) }
                .thenBy { eventSortTime(it) }
                .thenBy { it.eventId }
        )
    val nextEvent = visibleEvents.firstOrNull { event ->
        val eventDate = parseTripDate(event.date) ?: return@firstOrNull false
        !eventDate.isBefore(today)
    }

    val hasRoute = trip.originIata.isNotBlank() && trip.destinationIata.isNotBlank() ||
        trip.origin.isNotBlank() && trip.destination.isNotBlank()
    val hasInvalidDates = startDate == null || endDate == null
    val hasNoLocalEvents = !isEventDataLoading && visibleEvents.isEmpty()
    val startsToday = startDate == today
    val isInProgress = startDate != null && endDate != null &&
        today.isAfter(startDate) && !today.isAfter(endDate)
    val isFuture = startDate != null && today.isBefore(startDate)

    val level = when {
        hasInvalidDates || !hasRoute || hasNoLocalEvents -> HomeTripStatusLevel.NEEDS_ATTENTION
        startsToday -> HomeTripStatusLevel.READY
        isInProgress -> HomeTripStatusLevel.IN_PROGRESS
        isFuture -> HomeTripStatusLevel.UPCOMING
        else -> HomeTripStatusLevel.UPCOMING
    }

    val hasLanded = hasOutboundLanded(visibleEvents, today)

    return HomeTripStatusSummary(
        trip = trip,
        level = level,
        badgeText = when (level) {
            HomeTripStatusLevel.LOADING -> "Updating"
            HomeTripStatusLevel.EMPTY -> "No upcoming trips"
            HomeTripStatusLevel.UPCOMING -> "Upcoming"
            HomeTripStatusLevel.READY -> "Ready"
            HomeTripStatusLevel.IN_PROGRESS -> "In progress"
            HomeTripStatusLevel.NEEDS_ATTENTION -> "Needs attention"
        },
        routeLine = routeLine(trip, hasLanded),
        dateLine = dateLine(startDate, endDate, today),
        nextEventTitle = nextEventTitle(nextEvent, visibleEvents, isEventDataLoading),
        nextEventSubtitle = nextEventSubtitle(nextEvent, today),
        infoPills = infoPills(events = visibleEvents, destinationWeather = destinationWeather, today = today)
    )
}

private fun routeLine(trip: Itinerary, hasLanded: Boolean): String {
    val origin = trip.originIata.ifBlank { trip.origin.ifBlank { "Origin TBD" } }
    val destination = trip.destinationIata.ifBlank { trip.destination.ifBlank { "Destination TBD" } }
    
    return if (hasLanded) "$destination to $origin" else "$origin to $destination"
}

private fun dateLine(startDate: LocalDate?, endDate: LocalDate?, today: LocalDate): String {
    if (startDate == null || endDate == null) return "Dates need review"

    return when {
        today.isBefore(startDate) -> {
            val days = ChronoUnit.DAYS.between(today, startDate)
            val prefix = when (days) {
                0L -> "Starts today"
                1L -> "Starts tomorrow"
                else -> "Starts in $days days"
            }
            "$prefix - ${dateRange(startDate, endDate)}"
        }
        !today.isAfter(endDate) -> "In progress - ends ${formatMonthDay(endDate)}"
        else -> "Ended ${formatMonthDay(endDate)}"
    }
}

private fun dateRange(startDate: LocalDate, endDate: LocalDate): String {
    return if (startDate == endDate) {
        formatMonthDay(startDate)
    } else {
        "${formatMonthDay(startDate)} - ${formatMonthDay(endDate)}"
    }
}

private fun nextEventTitle(
    nextEvent: TravelEvent?,
    events: List<TravelEvent>,
    isEventDataLoading: Boolean
): String {
    if (nextEvent != null) {
        val name = nextEvent.displayName()
            ?: nextEvent.details["title"]
            ?: nextEvent.details["name"]
            ?: nextEvent.type.replaceFirstChar { it.titlecase(Locale.US) }
        return "Next: $name"
    }
    if (isEventDataLoading) return "Syncing latest itinerary"
    return if (events.isEmpty()) "No itinerary items yet" else "No more scheduled items"
}

private fun nextEventSubtitle(nextEvent: TravelEvent?, today: LocalDate): String {
    nextEvent ?: return ""
    val date = parseTripDate(nextEvent.date)
    val dateText = when (date) {
        null -> "Date TBD"
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> formatMonthDay(date)
    }
    val timeText = nextEvent.startTime.takeIf { it.isNotBlank() } ?: "Time TBD"
    val typeText = nextEvent.type.ifBlank { "Item" }.replaceFirstChar { it.titlecase(Locale.US) }
    return "$dateText at $timeText - $typeText"
}

private fun infoPills(
    events: List<TravelEvent>,
    destinationWeather: HomeTripInfoPill?,
    today: LocalDate
): List<HomeTripInfoPill> {
    return listOf(
        destinationWeather ?: weatherPill(events),
        flightPill(events, today),
        hotelPill(events, today)
    )
}

private fun weatherPill(events: List<TravelEvent>): HomeTripInfoPill {
    // Find the first event that has any weather data
    val event = events.firstOrNull { event ->
        event.detailValue(ATTR_WEATHER_TEMP_C, ATTR_WEATHER_CONDITION, ATTR_WEATHER_PRECIP_PCT) != null
    }
    
    if (event == null) {
        return HomeTripInfoPill("Weather", "--")
    }

    val temp = event.detailValue(ATTR_WEATHER_TEMP_C)?.toIntOrNull()?.let { "${it}C" }
    val condition = event.detailValue(ATTR_WEATHER_CONDITION)?.let(::shortPillText)
    val precip = event.detailValue(ATTR_WEATHER_PRECIP_PCT)?.toIntOrNull()

    val detail = when {
        temp != null && condition != null -> "$temp $condition"
        temp != null -> temp
        condition != null -> condition
        precip != null -> "Rain $precip%"
        else -> "--"
    }
    return HomeTripInfoPill("Weather", detail)
}

private fun flightPill(events: List<TravelEvent>, today: LocalDate): HomeTripInfoPill {
    val flights = events.filter { it.type.equals("flight", ignoreCase = true) }
    if (flights.isEmpty()) {
        return HomeTripInfoPill("Flight", "--")
    }

    val outbound = flights.firstOrNull { 
        it.details["trip_segment"].equals("outbound", ignoreCase = true) 
    } ?: flights.first()
    
    val returnFlight = flights.firstOrNull { 
        it.details["trip_segment"].equals("return", ignoreCase = true) 
    }

    // If outbound has already landed (or it's past departure time if no land time), show return flight
    val showReturn = hasOutboundLanded(events, today)

    val targetFlight = if (showReturn && returnFlight != null) returnFlight else outbound
    val title = if (showReturn && returnFlight != null) "Return" else "Flight"

    val time = targetFlight.startTime.takeIf { it.isNotBlank() }?.let(::compactDisplayTime)
    val route = listOfNotNull(
        targetFlight.details["origin_airport"]?.takeIf { it.isNotBlank() },
        targetFlight.details["destination_airport"]?.takeIf { it.isNotBlank() }
    ).takeIf { it.size == 2 }?.joinToString("-")

    return HomeTripInfoPill(title, time ?: route ?: "--")
}

private fun hotelPill(events: List<TravelEvent>, today: LocalDate): HomeTripInfoPill {
    val hotel = events.firstOrNull { event ->
        event.type.equals("hotel", ignoreCase = true) ||
            event.type.equals("lodging", ignoreCase = true)
    }
    if (hotel == null) {
        return HomeTripInfoPill("Hotel", "--")
    }

    val checkInDate = parseTripDate(hotel.date)
    val checkInTime = parseTripTime(hotel.detailValue(ATTR_CHECK_IN_TIME, "check_in", "check_in_time") ?: hotel.startTime)
    val now = LocalDateTime.of(today, LocalTime.now())

    // If already checked in, show check-out time
    val showCheckOut = if (checkInDate != null && checkInTime != null) {
        LocalDateTime.of(checkInDate, checkInTime).isBefore(now)
    } else false

    return if (showCheckOut) {
        val checkOut = hotel.detailValue(ATTR_CHECK_OUT_TIME, "check_out", "check_out_time")
            ?.takeIf { it.isNotBlank() }
            ?.let(::compactDisplayTime)
        HomeTripInfoPill("Check-out", checkOut ?: "--")
    } else {
        val checkIn = hotel.detailValue(ATTR_CHECK_IN_TIME, "check_in", "check_in_time")
            ?.takeIf { it.isNotBlank() }
            ?.let(::compactDisplayTime)
        HomeTripInfoPill("Check-in", checkIn ?: "--")
    }
}

private fun hasOutboundLanded(events: List<TravelEvent>, today: LocalDate): Boolean {
    val outboundFlight = events.firstOrNull {
        it.type.equals("flight", ignoreCase = true) && it.details["trip_segment"].equals("outbound", ignoreCase = true)
    } ?: events.firstOrNull { it.type.equals("flight", ignoreCase = true) } ?: return false

    val outboundDate = parseTripDate(outboundFlight.date) ?: return false
    val landsAt = parseTripTime(outboundFlight.endTime) ?: parseTripTime(outboundFlight.startTime) ?: return false
    val now = LocalDateTime.of(today, LocalTime.now())

    return LocalDateTime.of(outboundDate, landsAt).isBefore(now)
}

private fun compactDisplayTime(value: String): String {
    val parsed = parseTripTime(value)
    if (parsed != null) {
        val pattern = if (parsed.minute == 0) "h a" else "h:mm a"
        return parsed.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    }
    return shortPillText(value)
}

private fun shortPillText(value: String, maxChars: Int = 10): String {
    val normalized = value.trim()
    if (normalized.length <= maxChars) return normalized
    return normalized.take(maxChars - 1).trimEnd() + "."
}

private fun eventSortDate(event: TravelEvent): LocalDate {
    return parseTripDate(event.date) ?: LocalDate.MAX
}

private fun eventSortTime(event: TravelEvent): LocalTime {
    return parseTripTime(event.startTime) ?: LocalTime.MAX
}

private fun parseTripDate(value: String): LocalDate? {
    return runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()
}

private fun parseTripTime(value: String): LocalTime? {
    val normalized = value.trim().uppercase(Locale.US)
    if (normalized.isBlank()) return null
    val formatters = listOf(
        DateTimeFormatter.ofPattern("H:mm", Locale.US),
        DateTimeFormatter.ofPattern("HH:mm", Locale.US),
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    )
    return formatters.firstNotNullOfOrNull { formatter ->
        runCatching { LocalTime.parse(normalized, formatter) }.getOrNull()
    }
}

private fun formatMonthDay(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
}
