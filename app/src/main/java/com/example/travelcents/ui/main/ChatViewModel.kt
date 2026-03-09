package com.example.travelcents.ui.main

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyDvH65TdP3q3py2O6lNXrfW68Ot7A_rZk4"
    )

    private val chat = generativeModel.startChat(
        history = listOf(
            content(role = "user") { text("You are a helpful travel assistant for an app called TravelCents. Provide travel advice, trip suggestions, and help with itineraries.") },
            content(role = "model") { text("Bonjour! I'm your AI travel agent. How can I help you with your trips today?") }
        )
    )

    init {
        if (_messages.isEmpty()) {
            _messages.add(ChatMessage(text = "Bonjour! I'm your TravelCents AI travel agent. How can I help you today?", isFromUser = false))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isFromUser = true)
        _messages.add(userMessage)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = chat.sendMessage(text)
                val responseText = response.text
                
                if (responseText != null) {
                    _messages.add(ChatMessage(text = responseText, isFromUser = false))
                } else {
                    _messages.add(ChatMessage(text = "I received an empty response. This can happen due to safety filters.", isFromUser = false))
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                val userFriendlyError = when {
                    errorMsg.contains("404") -> "Error: Model 'gemini-2.5-flash' not found. Please ensure your API key is from Google AI Studio and has permission for this model."
                    errorMsg.contains("API_KEY_INVALID") -> "Error: The API Key provided is invalid."
                    errorMsg.contains("429") -> "Error: Rate limit exceeded. Please wait a moment."
                    else -> "Connection Error: Please check your internet and API key settings."
                }
                _messages.add(ChatMessage(text = userFriendlyError, isFromUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
