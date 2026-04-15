package com.example.travelcents.ui.main.aichat

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ai.ChatMessage
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val chatHistory = mutableListOf(
        LlmMessage(
            "system",
            "You are a helpful travel assistant for an app called TravelCents. Provide travel advice, trip suggestions, and help with itineraries."
        )
    )

    init {
        if (_messages.isEmpty()) {
            val initialAssistantMessage = "Bonjour! I'm your TravelCents AI travel agent. How can I help you today?"
            _messages.add(ChatMessage(text = initialAssistantMessage, isFromUser = false))
            chatHistory.add(LlmMessage("assistant", initialAssistantMessage))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isFromUser = true)
        _messages.add(userMessage)
        chatHistory.add(LlmMessage("user", text))

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val assistantResponse = LlmClient.complete(messages = chatHistory)
                    .ifBlank { "I'm sorry, I couldn't generate a response." }

                _messages.add(ChatMessage(text = assistantResponse, isFromUser = false))
                chatHistory.add(LlmMessage("assistant", assistantResponse))
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Check your AI provider settings and network connection."
                _messages.add(ChatMessage(text = "Error: $errorMsg", isFromUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

