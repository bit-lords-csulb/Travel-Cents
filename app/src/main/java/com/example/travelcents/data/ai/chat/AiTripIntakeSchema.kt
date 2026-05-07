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
    CARDS
}

enum class PlannerPhase {
    PRE_DESTINATION,
    VISUAL_RECOMMENDATIONS,
    DESTINATION_LOCKED,
    TRIP_BUILD
}

enum class PlannerQuestionSource {
    LLM,
    APP_FALLBACK,
    DISCOVERY,
    STARTER_GRID
}

data class AskedQuestionRecord(
    @SerializedName("topic_path")
    val topicPath: String = "",
    @SerializedName("question_id")
    val questionId: String = "",
    @SerializedName("source")
    val source: PlannerQuestionSource = PlannerQuestionSource.LLM,
    @SerializedName("phase")
    val phase: PlannerPhase = PlannerPhase.PRE_DESTINATION,
    @SerializedName("parent_topic_path")
    val parentTopicPath: String = "",
    @SerializedName("parent_answer_id")
    val parentAnswerId: String = "",
    @SerializedName("answer_ids")
    val answerIds: List<String> = emptyList(),
    @SerializedName("answered")
    val answered: Boolean = false
)

data class PlannerContext(
    val phase: PlannerPhase = PlannerPhase.PRE_DESTINATION,
    val askedQuestionRecords: List<AskedQuestionRecord> = emptyList(),
    val currentProfile: AiTripIntakeProfile = AiTripIntakeProfile(),
    val preDestinationQuestionCount: Int = 0,
    val visualRecommendationsDue: Boolean = false
)

data class PlannerValidationResult(
    val accepted: Boolean,
    val rejectionReason: String = "",
    val cardGroup: AiChatCardGroup? = null,
    val forceVisualAction: Boolean = false
)

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
    @SerializedName("date_from")
    val dateFrom: String = "",
    @SerializedName("date_to")
    val dateTo: String = "",
    @SerializedName("date_window")
    val dateWindow: String = "",
    @SerializedName("duration_days")
    val durationDays: Int? = null,
    @SerializedName("duration_flexible")
    val durationFlexible: Boolean = false,
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
    @SerializedName("cuisine_sub_preferences")
    val cuisineSubPreferences: List<String> = emptyList(),
    @SerializedName("activity_sub_categories")
    val activitySubCategories: List<String> = emptyList(),
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
                "date_from" to dateFrom,
                "date_to" to dateTo,
                "date_window" to dateWindow,
                "duration_days" to durationDays,
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
    @SerializedName("topic_path")
    val topicPath: String = "",
    @SerializedName("question_id")
    val questionId: String = id,
    @SerializedName("source")
    val source: PlannerQuestionSource = PlannerQuestionSource.LLM,
    @SerializedName("parent_topic_path")
    val parentTopicPath: String = "",
    @SerializedName("parent_answer_id")
    val parentAnswerId: String = "",
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
    @SerializedName("assistant_message")
    val assistantMessageText: String = "",
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
    @SerializedName("topic_path")
    val topicPath: String = "",
    @SerializedName("question_id")
    val questionId: String = "",
    @SerializedName("question_source")
    val questionSource: PlannerQuestionSource = PlannerQuestionSource.LLM,
    @SerializedName("parent_topic_path")
    val parentTopicPath: String = "",
    @SerializedName("parent_answer_id")
    val parentAnswerId: String = "",
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
    private val hasValidCardPayload: Boolean
        get() = nextAction == AiTripIntakeNextAction.ASK_MORE &&
            topicPath.isPlannerTopicPath() &&
            questionId.isNotBlank() &&
            questionTitle.isNotBlank() &&
            options.hasPlannerAnswerOptionCount()

    private val effectiveQuestionKind: AiTripIntakeQuestionKind
        get() = when {
            nextAction != AiTripIntakeNextAction.ASK_MORE -> AiTripIntakeQuestionKind.NONE
            questionKind == AiTripIntakeQuestionKind.CARDS || hasValidCardPayload -> AiTripIntakeQuestionKind.CARDS
            else -> AiTripIntakeQuestionKind.NONE
        }

    val assistantMessage: String
        get() = assistantMessageText.trim().ifBlank { ackKey.displayText() }

    val planningObjective: String
        get() = when (nextAction) {
            AiTripIntakeNextAction.SUGGEST_DESTINATIONS -> "Review destination ideas"
            AiTripIntakeNextAction.BUILD_TRIP -> "Build the trip"
            AiTripIntakeNextAction.ASK_MORE -> questionTitle.ifBlank { "Refine the plan" }
        }

    val followUpQuestion: AiTripIntakeFollowUpQuestion?
        get() {
            if (effectiveQuestionKind != AiTripIntakeQuestionKind.CARDS) return null

            return AiTripIntakeFollowUpQuestion(
                id = questionId,
                topicPath = topicPath,
                questionId = questionId,
                source = questionSource,
                parentTopicPath = parentTopicPath,
                parentAnswerId = parentAnswerId,
                title = questionTitle,
                subtitle = questionSubtitle,
                allowMultiple = allowMultiple,
                allowOther = allowOther,
                otherPromptHint = otherPromptHint,
                options = options
            ).takeIf { question ->
                question.id.isNotBlank() &&
                    question.title.isNotBlank() &&
                    question.topicPath.isPlannerTopicPath() &&
                    question.options.hasPlannerAnswerOptionCount()
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
        dateFrom = "",
        dateTo = "",
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
        dateFrom = patch.dateFrom.ifBlank { dateFrom },
        dateTo = patch.dateTo.ifBlank { dateTo },
        dateWindow = patch.dateWindow.ifBlank { dateWindow },
        durationDays = patch.durationDays ?: durationDays,
        durationFlexible = patch.durationFlexible || durationFlexible,
        budgetLevel = patch.budgetLevel.takeUnless { it == AiBudgetLevel.UNKNOWN } ?: budgetLevel,
        budgetTotal = patch.budgetTotal ?: budgetTotal,
        pace = patch.pace.takeUnless { it == AiTripPacePreference.UNKNOWN } ?: pace,
        interests = (interests + patch.interests).distinct(),
        cuisinePreferences = (cuisinePreferences + patch.cuisinePreferences).distinct(),
        cuisineSubPreferences = (cuisineSubPreferences + patch.cuisineSubPreferences).distinct(),
        activitySubCategories = (activitySubCategories + patch.activitySubCategories).distinct(),
        mustHaves = (mustHaves + patch.mustHaves).distinct(),
        avoid = (avoid + patch.avoid).distinct(),
        notes = (notes + patch.notes).distinct().takeLast(6),
        confidence = confidence + patch.confidence
    )
}

