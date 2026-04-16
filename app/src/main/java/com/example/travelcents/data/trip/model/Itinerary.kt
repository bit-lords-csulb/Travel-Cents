package com.example.travelcents.data.trip.model

data class Itinerary(
    val itineraryId: String,
    val userId: String,
    val tripName: String,
    val destination: String,
    val origin: String,
    val originIata: String = "",
    val destinationIata: String = "",
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
    val homeImageUrl: String = "",
    val ownerUid: String = userId,
    val memberUids: List<String> = listOf(ownerUid),
    val roleByUid: Map<String, String> = mapOf(ownerUid to "owner"),
    val accessSchemaVersion: Int = ACCESS_SCHEMA_VERSION
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "itineraryId" to itineraryId,
        "userId" to userId,
        "ownerUid" to ownerUid,
        "tripName" to tripName,
        "destination" to destination,
        "origin" to origin,
        "originIata" to originIata,
        "destinationIata" to destinationIata,
        "dateFrom" to dateFrom,
        "dateTo" to dateTo,
        "durationDays" to durationDays,
        "currency" to currency,
        "travelStyle" to travelStyle,
        "adults" to adults,
        "children" to children,
        "createdAt" to createdAt,
        "status" to status,
        "eventIds" to eventIds,
        "homeImageUrl" to homeImageUrl,
        "memberUids" to memberUids,
        "roleByUid" to roleByUid,
        "accessSchemaVersion" to accessSchemaVersion
    )

    companion object {
        const val ACCESS_SCHEMA_VERSION = 1
    }
}


