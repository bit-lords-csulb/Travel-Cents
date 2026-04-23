package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary

interface HomeSyncLocalStore {
    suspend fun getHomeTripCount(viewerUid: String): Int
    suspend fun getManifestVersion(viewerUid: String): String?
    suspend fun replaceHomeTripSummaries(
        viewerUid: String,
        trips: List<Itinerary>,
        manifestVersion: Long?,
        latestActiveTripKey: TripKey?
    )
    suspend fun recordManifestCheck(viewerUid: String, manifestVersion: Long?)
}

interface HomeSyncRemoteSource {
    suspend fun fetchManifest(viewerUid: String): TripManifestRemote?
    suspend fun fetchTripRefs(viewerUid: String): List<Itinerary>
    suspend fun backfillTripRefsForViewer(viewerUid: String, trips: List<Itinerary>)
}
