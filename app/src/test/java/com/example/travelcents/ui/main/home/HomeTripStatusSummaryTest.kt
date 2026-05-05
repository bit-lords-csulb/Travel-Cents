package com.example.travelcents.ui.main.home

import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_WEATHER_CONDITION
import com.example.travelcents.data.trip.model.ATTR_WEATHER_TEMP_C
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeTripStatusSummaryTest {

    private val today: LocalDate = LocalDate.of(2026, 5, 4)

    @Test
    fun resolveHomeStatusTrip_usesSelectedCarouselTrip() {
        val earlier = itinerary(id = "trip-1", dateFrom = "2026-06-01", dateTo = "2026-06-05")
        val selected = itinerary(id = "trip-2", dateFrom = "2026-07-01", dateTo = "2026-07-05")

        val resolved = resolveHomeStatusTrip(
            trips = listOf(earlier, selected),
            selectedTrip = selected,
            today = today
        )

        assertEquals("trip-2", resolved?.itineraryId)
    }

    @Test
    fun resolveHomeStatusTrip_fallsBackWhenSelectedTripEnded() {
        val ended = itinerary(id = "trip-1", dateFrom = "2026-04-01", dateTo = "2026-04-05")
        val upcoming = itinerary(id = "trip-2", dateFrom = "2026-06-01", dateTo = "2026-06-05")

        val resolved = resolveHomeStatusTrip(
            trips = listOf(ended, upcoming),
            selectedTrip = ended,
            today = today
        )

        assertEquals("trip-2", resolved?.itineraryId)
    }

    @Test
    fun resolveHomeStatusTrip_returnsNullWhenNoEligibleTrips() {
        val archived = itinerary(
            id = "trip-1",
            dateFrom = "2026-06-01",
            dateTo = "2026-06-05",
            status = "archived"
        )
        val ended = itinerary(id = "trip-2", dateFrom = "2026-04-01", dateTo = "2026-04-05")

        val resolved = resolveHomeStatusTrip(
            trips = listOf(archived, ended),
            selectedTrip = archived,
            today = today
        )

        assertNull(resolved)
    }

    @Test
    fun buildHomeTripStatusSummary_futureTripWithEventsIsUpcoming() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-14", dateTo = "2026-05-18")
        val events = listOf(
            event(id = "flight-1", tripId = "trip-1", type = "flight", date = "2026-05-14", time = "9:00 AM", title = "Outbound flight"),
            event(id = "hotel-1", tripId = "trip-1", type = "hotel", date = "2026-05-14", time = "3:00 PM", title = "Grand Hotel")
        )

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = events,
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals(HomeTripStatusLevel.UPCOMING, summary.level)
        assertEquals("Upcoming", summary.badgeText)
        assertTrue(summary.dateLine.contains("Starts in 10 days"))
        assertEquals("Next: Outbound flight", summary.nextEventTitle)
        assertEquals(listOf("Weather", "Flight", "Hotel"), summary.infoPills.map { it.title })
        assertEquals("--", summary.infoPills[0].detail)
        assertEquals("9 AM", summary.infoPills[1].detail)
        assertEquals("Grand Hot.", summary.infoPills[2].detail)
    }

    @Test
    fun buildHomeTripStatusSummary_startDateTripWithEventsIsReady() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-04", dateTo = "2026-05-08")
        val events = listOf(
            event(id = "flight-1", tripId = "trip-1", type = "flight", date = "2026-05-04", time = "9:00 AM", title = "Outbound flight"),
            event(id = "hotel-1", tripId = "trip-1", type = "hotel", date = "2026-05-04", time = "3:00 PM", title = "Grand Hotel")
        )

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = events,
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals(HomeTripStatusLevel.READY, summary.level)
        assertEquals("Ready", summary.badgeText)
        assertEquals("In progress - ends May 8", summary.dateLine)
    }

    @Test
    fun buildHomeTripStatusSummary_currentTripIsInProgress() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-01", dateTo = "2026-05-08")
        val events = listOf(
            event(id = "hotel-1", tripId = "trip-1", type = "hotel", date = "2026-05-04", time = "8:00 AM", title = "Breakfast")
        )

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = events,
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals(HomeTripStatusLevel.IN_PROGRESS, summary.level)
        assertEquals("In progress - ends May 8", summary.dateLine)
    }

    @Test
    fun buildHomeTripStatusSummary_weatherPillUsesEnrichedForecast() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-04", dateTo = "2026-05-08")
        val events = listOf(
            event(
                id = "restaurant-1",
                tripId = "trip-1",
                type = "restaurant",
                date = "2026-05-04",
                time = "7:00 PM",
                title = "Dinner",
                details = mapOf(
                    ATTR_WEATHER_TEMP_C to "23",
                    ATTR_WEATHER_CONDITION to "Clear"
                )
            )
        )

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = events,
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals("Weather", summary.infoPills[0].title)
        assertEquals("23C", summary.infoPills[0].detail)
    }

    @Test
    fun buildHomeTripStatusSummary_flightPillPrefersTimeThenRoute() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-04", dateTo = "2026-05-08")
        val flightWithTime = event(
            id = "flight-1",
            tripId = "trip-1",
            type = "flight",
            date = "2026-05-04",
            time = "9:00 AM",
            title = "Outbound flight",
            details = mapOf("origin_airport" to "LAX", "destination_airport" to "CDG")
        )
        val flightWithoutTime = flightWithTime.copy(startTime = "")

        val timeSummary = buildHomeTripStatusSummary(
            trip = trip,
            events = listOf(flightWithTime),
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )
        val routeSummary = buildHomeTripStatusSummary(
            trip = trip,
            events = listOf(flightWithoutTime),
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals("Flight", timeSummary.infoPills[1].title)
        assertEquals("9 AM", timeSummary.infoPills[1].detail)
        assertEquals("LAX-CDG", routeSummary.infoPills[1].detail)
    }

    @Test
    fun buildHomeTripStatusSummary_hotelPillPrefersCheckInThenName() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-04", dateTo = "2026-05-08")
        val hotelWithCheckIn = event(
            id = "hotel-1",
            tripId = "trip-1",
            type = "hotel",
            date = "2026-05-04",
            time = "3:00 PM",
            title = "Grand Hotel",
            details = mapOf(ATTR_HOTEL_NAME to "Grand Hotel", ATTR_CHECK_IN_TIME to "3:00 PM")
        )
        val hotelWithoutCheckIn = hotelWithCheckIn.copy(details = mapOf(ATTR_HOTEL_NAME to "Grand Hotel"))

        val checkInSummary = buildHomeTripStatusSummary(
            trip = trip,
            events = listOf(hotelWithCheckIn),
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )
        val nameSummary = buildHomeTripStatusSummary(
            trip = trip,
            events = listOf(hotelWithoutCheckIn),
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals("Hotel", checkInSummary.infoPills[2].title)
        assertEquals("3 PM", checkInSummary.infoPills[2].detail)
        assertEquals("Grand Hot.", nameSummary.infoPills[2].detail)
    }

    @Test
    fun buildHomeTripStatusSummary_choosesNextEventByDateAndTime() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-04", dateTo = "2026-05-08")
        val events = listOf(
            event(id = "late", tripId = "trip-1", type = "activity", date = "2026-05-04", time = "6:00 PM", title = "Dinner"),
            event(id = "early", tripId = "trip-1", type = "activity", date = "2026-05-04", time = "9:00 AM", title = "Museum")
        )

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = events,
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals("Next: Museum", summary.nextEventTitle)
        assertEquals("Today at 9:00 AM - Activity", summary.nextEventSubtitle)
    }

    @Test
    fun buildHomeTripStatusSummary_missingEventsNeedsAttention() {
        val trip = itinerary(id = "trip-1", dateFrom = "2026-05-14", dateTo = "2026-05-18")

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = emptyList(),
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals(HomeTripStatusLevel.NEEDS_ATTENTION, summary.level)
        assertEquals("No itinerary items yet", summary.nextEventTitle)
        assertEquals("--", summary.infoPills[0].detail)
        assertEquals("--", summary.infoPills[1].detail)
        assertEquals("--", summary.infoPills[2].detail)
    }

    @Test
    fun buildHomeTripStatusSummary_invalidDatesDoNotCrash() {
        val trip = itinerary(id = "trip-1", dateFrom = "", dateTo = "")
        val events = listOf(
            event(id = "event-1", tripId = "trip-1", type = "activity", date = "2026-05-14", time = "9:00 AM", title = "Museum")
        )

        val summary = buildHomeTripStatusSummary(
            trip = trip,
            events = events,
            isTripLoading = false,
            isEventDataLoading = false,
            today = today
        )

        assertEquals(HomeTripStatusLevel.NEEDS_ATTENTION, summary.level)
        assertEquals("Dates need review", summary.dateLine)
    }

    private fun itinerary(
        id: String,
        dateFrom: String,
        dateTo: String,
        status: String = "active"
    ): Itinerary {
        return Itinerary(
            itineraryId = id,
            userId = "owner-1",
            tripName = "Paris Trip",
            destination = "Paris",
            origin = "Los Angeles",
            originIata = "LAX",
            destinationIata = "CDG",
            dateFrom = dateFrom,
            dateTo = dateTo,
            durationDays = 4,
            currency = "USD",
            travelStyle = "comfort",
            adults = 2,
            children = 0,
            createdAt = "2026-01-01T00:00:00Z",
            status = status,
            eventIds = emptyList(),
            ownerUid = "owner-1"
        )
    }

    private fun event(
        id: String,
        tripId: String,
        type: String,
        date: String,
        time: String,
        title: String,
        details: Map<String, String> = emptyMap()
    ): TravelEvent {
        return TravelEvent(
            eventId = id,
            type = type,
            itineraryId = tripId,
            date = date,
            startTime = time,
            details = mapOf("title" to title) + details
        )
    }
}
