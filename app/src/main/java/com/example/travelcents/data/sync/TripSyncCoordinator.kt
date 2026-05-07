package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.Itinerary
import com.google.firebase.firestore.FirebaseFirestoreException

class TripSyncCoordinator(
    private val localDataSource: HomeSyncLocalStore,
    private val remoteDataSource: HomeSyncRemoteSource,
    private val legacyRemoteRepository: TripRepository
) {
    suspend fun refreshHomeIfNeeded(viewerUid: String) {
        val remoteManifest = runCatching {
            remoteDataSource.fetchManifest(viewerUid)
        }.getOrElse { error ->
            if (!isPermissionDenied(error)) throw error
            null
        }

        if (remoteManifest != null) {
            localDataSource.recordManifestCheck(viewerUid, remoteManifest.manifestVersion)

            val tripRefs = runCatching {
                remoteDataSource.fetchTripRefs(viewerUid)
            }.getOrElse { error ->
                if (!isPermissionDenied(error)) throw error
                fallbackTrips(viewerUid)
            }
            localDataSource.replaceHomeTripSummaries(
                viewerUid = viewerUid,
                trips = tripRefs,
                manifestVersion = remoteManifest.manifestVersion,
                latestActiveTripKey = remoteManifest.latestActiveTripKey
            )
            return
        }

        val fallbackTrips = fallbackTrips(viewerUid)
        localDataSource.replaceHomeTripSummaries(
            viewerUid = viewerUid,
            trips = fallbackTrips,
            manifestVersion = null,
            latestActiveTripKey = fallbackTrips.latestActiveTripKey()
        )
        runCatching {
            remoteDataSource.backfillTripRefsForViewer(viewerUid, fallbackTrips)
        }.getOrElse { error ->
            if (!isPermissionDenied(error)) throw error
        }
    }

    private fun List<Itinerary>.latestActiveTripKey(): TripKey? {
        return filterNot { trip -> trip.status.equals("archived", ignoreCase = true) }
            .maxByOrNull { trip -> trip.createdAt }
            ?.let { trip -> TripKey(ownerUid = trip.ownerUid, tripId = trip.itineraryId) }
    }

    private suspend fun fallbackTrips(viewerUid: String): List<Itinerary> {
        return legacyRemoteRepository.getTripSummaries(viewerUid)
            .sortedBy { itinerary -> itinerary.dateFrom }
    }

    private fun isPermissionDenied(error: Throwable): Boolean {
        return (error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
}
