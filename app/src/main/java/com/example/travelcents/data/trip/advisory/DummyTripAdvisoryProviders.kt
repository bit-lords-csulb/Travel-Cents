package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.CONFIDENCE_HIGH
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.ENVIRONMENT_INDOOR
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.ENVIRONMENT_OUTDOOR
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.SENSITIVITY_NONE
import com.example.travelcents.data.trip.model.ATTR_ACTIVITY_ENVIRONMENT
import com.example.travelcents.data.trip.model.ATTR_ADVISORY_REASON
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_ENVIRONMENT_CONFIDENCE
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.ATTR_WEATHER_SENSITIVITY
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.displayName
import java.util.Locale

class DummyTripWeatherContextProvider : TripWeatherContextProvider {
    override suspend fun weatherFor(
        event: TravelEvent,
        trip: Itinerary
    ): WeatherContext {
        val metadata = ActivityEnvironmentClassifier.classify(event)
        val text = event.searchText()

        return when {
            metadata.environment == ENVIRONMENT_INDOOR -> mild()
            "beach" in text || metadata.weatherSensitivity == ActivityEnvironmentMetadata.SENSITIVITY_HEAT ->
                WeatherContext(
                    condition = "Extreme heat",
                    precipitationPct = 5,
                    temperatureC = 36,
                    windKph = 10,
                    startsAtLocalTime = event.startTime
                )
            "kayak" in text ||
                "snorkel" in text ||
                metadata.weatherSensitivity == ActivityEnvironmentMetadata.SENSITIVITY_WIND ->
                WeatherContext(
                    condition = "High wind",
                    precipitationPct = 20,
                    temperatureC = 22,
                    windKph = 42,
                    startsAtLocalTime = event.startTime
                )
            metadata.isOutdoorLike || "safari" in text ->
                WeatherContext(
                    condition = "Heavy rain",
                    precipitationPct = 85,
                    temperatureC = 21,
                    windKph = 18,
                    startsAtLocalTime = event.startTime
                )
            else -> mild()
        }
    }

    private fun mild(): WeatherContext {
        return WeatherContext(
            condition = "Mild",
            precipitationPct = 10,
            temperatureC = 23,
            windKph = 8,
            startsAtLocalTime = null
        )
    }
}

class DummyTripTransportContextProvider : TripTransportContextProvider {
    override suspend fun transportFor(
        event: TravelEvent,
        previousEvent: TravelEvent?,
        trip: Itinerary
    ): TransportContext {
        if (previousEvent == null || previousEvent.date != event.date) {
            return normal()
        }

        val gap = minutesBetween(previousEvent.endTime, event.startTime)
        val text = event.searchText()
        val longRoute = listOf("airport", "remote", "safari", "cross-town", "crosstown")
            .any { it in text }

        return when {
            gap != null && gap <= 30 && longRoute -> TransportContext(
                walkMin = null,
                transitMin = 48,
                rideshareMin = 34,
                delayMin = 28,
                reliability = "low",
                summary = "Demo traffic makes this transfer tight from the previous stop."
            )
            gap != null && gap <= 20 -> TransportContext(
                walkMin = 35,
                transitMin = 25,
                rideshareMin = 18,
                delayMin = 20,
                reliability = "medium",
                summary = "Demo transit timing is tight for this plan."
            )
            else -> normal()
        }
    }

    private fun normal(): TransportContext {
        return TransportContext(
            walkMin = 12,
            transitMin = 18,
            rideshareMin = 10,
            delayMin = 0,
            reliability = "high",
            summary = "Demo transport looks workable."
        )
    }

    private fun minutesBetween(from: String, to: String): Int? {
        val fromMinutes = parseMinutes(from) ?: return null
        val toMinutes = parseMinutes(to) ?: return null
        return toMinutes - fromMinutes
    }

    private fun parseMinutes(value: String): Int? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }
}

