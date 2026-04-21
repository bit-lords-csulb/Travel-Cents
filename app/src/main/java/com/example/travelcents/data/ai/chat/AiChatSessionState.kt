package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.model.LlmMessage

data class AiChatSessionState(
    val sessionId: String? = null,
    val title: String = "",
    val profile: AiTravelerProfile = AiTravelerProfile(),
    val intakeProfile: AiTripIntakeProfile = AiTravelerProfile().intakeProfile(),
    val stage: AiChatStage = AiChatStage.ONBOARDING,
    val quickReplies: List<AiChatQuickReply> = emptyList(),
    val llmHistory: List<LlmMessage> = emptyList(),
    val starterCards: List<AiChatCardOption> = emptyList(),
    val draftText: String = "",
    val selectedDraftOptions: List<AiChatCardOption> = emptyList(),
    val activeResponseCardGroup: AiChatCardGroup? = null,
    val activeCuratedTripRow: AiCuratedTripRow? = null,
    val anchorMessageId: String? = null
)

data class AiChatUiState(
    val items: List<AiChatItem> = emptyList(),
    val quickReplies: List<AiChatQuickReply> = emptyList(),
    val starterCards: List<AiChatCardOption> = emptyList(),
    val draftText: String = "",
    val selectedDraftOptions: List<AiChatCardOption> = emptyList(),
    val historyEntries: List<AiChatHistoryEntry> = emptyList(),
    val anchorMessageId: String? = null,
    val activeResponseCardGroupId: String? = null,
    val activeCuratedTripRowId: String? = null,
    val isLoading: Boolean = false
)
