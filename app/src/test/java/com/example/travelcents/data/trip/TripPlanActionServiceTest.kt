package com.example.travelcents.data.trip

import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanActionServiceTest {

    private val tripKey = TripKey(ownerUid = "owner", tripId = "trip_123")

    @Test
    fun addEvent_upsertsEventAndReturnsConfirmation() {
        val gateway = FakeTripPlanActionRemoteGateway()
        val service = TripPlanActionService(remoteGateway = gateway)
        val event = baseEvent()

        val result = runSuspend {
            service.addEvent(tripKey = tripKey, event = event)
        }

        assertEquals(event, gateway.upsertedEvent)
        assertEquals("Plan added to your trip.", result.confirmationMessage)
    }

    @Test
    fun replaceSelectedOption_persistsUpdatedEventAndOptions() {
        val gateway = FakeTripPlanActionRemoteGateway()
        val service = TripPlanActionService(remoteGateway = gateway)
        val event = baseEvent()
        val options = listOf(
            EventOption(
                optionId = "old",
                eventId = event.eventId,
                source = "llm",
                selected = true,
                details = mapOf(ATTR_BUSINESS_NAME to "Old Place", "title" to "Old Place")
            ),
            EventOption(
                optionId = "new",
                eventId = event.eventId,
                source = "llm",
                selected = false,
                details = mapOf(ATTR_BUSINESS_NAME to "New Place", "title" to "New Place")
            )
        )

        val result = runSuspend {
            service.replaceSelectedOption(
                tripKey = tripKey,
                event = event,
                existingOptions = options,
                optionId = "new",
                persistOptions = true
            )
        }

        assertEquals(event.eventId, gateway.persistedEventId)
        assertEquals("new", result.event?.selectedOptionId)
        assertEquals("New Place", result.event?.details?.get("title"))
        assertTrue(result.options.first { it.optionId == "new" }.selected)
        assertFalse(result.options.first { it.optionId == "old" }.selected)
        assertEquals("Updated this trip option.", result.confirmationMessage)
    }

    @Test
    fun replaceSelectedOption_canPersistAsEventOnly() {
        val gateway = FakeTripPlanActionRemoteGateway()
        val service = TripPlanActionService(remoteGateway = gateway)
        val event = baseEvent()
        val options = listOf(
            EventOption(optionId = "old", eventId = event.eventId, source = "yelp", selected = true),
            EventOption(
                optionId = "new",
                eventId = event.eventId,
                source = "yelp",
                selected = false,
                details = mapOf(ATTR_BUSINESS_NAME to "New Yelp Place", "title" to "New Yelp Place")
            )
        )

        val result = runSuspend {
            service.replaceSelectedOption(
                tripKey = tripKey,
                event = event,
                existingOptions = options,
                optionId = "new",
                persistOptions = false
            )
        }

        assertNotNull(gateway.upsertedEvent)
        assertEquals(null, gateway.persistedEvent)
        assertEquals("new", result.event?.selectedOptionId)
    }

    @Test
    fun saveOption_appendsUnselectedOptionAndPersistsOnlyThatOption() {
        val gateway = FakeTripPlanActionRemoteGateway()
        val service = TripPlanActionService(remoteGateway = gateway)
        val event = baseEvent().copy(selectedOptionId = "selected")
        val existingOptions = listOf(
            EventOption(optionId = "selected", eventId = event.eventId, source = "llm", selected = true)
        )
        val savedOption = EventOption(
            optionId = "saved",
            source = "llm",
            selected = true,
            details = mapOf(ATTR_BUSINESS_NAME to "Saved Place")
        )

        val result = runSuspend {
            service.saveOption(
                tripKey = tripKey,
                event = event,
                existingOptions = existingOptions,
                option = savedOption
            )
        }

        assertEquals(setOf("saved"), gateway.updatedOptionIds)
        assertEquals(2, result.options.size)
        assertFalse(result.options.first { it.optionId == "saved" }.selected)
        assertEquals("Saved as an option for this slot.", result.confirmationMessage)
    }

    private fun baseEvent(): TravelEvent {
        return TravelEvent(
            eventId = "event_123",
            type = "restaurant",
            itineraryId = tripKey.tripId,
            details = mapOf("title" to "Original Place")
        )
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }
}

private class FakeTripPlanActionRemoteGateway : TripPlanActionRemoteGateway {
    var upsertedEvent: TravelEvent? = null
    var deletedEventId: String? = null
    var persistedEventId: String? = null
    var persistedEvent: TravelEvent? = null
    var persistedOptions: List<EventOption> = emptyList()
    var updatedOptionIds: Set<String>? = null

    override suspend fun upsertEvent(
        tripKey: TripKey,
        event: TravelEvent
    ) {
        upsertedEvent = event
    }

    override suspend fun deleteEvent(
        tripKey: TripKey,
        eventId: String
    ) {
        deletedEventId = eventId
    }

    override suspend fun persistEventAndOptions(
        tripKey: TripKey,
        eventId: String,
        event: TravelEvent,
        options: List<EventOption>,
        updatedOptionIds: Set<String>?
    ) {
        persistedEventId = eventId
        persistedEvent = event
        persistedOptions = options
        this.updatedOptionIds = updatedOptionIds
    }
}
