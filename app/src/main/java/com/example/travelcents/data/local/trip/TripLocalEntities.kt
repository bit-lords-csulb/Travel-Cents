package com.example.travelcents.data.local.trip

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travelcents.data.trip.model.Itinerary

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
