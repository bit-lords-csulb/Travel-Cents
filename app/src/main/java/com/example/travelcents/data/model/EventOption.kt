package com.example.travelcents.data.model

import java.util.UUID

data class EventOption(
    val optionId: String = UUID.randomUUID().toString(),
    val eventId: String = "",
    // "serp", "yelp", "groq"
    val source: String = "",
    val selected: Boolean = false,
    // userId -> vote value; session-only rejection state lives in ViewModel, not here
    val votes: Map<String, String> = emptyMap(),
    val imageUrl: String = "",
    val localImagePath: String = "",
    val details: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("optionId", optionId)
        put("eventId", eventId)
        put("source", source)
        put("selected", selected)
        put("votes", votes)
        put("imageUrl", imageUrl)
        put("localImagePath", localImagePath)
        putAll(details)
    }

    companion object {
        private val RESERVED = setOf(
            "optionId", "eventId", "source", "selected", "votes", "imageUrl", "localImagePath"
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): EventOption {
            val votes = (map["votes"] as? Map<String, String>) ?: emptyMap()
            return EventOption(
                optionId = map["optionId"] as? String ?: UUID.randomUUID().toString(),
                eventId = map["eventId"] as? String ?: "",
                source = map["source"] as? String ?: "",
                selected = map["selected"] as? Boolean ?: false,
                votes = votes,
                imageUrl = map["imageUrl"] as? String ?: "",
                localImagePath = map["localImagePath"] as? String ?: "",
                details = map.filterKeys { it !in RESERVED }.mapValues { it.value.toString() }
            )
        }
    }
}