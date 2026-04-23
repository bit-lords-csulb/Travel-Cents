package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary

data class CurrentTripRefreshPlan(
    val shouldFetchCanonicalSummary: Boolean,
    val shouldUpsertSummary: Boolean,
    val shouldRefreshEvents: Boolean,
    val shouldRefreshMembers: Boolean
)

internal object CurrentTripRefreshPlanner {
    fun plan(
        localSummary: Itinerary?,
        remoteSummaryCandidate: Itinerary?,
        tripKey: TripKey,
        localEventCount: Int,
        localEventIds: Set<String>,
        localMemberCount: Int
    ): CurrentTripRefreshPlan {
        if (remoteSummaryCandidate == null) {
            return CurrentTripRefreshPlan(
                shouldFetchCanonicalSummary = true,
                shouldUpsertSummary = false,
                shouldRefreshEvents = localSummary == null,
                shouldRefreshMembers = localSummary == null
            )
        }

        val summaryChanged = localSummary == null ||
            localSummary.summaryVersion != remoteSummaryCandidate.summaryVersion
        val eventsChanged = localSummary == null ||
            localSummary.eventsVersion != remoteSummaryCandidate.eventsVersion ||
            tripKey.tripId != remoteSummaryCandidate.itineraryId ||
            (localEventCount == 0 && remoteSummaryCandidate.eventIds.isNotEmpty()) ||
            localEventIds != remoteSummaryCandidate.eventIds.toSet()
        val membersChanged = localSummary == null ||
            localSummary.membersVersion != remoteSummaryCandidate.membersVersion ||
            (localMemberCount == 0 && remoteSummaryCandidate.memberUids.isNotEmpty())

        return CurrentTripRefreshPlan(
            shouldFetchCanonicalSummary = false,
            shouldUpsertSummary = summaryChanged,
            shouldRefreshEvents = eventsChanged,
            shouldRefreshMembers = membersChanged
        )
    }
}
