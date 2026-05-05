package com.example.travelcents.ui.main.chats.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.repository.DirectMessagesRepository
import com.example.travelcents.data.social.repository.FriendsRepository
import com.example.travelcents.data.social.repository.isOnlineNow
import com.example.travelcents.data.social.repository.presenceLabel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepository(),
    private val directMessagesRepository: DirectMessagesRepository = DirectMessagesRepository()
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
    private var activeFriendIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            startListening()
            startListeningToPendingRequests()
        }
    }

    private fun buildFriend(fUid: String, snap: DocumentSnapshot): Friend {
        val first         = snap.getString("firstName") ?: ""
        val last          = snap.getString("lastName")  ?: ""
        val isOnline      = snap.isOnlineNow()
        val lastSeenLabel = snap.presenceLabel(isOnline)
        return Friend(
            uid             = fUid,
            displayName     = "$first $last".trim().ifBlank { "Unknown" },
            email           = snap.getString("email") ?: "",
            profileImageUrl = snap.getString("profileImageUrl") ?: "",
            isOnline        = isOnline,
            lastSeenLabel   = lastSeenLabel
        )
    }

    private fun startListening() {
        if (currentUid.isEmpty()) return
        friendsCollectionListener = db
            .collection("users").document(currentUid).collection("friends")
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                activeFriendIds = snapshot.documents.map { it.id }.toSet()

                // Remove listeners for removed friends
                val staleIds = userDocListeners.keys - activeFriendIds
                staleIds.forEach { id -> userDocListeners[id]?.remove(); userDocListeners.remove(id) }
                _friends.value = _friends.value.filter { it.uid in activeFriendIds }

                if (activeFriendIds.isEmpty()) { _friends.value = emptyList(); return@addSnapshotListener }

                activeFriendIds.forEach { friendUid ->
                    if (friendUid in userDocListeners) return@forEach

                    userDocListeners[friendUid] = db.collection("users")
                        .document(friendUid)
                        .addSnapshotListener { userSnap, userError ->
                            if (userError != null) return@addSnapshotListener
                            if (userSnap == null || !userSnap.exists()) {
                                _friends.value = _friends.value.filterNot { it.uid == friendUid }
                                return@addSnapshotListener
                            }

                            val updated = buildFriend(friendUid, userSnap)
                            val current = _friends.value.toMutableList()
                            val idx = current.indexOfFirst { it.uid == friendUid }
                            if (idx >= 0) current[idx] = updated else current.add(updated)
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
        friendsRepository.removeFriend(currentUid, friendUid, onSuccess = {
            android.util.Log.d("FriendsViewModel", "Friend removed, now deleting DM with $friendUid")
            directMessagesRepository.deleteDirectChat(currentUid, friendUid) {
                android.util.Log.d("FriendsViewModel", "DM delete completed")
            }
        }, onFailure = {
            android.util.Log.e("FriendsViewModel", "Friend removal batch failed")
        })
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
