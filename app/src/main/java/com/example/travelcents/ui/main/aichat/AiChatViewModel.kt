package com.example.travelcents.ui.main.aichat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ai.chat.AiChatCardCatalog
import com.example.travelcents.data.ai.chat.AiChatCardGroup
import com.example.travelcents.data.ai.chat.AiChatCardOption
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.AiChatSender
import com.example.travelcents.data.ai.chat.AiChatSessionState
import com.example.travelcents.data.ai.chat.AiChatSessionStore
import com.example.travelcents.data.ai.chat.AiChatUiState
import com.example.travelcents.data.ai.chat.AiCuratedTripCatalog
import com.example.travelcents.data.ai.chat.AiCuratedTripSource
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiTravelerProfile
import com.example.travelcents.data.ai.chat.AiTravelerProfileReducer
import com.example.travelcents.data.ai.chat.AiTripIntakeDecisionType
import com.example.travelcents.data.ai.chat.AiTripIntakeOrchestrator
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.ai.chat.AiTripIntakeTurnResult
import com.example.travelcents.data.ai.chat.PersistedAiChatMessage
import com.example.travelcents.data.ai.chat.PersistedAiChatSnapshot
import com.example.travelcents.data.ai.chat.mergeIntakeProfile
import com.example.travelcents.data.ai.chat.mergePatch
import com.example.travelcents.data.ai.chat.toCardGroup
import com.example.travelcents.data.ai.chat.toModel
import com.example.travelcents.data.ai.chat.toPersisted
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.trip.TripKey
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val conversationItems = mutableListOf<AiChatItem.TextMessage>()
    private val sessionStore = AiChatSessionStore(application)
    private val curatedTripCatalog = AiCuratedTripCatalog()
    private val intakeOrchestrator = AiTripIntakeOrchestrator()
    private val auth = FirebaseAuth.getInstance()

    private var sessionState = AiChatSessionState()
    private var isLoading = false

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        restoreLastSessionOrStartFresh()
    }

    fun updateDraftText(text: String) {
        sessionState = sessionState.copy(draftText = text)
        publishUiState()
    }

    fun toggleStarterCard(option: AiChatCardOption) {
        toggleDraftOption(option, allowMultiple = true)
    }

    fun toggleResponseCard(option: AiChatCardOption, group: AiChatCardGroup) {
        toggleDraftOption(option, allowMultiple = group.allowMultiple)
    }

    fun handleRecommendedStarterSelection(
        starter: AiCuratedTripStarter,
        onOpenTrip: (TripKey) -> Unit,
        onCreateDraftTrip: (AiCuratedTripStarter, AiTripIntakeProfile) -> Unit
    ) {
        selectCuratedTrip(starter)

        when (starter.source) {
            AiCuratedTripSource.FIRESTORE -> {
                starter.tripKey?.let(onOpenTrip)
            }

            AiCuratedTripSource.GENERATED -> {
                onCreateDraftTrip(starter, sessionState.intakeProfile)
            }
        }
    }

    fun selectCuratedTrip(starter: AiCuratedTripStarter) {
        val userMessage = AiChatItem.TextMessage(
            text = buildCuratedTripSelectionMessage(starter),
            sender = AiChatSender.USER
        )
        val llmUserMessage = buildCuratedTripSelectionPrompt(starter)
        conversationItems += userMessage

        val updatedProfile = AiTravelerProfileReducer.merge(sessionState.profile, llmUserMessage)
        val updatedIntakeProfile = sessionState.intakeProfile.mergePatch(updatedProfile.intakeProfile())
        val assistantMessage = AiChatItem.TextMessage(
            text = buildCuratedTripAssistantReply(starter),
            sender = AiChatSender.ASSISTANT
        )
        conversationItems += assistantMessage

        val followUpGroup = buildLegacyFallbackFollowUpGroup(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            lastUserInput = llmUserMessage,
            askedGroupIds = sessionState.askedFollowUpGroupIds
        )

        sessionState = sessionState.copy(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            stage = AiTravelerProfileReducer.stageFor(updatedProfile),
            quickReplies = AiTravelerProfileReducer.quickRepliesFor(updatedProfile),
            llmHistory = sessionState.llmHistory +
                LlmMessage(role = "user", content = llmUserMessage) +
                LlmMessage(role = "assistant", content = assistantMessage.text),
            askedFollowUpGroupIds = recordAskedFollowUpGroupId(
                existingIds = sessionState.askedFollowUpGroupIds,
                group = followUpGroup
            ),
            activeResponseCardGroup = followUpGroup,
            activeCuratedTripRow = null,
            anchorMessageId = userMessage.id
        )

        persistLastSession()
        publishUiState()
        generateTitleIfNeeded()
    }

    fun removeDraftOption(optionId: String) {
        sessionState = sessionState.copy(
            selectedDraftOptions = sessionState.selectedDraftOptions.filterNot { option ->
                option.id == optionId
            }
        )
        publishUiState()
    }

    fun sendDraft() {
        val trimmedText = sessionState.draftText.trim()
        val selectedOptions = sessionState.selectedDraftOptions.distinctBy(AiChatCardOption::id)
        if (trimmedText.isBlank() && selectedOptions.isEmpty()) return

        val submittedMessage = AiChatItem.TextMessage(
            text = buildVisibleUserMessage(selectedOptions, trimmedText),
            sender = AiChatSender.USER
        )
        val llmUserMessage = buildLlmUserMessage(selectedOptions, trimmedText)

        conversationItems += submittedMessage

        val updatedProfile = AiTravelerProfileReducer.merge(sessionState.profile, llmUserMessage)
        val updatedIntakeProfile = sessionState.intakeProfile.mergePatch(updatedProfile.intakeProfile())
        sessionState = sessionState.copy(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            stage = AiTravelerProfileReducer.stageFor(updatedProfile),
            quickReplies = AiTravelerProfileReducer.quickRepliesFor(updatedProfile),
            llmHistory = sessionState.llmHistory + LlmMessage(role = "user", content = llmUserMessage),
            draftText = "",
            selectedDraftOptions = emptyList(),
            activeResponseCardGroup = null,
            activeCuratedTripRow = null,
            anchorMessageId = submittedMessage.id
        )
        isLoading = true
        persistLastSession()
        publishUiState()

        viewModelScope.launch {
            val intakeResult = runCatching {
                intakeOrchestrator.analyzeTurn(
                    currentProfile = sessionState.intakeProfile,
                    latestUserInput = llmUserMessage
                )
            }.onFailure { error ->
                Log.w(TAG, "Structured intake analysis failed. Falling back to legacy follow-up flow.", error)
            }.getOrNull()

            val mergedIntakeProfile = sessionState.intakeProfile.mergePatch(intakeResult?.profilePatch)
            val mergedProfile = sessionState.profile.mergeIntakeProfile(mergedIntakeProfile)

            val assistantResponse = runCatching {
                LlmClient.complete(
                    messages = buildLlmMessages(
                        profile = mergedProfile,
                        intakeProfile = mergedIntakeProfile,
                        history = sessionState.llmHistory
                    )
                )
            }.getOrElse { error ->
                val errorMessage = error.localizedMessage
                    ?: "Check your AI provider settings and network connection."
                "I hit a setup issue: $errorMessage"
            }.ifBlank {
                "That helps. I can narrow it down further with one more choice."
            }

            conversationItems += AiChatItem.TextMessage(
                text = assistantResponse,
                sender = AiChatSender.ASSISTANT
            )

            val followUpGroup = resolveFollowUpGroup(
                intakeResult = intakeResult,
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile,
                lastUserInput = llmUserMessage,
                askedGroupIds = sessionState.askedFollowUpGroupIds
            )
            val curatedTripRow = when (intakeResult?.decision?.type) {
                AiTripIntakeDecisionType.ASK_MORE -> null
                AiTripIntakeDecisionType.BUILD_FROM_SCRATCH -> curatedTripCatalog.recommendTrips(
                    profile = mergedProfile,
                    viewerUid = null
                )

                AiTripIntakeDecisionType.RECOMMEND_CURATED,
                null -> curatedTripCatalog.recommendTrips(
                    profile = mergedProfile,
                    viewerUid = currentUserId()
                )
            }

            sessionState = sessionState.copy(
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile,
                stage = AiTravelerProfileReducer.stageFor(mergedProfile),
                quickReplies = AiTravelerProfileReducer.quickRepliesFor(mergedProfile),
                llmHistory = sessionState.llmHistory + LlmMessage(
                    role = "assistant",
                    content = assistantResponse
                ),
                askedFollowUpGroupIds = recordAskedFollowUpGroupId(
                    existingIds = sessionState.askedFollowUpGroupIds,
                    group = followUpGroup
                ),
                activeResponseCardGroup = followUpGroup,
                activeCuratedTripRow = curatedTripRow
            )
            isLoading = false
            persistLastSession()
            publishUiState()
            generateTitleIfNeeded()
        }
    }

    fun startNewChat() {
        resetToFreshSession()
        publishUiState()
    }

    fun loadSession(sessionId: String) {
        val snapshot = sessionStore.listSessions(currentUserId())
            .firstOrNull { persisted -> persisted.sessionId == sessionId }
            ?: return

        sessionStore.setActiveSession(currentUserId(), sessionId)
        restoreSnapshot(snapshot)
        processSnapshot = snapshot
        publishUiState()
    }

    fun deleteSession(sessionId: String) {
        sessionStore.clearSession(currentUserId(), sessionId)
        if (sessionState.sessionId == sessionId) {
            resetToFreshSession()
        }
        publishUiState()
    }

    fun clearAllHistory() {
        sessionStore.clearAll(currentUserId())
        resetToFreshSession()
        publishUiState()
    }

    private fun toggleDraftOption(
        option: AiChatCardOption,
        allowMultiple: Boolean
    ) {
        val currentlySelected = sessionState.selectedDraftOptions
        val nextSelection = when {
            currentlySelected.any { selected -> selected.id == option.id } -> {
                currentlySelected.filterNot { selected -> selected.id == option.id }
            }

            allowMultiple -> currentlySelected + option

            else -> currentlySelected
                .filterNot { selected -> selected.groupId == option.groupId }
                .plus(option)
        }

        sessionState = sessionState.copy(selectedDraftOptions = nextSelection)
        publishUiState()
    }

    private fun publishUiState() {
        _uiState.value = AiChatUiState(
            items = buildVisibleItems(),
            quickReplies = sessionState.quickReplies,
            starterCards = sessionState.starterCards,
            draftText = sessionState.draftText,
            selectedDraftOptions = sessionState.selectedDraftOptions,
            historyEntries = sessionStore.historyEntries(currentUserId()),
            anchorMessageId = sessionState.anchorMessageId,
            activeResponseCardGroupId = sessionState.activeResponseCardGroup?.id,
            activeCuratedTripRowId = sessionState.activeCuratedTripRow?.id,
            isLoading = isLoading
        )
    }

    private fun buildVisibleItems(): List<AiChatItem> {
        return buildList {
            addAll(conversationItems)
            sessionState.activeResponseCardGroup?.let { group ->
                add(
                    AiChatItem.ResponseCardGroup(
                        id = group.id,
                        group = group
                    )
                )
            }
            sessionState.activeCuratedTripRow?.let { row ->
                add(
                    AiChatItem.CuratedTripRow(
                        id = row.id,
                        row = row
                    )
                )
            }
        }
    }

    private fun restoreLastSessionOrStartFresh() {
        val cached = processSnapshot
            ?.takeIf { snapshot ->
                snapshot.messages.any { message -> message.sender == AiChatSender.USER }
            }

        if (cached == null) {
            resetToFreshSession()
            publishUiState()
            return
        }

        restoreSnapshot(cached)
        publishUiState()
    }

    private fun restoreSnapshot(snapshot: PersistedAiChatSnapshot) {
        conversationItems.clear()
        conversationItems += snapshot.messages.map { message ->
            AiChatItem.TextMessage(
                text = message.text,
                sender = message.sender
            )
        }
        sessionState = AiChatSessionState(
            sessionId = snapshot.sessionId,
            title = snapshot.title,
            profile = snapshot.profile,
            intakeProfile = snapshot.intakeProfile ?: snapshot.profile.intakeProfile(),
            stage = snapshot.stage,
            quickReplies = snapshot.quickReplies.ifEmpty {
                AiTravelerProfileReducer.quickRepliesFor(snapshot.profile)
            },
            llmHistory = snapshot.llmHistory,
            starterCards = AiChatCardCatalog.starterCards(snapshot.sessionId),
            askedFollowUpGroupIds = snapshot.askedFollowUpGroupIds,
            activeResponseCardGroup = snapshot.activeResponseCardGroup?.toModel(),
            activeCuratedTripRow = snapshot.activeCuratedTripRow?.toModel(),
            anchorMessageId = snapshot.anchorMessageId
        )
        isLoading = false
    }

    private fun resetToFreshSession() {
        conversationItems.clear()
        val sessionId = UUID.randomUUID().toString()
        sessionState = AiChatSessionState(
            sessionId = sessionId,
            starterCards = AiChatCardCatalog.starterCards(sessionId)
        )
        isLoading = false
        processSnapshot = null
    }

    private fun persistLastSession() {
        if (conversationItems.none { item -> item.sender == AiChatSender.USER }) {
            return
        }

        val snapshot = PersistedAiChatSnapshot(
            sessionId = sessionState.sessionId ?: UUID.randomUUID().toString(),
            title = sessionState.title,
            createdAtEpochMs = existingSessionCreatedAt(),
            updatedAtEpochMs = System.currentTimeMillis(),
            messages = conversationItems.map { message ->
                PersistedAiChatMessage(
                    text = message.text,
                    sender = message.sender
                )
            },
            profile = sessionState.profile,
            intakeProfile = sessionState.intakeProfile,
            stage = sessionState.stage,
            quickReplies = sessionState.quickReplies,
            llmHistory = sessionState.llmHistory,
            askedFollowUpGroupIds = sessionState.askedFollowUpGroupIds,
            activeResponseCardGroup = sessionState.activeResponseCardGroup?.toPersisted(),
            activeCuratedTripRow = sessionState.activeCuratedTripRow?.toPersisted(),
            anchorMessageId = sessionState.anchorMessageId
        )

        processSnapshot = snapshot
        sessionStore.upsertSession(
            userId = currentUserId(),
            snapshot = snapshot,
            makeActive = true
        )
    }

    private fun currentUserId(): String? = auth.currentUser?.uid

    private fun existingSessionCreatedAt(): Long {
        val sessionId = sessionState.sessionId ?: return System.currentTimeMillis()
        return sessionStore.listSessions(currentUserId())
            .firstOrNull { snapshot -> snapshot.sessionId == sessionId }
            ?.createdAtEpochMs
            ?: System.currentTimeMillis()
    }

    private fun generateTitleIfNeeded() {
        if (sessionState.title.isNotBlank()) return
        if (conversationItems.count { item -> item.sender == AiChatSender.USER } == 0) return

        viewModelScope.launch {
            val generatedTitle = runCatching {
                LlmClient.complete(
                    messages = listOf(
                        LlmMessage(
                            role = "system",
                            content = "Generate a very short title for this travel planning chat. " +
                                "Return plain text only. Maximum 3 words. No punctuation unless required."
                        ),
                        LlmMessage(
                            role = "user",
                            content = buildTitlePrompt()
                        )
                    ),
                    temperature = 0.2,
                    maxTokens = 12
                )
            }.getOrNull().orEmpty()

            val sanitizedTitle = sanitizeGeneratedTitle(generatedTitle)
                .ifBlank { fallbackSessionTitle() }

            if (sanitizedTitle.isBlank()) return@launch

            sessionState = sessionState.copy(title = sanitizedTitle)
            persistLastSession()
            publishUiState()
        }
    }

    private fun buildTitlePrompt(): String {
        val userTurns = conversationItems
            .filter { item -> item.sender == AiChatSender.USER || item.sender == AiChatSender.ASSISTANT }
            .takeLast(6)
            .joinToString(separator = "\n") { item ->
                val role = if (item.sender == AiChatSender.USER) "User" else "Assistant"
                "$role: ${item.text}"
            }

        return "Conversation:\n$userTurns"
    }

    private fun sanitizeGeneratedTitle(rawTitle: String): String {
        return rawTitle
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .replace("\"", "")
            .replace("'", "")
            .replace(Regex("[\\p{Punct}&&[^&/-]]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { token -> token.isNotBlank() }
            .take(3)
            .joinToString(" ")
            .trim()
    }

    private fun fallbackSessionTitle(): String {
        return conversationItems.firstOrNull { item ->
            item.sender == AiChatSender.USER
        }?.text
            ?.lineSequence()
            ?.firstOrNull()
            .orEmpty()
            .split(Regex("\\s+"))
            .filter { token -> token.isNotBlank() }
            .take(3)
            .joinToString(" ")
            .trim()
    }

    private fun buildVisibleUserMessage(
        selectedOptions: List<AiChatCardOption>,
        typedText: String
    ): String {
        val selectionLine = selectedOptions.joinToString(separator = "  •  ") { option ->
            option.label
        }

        return listOf(selectionLine, typedText)
            .filter { value -> value.isNotBlank() }
            .joinToString(separator = "\n")
            .trim()
    }

    private fun buildLlmUserMessage(
        selectedOptions: List<AiChatCardOption>,
        typedText: String
    ): String {
        return buildList {
            if (selectedOptions.isNotEmpty()) {
                add("Selected choices:")
                addAll(selectedOptions.map { option -> "- ${option.message}" })
            }
            if (typedText.isNotBlank()) {
                add("Extra context: $typedText")
            }
        }.joinToString(separator = "\n").trim()
    }

    private fun buildCuratedTripSelectionMessage(starter: AiCuratedTripStarter): String {
        return when (starter.source) {
            AiCuratedTripSource.FIRESTORE -> "Use ${starter.title} as the starter"
            AiCuratedTripSource.GENERATED -> "Build ${starter.title} from scratch"
        }
    }

    private fun buildCuratedTripSelectionPrompt(starter: AiCuratedTripStarter): String {
        val sourceLine = when (starter.source) {
            AiCuratedTripSource.FIRESTORE -> {
                val key = starter.tripKey
                if (key != null) {
                    "Use saved curated trip starter ${starter.title} for ${starter.destination}. Source trip key: ${key.ownerUid}/${key.tripId}."
                } else {
                    "Use saved curated trip starter ${starter.title} for ${starter.destination}."
                }
            }

            AiCuratedTripSource.GENERATED ->
                "Generate a fresh trip starter from scratch for ${starter.destination} using ${starter.title} as the base."
        }

        return buildString {
            appendLine(sourceLine)
            appendLine("Travel style: ${starter.travelStyle}")
            appendLine("Duration: ${starter.durationDays} days")
            append("Starter summary: ${starter.summary}")
        }
    }

    private fun buildCuratedTripAssistantReply(starter: AiCuratedTripStarter): String {
        return when (starter.source) {
            AiCuratedTripSource.FIRESTORE ->
                "I can use that saved ${starter.destination} trip as the base. Tell me what you want to refine next."

            AiCuratedTripSource.GENERATED ->
                "I can build that fresh ${starter.destination} starter from scratch. Tell me what you want to refine next."
        }
    }

    private fun buildLlmMessages(
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile,
        history: List<LlmMessage>
    ): List<LlmMessage> {
        return buildList {
            add(LlmMessage(role = "system", content = BASE_SYSTEM_PROMPT))
            add(LlmMessage(role = "system", content = profile.promptSummary()))
            add(LlmMessage(role = "system", content = "Structured intake profile JSON:\n${intakeProfile.toJson()}"))
            addAll(history)
        }
    }

    private fun resolveFollowUpGroup(
        intakeResult: AiTripIntakeTurnResult?,
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile,
        lastUserInput: String,
        askedGroupIds: List<String>
    ): AiChatCardGroup? {
        return when (intakeResult?.decision?.type) {
            AiTripIntakeDecisionType.RECOMMEND_CURATED,
            AiTripIntakeDecisionType.BUILD_FROM_SCRATCH -> {
                Log.d(TAG, "No follow-up question shown because intake decision is ${intakeResult.decision.type}.")
                null
            }

            else -> {
                val intakeGroup = intakeResult
                    ?.followUpQuestion
                    ?.toCardGroup()
                    ?.takeUnless { group -> group.id in askedGroupIds }

                if (intakeGroup != null) {
                    Log.d(TAG, "Using intake-generated follow-up '${intakeGroup.id}'.")
                    return intakeGroup
                }

                if (intakeResult?.followUpQuestion != null) {
                    Log.w(
                        TAG,
                        "Discarded intake follow-up '${intakeResult.followUpQuestion.id}'. Falling back to legacy flow."
                    )
                }

                buildLegacyFallbackFollowUpGroup(
                    profile = profile,
                    intakeProfile = intakeProfile,
                    lastUserInput = lastUserInput,
                    askedGroupIds = askedGroupIds
                )
            }
        }
    }

    private fun buildLegacyFallbackFollowUpGroup(
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile,
        lastUserInput: String,
        askedGroupIds: List<String>
    ): AiChatCardGroup? {
        val fallbackGroup = AiChatCardCatalog.nextFollowUpGroup(
            profile = profile,
            lastUserInput = lastUserInput
        )

        if (fallbackGroup.id == DESTINATION_STYLE_GROUP_ID && intakeProfile.destinationStyle.isNotEmpty()) {
            Log.d(TAG, "Skipping legacy fallback '$DESTINATION_STYLE_GROUP_ID' because intake already has destinationStyle.")
            return null
        }

        if (fallbackGroup.id in askedGroupIds) {
            Log.d(TAG, "Skipping repeated legacy fallback '${fallbackGroup.id}'.")
            return null
        }

        Log.d(TAG, "Using legacy fallback follow-up '${fallbackGroup.id}'.")
        return fallbackGroup
    }

    private fun recordAskedFollowUpGroupId(
        existingIds: List<String>,
        group: AiChatCardGroup?
    ): List<String> {
        val groupId = group?.id ?: return existingIds
        return (existingIds + groupId).distinct()
    }

    private companion object {
        private const val DESTINATION_STYLE_GROUP_ID = "destination_style"
        private const val TAG = "AiChatViewModel"
        private const val BASE_SYSTEM_PROMPT =
            "You are TravelCents AI, a trip-planning copilot inside the TravelCents app. " +
                "Be concise, helpful, and practical. Keep replies to short acknowledgment paragraphs. " +
                "The app may present follow-up choices separately, so do not stack multiple questions or long questionnaires. " +
                "Use the traveler profile context when available. Do not mention model vendors or say you are a generic AI chatbot."

        private var processSnapshot: PersistedAiChatSnapshot? = null
    }
}
