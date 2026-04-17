package com.example.travelcents.data.sync

import android.util.Log
import com.example.travelcents.data.local.trip.LastOpenedTripState
import com.example.travelcents.data.local.trip.LocalTripMember
import com.example.travelcents.data.local.trip.TripSyncSection
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

        runCatching {
            homeSyncCoordinator.refreshHomeIfNeeded(viewerUid)
        }.onFailure { error ->
            Log.w(
                TAG,
                "Home trip refresh failed for viewer $viewerUid. Falling back to direct latest-trip lookup.",
                error
            )
        }
        return localDataSource.getLatestActiveTripKey(viewerUid)
            ?: legacyRemoteRepository.getLatestActiveTripKey(viewerUid)
    }

    suspend fun refreshTrip(
        viewerUid: String,
        tripKey: TripKey
    ): Itinerary? {
        val localSummary = localDataSource.getTripSummary(viewerUid, tripKey)
        val remoteTripRef = runCatching {
            remoteDataSource.fetchTripRef(viewerUid, tripKey)
        }.getOrNull()
        val localEventCount = localDataSource.getTripEventCount(tripKey)
        val localEventIds = localDataSource.getTripEventIds(tripKey).toSet()
        val localMemberCount = localDataSource.getTripMemberCount(tripKey)

        val initialPlan = CurrentTripRefreshPlanner.plan(
            localSummary = localSummary,
            remoteSummaryCandidate = remoteTripRef,
            tripKey = tripKey,
            localEventCount = localEventCount,
            localEventIds = localEventIds,
            localMemberCount = localMemberCount
        )

        val remoteSummary = if (initialPlan.shouldFetchCanonicalSummary) {
            runCatching {
                remoteDataSource.fetchTripSummary(tripKey)
                    ?: legacyRemoteRepository.getTripSummary(tripKey)
            }.onFailure { error ->
                localDataSource.recordTripSectionFailure(
                    tripKey = tripKey,
                    section = TripSyncSection.SUMMARY,
                    remoteVersion = localSummary?.summaryVersion,
                    error = error
                )
            }.getOrThrow()
        } else {
            remoteTripRef
        } ?: return null

        val refreshPlan = CurrentTripRefreshPlanner.plan(
            localSummary = localSummary,
            remoteSummaryCandidate = remoteSummary,
            tripKey = tripKey,
            localEventCount = localEventCount,
            localEventIds = localEventIds,
            localMemberCount = localMemberCount
        )

        if (refreshPlan.shouldUpsertSummary) {
            localDataSource.upsertTripSummary(
                viewerUid = viewerUid,
                itinerary = remoteSummary,
                isCurrentCandidate = true
            )
        }
        localDataSource.setLastOpenedTrip(tripKey)
        localDataSource.recordTripSectionCheck(
            tripKey = tripKey,
            section = TripSyncSection.SUMMARY,
            remoteVersion = remoteSummary.summaryVersion,
            localVersion = remoteSummary.summaryVersion.toString()
        )

        if (refreshPlan.shouldRefreshEvents) {
            runCatching {
                val events = remoteDataSource.fetchTripEvents(tripKey)
                localDataSource.replaceTripEvents(
                    tripKey = tripKey,
                    events = events,
                    eventVersionGroup = remoteSummary.eventsVersion
                )
            }.onFailure { error ->
                localDataSource.recordTripSectionFailure(
                    tripKey = tripKey,
                    section = TripSyncSection.EVENTS,
                    remoteVersion = remoteSummary.eventsVersion,
                    error = error
                )
                throw error
            }
        } else {
            localDataSource.recordTripSectionCheck(
                tripKey = tripKey,
                section = TripSyncSection.EVENTS,
                remoteVersion = remoteSummary.eventsVersion
            )
        }

        if (refreshPlan.shouldRefreshMembers) {
            runCatching {
                val cachedUserStubs = localDataSource.getUserStubs(viewerUid, remoteSummary.memberUids)
                val members = remoteDataSource.fetchTripMembers(
                    tripKey = tripKey,
                    cachedUserStubs = cachedUserStubs
                )
                localDataSource.replaceTripMembers(
                    tripKey = tripKey,
                    viewerUid = viewerUid,
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
            }.onFailure { error ->
                localDataSource.recordTripSectionFailure(
                    tripKey = tripKey,
                    section = TripSyncSection.MEMBERS,
                    remoteVersion = remoteSummary.membersVersion,
                    error = error
                )
                throw error
            }
        } else {
            localDataSource.recordTripSectionCheck(
                tripKey = tripKey,
                section = TripSyncSection.MEMBERS,
                remoteVersion = remoteSummary.membersVersion
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
            localDataSource.recordTripSectionCheck(
                tripKey = tripKey,
                section = TripSyncSection.OPTIONS,
                remoteVersion = expectedOptionsVersion,
                localVersion = localVersion.toString()
            )
            return emptyMap()
        }

        return runCatching {
            val remoteOptions = remoteDataSource.fetchTripOptionsBulk(tripKey)
            localDataSource.replaceTripOptions(
                tripKey = tripKey,
                optionsByEvent = remoteOptions,
                optionsVersionGroup = expectedOptionsVersion
            )
            remoteOptions
        }.onFailure { error ->
            localDataSource.recordTripSectionFailure(
                tripKey = tripKey,
                section = TripSyncSection.OPTIONS,
                remoteVersion = expectedOptionsVersion,
                error = error
            )
        }.getOrThrow()
    }

    private suspend fun localLastOpenedTrip(viewerUid: String): TripKey? {
        val lastOpenedTrip = localDataSource.getLastOpenedTrip() ?: return null
        val tripKey = lastOpenedTrip.toTripKey()
        val localSummary = localDataSource.getTripSummary(viewerUid, tripKey) ?: return null
        return tripKey.takeUnless { localSummary.status.equals("archived", ignoreCase = true) }
    }

    private fun LastOpenedTripState.toTripKey(): TripKey {
        return TripKey(ownerUid = ownerUid, tripId = tripId)
    }

    private companion object {
        private const val TAG = "CurrentTripSync"
    }
}
