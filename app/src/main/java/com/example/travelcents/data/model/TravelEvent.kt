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
    val imageUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val details: Map<String, String> = emptyMap(),
    // options are stored as a Firestore subcollection; populated in-memory only
    val options: List<EventOption> = emptyList()
) {
    // options go in their own subcollection — not included here
    fun toFirestoreMap(): Map<String, Any> = buildMap {
        put("eventId", eventId)
        put("type", type)
        put("itineraryId", itineraryId)
        put("tz", tz)
        put("date", date)
        put("startTime", startTime)
        put("endTime", endTime)
        put("imageUrl", imageUrl)
        put("photoUrls", photoUrls)
        putAll(details)
    }

    // cache map strips itineraryId but serializes options inline
    fun toCacheMap(): Map<String, Any> {
        val base = toFirestoreMap().toMutableMap()
        base.remove("itineraryId")
        base["options"] = options.map { it.toMap() }
        return base
    }

    companion object {
        private val RESERVED = setOf(
            "eventId", "type", "itineraryId", "tz", "date",
            "startTime", "endTime", "imageUrl", "photoUrls", "options"
        )

        fun fromCacheMap(map: Map<String, Any>): TravelEvent {
            @Suppress("UNCHECKED_CAST")
            val opts = (map["options"] as? List<Map<String, Any>>)
                ?.map { EventOption.fromMap(it) }
                ?: emptyList()
            val photos = (map["photoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            return TravelEvent(
                eventId = map["eventId"] as? String ?: UUID.randomUUID().toString(),
                type = map["type"] as? String ?: "",
                itineraryId = "",
                tz = map["tz"] as? String ?: "",
                date = map["date"] as? String ?: "",
                startTime = map["startTime"] as? String ?: "",
                endTime = map["endTime"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                photoUrls = photos,
                details = map.filterKeys { it !in RESERVED }.mapValues { it.value.toString() },
                options = opts
            )
        }
    }
}