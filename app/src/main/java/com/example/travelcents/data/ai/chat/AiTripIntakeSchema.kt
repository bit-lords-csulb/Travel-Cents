package com.example.travelcents.data.ai.chat

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Locale

private const val INTAKE_SCHEMA_VERSION = 1

enum class AiTripType {
    UNKNOWN,
    SOLO,
    ROMANTIC,
    FAMILY,
    FRIENDS,
    BUSINESS,
    MIXED
}

enum class AiBudgetLevel {
    UNKNOWN,
    BUDGET,
    COMFORT,
    LUXURY,
    MIXED
}

enum class AiTripPacePreference {
    UNKNOWN,
    RELAXED,
    BALANCED,
    PACKED
}

enum class AiTripIntakeDecisionType {
    ASK_MORE,
    RECOMMEND_CURATED,
    BUILD_FROM_SCRATCH
}

enum class AiTripIntakeAckKey {
    GOT_IT,
    SOUNDS_GOOD,
    UNDERSTOOD,
    PERFECT
}

enum class AiTripIntakeNextAction {
    ASK_MORE,
    SUGGEST_DESTINATIONS,
    BUILD_TRIP
}

enum class AiTripIntakeQuestionKind {
    NONE,
    CARDS,
    TEXT
}

data class AiTripIntakeProfile(
    @SerializedName("schema_version")
    val schemaVersion: Int = INTAKE_SCHEMA_VERSION,
    @SerializedName("trip_type")
    val tripType: AiTripType = AiTripType.UNKNOWN,
    @SerializedName("party_summary")
    val partySummary: String = "",
    @SerializedName("destination")
    val destination: String = "",
    @SerializedName("destination_style")
    val destinationStyle: List<String> = emptyList(),
    @SerializedName("origin")
    val origin: String = "",
    @SerializedName("date_window")
    val dateWindow: String = "",
    @SerializedName("duration_days")
    val durationDays: Int? = null,
    @SerializedName("budget_level")
    val budgetLevel: AiBudgetLevel = AiBudgetLevel.UNKNOWN,
    @SerializedName("budget_total")
    val budgetTotal: Double? = null,
    @SerializedName("pace")
    val pace: AiTripPacePreference = AiTripPacePreference.UNKNOWN,
    @SerializedName("interests")
    val interests: List<String> = emptyList(),
    @SerializedName("cuisine_preferences")
    val cuisinePreferences: List<String> = emptyList(),
    @SerializedName("must_haves")
    val mustHaves: List<String> = emptyList(),
    @SerializedName("avoid")
    val avoid: List<String> = emptyList(),
    @SerializedName("notes")
    val notes: List<String> = emptyList(),
    @SerializedName("confidence")
    val confidence: Map<String, Double> = emptyMap()
) {
    fun missingFields(): List<String> = buildList {
        if (tripType == AiTripType.UNKNOWN) add("trip_type")
        if (destination.isBlank() && destinationStyle.isEmpty()) add("destination_or_style")
        if (budgetLevel == AiBudgetLevel.UNKNOWN && budgetTotal == null) add("budget")
        if (pace == AiTripPacePreference.UNKNOWN) add("pace")
        if (interests.isEmpty()) add("interests")
    }

    fun toJson(gson: Gson = Gson()): String = gson.toJson(this)

    fun toPromptJson(gson: Gson = Gson()): String {
        return gson.toJson(
            linkedMapOf(
                "trip_type" to tripType.promptValue(),
                "party_summary" to partySummary,
                "destination" to destination,
                "destination_style" to destinationStyle.map { style -> style.lowercase(Locale.US) },
                "origin" to origin,
                "date_window" to dateWindow,
                "budget_level" to budgetLevel.promptValue(),
                "pace" to pace.promptValue(),
                "interests" to interests,
                "must_haves" to mustHaves,
                "avoid" to avoid,
                "notes" to notes
            )
        )
    }
}

data class AiTripIntakeAnswerOption(
    @SerializedName("id")
    val id: String,
    @SerializedName("label")
    val label: String,
    @SerializedName("message")
    val message: String
)

data class AiTripIntakeFollowUpQuestion(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("subtitle")
    val subtitle: String = "",
    @SerializedName("allow_multiple")
    val allowMultiple: Boolean = false,
    @SerializedName("allow_other")
    val allowOther: Boolean = false,
    @SerializedName("other_prompt_hint")
    val otherPromptHint: String = "",
    @SerializedName("options")
    val options: List<AiTripIntakeAnswerOption> = emptyList()
)

