package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.model.ATTR_ACTIVITY_ENVIRONMENT
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_ENVIRONMENT_CONFIDENCE
import com.example.travelcents.data.trip.model.ATTR_WEATHER_SENSITIVITY
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripAdvisoryEngineTest {

    private val trip = Itinerary(
        itineraryId = "trip-1",
        userId = "user-1",
        tripName = "Demo Trip",
        destination = "Nairobi",
        origin = "Los Angeles",
        dateFrom = "2026-06-01",
        dateTo = "2026-06-03",
        durationDays = 3,
        currency = "USD",
        travelStyle = "comfort",
        adults = 2,
        children = 0,
        createdAt = "2026-05-01T00:00:00Z",
        status = "draft",
        eventIds = emptyList()
    )

    private val engine = TripAdvisoryEngine(
        weatherProvider = DummyTripWeatherContextProvider(),
        transportProvider = DummyTripTransportContextProvider(),
        alternativeProvider = DummyTripAlternativeProvider(),
        nowEpochMs = { 123L }
    )

    @Test
    fun evaluate_returnsRainAdvisoryForOutdoorSafari() = runBlocking {
        val event = activity(
            eventId = "safari",
            title = "Safari Drive",
            environment = "outdoor",
            sensitivity = "rain",
            confidence = "high"
        )

        val advisories = engine.evaluate(trip, listOf(event), emptyMap())

        assertEquals(1, advisories.size)
        val advisory = advisories.first()
        assertEquals(AdvisoryReason.RAIN_OUTDOOR_ACTIVITY, advisory.reason)
        assertEquals(AdvisorySeverity.HIGH, advisory.severity)
        assertTrue(advisory.suggestedOptions.size >= 2)
        assertTrue(advisory.suggestedOptions.all { it.source == "dummy_advisory" })
    }

    @Test
    fun evaluate_rotatesRainAlternativesAcrossRainAdvisories() = runBlocking {
        val first = activity(
            eventId = "outdoor-1",
            title = "Safari Drive",
            environment = "outdoor",
            sensitivity = "rain",
            confidence = "high"
        )
        val second = activity(
            eventId = "outdoor-2",
            title = "Walking Tour",
            environment = "outdoor",
            sensitivity = "rain",
            confidence = "high"
        )

        val rainAdvisories = engine.evaluate(trip, listOf(first, second), emptyMap())
            .filter { it.reason == AdvisoryReason.RAIN_OUTDOOR_ACTIVITY }
            .sortedBy { it.eventId }

        assertEquals(2, rainAdvisories.size)
        val firstTitles = rainAdvisories[0].suggestedOptions.map(::optionTitle).toSet()
        val secondTitles = rainAdvisories[1].suggestedOptions.map(::optionTitle).toSet()
        assertEquals(3, firstTitles.size)
        assertEquals(3, secondTitles.size)
        assertTrue(firstTitles.intersect(secondTitles).isEmpty())
    }

    @Test
    fun evaluate_filtersAlreadySelectedAlternativeFromLaterAdvisories() = runBlocking {
        val existingIndoor = activity(
            eventId = "chosen-slot",
            title = "Indoor Culture Museum",
            environment = "indoor",
            sensitivity = "none",
            confidence = "high"
        )
        val selectedOption = EventOption(
            optionId = "chosen-option",
            eventId = existingIndoor.eventId,
            tripId = trip.itineraryId,
            source = "dummy_advisory",
            selected = true,
            details = mapOf(
                "title" to "Indoor Culture Museum",
                "activity_name" to "Indoor Culture Museum",
                ATTR_BUSINESS_NAME to "Indoor Culture Museum"
            )
        )
        val outdoor = activity(
            eventId = "outdoor",
            title = "Safari Drive",
            environment = "outdoor",
            sensitivity = "rain",
            confidence = "high"
        )

        val rainAdvisory = engine.evaluate(
            trip = trip,
            events = listOf(existingIndoor, outdoor),
            optionsByEvent = mapOf(existingIndoor.eventId to listOf(selectedOption))
        ).first { it.eventId == outdoor.eventId && it.reason == AdvisoryReason.RAIN_OUTDOOR_ACTIVITY }

        assertEquals(3, rainAdvisory.suggestedOptions.size)
        assertTrue(rainAdvisory.suggestedOptions.none { optionTitle(it) == "Indoor Culture Museum" })
    }

    @Test
    fun evaluate_filtersSessionAcceptedAlternativeNames() = runBlocking {
        val outdoor = activity(
            eventId = "outdoor",
            title = "Safari Drive",
            environment = "outdoor",
            sensitivity = "rain",
            confidence = "high"
        )

        val rainAdvisory = engine.evaluate(
            trip = trip,
            events = listOf(outdoor),
            optionsByEvent = emptyMap(),
            excludedSuggestionNames = setOf("Covered Market Hall")
        ).first { it.reason == AdvisoryReason.RAIN_OUTDOOR_ACTIVITY }

        assertEquals(3, rainAdvisory.suggestedOptions.size)
        assertTrue(rainAdvisory.suggestedOptions.none { optionTitle(it) == "Covered Market Hall" })
    }

    @Test
    fun evaluate_skipsIndoorMuseumDuringRainScenario() = runBlocking {
        val event = activity(
            eventId = "museum",
            title = "National Museum",
            environment = "indoor",
            sensitivity = "none",
            confidence = "high"
        )

        val advisories = engine.evaluate(trip, listOf(event), emptyMap())

        assertTrue(advisories.isEmpty())
    }

    @Test
    fun evaluate_usesKeywordInferenceWhenOptionalMetadataMissing() = runBlocking {
        val event = activity(
            eventId = "hike",
            title = "Mountain Hike",
            environment = null,
            sensitivity = null,
            confidence = null
        )

        val advisories = engine.evaluate(trip, listOf(event), emptyMap())

        assertTrue(advisories.any { it.reason == AdvisoryReason.RAIN_OUTDOOR_ACTIVITY })
    }

    @Test
    fun evaluate_skipsUnknownActivityWithoutMetadataOrKeywords() = runBlocking {
        val event = activity(
            eventId = "unknown",
            title = "Local Experience",
            environment = null,
            sensitivity = null,
            confidence = null
        )

        val advisories = engine.evaluate(trip, listOf(event), emptyMap())

        assertTrue(advisories.isEmpty())
    }

    @Test
    fun evaluate_returnsTransportDelayWhenTransferIsTooTight() = runBlocking {
        val previous = activity(
            eventId = "lunch",
            title = "Lunch",
            startTime = "12:00",
            endTime = "13:00",
            environment = "indoor",
            sensitivity = "none",
            confidence = "high"
        )
        val next = activity(
            eventId = "remote-safari",
            title = "Remote Safari Check-In",
            startTime = "13:25",
            environment = "outdoor",
            sensitivity = "rain",
            confidence = "high"
        )

        val advisories = engine.evaluate(trip, listOf(previous, next), emptyMap())

        assertTrue(advisories.any { it.reason == AdvisoryReason.TRANSIT_DELAY })
    }

    private fun activity(
        eventId: String,
        title: String,
        startTime: String = "14:00",
        endTime: String = "16:00",
        environment: String?,
        sensitivity: String?,
        confidence: String?
    ): TravelEvent {
        return TravelEvent(
            eventId = eventId,
            type = "activity",
            itineraryId = trip.itineraryId,
            date = "2026-06-01",
            startTime = startTime,
            endTime = endTime,
            details = buildMap {
                put("title", title)
                put("activity_name", title)
                put(ATTR_BUSINESS_NAME, title)
                environment?.let { put(ATTR_ACTIVITY_ENVIRONMENT, it) }
                sensitivity?.let { put(ATTR_WEATHER_SENSITIVITY, it) }
                confidence?.let { put(ATTR_ENVIRONMENT_CONFIDENCE, it) }
            }
        )
    }

    private fun optionTitle(option: EventOption): String {
        return option.details[ATTR_BUSINESS_NAME]
            ?: option.details["activity_name"]
            ?: option.details["title"]
            ?: ""
    }
}
