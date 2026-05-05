package com.example.travelcents.data.ai.remote

import com.example.travelcents.BuildConfig

object LlmConfig {
    private const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/"
    private const val DEFAULT_CHAT_MODEL = "llama-3.3-70b-versatile"
    private const val DEFAULT_INTAKE_MODEL = "llama-3.1-8b-instant"

    val apiKey: String
        get() = BuildConfig.LLM_API_KEY

    val baseUrl: String
        get() = normalizeBaseUrl(
            BuildConfig.LLM_BASE_URL.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
        )

    val model: String
        get() = chatModel

    val chatModel: String
        get() = BuildConfig.LLM_MODEL.takeIf { it.isNotBlank() } ?: DEFAULT_CHAT_MODEL

    val intakeModel: String
        get() = BuildConfig.LLM_INTAKE_MODEL.takeIf { it.isNotBlank() } ?: DEFAULT_INTAKE_MODEL

    private fun normalizeBaseUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}


