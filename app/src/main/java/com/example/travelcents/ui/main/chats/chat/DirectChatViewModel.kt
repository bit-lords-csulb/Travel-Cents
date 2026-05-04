package com.example.travelcents.ui.main.chats.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.model.Message
import com.example.travelcents.data.social.repository.DirectMessagesRepository
import com.example.travelcents.data.social.repository.SocialUserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DirectChatViewModel(
    private val friend: Friend,
    private val userRepository: SocialUserRepository = SocialUserRepository(),
    private val directMessagesRepository: DirectMessagesRepository = DirectMessagesRepository()
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _messages     = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText  = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _currentName  = MutableStateFlow("")
    private val _chatId       = MutableStateFlow("")
    val chatId: StateFlow<String> = _chatId.asStateFlow()

    private val _isOnline = MutableStateFlow(friend.isOnline)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var presenceListener: ListenerRegistration? = null

    init {
        fetchCurrentUserName()
        resolveDirectChat()
        startListeningToPresence()
    }

    private fun fetchCurrentUserName() {
        if (currentUid.isEmpty()) return
        userRepository.fetchUserDisplayName(currentUid) { name -> _currentName.value = name }
    }

    private fun startListeningToPresence() {
        presenceListener?.remove()
        presenceListener = userRepository.observeUserPresence(friend.uid) { online ->
            _isOnline.value = online
        }
    }

    // Find existing direct chat or create a new one, then start listening
    private fun resolveDirectChat() {
        if (currentUid.isEmpty()) return
        directMessagesRepository.getOrCreateDirectChat(
            myUid = currentUid,
            theirUid = friend.uid
        ) { chatId ->
            _chatId.value = chatId
            startListeningToMessages(chatId)
        }
    }

    private fun startListeningToMessages(chatId: String) {
        messagesListener?.remove()
        messagesListener = directMessagesRepository.listenToDirectMessages(chatId) { messages ->
            _messages.value = messages
        }
    }

    fun onMessageTextChange(text: String) { _messageText.value = text }

    fun sendMessage() {
        val text   = _messageText.value.trim()
        val chatId = _chatId.value
        if (text.isEmpty() || _currentName.value.isEmpty() || chatId.isEmpty()) return

        directMessagesRepository.sendDirectMessage(
            chatId     = chatId,
            text       = text,
            senderId   = currentUid,
            senderName = _currentName.value
        )
        _messageText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        presenceListener?.remove()
    }

    class Factory(private val friend: Friend) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DirectChatViewModel(friend) as T
        }
    }
}
