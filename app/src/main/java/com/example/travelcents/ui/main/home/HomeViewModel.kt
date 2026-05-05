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
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.remote.DestinationImageRepository
import com.example.travelcents.data.trip.remote.WikipediaApiService
import com.example.travelcents.data.social.model.BookmarkedPlace
import com.example.travelcents.data.social.repository.BookmarksRepository
import com.example.travelcents.data.user.UserProfileRepository
import com.example.travelcents.data.user.model.CurrentUserProfile
import android.location.Geocoder
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.WeatherRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class HomeUiState(
    val isLoading: Boolean = true,
    val viewerUid: String = "",
    val trips: List<Itinerary> = emptyList(),
    // itinerary id -> home card image URL
    val tripImages: Map<String, String> = emptyMap(),
    // itinerary id -> destination weather pill
    val destinationWeather: Map<String, HomeTripInfoPill> = emptyMap(),
    val selectedTripKey: TripKey? = null,
    val selectedTripEvents: List<TravelEvent> = emptyList(),
    val selectedTripEventsLoading: Boolean = false,
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
    private var selectedTripEventsJob: Job? = null
    private var selectedTripRefreshJob: Job? = null

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
        observeHomeTrips()
        observeBookmarks()
        loadAllTrips()
    }

    private fun observeBookmarks() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            bookmarksRepository.observeBookmarks(uid).collect { places ->
                _uiState.update { it.copy(bookmarks = places) }
            }
        }
    }

    fun removeBookmark(placeId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching { bookmarksRepository.removeBookmark(uid, placeId) }
        }
    }

    fun selectHomeTrip(tripKey: TripKey?) {
        val uid = auth.currentUser?.uid
        if (uid == null || tripKey == null) {
            selectedTripEventsJob?.cancel()
            selectedTripRefreshJob?.cancel()
            _uiState.update {
                it.copy(
                    selectedTripKey = null,
                    selectedTripEvents = emptyList(),
                    selectedTripEventsLoading = false
                )
            }
            return
        }

        if (_uiState.value.selectedTripKey == tripKey) return

        selectedTripEventsJob?.cancel()
        selectedTripRefreshJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTripKey = tripKey,
                selectedTripEvents = emptyList(),
                selectedTripEventsLoading = true
            )
        }

        selectedTripEventsJob = viewModelScope.launch {
            localDataSource.observeTripEvents(tripKey).collect { events ->
                _uiState.update { currentState ->
                    if (currentState.selectedTripKey != tripKey) {
                        currentState
                    } else {
                        currentState.copy(
                            selectedTripEvents = events,
                            selectedTripEventsLoading = if (events.isEmpty()) {
                                currentState.selectedTripEventsLoading
                            } else {
                                false
                            }
                        )
                    }
                }
            }
        }

        selectedTripRefreshJob = viewModelScope.launch {
            runCatching {
                refreshSelectedTripCache(uid, tripKey)
            }.onFailure { error ->
                Log.w(
                    "HomeViewModel",
                    "Failed to refresh selected home trip '${tripKey.tripId}': ${error.message}"
                )
            }

            // After refreshing events, try to fetch weather again for this specific trip
            // since we might have new event-based coordinates now.
            _uiState.value.trips.find { it.itineraryId == tripKey.tripId }?.let { trip ->
                fetchDestinationWeather(listOf(trip))
            }

            _uiState.update { currentState ->
                if (currentState.selectedTripKey == tripKey) {
                    currentState.copy(selectedTripEventsLoading = false)
                } else {
                    currentState
                }
            }
        }
    }

    private suspend fun refreshSelectedTripCache(viewerUid: String, tripKey: TripKey) {
        val remoteSummary = runCatching {
            tripSyncRemoteDataSource.fetchTripRef(viewerUid, tripKey)
        }.getOrNull()
            ?: runCatching {
                tripSyncRemoteDataSource.fetchTripSummary(tripKey)
            }.getOrNull()
            ?: runCatching {
                remoteRepository.getTripSummary(tripKey)
            }.getOrNull()
            ?: return

        localDataSource.upsertTripSummary(
            viewerUid = viewerUid,
            itinerary = remoteSummary,
            isCurrentCandidate = localDataSource.getLatestActiveTripKey(viewerUid) == tripKey
        )

        val events = tripSyncRemoteDataSource.fetchTripEvents(tripKey)
        localDataSource.replaceTripEvents(
            tripKey = tripKey,
            events = events,
            eventVersionGroup = remoteSummary.eventsVersion
        )
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
                fetchDestinationWeather(trips)
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

    private fun fetchDestinationWeather(trips: List<Itinerary>) {
        if (trips.isEmpty()) return
        
        trips.forEach { trip ->
            // Check if we already have weather and if it's still fresh
            if (_uiState.value.destinationWeather.containsKey(trip.itineraryId)) return@forEach
            
            viewModelScope.launch {
                Log.d("HomeViewModel", "Fetching weather for destination: ${trip.destination} (${trip.itineraryId})")
                val coords = resolveDestinationCoordinates(trip)
                if (coords != null) {
                    val snapshot = WeatherRepository.fetchSnapshot(
                        latitude = coords.first,
                        longitude = coords.second,
                        date = trip.dateFrom,
                        startTime = null,
                        timeZoneId = trip.timeZoneId.takeIf { it.isNotBlank() }
                    )
                    
                    if (snapshot != null) {
                        val detail = "${snapshot.temperatureC}C ${snapshot.condition}"
                        val pill = HomeTripInfoPill("Weather", detail)
                        _uiState.update { currentState ->
                            currentState.copy(
                                destinationWeather = currentState.destinationWeather + (trip.itineraryId to pill)
                            )
                        }
                        Log.d("HomeViewModel", "Weather updated for ${trip.itineraryId}: $detail")
                    } else {
                        Log.w("HomeViewModel", "Weather fetch failed for ${trip.destination}")
                    }
                } else {
                    Log.w("HomeViewModel", "Could not resolve coordinates for ${trip.destination}")
                }
            }
        }
    }

    private suspend fun resolveDestinationCoordinates(trip: Itinerary): Pair<Double, Double>? {
        // 1. Check local events first (fastest)
        val localEvents = localDataSource.getTripEvents(TripKey(trip.ownerUid, trip.itineraryId))
        localEvents.forEach { event ->
            val lat = event.detailValue(ATTR_LATITUDE)?.toDoubleOrNull()
            val lon = event.detailValue(ATTR_LONGITUDE)?.toDoubleOrNull()
            if (lat != null && lon != null) return lat to lon
        }

        // 2. Geocode the destination name (reliable fallback)
        return withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(getApplication())
                val addresses = geocoder.getFromLocationName(trip.destination, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].latitude to addresses[0].longitude
                } else null
            }.getOrNull()
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
