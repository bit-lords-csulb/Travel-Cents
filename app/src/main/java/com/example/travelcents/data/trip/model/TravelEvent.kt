package com.example.travelcents.data.trip.model

import com.google.firebase.firestore.PropertyName
import java.util.Locale
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
    @get:PropertyName("isNativeBookable")
    @field:PropertyName("isNativeBookable")
    val isNativeBookable: String? = null,
    @get:PropertyName("booking_url")
    @field:PropertyName("booking_url")
    val bookingUrl: String? = null,
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
        isNativeBookable?.let { put("isNativeBookable", it) }
        bookingUrl?.let { put("booking_url", it) }
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
            "selectedOptionId", "isNativeBookable", "nativeBookable",
            "bookingUrl", "booking_url", "options"
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
            val details = normalizedDetails(map)

            return TravelEvent(
                eventId = map["eventId"] as? String ?: fallbackEventId,
                type = map.firstString("type").ifBlank { inferType(details) },
                itineraryId = map["itineraryId"] as? String ?: fallbackItineraryId,
                tz = map["tz"] as? String ?: "",
                date = map["date"] as? String ?: "",
                startTime = map["startTime"] as? String ?: "",
                endTime = map["endTime"] as? String ?: "",
                imageUrl = imageUrl,
                localImagePath = localImagePath,
                photoUrls = photos,
                selectedOptionId = map["selectedOptionId"] as? String ?: "",
                isNativeBookable = map["isNativeBookable"]?.toString()
                    ?: map["nativeBookable"]?.toString(),
                bookingUrl = map["bookingUrl"]?.toString()
                    ?: map["booking_url"]?.toString(),
                details = details,
                options = options
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

        private fun inferType(details: Map<String, String>): String {
            val searchable = listOf(
                details[ATTR_CATEGORIES],
                details["yelpCategory"],
                details["category"],
                details["title"],
                details["name"]
            ).joinToString(" ").lowercase(Locale.US)

            return if (restaurantCategoryTokens.any { token -> token in searchable }) {
                "restaurant"
            } else {
                "activity"
            }
        }

        private fun looksLikeLocalImagePath(value: String): Boolean {
            return value.startsWith("/") || value.startsWith("file:/")
        }

        private val restaurantCategoryTokens = listOf(
            "restaurant",
            "dining",
            "food",
            "cafe",
            "coffee",
            "bar",
            "brewery",
            "bakery",
            "dessert",
            "sushi",
            "pizza",
            "ramen",
            "taco",
            "burger",
            "brunch",
            "breakfast",
            "steak",
            "seafood",
            "bbq",
            "wine",
            "cocktail",
            "sandwich",
            "noodle",
            "ice cream"
        )
    }
}
