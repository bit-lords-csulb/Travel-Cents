package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.CONFIDENCE_LOW
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.displayName

class TripAdvisoryEngine(
    private val weatherProvider: TripWeatherContextProvider,
    private val transportProvider: TripTransportContextProvider,
    private val alternativeProvider: TripAlternativeProvider,
    private val nowEpochMs: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun evaluate(
        trip: Itinerary,
        events: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ): List<TripAdvisory> {
        if (events.isEmpty()) return emptyList()

        val sortedEvents = events.sortedWith(
            compareBy(
                { it.date.ifBlank { "9999-12-31" } },
                { parseMinutes(it.startTime) ?: Int.MAX_VALUE },
                { it.eventId }
            )
        )

        return buildList {
            sortedEvents.forEachIndexed { index, event ->
                val previousEvent = sortedEvents
                    .take(index)
                    .lastOrNull { it.date == event.date }
                val metadata = ActivityEnvironmentClassifier.classify(event)
                val weather = weatherProvider.weatherFor(event, trip)
                val transport = transportProvider.transportFor(event, previousEvent, trip)

                if (weather != null) {
                    addAll(weatherAdvisories(trip, event, metadata, weather))
                }
                if (transport != null) {
                    addAll(transportAdvisories(trip, event, previousEvent, transport, weather))
                }
            }
        }.distinctBy { advisory -> advisory.dismissalKey }
    }

    private suspend fun weatherAdvisories(
        trip: Itinerary,
        event: TravelEvent,
        metadata: ActivityEnvironmentMetadata,
        weather: WeatherContext
    ): List<TripAdvisory> {
        if (!metadata.isOutdoorLike) return emptyList()

        return buildList {
            if (weather.precipitationPct >= 60) {
                add(
                    buildAdvisory(
                        trip = trip,
                        event = event,
                        reason = AdvisoryReason.RAIN_OUTDOOR_ACTIVITY,
                        severity = severityFor(metadata, high = weather.precipitationPct >= 80),
                        title = "Rain expected during ${event.shortName()}",
                        message = "${weather.condition} is likely around ${event.startTime.ifBlank { "this stop" }}. Consider moving this outdoor plan indoors.",
                        contextSummary = "${weather.precipitationPct}% precipitation"
                    )
                )
            }
            if (weather.temperatureC >= 34) {
                add(
                    buildAdvisory(
                        trip = trip,
                        event = event,
                        reason = AdvisoryReason.EXTREME_HEAT,
                        severity = severityFor(metadata, high = weather.temperatureC >= 36),
                        title = "Heat risk for ${event.shortName()}",
                        message = "${weather.temperatureC}C demo weather may make this outdoor plan uncomfortable.",
                        contextSummary = "${weather.temperatureC}C"
                    )
                )
            }
            if (weather.windKph >= 35) {
                add(
                    buildAdvisory(
                        trip = trip,
                        event = event,
                        reason = AdvisoryReason.HIGH_WIND,
                        severity = severityFor(metadata, high = weather.windKph >= 40),
                        title = "Wind may affect ${event.shortName()}",
                        message = "${weather.windKph} km/h demo wind could make this activity unreliable.",
                        contextSummary = "${weather.windKph} km/h wind"
                    )
                )
            }
        }
    }

    private suspend fun transportAdvisories(
        trip: Itinerary,
        event: TravelEvent,
        previousEvent: TravelEvent?,
        transport: TransportContext,
        weather: WeatherContext?
    ): List<TripAdvisory> {
        return buildList {
            if (transport.delayMin >= 20 && wouldArriveLate(previousEvent, event, transport)) {
                add(
                    buildAdvisory(
                        trip = trip,
                        event = event,
                        reason = AdvisoryReason.TRANSIT_DELAY,
                        severity = if (transport.delayMin >= 25) AdvisorySeverity.HIGH else AdvisorySeverity.MEDIUM,
                        title = "Transfer looks tight",
                        message = transport.summary,
                        contextSummary = "${transport.delayMin} min delay"
                    )
                )
            }

            val rainOrHeat = (weather?.precipitationPct ?: 0) >= 60 || (weather?.temperatureC ?: 0) >= 34
            if ((transport.walkMin ?: 0) > 30 && rainOrHeat) {
                add(
                    buildAdvisory(
                        trip = trip,
                        event = event,
                        reason = AdvisoryReason.WALKING_TIME_TOO_LONG,
                        severity = AdvisorySeverity.MEDIUM,
                        title = "Walking route is not ideal",
                        message = "The demo route has a long walk during rough weather.",
                        contextSummary = "${transport.walkMin} min walk"
                    )
                )
            }
        }
    }

    private suspend fun buildAdvisory(
        trip: Itinerary,
        event: TravelEvent,
        reason: AdvisoryReason,
        severity: AdvisorySeverity,
        title: String,
        message: String,
        contextSummary: String
    ): TripAdvisory {
        return TripAdvisory(
            advisoryId = "${event.eventId}:${reason.name}",
            eventId = event.eventId,
            severity = severity,
            reason = reason,
            title = title,
            message = message,
            affectedDate = event.date,
            affectedStartTime = event.startTime,
            contextSummary = contextSummary,
            suggestedOptions = alternativeProvider.alternativesFor(trip, event, reason),
            generatedAtEpochMs = nowEpochMs()
        )
    }

    private fun severityFor(
        metadata: ActivityEnvironmentMetadata,
        high: Boolean
    ): AdvisorySeverity {
        return when {
            metadata.confidence == CONFIDENCE_LOW && high -> AdvisorySeverity.MEDIUM
            metadata.confidence == CONFIDENCE_LOW -> AdvisorySeverity.LOW
            high -> AdvisorySeverity.HIGH
            else -> AdvisorySeverity.MEDIUM
        }
    }

    private fun wouldArriveLate(
        previousEvent: TravelEvent?,
        event: TravelEvent,
        transport: TransportContext
    ): Boolean {
        val previousEnd = previousEvent?.endTime?.let(::parseMinutes) ?: return transport.delayMin >= 20
        val nextStart = parseMinutes(event.startTime) ?: return transport.delayMin >= 20
        val travelTime = listOfNotNull(transport.transitMin, transport.rideshareMin, transport.walkMin)
            .minOrNull()
            ?: return transport.delayMin >= 20
        return previousEnd + travelTime + transport.delayMin > nextStart
    }

    private fun parseMinutes(value: String): Int? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    private fun TravelEvent.shortName(): String {
        return displayName()?.takeIf { it.isNotBlank() }
            ?: details["title"]?.takeIf { it.isNotBlank() }
            ?: type.ifBlank { "this plan" }
    }
}
