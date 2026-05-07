package com.example.travelcents.data.ai.chat

import android.content.Context
import androidx.core.content.edit
import com.example.travelcents.data.ai.model.LlmMessage
import com.google.gson.Gson
import java.util.UUID

data class PersistedAiChatSnapshot(
    val sessionId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val messages: List<PersistedAiChatMessage> = emptyList(),
    val profile: AiTravelerProfile = AiTravelerProfile(),
    val intakeProfile: AiTripIntakeProfile? = null,
    val planningObjective: String = "",
    val stage: AiChatStage = AiChatStage.ONBOARDING,
    val preferenceProfile: PreferenceProfile? = null,
    val discoveryTrack: DiscoveryTrack? = null,
    val discoverySlots: List<DiscoverySlot>? = null,
    val discoverySuggestionPool: List<SuggestionItem>? = null,
    val llmHistory: List<LlmMessage> = emptyList(),
    val askedQuestionRecords: List<AskedQuestionRecord> = emptyList(),
    val plannerPhase: PlannerPhase = PlannerPhase.PRE_DESTINATION,
    val preDestinationQuestionCount: Int = 0,
    val activeResponseCardGroup: PersistedAiChatCardGroup? = null,
    val activeDestinationRecommendationRow: PersistedAiDestinationRecommendationRow? = null,
    val activeCuratedTripRow: PersistedAiCuratedTripRow? = null,
    val activePlaceRecommendationRow: PersistedAiPlaceRecommendationRow? = null,
    val activePreferenceQuestionCard: AiChatItem.PreferenceQuestionCard? = null,
    val activeSuggestionCarouselCard: AiChatItem.SuggestionCarouselCard? = null,
    val anchorMessageId: String? = null,
    val lockedDestination: String? = null,
    val lockedDestinationImageUrl: String? = null,
    val previewDraft: AiChatPreviewDraft? = null
)

data class PersistedAiChatMessage(
    val text: String,
    val sender: AiChatSender,
    val tags: List<String>? = null
)

data class PersistedAiChatCardOption(
    val id: String,
    val label: String,
    val message: String,
    val groupId: String,
    val requiresText: Boolean = false
)

data class PersistedAiChatCardGroup(
    val id: String,
    val title: String,
    val subtitle: String,
    val options: List<PersistedAiChatCardOption>,
    val allowMultiple: Boolean,
    val allowOther: Boolean = false,
    val otherPromptHint: String = "",
    val topicPath: String = "",
    val questionId: String = id,
    val source: PlannerQuestionSource = PlannerQuestionSource.LLM,
    val parentTopicPath: String = "",
    val parentAnswerId: String = ""
)

data class PersistedAiCuratedTripStarter(
    val id: String,
    val title: String,
    val destination: String,
    val durationDays: Int,
    val travelStyle: String,
    val summary: String,
    val matchReason: String,
    val source: String,
    val ownerUid: String = "",
    val tripId: String = "",
    val seedId: String? = null,
    val durationOptions: List<Int>? = null,
    val highlights: List<String>? = null,
    val heroImageUrl: String? = null
)

data class PersistedAiCuratedTripRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val trips: List<PersistedAiCuratedTripStarter>
)

data class PersistedAiDestinationRecommendation(
    val id: String,
    val destination: String,
    val summary: String,
    val matchReason: String,
    val seedId: String? = null,
    val imageUrl: String? = null
)

data class PersistedAiDestinationRecommendationRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val recommendations: List<PersistedAiDestinationRecommendation>
)

data class PersistedAiPlaceRecommendation(
    val id: String,
    val name: String,
    val category: String,
    val area: String,
    val summary: String,
    val matchReason: String,
    val imageUrl: String? = null
)

data class PersistedAiPlaceRecommendationRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val recommendations: List<PersistedAiPlaceRecommendation>,
    val rowType: String = AiPlaceRecommendationRowType.GENERAL.name,
    val actionLabels: List<String> = emptyList(),
    val actionsEnabled: Boolean = false
)

data class PersistedAiChatStoreState(
    val activeSessionId: String? = null,
    val sessions: List<PersistedAiChatSnapshot> = emptyList()
)

