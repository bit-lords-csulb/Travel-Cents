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
    fun fromFirestoreMap_normalizesLegacyGroupEventShape() {
        val event = TravelEvent.fromFirestoreMap(
            map = mapOf(
                "title" to "Moonstone Cafe",
                "description" to "Late dinner",
                "location" to "12 Market St",
                "date" to "2026-06-01",
                "startTime" to "19:00",
                "photoUrl" to "https://img.example.com/group-photo.jpg",
                "yelpId" to "moonstone-cafe",
                "yelpUrl" to "https://yelp.example.com/moonstone",
                "yelpCategory" to "Restaurants",
                "yelpCategories" to listOf("Restaurants", "Coffee"),
                "yelpRating" to 4.5,
                "yelpReviewCount" to 120L,
                "yelpImageUrl" to "https://img.example.com/yelp-photo.jpg",
                "latitude" to 33.7701,
                "longitude" to -118.1937,
                "staticMapUrl" to "https://maps.example.com/static.png",
                "staticMapProvider" to "mapbox_staticmap"
            ),
            documentId = "group-event-1",
            fallbackItineraryId = "trip-1"
        )

        assertEquals("group-event-1", event.eventId)
        assertEquals("restaurant", event.type)
        assertEquals("https://img.example.com/group-photo.jpg", event.imageUrl)
        assertEquals(
            listOf(
                "https://img.example.com/group-photo.jpg",
                "https://img.example.com/yelp-photo.jpg"
            ),
            event.photoUrls
        )
        assertEquals("Moonstone Cafe", event.details["title"])
        assertEquals("Moonstone Cafe", event.details["name"])
        assertEquals("Moonstone Cafe", event.details[ATTR_BUSINESS_NAME])
        assertEquals("12 Market St", event.details[ATTR_BUSINESS_ADDRESS])
        assertEquals("moonstone-cafe", event.details[DETAIL_YELP_ID])
        assertEquals("https://yelp.example.com/moonstone", event.details[ATTR_YELP_URL])
        assertEquals("https://img.example.com/yelp-photo.jpg", event.details[ATTR_PROFILE_PHOTO_URL])
        assertEquals("Restaurants", event.details[ATTR_CATEGORIES])
        assertEquals("4.5", event.details[ATTR_AVERAGE_RATING])
        assertEquals("120", event.details[ATTR_REVIEW_COUNT])
        assertEquals("33.7701", event.details[ATTR_LATITUDE])
        assertEquals("-118.1937", event.details[ATTR_LONGITUDE])
        assertEquals("https://maps.example.com/static.png", event.details[ATTR_STATIC_MAP_URL])
        assertEquals("mapbox_staticmap", event.details[ATTR_STATIC_MAP_PROVIDER])
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

    @Test
    fun eventOptionFromMap_normalizesLegacyImageAndYelpAliases() {
        val option = EventOption.fromMap(
            mapOf(
                "optionId" to "option-1",
                "eventId" to "event-1",
                "source" to "yelp",
                "selected" to true,
                "title" to "Morning Market",
                "location" to "Pier 4",
                "photoUrl" to "https://img.example.com/market.jpg",
                "yelpId" to "morning-market",
                "yelpUrl" to "https://yelp.example.com/market",
                "yelpRating" to 4.2,
                "yelpReviewCount" to 77,
                "lat" to 40.7128,
                "lng" to -74.0060,
                "static_map_url" to "https://maps.example.com/market.png",
                "static_map_provider" to "mapbox_staticmap"
            )
        )

        assertEquals("https://img.example.com/market.jpg", option.imageUrl)
        assertEquals(listOf("https://img.example.com/market.jpg"), option.photoUrls)
        assertEquals("Morning Market", option.details[ATTR_BUSINESS_NAME])
        assertEquals("Pier 4", option.details[ATTR_BUSINESS_ADDRESS])
        assertEquals("morning-market", option.details[DETAIL_YELP_ID])
        assertEquals("https://yelp.example.com/market", option.details[ATTR_YELP_URL])
        assertEquals("4.2", option.details[ATTR_AVERAGE_RATING])
        assertEquals("77", option.details[ATTR_REVIEW_COUNT])
        assertEquals("40.7128", option.details[ATTR_LATITUDE])
        assertEquals("-74.006", option.details[ATTR_LONGITUDE])
        assertEquals("https://maps.example.com/market.png", option.details[ATTR_STATIC_MAP_URL])
        assertEquals("mapbox_staticmap", option.details[ATTR_STATIC_MAP_PROVIDER])
    }
}
