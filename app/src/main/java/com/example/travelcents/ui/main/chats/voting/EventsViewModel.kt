package com.example.travelcents.ui.main.chats.voting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.data.model.Event
import com.example.travelcents.data.model.Group
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventsViewModel(val group: Group) : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    val currentUid: String get() = auth.currentUser?.uid ?: ""
    val groupId: String get() = group.id

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var eventsListener: ListenerRegistration? = null

    init {
        startListening()
    }

    private fun startListening() {
        if (groupId.isEmpty()) return
        eventsListener = db.collection("groups")
            .document(groupId)
            .collection("events")
            // REMOVED .orderBy() because Firestore crashes when ordering by Arrays
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                // Add a log so we don't silently swallow future errors
                if (error != null) {
                    Log.e("EventsViewModel", "Error fetching events: ", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val fetchedEvents = snapshot.documents.mapNotNull { doc ->
                    val upvotes = (doc.get("upvotes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val downvotes = (doc.get("downvotes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    Event(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        date = doc.getString("date") ?: "", // Ensures the new Date field is pulled!
                        time = doc.getString("time") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdByName = doc.getString("createdByName") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        upvotes = upvotes,
                        downvotes = downvotes,
                        photoUrl = doc.getString("photoUrl") ?: "",
                        commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                        isWon = doc.getBoolean("isWon") ?: false
                    )
                }
                // Sort locally in Kotlin by actual voting score (Upvotes - Downvotes)
                _events.value = fetchedEvents.sortedByDescending { it.upvotes.size - it.downvotes.size }
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

    fun deleteEvent(event: Event, onComplete: () -> Unit = {}) {
        // Only the creator can delete it
        if (currentUid != event.createdBy) return

        val groupEventRef = db.collection("groups").document(groupId)
            .collection("events").document(event.id)

        // If the event hasn't won yet, or there is no linked trip, just delete it from the chat normally
        if (!event.isWon || group.linkedTripId.isEmpty() || group.linkedTripOwnerId.isEmpty()) {
            groupEventRef.delete().addOnSuccessListener { onComplete() }
            return
        }

        // If it HAS won, we need to clean up the private trip database too
        val tripRef = db.collection("users").document(group.linkedTripOwnerId)
            .collection("trips").document(group.linkedTripId)

        val tripEventRef = tripRef.collection("events").document(event.id)

        db.runBatch { batch ->
            // Delete from the Group Chat
            batch.delete(groupEventRef)

            // Delete from the Trip's private "events" subcollection
            batch.delete(tripEventRef)

            // Remove the ID from the main Trip document's "eventIds" array
            batch.update(tripRef, "eventIds", FieldValue.arrayRemove(event.id))
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("EventsViewModel", "Error deleting event: ", e)
        }
    }
    fun markEventAsWon(event: Event, onComplete: () -> Unit = {}) {
        // If there is no linked trip, just mark it won in the group chat and stop
        if (group.linkedTripId.isEmpty() || group.linkedTripOwnerId.isEmpty()) {
            db.collection("groups").document(groupId).collection("events").document(event.id)
                .update("isWon", true)
                .addOnSuccessListener { onComplete() }
            return
        }

        val groupEventRef = db.collection("groups").document(groupId)
            .collection("events").document(event.id)

        val tripRef = db.collection("users").document(group.linkedTripOwnerId)
            .collection("trips").document(group.linkedTripId)

        val tripEventRef = tripRef.collection("events").document(event.id)

        val wonEvent = event.copy(isWon = true)

        db.runBatch { batch ->
            // Mark it as won in the Group Chat's voting tab
            batch.update(groupEventRef, "isWon", true)

            // Save the UPDATED event into the user's Trip events subcollection
            batch.set(tripEventRef, wonEvent)

            // Append the event ID to the main Trip document's "eventIds" array
            batch.update(tripRef, "eventIds", FieldValue.arrayUnion(event.id))
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("EventsViewModel", "Error adding event to itinerary: ", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListener?.remove()
    }

    class Factory(private val group: Group) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EventsViewModel(group) as T
    }
}