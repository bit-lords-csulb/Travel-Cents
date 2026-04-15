package com.example.travelcents.ui.main.chats.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.data.trip.model.EventComment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventCommentsViewModel(
    private val groupId: String,
    private val eventId: String
) : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _comments = MutableStateFlow<List<EventComment>>(emptyList())
    val comments: StateFlow<List<EventComment>> = _comments.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _senderName = MutableStateFlow("")
    private var listener: ListenerRegistration? = null

    init {
        fetchSenderName()
        startListening()
    }

    private fun fetchSenderName() {
        if (currentUid.isEmpty()) return
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                _senderName.value = "$first $last".trim().ifBlank { "Unknown" }
            }
    }

    private fun startListening() {
        listener = db.collection("groups").document(groupId)
            .collection("events").document(eventId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _comments.value = snapshot.documents.mapNotNull { doc ->
                    EventComment(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        timestamp = doc.getTimestamp("timestamp")
                    )
                }
            }
    }

    fun onCommentTextChange(text: String) {
        _commentText.value = text
    }

    fun sendComment() {
        val text = _commentText.value.trim()
        if (text.isEmpty() || _senderName.value.isEmpty() || currentUid.isEmpty()) return

        val eventRef = db.collection("groups").document(groupId)
            .collection("events").document(eventId)

        val comment = hashMapOf(
            "text" to text,
            "senderId" to currentUid,
            "senderName" to _senderName.value,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.runBatch { batch ->
            batch.set(eventRef.collection("comments").document(), comment)
            batch.update(eventRef, "commentCount", FieldValue.increment(1))
        }
        _commentText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }

    class Factory(private val groupId: String, private val eventId: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EventCommentsViewModel(groupId, eventId) as T
    }
}