fun cardIdToIntakeProfilePatch(selectedCardIds: List<String>): AiTripIntakeProfile {
    var patch = AiTripIntakeProfile()
    for (id in selectedCardIds) {
        patch = when (id) {
            "budget_value"   -> patch.copy(budgetLevel = AiBudgetLevel.BUDGET)
            "budget_comfort" -> patch.copy(budgetLevel = AiBudgetLevel.COMFORT)
            "budget_splurge", "budget_luxury" -> patch.copy(budgetLevel = AiBudgetLevel.LUXURY)
            "pace_relaxed"   -> patch.copy(pace = AiTripPacePreference.RELAXED)
            "pace_balanced"  -> patch.copy(pace = AiTripPacePreference.BALANCED)
            "pace_packed"    -> patch.copy(pace = AiTripPacePreference.PACKED)
            "duration_weekend"   -> patch.copy(durationDays = 2)
            "duration_four_days" -> patch.copy(durationDays = 4)
            "duration_week"      -> patch.copy(durationDays = 7)
            "duration_flexible"  -> patch.copy(durationFlexible = true)
            "travel_timeline_next_month" -> patch.copy(dateWindow = "next month")
            "travel_timeline_3_months"   -> patch.copy(dateWindow = "in 3 months")
            "travel_timeline_6_months"   -> patch.copy(dateWindow = "in 6 months")
            "traveler_context_solo"    -> patch.copy(tripType = AiTripType.SOLO)
            "traveler_context_couple"  -> patch.copy(tripType = AiTripType.ROMANTIC)
            "traveler_context_family"  -> patch.copy(tripType = AiTripType.FAMILY)
            "traveler_context_friends" -> patch.copy(tripType = AiTripType.FRIENDS)
            "destination_style_beach"    -> patch.copy(destinationStyle = patch.destinationStyle + "beach")
            "destination_style_city"     -> patch.copy(destinationStyle = patch.destinationStyle + "city")
            "destination_style_nature"   -> patch.copy(destinationStyle = patch.destinationStyle + "nature")
            "destination_style_culture"  -> patch.copy(destinationStyle = patch.destinationStyle + "culture")
            "destination_style_food"     -> patch.copy(destinationStyle = patch.destinationStyle + "food")
            "destination_style_relaxing" -> patch.copy(destinationStyle = patch.destinationStyle + "relaxing")
            "interests_food"       -> patch.copy(interests = patch.interests + "food")
            "interests_history"    -> patch.copy(interests = patch.interests + "history")
            "interests_outdoors"   -> patch.copy(interests = patch.interests + "outdoors")
            "interests_shopping"   -> patch.copy(interests = patch.interests + "shopping")
            "interests_nightlife"  -> patch.copy(interests = patch.interests + "nightlife")
            "interests_relaxation" -> patch.copy(interests = patch.interests + "relaxation")
            "food_preferences_local"       -> patch.copy(cuisinePreferences = patch.cuisinePreferences + "local cuisine")
            "food_preferences_street_food" -> patch.copy(cuisinePreferences = patch.cuisinePreferences + "street food")
            "food_preferences_fine_dining" -> patch.copy(cuisinePreferences = patch.cuisinePreferences + "fine dining")
            "food_preferences_cafes"       -> patch.copy(cuisinePreferences = patch.cuisinePreferences + "cafes")
            "food_preferences_seafood"     -> patch.copy(cuisinePreferences = patch.cuisinePreferences + "seafood")
            "food_preferences_vegetarian"  -> patch.copy(cuisinePreferences = patch.cuisinePreferences + "vegetarian")
            "activity_preferences_landmarks" -> patch.copy(activitySubCategories = patch.activitySubCategories + "landmarks")
            "activity_preferences_museums"   -> patch.copy(activitySubCategories = patch.activitySubCategories + "museums")
            "activity_preferences_outdoors"  -> patch.copy(activitySubCategories = patch.activitySubCategories + "outdoor activities")
            "activity_preferences_tours"     -> patch.copy(activitySubCategories = patch.activitySubCategories + "tours")
            "activity_preferences_shows"     -> patch.copy(activitySubCategories = patch.activitySubCategories + "shows")
            "must_haves_walkable"     -> patch.copy(mustHaves = patch.mustHaves + "walkable")
            "must_haves_easy_flights" -> patch.copy(mustHaves = patch.mustHaves + "easy flights")
            "must_haves_great_food"   -> patch.copy(mustHaves = patch.mustHaves + "great food")
            "must_haves_scenic"       -> patch.copy(mustHaves = patch.mustHaves + "scenic views")
            else -> patch
        }
    }
    return patch
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
    if (id.isBlank() || title.isBlank() || !topicPath.isPlannerTopicPath() || !options.hasPlannerAnswerOptionCount()) {
        return null
    }

    val normalizedId = sanitizeFollowUpId(questionId.ifBlank { id })
    val normalizedTopicPath = topicPath.toPlannerTopicPath()
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
        topicPath = normalizedTopicPath,
        questionId = normalizedId,
        source = source,
        parentTopicPath = parentTopicPath.toPlannerTopicPath(),
        parentAnswerId = parentAnswerId
            .takeIf(String::isNotBlank)
            ?.let { rawParentAnswerId -> sanitizeFollowUpOptionId(rawParentAnswerId, normalizedId) }
            .orEmpty(),
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
        group.options.hasPlannerCardOptionCount()
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

fun List<AiTripIntakeAnswerOption>.hasPlannerAnswerOptionCount(): Boolean {
    return size in 4..6
}

fun List<AiChatCardOption>.hasPlannerCardOptionCount(): Boolean {
    return size in 4..6
}

fun String.toPlannerTopicPath(): String {
    val normalizedSegments = trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9.]+"), "_")
        .split('.')
        .map { segment -> segment.trim('_') }
        .filter(String::isNotBlank)
    return normalizedSegments.joinToString(".")
}

