package com.example.travelcents.ui.main.newtrip

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
import com.example.travelcents.data.remote.SerpRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
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
            try {
                // Step 1: generate itinerary metadata (includes IATA codes)
                _uiState.value = TripUiState.Loading(GROQ_ITINERARY_MESSAGES.random())
                val itinerary = GroqRepository.generateItinerary(request)

                // Step 2: Compute hotel budget slice (~40% of total budget / nights)
                val hotelBudgetPerNight = if (budget > 0 && itinerary.durationDays > 0)
                    (budget * 0.40) / itinerary.durationDays
                else 0.0

                // Step 3: Fetch flights + hotels in parallel (hotels filtered by budget)
                _uiState.value = TripUiState.Loading(SERP_FLIGHTS_MESSAGES.random())
                val flightsDeferred = async { SerpRepository.searchFlights(request, itinerary) }
                _uiState.value = TripUiState.Loading(SERP_HOTELS_MESSAGES.random())
                val hotelsDeferred = async { SerpRepository.searchHotels(request, itinerary, hotelBudgetPerNight) }
                val realFlights = flightsDeferred.await()
                val realHotels = hotelsDeferred.await()

                // Step 4: Compute remaining budget from real prices
                val flightPrice = realFlights.firstOrNull()?.details?.get("total_price")?.toDoubleOrNull() ?: 0.0
                val hotelPerNight = realHotels.firstOrNull()?.details?.get("price_per_night")?.toDoubleOrNull() ?: 0.0
                val hotelTotal = hotelPerNight * itinerary.durationDays
                val remainingBudget = if (budget > 0) maxOf(0.0, budget - flightPrice - hotelTotal) else 0.0

                // Step 5: Groq generates full daily schedule with budget context
                _uiState.value = TripUiState.Loading(GROQ_EVENTS_MESSAGES.random())
                val aiEvents = GroqRepository.generateEvents(itinerary, request, realFlights, realHotels, remainingBudget)

                val allEvents = realFlights + realHotels + aiEvents
                val linkedItinerary = itinerary.copy(eventIds = allEvents.map { it.eventId })

                _uiState.value = TripUiState.Loading(FIRESTORE_MESSAGES.random())
                saveToFirestore(uid, linkedItinerary, allEvents)
                _uiState.value = TripUiState.Success(linkedItinerary, allEvents)
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
        val db = FirebaseFirestore.getInstance()
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

    companion object {
        private val GROQ_ITINERARY_MESSAGES = listOf(
            "Asking Groq to plan your trip...",
            "Groq is crafting your itinerary...",
            "Generating trip structure with Groq...",
            "Consulting Groq for travel ideas..."
        )
        private val SERP_FLIGHTS_MESSAGES = listOf(
            "Checking flight availability...",
            "Searching for the best flights...",
            "Looking up flights via SerpAPI...",
            "Scanning flight options for your dates..."
        )
        private val SERP_HOTELS_MESSAGES = listOf(
            "Looking for suitable hotels...",
            "Searching for accommodations...",
            "Finding hotels via SerpAPI...",
            "Browsing hotel options at your destination..."
        )
        private val GROQ_EVENTS_MESSAGES = listOf(
            "Groq is building your full daily schedule...",
            "Planning restaurants and activities for every day...",
            "Groq is curating budget-aware daily experiences...",
            "Crafting a day-by-day itinerary with Groq..."
        )
        private val FIRESTORE_MESSAGES = listOf(
            "Saving your trip...",
            "Storing your itinerary...",
            "Almost done — saving to the clouds..."
        )
    }
}

sealed class TripUiState {
    data object Idle : TripUiState()
    data class Loading(val statusMessage: String = "Getting things ready...") : TripUiState()
    data class Success(val itinerary: Itinerary, val events: List<TravelEvent>) : TripUiState()
    data class Error(val message: String) : TripUiState()
}