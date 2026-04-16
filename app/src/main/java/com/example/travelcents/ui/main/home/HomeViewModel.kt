package com.example.travelcents.ui.main.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.user.UserProfileRepository
import com.example.travelcents.data.user.model.CurrentUserProfile
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.remote.DestinationImageRepository
import com.example.travelcents.data.trip.remote.WikipediaApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class HomeUiState(
    val isLoading: Boolean = true,
    val trips: List<Itinerary> = emptyList(),
    // itinerary id -> home card image URL
    val tripImages: Map<String, String> = emptyMap(),
    val profile: CurrentUserProfile = CurrentUserProfile(),
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userProfileRepository = UserProfileRepository(auth = auth, db = db)
    private val tripRepository: TripRepository = FirestoreTripRepository(db)

    private val wikipediaClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", WIKIMEDIA_USER_AGENT)
                .header("Api-User-Agent", WIKIMEDIA_USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    private val wikipedia: WikipediaApiService = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .client(wikipediaClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WikipediaApiService::class.java)
    private val destinationImages = DestinationImageRepository(wikipedia)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        loadAllTrips()
        viewModelScope.launch {
            userProfileRepository.syncCurrentUserGoogleProfile()
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            userProfileRepository.observeCurrentUserProfile().collect { profile ->
                _uiState.update { currentState -> currentState.copy(profile = profile) }
            }
        }
    }

    fun loadAllTrips() {
        val currentProfile = _uiState.value.profile
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = HomeUiState(
                isLoading = false,
                profile = currentProfile,
                errorMessage = "Not logged in"
            )
            return
        }

        _uiState.value = HomeUiState(isLoading = true, profile = currentProfile)

        viewModelScope.launch {
            runCatching {
                tripRepository.getTripSummaries(uid)
                    .sortedBy { itinerary -> itinerary.dateFrom }
            }.onSuccess { trips ->
                val cachedImages = trips
                    .filter { it.homeImageUrl.isNotBlank() }
                    .associate { it.itineraryId to it.homeImageUrl }

                _uiState.value = HomeUiState(
                    isLoading = false,
                    trips = trips,
                    tripImages = cachedImages,
                    profile = currentProfile
                )
                fetchDestinationImages(uid, trips.filter { it.homeImageUrl.isBlank() })
            }.onFailure { error ->
                Log.e("HomeViewModel", "Failed to load trips: ${error.message}")
                _uiState.value = HomeUiState(
                    isLoading = false,
                    profile = currentProfile,
                    errorMessage = error.message ?: "Failed to load trips"
                )
            }
        }
    }

    private fun fetchDestinationImages(uid: String, trips: List<Itinerary>) {
        if (trips.isEmpty()) return

        viewModelScope.launch {
            val images = _uiState.value.tripImages.toMutableMap()
            trips.forEach { trip ->
                val result = destinationImages.resolveDestinationImage(trip.destination)
                if (result.imageUrl != null) {
                    images[trip.itineraryId] = result.imageUrl
                    _uiState.update { it.copy(tripImages = images.toMap()) }
                    Log.d(
                        "HomeViewModel",
                        "Resolved '${trip.destination}' via '${result.matchedQuery}' to '${result.matchedTitle}' url='${result.imageUrl}'"
                    )
                    persistHomeImage(uid, trip.itineraryId, result.imageUrl)
                } else {
                    Log.w(
                        "HomeViewModel",
                        "No Wikimedia image for '${trip.destination}'. Reason=${result.reason}. Tried=${result.triedQueries.joinToString()}"
                    )
                }
            }
            _uiState.update { it.copy(tripImages = images.toMap()) }
        }
    }

    private suspend fun persistHomeImage(uid: String, tripId: String, imageUrl: String) {
        runCatching {
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .update("homeImageUrl", imageUrl)
                .await()
        }.onFailure { error ->
            Log.w("HomeViewModel", "Failed to persist home image for trip '$tripId': ${error.message}")
        }
    }

    private companion object {
        private const val WIKIMEDIA_CONTACT_URL = "https://github.com/bit-lords-csulb/Travel-Cents"
        private val WIKIMEDIA_USER_AGENT =
            "TravelCents/${BuildConfig.VERSION_NAME} (Android app; $WIKIMEDIA_CONTACT_URL)"
    }
}

