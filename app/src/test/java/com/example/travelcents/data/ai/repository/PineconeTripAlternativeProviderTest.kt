package com.example.travelcents.data.ai.repository

import com.example.travelcents.data.trip.advisory.AdvisoryReason
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PineconeTripAlternativeProviderTest {

    @Test
    fun alternativesFor_buildsRainQueryForPineconeBackedIndoorAlternatives() = runBlocking {
        var capturedRequest: ActivityAlternativeSearchRequest? = null
        val provider = PineconeTripAlternativeProvider { request ->
            capturedRequest = request
            listOf(
                EventOption(
                    optionId = "real-1",
                    source = "pinecone_advisory",
                    details = mapOf(ATTR_BUSINESS_NAME to "Real Indoor Museum")
                )
            )
        }

        val options = provider.alternativesFor(
            trip = trip(destination = "Nairobi"),
            event = activity(),
            reason = AdvisoryReason.RAIN_OUTDOOR_ACTIVITY
        )

        assertEquals(1, options.size)
        assertEquals("pinecone_advisory", options.first().source)
        val request = checkNotNull(capturedRequest)
        assertEquals("Nairobi", request.destination)
        assertEquals("Safari Drive", request.currentActivityTitle)
        assertEquals(AdvisoryReason.RAIN_OUTDOOR_ACTIVITY.name, request.reason)
        assertEquals(12, request.limit)
        assertTrue(request.query.contains("indoor rainy day activity"))
        assertTrue(request.query.contains("Safari Drive"))
    }

    @Test
    fun alternativesFor_skipsNetworkWhenDestinationIsBlank() = runBlocking {
        var fetchCalled = false
        val provider = PineconeTripAlternativeProvider {
            fetchCalled = true
            emptyList()
        }

        val options = provider.alternativesFor(
            trip = trip(destination = ""),
            event = activity(),
            reason = AdvisoryReason.RAIN_OUTDOOR_ACTIVITY
        )

        assertTrue(options.isEmpty())
        assertFalse(fetchCalled)
    }

    private fun trip(destination: String): Itinerary {
        return Itinerary(
            itineraryId = "trip-1",
            userId = "user-1",
            tripName = "Demo Trip",
            destination = destination,
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
    }

    private fun activity(): TravelEvent {
        return TravelEvent(
            eventId = "safari",
            type = "activity",
            itineraryId = "trip-1",
            date = "2026-06-01",
            startTime = "14:00",
            endTime = "16:00",
            details = mapOf(
                "title" to "Safari Drive",
                "activity_name" to "Safari Drive",
                ATTR_BUSINESS_NAME to "Safari Drive",
                "description" to "Outdoor wildlife drive"
            )
        )
    }
}
