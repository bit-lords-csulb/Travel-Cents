package com.example.travelcents.ui.main.newtrip

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ImageCacheManager
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.TravelRequest
import com.example.travelcents.data.remote.GroqRepository
import com.example.travelcents.data.remote.SerpRepository
import com.example.travelcents.data.remote.YelpRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class GenerationStep {
    IDLE,
    CRAFTING_ITINERARY,
    SEARCHING_FLIGHTS,
    FINDING_HOTELS,
    FINDING_RESTAURANTS,
    FINDING_ACTIVITIES,
    DOWNLOADING_IMAGES,
    SAVING,
    COMPLETE
}

class NewTripViewModel(application: Application) : AndroidViewModel(application) {

    // Form fields
    var origin by mutableStateOf("")
    var destination by mutableStateOf("")
    var dateFrom by mutableStateOf("")
    var dateTo by mutableStateOf("")
    var adults by mutableIntStateOf(1)
    var children by mutableIntStateOf(0)
    var pets by mutableIntStateOf(0)
    var travelStyle by mutableStateOf("comfort")
    var currency by mutableStateOf("USD")
    var budgetTotal by mutableStateOf("")
    var dietary by mutableStateOf(emptyList<String>())
    var interests by mutableStateOf(emptyList<String>())
    var specialRequests by mutableStateOf("")

    // Autocomplete
    private val allDestinations = listOf(
        "Paris, France", "Tokyo, Japan", "Bali, Indonesia", "New York, USA", "London, UK", "Dubai, UAE",
        "Rome, Italy", "Barcelona, Spain", "Amsterdam, Netherlands", "Singapore", "Bangkok, Thailand",
        "Seoul, South Korea", "Sydney, Australia", "Hong Kong", "Istanbul, Turkey", "Prague, Czech Republic",
        "Lisbon, Portugal", "Vienna, Austria", "Berlin, Germany", "Madrid, Spain", "Toronto, Canada",
        "Los Angeles, USA", "Chicago, USA", "San Francisco, USA", "Miami, USA", "Las Vegas, USA",
        "Vancouver, Canada", "Montreal, Canada", "Mexico City, Mexico", "Cancun, Mexico", "Rio de Janeiro, Brazil",
        "Buenos Aires, Argentina", "Santiago, Chile", "Lima, Peru", "Bogota, Colombia", "Cape Town, South Africa",
        "Johannesburg, South Africa", "Marrakech, Morocco", "Cairo, Egypt", "Nairobi, Kenya",
        "Abu Dhabi, UAE", "Doha, Qatar", "Mumbai, India", "New Delhi, India", "Bangalore, India",
        "Shanghai, China", "Beijing, China", "Guangzhou, China", "Taipei, Taiwan", "Osaka, Japan",
        "Kyoto, Japan", "Melbourne, Australia", "Auckland, New Zealand", "Athens, Greece", "Venice, Italy",
        "Florence, Italy", "Milan, Italy", "Munich, Germany", "Frankfurt, Germany", "Zurich, Switzerland",
        "Geneva, Switzerland", "Brussels, Belgium", "Dublin, Ireland", "Edinburgh, UK", "Stockholm, Sweden",
        "Oslo, Norway", "Copenhagen, Denmark", "Helsinki, Finland", "Warsaw, Poland", "Budapest, Hungary",
        "Bucharest, Romania", "Sofia, Bulgaria", "Belgrade, Serbia", "Zagreb, Croatia",
        "Ljubljana, Slovenia", "Bratislava, Slovakia", "Tallinn, Estonia", "Riga, Latvia", "Vilnius, Lithuania",
        "Moscow, Russia", "Saint Petersburg, Russia", "Tashkent, Uzbekistan",
        "Almaty, Kazakhstan", "Baku, Azerbaijan", "Tbilisi, Georgia", "Yerevan, Armenia",
        "Riyadh, Saudi Arabia", "Jeddah, Saudi Arabia", "Kuwait City, Kuwait", "Muscat, Oman",
        "Manama, Bahrain", "Beirut, Lebanon", "Amman, Jordan", "Tel Aviv, Israel",
        "Karachi, Pakistan", "Lahore, Pakistan", "Dhaka, Bangladesh", "Colombo, Sri Lanka",
        "Kathmandu, Nepal", "Male, Maldives", "Hanoi, Vietnam", "Ho Chi Minh City, Vietnam",
        "Phnom Penh, Cambodia", "Vientiane, Laos", "Yangon, Myanmar", "Kuala Lumpur, Malaysia",
        "Jakarta, Indonesia", "Manila, Philippines", "Perth, Australia", "Brisbane, Australia",
        "Adelaide, Australia", "Canberra, Australia", "Wellington, New Zealand", "Christchurch, New Zealand",
        "Honolulu, USA", "Anchorage, USA", "Seattle, USA", "Portland, USA", "Denver, USA",
        "Phoenix, USA", "Dallas, USA", "Houston, USA", "Atlanta, USA", "Boston, USA",
        "Philadelphia, USA", "Washington D.C., USA"
    ).sorted()

