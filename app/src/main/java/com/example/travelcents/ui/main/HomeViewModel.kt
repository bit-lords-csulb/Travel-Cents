package com.example.travelcents.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.remote.WikipediaApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class HomeUiState(
    val isLoading: Boolean = true,
    val trips: List<Itinerary> = emptyList(),
    // destination name -> Wikipedia thumbnail URL
    val tripImages: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val wikipedia: WikipediaApiService = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WikipediaApiService::class.java)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAllTrips()
    }

    fun loadAllTrips() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = HomeUiState(isLoading = false, errorMessage = "Not logged in")
            return
        }

        _uiState.value = HomeUiState(isLoading = true)

        db.collection("users").document(uid)
            .collection("trips")
            .orderBy("dateFrom", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val trips = snapshot.documents.mapNotNull { doc ->
                    try {
                        Itinerary(
                            itineraryId = doc.id,
                            userId = uid,
                            tripName = doc.getString("tripName") ?: "Unnamed Trip",
                            destination = doc.getString("destination") ?: "",
                            origin = doc.getString("origin") ?: "",
                            originIata = doc.getString("originIata") ?: "",
                            destinationIata = doc.getString("destinationIata") ?: "",
                            dateFrom = doc.getString("dateFrom") ?: "",
                            dateTo = doc.getString("dateTo") ?: "",
                            durationDays = (doc.getLong("durationDays") ?: 0L).toInt(),
                            currency = doc.getString("currency") ?: "USD",
                            travelStyle = doc.getString("travelStyle") ?: "",
                            adults = (doc.getLong("adults") ?: 1L).toInt(),
                            children = (doc.getLong("children") ?: 0L).toInt(),
                            createdAt = doc.getString("createdAt") ?: "",
                            status = doc.getString("status") ?: "",
                            eventIds = (doc.get("eventIds") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList()
                        )
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Failed to parse trip ${doc.id}: ${e.message}")
                        null
                    }
                }
                _uiState.value = HomeUiState(isLoading = false, trips = trips)
                fetchDestinationImages(trips.map { it.destination }.distinct())
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Failed to load trips: ${e.message}")
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load trips"
                )
            }
    }

    private fun fetchDestinationImages(destinations: List<String>) {
        viewModelScope.launch {
            val images = mutableMapOf<String, String>()
            destinations.forEach { destination ->
                runCatching {
                    wikipedia.getPageImage(
                        action = "query",
                        titles = destination,
                        prop = "pageimages",
                        format = "json",
                        size = 600
                    )
                }.onSuccess { response ->
                    val url = response.query?.pages?.values
                        ?.firstOrNull()?.thumbnail?.source
                    if (url != null) images[destination] = url
                }.onFailure { e ->
                    Log.e("HomeViewModel", "Wikipedia image fetch failed for $destination: ${e.message}")
                }
            }
            _uiState.update { it.copy(tripImages = images) }
        }
    }
}
