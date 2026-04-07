package com.example.travelcents.data.model

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
    val homeImageUrl: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "itineraryId" to itineraryId,
        "userId" to userId,
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
        "homeImageUrl" to homeImageUrl
    )
}
