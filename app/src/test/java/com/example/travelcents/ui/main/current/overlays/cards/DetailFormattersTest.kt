package com.example.travelcents.ui.main.current.overlays.cards

import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_HOTEL_DETAIL_URL
import com.example.travelcents.data.trip.model.ATTR_OFFER_COUNT
import com.example.travelcents.data.trip.model.TravelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailFormattersTest {

    @Test
    fun formatPrice_roundWholeDollars() {
        assertEquals("$1,234", formatPrice(1234.0))
    }

    @Test
    fun formatPrice_keepCentsWhenNeeded() {
        assertEquals("$1,234.50", formatPrice(1234.5))
    }

    @Test
    fun formatPrice_parseCurrencyText() {
        assertEquals("$1,234.50", formatPrice("$1,234.50"))
    }

    @Test
    fun formatPrice_returnNullForInvalidText() {
        assertNull(formatPrice("not-a-price"))
        assertNull(formatPrice(null))
    }

    @Test
    fun restaurantHoursTimeZoneLabel_formatsKnownTimeZone() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "restaurant",
            itineraryId = "trip-1",
            tz = "America/Los_Angeles"
        )

        assertEquals(
            "Restaurant local time: PDT (America/Los_Angeles)",
            restaurantHoursTimeZoneLabel(
                event = event,
                referenceDate = "2026-04-23",
                referenceTime = "1:00 PM"
            )
        )
    }

    @Test
    fun restaurantHoursTimeZoneLabel_reportsUnavailableWhenMissing() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "restaurant",
            itineraryId = "trip-1"
        )

        assertEquals(
            "Restaurant local time zone unavailable",
            restaurantHoursTimeZoneLabel(event)
        )
    }

    @Test
    fun preferredHotelBookingUrl_prefersBookingDotComOffer() {
        val event = TravelEvent(
            eventId = "hotel-1",
            type = "hotel",
            itineraryId = "trip-1",
            details = mapOf(
                ATTR_OFFER_COUNT to "2",
                "offer_0_source" to "Expedia",
                "offer_0_link" to "https://www.expedia.com/hotel",
                "offer_1_source" to "Booking.com",
                "offer_1_link" to "https://www.booking.com/hotel"
            ),
            bookingUrl = "https://fallback.example.com/hotel"
        )

        assertEquals("https://www.booking.com/hotel", preferredHotelBookingUrl(event))
    }

    @Test
    fun preferredHotelBookingUrl_fallsBackToStoredUrls() {
        val event = TravelEvent(
            eventId = "hotel-1",
            type = "hotel",
            itineraryId = "trip-1",
            details = mapOf(
                ATTR_BOOKING_URL to "https://book.example.com/hotel",
                ATTR_HOTEL_DETAIL_URL to "https://detail.example.com/hotel"
            )
        )

        assertEquals("https://book.example.com/hotel", preferredHotelBookingUrl(event))
    }
}
