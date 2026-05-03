package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.ai.remote.LlmConfig
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

private const val MAX_INTAKE_RESPONSE_TOKENS = 500

private val SUPPORTED_INTAKE_PROFILE_PATCH_FIELDS = listOf(
    "trip_type",
    "party_summary",
    "destination",
    "destination_style",
    "origin",
    "date_window",
    "duration_days",
    "budget_level",
    "budget_total",
    "pace",
    "interests",
    "cuisine_preferences",
    "must_haves",
    "avoid",
    "notes"
)

private val MINIMAL_DESTINATION_RESPONSE_EXAMPLE = """
{
  "recommendations": [
    {
      "id": "lisbon_portugal",
      "destination": "Lisbon, Portugal",
      "summary": "Coastal capital with hilltop views and Atlantic seafood.",
      "reason": "Romantic, walkable, and budget-friendly relative to Western Europe."
    },
    {
      "id": "santorini_greece",
      "destination": "Santorini, Greece",
      "summary": "Cliffside Cycladic island known for caldera sunsets.",
      "reason": "Beach + romance fit; works well at a relaxed pace."
    }
  ]
}
""".trimIndent()

private val MINIMAL_INTAKE_RESPONSE_EXAMPLE = """
{
  "ack_key": "got_it",
  "profile_patch": {
    "trip_type": "romantic",
    "destination_style": ["beach"]
  },
  "next_action": "ask_more",
  "question_id": "destination_type",
  "question_title": "What kind of destination?",
  "options": [
    { "id": "tropical", "label": "Tropical", "message": "Tropical." },
    { "id": "coastal_town", "label": "Coastal town", "message": "A coastal town." },
    { "id": "beach_resort", "label": "Beach resort", "message": "A beach resort." }
  ]
}
""".trimIndent()

class AiTripIntakeOrchestrator {
    suspend fun analyzeTurn(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage> = emptyList(),
        askedQuestionIds: List<String> = emptyList(),
        planningObjective: String = ""
    ): AiTripIntakeTurnResult? {
        if (latestUserInput.isBlank()) return null

        val rawResponse = LlmClient.complete(
            messages = buildIntakeMessages(
                currentProfile = currentProfile,
                latestUserInput = latestUserInput,
                history = history,
                askedQuestionIds = askedQuestionIds,
                planningObjective = planningObjective
            ),
            model = LlmConfig.intakeModel,
            temperature = 0.2,
            maxTokens = MAX_INTAKE_RESPONSE_TOKENS,
            responseFormat = intakeResponseFormat()
        )
        if (rawResponse.isBlank()) return null

        val root = runCatching {
            JsonParser.parseString(rawResponse).asJsonObject
        }.getOrNull() ?: return null

        return root.toMinimalTurnResult()
    }

    suspend fun suggestDestinations(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage> = emptyList()
    ): List<AiTripIntakeDestinationRecommendation> {
        if (latestUserInput.isBlank()) return emptyList()

        val rawResponse = LlmClient.complete(
            messages = buildDestinationMessages(
                currentProfile = currentProfile,
                latestUserInput = latestUserInput,
                history = history
            ),
            model = LlmConfig.intakeModel,
            temperature = 0.2,
            maxTokens = 600,
            responseFormat = destinationSuggestionResponseFormat()
        )
        if (rawResponse.isBlank()) {
            android.util.Log.w(
                "AiTripIntakeOrchestrator",
                "suggestDestinations: blank response from LLM"
            )
            return emptyList()
        }

        val root = runCatching {
            JsonParser.parseString(rawResponse).asJsonObject
        }.getOrNull() ?: run {
            android.util.Log.w(
                "AiTripIntakeOrchestrator",
                "suggestDestinations: response was not a JSON object. Raw: ${rawResponse.take(200)}"
            )
            return emptyList()
        }

        val recommendations = root.getAsJsonArrayOrNull("recommendations")
            ?.toDestinationRecommendations()
            .orEmpty()
            .take(3)

        if (recommendations.isEmpty()) {
            android.util.Log.w(
                "AiTripIntakeOrchestrator",
                "suggestDestinations: parsed 0 recommendations. Raw: ${rawResponse.take(300)}"
            )
        }
        return recommendations
    }

