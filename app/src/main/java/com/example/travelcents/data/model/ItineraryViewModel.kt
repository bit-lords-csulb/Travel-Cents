package com.example.travelcents.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.model.TravelEvent
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

data class CurrentTripUiState(
    val isLoading: Boolean = true,
    val currentTripId: String? = null,
    val tripTitle: String = "Loading Trip...",
    val dateFrom: String = "",
    val dateTo: String = "",
    val events: List<TravelEvent> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class ItineraryViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()

    private val _tripTitle = MutableStateFlow("Loading Trip...")
    val tripTitle: StateFlow<String> = _tripTitle.asStateFlow()

    private val _uiState = MutableStateFlow(CurrentTripUiState())
    val uiState: StateFlow<CurrentTripUiState> = _uiState.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var eventsListener: ListenerRegistration? = null

    private fun fetchLatestItinerary(uid: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { tripSnapshot ->
                if (tripSnapshot.isEmpty) {
                    Log.d("ItineraryViewModel", "No trips found.")
                    _uiState.value = CurrentTripUiState(
                        isLoading = false,
                        infoMessage = "No trip found yet. Create one from the New Trip tab."
                    )
                    return@addOnSuccessListener
                }

                handleTripDocument(uid, tripSnapshot.documents.first())
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "DATABASE ERROR: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load trip."
                    )
                }
            }
    }

    private fun fetchTrip(uid: String, tripId: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "That trip is no longer available."
                        )
                    }
                    return@addOnSuccessListener
                }

                handleTripDocument(uid, document)
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "DATABASE ERROR: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load trip."
                    )
                }
            }
    }

    private fun handleTripDocument(uid: String, document: DocumentSnapshot) {
        _tripTitle.value = document.getString("tripName") ?: "Unnamed Trip"
        _uiState.update {
            it.copy(
                isLoading = false,
                currentTripId = document.id,
                tripTitle = _tripTitle.value,
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

                if (snapshot != null) {
                    val fetchedEvents = snapshot.documents.mapNotNull { doc ->
                        val allData = doc.data ?: emptyMap()

                        val coreKeys = listOf(
                            "eventId",
                            "type",
                            "itineraryId",
                            "tz",
                            "date",
                            "startTime",
                            "endTime"
                        )
                        val detailsMap =
                            allData.filterKeys { it !in coreKeys }.mapValues { it.value.toString() }

                        TravelEvent(
                            eventId = doc.getString("eventId") ?: doc.id,
                            type = doc.getString("type") ?: "unknown",
                            itineraryId = doc.getString("itineraryId") ?: tripId,
                            tz = doc.getString("tz") ?: "",
                            date = doc.getString("date") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            details = detailsMap
                        )
                    }

                    _events.value = fetchedEvents
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            events = fetchedEvents,
                            infoMessage = if (fetchedEvents.isEmpty()) {
                                "No plans yet. Tap + to add one."
                            } else {
                                it.infoMessage
                            },
                            errorMessage = null
                        )
                    }
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
                    date = plan.date,
                    startTime = plan.startTime,
                    endTime = plan.endTime,
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
                Log.e("ItineraryViewModel", "Failed to delete event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete plan.") }
            }
        }
    }

    fun loadTrip(tripId: String? = null) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Log.e("ItineraryViewModel", "UID is NULL. Firebase isn't ready yet.")
            _uiState.value = CurrentTripUiState(
                isLoading = false,
                infoMessage = "Log in to load your current trip."
            )
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        Log.d("ItineraryViewModel", "UID found: $uid. Fetching trip: ${tripId ?: "Latest"}")

        if (tripId != null) {
            fetchTrip(uid, tripId)
        } else {
            fetchLatestItinerary(uid)
        }
    }

    override fun onCleared() {
        eventsListener?.remove()
        super.onCleared()
    }
}
