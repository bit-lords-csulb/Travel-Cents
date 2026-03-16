package com.example.travelcents.ui.main

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ChatMessage
import com.example.travelcents.data.GroqApi
import com.example.travelcents.data.GroqMessage
import com.example.travelcents.data.GroqRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val groqApi: GroqApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GroqApi::class.java)
    }

    private val chatHistory = mutableListOf(
        GroqMessage("system", "You are a helpful travel assistant for an app called TravelCents. Provide travel advice, trip suggestions, and help with itineraries.")
    )

    init {
        if (_messages.isEmpty()) {
            val initialAssistantMessage = "Bonjour! I'm your TravelCents AI travel agent. How can I help you today?"
            _messages.add(ChatMessage(text = initialAssistantMessage, isFromUser = false))
            chatHistory.add(GroqMessage("assistant", initialAssistantMessage))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isFromUser = true)
        _messages.add(userMessage)
        chatHistory.add(GroqMessage("user", text))

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = GroqRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = chatHistory.toList()
                )
                val response = groqApi.getChatCompletion(
                    apiKey = "Bearer ${BuildConfig.GROQ_API_KEY}",
                    request = request
                )
                val assistantResponse = response.choices.firstOrNull()?.message?.content ?: "I'm sorry, I couldn't generate a response."
                
                _messages.add(ChatMessage(text = assistantResponse, isFromUser = false))
                chatHistory.add(GroqMessage("assistant", assistantResponse))
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.localizedMessage ?: "Check your API key and network connection."}"
                _messages.add(ChatMessage(text = errorMsg, isFromUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
