package com.example.travelcents.data.local.trip

import androidx.room.withTransaction
import com.example.travelcents.data.media.CachedMediaAsset
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LastOpenedTripState(
    val ownerUid: String,
    val tripId: String
)

enum class TripSyncSection(val wireValue: String) {
    SUMMARY("summary"),
    EVENTS("events"),
    MEMBERS("members"),
    OPTIONS("options")
}

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
                entities.forEach { entity ->
                    upsertSyncState(
                        id = tripSyncStateId(
                            tripKey = TripKey(ownerUid = entity.ownerUid, tripId = entity.tripId),
                            section = TripSyncSection.SUMMARY
                        ),
                        remoteVersion = entity.summaryVersion.takeIf { it > 0 }?.toString(),
                        localVersion = entity.summaryVersion.takeIf { it > 0 }?.toString() ?: now.toString(),
                        checkedAtEpochMs = now,
                        successfulAtEpochMs = now,
                        status = SYNC_STATUS_SUCCESS,
                        error = null
                    )
                }
            }

            upsertSyncState(
                id = homeSyncStateId(viewerUid),
                remoteVersion = manifestVersion?.toString(),
                localVersion = now.toString(),
                checkedAtEpochMs = now,
                successfulAtEpochMs = now,
                status = SYNC_STATUS_SUCCESS,
                error = null
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

    suspend fun getLatestActiveTripKey(viewerUid: String): TripKey? {
        return database.tripSummaryDao()
            .getLatestActiveTripSummary(viewerUid)
            ?.let { entity ->
                TripKey(ownerUid = entity.ownerUid, tripId = entity.tripId)
            }
    }

    fun observeTripSummary(
        viewerUid: String,
        tripKey: TripKey
    ): Flow<Itinerary?> {
        return database.tripSummaryDao()
            .observeTripSummary(
                viewerUid = viewerUid,
                ownerUid = tripKey.ownerUid,
                tripId = tripKey.tripId
            )
            .map { entity -> entity?.toDomainModel() }
    }

    suspend fun getTripSummary(
        viewerUid: String,
        tripKey: TripKey
    ): Itinerary? {
        return database.tripSummaryDao()
            .getTripSummary(
                viewerUid = viewerUid,
                ownerUid = tripKey.ownerUid,
                tripId = tripKey.tripId
            )
            ?.toDomainModel()
    }

    suspend fun upsertTripSummary(
        viewerUid: String,
        itinerary: Itinerary,
        isCurrentCandidate: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.tripSummaryDao().upsertAll(
                listOf(
                    itinerary.toTripSummaryEntity(
                        viewerUid = viewerUid,
                        updatedAtEpochMs = now,
                        isCurrentCandidate = isCurrentCandidate
                    )
                )
            )
            upsertSectionSuccess(
                tripKey = TripKey(ownerUid = itinerary.ownerUid, tripId = itinerary.itineraryId),
                section = TripSyncSection.SUMMARY,
                version = itinerary.summaryVersion.takeIf { it > 0 }?.toString() ?: now.toString(),
                now = now
            )
        }
    }

    fun observeTripEvents(tripKey: TripKey): Flow<List<TravelEvent>> {
        return database.tripEventDao()
            .observeTripEvents(ownerUid = tripKey.ownerUid, tripId = tripKey.tripId)
            .map { entities -> entities.map(TripEventEntity::toDomainModel) }
    }

    suspend fun replaceTripEvents(
        tripKey: TripKey,
        events: List<TravelEvent>,
        eventVersionGroup: Long
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.tripEventDao().deleteForTrip(tripKey.ownerUid, tripKey.tripId)
            if (events.isNotEmpty()) {
                database.tripEventDao().upsertAll(
                    events.map { event ->
                        event.toTripEventEntity(
                            ownerUid = tripKey.ownerUid,
                            tripId = tripKey.tripId,
                            eventVersionGroup = eventVersionGroup,
                            updatedAtEpochMs = now
                        )
                    }
                )
            }
            upsertSectionSuccess(
                tripKey = tripKey,
                section = TripSyncSection.EVENTS,
                version = eventVersionGroup.takeIf { it > 0 }?.toString() ?: now.toString(),
                now = now
            )
        }
    }

    suspend fun getTripEventCount(tripKey: TripKey): Int {
        return database.tripEventDao().countForTrip(tripKey.ownerUid, tripKey.tripId)
    }

    suspend fun getTripEventIds(tripKey: TripKey): List<String> {
        return database.tripEventDao().getEventIdsForTrip(tripKey.ownerUid, tripKey.tripId)
    }

    suspend fun getTripEvents(tripKey: TripKey): List<TravelEvent> {
        return database.tripEventDao()
            .getForTrip(tripKey.ownerUid, tripKey.tripId)
            .map(TripEventEntity::toDomainModel)
    }

    fun observeTripMembers(tripKey: TripKey): Flow<List<LocalTripMember>> {
        return database.tripMemberDao()
            .observeTripMembers(ownerUid = tripKey.ownerUid, tripId = tripKey.tripId)
            .map { entities ->
                entities.map { entity ->
                    LocalTripMember(
                        memberUid = entity.memberUid,
                        role = entity.role,
                        displayName = entity.displayName,
                        avatarUrl = entity.avatarUrl
                    )
                }
            }
    }

    suspend fun replaceTripMembers(
        tripKey: TripKey,
        viewerUid: String,
        members: List<LocalTripMember>,
        memberVersion: Long
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.tripMemberDao().deleteForTrip(tripKey.ownerUid, tripKey.tripId)
            if (members.isNotEmpty()) {
                database.tripMemberDao().upsertAll(
                    members.map { member ->
                        TripMemberEntity(
                            id = tripMemberEntityId(
                                ownerUid = tripKey.ownerUid,
                                tripId = tripKey.tripId,
                                memberUid = member.memberUid
                            ),
                            ownerUid = tripKey.ownerUid,
                            tripId = tripKey.tripId,
                            memberUid = member.memberUid,
                            role = member.role,
                            memberVersion = memberVersion,
                            displayName = member.displayName,
                            avatarUrl = member.avatarUrl,
                            updatedAtEpochMs = now
                        )
                    }
                )
            }
            upsertUserStubs(
                viewerUid = viewerUid,
                stubs = members.map { member ->
                    LocalUserStub(
                        userUid = member.memberUid,
                        displayName = member.displayName,
                        avatarUrl = member.avatarUrl
                    )
                },
                now = now
            )
            upsertSectionSuccess(
                tripKey = tripKey,
                section = TripSyncSection.MEMBERS,
                version = memberVersion.takeIf { it > 0 }?.toString() ?: now.toString(),
                now = now
            )
        }
    }

    suspend fun getTripMemberCount(tripKey: TripKey): Int {
        return database.tripMemberDao().countForTrip(tripKey.ownerUid, tripKey.tripId)
    }

    fun observeTripOptions(tripKey: TripKey): Flow<Map<String, List<EventOption>>> {
        return database.eventOptionDao()
            .observeTripOptions(ownerUid = tripKey.ownerUid, tripId = tripKey.tripId)
            .map { entities ->
                entities.groupBy { entity -> entity.eventId }
                    .mapValues { (_, options) -> options.map(EventOptionEntity::toDomainModel) }
            }
    }

    suspend fun getOptionsForEvent(
        tripKey: TripKey,
        eventId: String
    ): List<EventOption> {
        return database.eventOptionDao()
            .getOptionsForEvent(
                ownerUid = tripKey.ownerUid,
                tripId = tripKey.tripId,
                eventId = eventId
            )
            .map(EventOptionEntity::toDomainModel)
    }

    suspend fun replaceTripOptions(
        tripKey: TripKey,
        optionsByEvent: Map<String, List<EventOption>>,
        optionsVersionGroup: Long
    ) {
        val now = System.currentTimeMillis()
        val entities = optionsByEvent.flatMap { (eventId, options) ->
            options.map { option ->
                option.toEntity(
                    ownerUid = tripKey.ownerUid,
                    tripId = tripKey.tripId,
                    eventId = eventId,
                    optionsVersionGroup = optionsVersionGroup,
                    updatedAtEpochMs = now
                )
            }
        }

        database.withTransaction {
            database.eventOptionDao().deleteForTrip(tripKey.ownerUid, tripKey.tripId)
            if (entities.isNotEmpty()) {
                database.eventOptionDao().upsertAll(entities)
            }
            upsertSectionSuccess(
                tripKey = tripKey,
                section = TripSyncSection.OPTIONS,
                version = optionsVersionGroup.takeIf { it > 0 }?.toString() ?: now.toString(),
                now = now
            )
        }
    }

    suspend fun getTripOptionsVersionGroup(tripKey: TripKey): Long? {
        return database.eventOptionDao().getTripOptionsVersionGroup(
            ownerUid = tripKey.ownerUid,
            tripId = tripKey.tripId
        )
    }

    suspend fun getTripOptions(tripKey: TripKey): Map<String, List<EventOption>> {
        return database.eventOptionDao()
            .getForTrip(tripKey.ownerUid, tripKey.tripId)
            .groupBy(EventOptionEntity::eventId)
            .mapValues { (_, entities) -> entities.map(EventOptionEntity::toDomainModel) }
    }

    suspend fun recordManifestCheck(viewerUid: String, manifestVersion: Long?) {
        val now = System.currentTimeMillis()
        val current = database.syncStateDao().getById(homeSyncStateId(viewerUid))
        upsertSyncState(
            id = homeSyncStateId(viewerUid),
            remoteVersion = manifestVersion?.toString(),
            localVersion = current?.localVersion,
            checkedAtEpochMs = now,
            successfulAtEpochMs = current?.lastSuccessfulSyncAtEpochMs,
            status = current?.syncStatus ?: SYNC_STATUS_IDLE,
            error = current?.error
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
        upsertSyncState(
            id = homeSyncStateId(viewerUid),
            remoteVersion = current?.remoteVersion,
            localVersion = current?.localVersion,
            checkedAtEpochMs = now,
            successfulAtEpochMs = current?.lastSuccessfulSyncAtEpochMs,
            status = SYNC_STATUS_ERROR,
            error = error.message
        )
    }

    suspend fun getUserStubs(
        viewerUid: String,
        userUids: Collection<String>
    ): Map<String, LocalUserStub> {
        val normalizedUserUids = userUids.filter { it.isNotBlank() }.distinct()
        if (normalizedUserUids.isEmpty()) return emptyMap()

        return database.userStubDao()
            .getForViewer(viewerUid, normalizedUserUids)
            .associate { entity ->
                entity.userUid to LocalUserStub(
                    userUid = entity.userUid,
                    displayName = entity.displayName,
                    avatarUrl = entity.photoUrl
                )
            }
    }

    suspend fun recordTripSectionCheck(
        tripKey: TripKey,
        section: TripSyncSection,
        remoteVersion: Long?,
        localVersion: String? = null
    ) {
        val now = System.currentTimeMillis()
        val existing = database.syncStateDao().getById(tripSyncStateId(tripKey, section))
        upsertSyncState(
            id = tripSyncStateId(tripKey, section),
            remoteVersion = remoteVersion?.toString(),
            localVersion = localVersion ?: existing?.localVersion,
            checkedAtEpochMs = now,
            successfulAtEpochMs = existing?.lastSuccessfulSyncAtEpochMs,
            status = existing?.syncStatus ?: SYNC_STATUS_IDLE,
            error = existing?.error
        )
    }

    suspend fun recordTripSectionFailure(
        tripKey: TripKey,
        section: TripSyncSection,
        remoteVersion: Long?,
        error: Throwable
    ) {
        val now = System.currentTimeMillis()
        val existing = database.syncStateDao().getById(tripSyncStateId(tripKey, section))
        upsertSyncState(
            id = tripSyncStateId(tripKey, section),
            remoteVersion = remoteVersion?.toString() ?: existing?.remoteVersion,
            localVersion = existing?.localVersion,
            checkedAtEpochMs = now,
            successfulAtEpochMs = existing?.lastSuccessfulSyncAtEpochMs,
            status = SYNC_STATUS_ERROR,
            error = error.message
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

    suspend fun upsertMediaAssets(
        tripKey: TripKey,
        assets: List<CachedMediaAsset>
    ) {
        if (assets.isEmpty()) return
        val now = System.currentTimeMillis()
        database.mediaAssetDao().upsertAll(
            assets.map { asset ->
                MediaAssetEntity(
                    id = mediaAssetEntityId(
                        ownerUid = tripKey.ownerUid,
                        tripId = tripKey.tripId,
                        remoteUrl = asset.remoteUrl
                    ),
                    ownerUid = tripKey.ownerUid,
                    tripId = tripKey.tripId,
                    remoteUrl = asset.remoteUrl,
                    localPath = asset.localPath,
                    contentHash = asset.contentHash,
                    downloadedAtEpochMs = now,
                    lastAccessedAtEpochMs = now
                )
            }
        )
    }

    suspend fun deleteMediaAssetsForTrip(tripKey: TripKey) {
        database.mediaAssetDao().deleteForTrip(tripKey.ownerUid, tripKey.tripId)
    }

    private suspend fun upsertUserStubs(
        viewerUid: String,
        stubs: List<LocalUserStub>,
        now: Long
    ) {
        val normalizedStubs = stubs
            .filter { stub -> stub.userUid.isNotBlank() }
            .distinctBy(LocalUserStub::userUid)
        if (normalizedStubs.isEmpty()) return

        database.userStubDao().upsertAll(
            normalizedStubs.map { stub ->
                UserStubEntity(
                    id = userStubEntityId(viewerUid, stub.userUid),
                    viewerUid = viewerUid,
                    userUid = stub.userUid,
                    displayName = stub.displayName,
                    photoUrl = stub.avatarUrl,
                    updatedAtEpochMs = now
                )
            }
        )
    }

    private suspend fun upsertSectionSuccess(
        tripKey: TripKey,
        section: TripSyncSection,
        version: String,
        now: Long
    ) {
        upsertSyncState(
            id = tripSyncStateId(tripKey, section),
            remoteVersion = version,
            localVersion = version,
            checkedAtEpochMs = now,
            successfulAtEpochMs = now,
            status = SYNC_STATUS_SUCCESS,
            error = null
        )
    }

    private suspend fun upsertSyncState(
        id: String,
        remoteVersion: String?,
        localVersion: String?,
        checkedAtEpochMs: Long?,
        successfulAtEpochMs: Long?,
        status: String,
        error: String?
    ) {
        database.syncStateDao().upsert(
            SyncStateEntity(
                id = id,
                remoteVersion = remoteVersion,
                localVersion = localVersion,
                lastCheckedAtEpochMs = checkedAtEpochMs,
                lastSuccessfulSyncAtEpochMs = successfulAtEpochMs,
                syncStatus = status,
                error = error
            )
        )
    }

    private companion object {
        const val APP_STATE_LAST_LOGGED_IN_UID = "lastLoggedInUid"
        const val APP_STATE_LAST_OPENED_TRIP_OWNER_UID = "lastOpenedTripOwnerUid"
        const val APP_STATE_LAST_OPENED_TRIP_ID = "lastOpenedTripId"
        const val SYNC_STATUS_IDLE = "idle"
        const val SYNC_STATUS_SUCCESS = "success"
        const val SYNC_STATUS_ERROR = "error"

        fun homeSyncStateId(viewerUid: String): String = "user:$viewerUid:home_summaries"

        fun tripSyncStateId(tripKey: TripKey, section: TripSyncSection): String =
            "trip:${tripKey.ownerUid}:${tripKey.tripId}:${section.wireValue}"
    }
}
