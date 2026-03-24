package com.example.travelcents.ui.main.chats.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.data.model.Event
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventsViewModel(val groupId: String) : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    val currentUid: String get() = auth.currentUser?.uid ?: ""

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
            .orderBy("upvotes", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null || snapshot == null) return@addSnapshotListener
                _events.value = snapshot.documents.mapNotNull { doc ->
                    val upvotes =
                        (doc.get("upvotes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val downvotes = (doc.get("downvotes") as? List<*>)?.filterIsInstance<String>()
                        ?: emptyList()
                    Event(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        time = doc.getString("time") ?: "",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdByName = doc.getString("createdByName") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        upvotes = upvotes,
                        downvotes = downvotes,
                        photoUrl = doc.getString("photoUrl") ?: "",
                        commentCount = (doc.getLong("commentCount") ?: 0L).toInt()
                    )
                }
            }
    }

    fun upvote(event: Event) {
        if (currentUid.isEmpty()) return
        val ref = db.collection("groups").document(groupId).collection("events").document(event.id)
        if (event.upvotes.contains(currentUid)) {
            // Already upvoted — remove upvote
            ref.update("upvotes", FieldValue.arrayRemove(currentUid))
        } else {
            // Add upvote and remove any downvote
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
            // Already downvoted — remove downvote
            ref.update("downvotes", FieldValue.arrayRemove(currentUid))
        } else {
            // Add downvote and remove any upvote
            ref.update(
                mapOf(
                    "downvotes" to FieldValue.arrayUnion(currentUid),
                    "upvotes" to FieldValue.arrayRemove(currentUid)
                )
            )
        }
    }

    fun deleteEvent(event: Event, onComplete: () -> Unit = {}) {
        if (currentUid != event.createdBy) return
        db.collection("groups").document(groupId).collection("events").document(event.id)
            .delete()
            .addOnSuccessListener { onComplete() }
    }

    override fun onCleared() {
        super.onCleared()
        eventsListener?.remove()
    }

    class Factory(private val groupId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EventsViewModel(groupId) as T
    }
}