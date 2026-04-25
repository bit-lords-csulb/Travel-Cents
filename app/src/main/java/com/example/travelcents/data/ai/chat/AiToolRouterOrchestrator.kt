package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.ai.remote.LlmConfig
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate

class AiToolRouterOrchestrator {
    suspend fun routeTools(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage> = emptyList(),
        planningObjective: String = ""
    ): AiToolRouterResult? {
        if (latestUserInput.isBlank()) return null

        val rawResponse = LlmClient.complete(
            messages = buildToolRouterMessages(
                currentProfile = currentProfile,
                latestUserInput = latestUserInput,
                history = history,
                planningObjective = planningObjective
            ),
            model = LlmConfig.intakeModel,
            temperature = 0.1,
            maxTokens = 700,
            responseFormat = toolRouterResponseFormat()
        )
        if (rawResponse.isBlank()) return null

        val root = runCatching {
            JsonParser.parseString(rawResponse).asJsonObject
        }.getOrNull() ?: return null

        val routedTools = root.getAsJsonArrayOrNull("tool_calls")
            ?.toToolCalls()
            .orEmpty()
        val viabilityWarning = root.getStringOrEmpty("viability_warning")

        return AiToolRouterResult(
            toolCalls = routedTools,
            viabilityWarning = viabilityWarning
        ).takeIf { result ->
            result.toolCalls.isNotEmpty() || result.viabilityWarning.isNotBlank()
        }
    }

    private fun buildToolRouterMessages(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage>,
        planningObjective: String
    ): List<LlmMessage> {
        return listOf(
            LlmMessage(
                role = "system",
                content =
                    "You are the TravelCents external tool router. " +
                        "Decide if the latest user turn needs a live external search. " +
                        "Return JSON only. " +
                        "Keep the response small: an optional viability_warning plus an array of tool_calls. " +
                        "Never ask follow-up questions. Never explain your reasoning. " +
                        "If no tool is needed yet, return an empty tool_calls array and an empty viability_warning string. " +
                        "Only emit tool calls when the destination or city is known and the latest turn is asking for concrete live options or results. " +
                        "Use at most one place-search tool call and at most one event-search tool call in the same response. " +
                        "Coverage note: Ticketmaster works best in the US, Canada, UK, Australia, and major European cities. " +
                        "Yelp works in the US, Canada, UK, France, Germany, Australia, New Zealand, and Mexico. " +
                        "SerpAPI hotels are global. " +
                        "If the user requests a live event such as a concert, game, or show in a destination that is unlikely to have Ticketmaster coverage, do not emit search_events. " +
                        "Instead set viability_warning to a short warning that names the limitation and suggests broader alternatives. " +
                        "For search_events use Ticketmaster-style classifications when possible, such as Music, Sports, Arts & Theatre, or Family. " +
                        "For search_hotels, only emit the tool call when check_in and check_out can be inferred as exact ISO dates in YYYY-MM-DD format. Otherwise do not emit search_hotels. " +
                        "For search_restaurants and search_activities, set cuisines or categories from the latest turn when useful, otherwise return empty arrays. " +
                        "If a field does not apply to a tool call, leave string fields empty and array fields empty."
            ),
            LlmMessage(
                role = "system",
                content =
                    "Current intake profile JSON:\n${currentProfile.toJson()}\n\n" +
                        "Current planning objective: ${planningObjective.ifBlank { "Not set yet" }}"
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    val recentHistory = history
                        .filter { message -> message.role == "user" || message.role == "assistant" }
                        .takeLast(6)
                    append("Recent chat turns:\n")
                    if (recentHistory.isEmpty()) {
                        append("None yet")
                    } else {
                        recentHistory.forEach { message ->
                            val role = if (message.role.equals("user", ignoreCase = true)) "User" else "Assistant"
                            appendLine("$role: ${message.content}")
                        }
                    }
                }
            ),
            LlmMessage(
                role = "user",
                content = latestUserInput
            )
        )
    }

    private fun toolRouterResponseFormat(): Map<String, Any> {
        return mapOf(
            "type" to "json_schema",
            "json_schema" to mapOf(
                "name" to "travelcents_tool_router",
                "strict" to true,
                "schema" to buildToolRouterSchema()
            )
        )
    }

    private fun buildToolRouterSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "tool_calls" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "additionalProperties" to false,
                        "properties" to mapOf(
                            "tool" to enumSchema(
                                "search_events",
                                "search_restaurants",
                                "search_activities",
                                "search_hotels"
                            ),
                            "city" to stringSchema(),
                            "classification" to stringSchema(),
                            "keyword" to stringSchema(),
                            "cuisines" to stringArraySchema(),
                            "categories" to stringArraySchema(),
                            "check_in" to stringSchema(),
                            "check_out" to stringSchema()
                        ),
                        "required" to listOf(
                            "tool",
                            "city",
                            "classification",
                            "keyword",
                            "cuisines",
                            "categories",
                            "check_in",
                            "check_out"
                        )
                    )
                ),
                "viability_warning" to stringSchema()
            ),
            "required" to listOf("tool_calls", "viability_warning")
        )
    }

    private fun JsonArray.toToolCalls(): List<AiToolCall> {
        return mapNotNull { element ->
            val root = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val city = root.getStringOrEmpty("city").trim()
            if (city.isBlank()) return@mapNotNull null

            when (root.getStringOrEmpty("tool")) {
                "search_events" -> {
                    AiToolCall.SearchEvents(
                        city = city,
                        classification = root.getStringOrEmpty("classification").trim().ifBlank { null },
                        keyword = root.getStringOrEmpty("keyword").trim().ifBlank { null }
                    )
                }

                "search_restaurants" -> {
                    AiToolCall.SearchRestaurants(
                        city = city,
                        cuisines = root.getStringList("cuisines")
                    )
                }

                "search_activities" -> {
                    AiToolCall.SearchActivities(
                        city = city,
                        categories = root.getStringList("categories")
                    )
                }

                "search_hotels" -> {
                    val checkIn = root.getStringOrEmpty("check_in").trim()
                    val checkOut = root.getStringOrEmpty("check_out").trim()
                    if (!isIsoDate(checkIn) || !isIsoDate(checkOut)) {
                        null
                    } else {
                        AiToolCall.SearchHotels(
                            city = city,
                            checkIn = checkIn,
                            checkOut = checkOut
                        )
                    }
                }

                else -> null
            }
        }
    }

    private fun isIsoDate(value: String): Boolean {
        return runCatching { LocalDate.parse(value) }.isSuccess
    }

    private fun enumSchema(vararg values: String): Map<String, Any> {
        return mapOf(
            "type" to "string",
            "enum" to values.toList()
        )
    }

    private fun stringSchema(): Map<String, Any> {
        return mapOf("type" to "string")
    }

    private fun stringArraySchema(): Map<String, Any> {
        return mapOf(
            "type" to "array",
            "items" to stringSchema()
        )
    }
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return takeIf(JsonElement::isJsonObject)?.asJsonObject
}

private fun JsonObject.getAsJsonArrayOrNull(key: String): JsonArray? {
    return get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray
}

private fun JsonObject.getStringOrEmpty(key: String): String {
    return get(key)?.takeIf(JsonElement::isJsonPrimitive)?.asString.orEmpty()
}

private fun JsonObject.getStringList(key: String): List<String> {
    return getAsJsonArrayOrNull(key)
        ?.mapNotNull { element ->
            element.takeIf(JsonElement::isJsonPrimitive)?.asString?.trim()?.takeIf(String::isNotBlank)
        }
        .orEmpty()
}
