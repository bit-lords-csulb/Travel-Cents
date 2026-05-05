package com.example.travelcents.ui.main.newTrip

import com.example.travelcents.data.trip.model.TravelEvent
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal data class TripEdgeGuardrails(
    val arrivalDate: String,
    val minimumStartTime: String?,
    val departureDate: String,
    val maximumEndTime: String?
)

internal object TripEdgeScheduler {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun buildGuardrails(
        outboundFlight: TravelEvent?,
        tripStartDate: String,
        returnFlight: TravelEvent?,
        tripEndDate: String
    ): TripEdgeGuardrails {
        val arrivalDate = outboundFlight?.details?.get("arrival_date")
            ?.takeIf { it.isNotBlank() }
            ?: outboundFlight?.date?.takeIf { it.isNotBlank() }
            ?: tripStartDate
        val arrivalTime = outboundFlight?.details?.get("arrival_time")
            ?.takeIf { it.isNotBlank() }
            ?: outboundFlight?.endTime?.takeIf { it.isNotBlank() }
        val minimumStartTime = parseTime(arrivalTime)
            ?.let { arrivalLocalTime ->
                val shifted = arrivalLocalTime.plusHours(3)
                if (shifted.isBefore(arrivalLocalTime)) "23:59" else shifted.format(timeFormatter)
            }

        val departureDate = returnFlight?.date?.takeIf { it.isNotBlank() }
            ?: tripEndDate
        val departureTime = returnFlight?.startTime
            ?.takeIf { it.isNotBlank() }
            ?: returnFlight?.details?.get("departure_time")
                ?.substringAfterLast(" ")
                ?.takeIf { it.isNotBlank() }
        val maximumEndTime = parseTime(departureTime)
            ?.let { departureLocalTime ->
                if (departureLocalTime.isBefore(LocalTime.of(5, 0))) {
                    "00:00"
                } else {
                    departureLocalTime.minusHours(5).format(timeFormatter)
                }
            }

        return TripEdgeGuardrails(
            arrivalDate = arrivalDate,
            minimumStartTime = minimumStartTime,
            departureDate = departureDate,
            maximumEndTime = maximumEndTime
        )
    }

    fun applyFlexibleEventWindow(
        events: List<TravelEvent>,
        guardrails: TripEdgeGuardrails
    ): List<TravelEvent> {
        return events.mapNotNull { constrainFlexibleEvent(it, guardrails) }
    }

    fun filterFixedEventWindow(
        events: List<TravelEvent>,
        guardrails: TripEdgeGuardrails
    ): List<TravelEvent> {
        val minTime = parseTime(guardrails.minimumStartTime)
        val maxTime = parseTime(guardrails.maximumEndTime)

        return events.filterNot { event ->
            val start = parseTime(event.startTime)
            val end = parseTime(event.endTime)
            (event.date == guardrails.arrivalDate && minTime != null && start?.isBefore(minTime) == true) ||
                (event.date == guardrails.departureDate && maxTime != null &&
                    ((start != null && !start.isBefore(maxTime)) || (end != null && end.isAfter(maxTime))))
        }
    }

    private fun constrainFlexibleEvent(
        event: TravelEvent,
        guardrails: TripEdgeGuardrails
    ): TravelEvent? {
        val minTime = parseTime(guardrails.minimumStartTime)
        val maxTime = parseTime(guardrails.maximumEndTime)
        var adjustedEvent = event

        if (adjustedEvent.date == guardrails.arrivalDate && minTime != null) {
            val start = parseTime(adjustedEvent.startTime)
            if (start != null && start.isBefore(minTime)) {
                val end = parseTime(adjustedEvent.endTime)
                val durationMinutes = if (end != null && end.isAfter(start)) {
                    Duration.between(start, end).toMinutes()
                } else {
                    120L
                }
                adjustedEvent = adjustedEvent.copy(
                    startTime = minTime.format(timeFormatter),
                    endTime = minTime.plusMinutes(durationMinutes).format(timeFormatter)
                )
            }
        }

        if (adjustedEvent.date == guardrails.departureDate && maxTime != null) {
            val start = parseTime(adjustedEvent.startTime)
            val end = parseTime(adjustedEvent.endTime)
            if ((start != null && !start.isBefore(maxTime)) || (end != null && end.isAfter(maxTime))) {
                return null
            }
        }

        return adjustedEvent
    }

    private fun parseTime(rawTime: String?): LocalTime? {
        val value = rawTime?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { LocalTime.parse(value, timeFormatter) }.getOrNull()
    }
}
