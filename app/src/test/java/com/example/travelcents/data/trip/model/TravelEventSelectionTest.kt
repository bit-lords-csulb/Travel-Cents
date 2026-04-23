package com.example.travelcents.data.trip.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TravelEventSelectionTest {

    @Test
    fun withSelectedOption_forHotelCopiesHeroGalleryAndName() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "hotel",
            itineraryId = "trip-1",
            imageUrl = "https://img.example.com/original-hero.jpg",
            localImagePath = "/data/user/0/com.example/files/trip_images/trip-1/original-hero.jpg",
            photoUrls = listOf(
                "https://img.example.com/original-hero.jpg",
                "https://img.example.com/original-gallery.jpg"
            ),
            details = mapOf(
                ATTR_HOTEL_NAME to "Original Hotel",
                "title" to "Original Hotel"
            )
        )
        val option = EventOption(
            optionId = "option-1",
            eventId = "event-1",
            source = "serp",
            selected = true,
            imageUrl = "https://img.example.com/selected-hero.jpg",
            photoUrls = listOf(
                "https://img.example.com/selected-hero.jpg",
                "https://img.example.com/selected-gallery-1.jpg",
                "https://img.example.com/selected-gallery-2.jpg"
            ),
            details = mapOf(
                ATTR_HOTEL_NAME to "Selected Hotel"
            )
        )

        val result = event.withSelectedOption(option)

        assertEquals("https://img.example.com/selected-hero.jpg", result.imageUrl)
        assertEquals(
            listOf(
                "https://img.example.com/selected-hero.jpg",
                "https://img.example.com/selected-gallery-1.jpg",
                "https://img.example.com/selected-gallery-2.jpg"
            ),
            result.photoUrls
        )
        assertEquals("/data/user/0/com.example/files/trip_images/trip-1/original-hero.jpg", result.localImagePath)
        assertEquals("Selected Hotel", result.details[ATTR_HOTEL_NAME])
        assertEquals("Selected Hotel", result.details["title"])
    }

    @Test
    fun withSelectedOption_keepsExistingGalleryWhenOptionGalleryMissing() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "hotel",
            itineraryId = "trip-1",
            imageUrl = "https://img.example.com/original-hero.jpg",
            photoUrls = listOf(
                "https://img.example.com/original-hero.jpg",
                "https://img.example.com/original-gallery.jpg"
            ),
            details = mapOf(ATTR_HOTEL_NAME to "Original Hotel")
        )
        val option = EventOption(
            optionId = "option-1",
            eventId = "event-1",
            source = "serp",
            selected = true,
            imageUrl = "https://img.example.com/selected-hero.jpg",
            photoUrls = emptyList(),
            details = mapOf(ATTR_HOTEL_NAME to "Selected Hotel")
        )

        val result = event.withSelectedOption(option)

        assertEquals("https://img.example.com/selected-hero.jpg", result.imageUrl)
        assertEquals(
            listOf(
                "https://img.example.com/original-hero.jpg",
                "https://img.example.com/original-gallery.jpg"
            ),
            result.photoUrls
        )
    }
}
