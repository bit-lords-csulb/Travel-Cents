package com.example.travelcents.data.ai.chat

import java.util.UUID

sealed interface AiChatItem {
    val id: String

    data class TextMessage(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
        val sender: AiChatSender
    ) : AiChatItem

    data class SystemStatus(
        override val id: String = UUID.randomUUID().toString(),
        val title: String,
        val detail: String
    ) : AiChatItem

    data class ResponseCardGroup(
        override val id: String,
        val group: AiChatCardGroup
    ) : AiChatItem

    data class DestinationRecommendationRow(
        override val id: String,
        val row: AiDestinationRecommendationRow
    ) : AiChatItem

    data class CuratedTripRow(
        override val id: String,
        val row: AiCuratedTripRow
    ) : AiChatItem

    data class PlaceRecommendationRow(
        override val id: String,
        val row: AiPlaceRecommendationRow
    ) : AiChatItem

    data class SingleEventCard(
        override val id: String,
        val card: AiSingleEventSuggestion
    ) : AiChatItem
}

enum class AiChatSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class AiChatCardOption(
    val id: String,
    val label: String,
    val message: String,
    val groupId: String = "",
    val requiresText: Boolean = false
)

data class AiChatCardGroup(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val options: List<AiChatCardOption>,
    val allowMultiple: Boolean = true,
    val allowOther: Boolean = false,
    val otherPromptHint: String = ""
)

data class AiDestinationRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val destination: String,
    val summary: String,
    val matchReason: String,
    val seedId: String? = null,
    val imageUrl: String? = null
)

data class AiDestinationRecommendationRow(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val recommendations: List<AiDestinationRecommendation>
)

data class AiPlaceRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val area: String = "",
    val summary: String = "",
    val matchReason: String,
    val imageUrl: String? = null
)

enum class AiPlaceRecommendationRowType {
    GENERAL,
    RESTAURANTS,
    HOTELS,
    ACTIVITIES,
    NEIGHBORHOODS,
    DAY_TRIPS
}

data class AiPlaceRecommendationRow(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val recommendations: List<AiPlaceRecommendation>,
    val rowType: AiPlaceRecommendationRowType = AiPlaceRecommendationRowType.GENERAL,
    val actionLabels: List<String> = emptyList(),
    val actionsEnabled: Boolean = false
)
