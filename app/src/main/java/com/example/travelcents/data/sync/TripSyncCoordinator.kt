package com.example.travelcents.data.sync

import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.Itinerary

class TripSyncCoordinator(
    private val localDataSource: TripLocalDataSource,
    private val remoteDataSource: TripSyncRemoteDataSource,
    private val legacyRemoteRepository: TripRepository
) {
    suspend fun refreshHomeIfNeeded(viewerUid: String) {
        val localTripCount = localDataSource.getHomeTripCount(viewerUid)
        val localManifestVersion = localDataSource.getManifestVersion(viewerUid)
        val remoteManifest = remoteDataSource.fetchManifest(viewerUid)

        if (remoteManifest != null) {
            localDataSource.recordManifestCheck(viewerUid, remoteManifest.manifestVersion)
            if (localTripCount > 0 && localManifestVersion == remoteManifest.manifestVersion.toString()) {
                return
            }

            val tripRefs = remoteDataSource.fetchTripRefs(viewerUid)
            localDataSource.replaceHomeTripSummaries(
                viewerUid = viewerUid,
                trips = tripRefs,
                manifestVersion = remoteManifest.manifestVersion,
                latestActiveTripKey = remoteManifest.latestActiveTripKey
            )
            return
        }

        val fallbackTrips = legacyRemoteRepository.getTripSummaries(viewerUid)
            .sortedBy { itinerary -> itinerary.dateFrom }
        localDataSource.replaceHomeTripSummaries(
            viewerUid = viewerUid,
            trips = fallbackTrips,
            manifestVersion = null,
            latestActiveTripKey = fallbackTrips.latestActiveTripKey()
        )
        remoteDataSource.backfillTripRefsForViewer(viewerUid, fallbackTrips)
    }

    private fun List<Itinerary>.latestActiveTripKey(): TripKey? {
        return filterNot { trip -> trip.status.equals("archived", ignoreCase = true) }
            .maxByOrNull { trip -> trip.createdAt }
            ?.let { trip -> TripKey(ownerUid = trip.ownerUid, tripId = trip.itineraryId) }
    }
}
