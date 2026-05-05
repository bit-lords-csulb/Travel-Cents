package com.example.travelcents.ui.main.chats.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.model.Message
import com.example.travelcents.data.social.repository.GroupsRepository
import com.example.travelcents.data.social.repository.SocialUserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class ChatViewModel(
    private val group: Group,
    private val userRepository: SocialUserRepository = SocialUserRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    val groupState: StateFlow<Group?> = groupsRepository.observeGroup(group.id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = group
        )

    private val auth = Firebase.auth
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _currentName = MutableStateFlow("")
    val currentName: StateFlow<String> = _currentName.asStateFlow()

    private var messagesListener: ListenerRegistration? = null

    init {
        fetchCurrentUserName()
        startListeningToMessages()
    }

    private fun fetchCurrentUserName() {
        if (currentUid.isEmpty()) return
        userRepository.fetchUserDisplayName(currentUid) { name ->
            _currentName.value = name
        }
    }

    private fun startListeningToMessages() {
        messagesListener?.remove()
        messagesListener = groupsRepository.listenToMessages(group.id) { messages ->
            _messages.value = messages
        }
    }

    fun onMessageTextChange(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty() || _currentName.value.isEmpty()) return

        groupsRepository.sendMessage(
            groupId = group.id,
            text = text,
            senderId = currentUid,
            senderName = _currentName.value
        )
        _messageText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }

    // Factory so we can pass group into the ViewModel
    class Factory(private val group: Group) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(group) as T
        }
    }
}
