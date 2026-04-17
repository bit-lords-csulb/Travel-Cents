package com.example.travelcents.ui.main.itinerary

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpOptionPoolItem
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_ACTIVITIES
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_RESTAURANTS
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.ui.main.current.CurrentTripUiState
import com.example.travelcents.ui.main.shared.TripMediaDetailPipeline
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.normalizeTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ShareTarget(
    val id: String,
    val name: String,
    val isGroup: Boolean
)

class ItineraryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val DEFAULT_TRIP_TITLE = "Loading Trip..."
        private const val EMPTY_PLANS_MESSAGE = "No plans yet. Tap + to add one."
        private const val SHARED_YELP_VISIBLE_OPTIONS = 5
        private const val SHARED_YELP_POOL_EXPANSION_SIZE = 10
    }

    private val _uiState = MutableStateFlow(CurrentTripUiState())
    val uiState: StateFlow<CurrentTripUiState> = _uiState.asStateFlow()

    private val _eventOptions = MutableStateFlow<Map<String, List<EventOption>>>(emptyMap())
    val eventOptions: StateFlow<Map<String, List<EventOption>>> = _eventOptions.asStateFlow()

    private val _rejectedOptions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val rejectedOptions: StateFlow<Map<String, Set<String>>> = _rejectedOptions.asStateFlow()

    private val _yelpReviews = MutableStateFlow<Map<String, List<YelpReview>>>(emptyMap())
    val yelpReviews: StateFlow<Map<String, List<YelpReview>>> = _yelpReviews.asStateFlow()

    private val _reviewsLoading = MutableStateFlow<Set<String>>(emptySet())
    val reviewsLoading: StateFlow<Set<String>> = _reviewsLoading.asStateFlow()
    private val _yelpEnrichmentInFlight = MutableStateFlow<Set<String>>(emptySet())

    private val _shareTargets = MutableStateFlow<List<ShareTarget>>(emptyList())
    val shareTargets: StateFlow<List<ShareTarget>> = _shareTargets.asStateFlow()

    private val _allTrips = MutableStateFlow<List<Itinerary>>(emptyList())
    val allTrips: StateFlow<List<Itinerary>> = _allTrips.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(db)
    private var eventsListener: ListenerRegistration? = null
    private var currentTripDestination: String = ""
    private val sharedYelpPools = mutableMapOf<String, List<YelpOptionPoolItem>>()
    private val sharedYelpWindowBoost = mutableMapOf<String, Int>()
    private val mediaDetailPipeline = TripMediaDetailPipeline(application)

    private fun resetTripState(
        isLoading: Boolean = false,
        tripTitle: String = DEFAULT_TRIP_TITLE,
        infoMessage: String? = null,
        errorMessage: String? = null
    ) {
        eventsListener?.remove()
        eventsListener = null
        currentTripDestination = ""
        sharedYelpPools.clear()
        sharedYelpWindowBoost.clear()
        _eventOptions.value = emptyMap()
        _rejectedOptions.value = emptyMap()
        _yelpReviews.value = emptyMap()
        _reviewsLoading.value = emptySet()
        _yelpEnrichmentInFlight.value = emptySet()
        _shareTargets.value = emptyList()
        _uiState.value = CurrentTripUiState(
            isLoading = isLoading,
            tripTitle = tripTitle,
            infoMessage = infoMessage,
            errorMessage = errorMessage
        )
    }

    private fun fetchLatestItinerary(uid: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { tripSnapshot ->
                if (tripSnapshot.isEmpty) {
                    resetTripState(
                        infoMessage = "No trip found yet. Create one from the New Trip tab."
                    )
                    return@addOnSuccessListener
                }

                handleTripDocument(uid, tripSnapshot.documents.first())
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "DATABASE ERROR: ${e.message}")
                resetTripState(errorMessage = e.message ?: "Failed to load trip.")
            }
    }

    private fun fetchTrip(uid: String, tripId: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    resetTripState(infoMessage = "That trip is no longer available.")
                    return@addOnSuccessListener
                }

                handleTripDocument(uid, document)
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "DATABASE ERROR: ${e.message}")
                resetTripState(errorMessage = e.message ?: "Failed to load trip.")
            }
    }

    private fun handleTripDocument(uid: String, document: DocumentSnapshot) {
        currentTripDestination = document.getString("destination") ?: ""
        _uiState.update {
            it.copy(
                isLoading = false,
                currentTripId = document.id,
                tripTitle = document.getString("tripName") ?: "Unnamed Trip",
                dateFrom = document.getString("dateFrom") ?: "",
                dateTo = document.getString("dateTo") ?: "",
                infoMessage = null,
                errorMessage = null
            )
        }

        listenToEvents(uid, document.id)
    }

    private fun listenToEvents(uid: String, tripId: String) {
        eventsListener?.remove()
        eventsListener = db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ItineraryViewModel", "Listen failed.", error)
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to load plans.") }
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                val fetchedEvents = docs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    TravelEvent.fromFirestoreMap(
                        map = data,
                        documentId = doc.id,
                        fallbackItineraryId = tripId
                    )
                }

                val sortedEvents = sortPlanEvents(fetchedEvents)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        events = sortedEvents,
                        infoMessage = when {
                            sortedEvents.isEmpty() && it.infoMessage.isNullOrBlank() -> EMPTY_PLANS_MESSAGE
                            sortedEvents.isNotEmpty() && it.infoMessage == EMPTY_PLANS_MESSAGE -> null
                            else -> it.infoMessage
                        },
                        errorMessage = null
                    )
                }

                viewModelScope.launch {
                    val optionsByEvent = loadOptionsForEvents(uid, tripId, sortedEvents)
                    val alignedOptionsByEvent = alignEventOptionsWithSelectedState(sortedEvents, optionsByEvent)
                    val enrichedEvents = mediaDetailPipeline.applySelectedOptions(
                        events = sortedEvents,
                        optionsByEvent = alignedOptionsByEvent,
                        sortEvents = ::sortPlanEvents
                    )
                    _eventOptions.value = alignedOptionsByEvent
                    _rejectedOptions.update { current ->
                        current.filterKeys { key -> alignedOptionsByEvent.containsKey(key) }
                    }
                    _uiState.update { state ->
                        state.copy(events = enrichedEvents)
                    }
                    prefetchSharedEventMedia(enrichedEvents)
                }
            }
    }

    private suspend fun loadOptionsForEvents(
        uid: String,
        tripId: String,
        events: List<TravelEvent>
    ): Map<String, List<EventOption>> {
        if (events.isEmpty()) return emptyMap()

        return events.map { event ->
            viewModelScope.async {
                val options = loadSharedYelpOptions(uid, tripId, event)
                    ?: db.collection("users")
                        .document(uid)
                        .collection("trips")
                        .document(tripId)
                        .collection("events")
                        .document(event.eventId)
                        .collection("options")
                        .get()
                        .await()
                        .documents
                        .map { doc ->
                            val raw = doc.data ?: emptyMap()
                            EventOption.fromMap(
                                raw + mapOf(
                                    "optionId" to (raw["optionId"]?.toString() ?: doc.id),
                                    "eventId" to event.eventId
                                )
                            )
                        }
                        .sortedByDescending { it.selected }
                event.eventId to options
            }
        }.awaitAll().toMap()
    }

    private suspend fun loadSharedYelpOptions(
        uid: String,
        tripId: String,
        event: TravelEvent
    ): List<EventOption>? {
        val poolType = yelpPoolTypeForEvent(event) ?: return null
        val tripKey = TripKey(ownerUid = uid, tripId = tripId)
        val initialPoolItems = sharedYelpPools[poolType] ?: tripSyncRemoteDataSource
            .fetchYelpOptionPool(tripKey, poolType)
            .also { items ->
                if (items.isNotEmpty()) {
                    sharedYelpPools[poolType] = items
                }
            }
        if (initialPoolItems.isEmpty()) return null

        val poolItems = maybeExpandSharedYelpPool(
            uid = uid,
            tripId = tripId,
            event = event,
            poolType = poolType,
            poolItems = initialPoolItems,
            rejectedIds = _rejectedOptions.value[event.eventId].orEmpty()
        )
        return synthesizeSharedYelpOptions(
            tripId = tripId,
            event = event,
            poolItems = poolItems,
            rejectedIds = _rejectedOptions.value[event.eventId].orEmpty()
        )
    }

    private fun synthesizeSharedYelpOptions(
        tripId: String,
        event: TravelEvent,
        poolItems: List<YelpOptionPoolItem>,
        rejectedIds: Set<String>
    ): List<EventOption> {
        val selectedProviderId = event.selectedOptionId
            .takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)
        val orderedPool = YelpRepository.orderedPoolItemsForEvent(
            pool = poolItems,
            tripId = tripId,
            eventId = event.eventId,
            selectedProviderId = selectedProviderId
        )
        val orderedAlternatives = orderedPool.filterNot { item -> item.providerId == selectedProviderId }
        val visibleAlternativeCount = SHARED_YELP_VISIBLE_OPTIONS +
            rejectedIds.count { rejectedId -> orderedAlternatives.any { item -> item.providerId == rejectedId } } +
            (sharedYelpWindowBoost[event.eventId] ?: 0)
        val synthesizedOptions = orderedAlternatives
            .take(visibleAlternativeCount)
            .map { item ->
                item.toEventOption(
                    eventId = event.eventId,
                    selected = false
                )
            }
            .toMutableList()

        if (!selectedProviderId.isNullOrBlank()) {
            synthesizedOptions.add(
                index = 0,
                element = EventOption(
                    optionId = selectedProviderId,
                    eventId = event.eventId,
                    source = "yelp",
                    selected = true,
                    imageUrl = event.imageUrl,
                    localImagePath = event.localImagePath,
                    photoUrls = event.photoUrls,
                    details = event.details
                )
            )
        }

        return synthesizedOptions
            .distinctBy(EventOption::optionId)
            .map { option -> option.copy(selected = option.optionId == selectedProviderId) }
    }

    private suspend fun maybeExpandSharedYelpPool(
        uid: String,
        tripId: String,
        event: TravelEvent,
        poolType: String,
        poolItems: List<YelpOptionPoolItem>,
        rejectedIds: Set<String>
    ): List<YelpOptionPoolItem> {
        val selectedProviderId = event.selectedOptionId
            .takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)
        val currentAlternativeCount = poolItems.count { item -> item.providerId != selectedProviderId }
        val requiredAlternativeCount =
            SHARED_YELP_VISIBLE_OPTIONS + rejectedIds.size + (sharedYelpWindowBoost[event.eventId] ?: 0)
        if (currentAlternativeCount >= requiredAlternativeCount) {
            return poolItems
        }

        val location = currentTripDestination.ifBlank { _uiState.value.destination }.ifBlank { return poolItems }
        val additionalItems = YelpRepository.fetchAdditionalPoolItems(
            location = location,
            poolType = poolType,
            existingProviderIds = poolItems.map(YelpOptionPoolItem::providerId).toSet(),
            targetCount = maxOf(
                SHARED_YELP_POOL_EXPANSION_SIZE,
                requiredAlternativeCount - currentAlternativeCount
            )
        )
        if (additionalItems.isEmpty()) return poolItems

        val mergedItems = (poolItems + additionalItems).distinctBy(YelpOptionPoolItem::providerId)
        tripSyncRemoteDataSource.upsertYelpOptionPoolItems(
            tripKey = TripKey(ownerUid = uid, tripId = tripId),
            poolType = poolType,
            items = additionalItems
        )
        sharedYelpPools[poolType] = mergedItems
        return mergedItems
    }

    private fun alignEventOptionsWithSelectedState(
        events: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ): Map<String, List<EventOption>> {
        if (events.isEmpty() || optionsByEvent.isEmpty()) return optionsByEvent

        val eventById = events.associateBy(TravelEvent::eventId)
        var changed = false
        val alignedOptions = optionsByEvent.mapValues { (eventId, options) ->
            val event = eventById[eventId] ?: return@mapValues options
            val selectedOptionId = event.selectedOptionId
                .takeIf { it.isNotBlank() }
                ?: event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() }
                ?: return@mapValues options

            val normalizedOptions = options.map { option ->
                option.copy(selected = option.optionId == selectedOptionId)
            }.toMutableList()

            if (normalizedOptions.none(EventOption::selected) && yelpPoolTypeForEvent(event) != null) {
                normalizedOptions.add(
                    index = 0,
                    element = EventOption(
                        optionId = selectedOptionId,
                        eventId = eventId,
                        source = "yelp",
                        selected = true,
                        imageUrl = event.imageUrl,
                        localImagePath = event.localImagePath,
                        photoUrls = event.photoUrls,
                        details = event.details
                    )
                )
            }

            val dedupedOptions = normalizedOptions.distinctBy(EventOption::optionId)
            if (dedupedOptions != options) changed = true
            dedupedOptions
        }

        return if (changed) alignedOptions else optionsByEvent
    }

    private fun yelpPoolTypeForEvent(event: TravelEvent): String? {
        val yelpBusinessId = event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() } ?: return null
        return when (event.type.lowercase()) {
            "restaurant", "dining", "food" -> YELP_POOL_TYPE_RESTAURANTS
            "activity" -> YELP_POOL_TYPE_ACTIVITIES
            else -> null
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
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
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
        viewModelScope.launch {
            try {
                val result = YelpRepository.enrichYelpBackedEvent(
                    event = event,
                    options = options,
                    forceRefresh = forceRefresh
                ) ?: return@launch

                applyEnrichedEventState(eventId, result.event, result.options)
                persistYelpEnrichment(uid, tripId, eventId, result)
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to enrich Yelp event", e)
            } finally {
                _yelpEnrichmentInFlight.update { it - yelpId }
            }
        }
    }

    private fun applyEnrichedEventState(
        eventId: String,
        enrichedEvent: TravelEvent,
        enrichedOptions: List<EventOption>
    ) {
        val mergedEvent = mediaDetailPipeline.mergeEventWithOptions(enrichedEvent, enrichedOptions)

        _eventOptions.update { it + (eventId to enrichedOptions) }
        _uiState.update { state ->
            state.copy(
                events = sortPlanEvents(
                    state.events.map { event ->
                        if (event.eventId == eventId) mergedEvent else event
                    }
                )
            )
        }
    }

    private suspend fun persistYelpEnrichment(
        uid: String,
        tripId: String,
        eventId: String,
        result: YelpRepository.YelpEventEnrichmentResult
    ) {
        val eventRef = db.collection("users")
            .document(uid)
            .collection("trips")
            .document(tripId)
            .collection("events")
            .document(eventId)

        val shouldPersistEventOnly = yelpPoolTypeForEvent(result.event) != null &&
            result.event.selectedOptionId.isNotBlank()
        if (shouldPersistEventOnly) {
            eventRef.set(result.event.toFirestoreMap()).await()
        } else {
            db.runBatch { batch ->
                batch.set(eventRef, result.event.toFirestoreMap())
                result.options
                    .filter { it.optionId in result.updatedOptionIds }
                    .forEach { option ->
                        batch.set(
                            eventRef.collection("options").document(option.optionId),
                            option.toMap()
                        )
                    }
            }.await()
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
                        ShareTarget(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unnamed Group",
                            isGroup = true
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
                    ShareTarget(
                        id = chatId,
                        name = userNames[otherUid] ?: "Unknown",
                        isGroup = false
                    )
                }

                _shareTargets.value = (groupTargets + directTargets).sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to fetch share targets", e)
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

    fun shareTripToChat(target: ShareTarget) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return

        viewModelScope.launch {
            try {
                val senderName = fetchUserNames(listOf(uid))[uid] ?: "Traveler"
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
                    "sharedTripId" to tripId,
                    "ownerUid" to uid,
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
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to share trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to share trip.") }
            }
        }
    }

    fun selectOption(eventId: String, optionId: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val options = _eventOptions.value[eventId].orEmpty()
        val selectedOption = options.firstOrNull { it.optionId == optionId } ?: return
        val event = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return
        val isYelpSelectionEvent = selectedOption.source.equals("yelp", ignoreCase = true) &&
            yelpPoolTypeForEvent(event) != null

        val updatedOptions = options.map { it.copy(selected = it.optionId == optionId) }
        val updatedEvent = mediaDetailPipeline.mergeEventWithOptions(event, updatedOptions)
        val updatedEvents = sortPlanEvents(
            _uiState.value.events.map {
                if (it.eventId == eventId) updatedEvent else it
            }
        )

        _eventOptions.update { it + (eventId to updatedOptions) }
        _rejectedOptions.update { current ->
            val nextRejected = current[eventId].orEmpty() - optionId
            current + (eventId to nextRejected)
        }
        _uiState.update { it.copy(events = updatedEvents) }
        prefetchSharedEventMedia(listOf(updatedEvent))

        viewModelScope.launch {
            try {
                val eventRef = db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)
                    .collection("events")
                    .document(eventId)

                if (isYelpSelectionEvent) {
                    eventRef.set(updatedEvent.toFirestoreMap()).await()
                } else {
                    db.runBatch { batch ->
                        batch.set(eventRef, updatedEvent.toFirestoreMap())
                        updatedOptions.forEach { option ->
                            batch.set(
                                eventRef.collection("options").document(option.optionId),
                                option.toMap()
                            )
                        }
                    }.await()
                }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to select option", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update selection.") }
                return@launch
            }

            ensureYelpEventEnriched(eventId)
        }
    }

    fun rejectOption(eventId: String, optionId: String) {
        _rejectedOptions.update { current ->
            current + (eventId to (current[eventId].orEmpty() + optionId))
        }
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val event = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return
        val poolType = yelpPoolTypeForEvent(event) ?: return
        val poolItems = sharedYelpPools[poolType].orEmpty()
        if (poolItems.isEmpty()) return

        viewModelScope.launch {
            try {
                val expandedPool = maybeExpandSharedYelpPool(
                    uid = uid,
                    tripId = tripId,
                    event = event,
                    poolType = poolType,
                    poolItems = poolItems,
                    rejectedIds = _rejectedOptions.value[eventId].orEmpty()
                )
                val updatedOptions = synthesizeSharedYelpOptions(
                    tripId = tripId,
                    event = event,
                    poolItems = expandedPool,
                    rejectedIds = _rejectedOptions.value[eventId].orEmpty()
                )
                _eventOptions.update { it + (eventId to updatedOptions) }
                _uiState.update { state ->
                    state.copy(events = mediaDetailPipeline.applySelectedOptions(
                        events = state.events,
                        optionsByEvent = _eventOptions.value,
                        sortEvents = ::sortPlanEvents
                    ))
                }
            } catch (e: Exception) {
                Log.w("ItineraryViewModel", "Failed to expand shared Yelp options", e)
            }
        }
    }

    fun patchEventFields(
        eventId: String,
        title: String,
        startTime: String,
        notes: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val currentEvent = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return

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
        _uiState.update { state ->
            state.copy(events = sortPlanEvents(state.events.map { if (it.eventId == eventId) updatedEvent else it }))
        }

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)
                    .collection("events")
                    .document(eventId)
                    .set(updatedEvent.toFirestoreMap())
                    .await()
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to patch event", e)
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
        val currentEvents = _uiState.value.events
        val movingEvent = currentEvents.firstOrNull { it.eventId == eventId } ?: return
        val normalizedFromDate = normalizeDate(fromDate)
        val normalizedToDate = normalizeDate(toDate)

        val grouped = currentEvents
            .groupBy { normalizeDate(it.date) }
            .mapValues { (_, events) ->
                events.sortedWith(compareBy({ it.details["sortOrder"]?.toIntOrNull() ?: 0 }, { it.startTime }))
                    .toMutableList()
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

        val updatedEvents = grouped
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

        _uiState.update { it.copy(events = updatedEvents) }
    }

    fun persistEventPlacements(affectedDates: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val normalizedDates = affectedDates.map(::normalizeDate).toSet()
        val affectedEvents = _uiState.value.events.filter { normalizeDate(it.date) in normalizedDates }

        if (affectedEvents.isEmpty()) return

        viewModelScope.launch {
            try {
                db.runBatch { batch ->
                    affectedEvents.forEach { event ->
                        val eventRef = db.collection("users")
                            .document(uid)
                            .collection("trips")
                            .document(tripId)
                            .collection("events")
                            .document(event.eventId)
                        batch.update(
                            eventRef,
                            mapOf(
                                "date" to normalizeDate(event.date),
                                "sortOrder" to (event.details["sortOrder"] ?: "0")
                            )
                        )
                    }
                }.await()
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to persist placements", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save event order.") }
            }
        }
    }

    fun archiveTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)
                    .update(
                        mapOf(
                            "status" to "archived",
                            "archivedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
                _uiState.update { it.copy(infoMessage = "Trip archived.", errorMessage = null) }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to archive trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to archive trip.") }
            }
        }
    }

    fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Remove from list immediately so the UI can switch away before Firestore deletes propagate
                val remaining = _allTrips.value.filter { it.itineraryId != tripId }
                _allTrips.value = remaining

                val tripRef = db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)

                listOf(YELP_POOL_TYPE_RESTAURANTS, YELP_POOL_TYPE_ACTIVITIES).forEach { poolType ->
                    val poolItems = tripRef.collection("optionPools")
                        .document(poolType)
                        .collection("items")
                        .get()
                        .await()
                        .documents
                    poolItems.forEach { it.reference.delete().await() }
                    tripRef.collection("optionPools").document(poolType).delete().await()
                }
                val eventDocs = tripRef.collection("events").get().await().documents
                eventDocs.forEach { eventDoc ->
                    val optionDocs = eventDoc.reference.collection("options").get().await().documents
                    optionDocs.forEach { it.reference.delete().await() }
                    eventDoc.reference.delete().await()
                }
                tripRef.delete().await()

                val nextTrip = remaining.firstOrNull()
                if (nextTrip != null) {
                    fetchTrip(uid, nextTrip.itineraryId)
                } else {
                    resetTripState(infoMessage = "No trips yet. Create one from the New Trip tab.")
                }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to delete trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete trip.") }
            }
        }
    }

    fun loadAllTrips() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val docs = db.collection("users").document(uid)
                    .collection("trips")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .documents

                _allTrips.value = docs.mapNotNull { doc ->
                    try {
                        Itinerary(
                            itineraryId = doc.id,
                            userId = uid,
                            tripName = doc.getString("tripName") ?: "Unnamed Trip",
                            destination = doc.getString("destination") ?: "",
                            origin = doc.getString("origin") ?: "",
                            originIata = doc.getString("originIata") ?: "",
                            destinationIata = doc.getString("destinationIata") ?: "",
                            dateFrom = doc.getString("dateFrom") ?: "",
                            dateTo = doc.getString("dateTo") ?: "",
                            durationDays = (doc.getLong("durationDays") ?: 0L).toInt(),
                            currency = doc.getString("currency") ?: "USD",
                            travelStyle = doc.getString("travelStyle") ?: "",
                            adults = (doc.getLong("adults") ?: 1L).toInt(),
                            children = (doc.getLong("children") ?: 0L).toInt(),
                            createdAt = doc.getString("createdAt") ?: "",
                            status = doc.getString("status") ?: "",
                            eventIds = (doc.get("eventIds") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList()
                        )
                    } catch (e: Exception) {
                        Log.e("ItineraryViewModel", "Failed to parse trip ${doc.id}: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to load all trips: ${e.message}")
            }
        }
    }

    fun renameTrip(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val trimmed = newName.trim().ifBlank { return }

        _uiState.update { it.copy(tripTitle = trimmed) }
        _allTrips.update { trips -> trips.map { if (it.itineraryId == tripId) it.copy(tripName = trimmed) else it } }

        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .update("tripName", trimmed)
                    .await()
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to rename trip: ${e.message}")
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to rename trip.") }
            }
        }
    }

    fun loadTrip(tripId: String? = null) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            resetTripState(infoMessage = "Log in to load your current trip.")
            return
        }

        resetTripState(isLoading = true)

        if (tripId != null) {
            fetchTrip(uid, tripId)
        } else {
            fetchLatestItinerary(uid)
        }
    }

    override fun onCleared() {
        eventsListener?.remove()
        eventsListener = null
        super.onCleared()
    }

    private fun prefetchSharedEventMedia(events: List<TravelEvent>) {
        viewModelScope.launch {
            runCatching {
                mediaDetailPipeline.prefetchSharedMedia(events)
            }.onFailure { error ->
                Log.w("ItineraryViewModel", "Failed to prefetch shared event media", error)
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
}