fun String.isPlannerTopicPath(): Boolean {
    val normalized = toPlannerTopicPath()
    if (normalized.isBlank()) return false
    return normalized.split('.').all { segment ->
        segment.matches(Regex("[a-z][a-z0-9_]*"))
    }
}

fun AiTripIntakeProfile.isReadyForDestinationRecommendations(): Boolean {
    val hasTravelerContext = tripType != AiTripType.UNKNOWN || partySummary.isNotBlank()
    val hasStyleDirection = destinationStyle.isNotEmpty() || interests.size >= 3
    val hasBudget = budgetLevel != AiBudgetLevel.UNKNOWN || budgetTotal != null
    val hasPace = pace != AiTripPacePreference.UNKNOWN
    return hasTravelerContext && hasStyleDirection && hasBudget && hasPace
}

fun PlannerContext.shouldForceVisualAction(): Boolean {
    if (visualRecommendationsDue) return true
    if (phase == PlannerPhase.PRE_DESTINATION && preDestinationQuestionCount >= PRE_DESTINATION_QUESTION_LIMIT) {
        return true
    }
    return phase == PlannerPhase.PRE_DESTINATION &&
        currentProfile.destination.isBlank() &&
        currentProfile.isReadyForDestinationRecommendations()
}

fun PlannerContext.allowedTopicPathSummary(): String {
    return allowedTopicRootsForPhase(phase).joinToString()
}

