package com.example.travelcents.data.sync

import com.example.travelcents.data.local.trip.LastOpenedTripState
import com.example.travelcents.data.local.trip.LocalTripMember
import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary

class CurrentTripSyncCoordinator(
    private val localDataSource: TripLocalDataSource,
    private val remoteDataSource: TripSyncRemoteDataSource,
    private val homeSyncCoordinator: TripSyncCoordinator,
    private val legacyRemoteRepository: TripRepository
) {
    suspend fun resolveLatestTripKey(viewerUid: String): TripKey? {
        localLastOpenedTrip(viewerUid)?.let { return it }

        localDataSource.getLatestActiveTripKey(viewerUid)?.let { return it }

        runCatching {
            remoteDataSource.fetchManifest(viewerUid)
        }.getOrNull()?.latestActiveTripKey?.let { return it }

        homeSyncCoordinator.refreshHomeIfNeeded(viewerUid)
        return localDataSource.getLatestActiveTripKey(viewerUid)
            ?: legacyRemoteRepository.getLatestActiveTripKey(viewerUid)
    }

    suspend fun refreshTrip(
        viewerUid: String,
        tripKey: TripKey
    ): Itinerary? {
        val localSummary = localDataSource.getTripSummary(viewerUid, tripKey)
        val remoteSummary = remoteDataSource.fetchTripSummary(tripKey)
            ?: legacyRemoteRepository.getTripSummary(tripKey)
            ?: return null

        localDataSource.upsertTripSummary(
            viewerUid = viewerUid,
            itinerary = remoteSummary,
            isCurrentCandidate = true
        )
        localDataSource.setLastOpenedTrip(tripKey)

        if (shouldRefreshEvents(localSummary = localSummary, remoteSummary = remoteSummary, tripKey = tripKey)) {
            val events = remoteDataSource.fetchTripEvents(tripKey)
            localDataSource.replaceTripEvents(
                tripKey = tripKey,
                events = events,
                eventVersionGroup = remoteSummary.eventsVersion
            )
        }

        if (shouldRefreshMembers(localSummary = localSummary, remoteSummary = remoteSummary, tripKey = tripKey)) {
            val members = remoteDataSource.fetchTripMembers(tripKey)
            localDataSource.replaceTripMembers(
                tripKey = tripKey,
                members = members.map { member ->
                    LocalTripMember(
                        memberUid = member.memberUid,
                        role = member.role,
                        displayName = member.displayName,
                        avatarUrl = member.avatarUrl
                    )
                },
                memberVersion = remoteSummary.membersVersion
            )
        }

        return remoteSummary
    }

    suspend fun hydrateOptionsIfNeeded(
        tripKey: TripKey,
        expectedOptionsVersion: Long
    ): Map<String, List<EventOption>> {
        val localVersion = localDataSource.getTripOptionsVersionGroup(tripKey)
        if (localVersion != null && localVersion == expectedOptionsVersion) {
            return emptyMap()
        }

        val remoteOptions = remoteDataSource.fetchTripOptionsBulk(tripKey)
        localDataSource.replaceTripOptions(
            tripKey = tripKey,
            optionsByEvent = remoteOptions,
            optionsVersionGroup = expectedOptionsVersion
        )
        return remoteOptions
    }

    private suspend fun localLastOpenedTrip(viewerUid: String): TripKey? {
        val lastOpenedTrip = localDataSource.getLastOpenedTrip() ?: return null
        val tripKey = lastOpenedTrip.toTripKey()
        val localSummary = localDataSource.getTripSummary(viewerUid, tripKey) ?: return null
        return tripKey.takeUnless { localSummary.status.equals("archived", ignoreCase = true) }
    }

    private suspend fun shouldRefreshEvents(
        localSummary: Itinerary?,
        remoteSummary: Itinerary,
        tripKey: TripKey
    ): Boolean {
        if (localSummary == null) return true
        if (localSummary.eventsVersion != remoteSummary.eventsVersion) return true
        if (tripKey.tripId != remoteSummary.itineraryId) return true

        val localEventCount = localDataSource.getTripEventCount(tripKey)
        if (localEventCount == 0 && remoteSummary.eventIds.isNotEmpty()) return true

        val localEventIds = localDataSource.getTripEventIds(tripKey).toSet()
        return localEventIds != remoteSummary.eventIds.toSet()
    }

    private suspend fun shouldRefreshMembers(
        localSummary: Itinerary?,
        remoteSummary: Itinerary,
        tripKey: TripKey
    ): Boolean {
        if (localSummary == null) return true
        if (localSummary.membersVersion != remoteSummary.membersVersion) return true

        val localMemberCount = localDataSource.getTripMemberCount(tripKey)
        return localMemberCount == 0 && remoteSummary.memberUids.isNotEmpty()
    }

    private fun LastOpenedTripState.toTripKey(): TripKey {
        return TripKey(ownerUid = ownerUid, tripId = tripId)
    }
}
