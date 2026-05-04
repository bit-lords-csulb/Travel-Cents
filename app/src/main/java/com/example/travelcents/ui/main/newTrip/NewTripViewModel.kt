package com.example.travelcents.ui.main.newTrip

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.ai.chat.AiTripType
import com.example.travelcents.data.ai.repository.TripPlannerRepository
import com.example.travelcents.data.media.TripMediaCacheStore
import com.example.travelcents.data.media.remoteMediaUrls
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.local.DestinationTimeZones
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_CHECK_OUT_TIME
import com.example.travelcents.data.trip.model.ATTR_DESTINATION_CITY
import com.example.travelcents.data.trip.model.ATTR_HERO_IMAGE_ATTRIBUTION
import com.example.travelcents.data.trip.model.ATTR_HERO_IMAGE_URL
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CITY
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.YelpOptionPoolItem
import com.example.travelcents.data.trip.remote.FlightHeroImageRepository
import com.example.travelcents.data.trip.remote.SerpRepository
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.TicketmasterRepository
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_RESTAURANTS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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

private data class ActivityScheduleWindow(
    val startDate: String,
    val minimumStartTime: String?,
    val endDate: String,
    val maximumEndTime: String?,
    val flightArrivalDate: String?,
    val flightArrivalTime: String?,
    val returnDepartureDate: String?,
    val returnDepartureTime: String?
)