data class AiChatHistoryEntry(
    val sessionId: String,
    val title: String,
    val snippet: String,
    val updatedAtEpochMs: Long
)

class AiChatSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadActiveSession(userId: String?): PersistedAiChatSnapshot? {
        val state = loadState(userId)
        val activeSessionId = state.activeSessionId ?: return null
        return state.sessions.firstOrNull { session -> session.sessionId == activeSessionId }
    }

    fun listSessions(userId: String?): List<PersistedAiChatSnapshot> {
        return loadState(userId).sessions
            .sortedByDescending(PersistedAiChatSnapshot::updatedAtEpochMs)
    }

    fun upsertSession(
        userId: String?,
        snapshot: PersistedAiChatSnapshot,
        makeActive: Boolean = true
    ) {
        val current = loadState(userId)
        val updatedSessions = current.sessions
            .filterNot { session -> session.sessionId == snapshot.sessionId }
            .plus(snapshot)
            .sortedByDescending(PersistedAiChatSnapshot::updatedAtEpochMs)
        val nextState = current.copy(
            activeSessionId = if (makeActive) snapshot.sessionId else current.activeSessionId,
            sessions = updatedSessions
        )
        saveState(userId, nextState)
    }

    fun setActiveSession(userId: String?, sessionId: String?) {
        val current = loadState(userId)
        saveState(
            userId = userId,
            state = current.copy(
                activeSessionId = sessionId?.takeIf { activeId ->
                    current.sessions.any { session -> session.sessionId == activeId }
                }
            )
        )
    }

    fun clearSession(userId: String?, sessionId: String) {
        val current = loadState(userId)
        val remainingSessions = current.sessions.filterNot { session -> session.sessionId == sessionId }
        saveState(
            userId = userId,
            state = current.copy(
                activeSessionId = current.activeSessionId.takeUnless { it == sessionId },
                sessions = remainingSessions
            )
        )
    }

    fun clearAll(userId: String?) {
        prefs.edit {
            remove(storeKeyFor(userId))
            remove(legacyKeyFor(userId))
        }
    }

    fun historyEntries(userId: String?): List<AiChatHistoryEntry> {
        return listSessions(userId).mapNotNull { snapshot ->
            val title = deriveTitle(snapshot)
            val snippet = deriveSnippet(snapshot)
            if (title.isBlank() && snippet.isBlank()) return@mapNotNull null

            AiChatHistoryEntry(
                sessionId = snapshot.sessionId,
                title = title.ifBlank { "TravelCents AI chat" },
                snippet = snippet,
                updatedAtEpochMs = snapshot.updatedAtEpochMs
            )
        }
    }

    private fun loadState(userId: String?): PersistedAiChatStoreState {
        val migrated = migrateLegacyIfNeeded(userId)
        val raw = prefs.getString(storeKeyFor(userId), null).orEmpty()
        if (raw.isBlank()) {
            return migrated ?: PersistedAiChatStoreState()
        }

        return runCatching {
            gson.fromJson(raw, PersistedAiChatStoreState::class.java)
        }.getOrNull() ?: migrated ?: PersistedAiChatStoreState()
    }

    private fun saveState(userId: String?, state: PersistedAiChatStoreState) {
        prefs.edit {
            putString(storeKeyFor(userId), gson.toJson(state))
        }
    }

    private fun migrateLegacyIfNeeded(userId: String?): PersistedAiChatStoreState? {
        val legacyKey = legacyKeyFor(userId)
        val legacyRaw = prefs.getString(legacyKey, null).orEmpty()
        if (legacyRaw.isBlank()) return null
        if (!prefs.getString(storeKeyFor(userId), null).isNullOrBlank()) return null

        val migratedSnapshot = runCatching {
            gson.fromJson(legacyRaw, PersistedAiChatSnapshot::class.java)
        }.getOrNull()?.copy(
            sessionId = UUID.randomUUID().toString(),
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        ) ?: return null

        val migratedState = PersistedAiChatStoreState(
            activeSessionId = migratedSnapshot.sessionId,
            sessions = listOf(migratedSnapshot)
        )
        saveState(userId, migratedState)
        prefs.edit { remove(legacyKey) }
        return migratedState
    }

    private fun deriveTitle(snapshot: PersistedAiChatSnapshot): String {
        if (snapshot.title.isNotBlank()) return snapshot.title.trim()

        val firstUserMessage = snapshot.messages.firstOrNull { message ->
            message.sender == AiChatSender.USER
        }?.renderMessageText().orEmpty().trim()
        if (firstUserMessage.isBlank()) return ""

        return firstUserMessage
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .trim()
            .removeSuffix(".")
            .split(Regex("\\s+"))
            .take(3)
            .joinToString(" ")
            .take(42)
            .trim()
    }

    private fun deriveSnippet(snapshot: PersistedAiChatSnapshot): String {
        return snapshot.messages.lastOrNull { message ->
            message.sender == AiChatSender.ASSISTANT
        }?.renderMessageText()?.trim().orEmpty().ifBlank {
            snapshot.messages.lastOrNull()?.renderMessageText()?.trim().orEmpty()
        }.lineSequence()
            .firstOrNull()
            .orEmpty()
            .take(120)
            .trim()
    }

    private fun storeKeyFor(userId: String?): String {
        val resolvedUserId = userId?.takeIf { it.isNotBlank() } ?: "anonymous"
        return "${STORE_KEY_PREFIX}_$resolvedUserId"
    }

    private fun legacyKeyFor(userId: String?): String {
        val resolvedUserId = userId?.takeIf { it.isNotBlank() } ?: "anonymous"
        return "${LEGACY_KEY_PREFIX}_$resolvedUserId"
    }

    private companion object {
        private const val PREFS_NAME = "ai_chat_sessions"
        private const val STORE_KEY_PREFIX = "ai_chat_store"
        private const val LEGACY_KEY_PREFIX = "last_ai_chat"
    }
}