fun PlannerContext.askedTopicSummary(): String {
    return askedQuestionRecords
        .map { record -> record.topicPath }
        .filter(String::isNotBlank)
        .distinct()
        .joinToString()
        .ifBlank { "None yet" }
}

fun PlannerContext.repairPrompt(rejectionReason: String): String {
    return buildString {
        appendLine("Repair the previous planner response.")
        appendLine("Rejection reason: $rejectionReason")
        appendLine("Current planner phase: $phase")
        appendLine("Allowed topic roots: ${allowedTopicPathSummary()}")
        appendLine("Already asked topic paths: ${askedTopicSummary()}")
        appendLine("If you ask a card question, topic_path is required and options must contain between 4 and 6 objects (4, 5, or 6).")
        appendLine("Use a child topic only when parent_topic_path and parent_answer_id justify a narrower drill-down.")
        if (shouldForceVisualAction()) {
            append("The app says visual recommendations are due now, so do not ask another question.")
        }
    }
}

fun PlannerContext.nextBestAllowedTopicPath(): String? {
    if (shouldForceVisualAction()) return null
    val askedTopics = askedQuestionRecords.map { record -> record.topicPath }.toSet()

    fun firstUnasked(candidates: List<String>): String? {
        return candidates.firstOrNull { candidate -> candidate !in askedTopics }
    }

    return when (phase) {
        PlannerPhase.PRE_DESTINATION -> {
            val profile = currentProfile
            firstUnasked(
                buildList {
                    if (profile.tripType == AiTripType.UNKNOWN && profile.partySummary.isBlank()) add("traveler_context")
                    if (profile.destinationStyle.isEmpty() && profile.interests.size < 2) add("destination_style")
                    if (profile.budgetLevel == AiBudgetLevel.UNKNOWN && profile.budgetTotal == null) add("budget")
                    if (profile.interests.size < 2) add("interests")
                    if (profile.pace == AiTripPacePreference.UNKNOWN) add("pace")
                    if (profile.cuisinePreferences.isEmpty()) add("food_preferences")
                    add("activity_preferences")
                    add("must_haves")
                }
            )
        }

        PlannerPhase.DESTINATION_LOCKED -> {
            val profile = currentProfile
            firstUnasked(
                buildList {
                    if (profile.dateWindow.isBlank()) add("travel_timeline")
                    if (profile.durationDays == null && !profile.durationFlexible && "duration" !in askedTopics) add("duration")
                    if (profile.dateWindow.isNotBlank() || profile.durationDays != null || profile.durationFlexible || "duration" in askedTopics) {
                        add("discovery_help")
                    }
                    if (profile.budgetLevel == AiBudgetLevel.UNKNOWN && profile.budgetTotal == null) add("budget")
                    if (profile.pace == AiTripPacePreference.UNKNOWN) add("pace")
                    if (profile.cuisinePreferences.isEmpty()) add("food_preferences")
                    add("activity_preferences")
                }
            )
        }

        PlannerPhase.VISUAL_RECOMMENDATIONS,
        PlannerPhase.TRIP_BUILD -> null
    }
}