data class AiTripIntakeDecision(
    @SerializedName("type")
    val type: AiTripIntakeDecisionType = AiTripIntakeDecisionType.ASK_MORE,
    @SerializedName("reason")
    val reason: String = "",
    @SerializedName("confidence")
    val confidence: Double? = null
)

data class AiTripIntakeDestinationRecommendation(
    @SerializedName("id")
    val id: String,
    @SerializedName("destination")
    val destination: String,
    @SerializedName("summary")
    val summary: String = "",
    @SerializedName("reason")
    val reason: String = ""
)

data class AiTripIntakePlaceRecommendation(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("area")
    val area: String = "",
    @SerializedName("summary")
    val summary: String = "",
    @SerializedName("reason")
    val reason: String = ""
)

data class AiTripIntakeTurnResult(
    @SerializedName("ack_key")
    val ackKey: AiTripIntakeAckKey = AiTripIntakeAckKey.GOT_IT,
    @SerializedName("profile_patch")
    val profilePatch: AiTripIntakeProfile = AiTripIntakeProfile(),
    @SerializedName("resolved_fields")
    val resolvedFields: List<String> = emptyList(),
    @SerializedName("missing_fields")
    val missingFields: List<String> = emptyList(),
    @SerializedName("next_action")
    val nextAction: AiTripIntakeNextAction = AiTripIntakeNextAction.ASK_MORE,
    @SerializedName("next_action_reason")
    val nextActionReason: String = "",
    @SerializedName("question_kind")
    val questionKind: AiTripIntakeQuestionKind = AiTripIntakeQuestionKind.NONE,
    @SerializedName("question_id")
    val questionId: String = "",
    @SerializedName("question_title")
    val questionTitle: String = "",
    @SerializedName("question_subtitle")
    val questionSubtitle: String = "",
    @SerializedName("allow_multiple")
    val allowMultiple: Boolean = false,
    @SerializedName("allow_other")
    val allowOther: Boolean = false,
    @SerializedName("other_prompt_hint")
    val otherPromptHint: String = "",
    @SerializedName("text_prompt")
    val textPrompt: String = "",
    @SerializedName("options")
    val options: List<AiTripIntakeAnswerOption> = emptyList(),
    @SerializedName("destination_recommendations")
    val destinationRecommendations: List<AiTripIntakeDestinationRecommendation> = emptyList(),
    @SerializedName("place_recommendations")
    val placeRecommendations: List<AiTripIntakePlaceRecommendation> = emptyList(),
    @SerializedName("decision")
    val decision: AiTripIntakeDecision = when (nextAction) {
        AiTripIntakeNextAction.BUILD_TRIP -> AiTripIntakeDecision(
            type = AiTripIntakeDecisionType.BUILD_FROM_SCRATCH,
            reason = nextActionReason
        )

        AiTripIntakeNextAction.ASK_MORE,
        AiTripIntakeNextAction.SUGGEST_DESTINATIONS -> AiTripIntakeDecision(
            type = AiTripIntakeDecisionType.ASK_MORE,
            reason = nextActionReason
        )
    }
) {
    val assistantMessage: String
        get() = buildList {
            add(ackKey.displayText())
            when (questionKind) {
                AiTripIntakeQuestionKind.CARDS -> {
                    listOf(questionTitle, questionSubtitle)
                        .filter { value -> value.isNotBlank() }
                        .joinToString(" ")
                        .takeIf { prompt -> prompt.isNotBlank() }
                        ?.let(::add)
                }

                AiTripIntakeQuestionKind.TEXT -> {
                    textPrompt.ifBlank { questionTitle }
                        .takeIf { prompt -> prompt.isNotBlank() }
                        ?.let(::add)
                }

                AiTripIntakeQuestionKind.NONE -> Unit
            }
        }.joinToString(" ").trim()

    val planningObjective: String
        get() = when (nextAction) {
            AiTripIntakeNextAction.SUGGEST_DESTINATIONS -> "Review destination ideas"
            AiTripIntakeNextAction.BUILD_TRIP -> "Build the trip"
            AiTripIntakeNextAction.ASK_MORE -> {
                questionTitle.ifBlank { textPrompt }
                    .ifBlank { nextActionReason }
                    .ifBlank { "Refine the plan" }
            }
        }

    val followUpQuestion: AiTripIntakeFollowUpQuestion?
        get() {
            if (questionKind != AiTripIntakeQuestionKind.CARDS) return null

            return AiTripIntakeFollowUpQuestion(
                id = questionId,
                title = questionTitle,
                subtitle = questionSubtitle,
                allowMultiple = allowMultiple,
                allowOther = allowOther,
                otherPromptHint = otherPromptHint,
                options = options
            ).takeIf { question ->
                question.id.isNotBlank() &&
                    question.title.isNotBlank() &&
                    question.options.size in 2..6
            }
        }
}