private fun PersistedAiChatMessage.renderMessageText(): String {
    val tagLine = tags.orEmpty().takeIf(List<String>::isNotEmpty)
        ?.joinToString(separator = " & ") { tag -> "($tag)" }
        .orEmpty()
    return listOf(tagLine, text.trim())
        .filter { value -> value.isNotBlank() }
        .joinToString(separator = "\n")
        .trim()
}

fun AiChatCardGroup.toPersisted(): PersistedAiChatCardGroup {
    return PersistedAiChatCardGroup(
        id = id,
        title = title,
        subtitle = subtitle,
        options = options.map { option ->
            PersistedAiChatCardOption(
                id = option.id,
                label = option.label,
                message = option.message,
                groupId = option.groupId,
                requiresText = option.requiresText
            )
        },
        allowMultiple = allowMultiple,
        allowOther = allowOther,
        otherPromptHint = otherPromptHint,
        topicPath = topicPath,
        questionId = questionId,
        source = source,
        parentTopicPath = parentTopicPath,
        parentAnswerId = parentAnswerId
    )
}

fun PersistedAiChatCardGroup.toModel(): AiChatCardGroup {
    return AiChatCardGroup(
        id = id,
        title = title,
        subtitle = subtitle,
        options = options.map { option ->
            AiChatCardOption(
                id = option.id,
                label = option.label,
                message = option.message,
                groupId = option.groupId,
                requiresText = option.requiresText
            )
        },
        allowMultiple = allowMultiple,
        allowOther = allowOther,
        otherPromptHint = otherPromptHint,
        topicPath = topicPath,
        questionId = questionId,
        source = source,
        parentTopicPath = parentTopicPath,
        parentAnswerId = parentAnswerId
    )
}

fun AiDestinationRecommendationRow.toPersisted(): PersistedAiDestinationRecommendationRow {
    return PersistedAiDestinationRecommendationRow(
        id = id,
        title = title,
        subtitle = subtitle,
        recommendations = recommendations.map { recommendation ->
            PersistedAiDestinationRecommendation(
                id = recommendation.id,
                destination = recommendation.destination,
                summary = recommendation.summary,
                matchReason = recommendation.matchReason,
                seedId = recommendation.seedId,
                imageUrl = recommendation.imageUrl
            )
        }
    )
}

