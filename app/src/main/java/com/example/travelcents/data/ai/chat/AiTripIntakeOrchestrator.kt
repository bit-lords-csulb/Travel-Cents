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
    "date_from",
    "date_to",
    "date_window",
    "duration_days",
    "budget_level",
    "budget_total",
    "pace",
    "interests",
    "cuisine_preferences",
    "cuisine_sub_preferences",
    "activity_sub_categories",
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
  "assistant_message": "Got it.",
  "next_action": "ask_more",
  "topic_path": "destination_style",
  "question_id": "destination_type",
  "allow_multiple": true,
  "question_title": "What kind of destination?",
  "options": [
    { "id": "tropical", "label": "Tropical", "message": "Tropical." },
    { "id": "coastal_town", "label": "Coastal town", "message": "A coastal town." },
    { "id": "beach_resort", "label": "Beach resort", "message": "A beach resort." },
    { "id": "hidden_cove", "label": "Hidden cove", "message": "A quieter hidden cove." },
    { "id": "island", "label": "Island", "message": "An island destination." },
    { "id": "warm_city", "label": "Warm city", "message": "A warm city near the coast." }
  ]
}
""".trimIndent()

class AiTripIntakeOrchestrator {
    suspend fun analyzeTurn(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String,
        history: List<LlmMessage> = emptyList(),
        askedQuestionIds: List<String> = emptyList(),
        planningObjective: String = "",
        plannerContext: PlannerContext? = null,
        repairInstruction: String = "",
        forcedTopicPath: String = ""
    ): AiTripIntakeTurnResult? {
        if (latestUserInput.isBlank()) return null

        val rawResponse = LlmClient.complete(
            messages = buildIntakeMessages(
                currentProfile = currentProfile,
                latestUserInput = latestUserInput,
                history = history,
                askedQuestionIds = askedQuestionIds,
                planningObjective = planningObjective,
                plannerContext = plannerContext,
                repairInstruction = repairInstruction,
                forcedTopicPath = forcedTopicPath
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
        planningObjective: String,
        plannerContext: PlannerContext?,
        repairInstruction: String,
        forcedTopicPath: String
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
                    append("Aggressively extract date_window from time expressions: 'next month' → 'next month', 'next summer' → 'summer', 'in 3 months' → 'in 3 months', 'June' → 'June', 'over the holidays' → 'December'. ")
                    append("Aggressively extract duration_days from duration expressions: 'a week' → 7, '10 days' → 10, 'long weekend' → 3, 'two weeks' → 14, 'about 5 days' → 5. ")
                    append("Aggressively extract budget_level from any budget signal: 'cheap', 'on a budget', 'budget-friendly' → budget; 'comfortable', 'mid-range', 'not too expensive' → comfort; 'luxury', 'high-end', 'no expense spared' → luxury; 'mix of both', 'some splurges', 'mostly budget but a few nice things' → mixed. ")
                    append("profile_patch is an additive partial patch, not a full replacement. ")
                    append("Only include profile_patch keys you can newly infer from the latest turn. Omit keys you cannot newly infer. ")
                    append("Supported profile_patch keys: ${SUPPORTED_INTAKE_PROFILE_PATCH_FIELDS.joinToString(", ")}. ")
                    append("ack_key must be exactly one of: got_it, sounds_good, understood, perfect. ")
                    append("assistant_message must be a single short acknowledgment sentence. ")
                    append("next_action must be exactly one of: ask_more, suggest_destinations, build_trip. ")
                    append("Do not ask a follow-up question for information already present in the profile. ")
                    append("Do not repeat the same planning gap if it appears in asked_question_ids or already asked topic paths. ")
                    append("Do not suggest specific destinations until the app planner says the profile is ready or the question budget is exhausted. Ask about missing allowed fields first. ")
                    append("If the user has enough direction for destination suggestions but no destination yet, set next_action to suggest_destinations. ")
                    append("If destination is known AND date_window is known AND duration_days is known, set next_action to build_trip. ")
                    append("If destination is known but date_window or duration_days is still missing, set next_action to ask_more and ask about the missing timing field — do NOT set build_trip just because destination is confirmed. ")
                    append("Return exactly one of these outcomes: no follow-up, or one cards follow-up. ")
                    append("If more information is still needed before either of those, set next_action to ask_more. ")
                    append("When next_action is ask_more, cards are the only valid follow-up mode. Ask exactly one short cards-friendly follow-up with 4, 5, or 6 options. ")
                    append("Use 5 or 6 options for broad preference topics like destination_style, interests, food_preferences, or activity_preferences; use 4 options for focused decisions like budget, pace, duration, restaurant help, or activity help. ")
                    append("Use topic_path for semantic progression and duplicate detection. Use question_id only as the specific card instance id. ")
                    append("Use a stable snake_case question_id, a required snake/dot topic_path, a short question_title, and short option labels. Users can still type free text instead of tapping a card. ")
                    append("When next_action is suggest_destinations, include destination_recommendations with 2 or 3 items if no destination is locked. ")
                    append("When next_action is suggest_destinations or build_trip, set topic_path, question_id, and question_title to empty strings and set options to an empty array. ")
                    append("Return JSON only.")
                }
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    append("Current intake profile JSON:\n${currentProfile.toPromptJson()}\n\n")
                    append("Fields still missing:\n${currentProfile.missingFields().joinToString().ifBlank { "None" }}")
                    if (currentProfile.destination.isNotBlank() && currentProfile.dateWindow.isBlank()) {
                        append("\n\nPriority rule: destination is set but date_window is missing. The ONLY valid next_action is ask_more. Ask when the user would like to travel with topic_path='travel_timeline' and question_id='travel_timeline'. Do not set next_action=build_trip.")
                    } else if (currentProfile.destination.isNotBlank() && currentProfile.dateWindow.isNotBlank() && currentProfile.durationDays == null) {
                        val durationAlreadyAsked = plannerContext?.askedQuestionRecords?.any { record -> record.topicPath == "duration" } == true
                        if (!durationAlreadyAsked) {
                            append("\n\nPriority rule: destination and date_window are set but duration_days is missing. The ONLY valid next_action is ask_more. Ask how many days the user wants to travel with topic_path='duration' and question_id='trip_duration'. Do not set next_action=build_trip.")
                        } else {
                            val nextTopic = plannerContext?.nextBestAllowedTopicPath()
                            if (nextTopic != null) {
                                append("\n\nNote: duration was already asked. Ask the next profile question using topic_path='$nextTopic' before building the trip.")
                            } else {
                                append("\n\nPriority rule: all timing and profile topics are confirmed. Set next_action to build_trip now.")
                            }
                        }
                    } else if (currentProfile.destination.isNotBlank() && currentProfile.dateWindow.isNotBlank() && currentProfile.durationDays != null) {
                        val nextTopic = plannerContext?.nextBestAllowedTopicPath()
                        if (nextTopic != null) {
                            append("\n\nNote: destination, date_window, and duration_days are confirmed. Ask the next profile question using topic_path='$nextTopic' before building the trip.")
                        } else {
                            append("\n\nPriority rule: destination, date_window, duration_days, and all remaining profile topics are confirmed. Set next_action to build_trip now.")
                        }
                    }
                    plannerContext?.let { context ->
                        appendLine()
                        appendLine()
                        appendLine("App planner phase: ${context.phase}")
                        appendLine("Allowed topic roots: ${context.allowedTopicPathSummary()}")
                        appendLine("Already asked topic paths: ${context.askedTopicSummary()}")
                        appendLine("Pre-destination card question count: ${context.preDestinationQuestionCount}")
                        if (context.shouldForceVisualAction()) {
                            append("Planner gate: visual recommendations are due now; do not ask another question.")
                        }
                    }
                    appendLine()
                    appendLine("Child topic rule: after food_preferences is answered with a broad cuisine (e.g., Japanese), you may ask one child follow-up with topic_path='food_preferences.<cuisine>' (e.g., 'food_preferences.japanese'), parent_topic_path='food_preferences', parent_answer_id matching the selected answer id, and patch cuisine_sub_preferences with specific sub-types (e.g., ['sushi', 'ramen']). Do the same for activity_preferences using topic_path='activity_preferences.<type>' and patch activity_sub_categories. Do not go deeper than one child level. Only ask the child follow-up if it adds meaningful value — skip it if the Layer 1 answer is already specific enough (e.g., if the user said 'sushi' directly at Layer 1, no child follow-up is needed).")
                }
            ),
            LlmMessage(
                role = "system",
                content = buildString {
                    appendLine("Return exactly one JSON object with these top-level keys and no others:")
                    appendLine("ack_key, assistant_message, profile_patch, next_action, topic_path, question_id, parent_topic_path, parent_answer_id, question_title, allow_multiple, options, destination_recommendations")
                    appendLine()
                    appendLine("Produce exactly one of these shapes:")
                    appendLine("1. No follow-up: next_action is suggest_destinations or build_trip, topic_path is '', question_id is '', question_title is '', allow_multiple is false, options is [].")
                    appendLine("2. One cards follow-up: next_action is ask_more, topic_path is required, question_id is snake_case, question_title is concise, allow_multiple is true or false, options has 4, 5, or 6 objects.")
                    appendLine()
                    appendLine("allow_multiple: set to true for broad preference topics where multiple answers apply — food_preferences, interests, activity_preferences, destination_style, must_haves. Set to false for single-choice decisions — budget, pace, duration, travel_timeline, traveler_context, discovery_help.")
                    appendLine()
                    appendLine("Do not return resolved_fields, missing_fields, next_action_reason, question_kind, question_subtitle, allow_other, other_prompt_hint, text_prompt, place_recommendations, or decision.")
                    appendLine("If next_action='ask_more', topic_path and question_id must be present, parent fields may be empty, and options must contain 4, 5, or 6 objects with id, label, and message.")
                    appendLine("If next_action is not 'ask_more', set topic_path, question_id, and question_title to empty strings and set options to [].")
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
                    if (repairInstruction.isNotBlank()) {
                        appendLine()
                        appendLine(repairInstruction)
                    }
                    if (forcedTopicPath.isNotBlank()) {
                        appendLine()
                        appendLine("Forced next topic_path: $forcedTopicPath")
                        appendLine("Ask exactly one card question for that topic_path. Do not choose a different topic.")
                    }
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
    val topicPath = getStringOrEmpty("topic_path").toPlannerTopicPath()
    val questionId = getStringOrEmpty("question_id")
    val questionTitle = getStringOrEmpty("question_title")
    val options = getAsJsonArrayOrNull("options")
        ?.toAnswerOptions()
        .orEmpty()
    val hasCardFollowUp = nextAction == AiTripIntakeNextAction.ASK_MORE &&
        topicPath.isPlannerTopicPath() &&
        questionId.isNotBlank() &&
        questionTitle.isNotBlank()

    return AiTripIntakeTurnResult(
        ackKey = enumValueOrDefault("ack_key", AiTripIntakeAckKey.GOT_IT),
        assistantMessageText = getStringOrEmpty("assistant_message"),
        profilePatch = getAsJsonObjectOrNull("profile_patch")?.toIntakeProfilePatch()
            ?: AiTripIntakeProfile(),
        nextAction = nextAction,
        questionKind = if (hasCardFollowUp) AiTripIntakeQuestionKind.CARDS else AiTripIntakeQuestionKind.NONE,
        topicPath = topicPath.takeIf { hasCardFollowUp }.orEmpty(),
        questionId = questionId.takeIf { hasCardFollowUp }.orEmpty(),
        parentTopicPath = getStringOrEmpty("parent_topic_path").toPlannerTopicPath().takeIf { hasCardFollowUp }.orEmpty(),
        parentAnswerId = getStringOrEmpty("parent_answer_id").takeIf { hasCardFollowUp }.orEmpty(),
        questionTitle = questionTitle.takeIf { hasCardFollowUp }.orEmpty(),
        options = options.takeIf { hasCardFollowUp }.orEmpty(),
        destinationRecommendations = getAsJsonArrayOrNull("destination_recommendations")
            ?.toDestinationRecommendations()
            .orEmpty()
            .take(3)
    )
}

private fun JsonObject.toIntakeProfilePatch(): AiTripIntakeProfile {
    return AiTripIntakeProfile(
        tripType = enumValueOrDefault("trip_type", AiTripType.UNKNOWN),
        partySummary = getStringOrEmpty("party_summary"),
        destination = getStringOrEmpty("destination"),
        destinationStyle = getStringList("destination_style"),
        origin = getStringOrEmpty("origin"),
        dateFrom = getStringOrEmpty("date_from"),
        dateTo = getStringOrEmpty("date_to"),
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
