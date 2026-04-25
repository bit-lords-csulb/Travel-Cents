package com.example.travelcents.ui.main.aichat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ai.chat.AiChatCardCatalog
import com.example.travelcents.data.ai.chat.AiChatCardGroup
import com.example.travelcents.data.ai.chat.AiChatCardOption
import com.example.travelcents.data.ai.chat.AiCuratedTripCatalog
import com.example.travelcents.data.ai.chat.AiDestinationRecommendation
import com.example.travelcents.data.ai.chat.AiDestinationRecommendationEngine
import com.example.travelcents.data.ai.chat.AiCuratedTripSource
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.AiChatSender
import com.example.travelcents.data.ai.chat.AiChatSessionState
import com.example.travelcents.data.ai.chat.AiChatSessionStore
import com.example.travelcents.data.ai.chat.AiChatUiState
import com.example.travelcents.data.ai.chat.AiTravelerProfile
import com.example.travelcents.data.ai.chat.AiTravelerProfileReducer
import com.example.travelcents.data.ai.chat.AiDestinationRecommendationRow
import com.example.travelcents.data.ai.chat.AiChatTripOption
import com.example.travelcents.data.ai.chat.AiPlaceRecommendationRow
import com.example.travelcents.data.ai.chat.AiPlaceRecommendationRowType
import com.example.travelcents.data.ai.chat.AiPlaceRecommendationCoordinator
import com.example.travelcents.data.ai.chat.AiRecommendationMapper
import com.example.travelcents.data.ai.chat.AiSingleEventCoordinator
import com.example.travelcents.data.ai.chat.AiSingleEventResolution
import com.example.travelcents.data.ai.chat.AiSingleEventSuggestion
import com.example.travelcents.data.ai.chat.AiToolCall
import com.example.travelcents.data.ai.chat.AiToolRouterOrchestrator
import com.example.travelcents.data.ai.chat.AiToolRouterResult
import com.example.travelcents.data.ai.chat.AiTripIntakeNextAction
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.ai.chat.AiTripIntakeOrchestrator
import com.example.travelcents.data.ai.chat.AiTripIntakeTurnResult
import com.example.travelcents.data.ai.chat.PersistedAiChatMessage
import com.example.travelcents.data.ai.chat.PersistedAiChatSnapshot
import com.example.travelcents.data.ai.chat.mergeIntakeProfile
import com.example.travelcents.data.ai.chat.mergePatch
import com.example.travelcents.data.ai.chat.toCardGroup
import com.example.travelcents.data.ai.chat.toDestinationRecommendationRow
import com.example.travelcents.data.ai.chat.withDestinationRecommendations
import com.example.travelcents.data.ai.chat.toModel
import com.example.travelcents.data.ai.chat.toPersisted
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.remote.DestinationImageRepository
import com.example.travelcents.data.trip.remote.WikipediaApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val conversationItems = mutableListOf<AiChatItem.TextMessage>()
    private val sessionStore = AiChatSessionStore(application)
    private val curatedTripCatalog = AiCuratedTripCatalog()
    private val destinationRecommendationEngine = AiDestinationRecommendationEngine()
    private val placeRecommendationCoordinator = AiPlaceRecommendationCoordinator(curatedTripCatalog = curatedTripCatalog)
    private val singleEventCoordinator = AiSingleEventCoordinator()
    private val intakeOrchestrator = AiTripIntakeOrchestrator()
    private val toolRouter = AiToolRouterOrchestrator()
    private val auth = FirebaseAuth.getInstance()
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(FirebaseFirestore.getInstance())

    private var pendingAddToTripEvent: AiSingleEventSuggestion? = null
    private var availableTripsForAdd: List<AiChatTripOption> = emptyList()

    private val wikipediaClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", WIKIMEDIA_USER_AGENT)
                .header("Api-User-Agent", WIKIMEDIA_USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    private val wikipedia: WikipediaApiService = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .client(wikipediaClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WikipediaApiService::class.java)
    private val destinationImages = DestinationImageRepository(wikipedia)
    private val heroImageCache = mutableMapOf<String, String>()

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
        if (isLoading) return
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

            AiCuratedTripSource.SEEDED,
            AiCuratedTripSource.GENERATED -> {
                onCreateDraftTrip(starter, sessionState.intakeProfile)
            }
        }
    }

    fun selectCuratedTrip(starter: AiCuratedTripStarter) {
        val isFirstTurn = conversationItems.isEmpty()
        val userMessage = AiChatItem.TextMessage(
            text = buildCuratedTripSelectionMessage(starter),
            sender = AiChatSender.USER
        )
        val llmUserMessage = buildCuratedTripSelectionPrompt(starter)
        conversationItems += userMessage

        val updatedProfile = AiTravelerProfileReducer.merge(sessionState.profile, llmUserMessage)
        val updatedIntakeProfile = sessionState.intakeProfile.mergePatch(updatedProfile.intakeProfile())
        val assistantReplyText = buildCuratedTripAssistantReply(starter)
        if (!isFirstTurn) {
            conversationItems += AiChatItem.TextMessage(
                text = assistantReplyText,
                sender = AiChatSender.ASSISTANT
            )
        }

        val updatedHistory = buildList {
            addAll(sessionState.llmHistory)
            add(LlmMessage(role = "user", content = llmUserMessage))
            if (!isFirstTurn) {
                add(LlmMessage(role = "assistant", content = assistantReplyText))
            }
        }

        sessionState = sessionState.copy(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            stage = AiTravelerProfileReducer.stageFor(updatedProfile),
            llmHistory = updatedHistory,
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            activeSingleEventCard = null,
            anchorMessageId = userMessage.id
        )

        persistLastSession()
        publishUiState()
        generateTitleIfNeeded()
    }

    fun selectDestinationRecommendation(recommendation: AiDestinationRecommendation) {
        val destination = recommendation.destination.trim()
        if (destination.isBlank()) return

        val isFirstTurn = conversationItems.isEmpty()
        val userMessage = AiChatItem.TextMessage(
            text = "Let's plan around $destination",
            sender = AiChatSender.USER
        )
        conversationItems += userMessage

        val llmUserMessage = buildDestinationRecommendationPrompt(recommendation)
        val updatedIntakeProfile = sessionState.intakeProfile.mergePatch(
            AiTripIntakeProfile(
                destination = destination,
                confidence = mapOf("destination" to 0.95)
            )
        )
        val updatedProfile = sessionState.profile.mergeIntakeProfile(updatedIntakeProfile)
        val assistantReply = buildDestinationRecommendationReply(recommendation)
        if (!isFirstTurn) {
            conversationItems += AiChatItem.TextMessage(
                text = assistantReply,
                sender = AiChatSender.ASSISTANT
            )
        }

        sessionState = sessionState.copy(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            planningObjective = if (recommendation.seedId != null) {
                "Pick a starter"
            } else {
                "Refine ${shortDestinationLabel(destination)}"
            },
            stage = AiTravelerProfileReducer.stageFor(updatedProfile),
            llmHistory = buildList {
                addAll(sessionState.llmHistory)
                add(LlmMessage(role = "user", content = llmUserMessage))
                if (!isFirstTurn) {
                    add(LlmMessage(role = "assistant", content = assistantReply))
                }
            },
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            activeSingleEventCard = null,
            draftText = "",
            selectedDraftOptions = emptyList(),
            anchorMessageId = userMessage.id
        )

        persistLastSession()
        publishUiState()
        generateTitleIfNeeded()
        launchHeroImageEnrichment(sessionState.activeCuratedTripRow?.id)
    }

    fun selectPlaceRecommendation(name: String, category: String, area: String) {
        val areaSuffix = area.takeIf { it.isNotBlank() }?.let { value -> " in $value" }.orEmpty()
        val visibleMessage = "Use $name"
        val llmUserMessage = "Place preference: include $name, a $category$areaSuffix, in the plan if it fits."
        submitUserTurn(
            visibleMessage = visibleMessage,
            llmUserMessage = llmUserMessage
        )
    }

    fun updateCuratedStarterDuration(
        starterId: String,
        durationDays: Int
    ) {
        val row = sessionState.activeCuratedTripRow ?: return
        val updatedTrips = row.trips.map { starter ->
            if (starter.id == starterId) {
                curatedTripCatalog.adjustStarterDuration(starter, durationDays)
            } else {
                starter
            }
        }
        if (updatedTrips == row.trips) return

        sessionState = sessionState.copy(
            activeCuratedTripRow = row.copy(trips = updatedTrips)
        )
        persistLastSession()
        publishUiState()
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
        if (trimmedText.isBlank() && selectedOptions.any { option -> option.requiresText }) return

        submitUserTurn(
            visibleMessage = buildVisibleUserMessage(selectedOptions, trimmedText),
            llmUserMessage = buildLlmUserMessage(selectedOptions, trimmedText)
        )
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
        launchHeroImageEnrichment(sessionState.activeCuratedTripRow?.id)
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

    fun requestAddSingleEventToTrip(suggestion: AiSingleEventSuggestion) {
        val viewerUid = currentUserId().orEmpty()
        if (viewerUid.isBlank()) {
            conversationItems += AiChatItem.TextMessage(
                text = "Sign in to add events to a trip.",
                sender = AiChatSender.SYSTEM
            )
            publishUiState()
            return
        }
        pendingAddToTripEvent = suggestion
        publishUiState()

        viewModelScope.launch {
            val trips = runCatching {
                tripSyncRemoteDataSource.fetchTripRefs(viewerUid)
            }.onFailure { error ->
                Log.w(TAG, "Failed to load trips for add-to-trip sheet: ${error.message}")
            }.getOrDefault(emptyList())

            availableTripsForAdd = trips
                .sortedByDescending { it.dateFrom }
                .map { itinerary ->
                    AiChatTripOption(
                        tripId = itinerary.itineraryId,
                        ownerUid = itinerary.ownerUid.ifBlank { viewerUid },
                        title = itinerary.tripName.ifBlank { itinerary.destination },
                        destination = itinerary.destination,
                        dateWindow = listOf(itinerary.dateFrom, itinerary.dateTo)
                            .filter { it.isNotBlank() }
                            .joinToString(" – "),
                        imageUrl = itinerary.homeImageUrl.takeIf { it.isNotBlank() }
                    )
                }
            publishUiState()
        }
    }

    fun dismissAddToTripSheet() {
        if (pendingAddToTripEvent == null) return
        pendingAddToTripEvent = null
        availableTripsForAdd = emptyList()
        publishUiState()
    }

    fun confirmAddSingleEventToTrip(tripOption: AiChatTripOption) {
        val suggestion = pendingAddToTripEvent ?: return
        val tripKey = TripKey(ownerUid = tripOption.ownerUid, tripId = tripOption.tripId)
        val eventToPersist = suggestion.event.copy(itineraryId = tripOption.tripId)

        pendingAddToTripEvent = null
        availableTripsForAdd = emptyList()
        publishUiState()

        viewModelScope.launch {
            val result = runCatching {
                tripSyncRemoteDataSource.upsertEvent(tripKey, eventToPersist)
            }
            val confirmationText = result.fold(
                onSuccess = {
                    "Added \"${suggestion.headline}\" to ${tripOption.title}."
                },
                onFailure = { error ->
                    Log.w(TAG, "upsertEvent failed: ${error.message}")
                    "I could not save that event to ${tripOption.title}. Try again in a moment."
                }
            )
            conversationItems += AiChatItem.TextMessage(
                text = confirmationText,
                sender = AiChatSender.ASSISTANT
            )
            if (result.isSuccess && sessionState.activeSingleEventCard?.id == suggestion.id) {
                sessionState = sessionState.copy(activeSingleEventCard = null)
            }
            persistLastSession()
            publishUiState()
        }
    }

    fun dismissSingleEventCard(id: String) {
        if (sessionState.activeSingleEventCard?.id != id) return
        sessionState = sessionState.copy(activeSingleEventCard = null)
        persistLastSession()
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
        val selectedOtherOption = sessionState.selectedDraftOptions.lastOrNull { option -> option.requiresText }
        val composerHint = if (selectedOtherOption != null) {
            sessionState.activeResponseCardGroup
                ?.takeIf { group -> group.id == selectedOtherOption.groupId }
                ?.otherPromptHint
                .orEmpty()
                .ifBlank { "Type the answer that fits best." }
        } else {
            ""
        }

        _uiState.value = AiChatUiState(
            items = buildVisibleItems(),
            starterCards = sessionState.starterCards,
            draftText = sessionState.draftText,
            selectedDraftOptions = sessionState.selectedDraftOptions,
            composerHint = composerHint,
            requiresTypedDraft = selectedOtherOption != null,
            historyEntries = sessionStore.historyEntries(currentUserId()),
            anchorMessageId = sessionState.anchorMessageId,
            activeResponseCardGroupId = sessionState.activeResponseCardGroup?.id,
            activeDestinationRecommendationRowId = sessionState.activeDestinationRecommendationRow?.id,
            activeCuratedTripRowId = sessionState.activeCuratedTripRow?.id,
            activePlaceRecommendationRowId = sessionState.activePlaceRecommendationRow?.id,
            activeSingleEventCardId = sessionState.activeSingleEventCard?.id,
            pendingAddToTripEvent = pendingAddToTripEvent,
            availableTrips = availableTripsForAdd,
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
            sessionState.activeDestinationRecommendationRow?.let { row ->
                add(
                    AiChatItem.DestinationRecommendationRow(
                        id = row.id,
                        row = row
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
            sessionState.activePlaceRecommendationRow?.let { row ->
                add(
                    AiChatItem.PlaceRecommendationRow(
                        id = row.id,
                        row = row
                    )
                )
            }
            sessionState.activeSingleEventCard?.let { card ->
                add(
                    AiChatItem.SingleEventCard(
                        id = card.id,
                        card = card
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
        launchHeroImageEnrichment(sessionState.activeCuratedTripRow?.id)
    }

    private fun restoreSnapshot(snapshot: PersistedAiChatSnapshot) {
        conversationItems.clear()
        conversationItems += snapshot.messages.map { message ->
            AiChatItem.TextMessage(
                text = message.text,
                sender = message.sender,
                tags = message.tags
            )
        }
        sessionState = AiChatSessionState(
            sessionId = snapshot.sessionId,
            title = snapshot.title,
            profile = snapshot.profile,
            intakeProfile = snapshot.intakeProfile ?: snapshot.profile.intakeProfile(),
            planningObjective = snapshot.planningObjective,
            stage = snapshot.stage,
            llmHistory = snapshot.llmHistory,
            starterCards = AiChatCardCatalog.starterCards(snapshot.sessionId),
            askedFollowUpGroupIds = snapshot.askedFollowUpGroupIds,
            activeResponseCardGroup = snapshot.activeResponseCardGroup?.toModel(),
            activeDestinationRecommendationRow = snapshot.activeDestinationRecommendationRow?.toModel(),
            activeCuratedTripRow = snapshot.activeCuratedTripRow?.toModel(),
            activePlaceRecommendationRow = snapshot.activePlaceRecommendationRow?.toModel(),
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
        populateQuickIdeaSurfaces()
    }

    private fun populateQuickIdeaSurfaces() {
        val targetSessionId = sessionState.sessionId ?: return
        viewModelScope.launch {
            val emptyProfile = AiTravelerProfile()
            val destinationRow = curatedTripCatalog.recommendDestinationRecommendations(emptyProfile)
            if (sessionState.sessionId != targetSessionId) return@launch
            if (sessionState.profile.hasSignals) return@launch
            if (conversationItems.isNotEmpty()) return@launch

            sessionState = sessionState.copy(
                activeDestinationRecommendationRow = destinationRow,
                activeCuratedTripRow = null
            )
            publishUiState()
        }
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
                    sender = message.sender,
                    tags = message.tags
                )
            },
            profile = sessionState.profile,
            intakeProfile = sessionState.intakeProfile,
            planningObjective = sessionState.planningObjective,
            stage = sessionState.stage,
            llmHistory = sessionState.llmHistory,
            askedFollowUpGroupIds = sessionState.askedFollowUpGroupIds,
            activeResponseCardGroup = sessionState.activeResponseCardGroup?.toPersisted(),
            activeDestinationRecommendationRow = sessionState.activeDestinationRecommendationRow?.toPersisted(),
            activeCuratedTripRow = sessionState.activeCuratedTripRow?.toPersisted(),
            activePlaceRecommendationRow = sessionState.activePlaceRecommendationRow?.toPersisted(),
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
        if (selectedOptions.isNotEmpty() && selectedOptions.all { option -> option.groupId == STARTER_CARD_GROUP_ID }) {
            val selectionLine = selectedOptions.joinToString(separator = " & ") { option ->
                "(${option.label})"
            }
            return listOf(selectionLine, typedText)
                .filter { value -> value.isNotBlank() }
                .joinToString(separator = "\n")
                .trim()
        }

        val selectionLine = selectedOptions
            .filterNot { option -> option.requiresText && typedText.isNotBlank() }
            .joinToString(separator = "  •  ") { option ->
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
        if (selectedOptions.isNotEmpty() && selectedOptions.all { option -> option.groupId == STARTER_CARD_GROUP_ID }) {
            return buildVisibleUserMessage(selectedOptions, typedText)
        }

        val standardOptions = selectedOptions.filterNot { option -> option.requiresText }
        val hasTypedOther = selectedOptions.any { option -> option.requiresText } && typedText.isNotBlank()

        return buildList {
            if (standardOptions.isNotEmpty()) {
                add("Selected choices:")
                addAll(standardOptions.map { option -> "- ${option.message}" })
            }
            if (hasTypedOther) {
                add("Custom answer: $typedText")
            } else if (typedText.isNotBlank()) {
                add("Extra context: $typedText")
            }
        }.joinToString(separator = "\n").trim()
    }

    private fun buildDestinationRecommendationPrompt(
        recommendation: AiDestinationRecommendation
    ): String {
        return buildString {
            append("Destination choice: use ")
            append(recommendation.destination)
            append(" as the planning destination.")
            if (recommendation.matchReason.isNotBlank()) {
                append(" Match reason: ")
                append(recommendation.matchReason)
                append('.')
            }
        }
    }

    private fun buildDestinationRecommendationReply(
        recommendation: AiDestinationRecommendation
    ): String {
        val destinationLabel = shortDestinationLabel(recommendation.destination)
        return if (recommendation.seedId != null) {
            "I can anchor the plan on $destinationLabel. Here are curated starter ideas to shape first."
        } else {
            "I can anchor the trip on $destinationLabel. Tell me what you want it to lean into and I will build from there."
        }
    }

    private fun shortDestinationLabel(destination: String): String {
        return destination.substringBefore(",").trim().ifBlank { destination }
    }

    private fun buildCuratedTripSelectionMessage(starter: AiCuratedTripStarter): String {
        return when (starter.source) {
            AiCuratedTripSource.FIRESTORE -> "Use ${starter.title} as the starter"
            AiCuratedTripSource.SEEDED -> "Use the curated ${starter.title} starter"
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

            AiCuratedTripSource.SEEDED ->
                "Use curated destination starter ${starter.title} for ${starter.destination}. This starter is editable and should act as the planning base."

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

            AiCuratedTripSource.SEEDED ->
                "I can use that curated ${starter.destination} starter as the base. Tell me what you want to refine next."

            AiCuratedTripSource.GENERATED ->
                "I can build that fresh ${starter.destination} starter from scratch. Tell me what you want to refine next."
        }
    }

    private fun buildLlmMessages(
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile,
        history: List<LlmMessage>,
        planningObjective: String,
        groundingContext: String? = null
    ): List<LlmMessage> {
        return buildList {
            add(LlmMessage(role = "system", content = BASE_SYSTEM_PROMPT))
            add(LlmMessage(role = "system", content = profile.promptSummary()))
            add(LlmMessage(role = "system", content = "Structured intake profile JSON:\n${intakeProfile.toJson()}"))
            if (planningObjective.isNotBlank()) {
                add(LlmMessage(role = "system", content = "Current planning objective: $planningObjective"))
            }
            groundingContext?.takeIf { context -> context.isNotBlank() }?.let { context ->
                add(LlmMessage(role = "system", content = context))
            }
            addAll(history)
        }
    }

    private suspend fun fallbackAssistantMessage(
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile,
        history: List<LlmMessage>,
        planningObjective: String,
        groundingContext: String? = null
    ): String {
        return runCatching {
            LlmClient.complete(
                messages = buildLlmMessages(
                    profile = profile,
                    intakeProfile = intakeProfile,
                    history = history,
                    planningObjective = planningObjective,
                    groundingContext = groundingContext
                )
            )
        }.getOrElse { error ->
            val errorMessage = error.localizedMessage
                ?: "Check your AI provider settings and network connection."
            "I hit a setup issue: $errorMessage"
        }.ifBlank {
            "That helps. I can narrow it down further with one more choice."
        }
    }

    private fun defaultAssistantAcknowledgement(): String = "Got it!"

    private fun submitUserTurn(
        visibleMessage: String,
        llmUserMessage: String
    ) {
        val trimmedVisibleMessage = visibleMessage.trim()
        val trimmedLlmUserMessage = llmUserMessage.trim()
        if (trimmedVisibleMessage.isBlank() || trimmedLlmUserMessage.isBlank()) return

        val submittedMessage = AiChatItem.TextMessage(
            text = trimmedVisibleMessage,
            sender = AiChatSender.USER
        )
        conversationItems += submittedMessage

        val updatedProfile = AiTravelerProfileReducer.merge(sessionState.profile, trimmedLlmUserMessage)
        val updatedIntakeProfile = sessionState.intakeProfile.mergePatch(updatedProfile.intakeProfile())
        sessionState = sessionState.copy(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            stage = AiTravelerProfileReducer.stageFor(updatedProfile),
            llmHistory = sessionState.llmHistory + LlmMessage(role = "user", content = trimmedLlmUserMessage),
            draftText = "",
            selectedDraftOptions = emptyList(),
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            activeSingleEventCard = null,
            anchorMessageId = submittedMessage.id
        )
        isLoading = true
        persistLastSession()
        publishUiState()

        viewModelScope.launch {
            val intakeResult = runCatching {
                intakeOrchestrator.analyzeTurn(
                    currentProfile = sessionState.intakeProfile,
                    latestUserInput = trimmedLlmUserMessage,
                    history = sessionState.llmHistory.dropLast(1),
                    askedQuestionIds = sessionState.askedFollowUpGroupIds,
                    planningObjective = sessionState.planningObjective
                )
            }.onFailure { error ->
                Log.w(TAG, "Structured intake analysis failed.", error)
            }.getOrNull()

            val initialMergedIntakeProfile = sessionState.intakeProfile.mergePatch(intakeResult?.profilePatch)
            val enrichedIntakeResult = if (
                intakeResult?.nextAction == AiTripIntakeNextAction.SUGGEST_DESTINATIONS &&
                initialMergedIntakeProfile.destination.isBlank()
            ) {
                val recommendations = runCatching {
                    intakeOrchestrator.suggestDestinations(
                        currentProfile = initialMergedIntakeProfile,
                        latestUserInput = trimmedLlmUserMessage,
                        history = sessionState.llmHistory
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Structured destination suggestion call failed.", error)
                }.getOrDefault(emptyList())

                if (recommendations.isNotEmpty()) {
                    intakeResult.withDestinationRecommendations(recommendations)
                } else {
                    intakeResult
                }
            } else {
                intakeResult
            }

            val mergedIntakeProfile = sessionState.intakeProfile.mergePatch(enrichedIntakeResult?.profilePatch)
            val mergedProfile = sessionState.profile.mergeIntakeProfile(mergedIntakeProfile)
            val assistantPlanningObjective = (enrichedIntakeResult?.planningObjective ?: "")
                .ifBlank { sessionState.planningObjective }
            val toolRouterResult = runCatching {
                toolRouter.routeTools(
                    currentProfile = mergedIntakeProfile,
                    latestUserInput = trimmedLlmUserMessage,
                    history = sessionState.llmHistory.dropLast(1),
                    planningObjective = assistantPlanningObjective
                )
            }.onFailure { error ->
                Log.w(TAG, "Tool routing failed. Continuing without routed tool calls.", error)
            }.getOrNull()
            val toolDispatch = runCatching {
                dispatchToolCalls(
                    toolRouterResult = toolRouterResult,
                    userMessage = trimmedLlmUserMessage,
                    intakeProfile = mergedIntakeProfile,
                    profile = mergedProfile
                )
            }.onFailure { error ->
                Log.w(TAG, "Tool dispatch failed. Continuing without live tool results.", error)
            }.getOrDefault(AiToolDispatchResult())
            val assistantResponse = buildAssistantResponse(
                intakeResult = enrichedIntakeResult,
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile,
                planningObjective = assistantPlanningObjective,
                history = sessionState.llmHistory,
                ticketmasterGrounding = toolDispatch.singleEventResolution?.groundingContext,
                viabilityWarning = toolDispatch.viabilityWarning,
                placeRecommendationRow = toolDispatch.placeRecommendationRow
            )

            conversationItems += AiChatItem.TextMessage(
                text = assistantResponse,
                sender = AiChatSender.ASSISTANT
            )

            val followUpGroup = resolveFollowUpGroup(
                intakeResult = enrichedIntakeResult,
                askedGroupIds = sessionState.askedFollowUpGroupIds
            )
            val destinationRecommendationRow = resolveDestinationRecommendationRow(
                intakeResult = enrichedIntakeResult,
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile
            )
            val isReadyForCurated = sessionState.llmHistory.size >= 4
            val curatedTripRow = when (enrichedIntakeResult?.nextAction) {
                AiTripIntakeNextAction.BUILD_TRIP -> {
                    if (isReadyForCurated) curatedTripCatalog.recommendTrips(
                        profile = mergedProfile,
                        viewerUid = currentUserId()
                    ) else null
                }

                AiTripIntakeNextAction.ASK_MORE,
                AiTripIntakeNextAction.SUGGEST_DESTINATIONS,
                null -> {
                    null
                }
            }
            val placeRecommendationRow = toolDispatch.placeRecommendationRow
            val singleEventCard = toolDispatch.singleEventResolution?.suggestion

            sessionState = sessionState.copy(
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile,
                planningObjective = assistantPlanningObjective,
                stage = AiTravelerProfileReducer.stageFor(mergedProfile),
                llmHistory = sessionState.llmHistory + LlmMessage(
                    role = "assistant",
                    content = assistantResponse
                ),
                askedFollowUpGroupIds = recordAskedFollowUpGroupId(
                    existingIds = sessionState.askedFollowUpGroupIds,
                    group = followUpGroup
                ),
                activeResponseCardGroup = followUpGroup,
                activeDestinationRecommendationRow = destinationRecommendationRow,
                activeCuratedTripRow = curatedTripRow,
                activePlaceRecommendationRow = placeRecommendationRow,
                activeSingleEventCard = singleEventCard
            )
            isLoading = false
            persistLastSession()
            publishUiState()
            generateTitleIfNeeded()
            launchHeroImageEnrichment(curatedTripRow?.id, destinationRecommendationRow?.id)
        }
    }

    private suspend fun buildAssistantResponse(
        intakeResult: AiTripIntakeTurnResult?,
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile,
        planningObjective: String,
        history: List<LlmMessage>,
        ticketmasterGrounding: String?,
        viabilityWarning: String,
        placeRecommendationRow: AiPlaceRecommendationRow?
    ): String {
        val baseResponse = if (!ticketmasterGrounding.isNullOrBlank()) {
            fallbackAssistantMessage(
                profile = profile,
                intakeProfile = intakeProfile,
                history = history,
                planningObjective = planningObjective,
                groundingContext = ticketmasterGrounding
            )
        } else {
            intakeResult?.assistantMessage
                ?.takeIf { message -> message.isNotBlank() }
                ?: defaultAssistantAcknowledgement()
        }

        return listOfNotNull(
            viabilityWarning.takeIf { warning -> warning.isNotBlank() },
            baseResponse.takeIf { response -> response.isNotBlank() },
            routedPlaceSummary(placeRecommendationRow)
                ?.takeUnless { summary -> ticketmasterGrounding.isNullOrBlank().not() }
        ).joinToString(separator = "\n\n")
    }

    private suspend fun dispatchToolCalls(
        toolRouterResult: AiToolRouterResult?,
        userMessage: String,
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): AiToolDispatchResult {
        if (toolRouterResult == null) return AiToolDispatchResult()

        val eventCall = toolRouterResult.toolCalls
            .filterIsInstance<AiToolCall.SearchEvents>()
            .firstOrNull()
        val placeCall = toolRouterResult.toolCalls.firstOrNull { toolCall ->
            toolCall is AiToolCall.SearchRestaurants ||
                toolCall is AiToolCall.SearchActivities ||
                toolCall is AiToolCall.SearchHotels
        }

        val singleEventResolution = eventCall?.let { toolCall ->
            singleEventCoordinator.resolve(
                toolCall = toolCall,
                userMessage = userMessage,
                intakeProfile = intakeProfile,
                profile = profile
            )
        }
        val placeRecommendationRow = placeCall?.let { toolCall ->
            placeRecommendationCoordinator.recommendRowForToolCall(
                toolCall = toolCall,
                intakeProfile = intakeProfile,
                profile = profile
            )
        }

        return AiToolDispatchResult(
            placeRecommendationRow = placeRecommendationRow,
            singleEventResolution = singleEventResolution,
            viabilityWarning = toolRouterResult.viabilityWarning
        )
    }

    private fun routedPlaceSummary(
        placeRecommendationRow: AiPlaceRecommendationRow?
    ): String? {
        return when (placeRecommendationRow?.rowType) {
            AiPlaceRecommendationRowType.RESTAURANTS -> "I pulled a few live restaurant options below."
            AiPlaceRecommendationRowType.ACTIVITIES -> "I pulled a few live activity options below."
            AiPlaceRecommendationRowType.HOTELS -> "I pulled a few live hotel options below."
            else -> null
        }
    }

    private fun resolveFollowUpGroup(
        intakeResult: AiTripIntakeTurnResult?,
        askedGroupIds: List<String>
    ): AiChatCardGroup? {
        val followUpQuestion = intakeResult
            ?.takeIf { result -> result.nextAction == AiTripIntakeNextAction.ASK_MORE }
            ?.followUpQuestion
            ?: return null

        val intakeGroup = followUpQuestion
            .toCardGroup()
            ?.takeUnless { group -> group.id in askedGroupIds }

        if (intakeGroup != null) {
            Log.d(TAG, "Using intake-generated follow-up '${intakeGroup.id}'.")
            return intakeGroup
        }

        Log.w(TAG, "Discarded intake follow-up '${followUpQuestion.id}'.")
        return null
    }

    private fun resolveDestinationRecommendationRow(
        intakeResult: AiTripIntakeTurnResult?,
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile
    ): AiDestinationRecommendationRow? {
        if (profile.destination.isNotBlank() || intakeProfile.destination.isNotBlank()) return null

        val intakeRow = intakeResult?.toDestinationRecommendationRow()
        if (intakeRow != null) return intakeRow

        return when (intakeResult?.nextAction) {
            AiTripIntakeNextAction.SUGGEST_DESTINATIONS,
            null -> AiRecommendationMapper.destinationRowFromEngine(
                destinationRecommendationEngine.rankDestinations(intakeProfile)
            )

            AiTripIntakeNextAction.ASK_MORE,
            AiTripIntakeNextAction.BUILD_TRIP -> null
        }
    }

    private fun recordAskedFollowUpGroupId(
        existingIds: List<String>,
        group: AiChatCardGroup?
    ): List<String> {
        val groupId = group?.id ?: return existingIds
        return (existingIds + groupId).distinct()
    }

    private fun launchHeroImageEnrichment(curatedRowId: String?, destinationRowId: String? = null) {
        if (curatedRowId != null) {
            viewModelScope.launch {
                val targetRow = sessionState.activeCuratedTripRow ?: return@launch
                if (targetRow.id != curatedRowId) return@launch

                val needsLookup = targetRow.trips.filter { starter ->
                    starter.heroImageUrl.isNullOrBlank()
                }
                if (needsLookup.isEmpty()) return@launch

                val resolved = mutableMapOf<String, String>()
                needsLookup.map { it.destination }
                    .distinct()
                    .forEach { destination ->
                        val key = heroImageCacheKey(destination)
                        val cached = heroImageCache[key]
                        if (cached != null) {
                            resolved[destination] = cached
                            return@forEach
                        }
                        val resolvedUrl = runCatching {
                            destinationImages.resolveDestinationImage(destination).imageUrl
                        }.onFailure { error ->
                            Log.w(TAG, "Hero image lookup failed for '$destination': ${error.message}")
                        }.getOrNull()
                        if (!resolvedUrl.isNullOrBlank()) {
                            heroImageCache[key] = resolvedUrl
                            resolved[destination] = resolvedUrl
                        }
                    }

                if (resolved.isEmpty()) return@launch

                val current = sessionState.activeCuratedTripRow ?: return@launch
                if (current.id != curatedRowId) return@launch

                val updatedTrips = current.trips.map { starter ->
                    if (!starter.heroImageUrl.isNullOrBlank()) starter
                    else resolved[starter.destination]?.let { url -> starter.copy(heroImageUrl = url) }
                        ?: starter
                }
                if (updatedTrips == current.trips) return@launch

                sessionState = sessionState.copy(activeCuratedTripRow = current.copy(trips = updatedTrips))
                persistLastSession()
                publishUiState()
            }
        }

        if (destinationRowId != null) {
            viewModelScope.launch {
                val targetRow = sessionState.activeDestinationRecommendationRow ?: return@launch
                if (targetRow.id != destinationRowId) return@launch

                val needsLookup = targetRow.recommendations.filter { rec ->
                    rec.imageUrl.isNullOrBlank()
                }
                if (needsLookup.isEmpty()) return@launch

                val resolved = mutableMapOf<String, String>()
                needsLookup.map { it.destination }
                    .distinct()
                    .forEach { destination ->
                        val key = heroImageCacheKey(destination)
                        val cached = heroImageCache[key]
                        if (cached != null) {
                            resolved[destination] = cached
                            return@forEach
                        }
                        val resolvedUrl = runCatching {
                            destinationImages.resolveDestinationImage(destination).imageUrl
                        }.onFailure { error ->
                            Log.w(TAG, "Destination image lookup failed for '$destination': ${error.message}")
                        }.getOrNull()
                        if (!resolvedUrl.isNullOrBlank()) {
                            heroImageCache[key] = resolvedUrl
                            resolved[destination] = resolvedUrl
                        }
                    }

                if (resolved.isEmpty()) return@launch

                val current = sessionState.activeDestinationRecommendationRow ?: return@launch
                if (current.id != destinationRowId) return@launch

                val updatedRecs = current.recommendations.map { rec ->
                    if (!rec.imageUrl.isNullOrBlank()) rec
                    else resolved[rec.destination]?.let { url -> rec.copy(imageUrl = url) }
                        ?: rec
                }
                if (updatedRecs == current.recommendations) return@launch

                sessionState = sessionState.copy(activeDestinationRecommendationRow = current.copy(recommendations = updatedRecs))
                persistLastSession()
                publishUiState()
            }
        }
    }

    private fun heroImageCacheKey(destination: String): String {
        return destination.substringBefore(",").trim().lowercase(Locale.US)
    }

    private companion object {
        private const val STARTER_CARD_GROUP_ID = "starter_grid"
        private const val TAG = "AiChatViewModel"
        private const val BASE_SYSTEM_PROMPT =
            "You are TravelCents AI, a trip-planning copilot inside the TravelCents app. " +
                "Be concise, helpful, and practical. Keep replies to short acknowledgment paragraphs. " +
                "The app may present follow-up choices separately, so do not stack multiple questions or long questionnaires. " +
                "Use the traveler profile context when available. Do not mention model vendors or say you are a generic AI chatbot."
        private const val WIKIMEDIA_CONTACT_URL = "https://github.com/bit-lords-csulb/Travel-Cents"
        private val WIKIMEDIA_USER_AGENT =
            "TravelCents/${BuildConfig.VERSION_NAME} (Android app; $WIKIMEDIA_CONTACT_URL)"

        private var processSnapshot: PersistedAiChatSnapshot? = null
    }
}

private data class AiToolDispatchResult(
    val placeRecommendationRow: AiPlaceRecommendationRow? = null,
    val singleEventResolution: AiSingleEventResolution? = null,
    val viabilityWarning: String = ""
)
