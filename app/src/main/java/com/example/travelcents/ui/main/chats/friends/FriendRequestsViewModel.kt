package com.example.travelcents.ui.main.chats.friends

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FriendRequestsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _pendingRequests = MutableStateFlow<List<Friend>>(emptyList())
    val pendingRequests: StateFlow<List<Friend>> = _pendingRequests.asStateFlow()

    private var listener: ListenerRegistration? = null

    init { startListening() }

    private fun startListening() {
        if (currentUid.isEmpty()) return

        // Listen for received pending requests
        listener = db.collection("users")
            .document(currentUid)
            .collection("friends")
            .whereEqualTo("status", "pending")
            .whereEqualTo("direction", "received")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val senderUids = snapshot.documents.map { it.id }
                if (senderUids.isEmpty()) { _pendingRequests.value = emptyList(); return@addSnapshotListener }

                // Fetch each sender's user doc
                db.collection("users")
                    .whereIn(FieldPath.documentId(), senderUids)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        _pendingRequests.value = userSnap.documents.mapNotNull { doc ->
                            val first = doc.getString("firstName") ?: ""
                            val last  = doc.getString("lastName")  ?: ""
                            Friend(
                                uid = doc.id,
                                displayName = "$first $last".trim().ifBlank { "Unknown" },
                                email = doc.getString("email") ?: ""
                            )
                        }
                    }
            }
    }

    fun acceptRequest(senderUid: String) {
        if (currentUid.isEmpty()) return
        val batch = db.batch()

        // Update both sides to accepted
        batch.update(
            db.collection("users").document(currentUid).collection("friends").document(senderUid),
            mapOf("status" to "accepted")
        )
        batch.update(
            db.collection("users").document(senderUid).collection("friends").document(currentUid),
            mapOf("status" to "accepted")
        )
        batch.commit()
    }

    fun declineRequest(senderUid: String) {
        if (currentUid.isEmpty()) return
        val batch = db.batch()

        // Delete both sides
        batch.delete(db.collection("users").document(currentUid).collection("friends").document(senderUid))
        batch.delete(db.collection("users").document(senderUid).collection("friends").document(currentUid))
        batch.commit()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}