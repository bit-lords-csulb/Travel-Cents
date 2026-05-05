package com.example.travelcents.ui.main.chats.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.repository.GroupsRepository
import com.example.travelcents.data.social.repository.SocialUserRepository
import com.example.travelcents.data.social.repository.FriendsRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripPreview(val id: String, val name: String, val ownerId: String)

class EditChatViewModel(
    private val initialGroup: Group,
    private val repository: GroupsRepository = GroupsRepository(),
    private val userRepository: SocialUserRepository = SocialUserRepository(),
    private val friendRepository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val currentUid = Firebase.auth.currentUser?.uid ?: ""

    val isOwner: Boolean = initialGroup.ownerId == currentUid

    private val _chatName = MutableStateFlow(initialGroup.name)
    val chatName: StateFlow<String> = _chatName.asStateFlow()

    private val _destination = MutableStateFlow(initialGroup.destination)
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _description = MutableStateFlow(initialGroup.description)
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _availableTrips = MutableStateFlow<List<TripPreview>>(emptyList())
    val availableTrips: StateFlow<List<TripPreview>> = _availableTrips.asStateFlow()

    private val _selectedTrip = MutableStateFlow<TripPreview?>(null)
    val selectedTrip: StateFlow<TripPreview?> = _selectedTrip.asStateFlow()

    private val _members = MutableStateFlow(initialGroup.members)
    val members: StateFlow<List<String>> = _members.asStateFlow()
    private val _memberProfiles = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val memberProfiles: StateFlow<Map<String, Pair<String, String>>> = _memberProfiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _groupsList = MutableStateFlow<List<Group>>(emptyList())
    val groupsList: StateFlow<List<Group>> = _groupsList

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stagedFriends = MutableStateFlow<List<Friend>>(emptyList())
    val stagedFriends: StateFlow<List<Friend>> = _stagedFriends.asStateFlow()

    private val _allFriends = MutableStateFlow<List<Friend>>(emptyList())

    init {
        viewModelScope.launch {
            repository.observeGroups().collect { groups ->
                _groupsList.value = groups
            }
        }
        fetchAvailableTrips()
        fetchMemberNames()
        fetchAllFriends()
    }

    private fun fetchAllFriends() {
        friendRepository.fetchFriends(currentUid) { friends ->
            _allFriends.value = friends
        }
    }

    private fun fetchAvailableTrips() {
        if (currentUid.isEmpty()) return
        repository.fetchUserTrips(currentUid) { trips ->
            _availableTrips.value = trips
            _selectedTrip.value = trips.find { it.id == initialGroup.linkedTripId }
        }
    }

    private fun fetchMemberNames() {
        initialGroup.members.forEach { uid ->
            userRepository.fetchUserFullProfile(uid) { name, photo ->
                val currentMap = _memberProfiles.value.toMutableMap()
                currentMap[uid] = Pair(name, photo)
                _memberProfiles.value = currentMap
            }
        }
    }

    val filteredFriends: StateFlow<List<Friend>> = combine(
        _allFriends,
        _searchQuery,
        _stagedFriends,
        _members
    ) { friends, query, staged, currentMembers ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val stagedIds = staged.map { it.uid }
            friends.filter { friend ->
                val matchesQuery = friend.displayName.contains(query, ignoreCase = true)
                // Ensure they aren't already in the group OR already selected (staged)
                val isNotAlreadyInChat = friend.uid !in currentMembers
                val isNotStaged = friend.uid !in stagedIds

                matchesQuery && isNotAlreadyInChat && isNotStaged
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun selectFriend(friend: Friend) {
        if (!_stagedFriends.value.contains(friend)) {
            _stagedFriends.value += friend
        }
        _searchQuery.value = ""
    }

    fun removeStagedFriend(friend: Friend) {
        _stagedFriends.value = _stagedFriends.value - friend
    }

    fun updateName(name: String) { _chatName.value = name }
    fun updateDestination(dest: String) { _destination.value = dest }
    fun updateDescription(desc: String) { _description.value = desc }
    fun updateImageUri(uri: Uri?) { _selectedImageUri.value = uri }
    fun selectTrip(trip: TripPreview) { _selectedTrip.value = trip }

    fun removeMember(memberId: String) {
        _members.value = _members.value.filter { it != memberId }
    }

    fun saveChanges(onComplete: () -> Unit) {
        if (_isLoading.value) return
        _isLoading.value = true

        val uriToUpload = _selectedImageUri.value

        if (uriToUpload != null) {
            repository.uploadGroupImage(uriToUpload, initialGroup.id) { downloadUrl ->
                val finalImageUrl = downloadUrl ?: initialGroup.groupImageUrl
                updateFirestoreDocument(finalImageUrl, onComplete)
            }
        } else {
            updateFirestoreDocument(initialGroup.groupImageUrl, onComplete)
        }
    }

    private fun updateFirestoreDocument(imageUrl: String, onComplete: () -> Unit) {
        // 1. Get the current member UIDs
        // 2. Map the staged friends to just their UIDs
        // 3. Combine them into one final list
        val finalMembersList = _members.value + _stagedFriends.value.map { it.uid }

        val updates = mapOf(
            "name" to _chatName.value.trim(),
            "destination" to _destination.value.trim(),
            "description" to _description.value.trim(),
            "members" to finalMembersList, // Use the combined list here!
            "linkedTripId" to (_selectedTrip.value?.id ?: ""),
            "linkedTripOwnerId" to (_selectedTrip.value?.ownerId ?: ""),
            "groupImageUrl" to imageUrl
        )

        repository.updateGroup(
            groupId = initialGroup.id,
            updates = updates,
            onSuccess = {
                // 1. Update the local members list so the UI shows the new people
                _members.value = finalMembersList
                // 2. Clear the "staged" chips so they disappear
                _stagedFriends.value = emptyList()

                _isLoading.value = false
                onComplete()
            },
            onFailure = {
                _isLoading.value = false
                onComplete()
            }
        )
    }

    fun leaveChat(onComplete: () -> Unit) {
        if (_isLoading.value) return
        _isLoading.value = true
        repository.leaveGroup(initialGroup.id, currentUid) {
            _isLoading.value = false
            onComplete()
        }
    }

    class Factory(private val group: Group) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditChatViewModel(group) as T
        }
    }
}