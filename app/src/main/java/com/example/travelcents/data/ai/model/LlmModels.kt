package com.example.travelcents.data.ai.model

import com.google.gson.annotations.SerializedName

data class LlmRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("response_format") val responseFormat: Map<String, @JvmSuppressWildcards Any>? = null
)

data class LlmMessage(
    val role: String,
    val content: String
)

data class LlmResponse(
    val choices: List<LlmChoice>
)

data class LlmChoice(
    val message: LlmMessage
)


