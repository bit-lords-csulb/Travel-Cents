package com.example.travelcents.data.model

import com.google.gson.annotations.SerializedName

// ── Business Search ──────────────────────────────────────────────────────────

data class YelpSearchResponse(
    val businesses: List<YelpBusiness> = emptyList(),
    val total: Int = 0
)

data class YelpBusiness(
    val id: String = "",
    val name: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    val rating: Double = 0.0,
    @SerializedName("review_count") val reviewCount: Int = 0,
    val price: String? = null,
    val categories: List<YelpCategory> = emptyList(),
    val location: YelpLocation? = null,
    val phone: String? = null,
    val coordinates: YelpCoordinates? = null,
    @SerializedName("is_closed") val isClosed: Boolean = false,
    val transactions: List<String> = emptyList(),
    val distance: Double? = null,
    // populated on detail fetch (/v3/businesses/{id})
    val hours: List<YelpHours>? = null,
    val photos: List<String>? = null,
    @SerializedName("attributes") val attributes: Map<String, Any>? = null
)

data class YelpCategory(
    val alias: String = "",
    val title: String = ""
)

data class YelpLocation(
    @SerializedName("address1") val address1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    @SerializedName("display_address") val displayAddress: List<String> = emptyList()
)

data class YelpCoordinates(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class YelpHours(
    @SerializedName("hours_type") val hoursType: String? = null,
    val open: List<YelpOpenHours>? = null,
    @SerializedName("is_open_now") val isOpenNow: Boolean? = null
)

data class YelpOpenHours(
    val day: Int = 0,
    val start: String = "",
    val end: String = "",
    @SerializedName("is_overnight") val isOvernight: Boolean = false
)

// ── Reviews ──────────────────────────────────────────────────────────────────

data class YelpReviewsResponse(
    val reviews: List<YelpReview> = emptyList(),
    val total: Int = 0
)

data class YelpReview(
    val id: String = "",
    val rating: Int = 0,
    val text: String = "",
    val url: String = "",
    val user: YelpUser? = null,
    @SerializedName("time_created") val timeCreated: String = ""
)

data class YelpUser(
    val name: String = "",
    @SerializedName("image_url") val imageUrl: String = ""
)

// ── Events Search ────────────────────────────────────────────────────────────

data class YelpEventsResponse(
    val events: List<YelpEvent> = emptyList(),
    val total: Int = 0
)

data class YelpEvent(
    val id: String = "",
    val name: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    val category: String = "",
    val description: String = "",
    @SerializedName("is_free") val isFree: Boolean = false,
    val cost: Double? = null,
    @SerializedName("cost_max") val costMax: Double? = null,
    @SerializedName("tickets_url") val ticketsUrl: String? = null,
    @SerializedName("time_start") val timeStart: String = "",
    @SerializedName("time_end") val timeEnd: String? = null,
    val location: YelpLocation? = null
)