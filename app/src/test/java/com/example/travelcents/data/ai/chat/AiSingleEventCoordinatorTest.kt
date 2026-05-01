package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_TICKET_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MAX
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MIN
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSingleEventCoordinatorTest {

    @Test
    fun buildGroundingContext_returnsNull_whenMessageHasNoEventIntent() = runBlocking {
        var didSearch = false
        val coordinator = AiSingleEventCoordinator(
            searchEvents = { _, _, _, _, _ ->
                didSearch = true
                emptyList()
            },
            isSearchAvailable = { true },
            clock = fixedClock()
        )

        val grounding = coordinator.buildGroundingContext(
            userMessage = "Find me some good ramen in Nashville",
            intakeProfile = AiTripIntakeProfile(
                destination = "Nashville, TN",
                dateWindow = "June 2026"
            ),
            profile = AiTravelerProfile(destination = "Nashville, TN")
        )

        assertNull(grounding)
        assertFalse(didSearch)
    }

    @Test
    fun buildGroundingContext_formatsTicketmasterResults_forConcertQueries() = runBlocking {
        var capturedLocation: String? = null
        var capturedStartDate: String? = null
        var capturedEndDate: String? = null
        var capturedKeyword: String? = null
        var capturedClassification: String? = null
        val coordinator = AiSingleEventCoordinator(
            searchEvents = { location, startDate, endDate, keyword, classification ->
                capturedLocation = location
                capturedStartDate = startDate
                capturedEndDate = endDate
                capturedKeyword = keyword
                capturedClassification = classification
                listOf(
                    ticketmasterEvent(
                        eventId = "show-1",
                        name = "Maren Morris",
                        date = "2026-06-12",
                        startTime = "19:30",
                        venue = "Ryman Auditorium",
                        address = "116 Rep. John Lewis Way N, Nashville, TN",
                        bookingUrl = "https://tickets.example.com/maren",
                        minPrice = "49",
                        maxPrice = "129"
                    ),
                    ticketmasterEvent(
                        eventId = "show-2",
                        name = "Jason Isbell",
                        date = "2026-06-18",
                        startTime = "20:00",
                        venue = "Ascend Amphitheater",
                        address = "310 1st Ave S, Nashville, TN",
                        bookingUrl = "https://tickets.example.com/jason",
                        minPrice = "59",
                        maxPrice = "149"
                    )
                )
            },
            isSearchAvailable = { true },
            clock = fixedClock()
        )

        val grounding = coordinator.buildGroundingContext(
            userMessage = "Any good concerts while I'm in Nashville?",
            intakeProfile = AiTripIntakeProfile(
                destination = "Nashville, TN",
                dateWindow = "June 2026"
            ),
            profile = AiTravelerProfile(destination = "Nashville, TN")
        )

        assertNotNull(grounding)
        assertEquals("Nashville, TN", capturedLocation)
        assertEquals("2026-06-01", capturedStartDate)
        assertEquals("2026-06-30", capturedEndDate)
        assertNull(capturedKeyword)
        assertEquals("Music", capturedClassification)
        assertTrue(grounding!!.contains("Ticketmaster live event grounding for this turn:"))
        assertTrue(grounding.contains("Destination: Nashville, TN"))
        assertTrue(grounding.contains("Date window: 2026-06-01 to 2026-06-30"))
        assertTrue(grounding.contains("Maren Morris"))
        assertTrue(grounding.contains("Ryman Auditorium"))
        assertTrue(grounding.contains("https://tickets.example.com/maren"))
        assertTrue(grounding.contains("Jason Isbell"))
    }

    @Test
    fun buildGroundingContext_includesNoResultsInstruction_forWeekendTicketQueries() = runBlocking {
        val coordinator = AiSingleEventCoordinator(
            searchEvents = { _, _, _, _, _ -> emptyList() },
            isSearchAvailable = { true },
            clock = fixedClock()
        )

        val grounding = coordinator.buildGroundingContext(
            userMessage = "Any tickets this weekend in Nashville?",
            intakeProfile = AiTripIntakeProfile(destination = "Nashville, TN"),
            profile = AiTravelerProfile(destination = "Nashville, TN")
        )

        assertNotNull(grounding)
        assertTrue(grounding!!.contains("Date window: 2026-04-25 to 2026-04-26"))
        assertTrue(grounding.contains("Ticketmaster returned no matching events"))
    }

    private fun ticketmasterEvent(
        eventId: String,
        name: String,
        date: String,
        startTime: String,
        venue: String,
        address: String,
        bookingUrl: String,
        minPrice: String,
        maxPrice: String
    ): TravelEvent {
        return TravelEvent(
            eventId = eventId,
            type = "activity",
            itineraryId = "",
            date = date,
            startTime = startTime,
            details = mapOf(
                ATTR_BUSINESS_NAME to name,
                ATTR_VENUE_NAME to venue,
                ATTR_BUSINESS_ADDRESS to address,
                ATTR_BOOKING_URL to bookingUrl,
                ATTR_TICKET_PRICE_MIN to minPrice,
                ATTR_TICKET_PRICE_MAX to maxPrice,
                ATTR_TICKET_CURRENCY to "USD"
            )
        )
    }

    private fun fixedClock(): Clock {
        return Clock.fixed(Instant.parse("2026-04-23T12:00:00Z"), ZoneOffset.UTC)
    }
}
