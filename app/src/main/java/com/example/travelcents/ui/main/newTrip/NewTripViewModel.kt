package com.example.travelcents.ui.main.newTrip

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.ai.chat.AiTripType
import com.example.travelcents.data.ai.repository.TripPlannerRepository
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.remote.SerpRepository
import com.example.travelcents.data.trip.remote.YelpRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

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
    var interests by mutableStateOf(emptyList<String>())
    var specialRequests by mutableStateOf("")

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

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Idle)
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private val _generationStep = MutableStateFlow(GenerationStep.IDLE)
    val generationStep: StateFlow<GenerationStep> = _generationStep.asStateFlow()

    private var draftTripId: String = newTripId()

    fun updateDestination(input: String) {
        destination = input
        filteredDestinations = if (input.length >= 2) {
            allDestinations.filter { it.contains(input, ignoreCase = true) }.take(5)
        } else {
            emptyList()
        }
    }

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
            budgetTotal = budgetTotal.toDoubleOrNull() ?: 0.0,
            interests = interests,
            specialRequests = specialRequests
        )

        viewModelScope.launch {
            try {
                _generationStep.value = GenerationStep.CRAFTING_ITINERARY
                _uiState.value = TripUiState.Loading(ITINERARY_MESSAGES.random())
                val generatedItinerary = TripPlannerRepository.generateItinerary(request)
                val itinerary = generatedItinerary.copy(
                    itineraryId = generateFreshTripId(),
                    userId = uid,
                    ownerUid = uid,
                    memberUids = listOf(uid),
                    roleByUid = mapOf(uid to TripAccessRole.OWNER.wireValue)
                )

                _generationStep.value = GenerationStep.SEARCHING_FLIGHTS
                _uiState.value = TripUiState.Loading(FLIGHT_MESSAGES.random())
                val flightsDeferred = async { SerpRepository.searchFlights(request, itinerary) }

                _generationStep.value = GenerationStep.FINDING_HOTELS
                _uiState.value = TripUiState.Loading(HOTEL_MESSAGES.random())
                val hotelsDeferred = async { SerpRepository.searchHotels(request, itinerary) }

                val realFlights = flightsDeferred.await()
                val realHotels = hotelsDeferred.await()
                val tripDates = generateTripDates(request.dateFrom, itinerary.durationDays)
                val firstFlightArrival = realFlights
                    .firstOrNull { it.date == tripDates.firstOrNull() }
                    ?.endTime
                    ?: "Unknown"

                _generationStep.value = GenerationStep.FINDING_RESTAURANTS
                _uiState.value = TripUiState.Loading(RESTAURANT_MESSAGES.random())
                val restaurantPoolTarget = tripDates.size * 5
                val restaurantPool = YelpRepository.fetchRestaurantPool(
                    location = itinerary.destination,
                    targetCount = restaurantPoolTarget
                )
                val restaurantEvents = YelpRepository.distributePoolToSelectedEvents(
                    pool = YelpRepository.mapBusinessesToPoolItems(restaurantPool),
                    dates = tripDates,
                    type = "restaurant",
                    itineraryId = itinerary.itineraryId
                )

                _generationStep.value = GenerationStep.FINDING_ACTIVITIES
                _uiState.value = TripUiState.Loading(ACTIVITY_MESSAGES.random())
                val aiActivities = TripPlannerRepository.getAIActivities(
                    request = request,
                    itineraryId = itinerary.itineraryId,
                    dates = tripDates,
                    flightArrival = firstFlightArrival
                )

                val allEvents = realFlights + realHotels + restaurantEvents + aiActivities
                val linkedItinerary = itinerary.copy(eventIds = allEvents.map { it.eventId })

                _generationStep.value = GenerationStep.DOWNLOADING_IMAGES
                _uiState.value = TripUiState.Loading(DOWNLOAD_MESSAGES.random())
                val heroImageUrls = allEvents.map { it.imageUrl }.filter { it.isNotBlank() }
                val localPaths = ImageCacheManager.downloadTripImages(
                    context = getApplication(),
                    tripId = linkedItinerary.itineraryId,
                    urls = heroImageUrls
                )

                val patchedEvents = allEvents.map { event ->
                    val localEventImg = localPaths[event.imageUrl]
                    if (localEventImg != null) {
                        event.copy(imageUrl = localEventImg, localImagePath = localEventImg)
                    } else {
                        event
                    }
                }

                _generationStep.value = GenerationStep.SAVING
                _uiState.value = TripUiState.Loading(SAVE_MESSAGES.random())
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
            for (option in event.options) {
                eventRef.collection("options")
                    .document(option.optionId)
                    .set(option.toMap())
                    .await()
            }
        }
    }

    fun resetState() {
        origin = ""
        destination = ""
        dateFrom = ""
        dateTo = ""
        adults = 1
        children = 0
        pets = 0
        travelStyle = "comfort"
        currency = "USD"
        budgetTotal = ""
        interests = emptyList()
        specialRequests = ""
        filteredDestinations = emptyList()
        generateFreshTripId()
        _uiState.value = TripUiState.Idle
        _generationStep.value = GenerationStep.IDLE
    }

    fun createDraftTripFromAiStarter(
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile,
        onTripReady: (TripKey) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.value = TripUiState.Error("You must be logged in to create a trip.")
            return
        }

        applyAiStarterToState(starter, intakeProfile)

        viewModelScope.launch {
            try {
                val tripKey = TripKey(ownerUid = uid, tripId = generateFreshTripId())
                val itinerary = buildDraftItinerary(
                    tripKey = tripKey,
                    starter = starter,
                    intakeProfile = intakeProfile
                )

                _generationStep.value = GenerationStep.SAVING
                _uiState.value = TripUiState.Loading("Creating your AI trip starter...")

                TripSyncRemoteDataSource(FirebaseFirestore.getInstance())
                    .createTrip(
                        ownerUid = uid,
                        itinerary = itinerary,
                        events = emptyList()
                    )

                _generationStep.value = GenerationStep.COMPLETE
                _uiState.value = TripUiState.Success(itinerary, emptyList())
                onTripReady(tripKey)
            } catch (e: Exception) {
                _generationStep.value = GenerationStep.IDLE
                _uiState.value = TripUiState.Error(e.message ?: "Failed to create AI trip starter.")
            }
        }
    }

    fun toggleInterest(item: String) {
        interests = if (item in interests) interests - item else interests + item
    }

    private fun applyAiStarterToState(
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile
    ) {
        val (draftAdults, draftChildren) = inferTravelerCounts(intakeProfile)
        destination = starter.destination
        origin = intakeProfile.origin.ifBlank { origin }
        travelStyle = starter.travelStyle.ifBlank { travelStyle }
        adults = draftAdults
        children = draftChildren
        interests = (intakeProfile.interests + intakeProfile.destinationStyle)
            .filter { it.isNotBlank() }
            .distinct()
        budgetTotal = intakeProfile.budgetTotal?.let(::formatBudgetTotal).orEmpty()
        specialRequests = intakeProfile.notes
            .ifEmpty { starter.summary.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty() }
            .joinToString(separator = "; ")
        filteredDestinations = emptyList()
    }

    private fun generateTripDates(dateFrom: String, durationDays: Int): List<String> {
        return try {
            val startDate = LocalDate.parse(dateFrom)
            val safeDuration = durationDays.coerceAtLeast(1)
            (0 until safeDuration).map { offset ->
                startDate.plusDays(offset.toLong()).toString()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildDraftItinerary(
        tripKey: TripKey,
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile
    ): Itinerary {
        val (draftAdults, draftChildren) = inferTravelerCounts(intakeProfile)
        val draftDuration = intakeProfile.durationDays ?: starter.durationDays.coerceAtLeast(1)

        return Itinerary(
            itineraryId = tripKey.tripId,
            userId = tripKey.ownerUid,
            tripName = starter.title.ifBlank { starter.destination.ifBlank { "AI Trip Starter" } },
            destination = starter.destination,
            origin = intakeProfile.origin,
            dateFrom = "",
            dateTo = "",
            durationDays = draftDuration.coerceAtLeast(1),
            currency = currency.ifBlank { "USD" },
            travelStyle = starter.travelStyle.ifBlank { "comfort" },
            adults = draftAdults,
            children = draftChildren,
            createdAt = Instant.now().toString(),
            status = "draft",
            eventIds = emptyList(),
            ownerUid = tripKey.ownerUid,
            memberUids = listOf(tripKey.ownerUid),
            roleByUid = mapOf(tripKey.ownerUid to TripAccessRole.OWNER.wireValue)
        )
    }

    private fun inferTravelerCounts(intakeProfile: AiTripIntakeProfile): Pair<Int, Int> {
        val normalizedParty = intakeProfile.partySummary.lowercase()

        return when {
            intakeProfile.tripType == AiTripType.SOLO || "solo" in normalizedParty -> 1 to 0
            intakeProfile.tripType == AiTripType.ROMANTIC ||
                "two adults" in normalizedParty ||
                "for two" in normalizedParty -> 2 to 0
            intakeProfile.tripType == AiTripType.FAMILY ||
                "family" in normalizedParty ||
                "kids" in normalizedParty ||
                "children" in normalizedParty -> 2 to 2
            intakeProfile.tripType == AiTripType.FRIENDS ||
                "friends" in normalizedParty ||
                "group" in normalizedParty -> 4 to 0
            else -> 2 to 0
        }
    }

    private fun formatBudgetTotal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun generateFreshTripId(): String {
        draftTripId = newTripId()
        return draftTripId
    }

    private fun newTripId(): String = UUID.randomUUID().toString()

    companion object {
        private val ITINERARY_MESSAGES = listOf(
            "Crafting your itinerary...",
            "Generating trip structure...",
            "Planning the base trip..."
        )

        private val FLIGHT_MESSAGES = listOf(
            "Checking flight availability...",
            "Searching for the best flights...",
            "Scanning flight options for your dates..."
        )

        private val HOTEL_MESSAGES = listOf(
            "Looking for top-rated hotels...",
            "Searching for accommodations...",
            "Browsing hotel options at your destination..."
        )

        private val RESTAURANT_MESSAGES = listOf(
            "Finding the best restaurants for each day...",
            "Searching local dining options via Yelp...",
            "Curating restaurant picks for your trip..."
        )

        private val ACTIVITY_MESSAGES = listOf(
            "Asking the local AI service for activities...",
            "Pulling emulator-backed activity recommendations...",
            "Generating activity ideas from the local microservice..."
        )

        private val DOWNLOAD_MESSAGES = listOf(
            "Saving photos for offline use...",
            "Downloading images for your trip...",
            "Caching your trip photos..."
        )

        private val SAVE_MESSAGES = listOf(
            "Saving your trip...",
            "Storing your itinerary...",
            "Writing everything to Firestore..."
        )
    }
}

sealed class TripUiState {
    data object Idle : TripUiState()
    data class Loading(val statusMessage: String = "Getting things ready...") : TripUiState()
    data class Success(val itinerary: Itinerary, val events: List<TravelEvent>) : TripUiState()
    data class Error(val message: String) : TripUiState()
}
