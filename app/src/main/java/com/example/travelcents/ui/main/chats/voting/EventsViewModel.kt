package com.example.travelcents.ui.main.chats.voting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.repository.SocialUserRepository
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Event
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventsViewModel(
    initialGroup: Group,
    private val userRepository: SocialUserRepository = SocialUserRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(db)

    private var _group = initialGroup
    val group: Group get() = _group

    fun updateGroup(newGroup: Group) {
        val linkedTripChanged = newGroup.linkedTripId != _group.linkedTripId ||
            newGroup.linkedTripOwnerId != _group.linkedTripOwnerId
        _group = newGroup
        if (linkedTripChanged) {
            observeLinkedTripAccess()
        }
    }

    val currentUid: String get() = auth.currentUser?.uid ?: ""
    val groupId: String get() = group.id

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _creatorProfiles = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val creatorProfiles: StateFlow<Map<String, Pair<String, String>>> = _creatorProfiles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canWriteLinkedTrip = MutableStateFlow(
        initialGroup.linkedTripId.isEmpty() || initialGroup.linkedTripOwnerId.isEmpty()
    )
    val canWriteLinkedTrip: StateFlow<Boolean> = _canWriteLinkedTrip.asStateFlow()

    private var eventsListener: ListenerRegistration? = null
    private var linkedTripAccessListener: ListenerRegistration? = null

    init {
        startListening()
        observeLinkedTripAccess()
    }

    private fun startListening() {
        if (groupId.isEmpty()) return
        eventsListener = db.collection("groups")
            .document(groupId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    Log.e("EventsViewModel", "Error fetching events: ", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val fetchedEvents = snapshot.documents.mapNotNull { doc ->
                    val upvotes =
                        (doc.get("upvotes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val downvotes = (doc.get("downvotes") as? List<*>)?.filterIsInstance<String>()
                        ?: emptyList()

                    Event(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        startTime = doc.getString("startTime") ?: doc.getString("time") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdByName = doc.getString("createdByName") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        upvotes = upvotes,
                        downvotes = downvotes,
                        photoUrl = doc.getString("photoUrl") ?: "",
                        commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                        isWon = doc.getBoolean("isWon") ?: false,
                        yelpId = doc.getString("yelpId") ?: "",
                        yelpUrl = doc.getString("yelpUrl") ?: "",
                        yelpCategory = doc.getString("yelpCategory") ?: "",
                        yelpCategories = (doc.get("yelpCategories") as? List<*>)
                            ?.filterIsInstance<String>()
                            .orEmpty(),
                        yelpRating = doc.getDouble("yelpRating"),
                        yelpReviewCount = (doc.getLong("yelpReviewCount") ?: 0L).toInt(),
                        yelpImageUrl = doc.getString("yelpImageUrl") ?: ""
                    )
                }
                _events.value =
                    fetchedEvents.sortedByDescending { it.upvotes.size }
                fetchMissingCreatorProfiles(fetchedEvents)
            }
    }

    private fun fetchMissingCreatorProfiles(events: List<Event>) {
        events
            .map { it.createdBy }
            .filter { it.isNotBlank() && it !in _creatorProfiles.value }
            .distinct()
            .forEach { uid ->
                userRepository.fetchUserFullProfile(uid) { name, photo ->
                    _creatorProfiles.value = _creatorProfiles.value + (uid to (name to photo))
                }
            }
    }

    private fun observeLinkedTripAccess() {
        linkedTripAccessListener?.remove()

        if (group.linkedTripId.isEmpty() || group.linkedTripOwnerId.isEmpty()) {
            _canWriteLinkedTrip.value = true
            return
        }

        if (currentUid.isEmpty()) {
            _canWriteLinkedTrip.value = false
            return
        }

        linkedTripAccessListener = db.collection("users")
            .document(group.linkedTripOwnerId)
            .collection("trips")
            .document(group.linkedTripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventsViewModel", "Error loading linked trip access", error)
                    _canWriteLinkedTrip.value = currentUid == group.linkedTripOwnerId
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    _canWriteLinkedTrip.value = false
                    return@addSnapshotListener
                }

                val roleByUid = (snapshot.get("roleByUid") as? Map<*, *>)
                    ?.mapNotNull { entry ->
                        val uid = entry.key as? String ?: return@mapNotNull null
                        val role = entry.value as? String ?: return@mapNotNull null
                        uid to role
                    }
                    ?.toMap()
                    .orEmpty()

                val role = when {
                    currentUid == group.linkedTripOwnerId -> TripAccessRole.OWNER
                    else -> TripAccessRole.fromWireValue(roleByUid[currentUid])
                }
                _canWriteLinkedTrip.value = role.canMutateEvents()
            }
    }

    fun upvote(event: Event) {
        if (currentUid.isEmpty()) return
        val ref = db.collection("groups").document(groupId).collection("events").document(event.id)
        if (event.upvotes.contains(currentUid)) {
            ref.update("upvotes", FieldValue.arrayRemove(currentUid))
        } else {
            ref.update(
                mapOf(
                    "upvotes" to FieldValue.arrayUnion(currentUid),
                    "downvotes" to FieldValue.arrayRemove(currentUid)
                )
            )
        }
    }

    fun downvote(event: Event) {
        if (currentUid.isEmpty()) return
        val ref = db.collection("groups").document(groupId).collection("events").document(event.id)
        if (event.downvotes.contains(currentUid)) {
            ref.update("downvotes", FieldValue.arrayRemove(currentUid))
        } else {
            ref.update(
                mapOf(
                    "downvotes" to FieldValue.arrayUnion(currentUid),
                    "upvotes" to FieldValue.arrayRemove(currentUid)
                )
            )
        }
    }

    fun canDeleteEvent(event: Event): Boolean {
        val hasLinkedTripAccess =
            group.linkedTripId.isNotEmpty() &&
            group.linkedTripOwnerId.isNotEmpty() &&
            _canWriteLinkedTrip.value

        return currentUid == event.createdBy || hasLinkedTripAccess
    }

    fun deleteEvent(event: Event, onComplete: () -> Unit = {}) {
        if (!canDeleteEvent(event)) return

        val groupEventRef = db.collection("groups").document(groupId)
            .collection("events").document(event.id)

        if (!event.isWon || group.linkedTripId.isEmpty() || group.linkedTripOwnerId.isEmpty()) {
            groupEventRef.delete().addOnSuccessListener { onComplete() }
            return
        }

        if (!_canWriteLinkedTrip.value) {
            Log.e(
                "EventsViewModel",
                "Unauthorized: User $currentUid cannot delete trip event ${event.id}"
            )
            return
        }

        val tripKey = TripKey(ownerUid = group.linkedTripOwnerId, tripId = group.linkedTripId)
        viewModelScope.launch {
            runCatching {
                tripSyncRemoteDataSource.deleteEvent(tripKey = tripKey, eventId = event.id)
                groupEventRef.delete().await()
            }.onFailure { e ->
                Log.e("EventsViewModel", "Error deleting event: ", e)
            }
            onComplete()
        }
    }

    fun markEventAsWon(event: Event, onComplete: () -> Unit = {}) {
        val currentUid = Firebase.auth.currentUser?.uid ?: return

        if (group.linkedTripId.isEmpty() || group.linkedTripOwnerId.isEmpty()) {
            db.collection("groups").document(groupId).collection("events").document(event.id)
                .update("isWon", true)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener { e ->
                    Log.e("EventsViewModel", "Error marking event as won: ", e)
                    onComplete()
                }
            return
        }

        if (!_canWriteLinkedTrip.value) {
            Log.e(
                "EventsViewModel",
                "Unauthorized: User $currentUid cannot write to trip ${group.linkedTripId}"
            )
            return
        }

        val groupEventRef = db.collection("groups").document(groupId)
            .collection("events").document(event.id)
        val linkedGroup = group
        val tripKey = TripKey(ownerUid = linkedGroup.linkedTripOwnerId, tripId = linkedGroup.linkedTripId)

        viewModelScope.launch {
            runCatching {
                val linkedTrip = runCatching {
                    tripSyncRemoteDataSource.fetchTripSummary(tripKey)
                }.onFailure { e ->
                    Log.w("EventsViewModel", "Unable to load linked trip summary for ${tripKey.tripId}", e)
                }.getOrNull()
                val travelEvent = event.toLinkedTripTravelEvent(
                    group = linkedGroup,
                    linkedTrip = linkedTrip
                )
                tripSyncRemoteDataSource.upsertEvent(tripKey = tripKey, event = travelEvent)
                groupEventRef.update(
                    mapOf(
                        "isWon" to true,
                        "linkedTripEventId" to travelEvent.eventId
                    )
                ).await()
            }.onFailure { e ->
                Log.e("EventsViewModel", "Error adding event to itinerary: ", e)
            }
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListener?.remove()
        linkedTripAccessListener?.remove()
    }

    class Factory(private val group: Group) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EventsViewModel(group) as T
    }
}