    var filteredDestinations by mutableStateOf(emptyList<String>())
        private set

    fun updateDestination(input: String) {
        destination = input
        filteredDestinations = if (input.length >= 2) {
            allDestinations.filter { it.contains(input, ignoreCase = true) }.take(5)
        } else {
            emptyList()
        }
    }

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Idle)
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()
    private val _generationStep = MutableStateFlow(GenerationStep.IDLE)
    val generationStep: StateFlow<GenerationStep> = _generationStep.asStateFlow()

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
                // Step 1: Groq generates itinerary metadata + IATA codes
                _generationStep.value = GenerationStep.CRAFTING_ITINERARY
                _uiState.value = TripUiState.Loading(GROQ_ITINERARY_MESSAGES.random())
                val itinerary = GroqRepository.generateItinerary(request)

                // Hotel budget slice (~40% of total / nights)
                val hotelBudgetPerNight = if (budget > 0 && itinerary.durationDays > 0)
                    (budget * 0.40) / itinerary.durationDays
                else 0.0

                // Step 2: Flights + hotels in parallel
                _generationStep.value = GenerationStep.SEARCHING_FLIGHTS
                _uiState.value = TripUiState.Loading(SERP_FLIGHTS_MESSAGES.random())
                val flightsDeferred = async { SerpRepository.searchFlights(request, itinerary) }
                _generationStep.value = GenerationStep.FINDING_HOTELS
                _uiState.value = TripUiState.Loading(SERP_HOTELS_MESSAGES.random())
                val hotelsDeferred = async { SerpRepository.searchHotels(request, itinerary, hotelBudgetPerNight) }
                val realFlights = flightsDeferred.await()
                val realHotels = hotelsDeferred.await()

                // Remaining budget for activity guidance
                val flightPrice = realFlights.firstOrNull()?.details?.get("total_price")?.toDoubleOrNull() ?: 0.0
                val hotelPerNight = realHotels.firstOrNull()?.details?.get("rate_per_night")?.toDoubleOrNull() ?: 0.0
                val hotelTotal = hotelPerNight * itinerary.durationDays
                val remainingBudget = if (budget > 0) maxOf(0.0, budget - flightPrice - hotelTotal) else 0.0

                val tripDates = generateTripDates(request.dateFrom, itinerary.durationDays)

                // Step 3: Yelp restaurants — 1 pooled call, distributed round-robin across days
                _generationStep.value = GenerationStep.FINDING_RESTAURANTS
                _uiState.value = TripUiState.Loading(YELP_RESTAURANTS_MESSAGES.random())
                val restaurantPool = YelpRepository.fetchRestaurantPool(itinerary.destination, request.dietary)
                val restaurantEvents = YelpRepository.distributePoolToEvents(
                    restaurantPool, tripDates, "restaurant", itinerary.itineraryId
                )

                // Step 4: Yelp activities (1 pooled call) + Yelp events (full trip range), in parallel
                _generationStep.value = GenerationStep.FINDING_ACTIVITIES
                _uiState.value = TripUiState.Loading(YELP_ACTIVITIES_MESSAGES.random())
                val activityPoolDeferred = async { YelpRepository.fetchActivityPool(itinerary.destination) }
                val yelpEventsDeferred = async {
                    YelpRepository.searchEvents(
                        location = itinerary.destination,
                        startDate = request.dateFrom,
                        endDate = request.dateTo,
                        itineraryId = itinerary.itineraryId
                    )
                }
                val activityEvents = YelpRepository.distributePoolToEvents(
                    activityPoolDeferred.await(), tripDates, "activity", itinerary.itineraryId
                )
                val localEvents = yelpEventsDeferred.await()

                val allEvents = realFlights + realHotels + restaurantEvents + activityEvents + localEvents
                val linkedItinerary = itinerary.copy(eventIds = allEvents.map { it.eventId })

                // Step 5: Download selected hero images only — alternative images are lazy-loaded on expand
                _generationStep.value = GenerationStep.DOWNLOADING_IMAGES
                _uiState.value = TripUiState.Loading(DOWNLOADING_MESSAGES.random())
                val heroImageUrls = allEvents.map { it.imageUrl }.filter { it.isNotBlank() }
                val localPaths = ImageCacheManager.downloadTripImages(
                    getApplication(),
                    itinerary.itineraryId,
                    heroImageUrls
                )

                // Patch localImagePath into event hero image where cached; alternatives remain remote URLs
                val patchedEvents = allEvents.map { event ->
                    val localEventImg = localPaths[event.imageUrl]
                    if (localEventImg != null) event.copy(imageUrl = localEventImg) else event
                }

                // Step 6: Save to Firestore (events + options subcollection)
                _generationStep.value = GenerationStep.SAVING
                _uiState.value = TripUiState.Loading(FIRESTORE_MESSAGES.random())
                saveToFirestore(uid, linkedItinerary, patchedEvents)

                _generationStep.value = GenerationStep.COMPLETE
                _uiState.value = TripUiState.Success(linkedItinerary, patchedEvents)

            } catch (e: Exception) {
                _generationStep.value = GenerationStep.IDLE
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
            val eventRef = tripRef.collection("events").document(event.eventId)
            eventRef.set(event.toFirestoreMap()).await()
            // Options stored as subcollection: events/{eventId}/options/{optionId}
            for (option in event.options) {
                eventRef.collection("options")
                    .document(option.optionId)
                    .set(option.toMap())
                    .await()
            }
        }
    }

    fun resetState() {
        _uiState.value = TripUiState.Idle
        _generationStep.value = GenerationStep.IDLE
    }

    fun toggleDietary(item: String) {
        dietary = if (item in dietary) dietary - item else dietary + item
    }

    fun setDietaryItems(items: List<String>) {
        dietary = items
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun toggleInterest(item: String) {
        interests = if (item in interests) interests - item else interests + item
    }

    // Generates a list of YYYY-MM-DD strings for each day of the trip
    private fun generateTripDates(dateFrom: String, durationDays: Int): List<String> {
        return try {
            val parts = dateFrom.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calendar months are 0-based
            val day = parts[2].toInt()
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, day)
            (0 until durationDays).map {
                val y = cal.get(java.util.Calendar.YEAR)
                val m = cal.get(java.util.Calendar.MONTH) + 1
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                "%04d-%02d-%02d".format(y, m, d)
            }
        } catch (_: Exception) {
            emptyList()
        }
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
            "Scanning flight options for your dates..."
        )
        private val SERP_HOTELS_MESSAGES = listOf(
            "Looking for top-rated hotels...",
            "Searching for accommodations...",
            "Browsing hotel options at your destination..."
        )
        private val YELP_RESTAURANTS_MESSAGES = listOf(
            "Finding the best restaurants for each day...",
            "Searching local dining options via Yelp...",
            "Curating restaurant picks for your trip..."
        )
        private val YELP_ACTIVITIES_MESSAGES = listOf(
            "Discovering activities and attractions...",
            "Finding things to do via Yelp...",
            "Searching local events and experiences..."
        )
        private val DOWNLOADING_MESSAGES = listOf(
            "Saving photos for offline use...",
            "Downloading images for your trip...",
            "Almost there — caching your trip photos..."
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