fun AiTravelerProfile.toIntakeProfile(): AiTripIntakeProfile {
    return AiTripIntakeProfile(
        tripType = inferTripType(),
        partySummary = partySummary,
        destination = destination,
        destinationStyle = inferDestinationStyle(),
        origin = origin,
        dateWindow = dateWindow,
        budgetLevel = inferBudgetLevel(),
        budgetTotal = inferBudgetTotal(),
        pace = inferPacePreference(),
        interests = interests,
        cuisinePreferences = cuisinePreferences,
        avoid = dislikes,
        notes = notes,
        confidence = buildConfidenceMap()
    )
}

fun AiTripIntakeProfile.mergePatch(patch: AiTripIntakeProfile?): AiTripIntakeProfile {
    patch ?: return this

    return copy(
        tripType = patch.tripType.takeUnless { it == AiTripType.UNKNOWN } ?: tripType,
        partySummary = patch.partySummary.ifBlank { partySummary },
        destination = patch.destination.ifBlank { destination },
        destinationStyle = (destinationStyle + patch.destinationStyle).distinct(),
        origin = patch.origin.ifBlank { origin },
        dateWindow = patch.dateWindow.ifBlank { dateWindow },
        durationDays = patch.durationDays ?: durationDays,
        budgetLevel = patch.budgetLevel.takeUnless { it == AiBudgetLevel.UNKNOWN } ?: budgetLevel,
        budgetTotal = patch.budgetTotal ?: budgetTotal,
        pace = patch.pace.takeUnless { it == AiTripPacePreference.UNKNOWN } ?: pace,
        interests = (interests + patch.interests).distinct(),
        cuisinePreferences = (cuisinePreferences + patch.cuisinePreferences).distinct(),
        mustHaves = (mustHaves + patch.mustHaves).distinct(),
        avoid = (avoid + patch.avoid).distinct(),
        notes = (notes + patch.notes).distinct().takeLast(6),
        confidence = confidence + patch.confidence
    )
}

fun AiTravelerProfile.mergeIntakeProfile(intakeProfile: AiTripIntakeProfile): AiTravelerProfile {
    return copy(
        destination = intakeProfile.destination.ifBlank { destination },
        origin = intakeProfile.origin.ifBlank { origin },
        dateWindow = intakeProfile.dateWindow.ifBlank { dateWindow },
        budgetSummary = intakeProfile.toBudgetSummary().ifBlank { budgetSummary },
        partySummary = intakeProfile.toPartySummary().ifBlank { partySummary },
        travelPace = intakeProfile.toPaceSummary().ifBlank { travelPace },
        interests = (interests + intakeProfile.interests + intakeProfile.destinationStyle).distinct(),
        cuisinePreferences = (cuisinePreferences + intakeProfile.cuisinePreferences).distinct(),
        dislikes = (dislikes + intakeProfile.avoid).distinct(),
        notes = (notes + intakeProfile.mustHaves + intakeProfile.notes).distinct().takeLast(6)
    )
}

