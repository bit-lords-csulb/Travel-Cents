package com.example.travelcents.ui.main.aichat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ai.chat.AiChatCardCatalog
import com.example.travelcents.data.ai.chat.AiChatCardGroup
import com.example.travelcents.data.ai.chat.AiChatCardOption
import com.example.travelcents.data.ai.chat.AiCuratedTripCatalog
import com.example.travelcents.data.ai.chat.AiCuratedTripRow
import com.example.travelcents.data.ai.chat.AiCuratedTripSeedCatalog
import com.example.travelcents.data.ai.chat.AiRecommendationMapper
import com.example.travelcents.data.ai.chat.AiDestinationRecommendation
import com.example.travelcents.data.ai.chat.AiCuratedTripSource
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.AiChatSender
import com.example.travelcents.data.ai.chat.AiChatSessionState
import com.example.travelcents.data.ai.chat.AiChatSessionStore
import com.example.travelcents.data.ai.chat.AiChatUiState
import com.example.travelcents.data.ai.chat.AiChatPreviewDraft
import com.example.travelcents.data.ai.chat.AiChatPreviewDraftType
import com.example.travelcents.data.ai.chat.AiTravelerProfile
import com.example.travelcents.data.ai.chat.AiTravelerProfileReducer
import com.example.travelcents.data.ai.chat.AiDestinationRecommendationRow
import com.example.travelcents.data.ai.chat.AiChatTripOption
import com.example.travelcents.data.ai.chat.AskedQuestionRecord
import com.example.travelcents.data.ai.chat.DiscoverySlot
import com.example.travelcents.data.ai.chat.DiscoverySlotStatus
import com.example.travelcents.data.ai.chat.DiscoveryTrack
import com.example.travelcents.data.ai.chat.AiPlaceRecommendationRow
import com.example.travelcents.data.ai.chat.AiPlaceRecommendationRowType
import com.example.travelcents.data.ai.chat.AiPlaceRecommendationCoordinator
import com.example.travelcents.data.ai.chat.AiSingleEventCoordinator
import com.example.travelcents.data.ai.chat.AiSingleEventResolution
import com.example.travelcents.data.ai.chat.AiSingleEventSuggestion
import com.example.travelcents.data.ai.chat.AiToolCall
import com.example.travelcents.data.ai.chat.AiToolRouterOrchestrator
import com.example.travelcents.data.ai.chat.AiToolRouterResult
import com.example.travelcents.data.ai.chat.AiTripIntakeNextAction
import com.example.travelcents.data.ai.chat.AiTripIntakeDestinationRecommendation
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.ai.chat.AiTripIntakeOrchestrator
import com.example.travelcents.data.ai.chat.AiTripIntakeTurnResult
import com.example.travelcents.data.ai.chat.MealType
import com.example.travelcents.data.ai.chat.PlannerContext
import com.example.travelcents.data.ai.chat.PlannerPhase
import com.example.travelcents.data.ai.chat.PlannerQuestionSource
import com.example.travelcents.data.ai.chat.PreferenceProfile
import com.example.travelcents.data.ai.chat.PersistedAiChatMessage
import com.example.travelcents.data.ai.chat.PersistedAiChatSnapshot
import com.example.travelcents.data.ai.chat.SuggestionItem
import com.example.travelcents.data.ai.chat.cardIdToIntakeProfilePatch
import com.example.travelcents.data.ai.chat.mergeIntakeProfile
import com.example.travelcents.data.ai.chat.mergePatch
import com.example.travelcents.data.ai.chat.isReadyForDestinationRecommendations
import com.example.travelcents.data.ai.chat.nextBestAllowedTopicPath
import com.example.travelcents.data.ai.chat.recordAnsweredPlannerQuestion
import com.example.travelcents.data.ai.chat.recordAskedPlannerQuestion
import com.example.travelcents.data.ai.chat.allowedTopicPathSummary
import com.example.travelcents.data.ai.chat.askedTopicSummary
import com.example.travelcents.data.ai.chat.repairPrompt
import com.example.travelcents.data.ai.chat.shouldForceVisualAction
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.chat.toDestinationRecommendationRow
import com.example.travelcents.data.ai.chat.toPlannerTopicPath
import com.example.travelcents.data.ai.chat.withDestinationRecommendations
import com.example.travelcents.data.ai.chat.validatePlannerTurn
import com.example.travelcents.data.ai.chat.toModel
import com.example.travelcents.data.ai.chat.toPersisted
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.media.UnsplashImageRepository
import com.example.travelcents.data.media.UnsplashSearchParams
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.social.model.BookmarkedPlace
import com.example.travelcents.data.social.repository.BookmarksRepository
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpOptionPoolItem
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.data.user.UserProfileRepository
import com.example.travelcents.data.user.model.CurrentUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val conversationItems = mutableListOf<AiChatItem.TextMessage>()
    private val sessionStore = AiChatSessionStore(application)
    private val curatedTripCatalog = AiCuratedTripCatalog()
    private val placeRecommendationCoordinator = AiPlaceRecommendationCoordinator(curatedTripCatalog = curatedTripCatalog)
    private val singleEventCoordinator = AiSingleEventCoordinator()
    private val intakeOrchestrator = AiTripIntakeOrchestrator()
    private val toolRouter = AiToolRouterOrchestrator()
    private val auth = FirebaseAuth.getInstance()
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(FirebaseFirestore.getInstance())
    private val bookmarksRepository = BookmarksRepository()
    private val userProfileRepository = UserProfileRepository()

    private val _bookmarkedPlaceIds = MutableStateFlow<Set<String>>(emptySet())
    private val _currentUserProfile = MutableStateFlow(CurrentUserProfile(firstName = "User", isLoading = true))

    private var pendingAddToTripEvent: AiSingleEventSuggestion? = null
    private var availableTripsForAdd: List<AiChatTripOption> = emptyList()
    private val destinationImages = UnsplashImageRepository()
    private val heroImageCache = mutableMapOf<String, String>()
    private var processSnapshot: PersistedAiChatSnapshot? = null

    private var sessionState = AiChatSessionState()
    private var isLoading = false

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        restoreLastSessionOrStartFresh()
        observeCurrentUserProfile()
        observeBookmarks()
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

    fun submitPreferenceQuestionAnswer(
        card: AiChatItem.PreferenceQuestionCard,
        answers: List<String>
    ) {
        answerPreferenceQuestion(card.id, answers)
    }

    fun startDiscoveryIfEligible() {
        if (!startDiscoveryIfEligibleInternal()) return
        persistLastSession()
        publishUiState()
    }

    fun answerPreferenceQuestion(
        cardId: String,
        answers: List<String>
    ) {
        val activeCard = sessionState.activePreferenceQuestionCard ?: return
        if (isLoading || activeCard.id != cardId || activeCard.answered) return
        val selectedAnswers = answers
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        if (selectedAnswers.isEmpty()) return

        val answerText = selectedAnswers.joinToString(", ")
        val answeredCard = activeCard.copy(
            answered = true,
            answerSummary = "Cuisine picks: $answerText"
        )
        conversationItems += AiChatItem.TextMessage(
            text = answerText,
            sender = AiChatSender.USER,
            tags = selectedAnswers
        )
        sessionState = sessionState.copy(
            preferenceProfile = sessionState.preferenceProfile.copy(cuisineTypes = selectedAnswers),
            discoverySlots = upsertFoodDiscoverySlot(
                status = DiscoverySlotStatus.SEARCHING,
                shownSuggestionIds = emptySet(),
                exhausted = false
            ),
            activePreferenceQuestionCard = answeredCard,
            activeSuggestionCarouselCard = null,
            discoverySuggestionPool = emptyList(),
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            pendingPlaceRecommendationRow = null,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null,
            anchorMessageId = null
        )
        isLoading = true
        persistLastSession()
        publishUiState()

        viewModelScope.launch {
            val suggestions = runCatching {
                buildFoodSuggestions(selectedAnswers)
            }.onFailure { error ->
                Log.w(TAG, "Food discovery search failed: ${error.message}", error)
            }.getOrDefault(emptyList())

            val visibleSuggestions = visibleFoodSuggestions(pool = suggestions)
            sessionState = sessionState.copy(
                discoverySuggestionPool = suggestions,
                discoverySlots = upsertFoodDiscoverySlot(
                    status = DiscoverySlotStatus.SHOWING_RESULTS,
                    shownSuggestionIds = visibleSuggestions.mapTo(mutableSetOf(), SuggestionItem::id),
                    exhausted = visibleSuggestions.isEmpty()
                ),
                activeSuggestionCarouselCard = foodSuggestionCarouselCard(visibleSuggestions)
            )
            isLoading = false
            persistLastSession()
            publishUiState()
        }
    }

    fun noteSuggestionAddedToPreview(suggestion: SuggestionItem) {
        markDiscoverySuggestion(suggestion, added = true)
        updatePreviewDraftWithAddedEvent(suggestion.rawEvent)
        conversationItems += AiChatItem.TextMessage(
            text = "Added \"${suggestion.name}\" to the preview trip.",
            sender = AiChatSender.ASSISTANT
        )
        persistLastSession()
        publishUiState()
    }

    fun bookmarkSuggestion(suggestion: SuggestionItem) {
        val uid = currentUserId().orEmpty()
        if (uid.isBlank()) {
            conversationItems += AiChatItem.TextMessage(
                text = "Sign in to save places.",
                sender = AiChatSender.SYSTEM
            )
            publishUiState()
            return
        }

        val bookmark = BookmarkedPlace(
            id = suggestion.providerId.ifBlank { suggestion.id },
            name = suggestion.name,
            category = suggestion.rawEvent.type.ifBlank { suggestion.subtitle },
            area = suggestion.address,
            imageUrl = suggestion.imageUrl,
            yelpUrl = suggestion.detailUrl,
            savedAtEpochMs = System.currentTimeMillis()
        )

        markDiscoverySuggestion(suggestion, bookmarked = true)
        refreshActiveFoodSuggestionCarousel()
        persistLastSession()
        publishUiState()

        viewModelScope.launch {
            val result = runCatching {
                bookmarksRepository.addBookmark(uid, bookmark)
            }.onFailure { error ->
                Log.w(TAG, "Failed to bookmark AI suggestion: ${error.message}", error)
            }

            conversationItems += AiChatItem.TextMessage(
                text = if (result.isSuccess) {
                    "Saved \"${suggestion.name}\" to your bookmarks."
                } else {
                    "I could not save \"${suggestion.name}\" right now."
                },
                sender = AiChatSender.ASSISTANT
            )
            persistLastSession()
            publishUiState()
        }
    }

    fun skipSuggestion(suggestion: SuggestionItem) {
        if (isLoading) return
        val activeCard = sessionState.activeSuggestionCarouselCard
        if (activeCard != null && activeCard.slotId == suggestion.slotId) {
            markDiscoverySuggestion(suggestion, skipped = true)
            refreshActiveFoodSuggestionCarousel()
            conversationItems += AiChatItem.TextMessage(
                text = "Skipped \"${suggestion.name}\".",
                sender = AiChatSender.USER
            )
            persistLastSession()
            publishUiState()
            return
        }

        submitUserTurn(
            visibleMessage = "Skip ${suggestion.name}",
            llmUserMessage = "Skip this suggested option for ${suggestion.slotId}: ${suggestion.name}. Suggest a better alternative."
        )
    }

    fun requestMoreSuggestions(card: AiChatItem.SuggestionCarouselCard) {
        if (isLoading || !card.hasMore || card.exhausted) return
        val shownNames = card.suggestions.joinToString { suggestion -> suggestion.name }
        submitUserTurn(
            visibleMessage = "Show more options",
            llmUserMessage = "Show more options for ${card.label}. Avoid repeating these suggestions: $shownNames."
        )
    }

    fun requestAddSuggestionToTrip(suggestion: SuggestionItem) {
        val dateLine = listOf(suggestion.rawEvent.date, suggestion.rawEvent.startTime)
            .filter(String::isNotBlank)
            .joinToString(" ")
        requestAddSingleEventToTrip(
            AiSingleEventSuggestion(
                id = suggestion.id,
                headline = suggestion.name,
                venue = suggestion.address,
                cityLine = suggestion.address,
                dateLine = dateLine,
                priceLine = "",
                category = suggestion.subtitle.ifBlank { suggestion.rawEvent.type.ifBlank { "Suggestion" } },
                imageUrl = suggestion.imageUrl,
                bookingUrl = suggestion.detailUrl,
                source = suggestion.source,
                event = suggestion.rawEvent
            )
        )
    }

    private fun startDiscoveryIfEligibleInternal(): Boolean {
        if (sessionState.discoveryTrack != DiscoveryTrack.NOT_STARTED) return false
        if (!hasDiscoveryTiming()) return false
        if (discoveryDestination().isBlank()) return false

        conversationItems += AiChatItem.TextMessage(
            text = "Before I build the trip, let me tune the food picks.",
            sender = AiChatSender.ASSISTANT
        )
        sessionState = sessionState.copy(
            discoveryTrack = DiscoveryTrack.FOOD,
            discoverySlots = upsertFoodDiscoverySlot(
                status = DiscoverySlotStatus.PENDING_QUESTION,
                shownSuggestionIds = emptySet(),
                exhausted = false
            ),
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            pendingPlaceRecommendationRow = null,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null,
            activePreferenceQuestionCard = foodPreferenceQuestionCard(),
            activeSuggestionCarouselCard = null,
            discoverySuggestionPool = emptyList()
        )
        return true
    }

    private fun foodPreferenceQuestionCard(): AiChatItem.PreferenceQuestionCard {
        return AiChatItem.PreferenceQuestionCard(
            id = FOOD_PREFERENCE_CARD_ID,
            track = DiscoveryTrack.FOOD,
            questionKey = "food_cuisine_preferences",
            question = "What food should I prioritize?",
            options = FOOD_PREFERENCE_OPTIONS,
            multiSelect = true
        )
    }

    private suspend fun buildFoodSuggestions(cuisines: List<String>): List<SuggestionItem> {
        val destination = discoveryDestination()
        if (destination.isBlank()) return emptyList()

        val businesses = YelpRepository.fetchRestaurantPool(
            location = destination,
            targetCount = foodSuggestionPoolTarget()
        )
        val rankedBusinesses = rankFoodBusinesses(businesses, cuisines)
        val poolItems = YelpRepository.mapBusinessesToPoolItems(rankedBusinesses)
            .take(FOOD_SUGGESTION_POOL_MAX)

        return poolItems.mapIndexed { index, item ->
            item.toFoodSuggestion(index = index)
        }
    }

    private fun rankFoodBusinesses(
        businesses: List<YelpBusiness>,
        cuisines: List<String>
    ): List<YelpBusiness> {
        val cuisineTerms = cuisines
            .map { cuisine -> cuisine.trim().lowercase(Locale.US) }
            .filter(String::isNotBlank)
        if (cuisineTerms.isEmpty()) return businesses

        return businesses.sortedWith(
            compareByDescending<YelpBusiness> { business ->
                val haystack = buildString {
                    append(business.name.lowercase(Locale.US))
                    business.categories.forEach { category ->
                        append(' ')
                        append(category.title.lowercase(Locale.US))
                        append(' ')
                        append(category.alias.lowercase(Locale.US))
                    }
                }
                cuisineTerms.count { term -> term in haystack }
            }.thenByDescending { business -> business.rating }
        )
    }

    private fun YelpOptionPoolItem.toFoodSuggestion(index: Int): SuggestionItem {
        val timeWindow = FOOD_TIME_WINDOWS[index % FOOD_TIME_WINDOWS.size]
        val event = TravelEvent(
            eventId = UUID.randomUUID().toString(),
            type = "restaurant",
            itineraryId = "",
            date = discoveryStartDate(),
            startTime = timeWindow.first,
            endTime = timeWindow.second,
            imageUrl = imageUrl,
            selectedOptionId = providerId,
            details = toEventDetails()
        )
        val subtitleParts = buildList {
            categories.firstOrNull()?.takeIf(String::isNotBlank)?.let(::add)
            priceTier.takeIf(String::isNotBlank)?.let(::add)
            rating?.takeIf { it > 0.0 }?.let { add("${it.formatRating()} stars") }
        }
        return SuggestionItem(
            id = providerId,
            name = name,
            subtitle = subtitleParts.joinToString(" | ").ifBlank { "Restaurant" },
            imageUrl = imageUrl.takeIf(String::isNotBlank),
            address = shortAddress,
            detailUrl = yelpUrl.takeIf(String::isNotBlank),
            source = source,
            providerId = providerId,
            slotId = FOOD_DISCOVERY_SLOT_ID,
            rawEvent = event
        )
    }

    private fun upsertFoodDiscoverySlot(
        status: DiscoverySlotStatus,
        shownSuggestionIds: Set<String>,
        exhausted: Boolean
    ): List<DiscoverySlot> {
        val existingSlot = sessionState.discoverySlots.firstOrNull { slot -> slot.id == FOOD_DISCOVERY_SLOT_ID }
        val nextSlot = (existingSlot ?: DiscoverySlot(
            id = FOOD_DISCOVERY_SLOT_ID,
            track = DiscoveryTrack.FOOD,
            dayIndex = 0,
            date = discoveryStartDate(),
            mealType = MealType.DINNER,
            title = "Dinner ideas",
            pageSize = FOOD_VISIBLE_SUGGESTION_COUNT
        )).copy(
            status = status,
            shownSuggestionIds = existingSlot?.shownSuggestionIds.orEmpty() + shownSuggestionIds,
            exhausted = exhausted,
            pageSize = FOOD_VISIBLE_SUGGESTION_COUNT
        )
        return sessionState.discoverySlots
            .filterNot { slot -> slot.id == FOOD_DISCOVERY_SLOT_ID } + nextSlot
    }

    private fun foodSuggestionCarouselCard(
        suggestions: List<SuggestionItem>
    ): AiChatItem.SuggestionCarouselCard {
        return AiChatItem.SuggestionCarouselCard(
            id = FOOD_SUGGESTION_CARD_ID,
            slotId = FOOD_DISCOVERY_SLOT_ID,
            track = DiscoveryTrack.FOOD,
            label = "Food ideas for ${shortDiscoveryDestination()}",
            suggestions = suggestions,
            hasMore = false,
            exhausted = suggestions.isEmpty()
        )
    }

    private fun visibleFoodSuggestions(
        pool: List<SuggestionItem> = sessionState.discoverySuggestionPool
    ): List<SuggestionItem> {
        val foodSlot = sessionState.discoverySlots.firstOrNull { slot -> slot.id == FOOD_DISCOVERY_SLOT_ID }
        val hiddenSuggestionIds = foodSlot?.let { slot ->
            slot.skippedSuggestionIds + slot.bookmarkedSuggestionIds
        }.orEmpty()
        return pool
            .filterNot { suggestion -> suggestion.id in hiddenSuggestionIds }
            .take(FOOD_VISIBLE_SUGGESTION_COUNT)
    }

    private fun refreshActiveFoodSuggestionCarousel() {
        if (sessionState.discoveryTrack != DiscoveryTrack.FOOD) return
        val visibleSuggestions = visibleFoodSuggestions()
        sessionState = sessionState.copy(
            discoverySlots = upsertFoodDiscoverySlot(
                status = DiscoverySlotStatus.SHOWING_RESULTS,
                shownSuggestionIds = visibleSuggestions.mapTo(mutableSetOf(), SuggestionItem::id),
                exhausted = visibleSuggestions.isEmpty()
            ),
            activeSuggestionCarouselCard = foodSuggestionCarouselCard(visibleSuggestions)
        )
    }

    private fun markDiscoverySuggestion(
        suggestion: SuggestionItem,
        added: Boolean = false,
        bookmarked: Boolean = false,
        skipped: Boolean = false
    ) {
        sessionState = sessionState.copy(
            discoverySlots = sessionState.discoverySlots.map { slot ->
                if (slot.id != suggestion.slotId) {
                    slot
                } else {
                    slot.copy(
                        addedSuggestionIds = if (added) slot.addedSuggestionIds + suggestion.id else slot.addedSuggestionIds,
                        bookmarkedSuggestionIds = if (bookmarked) slot.bookmarkedSuggestionIds + suggestion.id else slot.bookmarkedSuggestionIds,
                        skippedSuggestionIds = if (skipped) slot.skippedSuggestionIds + suggestion.id else slot.skippedSuggestionIds
                    )
                }
            }
        )
    }

    private fun updatePreviewDraftWithAddedEvent(event: TravelEvent) {
        val previewDraft = sessionState.previewDraft ?: return
        sessionState = sessionState.copy(
            previewDraft = previewDraft.copy(
                addedEvents = previewDraft.addedEvents.upsertPreviewDraftEvent(event)
            )
        )
    }

    private fun List<TravelEvent>.upsertPreviewDraftEvent(event: TravelEvent): List<TravelEvent> {
        val incomingKey = event.previewDraftIdentityKey()
        return if (any { existing -> existing.previewDraftIdentityKey() == incomingKey }) {
            this
        } else {
            this + event
        }
    }

    private fun TravelEvent.previewDraftIdentityKey(): String {
        return detailValue(DETAIL_YELP_ID)?.takeIf(String::isNotBlank)
            ?: selectedOptionId.takeIf(String::isNotBlank)
            ?: eventId
    }

    private fun discoveryDestination(): String {
        return sessionState.intakeProfile.destination
            .ifBlank { sessionState.lockedDestination.orEmpty() }
            .ifBlank { sessionState.profile.destination }
            .trim()
    }

    private fun hasDiscoveryTiming(): Boolean {
        val intakeProfile = sessionState.intakeProfile
        val hasExactDates = intakeProfile.dateFrom.isNotBlank() && intakeProfile.dateTo.isNotBlank()
        val hasRelativeDates = intakeProfile.dateWindow.isNotBlank() && intakeProfile.durationDays != null
        return hasExactDates || hasRelativeDates
    }

    private fun foodSuggestionPoolTarget(): Int {
        val intakeProfile = sessionState.intakeProfile
        val dayCount = intakeProfile.durationDays
            ?: runCatching {
                val start = LocalDate.parse(intakeProfile.dateFrom)
                val end = LocalDate.parse(intakeProfile.dateTo)
                ChronoUnit.DAYS.between(start, end).toInt() + 1
            }.getOrDefault(1)
        return (dayCount + 4).coerceIn(FOOD_SUGGESTION_POOL_MIN, FOOD_SUGGESTION_POOL_MAX)
    }

    private fun discoveryStartDate(): String {
        return sessionState.intakeProfile.dateFrom.ifBlank {
            LocalDate.now().plusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    private fun shortDiscoveryDestination(): String {
        return discoveryDestination()
            .substringBefore(",")
            .trim()
            .ifBlank { "your trip" }
    }

    private fun Double.formatRating(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
    }

    fun handleRecommendedStarterSelection(
        starter: AiCuratedTripStarter,
        onOpenTrip: (TripKey) -> Unit,
        onStarterSelected: (AiCuratedTripStarter, AiTripIntakeProfile) -> Unit
    ) {
        selectCuratedTrip(starter)

        when (starter.source) {
            AiCuratedTripSource.FIRESTORE -> {
                starter.tripKey?.let(onOpenTrip)
            }

            AiCuratedTripSource.SEEDED,
            AiCuratedTripSource.GENERATED -> {
                onStarterSelected(starter, sessionState.intakeProfile)
                // Phase 1 v10: keep food discovery logic available, but stop auto-starting it.
                // if (startDiscoveryIfEligibleInternal()) {
                //     persistLastSession()
                //     publishUiState()
                // }
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
            pendingPlaceRecommendationRow = null,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null,
            lockedDestination = starter.destination,
            lockedDestinationImageUrl = starter.heroImageUrl,
            previewDraft = AiChatPreviewDraft(
                type = AiChatPreviewDraftType.CURATED_STARTER,
                destination = starter.destination,
                intakeProfile = updatedIntakeProfile,
                curatedStarter = starter
            ),
            anchorMessageId = userMessage.id
        )

        persistLastSession()
        publishUiState()
        generateTitleIfNeeded()
    }

    fun selectDestinationRecommendation(
        recommendation: AiDestinationRecommendation,
        onDestinationLocked: (AiDestinationRecommendation, AiTripIntakeProfile) -> Unit = { _, _ -> }
    ) {
        val destination = recommendation.destination.trim()
        if (destination.isBlank()) return

        sessionState = sessionState.copy(
            lockedDestination = destination,
            lockedDestinationImageUrl = recommendation.imageUrl,
            intakeProfile = sessionState.intakeProfile.copy(destination = destination),
            previewDraft = AiChatPreviewDraft(
                type = AiChatPreviewDraftType.DESTINATION_LOCK,
                destination = destination,
                intakeProfile = sessionState.intakeProfile.copy(destination = destination)
            )
        )
        publishUiState()

        onDestinationLocked(recommendation, sessionState.intakeProfile)

        submitUserTurn(
            visibleMessage = "Let's plan around $destination",
            llmUserMessage = buildDestinationRecommendationPrompt(recommendation)
        )
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

        // Dead-end action chips bypass the intake loop and call deterministic handlers.
        if (trimmedText.isBlank() &&
            selectedOptions.size == 1 &&
            selectedOptions.first().groupId == AiChatCardCatalog.DEAD_END_GROUP_ID
        ) {
            dispatchDeadEndAction(selectedOptions.first())
            return
        }

        // Discovery cards (restaurants / activities / both) bypass the LLM and call Yelp directly.
        if (trimmedText.isBlank() &&
            selectedOptions.size == 1 &&
            selectedOptions.first().groupId == AiChatCardCatalog.DISCOVERY_GROUP_ID
        ) {
            dispatchDiscoveryAction(selectedOptions.first())
            return
        }

        val activeQuestionTitle = sessionState.activeResponseCardGroup?.title.orEmpty()

        submitUserTurn(
            visibleMessage = buildVisibleUserMessage(selectedOptions, trimmedText),
            llmUserMessage = buildLlmUserMessage(selectedOptions, trimmedText, activeQuestionTitle),
            visibleTags = buildVisibleUserTags(selectedOptions, trimmedText)
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
        launchHeroImageEnrichment(
            curatedRowId = sessionState.activeCuratedTripRow?.id,
            destinationRowId = sessionState.activeDestinationRecommendationRow?.id
        )
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

    private fun dispatchDeadEndAction(option: AiChatCardOption) {
        val submittedMessage = AiChatItem.TextMessage(
            text = "",
            sender = AiChatSender.USER,
            tags = listOf(option.label)
        )
        conversationItems += submittedMessage

        sessionState = sessionState.copy(
            llmHistory = sessionState.llmHistory + LlmMessage(role = "user", content = option.message),
            draftText = "",
            selectedDraftOptions = emptyList(),
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            pendingPlaceRecommendationRow = null,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null,
            anchorMessageId = submittedMessage.id
        )
        isLoading = true
        persistLastSession()
        publishUiState()

        when {
            option.id.endsWith("_suggest") -> handleSuggestDestinationsAction()
            option.id.endsWith("_build") -> handleBuildStarterTripAction()
            option.id.endsWith("_refine") -> handleKeepRefiningAction()
            else -> {
                Log.w(TAG, "Unknown dead-end action option id=${option.id}")
                finishDeadEndAction(
                    assistantText = "I'm not sure what to do with that. Pick one of the options below.",
                    followUpGroup = AiChatCardCatalog.deadEndActionGroup()
                )
            }
        }
    }

    private fun handleSuggestDestinationsAction() {
        viewModelScope.launch {
            val recommendations = runCatching {
                intakeOrchestrator.suggestDestinations(
                    currentProfile = sessionState.intakeProfile,
                    latestUserInput = "Suggest 2 or 3 destinations that fit my profile so far.",
                    history = sessionState.llmHistory.dropLast(1)
                )
            }.onFailure { error ->
                Log.w(TAG, "Suggest-destinations action failed: ${error.message}", error)
            }.getOrDefault(emptyList())

            val row = AiRecommendationMapper.destinationRowFromIntake(recommendations)
            if (row != null) {
                finishDeadEndAction(
                    assistantText = "Here are a few destinations that fit what you've shared so far. Tap one to use it as the planning destination.",
                    destinationRow = row
                )
            } else {
                finishDeadEndAction(
                    assistantText = "I couldn't pull destination ideas this time. Pick another option below.",
                    followUpGroup = AiChatCardCatalog.deadEndActionGroup()
                )
            }
        }
    }

    private fun handleBuildStarterTripAction() {
        viewModelScope.launch {
            val row = curatedTripCatalog.recommendSeededStarterRow(sessionState.profile)
            if (row != null) {
                finishDeadEndAction(
                    assistantText = "Here are starter trip templates that match your profile. Tap one to open or edit it.",
                    curatedTripRow = row
                )
            } else {
                finishDeadEndAction(
                    assistantText = "I couldn't match a starter trip to your profile yet. Pick another option below.",
                    followUpGroup = AiChatCardCatalog.deadEndActionGroup()
                )
            }
        }
    }

    private fun handleKeepRefiningAction() {
        viewModelScope.launch {
            val plannerContext = plannerContextFor(sessionState.intakeProfile)
            val plannerResolution = resolvePlannerTurn(
                latestUserInput = "Ask me one new allowed planner question I have not answered yet. " +
                    "If no question is allowed, set next_action=suggest_destinations and return no card.",
                history = sessionState.llmHistory.dropLast(1),
                plannerContext = plannerContext
            )
            val intakeResult = plannerResolution.intakeResult
            val followUpGroup = plannerResolution.followUpGroup

            if (followUpGroup != null) {
                val mergedIntakeProfile = sessionState.intakeProfile.mergePatch(intakeResult?.profilePatch)
                val mergedProfile = sessionState.profile.mergeIntakeProfile(mergedIntakeProfile)
                sessionState = sessionState.copy(
                    profile = mergedProfile,
                    intakeProfile = mergedIntakeProfile,
                    stage = AiTravelerProfileReducer.stageFor(mergedProfile),
                    askedQuestionRecords = sessionState.askedQuestionRecords.recordAskedPlannerQuestion(
                        group = followUpGroup,
                        phase = sessionState.plannerPhase
                    )
                )
                finishDeadEndAction(
                    assistantText = plannerResolution.assistantMessage
                        .ifBlank { intakeResult?.assistantMessage.orEmpty() }
                        .ifBlank { "Got it." },
                    followUpGroup = followUpGroup
                )
            } else {
                val recommendations = intakeResult?.destinationRecommendations
                    ?.takeIf { suggestions -> suggestions.isNotEmpty() }
                    ?: fallbackDestinationRecommendations(sessionState.intakeProfile)
                finishDeadEndAction(
                    assistantText = plannerResolution.assistantMessage.ifBlank {
                        "I have enough to show a few destination ideas."
                    },
                    destinationRow = AiRecommendationMapper.destinationRowFromIntake(recommendations)
                )
            }
        }
    }

    private fun dispatchDiscoveryAction(option: AiChatCardOption) {
        val submittedMessage = AiChatItem.TextMessage(
            text = "",
            sender = AiChatSender.USER,
            tags = listOf(option.label)
        )
        conversationItems += submittedMessage

        sessionState = sessionState.copy(
            llmHistory = sessionState.llmHistory + LlmMessage(role = "user", content = option.message),
            draftText = "",
            selectedDraftOptions = emptyList(),
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            pendingPlaceRecommendationRow = null,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null,
            anchorMessageId = submittedMessage.id
        )
        isLoading = true
        persistLastSession()
        publishUiState()

        when (option.id) {
            "discovery_restaurants" -> handleDiscoveryRestaurants()
            "discovery_activities" -> handleDiscoveryActivities()
            "discovery_both" -> handleDiscoveryBoth()
            "discovery_skip" -> handleDiscoverySkip()
            else -> {
                Log.w(TAG, "Unknown discovery option id=${option.id}")
                finishDeadEndAction(
                    assistantText = "Not sure what to find. Pick an option below.",
                    followUpGroup = AiChatCardCatalog.postDestinationDeadEndGroup()
                )
            }
        }
    }

    private fun handleDiscoveryRestaurants() {
        val destination = sessionState.intakeProfile.destination.ifBlank { sessionState.lockedDestination.orEmpty() }
        viewModelScope.launch {
            val row = runCatching {
                placeRecommendationCoordinator.recommendRowForToolCall(
                    toolCall = AiToolCall.SearchRestaurants(
                        city = destination,
                        cuisines = sessionState.intakeProfile.cuisinePreferences
                    ),
                    intakeProfile = sessionState.intakeProfile,
                    profile = sessionState.profile
                )
            }.onFailure { error ->
                Log.w(TAG, "Discovery restaurants failed: ${error.message}", error)
            }.getOrNull()

            if (row != null) {
                finishDeadEndAction(
                    assistantText = "Here are some restaurant options in $destination.",
                    placeRecommendationRow = row
                )
            } else {
                finishDeadEndAction(
                    assistantText = "I couldn't find restaurant options right now.",
                    followUpGroup = AiChatCardCatalog.postDestinationDeadEndGroup()
                )
            }
        }
    }

    private fun handleDiscoveryActivities() {
        val destination = sessionState.intakeProfile.destination.ifBlank { sessionState.lockedDestination.orEmpty() }
        viewModelScope.launch {
            val row = runCatching {
                placeRecommendationCoordinator.recommendRowForToolCall(
                    toolCall = AiToolCall.SearchActivities(
                        city = destination,
                        categories = sessionState.intakeProfile.activitySubCategories
                            .ifEmpty { sessionState.intakeProfile.interests }
                    ),
                    intakeProfile = sessionState.intakeProfile,
                    profile = sessionState.profile
                )
            }.onFailure { error ->
                Log.w(TAG, "Discovery activities failed: ${error.message}", error)
            }.getOrNull()

            if (row != null) {
                finishDeadEndAction(
                    assistantText = "Here are some activity options in $destination.",
                    placeRecommendationRow = row
                )
            } else {
                finishDeadEndAction(
                    assistantText = "I couldn't find activity options right now.",
                    followUpGroup = AiChatCardCatalog.postDestinationDeadEndGroup()
                )
            }
        }
    }

    private fun handleDiscoveryBoth() {
        val destination = sessionState.intakeProfile.destination.ifBlank { sessionState.lockedDestination.orEmpty() }
        viewModelScope.launch {
            val restaurantRow = runCatching {
                placeRecommendationCoordinator.recommendRowForToolCall(
                    toolCall = AiToolCall.SearchRestaurants(
                        city = destination,
                        cuisines = sessionState.intakeProfile.cuisinePreferences
                    ),
                    intakeProfile = sessionState.intakeProfile,
                    profile = sessionState.profile
                )
            }.onFailure { error ->
                Log.w(TAG, "Discovery restaurants (both) failed: ${error.message}", error)
            }.getOrNull()

            val activityRow = runCatching {
                placeRecommendationCoordinator.recommendRowForToolCall(
                    toolCall = AiToolCall.SearchActivities(
                        city = destination,
                        categories = sessionState.intakeProfile.activitySubCategories
                            .ifEmpty { sessionState.intakeProfile.interests }
                    ),
                    intakeProfile = sessionState.intakeProfile,
                    profile = sessionState.profile
                )
            }.onFailure { error ->
                Log.w(TAG, "Discovery activities (both) failed: ${error.message}", error)
            }.getOrNull()

            val primaryRow = restaurantRow ?: activityRow
            val secondaryRow = if (restaurantRow != null) activityRow else null

            if (primaryRow != null) {
                val assistantText = when {
                    restaurantRow != null && activityRow != null ->
                        "Here are restaurant and activity options in $destination."
                    restaurantRow != null -> "Here are some restaurant options in $destination."
                    else -> "Here are some activity options in $destination."
                }
                finishDeadEndAction(
                    assistantText = assistantText,
                    placeRecommendationRow = primaryRow,
                    pendingPlaceRecommendationRow = secondaryRow
                )
            } else {
                finishDeadEndAction(
                    assistantText = "I couldn't find options right now.",
                    followUpGroup = AiChatCardCatalog.postDestinationDeadEndGroup()
                )
            }
        }
    }

    private fun handleDiscoverySkip() {
        finishDeadEndAction(
            assistantText = "No problem. What would you like to do next?",
            followUpGroup = AiChatCardCatalog.postDestinationDeadEndGroup()
        )
    }

    private fun finishDeadEndAction(
        assistantText: String,
        followUpGroup: AiChatCardGroup? = null,
        destinationRow: AiDestinationRecommendationRow? = null,
        curatedTripRow: AiCuratedTripRow? = null,
        placeRecommendationRow: AiPlaceRecommendationRow? = null,
        pendingPlaceRecommendationRow: AiPlaceRecommendationRow? = null
    ) {
        if (assistantText.isNotBlank()) {
            conversationItems += AiChatItem.TextMessage(
                text = assistantText,
                sender = AiChatSender.ASSISTANT
            )
        }
        sessionState = sessionState.copy(
            llmHistory = sessionState.llmHistory + LlmMessage(
                role = "assistant",
                content = assistantText.ifBlank { "(action)" }
            ),
            activeResponseCardGroup = followUpGroup,
            activeDestinationRecommendationRow = destinationRow,
            activeCuratedTripRow = curatedTripRow,
            activePlaceRecommendationRow = placeRecommendationRow,
            pendingPlaceRecommendationRow = pendingPlaceRecommendationRow,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null
        )
        isLoading = false
        persistLastSession()
        publishUiState()
        launchHeroImageEnrichment(curatedTripRow?.id, destinationRow?.id)
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

    private fun observeBookmarks() {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            bookmarksRepository.observeBookmarks(uid).collect { bookmarks ->
                _bookmarkedPlaceIds.value = bookmarks.map { it.id }.toSet()
                publishUiState()
            }
        }
    }

    private fun observeCurrentUserProfile() {
        viewModelScope.launch {
            userProfileRepository.observeCurrentUserProfile().collect { profile ->
                _currentUserProfile.value = profile
                publishUiState()
            }
        }
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
        val userProfile = _currentUserProfile.value

        _uiState.value = AiChatUiState(
            items = buildVisibleItems(),
            userDisplayName = userProfile.chatDisplayName(),
            userProfileImageUrl = userProfile.profileImageUrl,
            isUserProfileLoading = userProfile.isLoading,
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
            bookmarkedPlaceIds = _bookmarkedPlaceIds.value,
            isLoading = isLoading,
            lockedDestination = sessionState.lockedDestination,
            lockedDestinationImageUrl = sessionState.lockedDestinationImageUrl,
            previewDraft = sessionState.previewDraft
        )
    }

    private fun CurrentUserProfile.chatDisplayName(): String {
        displayName
            .takeIf { it.isNotBlank() && it != "User" }
            ?.let { return it }

        username.trim()
            .takeIf(String::isNotBlank)
            ?.let { return it }

        email
            .substringBefore('@')
            .replace('.', ' ')
            .replace('_', ' ')
            .trim()
            .takeIf(String::isNotBlank)
            ?.let { return it }

        return "User"
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
            sessionState.donePlaceRecommendationRows.forEach { row ->
                add(AiChatItem.PlaceRecommendationRow(id = row.id, row = row))
            }
            sessionState.activePlaceRecommendationRow?.let { row ->
                val bookmarkedIds = _bookmarkedPlaceIds.value
                val enrichedRow = row.copy(
                    recommendations = row.recommendations.map { rec ->
                        rec.copy(isBookmarked = rec.id in bookmarkedIds)
                    }
                )
                add(AiChatItem.PlaceRecommendationRow(id = enrichedRow.id, row = enrichedRow))
            }
            sessionState.activeSingleEventCard?.let { card ->
                add(
                    AiChatItem.SingleEventCard(
                        id = card.id,
                        card = card
                    )
                )
            }
            sessionState.activePreferenceQuestionCard?.let(::add)
            sessionState.activeSuggestionCarouselCard?.let(::add)
        }
    }

    private fun restoreLastSessionOrStartFresh() {
        val cached = (processSnapshot ?: sessionStore.loadActiveSession(currentUserId()))
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
                tags = message.tags.orEmpty()
            )
        }
        sessionState = AiChatSessionState(
            sessionId = snapshot.sessionId,
            title = snapshot.title,
            profile = snapshot.profile,
            intakeProfile = snapshot.intakeProfile ?: snapshot.profile.intakeProfile(),
            planningObjective = snapshot.planningObjective,
            stage = snapshot.stage,
            preferenceProfile = snapshot.preferenceProfile ?: PreferenceProfile(),
            discoveryTrack = snapshot.discoveryTrack ?: DiscoveryTrack.NOT_STARTED,
            discoverySlots = snapshot.discoverySlots.orEmpty(),
            discoverySuggestionPool = snapshot.discoverySuggestionPool.orEmpty(),
            llmHistory = snapshot.llmHistory,
            starterCards = AiChatCardCatalog.starterCards(snapshot.sessionId),
            askedQuestionRecords = snapshot.askedQuestionRecords,
            plannerPhase = snapshot.plannerPhase,
            preDestinationQuestionCount = snapshot.preDestinationQuestionCount,
            activeResponseCardGroup = snapshot.activeResponseCardGroup?.toModel(),
            activeDestinationRecommendationRow = snapshot.activeDestinationRecommendationRow?.toModel(),
            activeCuratedTripRow = snapshot.activeCuratedTripRow?.toModel(),
            activePlaceRecommendationRow = snapshot.activePlaceRecommendationRow?.toModel(),
            pendingPlaceRecommendationRow = snapshot.pendingPlaceRecommendationRow?.toModel(),
            donePlaceRecommendationRows = snapshot.donePlaceRecommendationRows.orEmpty().map { it.toModel() },
            activePreferenceQuestionCard = snapshot.activePreferenceQuestionCard,
            activeSuggestionCarouselCard = snapshot.activeSuggestionCarouselCard,
            anchorMessageId = snapshot.anchorMessageId,
            lockedDestination = snapshot.lockedDestination,
            lockedDestinationImageUrl = snapshot.lockedDestinationImageUrl,
            previewDraft = snapshot.previewDraft
        )
        if (sessionState.discoveryTrack == DiscoveryTrack.FOOD &&
            sessionState.discoverySuggestionPool.isNotEmpty() &&
            sessionState.activeSuggestionCarouselCard == null
        ) {
            refreshActiveFoodSuggestionCarousel()
        }
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
                    sender = message.sender,
                    tags = message.tags
                )
            },
            profile = sessionState.profile,
            intakeProfile = sessionState.intakeProfile,
            planningObjective = sessionState.planningObjective,
            stage = sessionState.stage,
            preferenceProfile = sessionState.preferenceProfile,
            discoveryTrack = sessionState.discoveryTrack,
            discoverySlots = sessionState.discoverySlots,
            discoverySuggestionPool = sessionState.discoverySuggestionPool,
            llmHistory = sessionState.llmHistory,
            askedQuestionRecords = sessionState.askedQuestionRecords,
            plannerPhase = sessionState.plannerPhase,
            preDestinationQuestionCount = sessionState.preDestinationQuestionCount,
            activeResponseCardGroup = sessionState.activeResponseCardGroup?.toPersisted(),
            activeDestinationRecommendationRow = sessionState.activeDestinationRecommendationRow?.toPersisted(),
            activeCuratedTripRow = sessionState.activeCuratedTripRow?.toPersisted(),
            activePlaceRecommendationRow = sessionState.activePlaceRecommendationRow?.toPersisted(),
            pendingPlaceRecommendationRow = sessionState.pendingPlaceRecommendationRow?.toPersisted(),
            donePlaceRecommendationRows = sessionState.donePlaceRecommendationRows.map { it.toPersisted() },
            activePreferenceQuestionCard = sessionState.activePreferenceQuestionCard,
            activeSuggestionCarouselCard = sessionState.activeSuggestionCarouselCard,
            anchorMessageId = sessionState.anchorMessageId,
            lockedDestination = sessionState.lockedDestination,
            lockedDestinationImageUrl = sessionState.lockedDestinationImageUrl,
            previewDraft = sessionState.previewDraft
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
                "$role: ${item.renderMessageText()}"
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
        }?.renderMessageText()
            ?.lineSequence()
            ?.firstOrNull()
            .orEmpty()
            .split(Regex("\\s+"))
            .filter { token -> token.isNotBlank() }
            .take(3)
            .joinToString(" ")
            .trim()
    }

    private fun AiChatItem.TextMessage.renderMessageText(): String {
        val tagLine = tags.takeIf(List<String>::isNotEmpty)
            ?.joinToString(separator = " & ") { tag -> "($tag)" }
            .orEmpty()
        return listOf(tagLine, text.trim())
            .filter { value -> value.isNotBlank() }
            .joinToString(separator = "\n")
            .trim()
    }

    private fun buildVisibleUserMessage(
        selectedOptions: List<AiChatCardOption>,
        typedText: String
    ): String {
        // Card-only submissions render as chips; typed text is shown as a regular bubble below.
        return typedText
    }

    private fun buildVisibleUserTags(
        selectedOptions: List<AiChatCardOption>,
        typedText: String
    ): List<String> {
        val typedNonBlank = typedText.isNotBlank()
        return selectedOptions
            // Hide the Other chip when the user typed text — the bubble carries that text instead.
            .filterNot { option -> option.requiresText && typedNonBlank }
            .map(AiChatCardOption::label)
    }

    private fun buildLlmUserMessage(
        selectedOptions: List<AiChatCardOption>,
        typedText: String,
        activeQuestionTitle: String
    ): String {
        if (selectedOptions.isNotEmpty() && selectedOptions.all { option -> option.groupId == STARTER_CARD_GROUP_ID }) {
            return buildList {
                addAll(
                    selectedOptions.mapNotNull { option ->
                        option.message.trim().ifBlank { option.label.trim() }.ifBlank { null }
                    }
                )
                typedText.takeIf { text -> text.isNotBlank() }?.let { text ->
                    add("Extra context: $text")
                }
            }.joinToString(separator = "\n").trim()
        }

        val standardOptions = selectedOptions.filterNot { option -> option.requiresText }
        val otherSelected = selectedOptions.any { option -> option.requiresText }
        val typedNonBlank = typedText.isNotBlank()

        return buildList {
            if (standardOptions.isNotEmpty()) {
                add("Selected choices:")
                addAll(standardOptions.map { option -> "- ${option.message}" })
            }
            if (otherSelected && typedNonBlank) {
                add("Custom answer: $typedText")
            } else if (otherSelected && !typedNonBlank) {
                if (activeQuestionTitle.isNotBlank()) {
                    add("I would like different options for: $activeQuestionTitle")
                } else {
                    add("Please show me different options.")
                }
            } else if (typedNonBlank) {
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
            appendLine()
            appendLine("Destination is now locked. Respond with next_action=ask_more.")
            appendLine("Ask when the user would like to travel (question_id='travel_timeline').")
            appendLine("Set topic_path='travel_timeline' and suggest exactly 4 time options.")
            append("Do not set next_action=build_trip.")
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
            add(LlmMessage(role = "system", content = "Structured intake profile JSON:\n${intakeProfile.toPromptJson()}"))
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

    private fun submitUserTurn(
        visibleMessage: String,
        llmUserMessage: String,
        visibleTags: List<String> = emptyList()
    ) {
        val trimmedVisibleMessage = visibleMessage.trim()
        val trimmedLlmUserMessage = llmUserMessage.trim()
        if ((trimmedVisibleMessage.isBlank() && visibleTags.isEmpty()) || trimmedLlmUserMessage.isBlank()) return

        // Capture the question the user just answered before we clear the active card group.
        val answeredCardGroup = sessionState.activeResponseCardGroup
        val answeredDraftOptions = sessionState.selectedDraftOptions.distinctBy(AiChatCardOption::id)
        val answeredQuestionTitle = answeredCardGroup
            ?.takeIf { group -> group.id != AiChatCardCatalog.DEAD_END_GROUP_ID }
            ?.title
            .orEmpty()
        val answeredQuestionRecords = sessionState.askedQuestionRecords.recordAnsweredPlannerQuestion(
            group = answeredCardGroup,
            selectedOptions = answeredDraftOptions
        )
        val plannerQuestionRecords = recordStarterGridAnswers(
            existingRecords = answeredQuestionRecords,
            selectedOptions = answeredDraftOptions
        )

        // Promote the question into the last AI bubble so it sits above the user's answer,
        // not in the following AI response.
        if (answeredQuestionTitle.isNotBlank()) {
            val lastIndex = conversationItems.indexOfLast { it.sender == AiChatSender.ASSISTANT }
            if (lastIndex >= 0) {
                val prev = conversationItems[lastIndex]
                conversationItems[lastIndex] = prev.copy(
                    text = "${prev.text} $answeredQuestionTitle"
                )
            }
        }

        val submittedMessage = AiChatItem.TextMessage(
            text = trimmedVisibleMessage,
            sender = AiChatSender.USER,
            tags = visibleTags
        )
        conversationItems += submittedMessage

        val updatedProfile = AiTravelerProfileReducer.merge(sessionState.profile, trimmedLlmUserMessage)
        val cardPatch = cardIdToIntakeProfilePatch(answeredDraftOptions.map { it.id })
        val updatedIntakeProfile = sessionState.intakeProfile.mergePatch(updatedProfile.intakeProfile()).mergePatch(cardPatch)
        val updatedPlannerPhase = plannerPhaseFor(updatedIntakeProfile)
        sessionState = sessionState.copy(
            profile = updatedProfile,
            intakeProfile = updatedIntakeProfile,
            stage = AiTravelerProfileReducer.stageFor(updatedProfile),
            llmHistory = sessionState.llmHistory + LlmMessage(role = "user", content = trimmedLlmUserMessage),
            askedQuestionRecords = plannerQuestionRecords,
            plannerPhase = updatedPlannerPhase,
            draftText = "",
            selectedDraftOptions = emptyList(),
            activeResponseCardGroup = null,
            activeDestinationRecommendationRow = null,
            activeCuratedTripRow = null,
            activePlaceRecommendationRow = null,
            pendingPlaceRecommendationRow = null,
            donePlaceRecommendationRows = emptyList(),
            activeSingleEventCard = null,
            anchorMessageId = submittedMessage.id
        )
        isLoading = true
        persistLastSession()
        publishUiState()

        viewModelScope.launch {
            val plannerContext = plannerContextFor(sessionState.intakeProfile)
            val plannerResolution = resolvePlannerTurn(
                latestUserInput = trimmedLlmUserMessage,
                history = sessionState.llmHistory.dropLast(1),
                plannerContext = plannerContext
            )
            val intakeResult = plannerResolution.intakeResult

            val initialMergedIntakeProfile = sessionState.intakeProfile.mergePatch(intakeResult?.profilePatch)
            val enrichedIntakeResult = if (
                (plannerResolution.forceVisualAction ||
                    intakeResult?.nextAction == AiTripIntakeNextAction.SUGGEST_DESTINATIONS) &&
                initialMergedIntakeProfile.destination.isBlank()
            ) {
                val recommendations = intakeResult?.destinationRecommendations
                    ?.takeIf { suggestions -> suggestions.isNotEmpty() }
                    ?: runCatching {
                        intakeOrchestrator.suggestDestinations(
                            currentProfile = initialMergedIntakeProfile,
                            latestUserInput = trimmedLlmUserMessage,
                            history = sessionState.llmHistory
                        )
                    }.onFailure { error ->
                        Log.w(TAG, "Structured destination suggestion call failed.", error)
                    }.getOrDefault(emptyList())
                    .ifEmpty { fallbackDestinationRecommendations(initialMergedIntakeProfile) }

                if (recommendations.isNotEmpty()) {
                    (intakeResult ?: AiTripIntakeTurnResult(
                        assistantMessageText = plannerResolution.assistantMessage,
                        nextAction = AiTripIntakeNextAction.SUGGEST_DESTINATIONS
                    )).withDestinationRecommendations(recommendations)
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
            val destinationKnown = mergedIntakeProfile.destination.isNotBlank() ||
                sessionState.lockedDestination?.isNotBlank() == true
            val toolRouterResult = if (destinationKnown) {
                runCatching {
                    toolRouter.routeTools(
                        currentProfile = mergedIntakeProfile,
                        latestUserInput = trimmedLlmUserMessage,
                        history = sessionState.llmHistory.dropLast(1),
                        planningObjective = assistantPlanningObjective
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Tool routing failed. Continuing without routed tool calls.", error)
                }.getOrNull()
            } else {
                null
            }
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

            val intakeFollowUpGroup = plannerResolution.followUpGroup
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
                    ) ?: curatedTripCatalog.recommendSeededStarterRow(mergedProfile) else null
                }

                AiTripIntakeNextAction.ASK_MORE,
                AiTripIntakeNextAction.SUGGEST_DESTINATIONS,
                null -> {
                    if (plannerResolution.forceVisualAction && destinationKnown) {
                        curatedTripCatalog.recommendTrips(
                            profile = mergedProfile,
                            viewerUid = currentUserId()
                        ) ?: curatedTripCatalog.recommendSeededStarterRow(mergedProfile)
                    } else {
                        null
                    }
                }
            }
            val firstPlaceRow = toolDispatch.placeRecommendationRows.firstOrNull()
            val secondPlaceRow = toolDispatch.placeRecommendationRows.getOrNull(1)
            val singleEventCard = toolDispatch.singleEventResolution?.suggestion

            val anyUiResolved = intakeFollowUpGroup != null ||
                destinationRecommendationRow != null ||
                curatedTripRow != null ||
                firstPlaceRow != null ||
                singleEventCard != null
            val intakeDeadEnd = !anyUiResolved
            val isPostDestinationLockNow = sessionState.lockedDestination?.isNotBlank() == true
            val effectiveFollowUpGroup = when {
                intakeDeadEnd && isPostDestinationLockNow -> AiChatCardCatalog.postDestinationDeadEndGroup()
                intakeDeadEnd -> AiChatCardCatalog.deadEndActionGroup()
                else -> intakeFollowUpGroup
            }

            Log.d("PlannerDebug", buildString {
                appendLine("══ UI RESULT ══")
                appendLine("anyUiResolved=$anyUiResolved  intakeDeadEnd=$intakeDeadEnd")
                appendLine("followUpGroup=${intakeFollowUpGroup?.id} (source=${intakeFollowUpGroup?.source})")
                appendLine("effectiveFollowUpGroup=${effectiveFollowUpGroup?.id}")
                appendLine("destRow=${destinationRecommendationRow?.id}")
                appendLine("curatedRow=${curatedTripRow?.id}")
                appendLine("placeRow=${firstPlaceRow?.id}")
                append("nextAction=${enrichedIntakeResult?.nextAction}  forceVisual=${plannerResolution.forceVisualAction}")
            })

            if (intakeDeadEnd) {
                Log.w("PlannerDebug", "DEAD END — recovering with ${effectiveFollowUpGroup?.id}. rejection=${plannerResolution.rejectionReason}")
            }

            val followUpGroup = effectiveFollowUpGroup

            val assistantResponse = buildAssistantResponse(
                intakeResult = enrichedIntakeResult,
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile,
                planningObjective = assistantPlanningObjective,
                history = sessionState.llmHistory,
                ticketmasterGrounding = toolDispatch.singleEventResolution?.groundingContext,
                viabilityWarning = toolDispatch.viabilityWarning,
                placeRecommendationRow = firstPlaceRow,
                intakeDeadEnd = intakeDeadEnd,
                isPostDestinationLock = sessionState.lockedDestination?.isNotBlank() == true,
                assistantOverride = plannerResolution.assistantMessage
            )

            val debugTags = if (BuildConfig.DEBUG) plannerResolution.debugLog else emptyList()
            conversationItems += AiChatItem.TextMessage(
                text = assistantResponse,
                sender = AiChatSender.ASSISTANT,
                tags = debugTags
            )

            sessionState = sessionState.copy(
                profile = mergedProfile,
                intakeProfile = mergedIntakeProfile,
                planningObjective = assistantPlanningObjective,
                stage = AiTravelerProfileReducer.stageFor(mergedProfile),
                llmHistory = sessionState.llmHistory + LlmMessage(
                    role = "assistant",
                    content = assistantResponse
                ),
                askedQuestionRecords = sessionState.askedQuestionRecords.recordAskedPlannerQuestion(
                    group = intakeFollowUpGroup,
                    phase = sessionState.plannerPhase
                ),
                plannerPhase = plannerPhaseAfterTurn(
                    intakeProfile = mergedIntakeProfile,
                    destinationRecommendationRow = destinationRecommendationRow,
                    curatedTripRow = curatedTripRow
                ),
                preDestinationQuestionCount = nextPreDestinationQuestionCount(intakeFollowUpGroup),
                activeResponseCardGroup = followUpGroup,
                activeDestinationRecommendationRow = destinationRecommendationRow,
                activeCuratedTripRow = curatedTripRow,
                activePlaceRecommendationRow = firstPlaceRow,
                pendingPlaceRecommendationRow = secondPlaceRow,
                activeSingleEventCard = singleEventCard
            )
            isLoading = false
            // Phase 1 v10: keep food discovery logic available, but stop auto-starting it.
            // startDiscoveryIfEligibleInternal()
            persistLastSession()
            publishUiState()
            generateTitleIfNeeded()
            launchHeroImageEnrichment(curatedTripRow?.id, destinationRecommendationRow?.id)
        }
    }

    private suspend fun resolvePlannerTurn(
        latestUserInput: String,
        history: List<LlmMessage>,
        plannerContext: PlannerContext
    ): PlannerTurnResolution {
        val trail = mutableListOf<String>()
        val p = plannerContext.currentProfile

        Log.d("PlannerDebug", buildString {
            appendLine("══ PLANNER TURN ══")
            appendLine("phase=${plannerContext.phase}  preDestQ=${plannerContext.preDestinationQuestionCount}  forceVisual=${plannerContext.shouldForceVisualAction()}")
            appendLine("profile: dest='${p.destination}' dateWindow='${p.dateWindow}' duration=${p.durationDays} budget=${p.budgetLevel} pace=${p.pace} interests=${p.interests.size} cuisine=${p.cuisinePreferences.size}")
            appendLine("askedTopics=${plannerContext.askedTopicSummary()}")
            append("nextBestTopic=${plannerContext.nextBestAllowedTopicPath()}")
        })

        if (plannerContext.shouldForceVisualAction()) {
            val reason = "planner gate: visual recommendations due"
            trail += "FORCE_VISUAL [$reason]"
            Log.d("PlannerDebug", "Resolution: $reason")
            return PlannerTurnResolution(
                forceVisualAction = true,
                assistantMessage = "I have enough to show a few visual options now.",
                rejectionReason = reason,
                debugLog = trail
            )
        }

        val askedTopicPaths = plannerContext.askedQuestionRecords
            .map { record -> record.topicPath }
            .filter(String::isNotBlank)
            .distinct()

        suspend fun callPlanner(
            repairInstruction: String = "",
            forcedTopicPath: String = ""
        ): AiTripIntakeTurnResult? {
            return runCatching {
                intakeOrchestrator.analyzeTurn(
                    currentProfile = plannerContext.currentProfile,
                    latestUserInput = latestUserInput,
                    history = history,
                    askedQuestionIds = askedTopicPaths,
                    planningObjective = sessionState.planningObjective,
                    plannerContext = plannerContext,
                    repairInstruction = repairInstruction,
                    forcedTopicPath = forcedTopicPath
                )
            }.onFailure { error ->
                Log.w(TAG, "Structured intake planner call failed.", error)
            }.getOrNull()
        }

        fun logAttempt(label: String, result: AiTripIntakeTurnResult?, accepted: Boolean, reason: String) {
            val action = result?.nextAction?.name ?: "null"
            val topic = result?.topicPath?.ifBlank { "-" } ?: "null"
            val opts = result?.options?.size ?: 0
            val multi = result?.allowMultiple ?: false
            val line = "$label: action=$action topic=$topic opts=$opts multi=$multi → ${if (accepted) "ACCEPTED" else "REJECTED [$reason]"}"
            trail += line
            Log.d("PlannerDebug", line)
        }

        fun validate(result: AiTripIntakeTurnResult?): PlannerTurnResolution {
            if (result == null) {
                return PlannerTurnResolution(rejectionReason = "model returned no valid planner JSON")
            }

            val prospectiveProfile = plannerContext.currentProfile.mergePatch(result.profilePatch)
            val prospectiveContext = plannerContext.copy(
                currentProfile = prospectiveProfile,
                visualRecommendationsDue = plannerContext.visualRecommendationsDue ||
                    (plannerContext.phase == PlannerPhase.PRE_DESTINATION &&
                        (plannerContext.preDestinationQuestionCount >= PRE_DESTINATION_QUESTION_LIMIT ||
                            prospectiveProfile.isReadyForDestinationRecommendations()))
            )
            val validation = result.validatePlannerTurn(prospectiveContext)
            if (validation.accepted) {
                return PlannerTurnResolution(
                    intakeResult = result,
                    followUpGroup = validation.cardGroup,
                    forceVisualAction = validation.forceVisualAction
                )
            }

            return PlannerTurnResolution(
                intakeResult = result,
                forceVisualAction = validation.forceVisualAction,
                rejectionReason = validation.rejectionReason
            )
        }

        val first = validate(callPlanner())
        logAttempt("A1", first.intakeResult, first.isUsable, first.rejectionReason)
        if (first.isUsable) return first.copy(debugLog = trail)

        val repaired = validate(
            callPlanner(repairInstruction = plannerContext.repairPrompt(first.rejectionReason))
        )
        logAttempt("A2-repair", repaired.intakeResult, repaired.isUsable, repaired.rejectionReason)
        if (repaired.isUsable) return repaired.copy(debugLog = trail)

        val forcedTopicPath = plannerContext.nextBestAllowedTopicPath()
        Log.d("PlannerDebug", "forcedTopicPath=$forcedTopicPath")
        if (forcedTopicPath != null) {
            val forced = validate(
                callPlanner(
                    repairInstruction = plannerContext.repairPrompt(repaired.rejectionReason.ifBlank { first.rejectionReason }),
                    forcedTopicPath = forcedTopicPath
                )
            )
            logAttempt("A3-forced[$forcedTopicPath]", forced.intakeResult, forced.isUsable, forced.rejectionReason)
            if (forced.isUsable) return forced.copy(debugLog = trail)

            AiChatCardCatalog.fallbackQuestionGroup(forcedTopicPath)?.let { fallbackGroup ->
                val line = "APP_FALLBACK: $forcedTopicPath"
                trail += line
                Log.d("PlannerDebug", "Resolution: $line")
                return PlannerTurnResolution(
                    followUpGroup = fallbackGroup,
                    assistantMessage = "I can narrow that down with one more choice.",
                    rejectionReason = forced.rejectionReason.ifBlank { repaired.rejectionReason },
                    debugLog = trail
                )
            }
        }

        val finalReason = repaired.rejectionReason.ifBlank { first.rejectionReason }
        trail += "FORCE_VISUAL: all attempts exhausted [$finalReason]"
        Log.d("PlannerDebug", "Resolution: FORCE_VISUAL all attempts exhausted — $finalReason")
        return PlannerTurnResolution(
            forceVisualAction = true,
            assistantMessage = "I have enough to show a few visual options now.",
            rejectionReason = finalReason,
            debugLog = trail
        )
    }

    private fun plannerContextFor(intakeProfile: AiTripIntakeProfile): PlannerContext {
        val phase = plannerPhaseFor(intakeProfile)
        return PlannerContext(
            phase = phase,
            askedQuestionRecords = sessionState.askedQuestionRecords,
            currentProfile = intakeProfile,
            preDestinationQuestionCount = sessionState.preDestinationQuestionCount,
            visualRecommendationsDue = phase == PlannerPhase.PRE_DESTINATION &&
                (sessionState.preDestinationQuestionCount >= PRE_DESTINATION_QUESTION_LIMIT ||
                    intakeProfile.isReadyForDestinationRecommendations())
        )
    }

    private fun plannerPhaseFor(intakeProfile: AiTripIntakeProfile): PlannerPhase {
        return when {
            sessionState.lockedDestination?.isNotBlank() == true || intakeProfile.destination.isNotBlank() -> {
                PlannerPhase.DESTINATION_LOCKED
            }

            sessionState.activeDestinationRecommendationRow != null -> PlannerPhase.VISUAL_RECOMMENDATIONS
            else -> PlannerPhase.PRE_DESTINATION
        }
    }

    private fun plannerPhaseAfterTurn(
        intakeProfile: AiTripIntakeProfile,
        destinationRecommendationRow: AiDestinationRecommendationRow?,
        curatedTripRow: AiCuratedTripRow?
    ): PlannerPhase {
        return when {
            curatedTripRow != null -> PlannerPhase.TRIP_BUILD
            destinationRecommendationRow != null -> PlannerPhase.VISUAL_RECOMMENDATIONS
            else -> plannerPhaseFor(intakeProfile)
        }
    }

    private fun nextPreDestinationQuestionCount(group: AiChatCardGroup?): Int {
        if (group == null || sessionState.plannerPhase != PlannerPhase.PRE_DESTINATION) {
            return sessionState.preDestinationQuestionCount
        }
        if (group.topicPath.toPlannerTopicPath().isBlank()) {
            return sessionState.preDestinationQuestionCount
        }
        return (sessionState.preDestinationQuestionCount + 1).coerceAtMost(PRE_DESTINATION_QUESTION_LIMIT)
    }

    private fun recordStarterGridAnswers(
        existingRecords: List<AskedQuestionRecord>,
        selectedOptions: List<AiChatCardOption>
    ): List<AskedQuestionRecord> {
        val starterAnswers = selectedOptions
            .filter { option -> option.groupId == STARTER_CARD_GROUP_ID }
            .flatMap(::starterAnswerIds)
            .distinct()
        if (starterAnswers.isEmpty()) return existingRecords

        val starterRecord = AskedQuestionRecord(
            topicPath = "destination_style",
            questionId = STARTER_CARD_GROUP_ID,
            source = PlannerQuestionSource.STARTER_GRID,
            phase = PlannerPhase.PRE_DESTINATION,
            answerIds = starterAnswers,
            answered = true
        )
        return existingRecords
            .filterNot { record ->
                record.topicPath == starterRecord.topicPath &&
                    record.questionId == starterRecord.questionId
            }
            .plus(starterRecord)
    }

    private fun starterAnswerIds(option: AiChatCardOption): List<String> {
        val rawId = option.id.substringAfter(':', option.id)
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        val labelWords = option.label
            .lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .filter(String::isNotBlank)
        val semanticIds = labelWords.mapNotNull { word ->
            when (word) {
                "beach", "romantic", "nature", "city", "food", "family", "road", "spa",
                "girls", "adventure", "arts", "culture", "nightlife", "pet", "shopping",
                "wellness", "island", "coffee" -> word
                else -> null
            }
        }
        return (listOf(rawId) + semanticIds).filter(String::isNotBlank).distinct()
    }

    private fun fallbackDestinationRecommendations(
        intakeProfile: AiTripIntakeProfile
    ): List<AiTripIntakeDestinationRecommendation> {
        val profileTags = buildSet {
            addAll(intakeProfile.destinationStyle.map { style -> style.lowercase(Locale.US) })
            addAll(intakeProfile.interests.map { interest -> interest.lowercase(Locale.US) })
            when (intakeProfile.tripType) {
                com.example.travelcents.data.ai.chat.AiTripType.ROMANTIC -> add("romantic")
                com.example.travelcents.data.ai.chat.AiTripType.FAMILY -> add("family")
                com.example.travelcents.data.ai.chat.AiTripType.FRIENDS -> add("group")
                com.example.travelcents.data.ai.chat.AiTripType.SOLO -> add("solo")
                else -> Unit
            }
        }
        return AiCuratedTripSeedCatalog.seeds
            .sortedWith(
                compareByDescending<com.example.travelcents.data.ai.chat.AiCuratedTripSeed> { seed ->
                    seed.tags.count { tag -> tag.lowercase(Locale.US) in profileTags }
                }.thenBy { seed -> seed.destination }
            )
            .take(3)
            .map { seed ->
                AiTripIntakeDestinationRecommendation(
                    id = seed.id,
                    destination = seed.destination,
                    summary = seed.summary,
                    reason = "Matches the trip direction you have shared so far."
                )
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
        placeRecommendationRow: AiPlaceRecommendationRow?,
        intakeDeadEnd: Boolean,
        isPostDestinationLock: Boolean = false,
        assistantOverride: String = ""
    ): String {
        val structuredAck = assistantOverride
            .ifBlank { intakeResult?.assistantMessage.orEmpty() }
            .takeIf { message -> message.isNotBlank() }
        val baseResponse = when {
            !ticketmasterGrounding.isNullOrBlank() -> fallbackAssistantMessage(
                profile = profile,
                intakeProfile = intakeProfile,
                history = history,
                planningObjective = planningObjective,
                groundingContext = ticketmasterGrounding
            )
            intakeDeadEnd && isPostDestinationLock -> structuredAck ?: DESTINATION_ACK_POOL.random()
            intakeDeadEnd -> fallbackAssistantMessage(
                profile = profile,
                intakeProfile = intakeProfile,
                history = history,
                planningObjective = planningObjective,
                groundingContext = DEAD_END_RECOVERY_GROUNDING
            )
            structuredAck != null -> structuredAck
            else -> fallbackAssistantMessage(
                profile = profile,
                intakeProfile = intakeProfile,
                history = history,
                planningObjective = planningObjective
            )
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
        val placeCalls = toolRouterResult.toolCalls.filter { toolCall ->
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
        val placeRecommendationRows = placeCalls.mapNotNull { toolCall ->
            placeRecommendationCoordinator.recommendRowForToolCall(
                toolCall = toolCall,
                intakeProfile = intakeProfile,
                profile = profile
            )
        }

        return AiToolDispatchResult(
            placeRecommendationRows = placeRecommendationRows,
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

    fun doneBrowsingPlaceRow() {
        val current = sessionState.activePlaceRecommendationRow ?: return
        val doneRow = current.copy(isDone = true)
        val pending = sessionState.pendingPlaceRecommendationRow
        sessionState = sessionState.copy(
            donePlaceRecommendationRows = sessionState.donePlaceRecommendationRows + doneRow,
            activePlaceRecommendationRow = pending,
            pendingPlaceRecommendationRow = null
        )
        persistLastSession()
        publishUiState()
    }

    private fun resolveDestinationRecommendationRow(
        intakeResult: AiTripIntakeTurnResult?,
        profile: AiTravelerProfile,
        intakeProfile: AiTripIntakeProfile
    ): AiDestinationRecommendationRow? {
        if (profile.destination.isNotBlank() || intakeProfile.destination.isNotBlank()) return null
        return intakeResult?.toDestinationRecommendationRow()
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
                        val query = "$destination skyline"
                        val params = UnsplashSearchParams(orientation = "landscape")
                        val key = heroImageCacheKey(query, params)
                        val cached = heroImageCache[key]
                        if (cached != null) {
                            resolved[destination] = cached
                            return@forEach
                        }
                        val resolvedUrl = runCatching {
                            destinationImages.resolve(query = query, params = params)
                        }.onFailure { error ->
                            Log.w(TAG, "Hero image lookup failed for '$destination': ${error.message}")
                        }.getOrNull()
                        if (!resolvedUrl.isNullOrBlank()) {
                            heroImageCache[key] = resolvedUrl
                            resolved[destination] = resolvedUrl
                        }
                    }

                if (resolved.isEmpty()) return@launch

                val context = getApplication<Application>()
                val cachedMedia = runCatching {
                    ImageCacheManager.cacheMedia(
                        context = context,
                        bucketName = AI_CHAT_IMAGE_CACHE_BUCKET,
                        urls = resolved.values.toList()
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Curated trip image cache write failed: ${error.message}")
                }.getOrDefault(emptyMap())

                val current = sessionState.activeCuratedTripRow ?: return@launch
                if (current.id != curatedRowId) return@launch

                val updatedTrips = current.trips.map { starter ->
                    if (!starter.heroImageUrl.isNullOrBlank()) starter
                    else resolved[starter.destination]?.let { url ->
                        starter.copy(heroImageUrl = cachedMedia[url] ?: url)
                    }
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
                        val query = "$destination travel"
                        val params = UnsplashSearchParams()
                        val key = heroImageCacheKey(query, params)
                        val cached = heroImageCache[key]
                        if (cached != null) {
                            resolved[destination] = cached
                            return@forEach
                        }
                        val resolvedUrl = runCatching {
                            destinationImages.resolve(query = query, params = params)
                        }.onFailure { error ->
                            Log.w(TAG, "Destination image lookup failed for '$destination': ${error.message}")
                        }.getOrNull()
                        if (!resolvedUrl.isNullOrBlank()) {
                            heroImageCache[key] = resolvedUrl
                            resolved[destination] = resolvedUrl
                        }
                    }

                if (resolved.isEmpty()) return@launch

                val context = getApplication<Application>()
                val cachedMedia = runCatching {
                    ImageCacheManager.cacheMedia(
                        context = context,
                        bucketName = AI_CHAT_IMAGE_CACHE_BUCKET,
                        urls = resolved.values.toList()
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Destination image cache write failed: ${error.message}")
                }.getOrDefault(emptyMap())

                val current = sessionState.activeDestinationRecommendationRow ?: return@launch
                if (current.id != destinationRowId) return@launch

                val updatedRecs = current.recommendations.map { rec ->
                    if (!rec.imageUrl.isNullOrBlank()) rec
                    else resolved[rec.destination]?.let { url ->
                        rec.copy(imageUrl = cachedMedia[url] ?: url)
                    }
                        ?: rec
                }
                if (updatedRecs == current.recommendations) return@launch

                sessionState = sessionState.copy(activeDestinationRecommendationRow = current.copy(recommendations = updatedRecs))
                persistLastSession()
                publishUiState()
            }
        }
    }

    private fun heroImageCacheKey(query: String, params: UnsplashSearchParams): String {
        val sanitizedParams = params.sanitized()
        return listOf(
            query.trim().lowercase(Locale.US),
            sanitizedParams.orientation.orEmpty().lowercase(Locale.US),
            sanitizedParams.color.orEmpty().lowercase(Locale.US),
            sanitizedParams.orderBy.lowercase(Locale.US),
            sanitizedParams.contentFilter.lowercase(Locale.US),
            sanitizedParams.perPage.toString(),
            sanitizedParams.pageIndex.toString()
        ).joinToString("|")
    }

    private companion object {
        private const val STARTER_CARD_GROUP_ID = "starter_grid"
        private const val TAG = "AiChatViewModel"
        private const val AI_CHAT_IMAGE_CACHE_BUCKET = "ai_chat_destination_images"
        private const val FOOD_PREFERENCE_CARD_ID = "phase5_food_preference_question"
        private const val FOOD_SUGGESTION_CARD_ID = "phase5_food_suggestion_carousel"
        private const val FOOD_DISCOVERY_SLOT_ID = "phase5_food_dinner_slot"
        private const val FOOD_VISIBLE_SUGGESTION_COUNT = 3
        private const val FOOD_SUGGESTION_POOL_MIN = 10
        private const val FOOD_SUGGESTION_POOL_MAX = 15
        private const val PRE_DESTINATION_QUESTION_LIMIT = 4
        private val FOOD_PREFERENCE_OPTIONS = listOf(
            "Local favorites",
            "Street food",
            "Fine dining",
            "Cafes",
            "Vegetarian",
            "Seafood"
        )
        private val FOOD_TIME_WINDOWS = listOf(
            "18:30" to "20:00",
            "19:00" to "20:30",
            "20:00" to "21:30"
        )
        private const val BASE_SYSTEM_PROMPT =
            "You are TravelCents AI, a trip-planning copilot inside the TravelCents app. " +
                "Be concise, helpful, and practical. Keep replies to short acknowledgment paragraphs. " +
                "The app may present follow-up choices separately, so do not stack multiple questions or long questionnaires. " +
                "Use the traveler profile context when available. Do not mention model vendors or say you are a generic AI chatbot."
        private val DESTINATION_ACK_POOL = listOf(
            "Sounds good!", "Love it!", "Nice choice!", "Great pick!", "Perfect!"
        )
        private const val DEAD_END_RECOVERY_GROUNDING =
            "The structured intake step produced no follow-up. " +
                "Briefly summarize what you already know about the user's trip in 1 to 2 sentences, " +
                "then say the app is showing next-step options below to suggest destinations, build a starter trip, or keep refining. " +
                "Do not ask another question — the user will pick from the buttons."
    }
}

private data class AiToolDispatchResult(
    val placeRecommendationRows: List<AiPlaceRecommendationRow> = emptyList(),
    val singleEventResolution: AiSingleEventResolution? = null,
    val viabilityWarning: String = ""
)

private data class PlannerTurnResolution(
    val intakeResult: AiTripIntakeTurnResult? = null,
    val followUpGroup: AiChatCardGroup? = null,
    val forceVisualAction: Boolean = false,
    val assistantMessage: String = "",
    val rejectionReason: String = "",
    val debugLog: List<String> = emptyList()
) {
    val isUsable: Boolean
        get() = forceVisualAction ||
            followUpGroup != null ||
            (intakeResult != null && intakeResult.nextAction != AiTripIntakeNextAction.ASK_MORE)
}
