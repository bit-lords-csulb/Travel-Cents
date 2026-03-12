package com.example.travelcents.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.TravelRequest
import com.example.travelcents.data.remote.GroqRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NewTripViewModel : ViewModel() {

    // Form fields
    var origin by mutableStateOf("")
    var destination by mutableStateOf("")
    var dateFrom by mutableStateOf("")
    var dateTo by mutableStateOf("")
    var adults by mutableIntStateOf(1)
    var children by mutableIntStateOf(0)
    var travelStyle by mutableStateOf("comfort")
    var currency by mutableStateOf("USD")
    var budgetTotal by mutableStateOf("")
    var dietary by mutableStateOf(emptyList<String>())
    var interests by mutableStateOf(emptyList<String>())
    var specialRequests by mutableStateOf("")

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Idle)
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    fun generateTrip() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.value = TripUiState.Error("You must be logged in to create a trip.")
            return
        }

        if (origin.isBlank() || destination.isBlank() || dateFrom.isBlank() || dateTo.isBlank()) {
            _uiState.value = TripUiState.Error("Please fill in origin, destination, and dates.")
            return
        }

        val budget = budgetTotal.toDoubleOrNull() ?: 0.0

        val request = TravelRequest(
            userId = uid,
            origin = origin,
            destination = destination,
            dateFrom = dateFrom,
            dateTo = dateTo,
            adults = adults,
            children = children,
            travelStyle = travelStyle,
            currency = currency,
            budgetTotal = budget,
            dietary = dietary,
            interests = interests,
            specialRequests = specialRequests
        )

        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            try {
                val (itinerary, events) = GroqRepository.planTrip(request)
                saveToFirestore(uid, itinerary, events)
                _uiState.value = TripUiState.Success(itinerary, events)
            } catch (e: Exception) {
                _uiState.value = TripUiState.Error(e.message ?: "Failed to generate trip.")
            }
        }
    }

    private suspend fun saveToFirestore(
        uid: String,
        itinerary: Itinerary,
        events: List<TravelEvent>
    ) {
        val db = Firebase.firestore
        val tripRef = db.collection("users").document(uid)
            .collection("trips").document(itinerary.itineraryId)

        tripRef.set(itinerary.toFirestoreMap()).await()

        for (event in events) {
            tripRef.collection("events")
                .document(event.eventId)
                .set(event.toFirestoreMap())
                .await()
        }
    }

    fun resetState() {
        _uiState.value = TripUiState.Idle
    }

    fun toggleDietary(item: String) {
        dietary = if (item in dietary) dietary - item else dietary + item
    }

    fun toggleInterest(item: String) {
        interests = if (item in interests) interests - item else interests + item
    }
}

sealed class TripUiState {
    data object Idle : TripUiState()
    data object Loading : TripUiState()
    data class Success(val itinerary: Itinerary, val events: List<TravelEvent>) : TripUiState()
    data class Error(val message: String) : TripUiState()
}