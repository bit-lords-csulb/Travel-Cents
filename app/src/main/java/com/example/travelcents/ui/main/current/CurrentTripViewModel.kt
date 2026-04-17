package com.example.travelcents.ui.main.current

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.local.trip.TravelCentsDatabase
import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripPerformanceLogger
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.resolveTripName
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.data.sync.CurrentTripSyncCoordinator
import com.example.travelcents.data.sync.TripSyncCoordinator
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.normalizeTime
import com.example.travelcents.ui.main.shared.TripMediaDetailPipeline
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

data class EditablePlan(
    val eventId: String? = null,
    val type: String = "activity",
    val title: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val timeZoneId: String = defaultPlanTimeZoneId(),
    val location: String = "",
    val notes: String = "",
    val colorKey: String = "rose",
    val imageUrl: String = "",
    val existingDetails: Map<String, String> = emptyMap()
)

data class TripMemberUi(
    val uid: String,
    val displayName: String,
    val initial: Char
)

data class CurrentTripUiState(
    val isLoading: Boolean = true,
    val currentTripId: String? = null,
    val currentTripOwnerUid: String? = null,
    val viewerUid: String? = null,
    val accessRole: TripAccessRole = TripAccessRole.VIEWER,
    val canEditTrip: Boolean = false,
    val canManageTrip: Boolean = false,
    val tripTitle: String = "Loading Trip...",
    val destination: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val events: List<TravelEvent> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

data class ShareTarget(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val memberUids: List<String> = emptyList()
)

class CurrentTripViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val DEFAULT_TRIP_TITLE = "Loading Trip..."
        private const val EMPTY_PLANS_MESSAGE = "No plans yet. Tap + to add one."
        private const val NO_TRIP_MESSAGE = "No trip found yet. Create one from the New Trip tab."
        private val YELP_PREFETCH_TYPES = setOf("restaurant", "activity")
    }

    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()

    private val _tripTitle = MutableStateFlow(DEFAULT_TRIP_TITLE)
    val tripTitle: StateFlow<String> = _tripTitle.asStateFlow()

    private val _uiState = MutableStateFlow(CurrentTripUiState())
    val uiState: StateFlow<CurrentTripUiState> = _uiState.asStateFlow()

    private val _allTrips = MutableStateFlow<List<Itinerary>>(emptyList())
    val allTrips: StateFlow<List<Itinerary>> = _allTrips.asStateFlow()

    private val _eventOptions = MutableStateFlow<Map<String, List<EventOption>>>(emptyMap())
    val eventOptions: StateFlow<Map<String, List<EventOption>>> = _eventOptions.asStateFlow()

    private val _rejectedOptions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val rejectedOptions: StateFlow<Map<String, Set<String>>> = _rejectedOptions.asStateFlow()

    private val _optionsLoading = MutableStateFlow<Set<String>>(emptySet())
    val optionsLoading: StateFlow<Set<String>> = _optionsLoading.asStateFlow()

    private val _yelpReviews = MutableStateFlow<Map<String, List<YelpReview>>>(emptyMap())
    val yelpReviews: StateFlow<Map<String, List<YelpReview>>> = _yelpReviews.asStateFlow()

    private val _reviewsLoading = MutableStateFlow<Set<String>>(emptySet())
    val reviewsLoading: StateFlow<Set<String>> = _reviewsLoading.asStateFlow()
    private val _yelpEnrichmentInFlight = MutableStateFlow<Set<String>>(emptySet())

    private val _shareTargets = MutableStateFlow<List<ShareTarget>>(emptyList())
    val shareTargets: StateFlow<List<ShareTarget>> = _shareTargets.asStateFlow()

    private val _tripMembers = MutableStateFlow<List<TripMemberUi>>(emptyList())
    val tripMembers: StateFlow<List<TripMemberUi>> = _tripMembers.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tripRepository: TripRepository = FirestoreTripRepository(db)
    private val tripLocalDataSource = TripLocalDataSource(TravelCentsDatabase.getInstance(application))
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(db)
    private val tripSyncCoordinator = TripSyncCoordinator(
        localDataSource = tripLocalDataSource,
        remoteDataSource = tripSyncRemoteDataSource,
        legacyRemoteRepository = tripRepository
    )
    private val currentTripSyncCoordinator = CurrentTripSyncCoordinator(
        localDataSource = tripLocalDataSource,
        remoteDataSource = tripSyncRemoteDataSource,
        homeSyncCoordinator = tripSyncCoordinator,
        legacyRemoteRepository = tripRepository
    )
    private var currentTripSummaryJob: Job? = null
    private var currentTripEventsJob: Job? = null
    private var currentTripMembersJob: Job? = null
    private var currentTripOptionsJob: Job? = null
    private var allTripsJob: Job? = null
    private var allTripsObserverUid: String? = null
    private var currentTripKey: TripKey? = null
    private var currentTripSummary: Itinerary? = null
    private var currentTripDestination: String = ""
    private var localEventsSnapshot: List<TravelEvent> = emptyList()
    private val mediaDetailPipeline = TripMediaDetailPipeline(application)

    private fun resetTripState(
        isLoading: Boolean = false,
        tripTitle: String = DEFAULT_TRIP_TITLE,
        infoMessage: String? = null,
        errorMessage: String? = null
    ) {
        currentTripSummaryJob?.cancel()
        currentTripSummaryJob = null
        currentTripEventsJob?.cancel()
        currentTripEventsJob = null
        currentTripMembersJob?.cancel()
        currentTripMembersJob = null
        currentTripOptionsJob?.cancel()
        currentTripOptionsJob = null
        currentTripKey = null
        currentTripSummary = null
        currentTripDestination = ""
        localEventsSnapshot = emptyList()
        _events.value = emptyList()
        _tripTitle.value = tripTitle
        _eventOptions.value = emptyMap()
        _rejectedOptions.value = emptyMap()
        _optionsLoading.value = emptySet()
        _yelpReviews.value = emptyMap()
        _reviewsLoading.value = emptySet()
        _yelpEnrichmentInFlight.value = emptySet()
        _shareTargets.value = emptyList()
        _tripMembers.value = emptyList()
        _uiState.value = CurrentTripUiState(
            isLoading = isLoading,
            tripTitle = tripTitle,
            viewerUid = auth.currentUser?.uid,
            infoMessage = infoMessage,
            errorMessage = errorMessage
        )
    }

    private fun observeCurrentTrip(
        viewerUid: String,
        tripKey: TripKey
    ) {
        currentTripKey = tripKey
        TripPerformanceLogger.bindTrip(tripKey.tripId)
        currentTripSummaryJob?.cancel()
        currentTripSummaryJob = viewModelScope.launch {
            tripLocalDataSource.observeTripSummary(viewerUid, tripKey).collect { itinerary ->
                if (itinerary != null) {
                    handleObservedTripSummary(tripKey, itinerary)
                }
            }
        }

        currentTripEventsJob?.cancel()
        currentTripEventsJob = viewModelScope.launch {
            tripLocalDataSource.observeTripEvents(tripKey).collect { events ->
                localEventsSnapshot = sortPlanEvents(events)
                publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
            }
        }

        currentTripMembersJob?.cancel()
        currentTripMembersJob = viewModelScope.launch {
            tripLocalDataSource.observeTripMembers(tripKey).collect { members ->
                _tripMembers.value = members
                    .filterNot { member -> member.memberUid == tripKey.ownerUid && members.size == 1 }
                    .map { member ->
                        val displayName = member.displayName.ifBlank { member.memberUid }
                        TripMemberUi(
                            uid = member.memberUid,
                            displayName = displayName,
                            initial = displayName.firstOrNull { it.isLetter() } ?: '?'
                        )
                    }
            }
        }

        currentTripOptionsJob?.cancel()
        currentTripOptionsJob = viewModelScope.launch {
            tripLocalDataSource.observeTripOptions(tripKey).collect { optionsByEvent ->
                _eventOptions.value = optionsByEvent
                _rejectedOptions.update { current ->
                    current.filterKeys { key -> key in optionsByEvent || localEventsSnapshot.any { it.eventId == key } }
                }
                publishCurrentEvents(localEventsSnapshot, optionsByEvent)
            }
        }
    }

    private fun handleObservedTripSummary(
        tripKey: TripKey,
        itinerary: Itinerary
    ) {
        currentTripSummary = itinerary
        currentTripDestination = itinerary.destination
        val viewerUid = auth.currentUser?.uid
        val accessRole = when {
            viewerUid.isNullOrBlank() -> TripAccessRole.VIEWER
            viewerUid == tripKey.ownerUid -> TripAccessRole.OWNER
            else -> TripAccessRole.fromWireValue(itinerary.roleByUid[viewerUid])
        }
        val canEditTrip = accessRole.canMutateEvents()
        val canManageTrip = accessRole.canManageTrip()
        val storedTripTitle = itinerary.tripName
        val nextTripTitle = resolveTripName(storedTripTitle, currentTripDestination)
        _tripTitle.value = nextTripTitle
        _uiState.update {
            it.copy(
                isLoading = false,
                currentTripId = tripKey.tripId,
                currentTripOwnerUid = tripKey.ownerUid,
                viewerUid = viewerUid,
                accessRole = accessRole,
                canEditTrip = canEditTrip,
                canManageTrip = canManageTrip,
                tripTitle = nextTripTitle,
                destination = currentTripDestination,
                dateFrom = itinerary.dateFrom,
                dateTo = itinerary.dateTo,
                infoMessage = if (localEventsSnapshot.isEmpty()) EMPTY_PLANS_MESSAGE else null,
                errorMessage = null
            )
        }

        if (canManageTrip && nextTripTitle != storedTripTitle.trim()) {
            viewModelScope.launch {
                runCatching {
                    tripSyncRemoteDataSource.updateTripSummaryFields(
                        tripKey = tripKey,
                        fields = mapOf("tripName" to nextTripTitle)
                    )
                }
            }
        }
    }

    private fun publishCurrentEvents(
        baseEvents: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ) {
        val enrichedEvents = mediaDetailPipeline.applySelectedOptions(
            events = baseEvents,
            optionsByEvent = optionsByEvent,
            sortEvents = ::sortPlanEvents
        )
        _events.value = enrichedEvents
        _uiState.update {
            it.copy(
                isLoading = currentTripSummary == null && enrichedEvents.isEmpty(),
                events = enrichedEvents,
                infoMessage = when {
                    currentTripSummary == null -> it.infoMessage
                    enrichedEvents.isEmpty() -> EMPTY_PLANS_MESSAGE
                    it.infoMessage == EMPTY_PLANS_MESSAGE -> null
                    else -> it.infoMessage
                },
                errorMessage = null
            )
        }
        prefetchSharedEventMedia(enrichedEvents)
        prefetchYelpEnrichment(enrichedEvents)
    }

    private fun refreshCurrentTrip(
        viewerUid: String,
        tripKey: TripKey,
        missingTripMessage: String = "That trip is no longer available."
    ) {
        viewModelScope.launch {
            try {
                val refreshedSummary = currentTripSyncCoordinator.refreshTrip(
                    viewerUid = viewerUid,
                    tripKey = tripKey
                )
                if (refreshedSummary == null) {
                    resetTripState(infoMessage = missingTripMessage)
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "DATABASE ERROR: ${e.message}", e)
                if (currentTripSummary == null) {
                    resetTripState(errorMessage = e.message ?: "Failed to load trip.")
                } else {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to refresh trip.") }
                }
            }
        }
    }

    private fun refreshCurrentTripInBackground(tripKey: TripKey) {
        val viewerUid = auth.currentUser?.uid ?: return
        refreshCurrentTrip(viewerUid = viewerUid, tripKey = tripKey)
    }

    private fun refreshHomeTripCacheInBackground() {
        val viewerUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                tripSyncCoordinator.refreshHomeIfNeeded(viewerUid)
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to refresh local home trip cache", error)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun postError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun upsertPlan(plan: EditablePlan) {
        val tripKey = requireTripContributorKey("add or edit plans")
        if (auth.currentUser?.uid == null) {
            postError("Create or load a trip before adding plans.")
            return
        }
        if (tripKey == null) return

        viewModelScope.launch {
            try {
                val eventId = plan.eventId ?: UUID.randomUUID().toString()
                val mergedDetails = plan.existingDetails.toMutableMap().apply {
                    put("title", plan.title.trim())
                    put("colorKey", plan.colorKey)

                    if (plan.location.isBlank()) {
                        remove("location")
                    } else {
                        put("location", plan.location.trim())
                    }

                    if (plan.notes.isBlank()) {
                        remove("description")
                    } else {
                        put("description", plan.notes.trim())
                    }
                }

                val event = TravelEvent(
                    eventId = eventId,
                    type = plan.type,
                    itineraryId = tripKey.tripId,
                    tz = plan.timeZoneId.trim().ifBlank { defaultPlanTimeZoneId() },
                    date = normalizeDate(plan.date),
                    startTime = normalizeTime(plan.startTime),
                    endTime = normalizeTime(plan.endTime),
                    details = mergedDetails
                )

                localEventsSnapshot = sortPlanEvents(
                    localEventsSnapshot
                        .filterNot { existing -> existing.eventId == eventId }
                        .plus(event)
                )
                publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
                persistLocalTripSnapshot(events = localEventsSnapshot)
                tripSyncRemoteDataSource.upsertEvent(tripKey = tripKey, event = event)
                refreshCurrentTripInBackground(tripKey)

                _uiState.update {
                    it.copy(
                        infoMessage = if (plan.eventId == null) "Plan added to your trip." else "Plan updated.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to save event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save plan.") }
            }
        }
    }

    fun deletePlan(plan: EditablePlan) {
        val tripKey = requireTripContributorKey("delete plans")
        val eventId = plan.eventId

        if (auth.currentUser?.uid == null || eventId.isNullOrBlank()) {
            postError("This plan cannot be deleted yet.")
            return
        }
        if (tripKey == null) return

        viewModelScope.launch {
            try {
                localEventsSnapshot = sortPlanEvents(
                    localEventsSnapshot.filterNot { existing -> existing.eventId == eventId }
                )
                val updatedOptions = _eventOptions.value - eventId
                _eventOptions.value = updatedOptions
                _rejectedOptions.update { current -> current - eventId }
                publishCurrentEvents(localEventsSnapshot, updatedOptions)
                persistLocalTripSnapshot(
                    events = localEventsSnapshot,
                    options = updatedOptions,
                    persistOptions = true
                )
                tripSyncRemoteDataSource.deleteEvent(tripKey = tripKey, eventId = eventId)
                refreshCurrentTripInBackground(tripKey)

                _uiState.update {
                    it.copy(
                        infoMessage = "Plan deleted.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to delete event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete plan.") }
            }
        }
    }

    fun fetchYelpReviews(yelpId: String) {
        if (yelpId.isBlank() || yelpId in _reviewsLoading.value || yelpId in _yelpReviews.value) return

        viewModelScope.launch {
            _reviewsLoading.update { it + yelpId }
            val reviews = YelpRepository.getBusinessReviews(yelpId)
            _yelpReviews.update { it + (yelpId to reviews) }
            _reviewsLoading.update { it - yelpId }
        }
    }

    fun ensureYelpEventEnriched(
        eventId: String,
        forceRefresh: Boolean = false
    ) {
        val tripKey = currentTripWriteKeyIfOwner() ?: return
        val event = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return
        val options = _eventOptions.value[eventId].orEmpty()
        val yelpId = mediaDetailPipeline.yelpEnrichmentTargetId(
            event = event,
            options = options,
            inFlightIds = _yelpEnrichmentInFlight.value,
            forceRefresh = forceRefresh
        )
            ?: return

        _yelpEnrichmentInFlight.update { it + yelpId }
        TripPerformanceLogger.recordYelpEnrichmentAttempt(
            source = "CurrentTripViewModel.ensureYelpEventEnriched",
            detail = "tripId=${tripKey.tripId} eventId=$eventId type=${event.type}"
        )
        viewModelScope.launch {
            try {
                val result = YelpRepository.enrichYelpBackedEvent(
                    event = event,
                    options = options,
                    forceRefresh = forceRefresh
                ) ?: return@launch

                applyEnrichedEventState(eventId, result.event, result.options)
                persistYelpEnrichment(tripKey, eventId, result)
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to enrich Yelp event", e)
            } finally {
                _yelpEnrichmentInFlight.update { it - yelpId }
            }
        }
    }

    private fun prefetchYelpEnrichment(events: List<TravelEvent>) {
        if (currentTripWriteKeyIfOwner() == null) return
        events.forEach { event ->
            if (event.type.lowercase(Locale.US) !in YELP_PREFETCH_TYPES) return@forEach
            ensureYelpEventEnriched(event.eventId)
        }
    }

    private fun applyEnrichedEventState(
        eventId: String,
        enrichedEvent: TravelEvent,
        enrichedOptions: List<EventOption>
    ) {
        val mergedEvent = mediaDetailPipeline.mergeEventWithOptions(enrichedEvent, enrichedOptions)

        localEventsSnapshot = sortPlanEvents(
            localEventsSnapshot.map { event ->
                if (event.eventId == eventId) mergedEvent else event
            }
        )

        val updatedOptions = _eventOptions.value + (eventId to enrichedOptions)
        _eventOptions.value = updatedOptions
        publishCurrentEvents(localEventsSnapshot, updatedOptions)
        persistLocalTripSnapshot(
            events = localEventsSnapshot,
            options = updatedOptions,
            persistOptions = true
        )
    }

    private suspend fun persistYelpEnrichment(
        tripKey: TripKey,
        eventId: String,
        result: YelpRepository.YelpEventEnrichmentResult
    ) {
        tripSyncRemoteDataSource.persistEventAndOptions(
            tripKey = tripKey,
            eventId = eventId,
            event = result.event,
            options = result.options,
            updatedOptionIds = result.updatedOptionIds
        )
        persistLocalTripSnapshot(
            tripKey = tripKey,
            events = localEventsSnapshot,
            options = _eventOptions.value,
            persistOptions = true
        )
        refreshCurrentTripInBackground(tripKey)
    }

    fun ensureEventOptionsLoaded(eventId: String) {
        val tripKey = currentTripKey ?: return
        val summary = currentTripSummary ?: return
        viewModelScope.launch {
            if (eventId in _optionsLoading.value) return@launch
            try {
                val localVersion = tripLocalDataSource.getTripOptionsVersionGroup(tripKey)
                if (localVersion != null && localVersion == summary.optionsVersion) {
                    return@launch
                }

                _optionsLoading.update { it + eventId }
                currentTripSyncCoordinator.hydrateOptionsIfNeeded(
                    tripKey = tripKey,
                    expectedOptionsVersion = summary.optionsVersion
                )
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to load options", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load options.") }
            } finally {
                _optionsLoading.update { it - eventId }
            }
        }
    }

    private fun persistLocalTripSnapshot(
        tripKey: TripKey? = currentTripKey,
        events: List<TravelEvent> = localEventsSnapshot,
        options: Map<String, List<EventOption>> = _eventOptions.value,
        persistOptions: Boolean = false
    ) {
        val resolvedTripKey = tripKey ?: return
        val viewerUid = auth.currentUser?.uid ?: return
        val summary = currentTripSummary ?: return
        val updatedSummary = summary.copy(eventIds = events.map(TravelEvent::eventId))
        currentTripSummary = updatedSummary
        viewModelScope.launch {
            runCatching {
                tripLocalDataSource.upsertTripSummary(
                    viewerUid = viewerUid,
                    itinerary = updatedSummary,
                    isCurrentCandidate = true
                )
                tripLocalDataSource.replaceTripEvents(
                    tripKey = resolvedTripKey,
                    events = events,
                    eventVersionGroup = updatedSummary.eventsVersion.takeIf { it > 0 } ?: System.currentTimeMillis()
                )
                if (persistOptions) {
                    tripLocalDataSource.replaceTripOptions(
                        tripKey = resolvedTripKey,
                        optionsByEvent = options,
                        optionsVersionGroup = updatedSummary.optionsVersion.takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                }
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to update local current-trip cache", error)
            }
        }
    }

    fun fetchShareTargets() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val groupTargets = db.collection("groups")
                    .whereArrayContains("members", uid)
                    .get()
                    .await()
                    .documents
                    .map { doc ->
                        val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        ShareTarget(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unnamed Group",
                            isGroup = true,
                            memberUids = members
                        )
                    }

                val directChatDocs = db.collection("directChats")
                    .whereArrayContains("members", uid)
                    .get()
                    .await()
                    .documents

                val chatEntries = directChatDocs.mapNotNull { doc ->
                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val otherUid = members.firstOrNull { it != uid } ?: return@mapNotNull null
                    doc.id to otherUid
                }

                val userNames = fetchUserNames(chatEntries.map { it.second }.distinct())
                val directTargets = chatEntries.map { (chatId, otherUid) ->
                    val members = directChatDocs
                        .firstOrNull { doc -> doc.id == chatId }
                        ?.get("members")
                        .let { raw -> (raw as? List<*>)?.filterIsInstance<String>().orEmpty() }
                    ShareTarget(
                        id = chatId,
                        name = userNames[otherUid] ?: "Unknown",
                        isGroup = false,
                        memberUids = members
                    )
                }

                _shareTargets.value = (groupTargets + directTargets).sortedBy { it.name.lowercase(Locale.US) }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to fetch share targets", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load chats.") }
            }
        }
    }

    private suspend fun fetchUserNames(uids: List<String>): Map<String, String> {
        if (uids.isEmpty()) return emptyMap()

        return uids.chunked(30).flatMap { chunk ->
            db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .map { doc ->
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    doc.id to "$first $last".trim().ifBlank { "Unknown" }
                }
        }.toMap()
    }

    private fun currentTripWriteKey(): TripKey? {
        val tripId = _uiState.value.currentTripId ?: return null
        val ownerUid = currentTripKey?.ownerUid ?: auth.currentUser?.uid ?: return null
        return TripKey(ownerUid = ownerUid, tripId = tripId)
    }

    private fun currentTripWriteKeyIfOwner(): TripKey? {
        val tripKey = currentTripWriteKey() ?: return null
        return tripKey.takeIf { key -> key.ownerUid == auth.currentUser?.uid }
    }

    private fun currentTripWriteKeyIfContributor(): TripKey? {
        return currentTripWriteKey()?.takeIf { _uiState.value.canEditTrip }
    }

    private fun requireTripContributorKey(action: String): TripKey? {
        currentTripWriteKeyIfContributor()?.let { return it }
        if (currentTripWriteKey() != null) {
            postError("You do not have permission to $action.")
        }
        return null
    }

    private fun requireOwnerTripKey(action: String): TripKey? {
        currentTripWriteKeyIfOwner()?.let { return it }
        if (currentTripWriteKey() != null) {
            postError("Only the trip owner can $action.")
        }
        return null
    }

    private fun resolveSenderDisplayName(): String {
        val currentUser = auth.currentUser ?: return "Traveler"
        val authDisplayName = currentUser.displayName?.trim().orEmpty()
        if (authDisplayName.isNotBlank()) return authDisplayName

        val emailDisplayName = currentUser.email
            ?.substringBefore('@')
            ?.replace('.', ' ')
            ?.replace('_', ' ')
            ?.trim()
            .orEmpty()
        if (emailDisplayName.isNotBlank()) return emailDisplayName

        return "Traveler"
    }

    fun shareTripToChat(target: ShareTarget) {
        val uid = auth.currentUser?.uid ?: return
        val tripKey = requireOwnerTripKey("share this trip") ?: return

        viewModelScope.launch {
            try {
                tripRepository.ensureTripAccess(
                    key = tripKey,
                    memberUids = target.memberUids
                )

                val senderName = resolveSenderDisplayName()
                val container = if (target.isGroup) "groups" else "directChats"
                val chatRef = db.collection(container).document(target.id)
                val messageRef = chatRef.collection("messages").document()
                val coverImage = _uiState.value.events.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl
                val message = hashMapOf(
                    "text" to "Shared trip: ${_uiState.value.tripTitle}",
                    "senderId" to uid,
                    "senderName" to senderName,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "messageType" to "trip_card",
                    "sharedTripId" to tripKey.tripId,
                    "ownerUid" to tripKey.ownerUid,
                    "tripName" to _uiState.value.tripTitle,
                    "tripDestination" to currentTripDestination,
                    "tripDateFrom" to _uiState.value.dateFrom,
                    "tripDateTo" to _uiState.value.dateTo,
                    "coverImageUrl" to coverImage
                )

                db.runBatch { batch ->
                    batch.set(messageRef, message)
                    batch.update(
                        chatRef,
                        mapOf(
                            "lastMessage" to "Shared a trip",
                            "lastMessageTime" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()
                refreshCurrentTripInBackground(tripKey)
                refreshHomeTripCacheInBackground()
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to share trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to share trip.") }
            }
        }
    }

    fun selectOption(eventId: String, optionId: String) {
        val tripKey = requireTripContributorKey("change selected options") ?: return
        val options = _eventOptions.value[eventId].orEmpty()
        val selectedOption = options.firstOrNull { it.optionId == optionId } ?: return
        val event = localEventsSnapshot.firstOrNull { it.eventId == eventId }
            ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            ?: return

        val updatedOptions = options.map { option ->
            option.copy(selected = option.optionId == optionId)
                .scopedTo(
                    ownerUid = tripKey.ownerUid,
                    tripId = tripKey.tripId,
                    eventId = eventId
                )
        }
        val updatedEvent = mediaDetailPipeline.mergeEventWithOptions(event, updatedOptions)
        localEventsSnapshot = sortPlanEvents(
            localEventsSnapshot.map {
                if (it.eventId == eventId) updatedEvent else it
            }
        )

        val updatedOptionsByEvent = _eventOptions.value + (eventId to updatedOptions)
        _eventOptions.value = updatedOptionsByEvent
        _rejectedOptions.update { current ->
            val nextRejected = current[eventId].orEmpty() - optionId
            current + (eventId to nextRejected)
        }
        publishCurrentEvents(localEventsSnapshot, updatedOptionsByEvent)
        persistLocalTripSnapshot(
            events = localEventsSnapshot,
            options = updatedOptionsByEvent,
            persistOptions = true
        )
        prefetchSharedEventMedia(listOf(updatedEvent))

        viewModelScope.launch {
            try {
                tripSyncRemoteDataSource.persistEventAndOptions(
                    tripKey = tripKey,
                    eventId = eventId,
                    event = updatedEvent,
                    options = updatedOptions
                )
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to select option", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update selection.") }
                return@launch
            }

            refreshCurrentTripInBackground(tripKey)
            ensureYelpEventEnriched(eventId)
        }
    }

    fun rejectOption(eventId: String, optionId: String) {
        if (currentTripWriteKeyIfContributor() == null) {
            postError("You do not have permission to change options.")
            return
        }
        _rejectedOptions.update { current ->
            current + (eventId to (current[eventId].orEmpty() + optionId))
        }
    }

    fun patchEventFields(
        eventId: String,
        title: String,
        startTime: String,
        notes: String
    ) {
        val tripKey = requireTripContributorKey("edit plans") ?: return
        val currentEvent = localEventsSnapshot.firstOrNull { it.eventId == eventId }
            ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            ?: return

        val updatedDetails = currentEvent.details.toMutableMap().apply {
            val trimmedTitle = title.trim()
            val trimmedNotes = notes.trim()
            if (trimmedTitle.isBlank()) {
                remove("title")
            } else {
                put("title", trimmedTitle)
            }
            if (trimmedNotes.isBlank()) {
                remove("description")
                remove("notes")
            } else {
                put("description", trimmedNotes)
            }
        }

        val updatedEvent = currentEvent.copy(
            startTime = normalizeTime(startTime),
            details = updatedDetails
        )
        localEventsSnapshot = sortPlanEvents(
            localEventsSnapshot.map { if (it.eventId == eventId) updatedEvent else it }
        )
        publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
        persistLocalTripSnapshot(events = localEventsSnapshot)

        viewModelScope.launch {
            try {
                tripSyncRemoteDataSource.upsertEvent(tripKey = tripKey, event = updatedEvent)
                refreshCurrentTripInBackground(tripKey)
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to patch event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save event changes.") }
            }
        }
    }

    fun moveEventLocally(
        eventId: String,
        fromDate: String,
        toDate: String,
        toIndex: Int
    ) {
        val currentEvents = localEventsSnapshot
        val movingEvent = currentEvents.firstOrNull { it.eventId == eventId } ?: return
        val normalizedFromDate = normalizeDate(fromDate)
        val normalizedToDate = normalizeDate(toDate)

        val grouped = currentEvents
            .groupBy { normalizeDate(it.date) }
            .mapValues { (_, events) ->
                events.sortedWith(
                    compareBy(
                        { it.details["sortOrder"]?.toIntOrNull() ?: 0 },
                        { normalizeTime(it.startTime) }
                    )
                ).toMutableList()
            }
            .toMutableMap()

        val sourceList = grouped[normalizedFromDate] ?: mutableListOf()
        sourceList.removeAll { it.eventId == eventId }

        val targetList = if (normalizedToDate == normalizedFromDate) {
            sourceList
        } else {
            grouped.getOrPut(normalizedToDate) { mutableListOf() }
        }

        val insertionIndex = toIndex.coerceIn(0, targetList.size)
        targetList.add(insertionIndex, movingEvent.copy(date = normalizedToDate))

        localEventsSnapshot = grouped
            .toSortedMap(compareBy<String> { if (it.isBlank()) "9999-12-31" else it })
            .values
            .flatMap { dayEvents ->
                dayEvents.mapIndexed { index, event ->
                    event.copy(
                        details = event.details.toMutableMap().apply {
                            put("sortOrder", index.toString())
                        }
                    )
                }
            }

        publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
    }

    fun persistEventPlacements(affectedDates: Set<String>) {
        val tripKey = requireTripContributorKey("reorder plans") ?: return
        val normalizedDates = affectedDates.map(::normalizeDate).toSet()
        val affectedEvents = _uiState.value.events.filter { normalizeDate(it.date) in normalizedDates }

        if (affectedEvents.isEmpty()) return

        viewModelScope.launch {
            try {
                persistLocalTripSnapshot(events = localEventsSnapshot)
                tripSyncRemoteDataSource.persistEventPlacements(
                    tripKey = tripKey,
                    events = affectedEvents.map { event ->
                        event.copy(date = normalizeDate(event.date))
                    }
                )
                refreshCurrentTripInBackground(tripKey)
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to persist placements", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save event order.") }
            }
        }
    }

    fun archiveTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentTripId = _uiState.value.currentTripId
        val tripKey = requireOwnerTripKey("archive this trip") ?: return
        if (tripKey.tripId != tripId) {
            postError("Load the trip again before archiving it.")
            return
        }

        viewModelScope.launch {
            try {
                tripSyncRemoteDataSource.updateTripSummaryFields(
                    tripKey = tripKey,
                    fields = mapOf(
                        "status" to "archived",
                        "archivedAt" to FieldValue.serverTimestamp()
                    )
                )

                _allTrips.update { trips ->
                    trips.map {
                        if (it.itineraryId == tripId) it.copy(status = "archived") else it
                    }
                }
                currentTripSummary = currentTripSummary?.copy(status = "archived")
                currentTripSummary?.let { summary ->
                    tripLocalDataSource.upsertTripSummary(
                        viewerUid = uid,
                        itinerary = summary,
                        isCurrentCandidate = true
                    )
                }
                _uiState.update { it.copy(infoMessage = "Trip archived.", errorMessage = null) }
                refreshHomeTripCacheInBackground()

                if (currentTripId == tripId) {
                    loadTrip()
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to archive trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to archive trip.") }
            }
        }
    }

    fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripKey = requireOwnerTripKey("delete this trip") ?: return
        if (tripKey.tripId != tripId) {
            postError("Load the trip again before deleting it.")
            return
        }

        viewModelScope.launch {
            try {
                val remaining = _allTrips.value.filterNot { trip ->
                    trip.itineraryId == tripKey.tripId && trip.ownerUid == tripKey.ownerUid
                }
                _allTrips.value = remaining

                tripRepository.deleteTrip(tripKey)
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        ImageCacheManager.deleteTripImages(getApplication(), tripKey.tripId)
                    }.onFailure { error ->
                        Log.w("CurrentTripViewModel", "Failed to clear cached trip media", error)
                    }
                }
                refreshHomeTripCacheInBackground()

                val nextTrip = remaining.firstOrNull {
                    !it.status.equals("archived", ignoreCase = true)
                }
                if (nextTrip != null) {
                    loadTrip(TripKey(ownerUid = nextTrip.ownerUid, tripId = nextTrip.itineraryId))
                } else {
                    resetTripState(infoMessage = NO_TRIP_MESSAGE)
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to delete trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete trip.") }
            }
        }
    }

    fun loadAllTrips() {
        val uid = auth.currentUser?.uid ?: return

        if (allTripsObserverUid != uid || allTripsJob == null) {
            allTripsJob?.cancel()
            allTripsObserverUid = uid
            allTripsJob = viewModelScope.launch {
                tripLocalDataSource.observeHomeTripSummaries(uid).collect { trips ->
                    _allTrips.value = trips
                }
            }
        }

        refreshHomeTripCacheInBackground()
    }

    fun renameTrip(newName: String) {
        val tripKey = requireOwnerTripKey("rename this trip") ?: return
        val trimmed = newName.trim().ifBlank { return }
        val viewerUid = auth.currentUser?.uid ?: return

        _tripTitle.value = trimmed
        _uiState.update { it.copy(tripTitle = trimmed) }
        _allTrips.update { trips ->
            trips.map { if (it.itineraryId == tripKey.tripId) it.copy(tripName = trimmed) else it }
        }

        viewModelScope.launch {
            try {
                tripSyncRemoteDataSource.updateTripSummaryFields(
                    tripKey = tripKey,
                    fields = mapOf("tripName" to trimmed)
                )
                currentTripSummary = currentTripSummary?.copy(tripName = trimmed)
                currentTripSummary?.let { summary ->
                    tripLocalDataSource.upsertTripSummary(
                        viewerUid = viewerUid,
                        itinerary = summary,
                        isCurrentCandidate = true
                    )
                }
                refreshHomeTripCacheInBackground()
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to rename trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to rename trip.") }
            }
        }
    }

    fun loadTrip(tripId: String? = null) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Log.e("CurrentTripViewModel", "UID is NULL. Firebase isn't ready yet.")
            resetTripState(
                infoMessage = "Log in to load your current trip."
            )
            return
        }

        if (tripId != null) {
            loadTrip(TripKey(ownerUid = uid, tripId = tripId))
            return
        }

        resetTripState(isLoading = true)
        Log.d("CurrentTripViewModel", "UID found: $uid. Fetching trip: Latest")
        TripPerformanceLogger.beginTripLoad(
            trigger = "load_latest_trip",
            requestedTripId = null
        )
        viewModelScope.launch {
            try {
                val latestTripKey = currentTripSyncCoordinator.resolveLatestTripKey(uid)
                if (latestTripKey == null) {
                    Log.d("CurrentTripViewModel", "No active trips found.")
                    resetTripState(infoMessage = NO_TRIP_MESSAGE)
                    return@launch
                }

                observeCurrentTrip(uid, latestTripKey)
                refreshCurrentTrip(
                    viewerUid = uid,
                    tripKey = latestTripKey,
                    missingTripMessage = NO_TRIP_MESSAGE
                )
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "DATABASE ERROR: ${e.message}", e)
                resetTripState(errorMessage = e.message ?: "Failed to load trip.")
            }
        }
    }

    fun loadTrip(tripKey: TripKey) {
        val viewerUid = auth.currentUser?.uid
        if (viewerUid == null) {
            Log.e("CurrentTripViewModel", "UID is NULL. Firebase isn't ready yet.")
            resetTripState(
                infoMessage = "Log in to load your current trip."
            )
            return
        }

        resetTripState(isLoading = true)
        Log.d(
            "CurrentTripViewModel",
            "Loading trip by key: ownerUid=${tripKey.ownerUid}, tripId=${tripKey.tripId}"
        )
        TripPerformanceLogger.beginTripLoad(
            trigger = "load_trip_key",
            requestedTripId = tripKey.tripId
        )
        observeCurrentTrip(viewerUid, tripKey)
        refreshCurrentTrip(viewerUid = viewerUid, tripKey = tripKey)
    }

    private fun prefetchSharedEventMedia(events: List<TravelEvent>) {
        viewModelScope.launch {
            runCatching {
                mediaDetailPipeline.prefetchSharedMedia(events)
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to prefetch shared event media", error)
            }
        }
    }

    private fun sortPlanEvents(events: List<TravelEvent>): List<TravelEvent> {
        return events.sortedWith(
            compareBy<TravelEvent>(
                { normalizeDate(it.date) },
                { it.details["sortOrder"]?.toIntOrNull() ?: Int.MAX_VALUE },
                { normalizeTime(it.startTime) },
                { it.eventId }
            )
        )
    }

    override fun onCleared() {
        currentTripSummaryJob?.cancel()
        currentTripEventsJob?.cancel()
        currentTripMembersJob?.cancel()
        currentTripOptionsJob?.cancel()
        allTripsJob?.cancel()
        super.onCleared()
    }
}

