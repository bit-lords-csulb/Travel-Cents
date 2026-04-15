package com.example.travelcents.ui.main.chats.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.data.social.FirestoreRepository
import com.example.travelcents.data.social.model.Message
import com.example.travelcents.data.social.model.Group
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel(
    private val group: Group,
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

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
        repository.fetchUser(currentUid) { name ->
            _currentName.value = name
        }
    }

    private fun startListeningToMessages() {
        messagesListener?.remove()
        messagesListener = repository.listenToMessages(group.id) { messages ->
            _messages.value = messages
        }
    }

    fun onMessageTextChange(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty() || _currentName.value.isEmpty()) return

        repository.sendMessage(
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
