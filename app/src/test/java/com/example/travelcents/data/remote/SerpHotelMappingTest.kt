package com.example.travelcents.data.remote

import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.SerpGpsCoordinates
import com.example.travelcents.data.trip.model.SerpHotelImage
import com.example.travelcents.data.trip.model.SerpHotelPrice
import com.example.travelcents.data.trip.model.SerpHotelProperty
import com.example.travelcents.data.trip.model.SerpRatePerNight
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.remote.SerpRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerpHotelMappingTest {

    @Test
    fun mapHotelSearchResultToEvent_preservesSelectedHotelGalleryAndHero() {
        val request = sampleRequest()
        val itinerary = sampleItinerary()
        val selectedHotel = SerpHotelProperty(
            name = "Seaside Hotel",
            overallRating = 4.6,
            reviews = 812,
            ratePerNight = SerpRatePerNight(lowest = "$199", extractedLowest = 199.0),
            amenities = listOf("Pool", "Spa", "Wi-Fi", "Breakfast", "Parking", "Gym"),
            checkInTime = "15:00",
            checkOutTime = "11:00",
            gpsCoordinates = SerpGpsCoordinates(latitude = 21.282778, longitude = -157.829444),
            images = listOf(
                SerpHotelImage(originalImage = "https://img.example.com/selected-original-1.jpg"),
                SerpHotelImage(thumbnail = "https://img.example.com/selected-thumb-2.jpg")
            ),
            prices = listOf(
                SerpHotelPrice(
                    link = "https://booking.example.com/seaside",
                    ratePerNight = SerpRatePerNight(extractedLowest = 199.0)
                )
            )
        )
        val alternateHotel = SerpHotelProperty(
            name = "Harbor Hotel",
            images = listOf(
                SerpHotelImage(originalImage = "https://img.example.com/alt-original-1.jpg"),
                SerpHotelImage(thumbnail = "https://img.example.com/alt-thumb-2.jpg")
            )
        )

        val event = SerpRepository.mapHotelSearchResultToEvent(
            request = request,
            itinerary = itinerary,
            properties = listOf(selectedHotel, alternateHotel)
        )

        assertEquals(
            listOf(
                "https://img.example.com/selected-original-1.jpg",
                "https://img.example.com/selected-thumb-2.jpg"
            ),
            event.photoUrls
        )
        assertEquals("https://img.example.com/selected-original-1.jpg", event.imageUrl)
        assertEquals(event.photoUrls, event.options.first().photoUrls)
        assertEquals("Seaside Hotel", event.details[ATTR_HOTEL_NAME])
        assertEquals("4.6", event.details[ATTR_HOTEL_RATING])
        assertEquals("21.282778", event.details[ATTR_LATITUDE])
        assertEquals("-157.829444", event.details[ATTR_LONGITUDE])
        assertEquals("osm_staticmap", event.details[ATTR_STATIC_MAP_PROVIDER])
        assertTrue(event.details[ATTR_STATIC_MAP_URL].orEmpty().contains("center=21.282778,-157.829444"))
        assertEquals(
            listOf(
                "https://img.example.com/alt-original-1.jpg",
                "https://img.example.com/alt-thumb-2.jpg"
            ),
            event.options[1].photoUrls
        )
    }

    @Test
    fun mapHotelPhotos_fallsBackToThumbnailWhenOriginalMissing() {
        val hotel = SerpHotelProperty(
            name = "Thumbnail Hotel",
            images = listOf(
                SerpHotelImage(thumbnail = "https://img.example.com/thumb-only.jpg"),
                SerpHotelImage(originalImage = "https://img.example.com/original.jpg")
            )
        )

        val photos = SerpRepository.mapHotelPhotos(hotel)

        assertEquals(
            listOf(
                "https://img.example.com/thumb-only.jpg",
                "https://img.example.com/original.jpg"
            ),
            photos
        )
        assertTrue(photos.none { it.isBlank() })
    }

    private fun sampleRequest() = TravelRequest(
        userId = "user-1",
        origin = "Los Angeles, USA",
        destination = "Honolulu, USA",
        dateFrom = "2026-06-01",
        dateTo = "2026-06-07",
        adults = 4,
        children = 0,
        travelStyle = "comfort",
        currency = "USD",
        budgetTotal = 3000.0,
        interests = emptyList(),
        specialRequests = ""
    )

    private fun sampleItinerary() = Itinerary(
        itineraryId = "trip-1",
        userId = "user-1",
        tripName = "Hawaii",
        destination = "Honolulu, USA",
        origin = "Los Angeles, USA",
        originIata = "LAX",
        destinationIata = "HNL",
        dateFrom = "2026-06-01",
        dateTo = "2026-06-07",
        durationDays = 6,
        currency = "USD",
        travelStyle = "comfort",
        adults = 4,
        children = 0,
        createdAt = "2026-04-15T12:00:00Z",
        status = "active",
        eventIds = emptyList()
    )
}
