package com.example.travelcents.data.model

import java.util.UUID

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

    // Cache map omits itineraryId so cached events can be reused across trips
    fun toCacheMap(): Map<String, Any> =
        toFirestoreMap().toMutableMap().also { it.remove("itineraryId") }

    companion object {
        private val RESERVED = setOf("eventId", "type", "itineraryId", "tz", "date", "startTime", "endTime")

        fun fromCacheMap(map: Map<String, Any>): TravelEvent = TravelEvent(
            eventId = map["eventId"] as? String ?: UUID.randomUUID().toString(),
            type = map["type"] as? String ?: "",
            itineraryId = "",
            tz = map["tz"] as? String ?: "",
            date = map["date"] as? String ?: "",
            startTime = map["startTime"] as? String ?: "",
            endTime = map["endTime"] as? String ?: "",
            details = map.filterKeys { it !in RESERVED }.mapValues { it.value.toString() }
        )
    }
}