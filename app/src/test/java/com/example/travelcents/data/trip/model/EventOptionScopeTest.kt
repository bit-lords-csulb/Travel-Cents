package com.example.travelcents.data.trip.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EventOptionScopeTest {
    @Test
    fun scopedTo_persistsTripScopeInSerializedMap() {
        val option = EventOption(
            optionId = "option-1",
            source = "yelp",
            selected = true
        ).scopedTo(
            ownerUid = "owner-123",
            tripId = "trip-456",
            eventId = "event-789"
        )

        val restored = EventOption.fromMap(option.toMap())

        assertEquals("owner-123", restored.ownerUid)
        assertEquals("trip-456", restored.tripId)
        assertEquals("event-789", restored.eventId)
    }
}
