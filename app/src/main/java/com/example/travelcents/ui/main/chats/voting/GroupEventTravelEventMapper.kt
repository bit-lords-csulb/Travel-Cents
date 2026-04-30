package com.example.travelcents.ui.main.chats.voting

import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_PROFILE_PHOTO_URL
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.Event
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.normalizeTime
import java.util.Locale
import java.util.UUID

fun Event.toLinkedTripTravelEvent(
    group: Group,
    linkedTrip: Itinerary?
): TravelEvent {
    val eventType = inferTravelEventType()
    val stableEventId = id.ifBlank { UUID.randomUUID().toString() }
    val categories = yelpCategories
        .ifEmpty { listOf(yelpCategory) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val categoryLine = categories.joinToString(", ")
    val source = if (yelpId.isNotBlank()) "group_chat_yelp" else "group_chat"

    return TravelEvent(
        eventId = stableEventId,
        type = eventType,
        itineraryId = group.linkedTripId.ifBlank { linkedTrip?.itineraryId.orEmpty() },
        tz = linkedTrip?.timeZoneId.orEmpty(),
        date = normalizeDate(date),
        startTime = normalizeTime(startTime.ifBlank { time }),
        endTime = normalizeTime(endTime),
        imageUrl = photoUrl,
        photoUrls = listOf(photoUrl).filter { it.isNotBlank() },
        selectedOptionId = yelpId,
        details = buildMap {
            putIfNotBlank("title", title)
            putIfNotBlank("name", title)
            putIfNotBlank(ATTR_BUSINESS_NAME, title)
            if (eventType == "restaurant") {
                putIfNotBlank("restaurant_name", title)
            } else {
                putIfNotBlank("activity_name", title)
            }
            putIfNotBlank("description", description)
            putIfNotBlank("location", location)
            putIfNotBlank("address", location)
            putIfNotBlank(ATTR_BUSINESS_ADDRESS, location)
            putIfNotBlank("category", yelpCategory)
            putIfNotBlank(ATTR_CATEGORIES, categoryLine)
            putIfNotBlank(DETAIL_YELP_ID, yelpId)
            putIfNotBlank(ATTR_YELP_URL, yelpUrl)
            putIfNotBlank(ATTR_PROFILE_PHOTO_URL, yelpImageUrl.ifBlank { photoUrl })
            yelpRating?.let { put(ATTR_AVERAGE_RATING, it.toString()) }
            if (yelpReviewCount > 0) put(ATTR_REVIEW_COUNT, yelpReviewCount.toString())
            put("source", source)
            putIfNotBlank("group_id", group.id)
            putIfNotBlank("group_name", group.name)
            putIfNotBlank("group_event_id", stableEventId)
            putIfNotBlank("created_by", createdBy)
            putIfNotBlank("created_by_name", createdByName)
            put("group_upvote_count", upvotes.size.toString())
            put("group_downvote_count", downvotes.size.toString())
        }
    )
}

private fun Event.inferTravelEventType(): String {
    val searchable = buildString {
        append(yelpCategory)
        append(' ')
        append(yelpCategories.joinToString(" "))
        append(' ')
        append(title)
    }.lowercase(Locale.US)

    return if (restaurantCategoryTokens.any { token -> token in searchable }) {
        "restaurant"
    } else {
        "activity"
    }
}

private fun MutableMap<String, String>.putIfNotBlank(
    key: String,
    value: String?
) {
    value?.trim()?.takeIf { it.isNotBlank() }?.let { put(key, it) }
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
