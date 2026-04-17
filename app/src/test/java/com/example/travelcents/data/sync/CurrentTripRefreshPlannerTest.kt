package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentTripRefreshPlannerTest {

    @Test
    fun plan_usesTripRefOnlyWhenVersionsAndIdsMatch() {
        val tripKey = TripKey(ownerUid = "owner-1", tripId = "trip-1")
        val summary = itinerary(
            tripKey = tripKey,
            summaryVersion = 10L,
            eventsVersion = 20L,
            membersVersion = 30L,
            eventIds = listOf("event-1", "event-2"),
            memberUids = listOf("owner-1", "viewer-1")
        )

        val plan = CurrentTripRefreshPlanner.plan(
            localSummary = summary,
            remoteSummaryCandidate = summary,
            tripKey = tripKey,
            localEventCount = 2,
            localEventIds = setOf("event-1", "event-2"),
            localMemberCount = 2
        )

        assertFalse(plan.shouldFetchCanonicalSummary)
        assertFalse(plan.shouldUpsertSummary)
        assertFalse(plan.shouldRefreshEvents)
        assertFalse(plan.shouldRefreshMembers)
    }

    @Test
    fun plan_usesTripRefAndRefreshesSummaryWhenMissingLocally() {
        val tripKey = TripKey(ownerUid = "owner-1", tripId = "trip-1")
        val remoteTripRef = itinerary(
            tripKey = tripKey,
            summaryVersion = 10L,
            eventsVersion = 20L,
            membersVersion = 30L,
            eventIds = listOf("event-1"),
            memberUids = listOf("owner-1")
        )

        val plan = CurrentTripRefreshPlanner.plan(
            localSummary = null,
            remoteSummaryCandidate = remoteTripRef,
            tripKey = tripKey,
            localEventCount = 0,
            localEventIds = emptySet(),
            localMemberCount = 0
        )

        assertFalse(plan.shouldFetchCanonicalSummary)
        assertTrue(plan.shouldUpsertSummary)
        assertTrue(plan.shouldRefreshEvents)
        assertTrue(plan.shouldRefreshMembers)
    }

    @Test
    fun plan_fetchesCanonicalSummaryWhenTripRefUnavailable() {
        val tripKey = TripKey(ownerUid = "owner-1", tripId = "trip-1")

        val plan = CurrentTripRefreshPlanner.plan(
            localSummary = itinerary(
                tripKey = tripKey,
                summaryVersion = 10L,
                eventsVersion = 20L,
                membersVersion = 30L,
                eventIds = listOf("event-1"),
                memberUids = listOf("owner-1")
            ),
            remoteSummaryCandidate = null,
            tripKey = tripKey,
            localEventCount = 1,
            localEventIds = setOf("event-1"),
            localMemberCount = 1
        )

        assertTrue(plan.shouldFetchCanonicalSummary)
    }

    @Test
    fun plan_refreshesEventsWhenRemoteEventIdsChanged() {
        val tripKey = TripKey(ownerUid = "owner-1", tripId = "trip-1")
        val localSummary = itinerary(
            tripKey = tripKey,
            summaryVersion = 10L,
            eventsVersion = 20L,
            membersVersion = 30L,
            eventIds = listOf("event-1"),
            memberUids = listOf("owner-1")
        )
        val remoteTripRef = localSummary.copy(eventIds = listOf("event-1", "event-2"))

        val plan = CurrentTripRefreshPlanner.plan(
            localSummary = localSummary,
            remoteSummaryCandidate = remoteTripRef,
            tripKey = tripKey,
            localEventCount = 1,
            localEventIds = setOf("event-1"),
            localMemberCount = 1
        )

        assertFalse(plan.shouldFetchCanonicalSummary)
        assertFalse(plan.shouldUpsertSummary)
        assertTrue(plan.shouldRefreshEvents)
        assertFalse(plan.shouldRefreshMembers)
    }

    private fun itinerary(
        tripKey: TripKey,
        summaryVersion: Long,
        eventsVersion: Long,
        membersVersion: Long,
        eventIds: List<String>,
        memberUids: List<String>
    ): Itinerary {
        return Itinerary(
            itineraryId = tripKey.tripId,
            userId = tripKey.ownerUid,
            tripName = "Trip",
            destination = "Tokyo",
            origin = "LAX",
            dateFrom = "2026-07-01",
            dateTo = "2026-07-05",
            durationDays = 4,
            currency = "USD",
            travelStyle = "comfort",
            adults = 2,
            children = 0,
            createdAt = "2026-01-01T00:00:00Z",
            status = "active",
            eventIds = eventIds,
            ownerUid = tripKey.ownerUid,
            memberUids = memberUids,
            summaryVersion = summaryVersion,
            eventsVersion = eventsVersion,
            optionsVersion = 40L,
            membersVersion = membersVersion,
            updatedAtEpochMs = 50L
        )
    }
}
