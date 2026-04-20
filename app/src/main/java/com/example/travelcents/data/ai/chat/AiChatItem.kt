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

    data class CuratedTripRow(
        override val id: String,
        val row: AiCuratedTripRow
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
    val groupId: String = ""
)

data class AiChatCardGroup(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val options: List<AiChatCardOption>,
    val allowMultiple: Boolean = true
)
