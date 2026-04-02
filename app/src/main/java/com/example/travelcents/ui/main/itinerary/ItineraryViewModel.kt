package com.example.travelcents.ui.main.itinerary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.YelpReview
import com.example.travelcents.data.remote.YelpRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class EditablePlan(
    val eventId: String? = null,
    val type: String = "activity",
    val title: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val notes: String = "",
    val colorKey: String = "rose",
    val existingDetails: Map<String, String> = emptyMap()
)

data class TripSummary(
    val tripId: String,
    val tripName: String,
    val destination: String,
    val dateFrom: String,
    val dateTo: String,
    val coverImageUrl: String = ""
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

// Available chats/DMs for the share sheet
data class ShareTarget(
    val id: String,
    val name: String,
    val isGroup: Boolean
)

class ItineraryViewModel : ViewModel() {

    companion object {
        private const val DEFAULT_TRIP_TITLE = "Loading Trip..."
    }

    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()

    private val _tripTitle = MutableStateFlow(DEFAULT_TRIP_TITLE)
    val tripTitle: StateFlow<String> = _tripTitle.asStateFlow()

    private val _uiState = MutableStateFlow(CurrentTripUiState())
    val uiState: StateFlow<CurrentTripUiState> = _uiState.asStateFlow()

    private val _allTrips = MutableStateFlow<List<TripSummary>>(emptyList())
    val allTrips: StateFlow<List<TripSummary>> = _allTrips.asStateFlow()

    private val _archivedTrips = MutableStateFlow<List<TripSummary>>(emptyList())
    val archivedTrips: StateFlow<List<TripSummary>> = _archivedTrips.asStateFlow()

    // eventId -> loaded options list
    private val _eventOptions = MutableStateFlow<Map<String, List<EventOption>>>(emptyMap())
    val eventOptions: StateFlow<Map<String, List<EventOption>>> = _eventOptions.asStateFlow()

    // session-only: eventId -> set of rejected optionIds
    private val _rejectedOptions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val rejectedOptions: StateFlow<Map<String, Set<String>>> = _rejectedOptions.asStateFlow()

    // yelpId -> cached reviews list
    private val _yelpReviews = MutableStateFlow<Map<String, List<YelpReview>>>(emptyMap())
    val yelpReviews: StateFlow<Map<String, List<YelpReview>>> = _yelpReviews.asStateFlow()

    // yelpIds currently being fetched
    private val _reviewsLoading = MutableStateFlow<Set<String>>(emptySet())
    val reviewsLoading: StateFlow<Set<String>> = _reviewsLoading.asStateFlow()

    // share sheet state
    private val _shareTargets = MutableStateFlow<List<ShareTarget>>(emptyList())
    val shareTargets: StateFlow<List<ShareTarget>> = _shareTargets.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var eventsListener: ListenerRegistration? = null

    private fun resetTripState(
        isLoading: Boolean = false,
        tripTitle: String = DEFAULT_TRIP_TITLE,
        infoMessage: String? = null,
        errorMessage: String? = null
    ) {
        eventsListener?.remove()
        eventsListener = null
        _events.value = emptyList()
        _tripTitle.value = tripTitle
        _eventOptions.value = emptyMap()
        _rejectedOptions.value = emptyMap()
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
                    Log.d("ItineraryViewModel", "No trips found.")
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
        _tripTitle.value = document.getString("tripName") ?: "Unnamed Trip"
        _uiState.update {
            it.copy(
                isLoading = false,
                currentTripId = document.id,
                tripTitle = _tripTitle.value,
                destination = document.getString("destination") ?: "",
                dateFrom = document.getString("dateFrom") ?: "",
                dateTo = document.getString("dateTo") ?: "",
                infoMessage = null,
                errorMessage = null
            )
        }
        listenToEvents(uid, document.id)
    }

    private fun fetchAllTrips(uid: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val active = mutableListOf<TripSummary>()
                val archived = mutableListOf<TripSummary>()
                snapshot.documents.forEach { doc ->
                    val tripName = doc.getString("tripName") ?: return@forEach
                    val summary = TripSummary(
                        tripId = doc.id,
                        tripName = tripName,
                        destination = doc.getString("destination") ?: "",
                        dateFrom = doc.getString("dateFrom") ?: "",
                        dateTo = doc.getString("dateTo") ?: "",
                        coverImageUrl = doc.getString("coverImageUrl") ?: ""
                    )
                    if (doc.getString("status") == "archived") archived.add(summary)
                    else active.add(summary)
                }
                _allTrips.value = active
                _archivedTrips.value = archived
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "Failed to fetch trip list", e)
            }
    }

    fun archiveTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .update("status", "archived")
                    .await()
                if (_uiState.value.currentTripId == tripId) loadTrip()
                else fetchAllTrips(uid)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to archive trip.") }
            }
        }
    }

    fun restoreTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .update("status", "active")
                    .await()
                fetchAllTrips(uid)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to restore trip.") }
            }
        }
    }

    fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val eventsRef = db.collection("users").document(uid)
                    .collection("trips").document(tripId).collection("events")
                val events = eventsRef.get().await()
                if (!events.isEmpty) {
                    val batch = db.batch()
                    events.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .delete().await()
                if (_uiState.value.currentTripId == tripId) loadTrip()
                else fetchAllTrips(uid)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete trip.") }
            }
        }
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

                if (snapshot != null) {
                    val coreKeys = setOf("eventId", "type", "itineraryId", "tz", "date", "startTime", "endTime", "imageUrl")
                    val fetchedEvents = snapshot.documents.mapNotNull { doc ->
                        val allData = doc.data ?: emptyMap()
                        val detailsMap = allData.filterKeys { it !in coreKeys }.mapValues { it.value.toString() }
                        TravelEvent(
                            eventId = doc.getString("eventId") ?: doc.id,
                            type = doc.getString("type") ?: "unknown",
                            itineraryId = doc.getString("itineraryId") ?: tripId,
                            tz = doc.getString("tz") ?: "",
                            date = doc.getString("date") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            details = detailsMap
                        )
                    }

                    _events.value = fetchedEvents
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            events = fetchedEvents,
                            infoMessage = if (fetchedEvents.isEmpty()) "No plans yet. Tap + to add one." else it.infoMessage,
                            errorMessage = null
                        )
                    }

                    // Fetch options for all events in background
                    viewModelScope.launch { loadOptionsForEvents(uid, tripId, fetchedEvents.map { it.eventId }) }
                }
            }
    }

    private suspend fun loadOptionsForEvents(uid: String, tripId: String, eventIds: List<String>) {
        val optionsMap = _eventOptions.value.toMutableMap()
        for (eventId in eventIds) {
            if (optionsMap.containsKey(eventId)) continue // already loaded
            try {
                val snap = db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .collection("events").document(eventId)
                    .collection("options").get().await()
                val opts = snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    EventOption.fromMap(data)
                }
                optionsMap[eventId] = opts
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to load options for $eventId", e)
            }
        }
        _eventOptions.value = optionsMap
    }

    // Mark a new option as selected for an event; update Firestore
    fun selectOption(eventId: String, selectedOptId: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val currentOpts = _eventOptions.value[eventId] ?: return

        // Optimistic local update
        val updated = currentOpts.map { opt ->
            opt.copy(selected = opt.optionId == selectedOptId)
        }
        _eventOptions.update { it + (eventId to updated) }

        // Update the selected event's imageUrl + key detail fields
        val selectedOpt = updated.firstOrNull { it.selected }
        if (selectedOpt != null) {
            val eventRef = db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("events").document(eventId)
            viewModelScope.launch {
                try {
                    // Persist selection flags on all options
                    for (opt in updated) {
                        eventRef.collection("options").document(opt.optionId)
                            .update("selected", opt.selected).await()
                    }
                    // Update event document to reflect new selected option's name/image
                    val nameKey = selectedOpt.details.keys
                        .firstOrNull { it in listOf("name", "restaurant_name", "activity_name", "title") }
                    val patchMap = buildMap<String, Any> {
                        put("imageUrl", selectedOpt.imageUrl.ifBlank { selectedOpt.localImagePath })
                        if (nameKey != null) put(nameKey, selectedOpt.details[nameKey]!!)
                    }
                    if (patchMap.isNotEmpty()) eventRef.update(patchMap).await()
                } catch (e: Exception) {
                    Log.e("ItineraryViewModel", "Failed to persist option selection", e)
                }
            }
        }
    }

    // Track rejected option for a slot — session only, not persisted
    fun rejectOption(eventId: String, optId: String) {
        _rejectedOptions.update { current ->
            val set = (current[eventId] ?: emptySet()) + optId
            current + (eventId to set)
        }
    }

    fun isRejected(eventId: String, optId: String): Boolean =
        _rejectedOptions.value[eventId]?.contains(optId) == true

    // Update local list order only — call persistEventOrder() when drag ends
    fun reorderEventsLocally(date: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val currentEvents = _uiState.value.events.toMutableList()
        val dayEvents = currentEvents.filter { it.date == date }.toMutableList()
        if (fromIndex !in dayEvents.indices || toIndex !in dayEvents.indices) return

        val moved = dayEvents.removeAt(fromIndex)
        dayEvents.add(toIndex, moved)

        val otherEvents = currentEvents.filter { it.date != date }
        val reordered = otherEvents + dayEvents
        _uiState.update { it.copy(events = reordered) }
        _events.value = reordered
    }

    // Persist the current in-memory order for a date to Firestore
    fun persistEventOrder(date: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val dayEvents = _uiState.value.events.filter { it.date == date }
        if (dayEvents.isEmpty()) return

        viewModelScope.launch {
            try {
                val batch = db.batch()
                dayEvents.forEachIndexed { idx, event ->
                    val ref = db.collection("users").document(uid)
                        .collection("trips").document(tripId)
                        .collection("events").document(event.eventId)
                    batch.update(ref, "sortOrder", idx)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to persist event order", e)
            }
        }
    }

    // Lazy-fetch and cache Yelp reviews for a business
    fun fetchYelpReviews(yelpId: String) {
        if (yelpId.isBlank()) return
        if (_yelpReviews.value.containsKey(yelpId)) return // already cached
        if (_reviewsLoading.value.contains(yelpId)) return // already in flight

        _reviewsLoading.update { it + yelpId }
        viewModelScope.launch {
            val reviews = YelpRepository.getBusinessReviews(yelpId)
            _yelpReviews.update { it + (yelpId to reviews) }
            _reviewsLoading.update { it - yelpId }
        }
    }

    // Inline edit an event's title/time/notes — persists to Firestore
    fun patchEventFields(eventId: String, title: String?, startTime: String?, notes: String?) {
        val uid = auth.currentUser?.uid ?: return
        val tripId = _uiState.value.currentTripId ?: return
        val patchMap = buildMap<String, Any> {
            if (!title.isNullOrBlank()) put("title", title.trim())
            if (!startTime.isNullOrBlank()) put("startTime", startTime.trim())
            if (notes != null) put("description", notes.trim())
        }
        if (patchMap.isEmpty()) return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .collection("events").document(eventId)
                    .update(patchMap).await()
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to patch event fields", e)
            }
        }
    }

    // Fetch the user's groups + DMs for the share bottom sheet
    fun fetchShareTargets() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val groups = db.collection("groups")
                    .whereArrayContains("members", uid).get().await()
                val groupTargets = groups.documents.map { doc ->
                    ShareTarget(
                        id = doc.id,
                        name = doc.getString("name") ?: "Group",
                        isGroup = true
                    )
                }
                val dms = db.collection("directChats")
                    .whereArrayContains("members", uid).get().await()
                // Resolve DM partner names
                val dmTargets = dms.documents.mapNotNull { doc ->
                    val members = doc.get("members") as? List<*> ?: return@mapNotNull null
                    val partnerId = members.firstOrNull { it != uid } as? String ?: return@mapNotNull null
                    val partnerDoc = db.collection("users").document(partnerId).get().await()
                    val name = partnerDoc.getString("name") ?: partnerDoc.getString("displayName") ?: "User"
                    ShareTarget(id = doc.id, name = name, isGroup = false)
                }
                _shareTargets.value = groupTargets + dmTargets
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to fetch share targets", e)
            }
        }
    }

    // Send trip card to a chat (group or DM)
    fun shareTripToChat(target: ShareTarget) {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value
        val tripId = state.currentTripId ?: return
        val currentTrip = _allTrips.value.firstOrNull { it.tripId == tripId }
            ?: TripSummary(tripId, state.tripTitle, state.destination, state.dateFrom, state.dateTo)

        viewModelScope.launch {
            try {
                val senderName = db.collection("users").document(uid)
                    .get().await().getString("name") ?: "Traveler"

                val msgData = hashMapOf(
                    "text" to "Shared a trip: ${currentTrip.tripName}",
                    "senderId" to uid,
                    "senderName" to senderName,
                    "messageType" to "trip_card",
                    "sharedTripId" to tripId,
                    "ownerUid" to uid,
                    "tripName" to currentTrip.tripName,
                    "tripDestination" to currentTrip.destination,
                    "tripDateFrom" to currentTrip.dateFrom,
                    "tripDateTo" to currentTrip.dateTo,
                    "coverImageUrl" to currentTrip.coverImageUrl,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                val collection = if (target.isGroup) "groups" else "directChats"
                db.collection(collection).document(target.id)
                    .collection("messages").add(msgData).await()
                db.collection(collection).document(target.id)
                    .update(mapOf(
                        "lastMessage" to "📍 Shared a trip: ${currentTrip.tripName}",
                        "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )).await()
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to share trip", e)
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
                    if (plan.location.isBlank()) remove("location")
                    else put("location", plan.location.trim())
                    if (plan.notes.isBlank()) remove("description")
                    else put("description", plan.notes.trim())
                }

                val event = TravelEvent(
                    eventId = eventId,
                    type = plan.type,
                    itineraryId = tripId,
                    date = plan.date,
                    startTime = plan.startTime,
                    endTime = plan.endTime,
                    details = mergedDetails
                )

                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .collection("events").document(eventId)
                    .set(event.toFirestoreMap()).await()

                _uiState.update {
                    it.copy(
                        infoMessage = if (plan.eventId == null) "Plan added to your trip." else "Plan updated.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to save event", e)
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
                db.collection("users").document(uid)
                    .collection("trips").document(tripId)
                    .collection("events").document(eventId)
                    .delete().await()

                _uiState.update { it.copy(infoMessage = "Plan deleted.", errorMessage = null) }
            } catch (e: Exception) {
                Log.e("ItineraryViewModel", "Failed to delete event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete plan.") }
            }
        }
    }

    fun loadTrip(tripId: String? = null) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ItineraryViewModel", "UID is NULL. Firebase isn't ready yet.")
            resetTripState(infoMessage = "Log in to load your current trip.")
            return
        }

        resetTripState(isLoading = true)
        Log.d("ItineraryViewModel", "UID found: $uid. Fetching trip: ${tripId ?: "Latest"}")

        fetchAllTrips(uid)
        if (tripId != null) fetchTrip(uid, tripId) else fetchLatestItinerary(uid)
    }

    override fun onCleared() {
        eventsListener?.remove()
        eventsListener = null
        super.onCleared()
    }
}