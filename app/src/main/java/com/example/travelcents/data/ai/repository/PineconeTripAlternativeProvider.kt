package com.example.travelcents.data.ai.repository

import com.example.travelcents.data.trip.advisory.AdvisoryReason
import com.example.travelcents.data.trip.advisory.TripAlternativeProvider
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName

class PineconeTripAlternativeProvider(
    private val fetchAlternatives: suspend (ActivityAlternativeSearchRequest) -> List<EventOption> = { request ->
        TripPlannerRepository.getActivityAlternatives(request)
    }
) : TripAlternativeProvider {

    override suspend fun alternativesFor(
        trip: Itinerary,
        event: TravelEvent,
        reason: AdvisoryReason
    ): List<EventOption> {
        val destination = trip.destination
        if (destination.isBlank()) return emptyList()
        val currentTitle = event.displayName()
            ?: event.detailValue("title", "activity_name")
            ?: "this activity"
        val description = event.details["description"].orEmpty()
        val request = ActivityAlternativeSearchRequest(
            destination = destination,
            currentActivityTitle = currentTitle,
            currentActivityDescription = description,
            reason = reason.name,
            query = alternativeQuery(destination, reason),
            limit = ACTIVITY_ALTERNATIVE_LIMIT
        )

        return runCatching { fetchAlternatives(request) }
            .getOrDefault(emptyList())
    }

    private fun alternativeQuery(
        destination: String,
        reason: AdvisoryReason
    ): String {
        val activityType = when (reason) {
            AdvisoryReason.RAIN_OUTDOOR_ACTIVITY -> "rain safe indoor covered museum gallery theater aquarium market activity"
            AdvisoryReason.EXTREME_HEAT -> "air-conditioned indoor museum gallery theater aquarium market activity"
            AdvisoryReason.HIGH_WIND -> "wind safe indoor protected museum gallery theater aquarium market activity"
            AdvisoryReason.TRANSIT_DELAY,
            AdvisoryReason.WALKING_TIME_TOO_LONG,
            AdvisoryReason.RIDESHARE_COST_SPIKE -> "nearby indoor activity"
        }
        return "$activityType in $destination"
    }

    private companion object {
        const val ACTIVITY_ALTERNATIVE_LIMIT = 12
    }
}
