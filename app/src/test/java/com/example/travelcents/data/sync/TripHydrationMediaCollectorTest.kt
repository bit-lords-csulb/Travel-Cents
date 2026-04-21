package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripHydrationMediaCollectorTest {

    @Test
    fun collectUrls_includesDistinctEventAndOptionMediaAndStaticMaps() {
        val sharedPhoto = "https://img.example.com/shared.jpg"
        val eventStaticMap = "https://maps.example.com/event-map.png"
        val optionStaticMap = "https://maps.example.com/option-map.png"

        val events = listOf(
            TravelEvent(
                eventId = "event-1",
                type = "hotel",
                itineraryId = "trip-1",
                imageUrl = "https://img.example.com/event-hero.jpg",
                photoUrls = listOf(sharedPhoto),
                details = mapOf(ATTR_STATIC_MAP_URL to eventStaticMap)
            )
        )
        val optionsByEvent = mapOf(
            "event-1" to listOf(
                EventOption(
                    optionId = "option-1",
                    eventId = "event-1",
                    imageUrl = "https://img.example.com/option-hero.jpg",
                    photoUrls = listOf(sharedPhoto),
                    details = mapOf(
                        ATTR_BUSINESS_NAME to "Moonstone Cafe",
                        ATTR_STATIC_MAP_URL to optionStaticMap
                    )
                )
            )
        )

        val urls = TripHydrationMediaCollector.collectUrls(events, optionsByEvent)

        assertEquals(5, urls.size)
        assertTrue("https://img.example.com/event-hero.jpg" in urls)
        assertTrue("https://img.example.com/option-hero.jpg" in urls)
        assertTrue(sharedPhoto in urls)
        assertTrue(eventStaticMap in urls)
        assertTrue(optionStaticMap in urls)
    }
}
