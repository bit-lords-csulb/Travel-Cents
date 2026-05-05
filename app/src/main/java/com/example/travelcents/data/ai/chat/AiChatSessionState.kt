package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.trip.model.TravelEvent

enum class AiChatPreviewDraftType {
    DESTINATION_LOCK,
    CURATED_STARTER
}

data class AiChatPreviewDraft(
    val type: AiChatPreviewDraftType,
    val destination: String = "",
    val intakeProfile: AiTripIntakeProfile = AiTripIntakeProfile(),
    val curatedStarter: AiCuratedTripStarter? = null,
    val addedEvents: List<TravelEvent> = emptyList()
)

data class AiChatSessionState(
    val sessionId: String? = null,
    val title: String = "",
    val profile: AiTravelerProfile = AiTravelerProfile(),
    val intakeProfile: AiTripIntakeProfile = AiTravelerProfile().intakeProfile(),
    val planningObjective: String = "",
    val stage: AiChatStage = AiChatStage.ONBOARDING,
    val preferenceProfile: PreferenceProfile = PreferenceProfile(),
    val discoveryTrack: DiscoveryTrack = DiscoveryTrack.NOT_STARTED,
    val discoverySlots: List<DiscoverySlot> = emptyList(),
    val discoverySuggestionPool: List<SuggestionItem> = emptyList(),
    val llmHistory: List<LlmMessage> = emptyList(),
    val starterCards: List<AiChatCardOption> = emptyList(),
    val draftText: String = "",
    val selectedDraftOptions: List<AiChatCardOption> = emptyList(),
    val askedFollowUpGroupIds: List<String> = emptyList(),
    val activeResponseCardGroup: AiChatCardGroup? = null,
    val activeDestinationRecommendationRow: AiDestinationRecommendationRow? = null,
    val activeCuratedTripRow: AiCuratedTripRow? = null,
    val activePlaceRecommendationRow: AiPlaceRecommendationRow? = null,
    val activeSingleEventCard: AiSingleEventSuggestion? = null,
    val activePreferenceQuestionCard: AiChatItem.PreferenceQuestionCard? = null,
    val activeSuggestionCarouselCard: AiChatItem.SuggestionCarouselCard? = null,
    val anchorMessageId: String? = null,
    val lockedDestination: String? = null,
    val lockedDestinationImageUrl: String? = null,
    val previewDraft: AiChatPreviewDraft? = null
)

data class AiChatUiState(
    val items: List<AiChatItem> = emptyList(),
    val starterCards: List<AiChatCardOption> = emptyList(),
    val draftText: String = "",
    val selectedDraftOptions: List<AiChatCardOption> = emptyList(),
    val composerHint: String = "",
    val requiresTypedDraft: Boolean = false,
    val historyEntries: List<AiChatHistoryEntry> = emptyList(),
    val anchorMessageId: String? = null,
    val activeResponseCardGroupId: String? = null,
    val activeDestinationRecommendationRowId: String? = null,
    val activeCuratedTripRowId: String? = null,
    val activePlaceRecommendationRowId: String? = null,
    val activeSingleEventCardId: String? = null,
    val pendingAddToTripEvent: AiSingleEventSuggestion? = null,
    val availableTrips: List<AiChatTripOption> = emptyList(),
    val bookmarkedPlaceIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val lockedDestination: String? = null,
    val lockedDestinationImageUrl: String? = null,
    val previewDraft: AiChatPreviewDraft? = null
)

data class AiChatTripOption(
    val tripId: String,
    val ownerUid: String,
    val title: String,
    val destination: String,
    val dateWindow: String,
    val imageUrl: String?
)
