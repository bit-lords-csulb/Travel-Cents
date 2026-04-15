package com.example.travelcents.data.trip.model

import java.util.UUID

data class EventOption(
    val optionId: String = UUID.randomUUID().toString(),
    val eventId: String = "",
    // "serp", "yelp", "llm"
    val source: String = "",
    val selected: Boolean = false,
    // userId -> vote value; session-only rejection state lives in ViewModel, not here
    val votes: Map<String, String> = emptyMap(),
    val imageUrl: String = "",
    val localImagePath: String = "",
    val photoUrls: List<String> = emptyList(),
    val details: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("optionId", optionId)
        put("eventId", eventId)
        put("source", source)
        put("selected", selected)
        put("votes", votes)
        put("imageUrl", imageUrl)
        put("photoUrls", photoUrls)
        putAll(details)
    }

    companion object {
        private val RESERVED = setOf(
            "optionId", "eventId", "source", "selected", "votes", "imageUrl", "localImagePath", "photoUrls"
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): EventOption {
            val votes = (map["votes"] as? Map<String, String>) ?: emptyMap()
            val photos = (map["photoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val rawImageUrl = map["imageUrl"] as? String ?: ""
            val rawLocalImagePath = map["localImagePath"] as? String ?: ""
            val localImagePath = rawLocalImagePath.ifBlank {
                rawImageUrl.takeIf(::looksLikeLocalImagePath).orEmpty()
            }
            val imageUrl = rawImageUrl.takeUnless(::looksLikeLocalImagePath).orEmpty()
            return EventOption(
                optionId = map["optionId"] as? String ?: UUID.randomUUID().toString(),
                eventId = map["eventId"] as? String ?: "",
                source = map["source"] as? String ?: "",
                selected = map["selected"] as? Boolean ?: false,
                votes = votes,
                imageUrl = imageUrl,
                localImagePath = localImagePath,
                photoUrls = photos,
                details = map.filterKeys { it !in RESERVED }.mapValues { it.value.toString() }
            )
        }

        private fun looksLikeLocalImagePath(value: String): Boolean {
            return value.startsWith("/") || value.startsWith("file:/")
        }
    }
}


