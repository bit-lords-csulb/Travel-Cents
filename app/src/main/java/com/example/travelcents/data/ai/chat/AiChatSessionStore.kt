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
    val stage: AiChatStage = AiChatStage.ONBOARDING,
    val quickReplies: List<AiChatQuickReply> = emptyList(),
    val llmHistory: List<LlmMessage> = emptyList(),
    val activeResponseCardGroup: PersistedAiChatCardGroup? = null,
    val activeCuratedTripRow: PersistedAiCuratedTripRow? = null,
    val anchorMessageId: String? = null
)

data class PersistedAiChatMessage(
    val text: String,
    val sender: AiChatSender
)

data class PersistedAiChatCardOption(
    val id: String,
    val label: String,
    val message: String,
    val groupId: String
)

data class PersistedAiChatCardGroup(
    val id: String,
    val title: String,
    val subtitle: String,
    val options: List<PersistedAiChatCardOption>,
    val allowMultiple: Boolean
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
    val tripId: String = ""
)

data class PersistedAiCuratedTripRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val trips: List<PersistedAiCuratedTripStarter>
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
        }?.text.orEmpty().trim()
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
        }?.text?.trim().orEmpty().ifBlank {
            snapshot.messages.lastOrNull()?.text?.trim().orEmpty()
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
                groupId = option.groupId
            )
        },
        allowMultiple = allowMultiple
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
                groupId = option.groupId
            )
        },
        allowMultiple = allowMultiple
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
                tripId = starter.tripKey?.tripId.orEmpty()
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
                }
            )
        }
    )
}