fun AiTripIntakeTurnResult.validatePlannerTurn(
    context: PlannerContext
): PlannerValidationResult {
    if (nextAction != AiTripIntakeNextAction.ASK_MORE) {
        return PlannerValidationResult(accepted = true, forceVisualAction = nextAction == AiTripIntakeNextAction.SUGGEST_DESTINATIONS)
    }

    if (context.shouldForceVisualAction()) {
        return PlannerValidationResult(
            accepted = false,
            rejectionReason = "visual recommendations are due; no more card questions are allowed",
            forceVisualAction = true
        )
    }

    val question = followUpQuestion ?: return PlannerValidationResult(
        accepted = false,
        rejectionReason = "next_action=ask_more requires topic_path, question_id, title, and 4–6 valid options"
    )

    val group = question.toCardGroup() ?: return PlannerValidationResult(
        accepted = false,
        rejectionReason = "card question is malformed or does not have 4–6 usable options"
    )

    val topicPath = group.topicPath.toPlannerTopicPath()
    val duplicate = context.askedQuestionRecords.any { record ->
        record.topicPath == topicPath
    }
    val childAllowed = isAllowedChildRefinement(group, context.askedQuestionRecords)

    if (duplicate && !childAllowed) {
        return PlannerValidationResult(
            accepted = false,
            rejectionReason = "topic_path '$topicPath' was already asked"
        )
    }

    if (!topicPathAllowedForPhase(topicPath, context.phase)) {
        return PlannerValidationResult(
            accepted = false,
            rejectionReason = "topic_path '$topicPath' is outside phase $context.phase"
        )
    }

    if (!childAllowed && topicPathTargetsKnownProfileField(topicPath, context.currentProfile)) {
        return PlannerValidationResult(
            accepted = false,
            rejectionReason = "topic_path '$topicPath' targets profile information already known"
        )
    }

    if (group.parentTopicPath.isNotBlank() && !childAllowed) {
        return PlannerValidationResult(
            accepted = false,
            rejectionReason = "child topic '$topicPath' is missing a matching answered parent topic and answer"
        )
    }

    return PlannerValidationResult(
        accepted = true,
        cardGroup = group
    )
}

fun AiChatCardGroup.toAskedQuestionRecord(
    phase: PlannerPhase,
    answerIds: List<String> = emptyList(),
    answered: Boolean = false
): AskedQuestionRecord {
    return AskedQuestionRecord(
        topicPath = topicPath.toPlannerTopicPath(),
        questionId = questionId.ifBlank { id },
        source = source,
        phase = phase,
        parentTopicPath = parentTopicPath.toPlannerTopicPath(),
        parentAnswerId = parentAnswerId,
        answerIds = answerIds.map { answerId -> sanitizeFollowUpOptionId(answerId, id) }.distinct(),
        answered = answered
    )
}

fun List<AskedQuestionRecord>.recordAskedPlannerQuestion(
    group: AiChatCardGroup?,
    phase: PlannerPhase
): List<AskedQuestionRecord> {
    group ?: return this
    if (group.topicPath.isBlank()) return this
    val record = group.toAskedQuestionRecord(phase = phase)
    return filterNot { existing ->
        existing.topicPath == record.topicPath && existing.questionId == record.questionId
    }.plus(record)
}

fun List<AskedQuestionRecord>.recordAnsweredPlannerQuestion(
    group: AiChatCardGroup?,
    selectedOptions: List<AiChatCardOption>
): List<AskedQuestionRecord> {
    group ?: return this
    val topicPath = group.topicPath.toPlannerTopicPath()
    if (topicPath.isBlank()) return this
    val answerIds = selectedOptions
        .filter { option -> option.groupId == group.id }
        .map { option -> sanitizeFollowUpOptionId(option.id, group.id) }
        .distinct()
    return map { record ->
        if (record.topicPath == topicPath && record.questionId == group.questionId.ifBlank { group.id }) {
            record.copy(answerIds = answerIds, answered = true)
        } else {
            record
        }
    }
}

