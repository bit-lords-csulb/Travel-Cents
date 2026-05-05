package com.example.travelcents.ui.main.newTrip

import android.app.Application
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
import com.example.travelcents.data.trip.model.ATTR_OPTION_DATE
import com.example.travelcents.data.trip.model.ATTR_OPTION_END_TIME
import com.example.travelcents.data.trip.model.ATTR_OPTION_START_TIME
import com.example.travelcents.data.trip.model.ATTR_OPTION_TIME_LABEL
import com.example.travelcents.data.trip.model.ATTR_OPTION_TZ
import com.example.travelcents.data.trip.model.ATTR_OPTION_VARIANT_KIND
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.YelpOptionPoolItem
import com.example.travelcents.data.trip.remote.FlightHeroImageRepository
import com.example.travelcents.data.trip.remote.SerpRepository
import com.example.travelcents.data.trip.remote.buildFlightHeroImageRepository
import com.example.travelcents.data.trip.remote.enrichFlightHeroImages
import com.example.travelcents.data.trip.remote.TicketmasterRepository
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_ACTIVITIES
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_RESTAURANTS
import com.example.travelcents.data.trip.model.detailValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
        buildFlightHeroImageRepository()
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
                val flightArrivalDate = outboundFlight?.details?.get("arrival_date")
                    ?.takeIf { it.isNotBlank() }
                    ?: request.dateFrom
                val tripEdgeGuardrails = TripEdgeScheduler.buildGuardrails(
                    outboundFlight = outboundFlight,
                    tripStartDate = request.dateFrom,
                    returnFlight = returnFlight,
                    tripEndDate = request.dateTo
                )
                val tripDates = generateActivityDates(request.dateFrom, request.dateTo, flightArrivalDate)
                val yelpOptionPools = linkedMapOf<String, List<YelpOptionPoolItem>>()
                val constrainedHotels = TripEdgeScheduler.applyFlexibleEventWindow(realHotels, tripEdgeGuardrails)

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
                        tripEdgeGuardrails = tripEdgeGuardrails
                    )
                }

                // Step 4: Yelp activities (paged pooled fetch) + Yelp events (full trip range), in parallel
                _generationStep.value = GenerationStep.FINDING_ACTIVITIES
                _uiState.value = TripUiState.Loading(YELP_ACTIVITIES_MESSAGES.random())
                val activityEvents: List<TravelEvent>
                val localEvents: List<TravelEvent>
                if (tripDates.isEmpty()) {
                    activityEvents = emptyList()
                    localEvents = emptyList()
                } else {
                    val activityPoolDeferred = async {
                        YelpRepository.mapBusinessesToPoolItems(
                            YelpRepository.fetchActivityPool(
                            location = itinerary.destination,
                            targetCount = sharedYelpPoolTarget(tripDates.size)
                            )
                        )
                    }
                    val yelpEventsDeferred = async {
                        YelpRepository.searchEvents(
                            location = itinerary.destination,
                            startDate = flightArrivalDate,
                            endDate = request.dateTo,
                            itineraryId = itinerary.itineraryId
                        )
                    }
                    val ticketmasterEventsDeferred = async {
                        if (BuildConfig.TICKETMASTER_API_KEY.isBlank()) {
                            emptyList()
                        } else {
                            TicketmasterRepository.searchEventsForTrip(
                                location = itinerary.destination,
                                startDate = flightArrivalDate,
                                endDate = request.dateTo,
                                itineraryId = itinerary.itineraryId,
                                classification = interestsToTicketmasterClassification(request.interests)
                            )
                        }
                    }
                    val activityPool = activityPoolDeferred.await()
                    if (activityPool.isNotEmpty()) {
                        yelpOptionPools[YELP_POOL_TYPE_ACTIVITIES] = activityPool
                    }
                    activityEvents = applyActivityWindow(
                        YelpRepository.distributePoolToSelectedEvents(
                            activityPool, tripDates, "activity", itinerary.itineraryId
                        ),
                        tripEdgeGuardrails = tripEdgeGuardrails
                    )
                    val mergedLocalEvents = assembleLocalActivityEvents(
                        yelpEvents = yelpEventsDeferred.await(),
                        ticketmasterEvents = ticketmasterEventsDeferred.await(),
                        tripStartDate = tripDates.firstOrNull() ?: flightArrivalDate
                    )
                    localEvents = TripEdgeScheduler.filterFixedEventWindow(
                        mergedLocalEvents,
                        tripEdgeGuardrails
                    )
                }

                val allEvents = realFlights + constrainedHotels + restaurantEvents + activityEvents + localEvents
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
        return runCatching { LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    private fun applyActivityWindow(
        events: List<TravelEvent>,
        tripEdgeGuardrails: TripEdgeGuardrails
    ): List<TravelEvent> {
        return TripEdgeScheduler.applyFlexibleEventWindow(events, tripEdgeGuardrails)
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

    private fun assembleLocalActivityEvents(
        yelpEvents: List<TravelEvent>,
        ticketmasterEvents: List<TravelEvent>,
        tripStartDate: String
    ): List<TravelEvent> {
        val ticketmasterSelections = ticketmasterEvents.normalizeTicketmasterEvents(tripStartDate)
        if (yelpEvents.isEmpty()) return ticketmasterSelections
        if (ticketmasterSelections.isEmpty()) return yelpEvents

        return (yelpEvents + ticketmasterSelections)
            .groupBy(::localActivityDedupKey)
            .values
            .map(::preferredLocalActivityEvent)
    }

    private fun List<TravelEvent>.normalizeTicketmasterEvents(
        tripStartDate: String
    ): List<TravelEvent> {
        if (isEmpty()) return emptyList()
        val parsedTripStart = parseTripDate(tripStartDate) ?: return emptyList()
        return groupBy(::ticketmasterIdentityKey)
            .values
            .map(::canonicalTicketmasterEvent)
            .groupBy { event ->
                val eventDate = parseTripDate(event.date) ?: parsedTripStart
                (ChronoUnit.DAYS.between(parsedTripStart, eventDate).coerceAtLeast(0) / 7L).toInt()
            }
            .values
            .flatMap { weekEvents ->
                weekEvents.sortedWith(
                    compareByDescending<TravelEvent> { localActivitySignalScore(it) }
                        .thenBy { it.date.ifBlank { "9999-12-31" } }
                        .thenBy { it.startTime.ifBlank { "99:99" } }
                        .thenBy { it.details["title"].orEmpty() }
                ).take(1)
            }
    }

    private fun canonicalTicketmasterEvent(events: List<TravelEvent>): TravelEvent {
        val canonical = events.sortedWith(
            compareBy<TravelEvent>(
                { it.date.ifBlank { "9999-12-31" } },
                { it.startTime.ifBlank { "99:99" } },
                { -localActivitySignalScore(it) }
            )
        ).first()
        val selectedOption = canonical.toTicketmasterOption(selected = true)
        val alternateOptions = events
            .filterNot { it.eventId == canonical.eventId }
            .sortedWith(compareBy({ it.date.ifBlank { "9999-12-31" } }, { it.startTime.ifBlank { "99:99" } }))
            .map { it.toTicketmasterOption(selected = false) }

        return canonical.copy(
            selectedOptionId = selectedOption.optionId,
            options = listOf(selectedOption) + alternateOptions
        )
    }

    private fun TravelEvent.toTicketmasterOption(selected: Boolean): EventOption {
        val optionId = listOf(
            detailValue(ATTR_TICKETMASTER_EVENT_ID).orEmpty().ifBlank { eventId },
            date.ifBlank { "date_tbd" },
            startTime.ifBlank { "time_tbd" }
        ).joinToString("|")

        return EventOption(
            optionId = optionId,
            eventId = eventId,
            source = "ticketmaster",
            selected = selected,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            photoUrls = photoUrls,
            details = details + mapOf(
                ATTR_OPTION_DATE to date,
                ATTR_OPTION_START_TIME to startTime,
                ATTR_OPTION_END_TIME to endTime,
                ATTR_OPTION_TZ to tz,
                ATTR_OPTION_TIME_LABEL to listOfNotNull(
                    startTime.takeIf { it.isNotBlank() },
                    endTime.takeIf { it.isNotBlank() }
                ).joinToString(" - ").ifBlank { "Time TBD" },
                ATTR_OPTION_VARIANT_KIND to "ticketmaster_showtime"
            )
        )
    }

    private fun ticketmasterIdentityKey(event: TravelEvent): String {
        val titleToken = event.detailValue(ATTR_BUSINESS_NAME, "activity_name", "title", "name")
            .normalizedDedupToken()
        val venueToken = event.detailValue(
            ATTR_VENUE_NAME,
            ATTR_BUSINESS_ADDRESS,
            "location",
            "address"
        ).normalizedDedupToken()
        return "$titleToken|$venueToken"
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
        private val TRIP_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
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
        return enrichFlightHeroImages(flights, flightHeroImages)
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

