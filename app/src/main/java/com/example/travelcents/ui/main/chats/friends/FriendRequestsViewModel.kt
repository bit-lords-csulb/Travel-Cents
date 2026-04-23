package com.example.travelcents.ui.main.chats.friends

import androidx.lifecycle.ViewModel
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.repository.FriendsRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FriendRequestsViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val auth = Firebase.auth

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _pendingRequests = MutableStateFlow<List<Friend>>(emptyList())
    val pendingRequests: StateFlow<List<Friend>> = _pendingRequests.asStateFlow()

    private var listener: ListenerRegistration? = null

    init { startListening() }

    private fun startListening() {
        if (currentUid.isEmpty()) return
        listener = friendsRepository.listenToPendingReceivedRequests(currentUid) { requests ->
            _pendingRequests.value = requests
        }
    }

    fun acceptRequest(senderUid: String) {
        if (currentUid.isEmpty()) return
        friendsRepository.acceptFriendRequest(currentUid, senderUid)
    }

    fun declineRequest(senderUid: String) {
        if (currentUid.isEmpty()) return
        friendsRepository.declineFriendRequest(currentUid, senderUid)
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
