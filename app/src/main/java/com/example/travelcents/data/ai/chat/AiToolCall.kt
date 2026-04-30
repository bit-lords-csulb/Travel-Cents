package com.example.travelcents.data.ai.chat

sealed class AiToolCall {
    data class SearchEvents(
        val city: String,
        val classification: String?,
        val keyword: String?
    ) : AiToolCall()

    data class SearchRestaurants(
        val city: String,
        val cuisines: List<String>
    ) : AiToolCall()

    data class SearchActivities(
        val city: String,
        val categories: List<String>
    ) : AiToolCall()

    data class SearchHotels(
        val city: String,
        val checkIn: String,
        val checkOut: String
    ) : AiToolCall()
}

data class AiToolRouterResult(
    val toolCalls: List<AiToolCall> = emptyList(),
    val viabilityWarning: String = ""
)
