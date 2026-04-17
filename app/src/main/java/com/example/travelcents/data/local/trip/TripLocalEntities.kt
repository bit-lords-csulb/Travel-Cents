package com.example.travelcents.data.local.trip

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent

@Entity(tableName = "trip_summary")
data class TripSummaryEntity(
    @PrimaryKey val id: String,
    val viewerUid: String,
    val ownerUid: String,
    val tripId: String,
    val userId: String,
    val tripName: String,
    val destination: String,
    val origin: String,
    val originIata: String,
    val destinationIata: String,
    val dateFrom: String,
    val dateTo: String,
    val durationDays: Int,
    val currency: String,
    val travelStyle: String,
    val adults: Int,
    val children: Int,
    val createdAt: String,
    val status: String,
    val eventIds: List<String>,
    val homeImageUrl: String,
    val memberUids: List<String>,
    val roleByUid: Map<String, String>,
    val accessSchemaVersion: Int,
    val summaryVersion: Long,
    val eventsVersion: Long,
    val optionsVersion: Long,
    val membersVersion: Long,
    val lastHydratedAtEpochMs: Long,
    val isCurrentCandidate: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "user_stub")
data class UserStubEntity(
    @PrimaryKey val id: String,
    val viewerUid: String,
    val userUid: String,
    val displayName: String,
    val photoUrl: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: String,
    val remoteVersion: String?,
    val localVersion: String?,
    val lastCheckedAtEpochMs: Long?,
    val lastSuccessfulSyncAtEpochMs: Long?,
    val syncStatus: String,
    val error: String?
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val key: String,
    val stringValue: String?,
    val updatedAtEpochMs: Long
)

fun TripSummaryEntity.toDomainModel(): Itinerary = Itinerary(
    itineraryId = tripId,
    userId = userId,
    tripName = tripName,
    destination = destination,
    origin = origin,
    originIata = originIata,
    destinationIata = destinationIata,
    dateFrom = dateFrom,
    dateTo = dateTo,
    durationDays = durationDays,
    currency = currency,
    travelStyle = travelStyle,
    adults = adults,
    children = children,
    createdAt = createdAt,
    status = status,
    eventIds = eventIds,
    homeImageUrl = homeImageUrl,
    ownerUid = ownerUid,
    memberUids = memberUids,
    roleByUid = roleByUid,
    accessSchemaVersion = accessSchemaVersion,
    summaryVersion = summaryVersion,
    eventsVersion = eventsVersion,
    optionsVersion = optionsVersion,
    membersVersion = membersVersion,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Itinerary.toTripSummaryEntity(
    viewerUid: String,
    updatedAtEpochMs: Long,
    isCurrentCandidate: Boolean
): TripSummaryEntity = TripSummaryEntity(
    id = tripSummaryEntityId(viewerUid, ownerUid, itineraryId),
    viewerUid = viewerUid,
    ownerUid = ownerUid,
    tripId = itineraryId,
    userId = userId,
    tripName = tripName,
    destination = destination,
    origin = origin,
    originIata = originIata,
    destinationIata = destinationIata,
    dateFrom = dateFrom,
    dateTo = dateTo,
    durationDays = durationDays,
    currency = currency,
    travelStyle = travelStyle,
    adults = adults,
    children = children,
    createdAt = createdAt,
    status = status,
    eventIds = eventIds,
    homeImageUrl = homeImageUrl,
    memberUids = memberUids,
    roleByUid = roleByUid,
    accessSchemaVersion = accessSchemaVersion,
    summaryVersion = summaryVersion,
    eventsVersion = eventsVersion,
    optionsVersion = optionsVersion,
    membersVersion = membersVersion,
    lastHydratedAtEpochMs = updatedAtEpochMs,
    isCurrentCandidate = isCurrentCandidate,
    updatedAtEpochMs = updatedAtEpochMs
)

fun tripSummaryEntityId(viewerUid: String, ownerUid: String, tripId: String): String =
    "$viewerUid:$ownerUid:$tripId"

@Entity(tableName = "trip_event")
data class TripEventEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val tripId: String,
    val eventId: String,
    val type: String,
    val itineraryId: String,
    val tz: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val imageUrl: String,
    val localImagePath: String,
    val photoUrls: List<String>,
    val details: Map<String, String>,
    val eventVersionGroup: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "trip_member")
