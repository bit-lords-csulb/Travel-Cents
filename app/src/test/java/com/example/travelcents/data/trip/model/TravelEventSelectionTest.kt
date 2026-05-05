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

    @Test
    fun withSelectedOption_appliesScheduledVariantFields() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "activity",
            itineraryId = "trip-1",
            date = "2026-06-03",
            startTime = "18:00",
            endTime = "20:00",
            tz = "America/Los_Angeles",
            details = mapOf("title" to "Show")
        )
        val option = EventOption(
            optionId = "option-2",
            eventId = "event-1",
            source = "ticketmaster",
            selected = true,
            details = mapOf(
                ATTR_OPTION_DATE to "2026-06-04",
                ATTR_OPTION_START_TIME to "21:00",
                ATTR_OPTION_END_TIME to "23:00",
                ATTR_OPTION_TZ to "America/New_York",
                ATTR_BUSINESS_NAME to "Show"
            )
        )

        val result = event.withSelectedOption(option)

        assertEquals("2026-06-04", result.date)
        assertEquals("21:00", result.startTime)
        assertEquals("23:00", result.endTime)
        assertEquals("America/New_York", result.tz)
    }
}
