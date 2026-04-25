package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.model.LlmMessage

data class AiChatSessionState(
    val sessionId: String? = null,
    val title: String = "",
    val profile: AiTravelerProfile = AiTravelerProfile(),
    val intakeProfile: AiTripIntakeProfile = AiTravelerProfile().intakeProfile(),
    val planningObjective: String = "",
    val stage: AiChatStage = AiChatStage.ONBOARDING,
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
    val anchorMessageId: String? = null
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
    val isLoading: Boolean = false
)

data class AiChatTripOption(
    val tripId: String,
    val ownerUid: String,
    val title: String,
    val destination: String,
    val dateWindow: String,
    val imageUrl: String?
)