data class TripMemberEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val tripId: String,
    val memberUid: String,
    val role: String,
    val memberVersion: Long,
    val displayName: String,
    val avatarUrl: String,
    val updatedAtEpochMs: Long
)

data class LocalTripMember(
    val memberUid: String,
    val role: String,
    val displayName: String,
    val avatarUrl: String
)

data class LocalUserStub(
    val userUid: String,
    val displayName: String,
    val avatarUrl: String
)

@Entity(tableName = "event_option")
data class EventOptionEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val tripId: String,
    val eventId: String,
    val optionId: String,
    val source: String,
    val selected: Boolean,
    val votes: Map<String, String>,
    val imageUrl: String,
    val localImagePath: String,
    val photoUrls: List<String>,
    val details: Map<String, String>,
    val optionsVersionGroup: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "media_asset")
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val tripId: String,
    val remoteUrl: String,
    val localPath: String,
    val contentHash: String,
    val downloadedAtEpochMs: Long,
    val lastAccessedAtEpochMs: Long
)

fun TripEventEntity.toDomainModel(): TravelEvent = TravelEvent(
    eventId = eventId,
    type = type,
    itineraryId = itineraryId,
    tz = tz,
    date = date,
    startTime = startTime,
    endTime = endTime,
    imageUrl = imageUrl,
    localImagePath = localImagePath,
    photoUrls = photoUrls,
    details = details
)

fun TravelEvent.toTripEventEntity(
    ownerUid: String,
    tripId: String,
    eventVersionGroup: Long,
    updatedAtEpochMs: Long
): TripEventEntity = TripEventEntity(
    id = tripEventEntityId(ownerUid = ownerUid, tripId = tripId, eventId = eventId),
    ownerUid = ownerUid,
    tripId = tripId,
    eventId = eventId,
    type = type,
    itineraryId = itineraryId,
    tz = tz,
    date = date,
    startTime = startTime,
    endTime = endTime,
    imageUrl = imageUrl,
    localImagePath = localImagePath,
    photoUrls = photoUrls,
    details = details,
    eventVersionGroup = eventVersionGroup,
    updatedAtEpochMs = updatedAtEpochMs
)

fun EventOptionEntity.toDomainModel(): EventOption = EventOption(
    optionId = optionId,
    eventId = eventId,
    tripId = tripId,
    ownerUid = ownerUid,
    source = source,
    selected = selected,
    votes = votes,
    imageUrl = imageUrl,
    localImagePath = localImagePath,
    photoUrls = photoUrls,
    details = details
)

fun EventOption.toEntity(
    ownerUid: String,
    tripId: String,
    eventId: String,
    optionsVersionGroup: Long,
    updatedAtEpochMs: Long
): EventOptionEntity = EventOptionEntity(
    id = eventOptionEntityId(
        ownerUid = ownerUid,
        tripId = tripId,
        eventId = eventId,
        optionId = optionId
    ),
    ownerUid = ownerUid,
    tripId = tripId,
    eventId = eventId,
    optionId = optionId,
    source = source,
    selected = selected,
    votes = votes,
    imageUrl = imageUrl,
    localImagePath = localImagePath,
    photoUrls = photoUrls,
    details = details,
    optionsVersionGroup = optionsVersionGroup,
    updatedAtEpochMs = updatedAtEpochMs
)

fun tripEventEntityId(ownerUid: String, tripId: String, eventId: String): String =
    "$ownerUid:$tripId:$eventId"

fun tripMemberEntityId(ownerUid: String, tripId: String, memberUid: String): String =
    "$ownerUid:$tripId:$memberUid"

fun eventOptionEntityId(ownerUid: String, tripId: String, eventId: String, optionId: String): String =
    "$ownerUid:$tripId:$eventId:$optionId"

fun mediaAssetEntityId(ownerUid: String, tripId: String, remoteUrl: String): String =
    "$ownerUid:$tripId:$remoteUrl"

fun userStubEntityId(viewerUid: String, userUid: String): String =
    "$viewerUid:$userUid"
