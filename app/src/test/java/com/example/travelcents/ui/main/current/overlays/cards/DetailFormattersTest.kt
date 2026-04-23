package com.example.travelcents.ui.main.current.overlays.cards

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
}
