package com.example.travelcents.data.local.trip

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripSummaryDao {
    @Query(
        """
        SELECT * FROM trip_summary
        WHERE viewerUid = :viewerUid
        ORDER BY dateFrom ASC, createdAt DESC
        """
    )
    fun observeTripSummaries(viewerUid: String): Flow<List<TripSummaryEntity>>

    @Query("SELECT COUNT(*) FROM trip_summary WHERE viewerUid = :viewerUid")
    suspend fun countTripSummaries(viewerUid: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TripSummaryEntity>)

    @Query("DELETE FROM trip_summary WHERE viewerUid = :viewerUid")
    suspend fun deleteAllForViewer(viewerUid: String)

    @Query("DELETE FROM trip_summary WHERE viewerUid = :viewerUid AND id NOT IN (:ids)")
    suspend fun deleteMissingForViewer(viewerUid: String, ids: List<String>)

    @Query(
        """
        UPDATE trip_summary
        SET homeImageUrl = :homeImageUrl, updatedAtEpochMs = :updatedAtEpochMs
        WHERE viewerUid = :viewerUid AND ownerUid = :ownerUid AND tripId = :tripId
        """
    )
    suspend fun updateHomeImage(
        viewerUid: String,
        ownerUid: String,
        tripId: String,
        homeImageUrl: String,
        updatedAtEpochMs: Long
    )
}

@Dao
interface UserStubDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserStubEntity)
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SyncStateEntity?
}

@Dao
interface AppStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppStateEntity)

    @Query("SELECT stringValue FROM app_state WHERE key = :key LIMIT 1")
    suspend fun getStringValue(key: String): String?
}