class DummyTripAlternativeProvider : TripAlternativeProvider {
    override suspend fun alternativesFor(
        trip: Itinerary,
        event: TravelEvent,
        reason: AdvisoryReason
    ): List<EventOption> {
        val destination = trip.destination.ifBlank { "the city" }
        val templates = when (reason) {
            AdvisoryReason.RAIN_OUTDOOR_ACTIVITY -> listOf(
                AlternativeTemplate(
                    title = "Indoor Culture Museum",
                    description = "A covered museum stop near $destination for the same time window.",
                    address = "Museum District, $destination",
                    priceTier = "\$\$"
                ),
                AlternativeTemplate(
                    title = "Covered Market Hall",
                    description = "A dry indoor market with food stalls and local vendors.",
                    address = "Central Market, $destination",
                    priceTier = "\$"
                ),
                AlternativeTemplate(
                    title = "Aquarium Visit",
                    description = "An indoor backup that keeps the wildlife theme without the rain exposure.",
                    address = "Waterfront, $destination",
                    priceTier = "\$\$"
                )
            )
            AdvisoryReason.EXTREME_HEAT -> listOf(
                AlternativeTemplate(
                    title = "Air-Conditioned Gallery Walk",
                    description = "A cooler indoor plan with flexible pacing.",
                    address = "Arts District, $destination",
                    priceTier = "\$"
                ),
                AlternativeTemplate(
                    title = "Local Spa Break",
                    description = "A shaded indoor reset during the hottest part of the day.",
                    address = "Wellness Quarter, $destination",
                    priceTier = "\$\$\$"
                )
            )
            AdvisoryReason.HIGH_WIND -> listOf(
                AlternativeTemplate(
                    title = "Indoor Food Hall",
                    description = "A weather-proof food stop with several local options.",
                    address = "Old Town, $destination",
                    priceTier = "\$\$"
                ),
                AlternativeTemplate(
                    title = "Planetarium Show",
                    description = "An indoor experience that avoids the wind window.",
                    address = "Science Center, $destination",
                    priceTier = "\$\$"
                )
            )
            AdvisoryReason.TRANSIT_DELAY,
            AdvisoryReason.WALKING_TIME_TOO_LONG,
            AdvisoryReason.RIDESHARE_COST_SPIKE -> listOf(
                AlternativeTemplate(
                    title = "Nearby Cafe And Gallery",
                    description = "A closer indoor stop that reduces the transfer pressure.",
                    address = "Near your previous stop, $destination",
                    priceTier = "\$"
                ),
                AlternativeTemplate(
                    title = "Later Slot For ${event.displayName().orEmpty().ifBlank { "This Plan" }}",
                    description = "Keep the original idea but move it later to absorb the demo delay.",
                    address = event.details[ATTR_BUSINESS_ADDRESS].orEmpty().ifBlank { destination },
                    priceTier = event.details[ATTR_PRICE_TIER].orEmpty().ifBlank { "\$\$" }
                )
            )
        }

        return templates.mapIndexed { index, template ->
            EventOption(
                optionId = "demo_advisory_${event.eventId}_${reason.name.lowercase(Locale.US)}_$index",
                eventId = event.eventId,
                tripId = event.itineraryId,
                ownerUid = "",
                source = "dummy_advisory",
                selected = false,
                details = buildMap {
                    put("title", template.title)
                    put("activity_name", template.title)
                    put(ATTR_BUSINESS_NAME, template.title)
                    put("description", template.description)
                    put(ATTR_BUSINESS_ADDRESS, template.address)
                    put("address", template.address)
                    put(ATTR_PRICE_TIER, template.priceTier)
                    put(ATTR_ACTIVITY_ENVIRONMENT, ENVIRONMENT_INDOOR)
                    put(ATTR_WEATHER_SENSITIVITY, SENSITIVITY_NONE)
                    put(ATTR_ENVIRONMENT_CONFIDENCE, CONFIDENCE_HIGH)
                    put(ATTR_ADVISORY_REASON, reason.name)
                    put(ATTR_LATITUDE, "0.0")
                    put(ATTR_LONGITUDE, "0.0")
                    put("colorKey", "lightBlue")
                    put("sortOrder", event.details["sortOrder"].orEmpty())
                }
            )
        }
    }

    private data class AlternativeTemplate(
        val title: String,
        val description: String,
        val address: String,
        val priceTier: String
    )
}

private fun TravelEvent.searchText(): String {
    return listOfNotNull(
        type,
        displayName(),
        details["title"],
        details["activity_name"],
        details["description"],
        details["category"]
    ).joinToString(" ").lowercase(Locale.US)
}
