package com.example.travelcents.data.model

data class TravelEvent(
    val eventId: String,
    val type: String,
    val itineraryId: String,
    val tz: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val details: Map<String, String> = emptyMap()
) {
    fun toFirestoreMap(): Map<String, Any> = buildMap {
        put("eventId", eventId)
        put("type", type)
        put("itineraryId", itineraryId)
        put("tz", tz)
        put("date", date)
        put("startTime", startTime)
        put("endTime", endTime)
        putAll(details)
    }
}