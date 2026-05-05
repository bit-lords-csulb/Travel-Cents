package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.model.TravelEvent

data class PreferenceProfile(
    val cuisineTypes: List<String> = emptyList(),
    val diningStyle: String? = null,
    val dietaryRestrictions: List<String> = emptyList(),
    val activityStyles: List<String> = emptyList(),
    val activityPace: String? = null,
    val wantsMusicEvents: Boolean? = null,
    val musicGenres: List<String> = emptyList(),
    val eventTypes: List<String> = emptyList()
)

enum class DiscoveryTrack {
    NOT_STARTED,
    FOOD,
    ACTIVITIES,
    EVENTS,
    COMPLETE
}

enum class DiscoverySlotStatus {
    PENDING_QUESTION,
    READY_TO_SEARCH,
    SEARCHING,
    SHOWING_RESULTS,
    COMPLETED,
    STALE
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER
}

data class DiscoverySlot(
    val id: String,
    val track: DiscoveryTrack,
    val dayIndex: Int? = null,
    val date: String? = null,
    val mealType: MealType? = null,
    val title: String,
    val status: DiscoverySlotStatus = DiscoverySlotStatus.PENDING_QUESTION,
    val page: Int = 0,
    val pageSize: Int = 6,
    val exhausted: Boolean = false,
    val providerCursor: String? = null,
    val shownSuggestionIds: Set<String> = emptySet(),
    val skippedSuggestionIds: Set<String> = emptySet(),
    val addedSuggestionIds: Set<String> = emptySet(),
    val bookmarkedSuggestionIds: Set<String> = emptySet()
)

data class SuggestionItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val imageUrl: String?,
    val address: String,
    val detailUrl: String?,
    val source: String,
    val providerId: String,
    val slotId: String,
    val rawEvent: TravelEvent
)
