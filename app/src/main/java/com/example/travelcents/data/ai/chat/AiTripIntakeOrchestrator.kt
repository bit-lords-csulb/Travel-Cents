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
        latestUserInput: String
    ): AiTripIntakeTurnResult? {
        if (latestUserInput.isBlank()) return null

        val rawResponse = LlmClient.complete(
            messages = buildMessages(currentProfile, latestUserInput),
            model = LlmConfig.intakeModel,
            temperature = 0.2,
            maxTokens = 900,
            responseFormat = responseFormat()
        )
        if (rawResponse.isBlank()) return null

        val root = runCatching {
            JsonParser.parseString(rawResponse).asJsonObject
        }.getOrNull() ?: return null

        return AiTripIntakeTurnResult(
            profilePatch = root.getAsJsonObjectOrNull("profile_patch")?.toIntakeProfilePatch()
                ?: AiTripIntakeProfile(),
            resolvedFields = root.getStringList("resolved_fields"),
            missingFields = root.getStringList("missing_fields"),
            followUpQuestion = root.getAsJsonObjectOrNull("follow_up_question")?.toFollowUpQuestion(),
            decision = root.getAsJsonObjectOrNull("decision")?.toDecision() ?: AiTripIntakeDecision()
        )
    }

    private fun buildMessages(
        currentProfile: AiTripIntakeProfile,
        latestUserInput: String
    ): List<LlmMessage> {
        return listOf(
            LlmMessage(
                role = "system",
                content =
                    "You are the TravelCents intake orchestrator. " +
                        "Read the user's latest turn and the current structured trip profile. " +
                        "Infer any trip requirements stated directly or indirectly. " +
                        "Fill only fields supported by the schema. " +
                        "Treat the profile_patch as an additive patch, not a full replacement. " +
                        "For fields you cannot newly infer from the latest turn, leave strings empty, arrays empty, numbers null, and enums as unknown. " +
                        "If important information is still missing, return one concise follow-up question with up to 4 card options. " +
                        "If enough information is present to move forward, set decision.type to recommend_curated or build_from_scratch. " +
                        "Do not ask a follow-up question for information already present in the profile. " +
                        "Return JSON only."
            ),
            LlmMessage(
                role = "system",
                content =
                    "Current intake profile JSON:\n${currentProfile.toJson()}\n\n" +
                        "Fields still missing:\n${currentProfile.missingFields().joinToString()}"
            ),
            LlmMessage(
                role = "system",
                content =
                    "Return a JSON object with these top-level keys: " +
                        "profile_patch, resolved_fields, missing_fields, follow_up_question, decision. " +
                        "Use null for follow_up_question if no question is needed. " +
                        "Decision types: ask_more, recommend_curated, build_from_scratch. " +
                        "Never repeat a follow-up question that targets information already present in the profile."
            ),
            LlmMessage(
                role = "user",
                content = latestUserInput
            )
        )
    }

    private fun responseFormat(): Map<String, Any> {
        return mapOf(
            "type" to "json_schema",
            "json_schema" to mapOf(
                "name" to "travelcents_intake_turn",
                "strict" to true,
                "schema" to buildTurnSchema()
            )
        )
    }

    private fun buildTurnSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "profile_patch" to buildProfilePatchSchema(),
                "resolved_fields" to stringArraySchema(),
                "missing_fields" to stringArraySchema(),
                "follow_up_question" to buildFollowUpQuestionSchema(),
                "decision" to buildDecisionSchema()
            ),
            "required" to listOf(
                "profile_patch",
                "resolved_fields",
                "missing_fields",
                "follow_up_question",
                "decision"
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
                "duration_days" to nullableIntegerSchema(),
                "budget_level" to enumSchema("unknown", "budget", "comfort", "luxury", "mixed"),
                "budget_total" to nullableNumberSchema(),
                "pace" to enumSchema("unknown", "relaxed", "balanced", "packed"),
                "interests" to stringArraySchema(),
                "cuisine_preferences" to stringArraySchema(),
                "must_haves" to stringArraySchema(),
                "avoid" to stringArraySchema(),
                "notes" to stringArraySchema(),
                "confidence" to buildConfidenceSchema()
            ),
            "required" to listOf(
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
                "notes",
                "confidence"
            )
        )
    }

    private fun buildConfidenceSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "trip_type" to nullableNumberSchema(),
                "destination" to nullableNumberSchema(),
                "destination_style" to nullableNumberSchema(),
                "origin" to nullableNumberSchema(),
                "date_window" to nullableNumberSchema(),
                "budget" to nullableNumberSchema(),
                "pace" to nullableNumberSchema(),
                "interests" to nullableNumberSchema(),
                "cuisine_preferences" to nullableNumberSchema()
            ),
            "required" to listOf(
                "trip_type",
                "destination",
                "destination_style",
                "origin",
                "date_window",
                "budget",
                "pace",
                "interests",
                "cuisine_preferences"
            )
        )
    }

    private fun buildFollowUpQuestionSchema(): Map<String, Any> {
        return mapOf(
            "type" to listOf("object", "null"),
            "additionalProperties" to false,
            "properties" to mapOf(
                "id" to stringSchema(),
                "title" to stringSchema(),
                "subtitle" to stringSchema(),
                "allow_multiple" to mapOf("type" to "boolean"),
                "options" to mapOf(
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
            ),
            "required" to listOf("id", "title", "subtitle", "allow_multiple", "options")
        )
    }

    private fun buildDecisionSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "type" to enumSchema("ask_more", "recommend_curated", "build_from_scratch"),
                "reason" to stringSchema(),
                "confidence" to nullableNumberSchema()
            ),
            "required" to listOf("type", "reason", "confidence")
        )
    }

    private fun stringSchema(): Map<String, Any> = mapOf("type" to "string")

    private fun stringArraySchema(): Map<String, Any> {
        return mapOf(
            "type" to "array",
            "items" to stringSchema()
        )
    }

    private fun nullableIntegerSchema(): Map<String, Any> = mapOf("type" to listOf("integer", "null"))

    private fun nullableNumberSchema(): Map<String, Any> = mapOf("type" to listOf("number", "null"))

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
