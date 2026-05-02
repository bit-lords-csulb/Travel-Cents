package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.model.ATTR_ACTIVITY_ENVIRONMENT
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_ENVIRONMENT_CONFIDENCE
import com.example.travelcents.data.trip.model.ATTR_WEATHER_SENSITIVITY
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
}
