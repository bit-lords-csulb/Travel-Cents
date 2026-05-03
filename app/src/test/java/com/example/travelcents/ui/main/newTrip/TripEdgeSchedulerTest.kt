package com.example.travelcents.ui.main.newTrip

import com.example.travelcents.data.trip.model.TravelEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripEdgeSchedulerTest {

    @Test
    fun applyFlexibleEventWindow_shiftsArrivalDayEventsByThreeHours() {
        val guardrails = TripEdgeScheduler.buildGuardrails(
            outboundFlight = flight(
                date = "2026-06-01",
                startTime = "11:40",
                endTime = "14:50",
                details = mapOf(
                    "arrival_date" to "2026-06-01",
                    "arrival_time" to "14:50"
                )
            ),
            tripStartDate = "2026-06-01",
            returnFlight = null,
            tripEndDate = "2026-06-07"
        )

        val shifted = TripEdgeScheduler.applyFlexibleEventWindow(
            events = listOf(event(type = "restaurant", date = "2026-06-01", startTime = "16:00", endTime = "17:30")),
            guardrails = guardrails
        )

        assertEquals(1, shifted.size)
        assertEquals("17:50", shifted.first().startTime)
        assertEquals("19:20", shifted.first().endTime)
    }

    @Test
    fun applyFlexibleEventWindow_dropsEventsInsideReturnFlightBuffer() {
        val guardrails = TripEdgeScheduler.buildGuardrails(
            outboundFlight = null,
            tripStartDate = "2026-06-01",
            returnFlight = flight(
                date = "2026-06-07",
                startTime = "20:00",
                endTime = "23:30"
            ),
            tripEndDate = "2026-06-07"
        )

        val remaining = TripEdgeScheduler.applyFlexibleEventWindow(
            events = listOf(
                event(type = "activity", date = "2026-06-07", startTime = "14:00", endTime = "15:00"),
                event(type = "restaurant", date = "2026-06-07", startTime = "15:30", endTime = "17:00")
            ),
            guardrails = guardrails
        )

        assertEquals(1, remaining.size)
        assertEquals("14:00", remaining.first().startTime)
    }

    @Test
    fun filterFixedEventWindow_dropsEarlyArrivalDayEvents() {
        val guardrails = TripEdgeScheduler.buildGuardrails(
            outboundFlight = flight(
                date = "2026-06-01",
                startTime = "11:40",
                endTime = "14:50",
                details = mapOf(
                    "arrival_date" to "2026-06-01",
                    "arrival_time" to "14:50"
                )
            ),
            tripStartDate = "2026-06-01",
            returnFlight = flight(
                date = "2026-06-07",
                startTime = "20:00",
                endTime = "23:30"
            ),
            tripEndDate = "2026-06-07"
        )

        val remaining = TripEdgeScheduler.filterFixedEventWindow(
            events = listOf(
                event(type = "activity", date = "2026-06-01", startTime = "16:00", endTime = "17:30"),
                event(type = "activity", date = "2026-06-01", startTime = "18:00", endTime = "19:30"),
                event(type = "activity", date = "2026-06-07", startTime = "15:30", endTime = "16:30")
            ),
            guardrails = guardrails
        )

        assertEquals(1, remaining.size)
        assertEquals("18:00", remaining.first().startTime)
        assertTrue(remaining.none { it.date == "2026-06-07" })
    }

    private fun flight(
        date: String,
        startTime: String,
        endTime: String,
        details: Map<String, String> = emptyMap()
    ) = TravelEvent(
        eventId = "flight-$date-$startTime",
        type = "flight",
        itineraryId = "trip-1",
        date = date,
        startTime = startTime,
        endTime = endTime,
        details = details
    )

    private fun event(
        type: String,
        date: String,
        startTime: String,
        endTime: String
    ) = TravelEvent(
        eventId = "$type-$date-$startTime",
        type = type,
        itineraryId = "trip-1",
        date = date,
        startTime = startTime,
        endTime = endTime
    )
}
