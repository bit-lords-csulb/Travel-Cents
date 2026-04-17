package com.example.travelcents.data.local.trip

import androidx.room.withTransaction
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripLocalDataSource(
    private val database: TravelCentsDatabase
) {
    fun observeHomeTripSummaries(viewerUid: String): Flow<List<Itinerary>> {
        return database.tripSummaryDao()
            .observeTripSummaries(viewerUid)
            .map { entities -> entities.map(TripSummaryEntity::toDomainModel) }
    }

    suspend fun replaceHomeTripSummaries(viewerUid: String, trips: List<Itinerary>) {
        val now = System.currentTimeMillis()
        val entities = trips.map { itinerary ->
            itinerary.toTripSummaryEntity(viewerUid = viewerUid, updatedAtEpochMs = now)
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
                    remoteVersion = null,
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
        database.syncStateDao().upsert(
            SyncStateEntity(
                id = homeSyncStateId(viewerUid),
                remoteVersion = null,
                localVersion = null,
                lastCheckedAtEpochMs = now,
                lastSuccessfulSyncAtEpochMs = null,
                syncStatus = SYNC_STATUS_ERROR,
                error = error.message
            )
        )
    }

    private companion object {
        const val APP_STATE_LAST_LOGGED_IN_UID = "lastLoggedInUid"
        const val SYNC_STATUS_SUCCESS = "success"
        const val SYNC_STATUS_ERROR = "error"

        fun homeSyncStateId(viewerUid: String): String = "user:$viewerUid:home_summaries"
    }
}