fun AiTripIntakeFollowUpQuestion.toCardGroup(): AiChatCardGroup? {
    if (id.isBlank() || title.isBlank() || options.isEmpty()) return null

    val normalizedId = sanitizeFollowUpId(id)
    val maxRegularOptions = if (allowOther) 5 else 6
    return AiChatCardGroup(
        id = normalizedId,
        title = title,
        subtitle = subtitle,
        allowMultiple = allowMultiple,
        allowOther = allowOther,
        otherPromptHint = otherPromptHint.ifBlank {
            "Type the answer that fits best."
        },
        options = options
            .filter { option -> option.id.isNotBlank() && option.label.isNotBlank() && option.message.isNotBlank() }
            .take(maxRegularOptions)
            .mapNotNull { option ->
                val normalizedLabel = shortenOptionLabel(option.label)
                if (normalizedLabel.isBlank()) {
                    null
                } else if (normalizedLabel.equals("Other", ignoreCase = true)) {
                    AiChatCardOption(
                        id = "${normalizedId}_other",
                        label = "Other",
                        message = otherPromptHint.ifBlank { "I want to type my own answer." },
                        groupId = normalizedId,
                        requiresText = true
                    )
                } else {
                    AiChatCardOption(
                        id = sanitizeFollowUpOptionId(option.id, normalizedId),
                        label = normalizedLabel,
                        message = option.message,
                        groupId = normalizedId
                    )
                }
            }
            .distinctBy { option -> option.id }
            .take(maxRegularOptions)
            .let { normalizedOptions ->
                val otherOption = normalizedOptions.firstOrNull { option -> option.requiresText }
                if (allowOther && otherOption == null) {
                    normalizedOptions + AiChatCardOption(
                        id = "${normalizedId}_other",
                        label = "Other",
                        message = otherPromptHint.ifBlank { "I want to type my own answer." },
                        groupId = normalizedId,
                        requiresText = true
                    )
                } else {
                    normalizedOptions
                }
            }
    ).takeIf { group ->
        group.options.size in 2..6
    }
}

fun AiTripIntakeTurnResult.toDestinationRecommendationRow(): AiDestinationRecommendationRow? {
    return AiRecommendationMapper.destinationRowFromIntake(destinationRecommendations)
}

fun AiTripIntakeTurnResult.withDestinationRecommendations(
    recommendations: List<AiTripIntakeDestinationRecommendation>
): AiTripIntakeTurnResult {
    return copy(destinationRecommendations = recommendations)
}

fun AiTripIntakeTurnResult.toPlaceRecommendationRow(): AiPlaceRecommendationRow? {
    return AiRecommendationMapper.placeRowFromIntake(placeRecommendations)
}

private fun sanitizeFollowUpId(rawId: String): String {
    return rawId
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "follow_up" }
}

private fun sanitizeFollowUpOptionId(rawId: String, groupId: String): String {
    val normalized = rawId
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    return if (normalized.isBlank()) "${groupId}_option" else normalized
}

private fun shortenOptionLabel(rawLabel: String): String {
    val trimmed = rawLabel.trim()
    if (trimmed.isBlank()) return ""
    return trimmed
        .split(Regex("\\s+"))
        .filter { token -> token.isNotBlank() }
        .take(2)
        .joinToString(" ")
}

private fun AiTravelerProfile.inferTripType(): AiTripType {
    val normalized = listOf(partySummary, notes.joinToString(" "))
        .joinToString(" ")
        .lowercase()

    return when {
        "family" in normalized || "kids" in normalized || "children" in normalized -> AiTripType.FAMILY
        "romantic" in normalized ||
            "couple" in normalized ||
            "for two" in normalized ||
            "wife" in normalized ||
            "husband" in normalized ||
            "partner" in normalized ||
            "spouse" in normalized ||
            "boyfriend" in normalized ||
            "girlfriend" in normalized ||
            "fiance" in normalized ||
            "fiancee" in normalized -> AiTripType.ROMANTIC
        "solo" in normalized -> AiTripType.SOLO
        "friends" in normalized || "group" in normalized -> AiTripType.FRIENDS
        "business" in normalized || "work trip" in normalized || "conference" in normalized -> AiTripType.BUSINESS
        else -> AiTripType.UNKNOWN
    }
}

private fun AiTravelerProfile.inferDestinationStyle(): List<String> {
    val styles = buildList {
        interests.forEach { interest ->
            when (interest.lowercase()) {
                "food" -> add("food_first")
                "culture", "history", "art" -> add("culture")
                "beach", "relaxation" -> add("beach")
                "nature", "hiking", "adventure" -> add("nature")
                "nightlife" -> add("nightlife")
                "shopping" -> add("shopping")
            }
        }

        val notesText = notes.joinToString(" ").lowercase()
        if ("walkable" in notesText) add("walkable_city")
        if ("tropical" in notesText || "warm" in notesText) add("warm_weather")
    }

    return styles.distinct()
}