    private fun buildIntakeMessages(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage>,
        askedQuestionIds: List<String>,
        planningObjective: String
    ): List<LlmMessage> {
        return listOf(
            LlmMessage(
                role = "system",
                content = buildString {
                    append("You are the TravelCents intake orchestrator. ")
                    append("Read the user's latest turn and the current structured trip profile, then return a small JSON object for the next intake step. ")
                    append("Aggressively infer trip requirements stated directly or indirectly. ")
                    append("Examples: 'me and my wife', 'my husband and I', 'my partner and I', or 'for my spouse and me' imply party_summary='Two adults' and trip_type='romantic'. ")
                    append("A tropical or warm location implies a warm-weather destination style. ")
                    append("profile_patch is an additive partial patch, not a full replacement. ")
                    append("Only include profile_patch keys you can newly infer from the latest turn. Omit keys you cannot newly infer. ")
                    append("Supported profile_patch keys: ${SUPPORTED_INTAKE_PROFILE_PATCH_FIELDS.joinToString(", ")}. ")
                    append("ack_key must be exactly one of: got_it, sounds_good, understood, perfect. ")
                    append("next_action must be exactly one of: ask_more, suggest_destinations, build_trip. ")
                    append("Do not ask a follow-up question for information already present in the profile. ")
                    append("Do not repeat the same planning gap if it appears in asked_question_ids. ")
                    append("Do not suggest specific destinations until you have gathered trip_type, at least 2 interests, and budget_level. Ask about missing fields first. ")
                    append("If the user has enough direction for destination suggestions but no destination yet, set next_action to suggest_destinations. ")
                    append("If the destination is already clear and the trip can move forward, set next_action to build_trip. ")
                    append("Return exactly one of these outcomes: no follow-up, or one cards follow-up. ")
                    append("If more information is still needed before either of those, set next_action to ask_more. ")
                    append("When next_action is ask_more, cards are the only valid follow-up mode. Ask exactly one short cards-friendly follow-up with 2 to 6 options. ")
                    append("Use a stable snake_case question_id, a short question_title, and short option labels. Users can still type free text instead of tapping a card. ")
                    append("When next_action is suggest_destinations or build_trip, set question_id and question_title to empty strings and set options to an empty array. ")
                    append("Return JSON only.")
                }
            ),
            LlmMessage(
                role = "system",
                content =
                    "Current intake profile JSON:\n${currentProfile.toPromptJson()}\n\n" +
                        "Fields still missing:\n${currentProfile.missingFields().joinToString().ifBlank { "None" }}"
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    appendLine("Return exactly one JSON object with these top-level keys and no others:")
                    appendLine("ack_key, profile_patch, next_action, question_id, question_title, options")
                    appendLine()
                    appendLine("Produce exactly one of these shapes:")
                    appendLine("1. No follow-up: next_action is suggest_destinations or build_trip, question_id is '', question_title is '', options is [].")
                    appendLine("2. One cards follow-up: next_action is ask_more, question_id is snake_case, question_title is concise, options has 2 to 6 objects.")
                    appendLine()
                    appendLine("Do not return resolved_fields, missing_fields, next_action_reason, question_kind, question_subtitle, allow_multiple, allow_other, other_prompt_hint, text_prompt, destination_recommendations, place_recommendations, or decision.")
                    appendLine("If next_action='ask_more', question_id must be snake_case, question_title must be concise, and options must contain 2 to 6 objects with id, label, and message.")
                    appendLine("If next_action is not 'ask_more', set question_id and question_title to empty strings and set options to [].")
                    appendLine("profile_patch may be {} when the latest turn adds no new structured facts.")
                    appendLine()
                    appendLine("Example:")
                    append(MINIMAL_INTAKE_RESPONSE_EXAMPLE)
                }
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    appendLine("Current planning objective: ${planningObjective.ifBlank { "Not set yet" }}")
                    appendLine("Asked question ids: ${askedQuestionIds.joinToString().ifBlank { "None yet" }}")
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

    private fun buildDestinationMessages(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage>
    ): List<LlmMessage> {
        return listOf(
            LlmMessage(
                role = "system",
                content =
                    "You are the TravelCents destination recommender. " +
                        "Use the user's latest turn and structured trip profile to return 2 or 3 destination recommendations that tightly fit the request. " +
                        "Do not ask questions. Do not explain your process. " +
                        "Prefer varied but realistic fits. " +
                        "Keep each summary and reason concise. " +
                        "Return JSON only."
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    appendLine("Return exactly one JSON object with a top-level 'recommendations' array of 2 or 3 items.")
                    appendLine("Each item must have these fields and no others:")
                    appendLine("  id: stable snake_case slug derived from the destination, e.g. 'lisbon_portugal'")
                    appendLine("  destination: human-readable place name, e.g. 'Lisbon, Portugal'")
                    appendLine("  summary: 1 short sentence describing the place")
                    appendLine("  reason: 1 short sentence explaining why it fits the user's profile")
                    appendLine()
                    appendLine("Example:")
                    append(MINIMAL_DESTINATION_RESPONSE_EXAMPLE)
                }
            ),
            LlmMessage(
                role = "system",
                content = "Current intake profile JSON:\n${currentProfile.toPromptJson()}"
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    val recentHistory = history
                        .filter { message -> message.role == "user" || message.role == "assistant" }
                        .takeLast(4)
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

    private fun intakeResponseFormat(): Map<String, Any> {
        return mapOf("type" to "json_object")
    }

    private fun destinationSuggestionResponseFormat(): Map<String, Any> {
        return mapOf("type" to "json_object")
    }
}

private fun JsonObject.toMinimalTurnResult(): AiTripIntakeTurnResult {
    val nextAction = enumValueOrDefault("next_action", AiTripIntakeNextAction.ASK_MORE)
    val questionId = getStringOrEmpty("question_id")
    val questionTitle = getStringOrEmpty("question_title")
    val options = getAsJsonArrayOrNull("options")
        ?.toAnswerOptions()
        .orEmpty()
        .take(6)
    val hasCardFollowUp = nextAction == AiTripIntakeNextAction.ASK_MORE &&
        questionId.isNotBlank() &&
        questionTitle.isNotBlank() &&
        options.size in 2..6

    return AiTripIntakeTurnResult(
        ackKey = enumValueOrDefault("ack_key", AiTripIntakeAckKey.GOT_IT),
        profilePatch = getAsJsonObjectOrNull("profile_patch")?.toIntakeProfilePatch()
            ?: AiTripIntakeProfile(),
        nextAction = nextAction,
        questionKind = if (hasCardFollowUp) AiTripIntakeQuestionKind.CARDS else AiTripIntakeQuestionKind.NONE,
        questionId = questionId.takeIf { hasCardFollowUp }.orEmpty(),
        questionTitle = questionTitle.takeIf { hasCardFollowUp }.orEmpty(),
        options = options.takeIf { hasCardFollowUp }.orEmpty()
    )
}

private fun JsonObject.toIntakeProfilePatch(): AiTripIntakeProfile {
    return AiTripIntakeProfile(
        tripType = enumValueOrDefault("trip_type", AiTripType.UNKNOWN),
        partySummary = getStringOrEmpty("party_summary"),
        destination = getStringOrEmpty("destination"),
        destinationStyle = getStringList("destination_style"),
        origin = getStringOrEmpty("origin"),
        dateWindow = getStringOrEmpty("date_window"),
        durationDays = getIntOrNull("duration_days"),
        budgetLevel = enumValueOrDefault("budget_level", AiBudgetLevel.UNKNOWN),
        budgetTotal = getDoubleOrNull("budget_total"),
        pace = enumValueOrDefault("pace", AiTripPacePreference.UNKNOWN),
        interests = getStringList("interests"),
        cuisinePreferences = getStringList("cuisine_preferences"),
        mustHaves = getStringList("must_haves"),
        avoid = getStringList("avoid"),
        notes = getStringList("notes"),
        confidence = getDoubleMap("confidence")
    )
}

private fun JsonArray.toAnswerOptions(): List<AiTripIntakeAnswerOption> {
    return mapNotNull { element ->
        element.asJsonObjectOrNull()?.let { option ->
            val optionId = option.getStringOrEmpty("id")
            val label = option.getStringOrEmpty("label")
            val message = option.getStringOrEmpty("message")
            if (optionId.isBlank() || label.isBlank() || message.isBlank()) {
                null
            } else {
                AiTripIntakeAnswerOption(
                    id = optionId,
                    label = label,
                    message = message
                )
            }
        }
    }
}

private fun JsonObject.toFollowUpQuestion(): AiTripIntakeFollowUpQuestion? {
    val id = getStringOrEmpty("id")
    val title = getStringOrEmpty("title")
    if (id.isBlank() || title.isBlank()) return null

    val options = getAsJsonArrayOrNull("options")
        ?.mapNotNull { element ->
            element.asJsonObjectOrNull()?.let { option ->
                val optionId = option.getStringOrEmpty("id")
                val label = option.getStringOrEmpty("label")
                val message = option.getStringOrEmpty("message")
                if (optionId.isBlank() || label.isBlank() || message.isBlank()) {
                    null
                } else {
                    AiTripIntakeAnswerOption(
                        id = optionId,
                        label = label,
                        message = message
                    )
                }
            }
        }
        .orEmpty()

    return AiTripIntakeFollowUpQuestion(
        id = id,
        title = title,
        subtitle = getStringOrEmpty("subtitle"),
        allowMultiple = getBooleanOrDefault("allow_multiple", false),
        allowOther = getBooleanOrDefault("allow_other", false),
        otherPromptHint = getStringOrEmpty("other_prompt_hint"),
        options = options
    ).takeIf { question -> question.options.isNotEmpty() }
}

private fun JsonObject.toDecision(): AiTripIntakeDecision {
    return AiTripIntakeDecision(
        type = enumValueOrDefault("type", AiTripIntakeDecisionType.ASK_MORE),
        reason = getStringOrEmpty("reason"),
        confidence = getDoubleOrNull("confidence")
    )
}

private fun JsonArray.toDestinationRecommendations(): List<AiTripIntakeDestinationRecommendation> {
    return mapNotNull { element ->
        element.asJsonObjectOrNull()?.let { recommendation ->
            val id = recommendation.getStringOrEmpty("id")
            val destination = recommendation.getStringOrEmpty("destination")
            if (id.isBlank() || destination.isBlank()) {
                null
            } else {
                AiTripIntakeDestinationRecommendation(
                    id = id,
                    destination = destination,
                    summary = recommendation.getStringOrEmpty("summary"),
                    reason = recommendation.getStringOrEmpty("reason")
                )
            }
        }
    }
}

private fun JsonArray.toPlaceRecommendations(): List<AiTripIntakePlaceRecommendation> {
    return mapNotNull { element ->
        element.asJsonObjectOrNull()?.let { recommendation ->
            val id = recommendation.getStringOrEmpty("id")
            val name = recommendation.getStringOrEmpty("name")
            val category = recommendation.getStringOrEmpty("category")
            if (id.isBlank() || name.isBlank() || category.isBlank()) {
                null
            } else {
                AiTripIntakePlaceRecommendation(
                    id = id,
                    name = name,
                    category = category,
                    area = recommendation.getStringOrEmpty("area"),
                    summary = recommendation.getStringOrEmpty("summary"),
                    reason = recommendation.getStringOrEmpty("reason")
                )
            }
        }
    }
}

private fun JsonObject.getStringOrEmpty(key: String): String {
    return get(key)?.takeIf(JsonElement::isJsonPrimitive)?.asString.orEmpty().trim()
}

private fun JsonObject.getStringList(key: String): List<String> {
    return getAsJsonArrayOrNull(key)
        ?.mapNotNull { element ->
            element.takeIf(JsonElement::isJsonPrimitive)?.asString?.trim()?.takeIf(String::isNotBlank)
        }
        .orEmpty()
}

private fun JsonObject.getDoubleMap(key: String): Map<String, Double> {
    return getAsJsonObjectOrNull(key)
        ?.entrySet()
        ?.mapNotNull { (entryKey, value) ->
            value.takeIf(JsonElement::isJsonPrimitive)?.asDouble?.let { doubleValue ->
                entryKey to doubleValue
            }
        }
        ?.toMap()
        .orEmpty()
}

private fun JsonObject.getIntOrNull(key: String): Int? {
    return get(key)?.takeIf(JsonElement::isJsonPrimitive)?.runCatching { asInt }?.getOrNull()
}

private fun JsonObject.getDoubleOrNull(key: String): Double? {
    return get(key)?.takeIf(JsonElement::isJsonPrimitive)?.runCatching { asDouble }?.getOrNull()
}

private fun JsonObject.getBooleanOrDefault(key: String, default: Boolean): Boolean {
    return get(key)?.takeIf(JsonElement::isJsonPrimitive)?.runCatching { asBoolean }?.getOrDefault(default)
        ?: default
}

private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? {
    return get(key)?.asJsonObjectOrNull()
}

private fun JsonObject.getAsJsonArrayOrNull(key: String): JsonArray? {
    return get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return takeIf(JsonElement::isJsonObject)?.asJsonObject
}

private inline fun <reified T : Enum<T>> JsonObject.enumValueOrDefault(key: String, default: T): T {
    val raw = getStringOrEmpty(key)
    if (raw.isBlank()) return default

    return enumValues<T>().firstOrNull { value ->
        value.name.equals(raw.replace('-', '_'), ignoreCase = true)
    } ?: default
}
