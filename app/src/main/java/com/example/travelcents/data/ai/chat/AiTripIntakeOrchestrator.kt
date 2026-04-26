package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.ai.remote.LlmConfig
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
            maxTokens = 900,
            responseFormat = intakeResponseFormat()
        )
        if (rawResponse.isBlank()) return null

        val root = runCatching {
            JsonParser.parseString(rawResponse).asJsonObject
        }.getOrNull() ?: return null

        return AiTripIntakeTurnResult(
            ackKey = root.enumValueOrDefault("ack_key", AiTripIntakeAckKey.GOT_IT),
            profilePatch = root.getAsJsonObjectOrNull("profile_patch")?.toIntakeProfilePatch()
                ?: AiTripIntakeProfile(),
            resolvedFields = root.getStringList("resolved_fields"),
            missingFields = root.getStringList("missing_fields"),
            nextAction = root.enumValueOrDefault("next_action", AiTripIntakeNextAction.ASK_MORE),
            nextActionReason = root.getStringOrEmpty("next_action_reason"),
            questionKind = root.enumValueOrDefault("question_kind", AiTripIntakeQuestionKind.NONE),
            questionId = root.getStringOrEmpty("question_id"),
            questionTitle = root.getStringOrEmpty("question_title"),
            questionSubtitle = root.getStringOrEmpty("question_subtitle"),
            allowMultiple = root.getBooleanOrDefault("allow_multiple", false),
            allowOther = root.getBooleanOrDefault("allow_other", false),
            otherPromptHint = root.getStringOrEmpty("other_prompt_hint"),
            textPrompt = root.getStringOrEmpty("text_prompt"),
            options = root.getAsJsonArrayOrNull("options")
                ?.toAnswerOptions()
                .orEmpty()
        )
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
        if (rawResponse.isBlank()) return emptyList()

        val root = runCatching {
            JsonParser.parseString(rawResponse).asJsonObject
        }.getOrNull() ?: return emptyList()

        return root.getAsJsonArrayOrNull("recommendations")
            ?.toDestinationRecommendations()
            .orEmpty()
            .take(3)
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
                content =
                    "You are the TravelCents intake orchestrator. " +
                        "Read the user's latest turn and the current structured trip profile, then update the structured trip patch. " +
                        "Aggressively infer trip requirements stated directly or indirectly. " +
                        "Examples: 'me and my wife', 'my husband and I', 'my partner and I', or 'for my spouse and me' imply party_summary of two adults and a romantic trip type. " +
                        "A tropical or warm location implies a warm-weather destination style. " +
                        "Fill only fields supported by the schema. " +
                        "Treat the profile_patch as an additive patch, not a full replacement. " +
                        "For fields you cannot newly infer from the latest turn, leave strings empty, arrays empty, and enums as unknown. " +
                        "ack_key must be exactly one of: got_it, sounds_good, understood, perfect. " +
                        "Use question_kind='cards' only for simple discrete choices. " +
                        "Use question_kind='text' only for open-ended follow-ups. " +
                        "Use question_kind='none' when no follow-up question is needed on this turn. " +
                        "If question_kind='none', leave question_id, question_title, question_subtitle, other_prompt_hint, and text_prompt empty, set allow_multiple and allow_other to false, and return options as an empty array. " +
                        "If question_kind='cards', provide a stable snake_case question_id, a short question_title, 2 to 6 options, and keep text_prompt empty. " +
                        "If question_kind='text', provide a short text_prompt, keep options empty, and do not ask the same planning gap twice. " +
                        "Do not suggest specific destinations until you have gathered trip_type, at least 2 interests, and budget_level. Ask about missing fields first. " +
                        "If the user has enough direction for destination suggestions but no destination yet, set next_action to suggest_destinations. " +
                        "If the destination is already clear and the trip can move forward, set next_action to build_trip. " +
                        "If more information is still needed before either of those, set next_action to ask_more. " +
                        "Do not ask a follow-up question for information already present in the profile. " +
                        "Do not repeat the same planning gap if it appears in asked_question_ids. " +
                        "Return JSON only."
            ),
            LlmMessage(
                role = "system",
                content =
                    "Current intake profile JSON:\n${currentProfile.toPromptJson()}\n\n" +
                        "Fields still missing:\n${currentProfile.missingFields().joinToString()}"
            ),
            LlmMessage(
                role = "system",
                content =
                    "Return a JSON object with these top-level keys: " +
                        "ack_key, profile_patch, resolved_fields, missing_fields, next_action, next_action_reason, question_kind, question_id, question_title, question_subtitle, allow_multiple, allow_other, other_prompt_hint, text_prompt, options. " +
                        "Never repeat a follow-up question that targets information already present in the profile."
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
        return mapOf(
            "type" to "json_schema",
            "json_schema" to mapOf(
                "name" to "travelcents_intake_turn",
                "strict" to true,
                "schema" to buildTurnSchema()
            )
        )
    }

    private fun destinationSuggestionResponseFormat(): Map<String, Any> {
        return mapOf(
            "type" to "json_schema",
            "json_schema" to mapOf(
                "name" to "travelcents_destination_suggestions",
                "strict" to true,
                "schema" to buildDestinationSuggestionSchema()
            )
        )
    }

    private fun buildTurnSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "ack_key" to enumSchema("got_it", "sounds_good", "understood", "perfect"),
                "profile_patch" to buildProfilePatchSchema(),
                "resolved_fields" to stringArraySchema(),
                "missing_fields" to stringArraySchema(),
                "next_action" to enumSchema("ask_more", "suggest_destinations", "build_trip"),
                "next_action_reason" to stringSchema(),
                "question_kind" to enumSchema("none", "cards", "text"),
                "question_id" to stringSchema(),
                "question_title" to stringSchema(),
                "question_subtitle" to stringSchema(),
                "allow_multiple" to booleanSchema(),
                "allow_other" to booleanSchema(),
                "other_prompt_hint" to stringSchema(),
                "text_prompt" to stringSchema(),
                "options" to buildAnswerOptionsSchema()
            ),
            "required" to listOf(
                "ack_key",
                "profile_patch",
                "resolved_fields",
                "missing_fields",
                "next_action",
                "next_action_reason",
                "question_kind",
                "question_id",
                "question_title",
                "question_subtitle",
                "allow_multiple",
                "allow_other",
                "other_prompt_hint",
                "text_prompt",
                "options"
            )
        )
    }

    private fun buildProfilePatchSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "trip_type" to enumSchema("unknown", "solo", "romantic", "family", "friends", "business", "mixed"),
                "party_summary" to stringSchema(),
                "destination" to stringSchema(),
                "destination_style" to stringArraySchema(),
                "origin" to stringSchema(),
                "date_window" to stringSchema(),
                "budget_level" to enumSchema("unknown", "budget", "comfort", "luxury", "mixed"),
                "pace" to enumSchema("unknown", "relaxed", "balanced", "packed"),
                "interests" to stringArraySchema(),
                "must_haves" to stringArraySchema(),
                "avoid" to stringArraySchema(),
                "notes" to stringArraySchema()
            ),
            "required" to listOf(
                "trip_type",
                "party_summary",
                "destination",
                "destination_style",
                "origin",
                "date_window",
                "budget_level",
                "pace",
                "interests",
                "must_haves",
                "avoid",
                "notes"
            )
        )
    }

    private fun buildAnswerOptionsSchema(): Map<String, Any> {
        return mapOf(
            "type" to "array",
            "items" to mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "properties" to mapOf(
                    "id" to stringSchema(),
                    "label" to stringSchema(),
                    "message" to stringSchema()
                ),
                "required" to listOf("id", "label", "message")
            )
        )
    }

    private fun buildDestinationSuggestionSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "recommendations" to buildDestinationRecommendationSchema()
            ),
            "required" to listOf("recommendations")
        )
    }

    private fun buildDestinationRecommendationSchema(): Map<String, Any> {
        return mapOf(
            "type" to "array",
            "items" to mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "properties" to mapOf(
                    "id" to stringSchema(),
                    "destination" to stringSchema(),
                    "summary" to stringSchema(),
                    "reason" to stringSchema()
                ),
                "required" to listOf("id", "destination", "summary", "reason")
            )
        )
    }

    private fun stringSchema(): Map<String, Any> = mapOf("type" to "string")

    private fun booleanSchema(): Map<String, Any> = mapOf("type" to "boolean")

    private fun stringArraySchema(): Map<String, Any> {
        return mapOf(
            "type" to "array",
            "items" to stringSchema()
        )
    }

    private fun enumSchema(vararg values: String): Map<String, Any> {
        return mapOf(
            "type" to "string",
            "enum" to values.toList()
        )
    }
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
