package com.example.travelcents.ui.main.chats.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.DirectChatPreview
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.repository.DirectMessagesRepository
import com.example.travelcents.data.social.repository.GroupsRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ChatsViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val directMessagesRepository: DirectMessagesRepository = DirectMessagesRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    // Groups
    private val _groups = MutableStateFlow<List<Group>>(emptyList())

    // Direct Chats
    private val _directChats = MutableStateFlow<List<DirectChatPreview>>(emptyList())

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredGroupsSearch: StateFlow<List<Group>> = combine(
        _groups,
        _searchQuery
    ) { groups, query ->
        if (query.isBlank()) groups else groups.filter {
            it.name.contains(
                query,
                ignoreCase = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDirectChats: StateFlow<List<DirectChatPreview>> = combine(
        _directChats,
        _searchQuery
    ) { chats, query ->
        if (query.isBlank()) chats else chats.filter {
            it.otherUserName.contains(
                query,
                ignoreCase = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var groupsListener:      ListenerRegistration? = null
    private var directChatsListener: ListenerRegistration? = null

    init { viewModelScope.launch { startListening() } }

    fun startListening() {
        if (currentUid.isEmpty()) return

        groupsListener?.remove()
        groupsListener = groupsRepository.listenToGroups(currentUid) { groups ->
            _groups.value = groups
        }

        directChatsListener?.remove()
        directChatsListener = directMessagesRepository.listenToDirectChatPreviews(currentUid) { chats ->
            android.util.Log.d("DirectChats", "got ${chats.size} chats")
            _directChats.value = chats
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
        directChatsListener?.remove()
    }
}
