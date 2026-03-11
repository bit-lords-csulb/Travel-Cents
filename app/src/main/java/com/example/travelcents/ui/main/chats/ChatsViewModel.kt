package com.example.travelcents.ui.main.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.emptyList

class ChatsViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredGroups: StateFlow<List<Group>> = combine(_groups, _searchQuery) { groups, query ->
        if (query.isBlank()) groups
        else groups.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
