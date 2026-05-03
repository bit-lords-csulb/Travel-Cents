package com.example.travelcents.ui.main.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.local.trip.TravelCentsDatabase
import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.sync.TripSyncCoordinator
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.LocalFirstTripRepository
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripPerformanceLogger
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.remote.DestinationImageRepository
import com.example.travelcents.data.trip.remote.WikipediaApiService
import com.example.travelcents.data.social.model.BookmarkedPlace
import com.example.travelcents.data.social.repository.BookmarksRepository
import com.example.travelcents.data.user.UserProfileRepository
import com.example.travelcents.data.user.model.CurrentUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class HomeUiState(
    val isLoading: Boolean = true,
    val viewerUid: String = "",
    val trips: List<Itinerary> = emptyList(),
    // itinerary id -> home card image URL
    val tripImages: Map<String, String> = emptyMap(),
    val profile: CurrentUserProfile = CurrentUserProfile(),
    val bookmarks: List<BookmarkedPlace> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userProfileRepository = UserProfileRepository(auth = auth, db = db)
    private val bookmarksRepository = BookmarksRepository(db)
    private val localDataSource = TripLocalDataSource(TravelCentsDatabase.getInstance(application))
    private val remoteRepository = FirestoreTripRepository(db)
    private val tripRepository = LocalFirstTripRepository(
        localDataSource = localDataSource,
        remoteRepository = remoteRepository
    )
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(db)
    private val tripSyncCoordinator = TripSyncCoordinator(
        localDataSource = localDataSource,
        remoteDataSource = tripSyncRemoteDataSource,
        legacyRemoteRepository = remoteRepository
    )
    private var homeTripsJob: Job? = null

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

    private var bookmarksJob: Job? = null
    private var lastBookmarksUid: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        attachBookmarksObserver(firebaseAuth.currentUser?.uid)
    }

    init {
        observeProfile()
        observeHomeTrips()
        attachBookmarksObserver(auth.currentUser?.uid)
        auth.addAuthStateListener(authStateListener)
        loadAllTrips()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun attachBookmarksObserver(uid: String?) {
        if (uid == lastBookmarksUid && bookmarksJob?.isActive == true) return
        bookmarksJob?.cancel()
        lastBookmarksUid = uid
        Log.d("HomeViewModel", "attachBookmarksObserver: subscribing for uid=$uid")
        val firestoreFlow = if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            bookmarksRepository.observeBookmarks(uid)
        }
        bookmarksJob = viewModelScope.launch {
            firestoreFlow.collect { bookmarks ->
                Log.d("HomeViewModel", "bookmarks update uid=$uid count=${bookmarks.size}")
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    fun removeBookmark(placeId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching { bookmarksRepository.removeBookmark(uid, placeId) }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            userProfileRepository.observeCurrentUserProfile().collect { profile ->
                _uiState.update { currentState -> currentState.copy(profile = profile) }
            }
        }
    }

    private fun observeHomeTrips() {
        val uid = auth.currentUser?.uid ?: return
        homeTripsJob?.cancel()
        homeTripsJob = viewModelScope.launch {
            tripRepository.observeHomeTripSummaries(uid).collect { trips ->
                val cachedImages = trips
                    .filter { itinerary -> itinerary.homeImageUrl.isNotBlank() }
                    .associate { itinerary -> itinerary.itineraryId to itinerary.homeImageUrl }

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = currentState.isLoading && trips.isEmpty(),
                        viewerUid = uid,
                        trips = trips,
                        tripImages = cachedImages,
                        errorMessage = if (trips.isNotEmpty()) null else currentState.errorMessage
                    )
                }
            }
        }
    }

    fun loadAllTrips() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(isLoading = false, viewerUid = "", errorMessage = "Not logged in") }
            return
        }

        TripPerformanceLogger.beginHomeLoad(trigger = "HomeViewModel.loadAllTrips", viewerUid = uid)
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = currentState.trips.isEmpty(),
                viewerUid = uid,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                tripSyncCoordinator.refreshHomeIfNeeded(uid)
            }.onSuccess {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        viewerUid = uid,
                        errorMessage = null
                    )
                }
                val trips = _uiState.value.trips
                fetchDestinationImages(trips.filter { it.homeImageUrl.isBlank() })
            }.onFailure { error ->
                Log.e("HomeViewModel", "Failed to load trips: ${error.message}")
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        viewerUid = uid,
                        errorMessage = if (currentState.trips.isEmpty()) {
                            error.message ?: "Failed to load trips"
                        } else {
                            currentState.errorMessage
                        }
                    )
                }
            }
        }
    }

    private fun fetchDestinationImages(trips: List<Itinerary>) {
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
                    if (trip.ownerUid == _uiState.value.viewerUid) {
                        persistHomeImage(trip.ownerUid, trip.itineraryId, result.imageUrl)
                    }
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

    private suspend fun persistHomeImage(ownerUid: String, tripId: String, imageUrl: String) {
        val viewerUid = _uiState.value.viewerUid
        if (viewerUid.isNotBlank()) {
            tripRepository.updateLocalHomeImage(
                viewerUid = viewerUid,
                tripKey = TripKey(ownerUid = ownerUid, tripId = tripId),
                imageUrl = imageUrl
            )
        }

        runCatching {
            tripSyncRemoteDataSource.updateHomeImage(
                tripKey = TripKey(ownerUid = ownerUid, tripId = tripId),
                imageUrl = imageUrl
            )
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
