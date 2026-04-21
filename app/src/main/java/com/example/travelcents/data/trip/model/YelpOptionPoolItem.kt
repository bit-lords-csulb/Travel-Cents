package com.example.travelcents.data.trip.model

const val YELP_POOL_TYPE_RESTAURANTS = "restaurants"
const val YELP_POOL_TYPE_ACTIVITIES = "activities"

data class YelpOptionPoolItem(
    val providerId: String,
    val source: String = "yelp",
    val name: String = "",
    val imageUrl: String = "",
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val priceTier: String = "",
    val categories: List<String> = emptyList(),
    val shortAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val staticMapUrl: String = "",
    val staticMapProvider: String = "",
    val yelpUrl: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = buildMap {
        put("providerId", providerId)
        put("source", source)
        put("name", name)
        if (imageUrl.isNotBlank()) put("imageUrl", imageUrl)
        rating?.let { put("rating", it) }
        reviewCount?.let { put("reviewCount", it) }
        if (priceTier.isNotBlank()) put("priceTier", priceTier)
        if (categories.isNotEmpty()) put("categories", categories)
        if (shortAddress.isNotBlank()) put("shortAddress", shortAddress)
        latitude?.let { put("latitude", it) }
        longitude?.let { put("longitude", it) }
        if (staticMapUrl.isNotBlank()) put("staticMapUrl", staticMapUrl)
        if (staticMapProvider.isNotBlank()) put("staticMapProvider", staticMapProvider)
        if (yelpUrl.isNotBlank()) put("yelpUrl", yelpUrl)
    }

    fun toEventDetails(): Map<String, String> = buildMap {
        put(DETAIL_YELP_ID, providerId)
        if (name.isNotBlank()) {
            put(ATTR_BUSINESS_NAME, name)
            put("title", name)
        }
        rating?.let { put(ATTR_AVERAGE_RATING, it.toString()) }
        reviewCount?.let { put(ATTR_REVIEW_COUNT, it.toString()) }
        if (priceTier.isNotBlank()) put(ATTR_PRICE_TIER, priceTier)
        if (categories.isNotEmpty()) put(ATTR_CATEGORIES, categories.joinToString(", "))
        if (shortAddress.isNotBlank()) put(ATTR_BUSINESS_ADDRESS, shortAddress)
        latitude?.let { put(ATTR_LATITUDE, it.toString()) }
        longitude?.let { put(ATTR_LONGITUDE, it.toString()) }
        if (staticMapUrl.isNotBlank()) put(ATTR_STATIC_MAP_URL, staticMapUrl)
        if (staticMapProvider.isNotBlank()) put(ATTR_STATIC_MAP_PROVIDER, staticMapProvider)
        if (yelpUrl.isNotBlank()) put(ATTR_YELP_URL, yelpUrl)
        if (imageUrl.isNotBlank()) put(ATTR_PROFILE_PHOTO_URL, imageUrl)
    }

    fun toEventOption(
        eventId: String,
        selected: Boolean
    ): EventOption {
        return EventOption(
            optionId = providerId,
            eventId = eventId,
            source = source,
            selected = selected,
            imageUrl = imageUrl,
            details = toEventDetails()
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(
            documentId: String,
            map: Map<String, Any>
        ): YelpOptionPoolItem {
            return YelpOptionPoolItem(
                providerId = map["providerId"]?.toString().orEmpty().ifBlank { documentId },
                source = map["source"]?.toString().orEmpty().ifBlank { "yelp" },
                name = map["name"]?.toString().orEmpty(),
                imageUrl = map["imageUrl"]?.toString().orEmpty(),
                rating = (map["rating"] as? Number)?.toDouble(),
                reviewCount = (map["reviewCount"] as? Number)?.toInt(),
                priceTier = map["priceTier"]?.toString().orEmpty(),
                categories = (map["categories"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
                shortAddress = map["shortAddress"]?.toString().orEmpty(),
                latitude = (map["latitude"] as? Number)?.toDouble(),
                longitude = (map["longitude"] as? Number)?.toDouble(),
                staticMapUrl = map["staticMapUrl"]?.toString().orEmpty(),
                staticMapProvider = map["staticMapProvider"]?.toString().orEmpty(),
                yelpUrl = map["yelpUrl"]?.toString().orEmpty()
            )
        }
    }
}
