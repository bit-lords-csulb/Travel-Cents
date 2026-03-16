package com.example.travelcents.ui.main.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.FirestoreRepository
import com.example.travelcents.ui.main.chats.chat.Group
import com.example.travelcents.ui.main.chats.friends.Friend
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NewTripViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _allFriends = MutableStateFlow<List<Friend>>(emptyList())

    private val _selectedFriends = MutableStateFlow<List<Friend>>(emptyList())
    val selectedFriends: StateFlow<List<Friend>> = _selectedFriends.asStateFlow()

    private val _selectedDestination = MutableStateFlow<Destination?>(null)
    val selectedDestination: StateFlow<Destination?> = _selectedDestination.asStateFlow()

    private val _friendSearch = MutableStateFlow("")
    val friendSearch: StateFlow<String> = _friendSearch.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    val filteredFriends: StateFlow<List<Friend>> = combine(_allFriends, _friendSearch) { friends, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            friends.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadFriends()
    }

    private fun loadFriends() {
        if (currentUid.isEmpty()) return
        repository.fetchFriends(currentUid) { friends ->
            _allFriends.value = friends
            android.util.Log.d("NewTripVM", "Loaded ${friends.size} friends: $friends")
        }
    }

    fun onSearchChange(query: String) {
        _friendSearch.value = query
    }

    fun selectFriend(friend: Friend) {
        if (_selectedFriends.value.none { it.uid == friend.uid }) {
            _selectedFriends.value += friend
        }
        _friendSearch.value = ""
    }

    fun removeFriend(friend: Friend) {
        _selectedFriends.value = _selectedFriends.value.filter { it.uid != friend.uid }
    }

    fun toggleDestination(destination: Destination) {
        _selectedDestination.value =
            if (_selectedDestination.value == destination) null else destination
    }

    fun createTrip(onSuccess: (Group) -> Unit) {
        if (_selectedFriends.value.isEmpty() || _isCreating.value) return
        _isCreating.value = true

        val groupName = _selectedDestination.value?.name
            ?: _selectedFriends.value.joinToString(", ") {
                it.displayName.split(" ").first()
            }
        val members = _selectedFriends.value.map { it.uid } + currentUid
        val emoji = _selectedDestination.value?.emoji ?: ""

        repository.createGroup(
            name = groupName,
            members = members,
            destinationEmoji = emoji,
            onSuccess = { groupId ->

                repository.fetchGroup(groupId) { group ->
                    _isCreating.value = false
                    if (group != null) onSuccess(group)
                }
            },
            onFailure = {
                _isCreating.value = false
            }
        )
    }
}