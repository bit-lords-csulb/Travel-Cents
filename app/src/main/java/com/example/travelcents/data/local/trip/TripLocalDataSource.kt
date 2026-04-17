package com.example.travelcents.data.local.trip

import androidx.room.withTransaction
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LastOpenedTripState(
    val ownerUid: String,
    val tripId: String
)

class TripLocalDataSource(
    private val database: TravelCentsDatabase
) {
    fun observeHomeTripSummaries(viewerUid: String): Flow<List<Itinerary>> {
        return database.tripSummaryDao()
            .observeTripSummaries(viewerUid)
            .map { entities -> entities.map(TripSummaryEntity::toDomainModel) }
    }

    suspend fun replaceHomeTripSummaries(
        viewerUid: String,
        trips: List<Itinerary>,
        manifestVersion: Long?,
        latestActiveTripKey: TripKey?
    ) {
        val now = System.currentTimeMillis()
        val entities = trips.map { itinerary ->
            itinerary.toTripSummaryEntity(
                viewerUid = viewerUid,
                updatedAtEpochMs = now,
                isCurrentCandidate = latestActiveTripKey == TripKey(
                    ownerUid = itinerary.ownerUid,
                    tripId = itinerary.itineraryId
                )
            )
        }

        database.withTransaction {
            if (entities.isEmpty()) {
                database.tripSummaryDao().deleteAllForViewer(viewerUid)
            } else {
                database.tripSummaryDao().upsertAll(entities)
                database.tripSummaryDao().deleteMissingForViewer(
                    viewerUid = viewerUid,
                    ids = entities.map(TripSummaryEntity::id)
                )
            }

            database.syncStateDao().upsert(
                SyncStateEntity(
                    id = homeSyncStateId(viewerUid),
                    remoteVersion = manifestVersion?.toString(),
                    localVersion = now.toString(),
                    lastCheckedAtEpochMs = now,
                    lastSuccessfulSyncAtEpochMs = now,
                    syncStatus = SYNC_STATUS_SUCCESS,
                    error = null
                )
            )
            database.appStateDao().upsert(
                AppStateEntity(
                    key = APP_STATE_LAST_LOGGED_IN_UID,
                    stringValue = viewerUid,
                    updatedAtEpochMs = now
                )
            )
        }
    }

    suspend fun getHomeTripCount(viewerUid: String): Int {
        return database.tripSummaryDao().countTripSummaries(viewerUid)
    }

    suspend fun getManifestVersion(viewerUid: String): String? {
        return database.syncStateDao().getById(homeSyncStateId(viewerUid))?.remoteVersion
    }

    suspend fun recordManifestCheck(viewerUid: String, manifestVersion: Long?) {
        val now = System.currentTimeMillis()
        val current = database.syncStateDao().getById(homeSyncStateId(viewerUid))
        database.syncStateDao().upsert(
            SyncStateEntity(
                id = homeSyncStateId(viewerUid),
                remoteVersion = manifestVersion?.toString(),
                localVersion = current?.localVersion,
                lastCheckedAtEpochMs = now,
                lastSuccessfulSyncAtEpochMs = current?.lastSuccessfulSyncAtEpochMs,
                syncStatus = current?.syncStatus ?: SYNC_STATUS_IDLE,
                error = current?.error
            )
        )
    }

    suspend fun updateHomeImage(
        viewerUid: String,
        tripKey: TripKey,
        imageUrl: String
    ) {
        database.tripSummaryDao().updateHomeImage(
            viewerUid = viewerUid,
            ownerUid = tripKey.ownerUid,
            tripId = tripKey.tripId,
            homeImageUrl = imageUrl,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    suspend fun recordHomeRefreshFailure(viewerUid: String, error: Throwable) {
        val now = System.currentTimeMillis()
        val current = database.syncStateDao().getById(homeSyncStateId(viewerUid))
        database.syncStateDao().upsert(
            SyncStateEntity(
                id = homeSyncStateId(viewerUid),
                remoteVersion = current?.remoteVersion,
                localVersion = current?.localVersion,
                lastCheckedAtEpochMs = now,
                lastSuccessfulSyncAtEpochMs = current?.lastSuccessfulSyncAtEpochMs,
                syncStatus = SYNC_STATUS_ERROR,
                error = error.message
            )
        )
    }

    suspend fun setLastOpenedTrip(tripKey: TripKey) {
        val now = System.currentTimeMillis()
        database.appStateDao().upsert(
            AppStateEntity(
                key = APP_STATE_LAST_OPENED_TRIP_OWNER_UID,
                stringValue = tripKey.ownerUid,
                updatedAtEpochMs = now
            )
        )
        database.appStateDao().upsert(
            AppStateEntity(
                key = APP_STATE_LAST_OPENED_TRIP_ID,
                stringValue = tripKey.tripId,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun getLastOpenedTrip(): LastOpenedTripState? {
        val ownerUid = database.appStateDao().getStringValue(APP_STATE_LAST_OPENED_TRIP_OWNER_UID)
        val tripId = database.appStateDao().getStringValue(APP_STATE_LAST_OPENED_TRIP_ID)
        if (ownerUid.isNullOrBlank() || tripId.isNullOrBlank()) return null
        return LastOpenedTripState(ownerUid = ownerUid, tripId = tripId)
    }

    private companion object {
        const val APP_STATE_LAST_LOGGED_IN_UID = "lastLoggedInUid"
        const val APP_STATE_LAST_OPENED_TRIP_OWNER_UID = "lastOpenedTripOwnerUid"
        const val APP_STATE_LAST_OPENED_TRIP_ID = "lastOpenedTripId"
        const val SYNC_STATUS_IDLE = "idle"
        const val SYNC_STATUS_SUCCESS = "success"
        const val SYNC_STATUS_ERROR = "error"

        fun homeSyncStateId(viewerUid: String): String = "user:$viewerUid:home_summaries"
    }
}
