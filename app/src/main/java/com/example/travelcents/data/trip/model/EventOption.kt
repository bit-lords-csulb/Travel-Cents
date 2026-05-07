package com.example.travelcents.data.trip.model

import java.util.UUID

data class EventOption(
    val optionId: String = UUID.randomUUID().toString(),
    val eventId: String = "",
    val tripId: String = "",
    val ownerUid: String = "",
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
    fun scopedTo(ownerUid: String, tripId: String, eventId: String = this.eventId): EventOption {
        return copy(
            ownerUid = ownerUid,
            tripId = tripId,
            eventId = eventId
        )
    }

    fun toMap(): Map<String, Any> = buildMap {
        put("optionId", optionId)
        put("eventId", eventId)
        if (tripId.isNotBlank()) put("tripId", tripId)
        if (ownerUid.isNotBlank()) put("ownerUid", ownerUid)
        put("source", source)
        put("selected", selected)
        put("votes", votes)
        put("imageUrl", imageUrl)
        put("photoUrls", photoUrls)
        putAll(details)
    }

    companion object {
        private val RESERVED = setOf(
            "optionId", "eventId", "tripId", "ownerUid", "source", "selected", "votes",
            "imageUrl", "localImagePath", "photoUrls"
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): EventOption {
            val votes = (map["votes"] as? Map<String, String>) ?: emptyMap()
            val rawImageUrl = map.firstString(
                "imageUrl",
                "photoUrl",
                "yelpImageUrl",
                ATTR_HERO_IMAGE_URL,
                ATTR_PROFILE_PHOTO_URL
            )
            val rawLocalImagePath = map["localImagePath"] as? String ?: ""
            val localImagePath = rawLocalImagePath.ifBlank {
                rawImageUrl.takeIf(::looksLikeLocalImagePath).orEmpty()
            }
            val imageUrl = rawImageUrl.takeUnless(::looksLikeLocalImagePath).orEmpty()
            val photos = map.stringList("photoUrls")
                .plus(
                    map.stringsFor(
                        "photoUrl",
                        "yelpImageUrl",
                        ATTR_HERO_IMAGE_URL,
                        ATTR_PROFILE_PHOTO_URL
                    )
                )
                .filterNot(::looksLikeLocalImagePath)
                .distinct()
            return EventOption(
                optionId = map["optionId"] as? String ?: UUID.randomUUID().toString(),
                eventId = map["eventId"] as? String ?: "",
                tripId = map["tripId"] as? String ?: "",
                ownerUid = map["ownerUid"] as? String ?: "",
                source = map["source"] as? String ?: "",
                selected = map["selected"] as? Boolean ?: false,
                votes = votes,
                imageUrl = imageUrl,
                localImagePath = localImagePath,
                photoUrls = photos,
                details = normalizedDetails(map)
            )
        }

        private fun normalizedDetails(map: Map<String, Any>): Map<String, String> {
            return map.filterKeys { it !in RESERVED }
                .mapValues { it.value.toString() }
                .toMutableMap()
                .apply {
                    putAliasIfPresent(map, "title", "name")
                    putAliasIfPresent(map, "title", ATTR_BUSINESS_NAME)
                    putAliasIfPresent(map, "location", "address")
                    putAliasIfPresent(map, "location", ATTR_BUSINESS_ADDRESS)
                    putAliasIfPresent(map, "yelpId", DETAIL_YELP_ID)
                    putAliasIfPresent(map, "yelpUrl", ATTR_YELP_URL)
                    putAliasIfPresent(map, "yelpImageUrl", ATTR_PROFILE_PHOTO_URL)
                    putAliasIfPresent(map, "photoUrl", ATTR_PROFILE_PHOTO_URL)
                    putAliasIfPresent(map, "yelpCategory", ATTR_CATEGORIES)
                    putAliasIfPresent(map, "yelpRating", ATTR_AVERAGE_RATING)
                    putAliasIfPresent(map, "yelpReviewCount", ATTR_REVIEW_COUNT)
                    putAliasIfPresent(map, "latitude", ATTR_LATITUDE)
                    putAliasIfPresent(map, "lat", ATTR_LATITUDE)
                    putAliasIfPresent(map, "longitude", ATTR_LONGITUDE)
                    putAliasIfPresent(map, "lng", ATTR_LONGITUDE)
                    putAliasIfPresent(map, "lon", ATTR_LONGITUDE)
                    putAliasIfPresent(map, "staticMapUrl", ATTR_STATIC_MAP_URL)
                    putAliasIfPresent(map, "static_map_url", ATTR_STATIC_MAP_URL)
                    putAliasIfPresent(map, "staticMapProvider", ATTR_STATIC_MAP_PROVIDER)
                    putAliasIfPresent(map, "static_map_provider", ATTR_STATIC_MAP_PROVIDER)
                    if (this[ATTR_CATEGORIES].isNullOrBlank()) {
                        map.stringList("yelpCategories")
                            .joinToString(", ")
                            .takeIf { it.isNotBlank() }
                            ?.let { this[ATTR_CATEGORIES] = it }
                    }
                }
        }

        private fun MutableMap<String, String>.putAliasIfPresent(
            source: Map<String, Any>,
            sourceKey: String,
            targetKey: String
        ) {
            if (!this[targetKey].isNullOrBlank()) return
            source[sourceKey]
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { this[targetKey] = it }
        }

        private fun Map<String, Any>.firstString(vararg keys: String): String {
            return keys.asSequence()
                .mapNotNull { key -> this[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                .firstOrNull()
                .orEmpty()
        }

        private fun Map<String, Any>.stringList(key: String): List<String> {
            return (this[key] as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                .orEmpty()
        }

        private fun Map<String, Any>.stringsFor(vararg keys: String): List<String> {
            return keys.mapNotNull { key ->
                this[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
            }
        }

        private fun looksLikeLocalImagePath(value: String): Boolean {
            return value.startsWith("/") || value.startsWith("file:/")
        }
    }
}
