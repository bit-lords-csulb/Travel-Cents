package com.example.travelcents.ui.main.current.calendar

import com.example.travelcents.data.trip.model.ATTR_ARRIVAL_DAY_OFFSET
import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_CHECK_OUT_TIME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentTripCalendarLayoutUtilsTest {

    @Test
    fun visibleWeekDatesForSelection_extendsBeyondTripRange() {
        val dates = visibleWeekDatesForSelection(
            allDates = listOf("2026-06-10", "2026-06-11"),
            selectedDate = "2026-06-03"
        )

        assertEquals("2026-06-03", dates.first())
        assertEquals("2026-06-09", dates.last())
    }

    @Test
    fun flightSpan_usesProviderArrivalDateAcrossMidnight() {
        val event = TravelEvent(
            eventId = "flight-1",
            type = "flight",
            itineraryId = "trip-1",
            date = "2026-08-01",
            startTime = "21:30",
            endTime = "05:15",
            details = mapOf(
                "departure_time" to "2026-08-01 21:30",
                "arrival_date" to "2026-08-02",
                "arrival_time" to "05:15",
                ATTR_ARRIVAL_DAY_OFFSET to "1"
            )
        )

        val departureDay = eventSpanForDate(event, "2026-08-01")
        val arrivalDay = eventSpansForDate(listOf(event), "2026-08-02").firstOrNull()

        assertNotNull(departureDay)
        assertNotNull(arrivalDay)
        assertEquals(21 * 60 + 30, departureDay!!.startMinutes)
        assertTrue(departureDay.continuesAfter)
        assertEquals(0, arrivalDay!!.startMinutes)
        assertEquals(5 * 60 + 15, arrivalDay.endMinutes)
        assertTrue(arrivalDay.continuesBefore)
    }

    @Test
    fun hotelCheckInAndCheckout_onlyRenderNearBoundaryTimes() {
        val event = TravelEvent(
            eventId = "hotel-1",
            type = "hotel",
            itineraryId = "trip-1",
            date = "2026-07-04",
            details = mapOf(
                ATTR_HOTEL_NAME to "Harbor Hotel",
                "check_in_date" to "2026-07-04",
                ATTR_CHECK_IN_TIME to "3:00 PM",
                "check_out_date" to "2026-07-06",
                ATTR_CHECK_OUT_TIME to "11:00 AM"
            )
        )

        val middleDaySpans = eventSpansForDate(listOf(event), "2026-07-05")
        val checkoutDaySpans = eventSpansForDate(listOf(event), "2026-07-06")
        val checkInDaySpans = eventSpansForDate(listOf(event), "2026-07-04")

        assertEquals(1, checkoutDaySpans.size)
        assertTrue(middleDaySpans.isEmpty())
        assertEquals(1, checkInDaySpans.size)
        assertTrue(checkInDaySpans.any { it.titleOverride == "Harbor Hotel" && it.startMinutes == 13 * 60 && it.endMinutes == 15 * 60 })
        assertTrue(checkoutDaySpans.any { it.titleOverride == "Harbor Hotel checkout" })
        assertTrue(checkoutDaySpans.any { it.titleOverride == "Harbor Hotel checkout" && it.startMinutes == 9 * 60 && it.endMinutes == 11 * 60 })
    }
}