fun PersistedAiDestinationRecommendationRow.toModel(): AiDestinationRecommendationRow {
    return AiDestinationRecommendationRow(
        id = id,
        title = title,
        subtitle = subtitle,
        recommendations = recommendations.map { recommendation ->
            AiDestinationRecommendation(
                id = recommendation.id,
                destination = recommendation.destination,
                summary = recommendation.summary,
                matchReason = recommendation.matchReason,
                seedId = recommendation.seedId,
                imageUrl = recommendation.imageUrl
            )
        }
    )
}

fun AiCuratedTripRow.toPersisted(): PersistedAiCuratedTripRow {
    return PersistedAiCuratedTripRow(
        id = id,
        title = title,
        subtitle = subtitle,
        trips = trips.map { starter ->
            PersistedAiCuratedTripStarter(
                id = starter.id,
                title = starter.title,
                destination = starter.destination,
                durationDays = starter.durationDays,
                travelStyle = starter.travelStyle,
                summary = starter.summary,
                matchReason = starter.matchReason,
                source = starter.source.name,
                ownerUid = starter.tripKey?.ownerUid.orEmpty(),
                tripId = starter.tripKey?.tripId.orEmpty(),
                seedId = starter.seedId,
                durationOptions = starter.durationOptions,
                highlights = starter.highlights,
                heroImageUrl = starter.heroImageUrl
            )
        }
    )
}

fun PersistedAiCuratedTripRow.toModel(): AiCuratedTripRow {
    return AiCuratedTripRow(
        id = id,
        title = title,
        subtitle = subtitle,
        trips = trips.map { starter ->
            AiCuratedTripStarter(
                id = starter.id,
                title = starter.title,
                destination = starter.destination,
                durationDays = starter.durationDays,
                travelStyle = starter.travelStyle,
                summary = starter.summary,
                matchReason = starter.matchReason,
                source = runCatching {
                    AiCuratedTripSource.valueOf(starter.source)
                }.getOrDefault(AiCuratedTripSource.GENERATED),
                tripKey = if (starter.ownerUid.isNotBlank() && starter.tripId.isNotBlank()) {
                    com.example.travelcents.data.trip.TripKey(
                        ownerUid = starter.ownerUid,
                        tripId = starter.tripId
                    )
                } else {
                    null
                },
                seedId = starter.seedId,
                durationOptions = starter.durationOptions.orEmpty(),
                highlights = starter.highlights.orEmpty(),
                heroImageUrl = starter.heroImageUrl
            )
        }
    )
}

fun AiPlaceRecommendationRow.toPersisted(): PersistedAiPlaceRecommendationRow {
    return PersistedAiPlaceRecommendationRow(
        id = id,
        title = title,
        subtitle = subtitle,
        recommendations = recommendations.map { recommendation ->
            PersistedAiPlaceRecommendation(
                id = recommendation.id,
                name = recommendation.name,
                category = recommendation.category,
                area = recommendation.area,
                summary = recommendation.summary,
                matchReason = recommendation.matchReason,
                imageUrl = recommendation.imageUrl
            )
        },
        rowType = rowType.name,
        actionLabels = actionLabels,
        actionsEnabled = actionsEnabled
    )
}

fun PersistedAiPlaceRecommendationRow.toModel(): AiPlaceRecommendationRow {
    return AiPlaceRecommendationRow(
        id = id,
        title = title,
        subtitle = subtitle,
        recommendations = recommendations.map { recommendation ->
            AiPlaceRecommendation(
                id = recommendation.id,
                name = recommendation.name,
                category = recommendation.category,
                area = recommendation.area,
                summary = recommendation.summary,
                matchReason = recommendation.matchReason,
                imageUrl = recommendation.imageUrl
            )
        },
        rowType = runCatching {
            AiPlaceRecommendationRowType.valueOf(rowType)
        }.getOrDefault(AiPlaceRecommendationRowType.GENERAL),
        actionLabels = actionLabels,
        actionsEnabled = actionsEnabled
    )
}
