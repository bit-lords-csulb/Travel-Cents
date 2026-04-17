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

    @Query(
        """
        SELECT * FROM trip_summary
        WHERE viewerUid = :viewerUid AND ownerUid = :ownerUid AND tripId = :tripId
        LIMIT 1
        """
    )
    fun observeTripSummary(viewerUid: String, ownerUid: String, tripId: String): Flow<TripSummaryEntity?>

    @Query(
        """
        SELECT * FROM trip_summary
        WHERE viewerUid = :viewerUid AND ownerUid = :ownerUid AND tripId = :tripId
        LIMIT 1
        """
    )
    suspend fun getTripSummary(viewerUid: String, ownerUid: String, tripId: String): TripSummaryEntity?

    @Query(
        """
        SELECT * FROM trip_summary
        WHERE viewerUid = :viewerUid AND LOWER(status) != 'archived'
        ORDER BY isCurrentCandidate DESC, createdAt DESC, dateFrom DESC
        LIMIT 1
        """
    )
    suspend fun getLatestActiveTripSummary(viewerUid: String): TripSummaryEntity?

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
interface TripEventDao {
    @Query(
        """
        SELECT * FROM trip_event
        WHERE ownerUid = :ownerUid AND tripId = :tripId
        ORDER BY date ASC, startTime ASC, eventId ASC
        """
    )
    fun observeTripEvents(ownerUid: String, tripId: String): Flow<List<TripEventEntity>>

    @Query("SELECT COUNT(*) FROM trip_event WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun countForTrip(ownerUid: String, tripId: String): Int

    @Query("SELECT eventId FROM trip_event WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun getEventIdsForTrip(ownerUid: String, tripId: String): List<String>

    @Query(
        """
        SELECT * FROM trip_event
        WHERE ownerUid = :ownerUid AND tripId = :tripId
        ORDER BY date ASC, startTime ASC, eventId ASC
        """
    )
    suspend fun getForTrip(ownerUid: String, tripId: String): List<TripEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TripEventEntity>)

    @Query("DELETE FROM trip_event WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun deleteForTrip(ownerUid: String, tripId: String)
}

@Dao
interface TripMemberDao {
    @Query(
        """
        SELECT * FROM trip_member
        WHERE ownerUid = :ownerUid AND tripId = :tripId
        ORDER BY displayName ASC, memberUid ASC
        """
    )
    fun observeTripMembers(ownerUid: String, tripId: String): Flow<List<TripMemberEntity>>

    @Query("SELECT COUNT(*) FROM trip_member WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun countForTrip(ownerUid: String, tripId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TripMemberEntity>)

    @Query("DELETE FROM trip_member WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun deleteForTrip(ownerUid: String, tripId: String)
}

@Dao
interface EventOptionDao {
    @Query(
        """
        SELECT * FROM event_option
        WHERE ownerUid = :ownerUid AND tripId = :tripId
        ORDER BY eventId ASC, selected DESC, optionId ASC
        """
    )
    fun observeTripOptions(ownerUid: String, tripId: String): Flow<List<EventOptionEntity>>

    @Query(
        """
        SELECT * FROM event_option
        WHERE ownerUid = :ownerUid AND tripId = :tripId AND eventId = :eventId
        ORDER BY selected DESC, optionId ASC
        """
    )
    suspend fun getOptionsForEvent(ownerUid: String, tripId: String, eventId: String): List<EventOptionEntity>

    @Query(
        """
        SELECT optionsVersionGroup FROM event_option
        WHERE ownerUid = :ownerUid AND tripId = :tripId
        LIMIT 1
        """
    )
    suspend fun getTripOptionsVersionGroup(ownerUid: String, tripId: String): Long?

    @Query(
        """
        SELECT * FROM event_option
        WHERE ownerUid = :ownerUid AND tripId = :tripId
        ORDER BY eventId ASC, selected DESC, optionId ASC
        """
    )
    suspend fun getForTrip(ownerUid: String, tripId: String): List<EventOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<EventOptionEntity>)

    @Query("DELETE FROM event_option WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun deleteForTrip(ownerUid: String, tripId: String)
}

@Dao
interface UserStubDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserStubEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<UserStubEntity>)

    @Query(
        """
        SELECT * FROM user_stub
        WHERE viewerUid = :viewerUid AND userUid IN (:userUids)
        """
    )
    suspend fun getForViewer(viewerUid: String, userUids: List<String>): List<UserStubEntity>
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

@Dao
interface MediaAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MediaAssetEntity>)

    @Query("DELETE FROM media_asset WHERE ownerUid = :ownerUid AND tripId = :tripId")
    suspend fun deleteForTrip(ownerUid: String, tripId: String)
}
