package com.example.travelcents.ui.main.chats.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.data.FirestoreRepository
import com.example.travelcents.data.model.Group
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripPreview(val id: String, val name: String, val ownerId: String)

class EditChatViewModel(
    private val initialGroup: Group,
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val currentUid = Firebase.auth.currentUser?.uid ?: ""

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
    private val _memberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val memberNames: StateFlow<Map<String, String>> = _memberNames.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchAvailableTrips()
        fetchMemberNames()
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
            repository.fetchUser(uid) { displayName ->
                _memberNames.value = _memberNames.value.toMutableMap().apply {
                    put(uid, displayName)
                }
            }
        }
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
        val updates = mapOf(
            "name" to _chatName.value.trim(),
            "destination" to _destination.value.trim(),
            "description" to _description.value.trim(),
            "members" to _members.value,
            "linkedTripId" to (_selectedTrip.value?.id ?: ""),
            "linkedTripOwnerId" to (_selectedTrip.value?.ownerId ?: ""),
            "groupImageUrl" to imageUrl
        )

        repository.updateGroup(
            groupId = initialGroup.id,
            updates = updates,
            onSuccess = {
                _isLoading.value = false
                onComplete()
            },
            onFailure = {
                _isLoading.value = false
                onComplete()
            }
        )
    }

    class Factory(private val group: Group) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditChatViewModel(group) as T
        }
    }
}