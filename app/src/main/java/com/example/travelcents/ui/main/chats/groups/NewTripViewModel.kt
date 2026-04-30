package com.example.travelcents.ui.main.chats.groups

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.repository.FriendsRepository
import com.example.travelcents.data.social.repository.GroupsRepository
import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.TripPreview
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewTripViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val tripRepository = FirestoreTripRepository(db)
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _allFriends = MutableStateFlow<List<Friend>>(emptyList())

    private val _selectedFriends = MutableStateFlow<List<Friend>>(emptyList())
    val selectedFriends: StateFlow<List<Friend>> = _selectedFriends.asStateFlow()

    private val _friendSearch = MutableStateFlow("")
    val friendSearch: StateFlow<String> = _friendSearch.asStateFlow()

    private val _chatName = MutableStateFlow("")
    val chatName: StateFlow<String> = _chatName.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedTrip = MutableStateFlow<TripPreview?>(null)
    val selectedTrip: StateFlow<TripPreview?> = _selectedTrip.asStateFlow()

    private val _userTrips = MutableStateFlow<List<TripPreview>>(emptyList())
    val userTrips: StateFlow<List<TripPreview>> = _userTrips.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    val filteredFriends: StateFlow<List<Friend>> = combine(
        _allFriends,
        _friendSearch
    ) { friends, query ->
        if (query.isBlank()) emptyList()
        else friends.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFriends()
        loadUserTrips() // Fetch user's actual generated itineraries
    }

    private fun loadFriends() {
        if (currentUid.isEmpty()) return
        friendsRepository.fetchFriends(currentUid) { friends ->
            _allFriends.value = friends
        }
    }

    private fun loadUserTrips() {
        if (currentUid.isEmpty()) return
        db.collection("users").document(currentUid).collection("trips")
            .get()
            .addOnSuccessListener { snapshot ->
                val trips = snapshot.documents.mapNotNull { doc ->
                    val destinationString = doc.getString("destination") ?: "Unknown Location"

                    TripPreview(
                        id = doc.id,
                        tripName = doc.getString("tripName") ?: "Unnamed Trip",
                        destination = destinationString,
                        emoji = getEmojiForDestination(destinationString)
                    )
                }
                _userTrips.value = trips
            }
            .addOnFailureListener { e ->
                Log.e("NewTripViewModel", "Error loading trips", e)
            }
    }

    fun onSearchChange(query: String) { _friendSearch.value = query }
    fun onChatNameChange(name: String) { _chatName.value = name }
    fun onDescriptionChange(desc: String) { _description.value = desc }

    fun selectFriend(friend: Friend) {
        _selectedFriends.update { current ->
            if (current.none { it.uid == friend.uid }) current + friend else current
        }
        _friendSearch.value = ""
    }

    fun removeFriend(friend: Friend) {
        _selectedFriends.value = _selectedFriends.value.filter { it.uid != friend.uid }
    }

    fun toggleTripSelection(trip: TripPreview) {
        _selectedTrip.value = if (_selectedTrip.value == trip) null else trip
    }

    fun createTrip(onSuccess: (Group) -> Unit) {
        if (_selectedFriends.value.isEmpty() || _isCreating.value) return
        _isCreating.value = true

        // Determine final chat name
        val finalGroupName = _chatName.value.ifBlank {
            _selectedTrip.value?.tripName ?: _selectedFriends.value.joinToString(", ") { it.displayName.split(" ").first() }
        }

        // Setup members and trip link logic
        val members = _selectedFriends.value.map { it.uid } + currentUid
        val linkedTripId = _selectedTrip.value?.id ?: ""
        val linkedTripOwnerId = if (linkedTripId.isNotEmpty()) currentUid else ""
        val emoji = _selectedTrip.value?.emoji ?: "✈️"

        // Create Group in Firestore
        groupsRepository.createGroup(
            name = finalGroupName,
            members = members,
            destinationEmoji = emoji,
            linkedTripId = linkedTripId,           // Passes the specific trip ID
            linkedTripOwnerId = linkedTripOwnerId, // Passes the owner's UID
            onSuccess = { groupId ->
                groupsRepository.fetchGroup(groupId) { group ->
                    _isCreating.value = false
                    if (linkedTripId.isNotEmpty()) {
                        viewModelScope.launch {
                            runCatching {
                                tripRepository.ensureTripAccess(
                                    key = TripKey(
                                        ownerUid = linkedTripOwnerId,
                                        tripId = linkedTripId
                                    ),
                                    memberUids = members,
                                    defaultRole = TripAccessRole.EDITOR
                                )
                            }.onFailure { error ->
                                Log.e("NewTripViewModel", "Failed to grant linked trip access", error)
                            }
                        }
                    }
                    if (group != null) onSuccess(group)
                }
            },
            onFailure = { _isCreating.value = false }
        )
    }

    fun resetForm() {
        _chatName.value = ""
        _description.value = ""
        _friendSearch.value = ""
        _selectedFriends.value = emptyList()
        _selectedTrip.value = null
    }
    private fun getEmojiForDestination(destination: String): String {
        val dest = destination.lowercase()
        return when {
            dest.contains("tokyo") || dest.contains("japan") -> "🗼"
            dest.contains("paris") || dest.contains("france") -> "🥐"
            dest.contains("hawaii") || dest.contains("maui") -> "🌺"
            dest.contains("seoul") || dest.contains("korea") -> "🇰🇷"
            dest.contains("new york") || dest.contains("nyc") -> "🗽"
            dest.contains("london") || dest.contains("uk") || dest.contains("england") -> "🎡"
            dest.contains("mexico") || dest.contains("cancun") -> "🌮"
            dest.contains("italy") || dest.contains("rome") -> "🍝"
            dest.contains("bali") || dest.contains("indonesia") -> "🏝️"
            dest.contains("swiss") || dest.contains("switzerland") -> "🏔️"
            dest.contains("beach") || dest.contains("maldives") -> "⛱️"
            else -> "✈️" // Fallback if no keywords match
        }
    }
}
