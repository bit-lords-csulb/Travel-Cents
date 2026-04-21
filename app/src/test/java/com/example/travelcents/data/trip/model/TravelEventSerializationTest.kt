package com.example.travelcents.data.trip.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelEventSerializationTest {

    @Test
    fun toFirestoreMap_omitsLocalImagePathForSharedState() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "hotel",
            itineraryId = "trip-1",
            imageUrl = "https://img.example.com/hero.jpg",
            localImagePath = "/data/user/0/com.example/files/trip_images/trip-1/hero.jpg",
            photoUrls = listOf("https://img.example.com/hero.jpg"),
            details = mapOf(ATTR_HOTEL_NAME to "Seaside Hotel")
        )

        val firestoreMap = event.toFirestoreMap()

        assertFalse(firestoreMap.containsKey("localImagePath"))
        assertEquals("https://img.example.com/hero.jpg", firestoreMap["imageUrl"])
        assertEquals("Seaside Hotel", firestoreMap[ATTR_HOTEL_NAME])
    }

    @Test
    fun eventOptionToMap_omitsLocalImagePathForSharedState() {
        val option = EventOption(
            optionId = "option-1",
            eventId = "event-1",
            imageUrl = "https://img.example.com/hero.jpg",
            localImagePath = "/data/user/0/com.example/files/trip_images/trip-1/hero.jpg",
            details = mapOf(ATTR_BUSINESS_NAME to "Moonstone Cafe")
        )

        val serialized = option.toMap()

        assertFalse(serialized.containsKey("localImagePath"))
        assertEquals("Moonstone Cafe", serialized[ATTR_BUSINESS_NAME])
    }

    @Test
    fun fromFirestoreMap_deserializesWithoutLocalImagePath() {
        val event = TravelEvent.fromFirestoreMap(
            map = mapOf(
                "eventId" to "event-1",
                "type" to "restaurant",
                "itineraryId" to "trip-1",
                "date" to "2026-06-01",
                "startTime" to "19:00",
                "endTime" to "21:00",
                "imageUrl" to "https://img.example.com/hero.jpg",
                "photoUrls" to listOf("https://img.example.com/hero.jpg"),
                ATTR_BUSINESS_NAME to "Moonstone Cafe"
            ),
            documentId = "event-1",
            fallbackItineraryId = "trip-1"
        )

        assertEquals("", event.localImagePath)
        assertEquals("https://img.example.com/hero.jpg", event.imageUrl)
        assertEquals("Moonstone Cafe", event.details[ATTR_BUSINESS_NAME])
    }

    @Test
    fun fromCacheMap_still_readsLegacyLocalImagePathShape() {
        val event = TravelEvent.fromCacheMap(
            mapOf(
                "eventId" to "event-1",
                "type" to "hotel",
                "imageUrl" to "file:/data/user/0/com.example/files/trip_images/trip-1/hero.jpg",
                "photoUrls" to emptyList<String>(),
                ATTR_HOTEL_NAME to "Seaside Hotel",
                "options" to emptyList<Map<String, Any>>()
            )
        )

        assertTrue(event.localImagePath.startsWith("file:/"))
        assertEquals("", event.imageUrl)
        assertEquals("Seaside Hotel", event.details[ATTR_HOTEL_NAME])
    }

    @Test
    fun eventOptionFromMap_still_readsLegacyLocalImagePathShape() {
        val option = EventOption.fromMap(
            mapOf(
                "optionId" to "option-1",
                "eventId" to "event-1",
                "source" to "yelp",
                "imageUrl" to "file:/data/user/0/com.example/files/trip_images/trip-1/hero.jpg",
                ATTR_BUSINESS_NAME to "Moonstone Cafe"
            )
        )

        assertTrue(option.localImagePath.startsWith("file:/"))
        assertEquals("", option.imageUrl)
        assertEquals("Moonstone Cafe", option.details[ATTR_BUSINESS_NAME])
    }
}
