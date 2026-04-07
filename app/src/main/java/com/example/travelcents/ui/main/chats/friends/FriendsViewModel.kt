package com.example.travelcents.ui.main.chats.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FriendsViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _friends     = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pendingRequestCount = MutableStateFlow(0)
    val pendingRequestCount: StateFlow<Int> = _pendingRequestCount.asStateFlow()

    val filteredFriends: StateFlow<List<Friend>> = combine(_friends, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.displayName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userDocListeners          = mutableMapOf<String, ListenerRegistration>()
    private var friendsCollectionListener: ListenerRegistration? = null
    private var pendingRequestsListener:   ListenerRegistration? = null

    init {
        viewModelScope.launch {
            startListening()
            startListeningToPendingRequests()
        }
    }

    private fun buildFriend(fUid: String, snap: DocumentSnapshot): Friend {
        val first         = snap.getString("firstName") ?: ""
        val last          = snap.getString("lastName")  ?: ""
        val isOnline      = snap.getBoolean("isOnline") ?: false
        val lastSeenLabel = buildLastSeenLabel(isOnline, snap)
        return Friend(
            uid             = fUid,
            displayName     = "$first $last".trim().ifBlank { "Unknown" },
            email           = snap.getString("email") ?: "",
            profileImageUrl = snap.getString("profileImageUrl") ?: "",
            isOnline        = isOnline,
            lastSeenLabel   = lastSeenLabel
        )
    }

    private fun buildLastSeenLabel(isOnline: Boolean, snap: DocumentSnapshot): String {
        if (isOnline) return "Online"
        val lastSeen = snap.getTimestamp("lastSeen") ?: return "Offline"
        val diffMin  = (System.currentTimeMillis() - lastSeen.toDate().time) / 60_000
        return when {
            diffMin < 60   -> "Last seen ${diffMin}m ago"
            diffMin < 1440 -> "Last seen ${diffMin / 60}h ago"
            else           -> "Last seen yesterday"
        }
    }

    private fun startListening() {
        if (currentUid.isEmpty()) return
        friendsCollectionListener = db
            .collection("users").document(currentUid).collection("friends")
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val activeFriendIds = snapshot.documents.map { it.id }.toSet()

                // Remove listeners for removed friends
                val staleIds = userDocListeners.keys - activeFriendIds
                staleIds.forEach { id -> userDocListeners[id]?.remove(); userDocListeners.remove(id) }

                if (activeFriendIds.isEmpty()) { _friends.value = emptyList(); return@addSnapshotListener }

                // Batch-fetch all friend user docs in one whereIn query instead of N individual listeners
                activeFriendIds.chunked(30).forEach { batch ->
                    db.collection("users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                        .get()
                        .addOnSuccessListener { userDocs ->
                            val fetched = userDocs.documents.mapNotNull { snap ->
                                if (!snap.exists()) null else buildFriend(snap.id, snap)
                            }
                            val current = _friends.value.toMutableList()
                            fetched.forEach { updated ->
                                val idx = current.indexOfFirst { it.uid == updated.uid }
                                if (idx >= 0) current[idx] = updated else current.add(updated)
                            }
                            _friends.value = current
                                .filter { it.uid in activeFriendIds }
                                .sortedBy { it.displayName }
                        }
                }
            }
    }

    private fun startListeningToPendingRequests() {
        if (currentUid.isEmpty()) return
        pendingRequestsListener = db
            .collection("users").document(currentUid).collection("friends")
            .whereEqualTo("status", "pending")
            .whereEqualTo("direction", "received")
            .addSnapshotListener { snapshot, _ ->
                _pendingRequestCount.value = snapshot?.documents?.size ?: 0
            }
    }

    // Removes friend relationship and deletes the DM chat
    fun removeFriend(friendUid: String) {
        if (currentUid.isEmpty()) return
        val batch = db.batch()
        batch.delete(db.collection("users").document(currentUid).collection("friends").document(friendUid))
        batch.delete(db.collection("users").document(friendUid).collection("friends").document(currentUid))
        batch.commit().addOnSuccessListener {
            android.util.Log.d("FriendsViewModel", "Friend removed, now deleting DM with $friendUid")
            repository.deleteDirectChat(currentUid, friendUid) {
                android.util.Log.d("FriendsViewModel", "DM delete completed")
            }
        }.addOnFailureListener {
            android.util.Log.e("FriendsViewModel", "Batch failed: ${it.message}")
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    override fun onCleared() {
        super.onCleared()
        friendsCollectionListener?.remove()
        pendingRequestsListener?.remove()
        userDocListeners.values.forEach { it.remove() }
        userDocListeners.clear()
    }
}