private fun ActivityScheduleWindow.toPayloadMap(): Map<String, Any> = buildMap {
    put("startDate", startDate)
    minimumStartTime?.let { put("minimumStartTime", it) }
    put("endDate", endDate)
    maximumEndTime?.let { put("maximumEndTime", it) }
    flightArrivalDate?.let { put("flightArrivalDate", it) }
    flightArrivalTime?.let { put("flightArrivalTime", it) }
    returnDepartureDate?.let { put("returnDepartureDate", it) }
    returnDepartureTime?.let { put("returnDepartureTime", it) }
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

    private val flightHeroImages: FlightHeroImageRepository by lazy {
        val wikipediaClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val wikipedia = retrofit2.Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(wikipediaClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.travelcents.data.trip.remote.WikipediaApiService::class.java)
        FlightHeroImageRepository(
            destinationImages = com.example.travelcents.data.trip.remote.DestinationImageRepository(wikipedia)
        )
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
            interests = interests,
            specialRequests = specialRequests
        )

        viewModelScope.launch {
            try {
                // Step 1: AI planner generates itinerary metadata + IATA codes
                _generationStep.value = GenerationStep.CRAFTING_ITINERARY
                _uiState.value = TripUiState.Loading(LLM_ITINERARY_MESSAGES.random())
                val itinerary = TripPlannerRepository.generateItinerary(request)

                // Step 2: Flights + hotels in parallel
                _generationStep.value = GenerationStep.SEARCHING_FLIGHTS
                _uiState.value = TripUiState.Loading(SERP_FLIGHTS_MESSAGES.random())
                val flightsDeferred = async { SerpRepository.searchFlights(request, itinerary) }
                _generationStep.value = GenerationStep.FINDING_HOTELS
                _uiState.value = TripUiState.Loading(SERP_HOTELS_MESSAGES.random())
                val hotelsDeferred = async { SerpRepository.searchHotels(request, itinerary) }
                val realFlights = enrichFlightsWithHeroImages(flightsDeferred.await())
                val realHotels = hotelsDeferred.await()

                // Remaining budget for activity guidance
                val flightPrice = realFlights.firstOrNull()?.details?.get("total_price")?.toDoubleOrNull() ?: 0.0
                val hotelPerNight = realHotels.firstOrNull()?.details?.get("rate_per_night")?.toDoubleOrNull() ?: 0.0
                val hotelTotal = hotelPerNight * itinerary.durationDays
                val remainingBudget = if (budget > 0) maxOf(0.0, budget - flightPrice - hotelTotal) else 0.0

                val outboundFlight = realFlights.firstOrNull { it.details["trip_segment"] == "outbound" }
                val returnFlight = realFlights.firstOrNull { it.details["trip_segment"] == "return" }
                val scheduleWindow = buildActivityScheduleWindow(request, outboundFlight, returnFlight)
                val tripDates = generateActivityDates(request.dateFrom, scheduleWindow.endDate, scheduleWindow.startDate)
                val flightContext = buildFlightContext(realFlights)
                val hotelContext = buildHotelContext(realHotels.firstOrNull())
                val yelpOptionPools = linkedMapOf<String, List<YelpOptionPoolItem>>()

                // Step 3: Yelp restaurants — fetch a compact shared pool and only persist the selected daily event.
                _generationStep.value = GenerationStep.FINDING_RESTAURANTS
                _uiState.value = TripUiState.Loading(YELP_RESTAURANTS_MESSAGES.random())
                val restaurantEvents = if (tripDates.isEmpty()) {
                    emptyList()
                } else {
                    val restaurantPool = YelpRepository.mapBusinessesToPoolItems(
                        YelpRepository.fetchRestaurantPool(
                        location = itinerary.destination,
                        targetCount = sharedYelpPoolTarget(tripDates.size)
                        )
                    )
                    if (restaurantPool.isNotEmpty()) {
                        yelpOptionPools[YELP_POOL_TYPE_RESTAURANTS] = restaurantPool
                    }
                    applyActivityWindow(
                        YelpRepository.distributePoolToSelectedEvents(
                            restaurantPool, tripDates, "restaurant", itinerary.itineraryId
                        ),
                        earliestDate = scheduleWindow.startDate,
                        minimumStartTime = scheduleWindow.minimumStartTime,
                        latestDate = scheduleWindow.endDate,
                        maximumEndTime = scheduleWindow.maximumEndTime
                    )
                }

                val firstFlightArrival = formatWindowDateTime(
                    scheduleWindow.flightArrivalDate,
                    scheduleWindow.flightArrivalTime
                ) ?: "Unknown"

                // Step 4: AI activities + local events (full trip range), in parallel
                _generationStep.value = GenerationStep.FINDING_ACTIVITIES
                _uiState.value = TripUiState.Loading(AI_ACTIVITIES_MESSAGES.random())
                val activityEvents: List<TravelEvent>
                val localEvents: List<TravelEvent>
                if (tripDates.isEmpty()) {
                    activityEvents = emptyList()
                    localEvents = emptyList()
                } else {
                    val aiActivitiesDeferred = async {
                        runCatching {
                            TripPlannerRepository.getAIActivities(
                                request = request,
                                itineraryId = itinerary.itineraryId,
                                dates = tripDates,
                                flightArrival = firstFlightArrival,
                                activityWindow = scheduleWindow.toPayloadMap(),
                                flights = flightContext,
                                hotel = hotelContext
                            )
                        }.getOrElse { error ->
                            Log.w(TAG, "Local activity emulator unavailable; continuing without AI inventory activities.", error)
                            emptyList()
                        }
                    }
                    val yelpEventsDeferred = async {
                        runCatching {
                            YelpRepository.searchEvents(
                                location = itinerary.destination,
                                startDate = scheduleWindow.startDate,
                                endDate = scheduleWindow.endDate,
                                itineraryId = itinerary.itineraryId
                            )
                        }.getOrElse { error ->
                            Log.w(TAG, "Yelp activity fallback unavailable; continuing without Yelp events.", error)
                            emptyList()
                        }
                    }
                    val ticketmasterEventsDeferred = async {
                        runCatching {
                            if (BuildConfig.TICKETMASTER_API_KEY.isBlank()) {
                                emptyList()
                            } else {
                                TicketmasterRepository.searchEventsForTrip(
                                    location = itinerary.destination,
                                    startDate = scheduleWindow.startDate,
                                    endDate = scheduleWindow.endDate,
                                    itineraryId = itinerary.itineraryId,
                                    classification = interestsToTicketmasterClassification(request.interests)
                                )
                            }
                        }.getOrElse { error ->
                            Log.w(TAG, "Ticketmaster activity fallback unavailable; continuing without ticketed events.", error)
                            emptyList()
                        }
                    }
                    activityEvents = applyActivityWindow(
                        aiActivitiesDeferred.await(),
                        earliestDate = scheduleWindow.startDate,
                        minimumStartTime = scheduleWindow.minimumStartTime,
                        latestDate = scheduleWindow.endDate,
                        maximumEndTime = scheduleWindow.maximumEndTime
                    )
                    val mergedLocalEvents = mergeLocalActivityEvents(
                        yelpEvents = yelpEventsDeferred.await(),
                        ticketmasterEvents = ticketmasterEventsDeferred.await()
                    )
                    localEvents = filterEventsAfterTime(
                        filterEventsBeforeWindow(
                            mergedLocalEvents,
                            scheduleWindow.startDate,
                            scheduleWindow.minimumStartTime
                        ),
                        scheduleWindow.endDate,
                        scheduleWindow.maximumEndTime
                    )
                }

                val allEvents = normalizeGeneratedEventOrder(
                    realFlights + realHotels + restaurantEvents + activityEvents + localEvents
                )
                val linkedItinerary = itinerary.copy(eventIds = allEvents.map { it.eventId })

                // Step 5: Download selected hero images plus the selected hotel galleries.
                _generationStep.value = GenerationStep.DOWNLOADING_IMAGES
                _uiState.value = TripUiState.Loading(DOWNLOADING_MESSAGES.random())
                val heroImageUrls = allEvents
                    .filterNot { it.type.equals("hotel", ignoreCase = true) }
                    .map { it.imageUrl }
                val selectedHotelGalleryUrls = allEvents
                    .filter { it.type.equals("hotel", ignoreCase = true) }
                    .flatMap { it.remoteMediaUrls() }
                val mediaUrls = (heroImageUrls + selectedHotelGalleryUrls)
                    .filter { it.isNotBlank() }
                    .distinct()
                val localPaths = TripMediaCacheStore.cacheTripMedia(
                    context = getApplication(),
                    tripKey = TripKey(ownerUid = uid, tripId = itinerary.itineraryId),
                    urls = mediaUrls
                )

                // Keep the remote hero URL intact and persist the local cache path separately.
                val patchedEvents = allEvents.map { event ->
                    val localEventImg = localPaths[event.imageUrl]
                    if (localEventImg != null) event.copy(localImagePath = localEventImg) else event
                }

                // Step 6: Save to Firestore (events + options subcollection)
                _generationStep.value = GenerationStep.SAVING
                _uiState.value = TripUiState.Loading(FIRESTORE_MESSAGES.random())
                saveToFirestore(
                    uid = uid,
                    itinerary = linkedItinerary,
                    events = patchedEvents,
                    yelpOptionPools = yelpOptionPools
                )

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
        events: List<TravelEvent>,
        yelpOptionPools: Map<String, List<YelpOptionPoolItem>>
    ) {
        TripSyncRemoteDataSource(FirebaseFirestore.getInstance())
            .createTrip(
                ownerUid = uid,
                itinerary = itinerary,
                events = events,
                yelpOptionPools = yelpOptionPools
            )
    }

    fun resetState() {
        _uiState.value = TripUiState.Idle
        _generationStep.value = GenerationStep.IDLE
    }

    fun createDraftTripFromAiStarter(
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile,
        onTripReady: (TripKey) -> Unit = {}
    ) {
        commitItinerary(
            starter = starter,
            intakeProfile = intakeProfile,
            onTripReady = onTripReady
        )
    }

    fun commitItinerary(
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile,
        events: List<TravelEvent> = emptyList(),
        onTripReady: (TripKey) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.value = TripUiState.Error("You must be logged in to create a trip.")
            return
        }

        viewModelScope.launch {
            try {
                val tripKey = TripKey(ownerUid = uid, tripId = UUID.randomUUID().toString())
                val baseItinerary = buildDraftItinerary(
                    tripKey = tripKey,
                    starter = starter,
                    intakeProfile = intakeProfile
                )
                val committedEvents = events.map { event ->
                    event.copy(itineraryId = tripKey.tripId)
                }
                val itinerary = baseItinerary.copy(
                    eventIds = committedEvents.map(TravelEvent::eventId)
                )

                _generationStep.value = GenerationStep.SAVING
                _uiState.value = TripUiState.Loading("Creating your AI trip starter...")
                TripSyncRemoteDataSource(FirebaseFirestore.getInstance())
                    .createTrip(
                        ownerUid = uid,
                        itinerary = itinerary,
                        events = committedEvents
                    )

                _generationStep.value = GenerationStep.COMPLETE
                _uiState.value = TripUiState.Success(itinerary, committedEvents)
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

    private fun generateActivityDates(
        dateFrom: String,
        dateTo: String,
        firstActivityDate: String
    ): List<String> {
        val tripStart = parseTripDate(dateFrom) ?: return emptyList()
        val tripEnd = parseTripDate(dateTo) ?: return emptyList()
        if (tripEnd.isBefore(tripStart)) return emptyList()

        val requestedStart = parseTripDate(firstActivityDate) ?: tripStart
        val activityStart = if (requestedStart.isBefore(tripStart)) tripStart else requestedStart
        if (activityStart.isAfter(tripEnd)) return emptyList()

        return generateSequence(activityStart) { current ->
            current.plusDays(1).takeIf { !it.isAfter(tripEnd) }
        }.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
            .toList()
    }

    private fun parseTripDate(rawDate: String): LocalDate? {
        val normalized = normalizedTripDate(rawDate) ?: return null
        return runCatching { LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    private fun buildActivityScheduleWindow(
        request: TravelRequest,
        outboundFlight: TravelEvent?,
        returnFlight: TravelEvent?
    ): ActivityScheduleWindow {
        val arrivalDate = flightArrivalDate(outboundFlight) ?: request.dateFrom
        val arrivalTime = flightArrivalTime(outboundFlight)
        val earliestDateTime = buildDateTime(arrivalDate, arrivalTime)
            ?.plusHours(ACTIVITY_BUFFER_AFTER_ARRIVAL_HOURS)
            ?.let { candidate ->
                if (candidate.toLocalTime().isAfter(LATEST_FIRST_DAY_ACTIVITY_START)) {
                    LocalDateTime.of(candidate.toLocalDate().plusDays(1), DEFAULT_ACTIVITY_START_TIME)
                } else {
                    candidate
                }
            }
        val rawStartDate = earliestDateTime?.toLocalDate()?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ?: arrivalDate
        val parsedStartDate = parseTripDate(rawStartDate)
        val tripStartDate = parseTripDate(request.dateFrom)
        val startDate = if (parsedStartDate != null && tripStartDate != null && parsedStartDate.isBefore(tripStartDate)) {
            request.dateFrom
        } else {
            rawStartDate
        }
        val earliestTime = if (startDate == rawStartDate) {
            earliestDateTime?.toLocalTime()?.format(TRIP_TIME_FORMATTER)
        } else {
            null
        }
        val rawReturnDate = flightDepartureDate(returnFlight) ?: request.dateTo
        val parsedReturnDate = parseTripDate(rawReturnDate)
        val tripEndDate = parseTripDate(request.dateTo)
        val returnDate = if (parsedReturnDate != null && tripEndDate != null && parsedReturnDate.isAfter(tripEndDate)) {
            request.dateTo
        } else {
            rawReturnDate
        }
        val returnTime = flightDepartureTime(returnFlight)

        return ActivityScheduleWindow(
            startDate = startDate,
            minimumStartTime = earliestTime,
            endDate = returnDate,
            maximumEndTime = if (returnDate == rawReturnDate) returnTime else null,
            flightArrivalDate = arrivalDate,
            flightArrivalTime = arrivalTime,
            returnDepartureDate = returnDate,
            returnDepartureTime = if (returnDate == rawReturnDate) returnTime else null
        )
    }

    private fun buildFlightContext(flights: List<TravelEvent>): List<Map<String, Any>> {
        return flights
            .filter { it.type.equals("flight", ignoreCase = true) }
            .map { flight ->
                buildMap {
                    putIfNotBlank("segment", flight.details["trip_segment"])
                    putIfNotBlank("departureDate", flightDepartureDate(flight))
                    putIfNotBlank("departureTime", flightDepartureTime(flight))
                    putIfNotBlank("departureTimestamp", flight.details["departure_time"])
                    putIfNotBlank("arrivalDate", flightArrivalDate(flight))
                    putIfNotBlank("arrivalTime", flightArrivalTime(flight))
                    putIfNotBlank("originAirport", flight.details["origin_airport"])
                    putIfNotBlank("destinationAirport", flight.details["destination_airport"])
                    putIfNotBlank("durationMinutes", flight.details["flight_duration_min"])
                }
            }
    }

    private fun buildHotelContext(hotel: TravelEvent?): Map<String, Any> {
        if (hotel == null) return emptyMap()
        return buildMap {
            putIfNotBlank("name", hotel.detailValue(ATTR_HOTEL_NAME, "hotel_name", "title", "name"))
            putIfNotBlank("city", hotel.detailValue(ATTR_HOTEL_CITY, "city"))
            putIfNotBlank("checkInDate", hotel.details["check_in_date"] ?: hotel.date)
            putIfNotBlank("checkInTime", normalizedTripTime(hotel.detailValue(ATTR_CHECK_IN_TIME) ?: hotel.startTime))
            putIfNotBlank("checkOutDate", hotel.details["check_out_date"])
            putIfNotBlank("checkOutTime", normalizedTripTime(hotel.detailValue(ATTR_CHECK_OUT_TIME) ?: hotel.endTime))
            putIfNotBlank("latitude", hotel.detailValue(ATTR_LATITUDE))
            putIfNotBlank("longitude", hotel.detailValue(ATTR_LONGITUDE))
        }
    }

    private fun buildDateTime(date: String?, time: String?): LocalDateTime? {
        val parsedDate = date?.let(::parseTripDate) ?: return null
        val parsedTime = parseTripTime(time) ?: return null
        return LocalDateTime.of(parsedDate, parsedTime)
    }

    private fun flightArrivalDate(flight: TravelEvent?): String? {
        return normalizedTripDate(flight?.details?.get("arrival_date"))
            ?: normalizedTripDate(flight?.details?.get("arrival_time"))
            ?: normalizedTripDate(flight?.details?.get("leg_0_arrival"))
            ?: normalizedTripDate(flight?.date)
    }

    private fun flightArrivalTime(flight: TravelEvent?): String? {
        return normalizedTripTime(
            flight?.details?.get("arrival_time")?.takeIf { it.isNotBlank() }
                ?: flight?.endTime?.takeIf { it.isNotBlank() }
        )
    }

    private fun flightDepartureDate(flight: TravelEvent?): String? {
        return normalizedTripDate(flight?.details?.get("departure_time"))
            ?: normalizedTripDate(flight?.date)
    }

    private fun flightDepartureTime(flight: TravelEvent?): String? {
        return normalizedTripTime(
            flight?.startTime?.takeIf { it.isNotBlank() }
                ?: flight?.details?.get("departure_time")?.takeIf { it.isNotBlank() }
        )
    }

    private fun formatWindowDateTime(date: String?, time: String?): String? {
        return when {
            !date.isNullOrBlank() && !time.isNullOrBlank() -> "$date $time"
            !date.isNullOrBlank() -> date
            !time.isNullOrBlank() -> time
            else -> null
        }
    }

    private fun normalizedTripTime(rawTime: String?): String? {
        return parseTripTime(rawTime)?.format(TRIP_TIME_FORMATTER)
    }

    private fun MutableMap<String, Any>.putIfNotBlank(key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            put(key, value)
        }
    }

    private fun normalizedTripDate(rawDate: String?): String? {
        val value = rawDate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return ISO_DATE_REGEX.find(value)?.value
    }

    private fun normalizeGeneratedEventOrder(events: List<TravelEvent>): List<TravelEvent> {
        return events
            .groupBy { normalizedTripDate(it.date).orEmpty().ifBlank { it.date } }
            .toSortedMap(compareBy<String> { if (it.isBlank()) "9999-12-31" else it })
            .values
            .flatMap { dayEvents ->
                dayEvents
                    .sortedWith(
                        compareBy<TravelEvent>(
                            { parseTripTime(it.startTime) ?: LocalTime.MAX },
                            { generatedEventTypePriority(it) },
                            { it.eventId }
                        )
                    )
                    .mapIndexed { index, event ->
                        event.copy(
                            date = normalizedTripDate(event.date) ?: event.date,
                            details = event.details + ("sortOrder" to index.toString())
                        )
                    }
            }
    }

    private fun generatedEventTypePriority(event: TravelEvent): Int {
        return when (event.type.lowercase(Locale.US)) {
            "flight" -> 0
            "hotel" -> 1
            "restaurant", "dining", "food" -> 2
            else -> 3
        }
    }

    private fun applyActivityWindow(
        events: List<TravelEvent>,
        earliestDate: String,
        minimumStartTime: String?,
        latestDate: String,
        maximumEndTime: String?
    ): List<TravelEvent> {
        return filterEventsAfterTime(
            deferSyntheticEventsBeforeWindow(events, earliestDate, minimumStartTime),
            latestDate,
            maximumEndTime
        )
    }

    private fun deferSyntheticEventsBeforeWindow(
        events: List<TravelEvent>,
        earliestDate: String,
        minimumStartTime: String?
    ): List<TravelEvent> {
        val minDate = parseTripDate(earliestDate) ?: return events
        val minTime = parseTripTime(minimumStartTime)
        return events.map { event ->
            val eventDate = parseTripDate(event.date) ?: return@map event
            val start = parseTripTime(event.startTime)

            if (eventDate.isBefore(minDate)) {
                return@map event.copy(
                    date = earliestDate,
                    startTime = (minTime ?: start ?: DEFAULT_ACTIVITY_START_TIME).format(TRIP_TIME_FORMATTER),
                    endTime = shiftedEndTime(start, parseTripTime(event.endTime), minTime ?: start ?: DEFAULT_ACTIVITY_START_TIME)
                )
            }

            if (eventDate.isAfter(minDate) || minTime == null) return@map event
            if (start != null && !start.isBefore(minTime)) return@map event

            event.copy(
                startTime = minTime.format(TRIP_TIME_FORMATTER),
                endTime = shiftedEndTime(start, parseTripTime(event.endTime), minTime)
            )
        }
    }

    private fun filterEventsBeforeWindow(
        events: List<TravelEvent>,
        earliestDate: String,
        minimumStartTime: String?
    ): List<TravelEvent> {
        val minDate = parseTripDate(earliestDate) ?: return events
        val minTime = parseTripTime(minimumStartTime)
        return events.filter { event ->
            val eventDate = parseTripDate(event.date) ?: return@filter true
            when {
                eventDate.isBefore(minDate) -> false
                eventDate.isAfter(minDate) -> true
                minTime == null -> true
                else -> parseTripTime(event.startTime)?.let { start -> !start.isBefore(minTime) } ?: false
            }
        }
    }

    private fun shiftedEndTime(originalStart: LocalTime?, originalEnd: LocalTime?, newStart: LocalTime): String {
        val durationMinutes = if (originalStart != null && originalEnd != null && originalEnd.isAfter(originalStart)) {
            Duration.between(originalStart, originalEnd).toMinutes()
        } else {
            DEFAULT_ACTIVITY_DURATION_MINUTES
        }
        return newStart.plusMinutes(durationMinutes).format(TRIP_TIME_FORMATTER)
    }

    private fun filterEventsAfterTime(
        events: List<TravelEvent>,
        targetDate: String,
        maximumEndTime: String?
    ): List<TravelEvent> {
        val maxTime = parseTripTime(maximumEndTime) ?: return events
        return events.filterNot { event ->
            if (event.date != targetDate) return@filterNot false

            val start = parseTripTime(event.startTime)
            val end = parseTripTime(event.endTime)
            (start != null && !start.isBefore(maxTime)) ||
                (end != null && end.isAfter(maxTime))
        }
    }

    private fun parseTripTime(rawTime: String?): LocalTime? {
        val value = rawTime?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalized = value.replace(Regex("\\s+"), " ")
        val clockCandidate = if (MERIDIEM_REGEX.containsMatchIn(normalized)) {
            normalized.split(" ").takeLast(2).joinToString(" ")
        } else {
            normalized
                .substringAfterLast("T")
                .substringAfterLast(" ")
                .substringBefore("+")
                .substringBefore("-")
                .removeSuffix("Z")
        }
        return sequenceOf(clockCandidate, normalized)
            .distinct()
            .mapNotNull { candidate ->
                TRIP_INPUT_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
                    runCatching { LocalTime.parse(candidate, formatter) }.getOrNull()
                }
            }
            .firstOrNull()
    }

    private fun sharedYelpPoolTarget(dayCount: Int): Int {
        return (dayCount + 4).coerceIn(10, 15)
    }

    private fun interestsToTicketmasterClassification(interests: List<String>): String? {
        val mapped = interests
            .mapNotNull(::ticketmasterClassificationForInterest)
            .toSet()
        return mapped.singleOrNull()
    }

    private fun ticketmasterClassificationForInterest(rawInterest: String): String? {
        val interest = rawInterest
            .replace('_', ' ')
            .trim()
            .lowercase(Locale.US)

        return when {
            interest.contains("music") ||
                interest.contains("festival") ||
                interest.contains("concert") ||
                interest.contains("jazz") ||
                interest.contains("nightlife") -> "music"

            interest.contains("sport") ||
                interest.contains("golf") ||
                interest.contains("tennis") ||
                interest.contains("marathon") ||
                interest.contains("triathlon") ||
                interest.contains("skateboarding") ||
                interest.contains("bmx") ||
                interest.contains("motocross") ||
                interest.contains("go kart") ||
                interest.contains("f1") ||
                interest.contains("ski") -> "sports"

            interest.contains("culture") ||
                interest.contains("museum") ||
                interest.contains("art") ||
                interest.contains("history") ||
                interest.contains("architecture") ||
                interest.contains("comedy") ||
                interest.contains("theater") ||
                interest.contains("theatre") ||
                interest.contains("opera") ||
                interest.contains("ballet") ||
                interest.contains("painting") ||
                interest.contains("pottery") -> "arts"

            interest.contains("family") ||
                interest.contains("theme park") -> "family"

            interest.contains("film") -> "film"

            else -> null
        }
    }

    private fun mergeLocalActivityEvents(
        yelpEvents: List<TravelEvent>,
        ticketmasterEvents: List<TravelEvent>
    ): List<TravelEvent> {
        if (yelpEvents.isEmpty()) return ticketmasterEvents
        if (ticketmasterEvents.isEmpty()) return yelpEvents

        return (yelpEvents + ticketmasterEvents)
            .groupBy(::localActivityDedupKey)
            .values
            .map(::preferredLocalActivityEvent)
    }

    private fun preferredLocalActivityEvent(events: List<TravelEvent>): TravelEvent {
        return events.maxWithOrNull(
            compareBy<TravelEvent>(
                { if (isTicketmasterBacked(it)) 1 else 0 },
                { localActivitySignalScore(it) }
            )
        ) ?: events.first()
    }

    private fun localActivitySignalScore(event: TravelEvent): Int {
        return listOf(
            event.detailValue(ATTR_VENUE_NAME),
            event.detailValue(ATTR_BUSINESS_ADDRESS, "address"),
            event.detailValue("description"),
            event.detailValue("location"),
            event.imageUrl.takeIf { it.isNotBlank() }
        ).count { !it.isNullOrBlank() }
    }

    private fun localActivityDedupKey(event: TravelEvent): String {
        val date = event.date.ifBlank { "date_tbd" }
        val startTime = event.startTime.ifBlank { "time_tbd" }
        val venueToken = event.detailValue(
            ATTR_VENUE_NAME,
            ATTR_BUSINESS_ADDRESS,
            "location",
            "address",
            ATTR_BUSINESS_NAME,
            "activity_name"
        ).normalizedDedupToken()
        return "$date|$startTime|$venueToken"
    }

    private fun isTicketmasterBacked(event: TravelEvent): Boolean {
        return !event.detailValue(ATTR_TICKETMASTER_EVENT_ID).isNullOrBlank()
    }

    private fun String?.normalizedDedupToken(): String {
        return this
            ?.lowercase(Locale.US)
            ?.replace("&", " and ")
            ?.replace(Regex("[^a-z0-9 ]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.ifBlank { "unknown" }
            ?: "unknown"
    }

    companion object {
        private const val TAG = "NewTripViewModel"
        private const val ACTIVITY_BUFFER_AFTER_ARRIVAL_HOURS = 2L
        private const val DEFAULT_ACTIVITY_DURATION_MINUTES = 120L
        private val ISO_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val TRIP_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private val DEFAULT_ACTIVITY_START_TIME = LocalTime.of(10, 0)
        private val LATEST_FIRST_DAY_ACTIVITY_START = LocalTime.of(20, 0)
        private val TRIP_INPUT_TIME_FORMATTERS = listOf(
            TRIP_TIME_FORMATTER,
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("h:mm a", Locale.US),
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        )
        private val MERIDIEM_REGEX = Regex("\\b[AP]M\\b", RegexOption.IGNORE_CASE)
        private val LLM_ITINERARY_MESSAGES = listOf(
            "Asking the AI planner to map out your trip...",
            "The AI planner is crafting your itinerary...",
            "Generating trip structure with the itinerary model...",
            "Consulting the AI planner for travel ideas..."
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
        private val AI_ACTIVITIES_MESSAGES = listOf(
            "Discovering activities and attractions...",
            "Generating AI-picked activities...",
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

    private fun buildDraftItinerary(
        tripKey: TripKey,
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile
    ): Itinerary {
        val (draftAdults, draftChildren) = inferTravelerCounts(intakeProfile)
        return Itinerary(
            itineraryId = tripKey.tripId,
            userId = tripKey.ownerUid,
            tripName = starter.title.ifBlank { starter.destination.ifBlank { "AI Trip Starter" } },
            destination = starter.destination,
            origin = intakeProfile.origin,
            timeZoneId = DestinationTimeZones.resolveTimeZoneId(starter.destination).orEmpty(),
            dateFrom = "",
            dateTo = "",
            durationDays = starter.durationDays.coerceAtLeast(1),
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

    private suspend fun enrichFlightsWithHeroImages(flights: List<TravelEvent>): List<TravelEvent> {
        if (flights.isEmpty()) return flights
        return flights.map { event ->
            if (!event.type.equals("flight", ignoreCase = true)) return@map event
            val destinationCity = event.details[ATTR_DESTINATION_CITY]
                ?.takeIf { it.isNotBlank() }
                .orEmpty()
            val airlineIata = FlightHeroImageRepository.extractAirlineIata(event.details["flight_number"])
            val resolved = flightHeroImages.resolveFlightHero(destinationCity, airlineIata)
                ?: return@map event
            event.copy(
                imageUrl = resolved.imageUrl,
                details = event.details + buildMap {
                    put(ATTR_HERO_IMAGE_URL, resolved.imageUrl)
                    resolved.attribution?.let { put(ATTR_HERO_IMAGE_ATTRIBUTION, it) }
                }
            )
        }
    }

    private fun inferTravelerCounts(intakeProfile: AiTripIntakeProfile): Pair<Int, Int> {
        val normalizedParty = intakeProfile.partySummary.lowercase()

        return when {
            intakeProfile.tripType == AiTripType.SOLO || "solo" in normalizedParty -> 1 to 0
            intakeProfile.tripType == AiTripType.ROMANTIC || "two adults" in normalizedParty || "for two" in normalizedParty -> 2 to 0
            intakeProfile.tripType == AiTripType.FAMILY || "family" in normalizedParty || "kids" in normalizedParty || "children" in normalizedParty -> 2 to 2
            intakeProfile.tripType == AiTripType.FRIENDS || "friends" in normalizedParty || "group" in normalizedParty -> 4 to 0
            else -> 2 to 0
        }
    }
}

sealed class TripUiState {
    data object Idle : TripUiState()
    data class Loading(val statusMessage: String = "Getting things ready...") : TripUiState()
    data class Success(val itinerary: Itinerary, val events: List<TravelEvent>) : TripUiState()
    data class Error(val message: String) : TripUiState()
}