private fun AiTravelerProfile.inferBudgetLevel(): AiBudgetLevel {
    val normalized = budgetSummary.lowercase()
    return when {
        "luxury" in normalized -> AiBudgetLevel.LUXURY
        "comfort" in normalized -> AiBudgetLevel.COMFORT
        "budget" in normalized || "affordable" in normalized -> AiBudgetLevel.BUDGET
        "balanced" in normalized || "splurge" in normalized -> AiBudgetLevel.MIXED
        else -> AiBudgetLevel.UNKNOWN
    }
}

private fun AiTravelerProfile.inferBudgetTotal(): Double? {
    val amount = Regex("\\$\\s?([\\d,]+(?:\\.\\d+)?)").find(budgetSummary)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(",", "")
    return amount?.toDoubleOrNull()
}

private fun AiTravelerProfile.inferPacePreference(): AiTripPacePreference {
    return when (travelPace.lowercase()) {
        "relaxed" -> AiTripPacePreference.RELAXED
        "balanced" -> AiTripPacePreference.BALANCED
        "packed" -> AiTripPacePreference.PACKED
        else -> AiTripPacePreference.UNKNOWN
    }
}

private fun AiTravelerProfile.buildConfidenceMap(): Map<String, Double> {
    return buildMap {
        put("trip_type", if (inferTripType() == AiTripType.UNKNOWN) 0.0 else 0.6)
        put("destination", if (destination.isBlank()) 0.0 else 0.9)
        put("destination_style", if (inferDestinationStyle().isEmpty()) 0.0 else 0.55)
        put("origin", if (origin.isBlank()) 0.0 else 0.85)
        put("date_window", if (dateWindow.isBlank()) 0.0 else 0.75)
        put("budget", if (budgetSummary.isBlank()) 0.0 else 0.8)
        put("pace", if (travelPace.isBlank()) 0.0 else 0.8)
        put("interests", if (interests.isEmpty()) 0.0 else 0.9)
        put("cuisine_preferences", if (cuisinePreferences.isEmpty()) 0.0 else 0.7)
    }
}

private fun AiTripIntakeProfile.toBudgetSummary(): String {
    val levelSummary = when (budgetLevel) {
        AiBudgetLevel.BUDGET -> "Budget-conscious"
        AiBudgetLevel.COMFORT -> "Comfort leaning"
        AiBudgetLevel.LUXURY -> "Luxury leaning"
        AiBudgetLevel.MIXED -> "Balanced with splurges"
        AiBudgetLevel.UNKNOWN -> ""
    }

    return when {
        budgetTotal != null && levelSummary.isNotBlank() -> "$levelSummary around \$$budgetTotal"
        budgetTotal != null -> "Around \$$budgetTotal"
        else -> levelSummary
    }
}

private fun AiTripIntakeProfile.toPartySummary(): String {
    return partySummary.ifBlank {
        when (tripType) {
            AiTripType.SOLO -> "Solo traveler"
            AiTripType.ROMANTIC -> "Two adults"
            AiTripType.FAMILY -> "Family trip"
            AiTripType.FRIENDS -> "Group getaway"
            AiTripType.BUSINESS -> "Business trip"
            AiTripType.MIXED,
            AiTripType.UNKNOWN -> ""
        }
    }
}

private fun AiTripIntakeProfile.toPaceSummary(): String {
    return when (pace) {
        AiTripPacePreference.RELAXED -> "Relaxed"
        AiTripPacePreference.BALANCED -> "Balanced"
        AiTripPacePreference.PACKED -> "Packed"
        AiTripPacePreference.UNKNOWN -> ""
    }
}

private fun AiTripIntakeAckKey.displayText(): String {
    return when (this) {
        AiTripIntakeAckKey.GOT_IT -> "Got it!"
        AiTripIntakeAckKey.SOUNDS_GOOD -> "Sounds good!"
        AiTripIntakeAckKey.UNDERSTOOD -> "Understood."
        AiTripIntakeAckKey.PERFECT -> "Perfect."
    }
}

private fun AiTripType.promptValue(): String = name.lowercase(Locale.US)

private fun AiBudgetLevel.promptValue(): String = name.lowercase(Locale.US)

private fun AiTripPacePreference.promptValue(): String = name.lowercase(Locale.US)
