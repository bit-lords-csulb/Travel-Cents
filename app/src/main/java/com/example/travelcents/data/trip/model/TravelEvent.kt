package com.example.travelcents.data.trip.model

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
    val localImagePath: String = "",
    val photoUrls: List<String> = emptyList(),
    val selectedOptionId: String = "",
    val details: Map<String, String> = emptyMap(),
    // options are stored as a Firestore subcollection; populated in-memory only
    val options: List<EventOption> = emptyList()
) {
    // Device-local cache paths are not shared/canonical Firestore state.
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
        if (selectedOptionId.isNotBlank()) put("selectedOptionId", selectedOptionId)
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
            "startTime", "endTime", "imageUrl", "localImagePath", "photoUrls",
            "selectedOptionId", "options"
        )

        fun fromFirestoreMap(
            map: Map<String, Any>,
            documentId: String,
            fallbackItineraryId: String
        ): TravelEvent {
            return fromStoredMap(
                map = map,
                fallbackEventId = documentId,
                fallbackItineraryId = fallbackItineraryId,
                options = emptyList()
            )
        }

        fun fromCacheMap(map: Map<String, Any>): TravelEvent {
            @Suppress("UNCHECKED_CAST")
            val opts = (map["options"] as? List<Map<String, Any>>)
                ?.map { EventOption.fromMap(it) }
                ?: emptyList()

            return fromStoredMap(
                map = map,
                fallbackEventId = UUID.randomUUID().toString(),
                fallbackItineraryId = "",
                options = opts
            )
        }

        private fun fromStoredMap(
            map: Map<String, Any>,
            fallbackEventId: String,
            fallbackItineraryId: String,
            options: List<EventOption>
        ): TravelEvent {
            val photos = (map["photoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val rawImageUrl = map["imageUrl"] as? String ?: ""
            val rawLocalImagePath = map["localImagePath"] as? String ?: ""
            val localImagePath = rawLocalImagePath.ifBlank {
                rawImageUrl.takeIf(::looksLikeLocalImagePath).orEmpty()
            }
            val imageUrl = rawImageUrl.takeUnless(::looksLikeLocalImagePath).orEmpty()

            return TravelEvent(
                eventId = map["eventId"] as? String ?: fallbackEventId,
                type = map["type"] as? String ?: "",
                itineraryId = map["itineraryId"] as? String ?: fallbackItineraryId,
                tz = map["tz"] as? String ?: "",
                date = map["date"] as? String ?: "",
                startTime = map["startTime"] as? String ?: "",
                endTime = map["endTime"] as? String ?: "",
                imageUrl = imageUrl,
                localImagePath = localImagePath,
                photoUrls = photos,
                selectedOptionId = map["selectedOptionId"] as? String ?: "",
                details = map.filterKeys { it !in RESERVED }.mapValues { it.value.toString() },
                options = options
            )
        }

        private fun looksLikeLocalImagePath(value: String): Boolean {
            return value.startsWith("/") || value.startsWith("file:/")
        }
    }
}


