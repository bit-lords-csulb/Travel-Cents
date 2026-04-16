package com.example.travelcents.ui.main.current

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.media.prefetchSelectedHotelGalleries
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.resolveTripName
import com.example.travelcents.data.trip.model.withSelectedOption
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isGroup: Boolean
)

class CurrentTripViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val DEFAULT_TRIP_TITLE = "Loading Trip..."
        private const val EMPTY_PLANS_MESSAGE = "No plans yet. Tap + to add one."
        private const val NO_TRIP_MESSAGE = "No trip found yet. Create one from the New Trip tab."
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
    private var eventsListener: ListenerRegistration? = null
    private var currentTripDestination: String = ""

    private fun resetTripState(
        isLoading: Boolean = false,
        tripTitle: String = DEFAULT_TRIP_TITLE,
        infoMessage: String? = null,
        errorMessage: String? = null
    ) {
        eventsListener?.remove()
        eventsListener = null
        currentTripDestination = ""
        _events.value = emptyList()
        _tripTitle.value = tripTitle
        _allTrips.value = emptyList()
        _eventOptions.value = emptyMap()
        _rejectedOptions.value = emptyMap()
        _yelpReviews.value = emptyMap()
        _reviewsLoading.value = emptySet()
        _yelpEnrichmentInFlight.value = emptySet()
        _shareTargets.value = emptyList()
        _tripMembers.value = emptyList()
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
            .get()
            .addOnSuccessListener { tripSnapshot ->
                val latestActiveTrip = tripSnapshot.documents.firstOrNull { document ->
                    !document.getString("status").orEmpty().equals("archived", ignoreCase = true)
                }

                if (latestActiveTrip == null) {
                    Log.d("CurrentTripViewModel", "No active trips found.")
                    resetTripState(
                        infoMessage = NO_TRIP_MESSAGE
                    )
                    return@addOnSuccessListener
                }

                handleTripDocument(uid, latestActiveTrip)
            }
            .addOnFailureListener { e ->
                Log.e("CurrentTripViewModel", "DATABASE ERROR: ${e.message}", e)
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
                Log.e("CurrentTripViewModel", "DATABASE ERROR: ${e.message}", e)
                resetTripState(errorMessage = e.message ?: "Failed to load trip.")
            }
    }

    private fun handleTripDocument(uid: String, document: DocumentSnapshot) {
        currentTripDestination = document.getString("destination") ?: ""
        val storedTripTitle = document.getString("tripName")
        val nextTripTitle = resolveTripName(storedTripTitle, currentTripDestination)
        _tripTitle.value = nextTripTitle
        _uiState.update {
            it.copy(
                isLoading = false,
                currentTripId = document.id,
                tripTitle = nextTripTitle,
                destination = currentTripDestination,
                dateFrom = document.getString("dateFrom") ?: "",
                dateTo = document.getString("dateTo") ?: "",
                infoMessage = null,
                errorMessage = null
            )
        }

        if (nextTripTitle != storedTripTitle.orEmpty().trim()) {
            viewModelScope.launch {
                runCatching {
                    db.collection("users").document(uid)
                        .collection("trips").document(document.id)
                        .update("tripName", nextTripTitle)
                        .await()
                }
            }
        }

        listenToEvents(uid, document.id)
        fetchTripMembers(document.id)
    }

    private fun listenToEvents(uid: String, tripId: String) {
        eventsListener?.remove()
        eventsListener = db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CurrentTripViewModel", "Listen failed.", error)
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
                _events.value = sortedEvents
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
                    val enrichedEvents = sortPlanEvents(
                        sortedEvents.map { event ->
                            val options = optionsByEvent[event.eventId].orEmpty()
                            val baseEvent = event.copy(options = options)
                            val selectedOption = options.firstOrNull { it.selected }
                            if (selectedOption != null) {
                                baseEvent.withSelectedOption(selectedOption).copy(options = options)
                            } else {
                                baseEvent
                            }
                        }
                    )
                    _eventOptions.value = optionsByEvent
                    _rejectedOptions.update { current ->
                        current.filterKeys { key -> optionsByEvent.containsKey(key) }
                    }
                    _events.value = enrichedEvents
                    _uiState.update { state ->
                        state.copy(events = enrichedEvents)
                    }
                    prefetchHotelGalleries(enrichedEvents)
                    prefetchYelpEnrichment(enrichedEvents)
                }
            }
    }

    private suspend fun loadOptionsForEvents(
        uid: String,
        tripId: String,
        events: List<TravelEvent>
    ): Map<String, List<EventOption>> = coroutineScope {
        if (events.isEmpty()) return@coroutineScope emptyMap()

        events.map { event ->
            async {
                val options = db.collection("users")
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

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun postError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun upsertPlan(plan: EditablePlan) {
        val uid = auth.currentUser?.uid
        val tripId = _uiState.value.currentTripId
        if (uid == null || tripId.isNullOrBlank()) {
            postError("Create or load a trip before adding plans.")
            return
        }

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
                    itineraryId = tripId,
                    tz = plan.timeZoneId.trim().ifBlank { defaultPlanTimeZoneId() },
                    date = normalizeDate(plan.date),
                    startTime = normalizeTime(plan.startTime),
                    endTime = normalizeTime(plan.endTime),
                    details = mergedDetails
                )

                db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)
                    .collection("events")
                    .document(eventId)
                    .set(event.toFirestoreMap())
                    .await()

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
        val uid = auth.currentUser?.uid
        val tripId = _uiState.value.currentTripId
        val eventId = plan.eventId

        if (uid == null || tripId.isNullOrBlank() || eventId.isNullOrBlank()) {
            postError("This plan cannot be deleted yet.")
            return
        }

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)
                    .collection("events")
                    .document(eventId)
                    .delete()
                    .await()

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
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val event = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return
        val options = _eventOptions.value[eventId].orEmpty()
        val yelpId = options.firstOrNull { it.selected }
            ?.detailValue(DETAIL_YELP_ID)
            ?.takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() }
            ?: return

        if (!forceRefresh && yelpId in _yelpEnrichmentInFlight.value) return
        if (!forceRefresh && !YelpRepository.needsEventEnrichment(event, options)) return

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
                Log.e("CurrentTripViewModel", "Failed to enrich Yelp event", e)
            } finally {
                _yelpEnrichmentInFlight.update { it - yelpId }
            }
        }
    }

    private fun prefetchYelpEnrichment(events: List<TravelEvent>) {
        events.forEach { event ->
            ensureYelpEventEnriched(event.eventId)
        }
    }

    private fun applyEnrichedEventState(
        eventId: String,
        enrichedEvent: TravelEvent,
        enrichedOptions: List<EventOption>
    ) {
        val selectedOption = enrichedOptions.firstOrNull { it.selected }
        val eventWithOptions = enrichedEvent.copy(options = enrichedOptions)
        val mergedEvent = if (selectedOption != null) {
            eventWithOptions.withSelectedOption(selectedOption).copy(options = enrichedOptions)
        } else {
            eventWithOptions
        }

        val updatedEvents = sortPlanEvents(
            _uiState.value.events.map { event ->
                if (event.eventId == eventId) mergedEvent else event
            }
        )

        _eventOptions.update { it + (eventId to enrichedOptions) }
        _events.value = updatedEvents
        _uiState.update { it.copy(events = updatedEvents) }
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

    private fun fetchTripMembers(tripId: String) {
        viewModelScope.launch {
            try {
                val groupDocs = db.collection("groups")
                    .whereEqualTo("linkedTripId", tripId)
                    .get()
                    .await()
                    .documents

                val memberUids = groupDocs.firstOrNull()
                    ?.let { doc ->
                        @Suppress("UNCHECKED_CAST")
                        (doc.get("members") as? List<*>)?.filterIsInstance<String>()
                    }
                    .orEmpty()

                if (memberUids.isEmpty()) {
                    _tripMembers.value = emptyList()
                    return@launch
                }

                val nameMap = fetchUserNames(memberUids)
                _tripMembers.value = memberUids.mapNotNull { uid ->
                    val name = nameMap[uid] ?: return@mapNotNull null
                    TripMemberUi(
                        uid = uid,
                        displayName = name,
                        initial = name.firstOrNull { it.isLetter() } ?: '?'
                    )
                }
            } catch (e: Exception) {
                Log.d("CurrentTripViewModel", "Could not load trip members: ${e.message}")
                _tripMembers.value = emptyList()
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
                Log.e("CurrentTripViewModel", "Failed to share trip", e)
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

        val updatedOptions = options.map { it.copy(selected = it.optionId == optionId) }
        val updatedEvent = event.withSelectedOption(selectedOption).copy(options = updatedOptions)
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
        _events.value = updatedEvents
        _uiState.update { it.copy(events = updatedEvents) }
        prefetchHotelGalleries(listOf(updatedEvent))

        viewModelScope.launch {
            try {
                val eventRef = db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)
                    .collection("events")
                    .document(eventId)

                db.runBatch { batch ->
                    batch.set(eventRef, updatedEvent.toFirestoreMap())
                    updatedOptions.forEach { option ->
                        batch.set(
                            eventRef.collection("options").document(option.optionId),
                            option.toMap()
                        )
                    }
                }.await()
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to select option", e)
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
        val updatedEvents = sortPlanEvents(
            _uiState.value.events.map { if (it.eventId == eventId) updatedEvent else it }
        )
        _events.value = updatedEvents
        _uiState.update { state ->
            state.copy(events = updatedEvents)
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
        val currentEvents = _uiState.value.events
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

        _events.value = updatedEvents
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
                Log.e("CurrentTripViewModel", "Failed to persist placements", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save event order.") }
            }
        }
    }

    fun archiveTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentTripId = _uiState.value.currentTripId

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

                _allTrips.update { trips ->
                    trips.map {
                        if (it.itineraryId == tripId) it.copy(status = "archived") else it
                    }
                }
                _uiState.update { it.copy(infoMessage = "Trip archived.", errorMessage = null) }

                if (currentTripId == tripId) {
                    fetchLatestItinerary(uid)
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to archive trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to archive trip.") }
            }
        }
    }

    fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val remaining = _allTrips.value.filter { it.itineraryId != tripId }
                _allTrips.value = remaining

                val tripRef = db.collection("users")
                    .document(uid)
                    .collection("trips")
                    .document(tripId)

                val eventDocs = tripRef.collection("events").get().await().documents
                eventDocs.forEach { eventDoc ->
                    val optionDocs = eventDoc.reference.collection("options").get().await().documents
                    optionDocs.forEach { it.reference.delete().await() }
                    eventDoc.reference.delete().await()
                }
                tripRef.delete().await()

                val nextTrip = remaining.firstOrNull {
                    !it.status.equals("archived", ignoreCase = true)
                }
                if (nextTrip != null) {
                    fetchTrip(uid, nextTrip.itineraryId)
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
                            tripName = resolveTripName(
                                doc.getString("tripName"),
                                doc.getString("destination") ?: ""
                            ),
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
                                ?.filterIsInstance<String>() ?: emptyList(),
                            homeImageUrl = doc.getString("homeImageUrl") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("CurrentTripViewModel", "Failed to parse trip ${doc.id}: ${e.message}", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to load all trips", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load trips.") }
            }
        }
    }

    fun renameTrip(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val trimmed = newName.trim().ifBlank { return }

        _tripTitle.value = trimmed
        _uiState.update { it.copy(tripTitle = trimmed) }
        _allTrips.update { trips ->
            trips.map { if (it.itineraryId == tripId) it.copy(tripName = trimmed) else it }
        }

        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .update("tripName", trimmed)
                    .await()
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

        resetTripState(isLoading = true)
        Log.d("CurrentTripViewModel", "UID found: $uid. Fetching trip: ${tripId ?: "Latest"}")

        if (tripId != null) {
            fetchTrip(uid, tripId)
        } else {
            fetchLatestItinerary(uid)
        }
    }

    private fun prefetchHotelGalleries(events: List<TravelEvent>) {
        if (events.none { it.type.equals("hotel", ignoreCase = true) && it.itineraryId.isNotBlank() }) {
            return
        }

        viewModelScope.launch {
            runCatching {
                prefetchSelectedHotelGalleries(getApplication<Application>(), events)
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to prefetch hotel galleries", error)
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
        eventsListener?.remove()
        eventsListener = null
        super.onCleared()
    }
}

