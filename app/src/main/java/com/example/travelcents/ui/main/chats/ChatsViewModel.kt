package com.example.travelcents.ui.main.chats

import androidx.lifecycle.ViewModel
import com.example.travelcents.data.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatsViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredGroups: List<Group>
        get () = if (_searchQuery.value.isBlank()) _groups.value
                 else _groups.value.filter {
                     it.name.contains(_searchQuery.value, ignoreCase = true)
                 }

    private var groupsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        startListening()
    }

    fun startListening() {
        if (currentUid.isEmpty()) return
        groupsListener?.remove()
        groupsListener = repository.listenToGroups(currentUid) { groups ->
            _groups.value = groups
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
    }

}