private const val PRE_DESTINATION_QUESTION_LIMIT = 7

private fun allowedTopicRootsForPhase(phase: PlannerPhase): Set<String> {
    return when (phase) {
        PlannerPhase.PRE_DESTINATION -> setOf(
            "traveler_context",
            "party",
            "destination_style",
            "interests",
            "food_preferences",
            "activity_preferences",
            "budget",
            "pace",
            "duration",
            "travel_timeline",
            "date_window",
            "origin",
            "must_haves",
            "avoid"
        )

        PlannerPhase.DESTINATION_LOCKED -> setOf(
            "travel_timeline",
            "date_window",
            "duration",
            "discovery_help",
            "budget",
            "pace",
            "food_preferences",
            "activity_preferences",
            "restaurant_help",
            "activity_help",
            "hotel_preferences",
            "neighborhood_preferences",
            "must_haves",
            "avoid"
        )

        PlannerPhase.VISUAL_RECOMMENDATIONS,
        PlannerPhase.TRIP_BUILD -> emptySet()
    }
}

private fun topicPathAllowedForPhase(topicPath: String, phase: PlannerPhase): Boolean {
    val root = topicPath.substringBefore('.')
    return root in allowedTopicRootsForPhase(phase)
}

private fun isAllowedChildRefinement(
    group: AiChatCardGroup,
    askedQuestionRecords: List<AskedQuestionRecord>
): Boolean {
    val topicPath = group.topicPath.toPlannerTopicPath()
    val parentTopicPath = group.parentTopicPath.toPlannerTopicPath()
    val parentAnswerId = group.parentAnswerId
    if (topicPath.isBlank() || parentTopicPath.isBlank() || parentAnswerId.isBlank()) return false
    if (!topicPath.startsWith("$parentTopicPath.")) return false
    if (askedQuestionRecords.any { record -> record.topicPath == topicPath }) return false
    fun List<String>.containsAnswerId(rawAnswerId: String): Boolean {
        return any { answerId -> answerId == rawAnswerId || answerId.endsWith("_$rawAnswerId") }
    }
    return askedQuestionRecords.any { record ->
        record.topicPath == parentTopicPath &&
            record.answered &&
            record.answerIds.containsAnswerId(parentAnswerId)
    }
}

private fun topicPathTargetsKnownProfileField(
    topicPath: String,
    profile: AiTripIntakeProfile
): Boolean {
    return when (topicPath.substringBefore('.')) {
        "traveler_context",
        "party" -> profile.tripType != AiTripType.UNKNOWN || profile.partySummary.isNotBlank()
        "destination_style" -> profile.destinationStyle.isNotEmpty()
        "interests" -> profile.interests.size >= 2
        "food_preferences" -> profile.cuisinePreferences.isNotEmpty()
        "activity_preferences" -> profile.activitySubCategories.isNotEmpty()
        "budget" -> profile.budgetLevel != AiBudgetLevel.UNKNOWN || profile.budgetTotal != null
        "pace" -> profile.pace != AiTripPacePreference.UNKNOWN
        "duration" -> profile.durationDays != null || profile.durationFlexible
        "travel_timeline",
        "date_window" -> profile.dateWindow.isNotBlank() || (profile.dateFrom.isNotBlank() && profile.dateTo.isNotBlank())
        "origin" -> profile.origin.isNotBlank()
        else -> false
    }
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
        .take(4)
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
        AiTripIntakeAckKey.GOT_IT -> listOf("Got it!", "Noted!", "Makes sense!").random()
        AiTripIntakeAckKey.SOUNDS_GOOD -> listOf("Sounds good!", "Love it!", "Nice choice!").random()
        AiTripIntakeAckKey.UNDERSTOOD -> listOf("Got it!", "Understood.", "Makes sense!").random()
        AiTripIntakeAckKey.PERFECT -> listOf("Perfect!", "Great choice!", "Awesome!").random()
    }
}

private fun AiTripType.promptValue(): String = name.lowercase(Locale.US)

private fun AiBudgetLevel.promptValue(): String = name.lowercase(Locale.US)

private fun AiTripPacePreference.promptValue(): String = name.lowercase(Locale.US)
