package com.example.travelcents.ui.main.current

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiCuratedTripToItineraryMapper
import com.example.travelcents.data.ai.chat.AiDestinationLockMapper
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.ai.chat.PREVIEW_TRIP_STATUS
import com.example.travelcents.data.media.TripMediaCacheStore
import com.example.travelcents.data.local.trip.TravelCentsDatabase
import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.sync.CurrentTripSyncCoordinator
import com.example.travelcents.data.sync.TripHydrationWorker
import com.example.travelcents.data.sync.TripSyncCoordinator
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripPlanActionService
import com.example.travelcents.data.trip.TripPerformanceLogger
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.local.DestinationTimeZones
import com.example.travelcents.data.trip.model.ATTR_BIKE_SCORE
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_CURRENT_BUSYNESS
import com.example.travelcents.data.trip.model.ATTR_ESTIMATED_HOME_COST
import com.example.travelcents.data.trip.model.ATTR_ESTIMATED_LOCAL_COST
import com.example.travelcents.data.trip.model.ATTR_ESTIMATED_WAIT_MIN
import com.example.travelcents.data.trip.model.ATTR_FX_HISTORY_30D
import com.example.travelcents.data.trip.model.ATTR_HAS_OUTDOOR_SEATING
import com.example.travelcents.data.trip.model.ATTR_HOME_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LOCAL_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_LYFT_DEEPLINK
import com.example.travelcents.data.trip.model.ATTR_NEAR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_NEIGHBORHOOD_NOTE
import com.example.travelcents.data.trip.model.ATTR_OPTION_DATE
import com.example.travelcents.data.trip.model.ATTR_OPTION_END_TIME
import com.example.travelcents.data.trip.model.ATTR_OPTION_START_TIME
import com.example.travelcents.data.trip.model.ATTR_POPULAR_TIMES_JSON
import com.example.travelcents.data.trip.model.ATTR_PRICE_LEVEL_USD
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.ATTR_RIDESHARE_ESTIMATE_USD
import com.example.travelcents.data.trip.model.ATTR_RIDESHARE_MIN
import com.example.travelcents.data.trip.model.ATTR_TRANSIT_MIN
import com.example.travelcents.data.trip.model.ATTR_TRANSIT_SCORE
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_TRANSPORT_ANCHOR_LABEL
import com.example.travelcents.data.trip.model.ATTR_UBER_DEEPLINK
import com.example.travelcents.data.trip.model.ATTR_WALK_MIN
import com.example.travelcents.data.trip.model.ATTR_WALK_SCORE
import com.example.travelcents.data.trip.model.ATTR_WEATHER_CONDITION
import com.example.travelcents.data.trip.model.ATTR_WEATHER_PRECIP_PCT
import com.example.travelcents.data.trip.model.ATTR_WEATHER_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_WEATHER_TEMP_C
import com.example.travelcents.data.trip.model.ATTR_WEATHER_WIND_KPH
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_ACTIVITIES
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_RESTAURANTS
import com.example.travelcents.data.trip.model.YelpOptionPoolItem
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.data.trip.model.resolveTripName
import com.example.travelcents.data.trip.remote.CurrencyPreviewRepository
import com.example.travelcents.data.trip.remote.FlightHeroImageRepository
import com.example.travelcents.data.trip.remote.PopularTimesRepository
import com.example.travelcents.data.trip.remote.TransportRepository
import com.example.travelcents.data.trip.remote.WalkScoreRepository
import com.example.travelcents.data.trip.remote.WeatherRepository
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.data.trip.remote.buildFlightHeroImageRepository
import com.example.travelcents.data.trip.remote.enrichFlightHeroImages
import com.example.travelcents.data.trip.remote.needsFlightHeroBackfill
import com.example.travelcents.ui.main.shared.TripMediaDetailPipeline
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.normalizeTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Currency
import java.util.Locale
import java.util.UUID

data class EditablePlan(
    val eventId: String? = null,
    val type: String = "activity",
    val title: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val timeZoneId: String = defaultPlanTimeZoneId(),
    val location: String = "",
    val notes: String = "",
    val colorKey: String = "rose",
    val imageUrl: String = "",
    val existingDetails: Map<String, String> = emptyMap()
)

data class TripMemberUi(
    val uid: String,
    val displayName: String,
    val initial: Char
)

data class CurrentTripUiState(
    val isLoading: Boolean = true,
    val currentTripId: String? = null,
    val currentTripOwnerUid: String? = null,
    val viewerUid: String? = null,
    val accessRole: TripAccessRole = TripAccessRole.VIEWER,
    val canEditTrip: Boolean = false,
    val canManageTrip: Boolean = false,
    val tripTitle: String = "Loading Trip...",
    val destination: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val adults: Int = 1,
    val children: Int = 0,
    val events: List<TravelEvent> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isPreview: Boolean = false
)

sealed class PreviewSource {
    data class CuratedStarter(
        val starter: AiCuratedTripStarter,
        val intakeProfile: AiTripIntakeProfile
    ) : PreviewSource()

    data class DestinationLock(
        val destination: String,
        val intakeProfile: AiTripIntakeProfile
    ) : PreviewSource()
}

data class ShareTarget(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val memberUids: List<String> = emptyList()
)

class CurrentTripViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val DEFAULT_TRIP_TITLE = "Loading Trip..."
        private const val EMPTY_PLANS_MESSAGE = "No plans yet. Tap + to add one."
        private const val NO_TRIP_MESSAGE = "No trip found yet. Create one from the New Trip tab."
        private const val SHARED_YELP_VISIBLE_OPTIONS = 5
        private const val SHARED_YELP_POOL_EXPANSION_SIZE = 10
    }

    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()

    private val _tripTitle = MutableStateFlow(DEFAULT_TRIP_TITLE)
    val tripTitle: StateFlow<String> = _tripTitle.asStateFlow()

    private val _uiState = MutableStateFlow(CurrentTripUiState())
    val uiState: StateFlow<CurrentTripUiState> = _uiState.asStateFlow()

    private val _allTrips = MutableStateFlow<List<Itinerary>>(emptyList())
    val allTrips: StateFlow<List<Itinerary>> = _allTrips.asStateFlow()

    private val _eventOptions = MutableStateFlow<Map<String, List<EventOption>>>(emptyMap())
    val eventOptions: StateFlow<Map<String, List<EventOption>>> = _eventOptions.asStateFlow()

    private val _rejectedOptions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val rejectedOptions: StateFlow<Map<String, Set<String>>> = _rejectedOptions.asStateFlow()

    private val _optionsLoading = MutableStateFlow<Set<String>>(emptySet())
    val optionsLoading: StateFlow<Set<String>> = _optionsLoading.asStateFlow()

    private val _yelpReviews = MutableStateFlow<Map<String, List<YelpReview>>>(emptyMap())
    val yelpReviews: StateFlow<Map<String, List<YelpReview>>> = _yelpReviews.asStateFlow()

    private val _reviewsLoading = MutableStateFlow<Set<String>>(emptySet())
    val reviewsLoading: StateFlow<Set<String>> = _reviewsLoading.asStateFlow()
    private val _yelpEnrichmentInFlight = MutableStateFlow<Set<String>>(emptySet())
    private val _restaurantLiveContextInFlight = MutableStateFlow<Set<String>>(emptySet())

    private val _shareTargets = MutableStateFlow<List<ShareTarget>>(emptyList())
    val shareTargets: StateFlow<List<ShareTarget>> = _shareTargets.asStateFlow()

    private val _tripMembers = MutableStateFlow<List<TripMemberUi>>(emptyList())
    val tripMembers: StateFlow<List<TripMemberUi>> = _tripMembers.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tripRepository: TripRepository = FirestoreTripRepository(db)
    private val tripPlanActionService = TripPlanActionService()
    private val tripLocalDataSource = TripLocalDataSource(TravelCentsDatabase.getInstance(application))
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(db)
    private val tripSyncCoordinator = TripSyncCoordinator(
        localDataSource = tripLocalDataSource,
        remoteDataSource = tripSyncRemoteDataSource,
        legacyRemoteRepository = tripRepository
    )
    private val currentTripSyncCoordinator = CurrentTripSyncCoordinator(
        localDataSource = tripLocalDataSource,
        remoteDataSource = tripSyncRemoteDataSource,
        homeSyncCoordinator = tripSyncCoordinator,
        legacyRemoteRepository = tripRepository
    )
    private var currentTripSummaryJob: Job? = null
    private var currentTripEventsJob: Job? = null
    private var currentTripMembersJob: Job? = null
    private var currentTripOptionsJob: Job? = null
    private var allTripsJob: Job? = null
    private var allTripsObserverUid: String? = null
    private var currentTripKey: TripKey? = null
    private var currentTripSummary: Itinerary? = null
    private var currentTripDestination: String = ""
    private var currentTripTimeZoneId: String = ""
    private var localEventsSnapshot: List<TravelEvent> = emptyList()
    private val sharedYelpPools = mutableMapOf<String, List<YelpOptionPoolItem>>()
    private val sharedYelpWindowBoost = mutableMapOf<String, Int>()
    private val mediaDetailPipeline = TripMediaDetailPipeline(application)
    private val currencyPreviewRepository = CurrencyPreviewRepository(application)
    private val flightHeroImages: FlightHeroImageRepository by lazy { buildFlightHeroImageRepository() }
    private val flightHeroBackfillInFlight = mutableSetOf<String>()
    private var liveEventDetailOverrides: Map<String, Map<String, String>> = emptyMap()

    private fun resetTripState(
        isLoading: Boolean = false,
        tripTitle: String = DEFAULT_TRIP_TITLE,
        infoMessage: String? = null,
        errorMessage: String? = null
    ) {
        currentTripSummaryJob?.cancel()
        currentTripSummaryJob = null
        currentTripEventsJob?.cancel()
        currentTripEventsJob = null
        currentTripMembersJob?.cancel()
        currentTripMembersJob = null
        currentTripOptionsJob?.cancel()
        currentTripOptionsJob = null
        currentTripKey = null
        currentTripSummary = null
        currentTripDestination = ""
        currentTripTimeZoneId = ""
        localEventsSnapshot = emptyList()
        sharedYelpPools.clear()
        sharedYelpWindowBoost.clear()
        _events.value = emptyList()
        _tripTitle.value = tripTitle
        _eventOptions.value = emptyMap()
        _rejectedOptions.value = emptyMap()
        _optionsLoading.value = emptySet()
        _yelpReviews.value = emptyMap()
        _reviewsLoading.value = emptySet()
        _yelpEnrichmentInFlight.value = emptySet()
        _restaurantLiveContextInFlight.value = emptySet()
        _shareTargets.value = emptyList()
        _tripMembers.value = emptyList()
        liveEventDetailOverrides = emptyMap()
        flightHeroBackfillInFlight.clear()
        _uiState.value = CurrentTripUiState(
            isLoading = isLoading,
            tripTitle = tripTitle,
            viewerUid = auth.currentUser?.uid,
            infoMessage = infoMessage,
            errorMessage = errorMessage
        )
    }

    private fun observeCurrentTrip(
        viewerUid: String,
        tripKey: TripKey
    ) {
        currentTripKey = tripKey
        TripPerformanceLogger.bindTrip(tripKey.tripId)
        currentTripSummaryJob?.cancel()
        currentTripSummaryJob = viewModelScope.launch {
            tripLocalDataSource.observeTripSummary(viewerUid, tripKey).collect { itinerary ->
                if (itinerary != null) {
                    handleObservedTripSummary(tripKey, itinerary)
                }
            }
        }

        currentTripEventsJob?.cancel()
        currentTripEventsJob = viewModelScope.launch {
            tripLocalDataSource.observeTripEvents(tripKey).collect { events ->
                localEventsSnapshot = sortPlanEvents(events)
                publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
                backfillFlightHeroesIfNeeded(tripKey, localEventsSnapshot)
            }
        }

        currentTripMembersJob?.cancel()
        currentTripMembersJob = viewModelScope.launch {
            tripLocalDataSource.observeTripMembers(tripKey).collect { members ->
                _tripMembers.value = members
                    .filterNot { member -> member.memberUid == tripKey.ownerUid && members.size == 1 }
                    .map { member ->
                        val displayName = member.displayName.ifBlank { member.memberUid }
                        TripMemberUi(
                            uid = member.memberUid,
                            displayName = displayName,
                            initial = displayName.firstOrNull { it.isLetter() } ?: '?'
                        )
                    }
            }
        }

        currentTripOptionsJob?.cancel()
        currentTripOptionsJob = viewModelScope.launch {
            tripLocalDataSource.observeTripOptions(tripKey).collect { optionsByEvent ->
                _eventOptions.value = optionsByEvent
                _rejectedOptions.update { current ->
                    current.filterKeys { key -> key in optionsByEvent || localEventsSnapshot.any { it.eventId == key } }
                }
                publishCurrentEvents(localEventsSnapshot, optionsByEvent)
            }
        }
    }

    private fun handleObservedTripSummary(
        tripKey: TripKey,
        itinerary: Itinerary
    ) {
        val resolvedTripTimeZoneId = resolveTripTimeZoneId(itinerary)
        val effectiveItinerary = if (
            resolvedTripTimeZoneId.isNotBlank() &&
            resolvedTripTimeZoneId != itinerary.timeZoneId
        ) {
            itinerary.copy(timeZoneId = resolvedTripTimeZoneId)
        } else {
            itinerary
        }
        currentTripSummary = effectiveItinerary
        currentTripDestination = effectiveItinerary.destination
        currentTripTimeZoneId = effectiveItinerary.timeZoneId
        val viewerUid = auth.currentUser?.uid
        val accessRole = when {
            viewerUid.isNullOrBlank() -> TripAccessRole.VIEWER
            viewerUid == tripKey.ownerUid -> TripAccessRole.OWNER
            else -> TripAccessRole.fromWireValue(effectiveItinerary.roleByUid[viewerUid])
        }
        val canEditTrip = accessRole.canMutateEvents()
        val canManageTrip = accessRole.canManageTrip()
        val storedTripTitle = effectiveItinerary.tripName
        val nextTripTitle = resolveTripName(storedTripTitle, currentTripDestination)
        _tripTitle.value = nextTripTitle
        _uiState.update {
            it.copy(
                isLoading = false,
                currentTripId = tripKey.tripId,
                currentTripOwnerUid = tripKey.ownerUid,
                viewerUid = viewerUid,
                accessRole = accessRole,
                canEditTrip = canEditTrip,
                canManageTrip = canManageTrip,
                tripTitle = nextTripTitle,
                destination = currentTripDestination,
                dateFrom = effectiveItinerary.dateFrom,
                dateTo = effectiveItinerary.dateTo,
                adults = effectiveItinerary.adults,
                children = effectiveItinerary.children,
                infoMessage = if (localEventsSnapshot.isEmpty()) EMPTY_PLANS_MESSAGE else null,
                errorMessage = null
            )
        }

        backfillTripTimeZoneIfNeeded(
            viewerUid = viewerUid,
            tripKey = tripKey,
            originalItinerary = itinerary,
            resolvedItinerary = effectiveItinerary,
            canManageTrip = canManageTrip
        )
        backfillFlightHeroesIfNeeded(tripKey, localEventsSnapshot)

        if (canManageTrip && nextTripTitle != storedTripTitle.trim()) {
            viewModelScope.launch {
                runCatching {
                    tripSyncRemoteDataSource.updateTripSummaryFields(
                        tripKey = tripKey,
                        fields = mapOf("tripName" to nextTripTitle)
                    )
                }
            }
        }
    }

    private fun publishCurrentEvents(
        baseEvents: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ) {
        val alignedOptionsByEvent = alignEventOptionsWithSelectedState(baseEvents, optionsByEvent)
        if (alignedOptionsByEvent != _eventOptions.value) {
            _eventOptions.value = alignedOptionsByEvent
        }
        val enrichedEvents = mediaDetailPipeline.applySelectedOptions(
            events = baseEvents,
            optionsByEvent = alignedOptionsByEvent,
            sortEvents = ::sortPlanEvents
        )
        val visibleEvents = enrichedEvents.map { event ->
            val eventWithTripTimeZone = applyTripTimeZone(event)
            val overrides = liveEventDetailOverrides[event.eventId].orEmpty()
            if (overrides.isEmpty()) {
                eventWithTripTimeZone
            } else {
                eventWithTripTimeZone.copy(details = eventWithTripTimeZone.details + overrides)
            }
        }
        _events.value = visibleEvents
        _uiState.update {
            it.copy(
                isLoading = currentTripSummary == null && visibleEvents.isEmpty(),
                events = visibleEvents,
                infoMessage = when {
                    currentTripSummary == null -> it.infoMessage
                    visibleEvents.isEmpty() -> EMPTY_PLANS_MESSAGE
                    it.infoMessage == EMPTY_PLANS_MESSAGE -> null
                    else -> it.infoMessage
                },
                errorMessage = null
            )
        }
        prefetchSharedEventMedia(visibleEvents)
    }

    private fun resolveTripTimeZoneId(itinerary: Itinerary): String {
        return itinerary.timeZoneId.takeIf { it.isNotBlank() }
            ?: DestinationTimeZones.resolveTimeZoneId(
                destination = itinerary.destination,
                destinationIata = itinerary.destinationIata
            ).orEmpty()
    }

    private fun backfillTripTimeZoneIfNeeded(
        viewerUid: String?,
        tripKey: TripKey,
        originalItinerary: Itinerary,
        resolvedItinerary: Itinerary,
        canManageTrip: Boolean
    ) {
        if (viewerUid.isNullOrBlank()) return
        if (resolvedItinerary.timeZoneId.isBlank()) return
        if (originalItinerary.timeZoneId == resolvedItinerary.timeZoneId) return

        viewModelScope.launch {
            runCatching {
                tripLocalDataSource.upsertTripSummary(
                    viewerUid = viewerUid,
                    itinerary = resolvedItinerary,
                    isCurrentCandidate = true
                )
            }
            if (canManageTrip) {
                runCatching {
                    tripSyncRemoteDataSource.updateTripSummaryFields(
                        tripKey = tripKey,
                        fields = mapOf("timeZoneId" to resolvedItinerary.timeZoneId)
                    )
                }
            }
        }
    }

    private fun applyTripTimeZone(event: TravelEvent): TravelEvent {
        val tripTimeZoneId = currentTripTimeZoneId.takeIf { it.isNotBlank() } ?: return event
        if (event.tz.isNotBlank()) return event
        return event.copy(tz = tripTimeZoneId)
    }

    private fun replaceLiveDetailOverrides(
        eventId: String,
        overrides: Map<String, String>
    ) {
        val current = liveEventDetailOverrides[eventId].orEmpty()
        if (current == overrides) return

        liveEventDetailOverrides = liveEventDetailOverrides.toMutableMap().apply {
            if (overrides.isEmpty()) {
                remove(eventId)
            } else {
                put(eventId, overrides)
            }
        }
        publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
    }

    private fun backfillFlightHeroesIfNeeded(
        tripKey: TripKey,
        events: List<TravelEvent>
    ) {
        if (!_uiState.value.canEditTrip) return

        val candidates = events.filter { event ->
            event.type.equals("flight", ignoreCase = true) &&
                event.eventId !in flightHeroBackfillInFlight &&
                event.needsFlightHeroBackfill()
        }
        if (candidates.isEmpty()) return

        val candidateIds = candidates.mapTo(mutableSetOf()) { it.eventId }
        flightHeroBackfillInFlight.addAll(candidateIds)

        viewModelScope.launch {
            try {
                var enrichedCandidates = enrichFlightHeroImages(candidates, flightHeroImages)
                val heroUrls = enrichedCandidates
                    .map { it.imageUrl }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (heroUrls.isNotEmpty()) {
                    val localPaths = runCatching {
                        TripMediaCacheStore.cacheTripMedia(
                            context = getApplication(),
                            tripKey = tripKey,
                            urls = heroUrls
                        )
                    }.onFailure { error ->
                        Log.w("CurrentTripViewModel", "Failed to cache flight hero media", error)
                    }.getOrDefault(emptyMap())
                    if (localPaths.isNotEmpty()) {
                        enrichedCandidates = enrichedCandidates.map { event ->
                            localPaths[event.imageUrl]
                                ?.let { localPath -> event.copy(localImagePath = localPath) }
                                ?: event
                        }
                    }
                }
                val changedById = enrichedCandidates
                    .zip(candidates)
                    .mapNotNull { (updated, original) ->
                        updated.takeIf { it != original }?.let { it.eventId to it }
                    }
                    .toMap()
                if (changedById.isEmpty()) return@launch

                localEventsSnapshot = sortPlanEvents(
                    localEventsSnapshot.map { event -> changedById[event.eventId] ?: event }
                )
                publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
                persistLocalTripSnapshot(events = localEventsSnapshot)

                changedById.values.forEach { event ->
                    runCatching {
                        tripSyncRemoteDataSource.upsertEvent(tripKey = tripKey, event = event)
                    }.onFailure { error ->
                        Log.w("CurrentTripViewModel", "Failed to backfill flight hero image", error)
                    }
                }
            } finally {
                flightHeroBackfillInFlight.removeAll(candidateIds)
            }
        }
    }

    private fun alignEventOptionsWithSelectedState(
        events: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ): Map<String, List<EventOption>> {
        if (events.isEmpty() || optionsByEvent.isEmpty()) return optionsByEvent

        var changed = false
        val eventById = events.associateBy(TravelEvent::eventId)
        val alignedOptions = optionsByEvent.mapValues { (eventId, options) ->
            val event = eventById[eventId] ?: return@mapValues options
            val selectedOptionId = event.selectedOptionId
                .takeIf { it.isNotBlank() }
                ?: event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() }
                ?: return@mapValues options

            val normalizedOptions = options.map { option ->
                option.copy(selected = option.optionId == selectedOptionId)
            }.toMutableList()

            if (normalizedOptions.none(EventOption::selected) && yelpPoolTypeForEvent(event) != null) {
                normalizedOptions.add(
                    index = 0,
                    element = EventOption(
                        optionId = selectedOptionId,
                        eventId = eventId,
                        source = "yelp",
                        selected = true,
                        imageUrl = event.imageUrl,
                        localImagePath = event.localImagePath,
                        photoUrls = event.photoUrls,
                        details = event.details
                    )
                )
            }

            val dedupedOptions = normalizedOptions.distinctBy(EventOption::optionId)
            if (dedupedOptions != options) changed = true
            dedupedOptions
        }

        return if (changed) alignedOptions else optionsByEvent
    }

    private fun refreshCurrentTrip(
        viewerUid: String,
        tripKey: TripKey,
        missingTripMessage: String = "That trip is no longer available."
    ) {
        viewModelScope.launch {
            try {
                val refreshedSummary = currentTripSyncCoordinator.refreshTrip(
                    viewerUid = viewerUid,
                    tripKey = tripKey
                )
                if (refreshedSummary == null) {
                    resetTripState(infoMessage = missingTripMessage)
                } else {
                    TripHydrationWorker.enqueue(getApplication(), tripKey)
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "DATABASE ERROR: ${e.message}", e)
                if (currentTripSummary == null) {
                    resetTripState(errorMessage = e.message ?: "Failed to load trip.")
                } else {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to refresh trip.") }
                }
            }
        }
    }

    private fun refreshCurrentTripInBackground(tripKey: TripKey) {
        val viewerUid = auth.currentUser?.uid ?: return
        refreshCurrentTrip(viewerUid = viewerUid, tripKey = tripKey)
    }

    private fun refreshHomeTripCacheInBackground() {
        val viewerUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                tripSyncCoordinator.refreshHomeIfNeeded(viewerUid)
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to refresh local home trip cache", error)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun postError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun upsertPlan(plan: EditablePlan) {
        val tripKey = requireTripContributorKey("add or edit plans")
        if (auth.currentUser?.uid == null) {
            postError("Create or load a trip before adding plans.")
            return
        }
        if (tripKey == null) return

        viewModelScope.launch {
            try {
                val eventId = plan.eventId ?: UUID.randomUUID().toString()
                val mergedDetails = plan.existingDetails.toMutableMap().apply {
                    put("title", plan.title.trim())
                    put("colorKey", plan.colorKey)

                    if (plan.location.isBlank()) {
                        remove("location")
                    } else {
                        put("location", plan.location.trim())
                    }

                    if (plan.notes.isBlank()) {
                        remove("description")
                    } else {
                        put("description", plan.notes.trim())
                    }
                }

                val event = TravelEvent(
                    eventId = eventId,
                    type = plan.type,
                    itineraryId = tripKey.tripId,
                    tz = plan.timeZoneId.trim().ifBlank { defaultPlanTimeZoneId() },
                    date = normalizeDate(plan.date),
                    startTime = normalizeTime(plan.startTime),
                    endTime = normalizeTime(plan.endTime),
                    details = mergedDetails
                )

                localEventsSnapshot = sortPlanEvents(
                    localEventsSnapshot
                        .filterNot { existing -> existing.eventId == eventId }
                        .plus(event)
                )
                publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
                persistLocalTripSnapshot(events = localEventsSnapshot)
                tripSyncRemoteDataSource.upsertEvent(tripKey = tripKey, event = event)
                refreshCurrentTripInBackground(tripKey)

                _uiState.update {
                    it.copy(
                        infoMessage = if (plan.eventId == null) "Plan added to your trip." else "Plan updated.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to save event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save plan.") }
            }
        }
    }

    fun deletePlan(plan: EditablePlan) {
        val tripKey = requireTripContributorKey("delete plans")
        val eventId = plan.eventId

        if (auth.currentUser?.uid == null || eventId.isNullOrBlank()) {
            postError("This plan cannot be deleted yet.")
            return
        }
        if (tripKey == null) return

        viewModelScope.launch {
            try {
                localEventsSnapshot = sortPlanEvents(
                    localEventsSnapshot.filterNot { existing -> existing.eventId == eventId }
                )
                val updatedOptions = _eventOptions.value - eventId
                _eventOptions.value = updatedOptions
                _rejectedOptions.update { current -> current - eventId }
                publishCurrentEvents(localEventsSnapshot, updatedOptions)
                persistLocalTripSnapshot(
                    events = localEventsSnapshot,
                    options = updatedOptions,
                    persistOptions = true
                )
                tripSyncRemoteDataSource.deleteEvent(tripKey = tripKey, eventId = eventId)
                refreshCurrentTripInBackground(tripKey)

                _uiState.update {
                    it.copy(
                        infoMessage = "Plan deleted.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to delete event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete plan.") }
            }
        }
    }

    fun fetchYelpReviews(yelpId: String) {
        if (yelpId.isBlank() || yelpId in _reviewsLoading.value || yelpId in _yelpReviews.value) return

        viewModelScope.launch {
            _reviewsLoading.update { it + yelpId }
            val reviews = YelpRepository.getBusinessReviews(yelpId)
            _yelpReviews.update { it + (yelpId to reviews) }
            _reviewsLoading.update { it - yelpId }
        }
    }

    fun ensureYelpEventEnriched(
        eventId: String,
        forceRefresh: Boolean = false
    ) {
        val tripKey = currentTripWriteKeyIfOwner() ?: return
        val event = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return
        val options = _eventOptions.value[eventId].orEmpty()
        val yelpId = mediaDetailPipeline.yelpEnrichmentTargetId(
            event = event,
            options = options,
            inFlightIds = _yelpEnrichmentInFlight.value,
            forceRefresh = forceRefresh
        )
            ?: return

        _yelpEnrichmentInFlight.update { it + yelpId }
        TripPerformanceLogger.recordYelpEnrichmentAttempt(
            source = "CurrentTripViewModel.ensureYelpEventEnriched",
            detail = "tripId=${tripKey.tripId} eventId=$eventId type=${event.type}"
        )
        viewModelScope.launch {
            try {
                val result = YelpRepository.enrichYelpBackedEvent(
                    event = event,
                    options = options,
                    forceRefresh = forceRefresh
                ) ?: return@launch

                applyEnrichedEventState(eventId, result.event, result.options)
                persistYelpEnrichment(tripKey, eventId, result)
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to enrich Yelp event", e)
            } finally {
                _yelpEnrichmentInFlight.update { it - yelpId }
            }
        }
    }

    private fun applyEnrichedEventState(
        eventId: String,
        enrichedEvent: TravelEvent,
        enrichedOptions: List<EventOption>
    ) {
        val mergedEvent = mediaDetailPipeline.mergeEventWithOptions(enrichedEvent, enrichedOptions)

        localEventsSnapshot = sortPlanEvents(
            localEventsSnapshot.map { event ->
                if (event.eventId == eventId) mergedEvent else event
            }
        )

        val updatedOptions = _eventOptions.value + (eventId to enrichedOptions)
        _eventOptions.value = updatedOptions
        publishCurrentEvents(localEventsSnapshot, updatedOptions)
        persistLocalTripSnapshot(
            events = localEventsSnapshot,
            options = updatedOptions,
            persistOptions = true
        )
    }

    private suspend fun persistYelpEnrichment(
        tripKey: TripKey,
        eventId: String,
        result: YelpRepository.YelpEventEnrichmentResult
    ) {
        val shouldPersistEventOnly = yelpPoolTypeForEvent(result.event) != null &&
            result.event.selectedOptionId.isNotBlank()
        if (shouldPersistEventOnly) {
            tripSyncRemoteDataSource.upsertEvent(
                tripKey = tripKey,
                event = result.event
            )
        } else {
            tripSyncRemoteDataSource.persistEventAndOptions(
                tripKey = tripKey,
                eventId = eventId,
                event = result.event,
                options = result.options,
                updatedOptionIds = result.updatedOptionIds
            )
        }
        persistLocalTripSnapshot(
            tripKey = tripKey,
            events = localEventsSnapshot,
            options = _eventOptions.value,
            persistOptions = !shouldPersistEventOnly
        )
        refreshCurrentTripInBackground(tripKey)
    }

    fun refreshEventLiveContext(eventId: String) {
        val event = _uiState.value.events.firstOrNull { it.eventId == eventId } ?: return
        val isRestaurant = isRestaurantEvent(event)
        val isTicketmasterBacked = isTicketmasterBackedEvent(event)
        if ((!isRestaurant && !isTicketmasterBacked) || eventId in _restaurantLiveContextInFlight.value) return

        val venueName = event.displayName()?.takeIf { it.isNotBlank() }
        val venueAddress = event.detailValue(ATTR_BUSINESS_ADDRESS, "address")
            ?.takeIf { it.isNotBlank() }
        val transportAnchor = resolveTransportAnchor(event)
        val transportDestination = resolveTransportDestination(event)
        val latitude = event.detailValue(ATTR_LATITUDE)?.toDoubleOrNull()
        val longitude = event.detailValue(ATTR_LONGITUDE)?.toDoubleOrNull()
        val hasOutdoorSeating = event.detailValue(ATTR_HAS_OUTDOOR_SEATING)
            ?.equals("true", ignoreCase = true) == true
        val tripCurrency = currentTripSummary?.currency
            ?.takeIf { it.isNotBlank() }
            ?.uppercase(Locale.US)
        val homeCurrency = resolvedHomeCurrencyCode()
        val usdAnchorAmount = restaurantUsdAnchorAmount(event)
        val walkScoreAddress = venueAddress
            ?: transportDestination?.address
            ?: event.details["location"]?.takeIf { it.isNotBlank() }

        val lacksRestaurantInputs =
            venueAddress == null &&
                (transportAnchor == null || transportDestination == null) &&
                (!hasOutdoorSeating || latitude == null || longitude == null) &&
                (latitude == null || longitude == null || walkScoreAddress == null) &&
                (tripCurrency == null || usdAnchorAmount == null)
        val lacksTicketmasterTransport = transportAnchor == null || transportDestination == null

        if (
            (isRestaurant && lacksRestaurantInputs) ||
            (isTicketmasterBacked && !isRestaurant && lacksTicketmasterTransport)
        ) return

        viewModelScope.launch {
            _restaurantLiveContextInFlight.update { it + eventId }
            try {
                val overrides = buildMap {
                    if (isRestaurant && venueName != null && venueAddress != null) {
                        PopularTimesRepository.fetchSnapshot(
                            venueName = venueName,
                            venueAddress = venueAddress,
                            yelpId = event.detailValue(DETAIL_YELP_ID)
                        )?.let { snapshot ->
                            put(ATTR_POPULAR_TIMES_JSON, snapshot.popularTimesJson)
                            snapshot.currentBusyness?.let {
                                put(ATTR_CURRENT_BUSYNESS, it.toString())
                            }
                            snapshot.estimatedWaitMin?.let {
                                put(ATTR_ESTIMATED_WAIT_MIN, it.toString())
                            }
                        }
                    }

                    if (transportAnchor != null && transportDestination != null) {
                        TransportRepository.fetchSnapshot(
                            anchor = transportAnchor,
                            destination = transportDestination
                        )?.let { snapshot ->
                            snapshot.walkMin?.let {
                                put(ATTR_WALK_MIN, it.toString())
                            }
                            snapshot.transitMin?.let {
                                put(ATTR_TRANSIT_MIN, it.toString())
                            }
                            snapshot.rideshareMin?.let {
                                put(ATTR_RIDESHARE_MIN, it.toString())
                            }
                            snapshot.rideshareEstimateUsd?.let {
                                put(ATTR_RIDESHARE_ESTIMATE_USD, it)
                            }
                            snapshot.uberDeeplink?.let {
                                put(ATTR_UBER_DEEPLINK, it)
                            }
                            snapshot.lyftDeeplink?.let {
                                put(ATTR_LYFT_DEEPLINK, it)
                            }
                            put(ATTR_TRANSPORT_ANCHOR_LABEL, snapshot.transportAnchorLabel)
                        }
                    }

                    if (isRestaurant && hasOutdoorSeating && latitude != null && longitude != null) {
                        WeatherRepository.fetchSnapshot(
                            latitude = latitude,
                            longitude = longitude,
                            date = event.date,
                            startTime = event.startTime,
                            timeZoneId = event.tz
                        )?.let { snapshot ->
                            put(ATTR_WEATHER_TEMP_C, snapshot.temperatureC.toString())
                            put(ATTR_WEATHER_CONDITION, snapshot.condition)
                            snapshot.precipPct?.let {
                                put(ATTR_WEATHER_PRECIP_PCT, it.toString())
                            }
                            snapshot.windKph?.let {
                                put(ATTR_WEATHER_WIND_KPH, it.toString())
                            }
                            put(ATTR_WEATHER_SUMMARY, snapshot.summary)
                        }
                    }

                    if (isRestaurant && latitude != null && longitude != null && walkScoreAddress != null) {
                        WalkScoreRepository.fetchSnapshot(
                            latitude = latitude,
                            longitude = longitude,
                            address = walkScoreAddress
                        )?.let { snapshot ->
                            snapshot.walkScore?.let {
                                put(ATTR_WALK_SCORE, it.toString())
                            }
                            snapshot.transitScore?.let {
                                put(ATTR_TRANSIT_SCORE, it.toString())
                            }
                            snapshot.bikeScore?.let {
                                put(ATTR_BIKE_SCORE, it.toString())
                            }
                            snapshot.nearCategories
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString(", ")
                                ?.let { categories ->
                                    put(ATTR_NEAR_CATEGORIES, categories)
                                }
                            snapshot.neighborhoodNote?.let {
                                put(ATTR_NEIGHBORHOOD_NOTE, it)
                            }
                        }
                    }

                    if (isRestaurant && tripCurrency != null && usdAnchorAmount != null) {
                        currencyPreviewRepository.buildPreview(
                            localCurrency = tripCurrency,
                            homeCurrency = homeCurrency,
                            usdAnchorAmount = usdAnchorAmount
                        )?.let { snapshot ->
                            put(ATTR_LOCAL_CURRENCY, snapshot.localCurrency)
                            put(ATTR_HOME_CURRENCY, snapshot.homeCurrency)
                            put(
                                ATTR_ESTIMATED_LOCAL_COST,
                                String.format(Locale.US, "%.2f", snapshot.localCost)
                            )
                            put(
                                ATTR_ESTIMATED_HOME_COST,
                                String.format(Locale.US, "%.2f", snapshot.homeCost)
                            )
                            snapshot.fxHistory30d?.let {
                                put(ATTR_FX_HISTORY_30D, it)
                            }
                        }
                    }
                }

                replaceLiveDetailOverrides(eventId, overrides)
            } catch (e: Exception) {
                Log.w("CurrentTripViewModel", "Failed to refresh event live context", e)
            } finally {
                _restaurantLiveContextInFlight.update { it - eventId }
            }
        }
    }

    fun refreshRestaurantLiveContext(eventId: String) {
        refreshEventLiveContext(eventId)
    }

    fun ensureEventOptionsLoaded(eventId: String) {
        val tripKey = currentTripKey ?: return
        val summary = currentTripSummary ?: return
        viewModelScope.launch {
            if (eventId in _optionsLoading.value) return@launch
            if (_eventOptions.value[eventId].orEmpty().isNotEmpty()) return@launch
            try {
                _optionsLoading.update { it + eventId }
                val event = localEventsSnapshot.firstOrNull { it.eventId == eventId }
                    ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
                if (event != null) {
                    val sharedYelpOptions = loadSharedYelpOptions(tripKey, event)
                    if (sharedYelpOptions != null) {
                        val updatedOptions = _eventOptions.value + (eventId to sharedYelpOptions)
                        _eventOptions.value = updatedOptions
                        publishCurrentEvents(localEventsSnapshot, updatedOptions)
                        return@launch
                    }
                }

                val localVersion = tripLocalDataSource.getTripOptionsVersionGroup(tripKey)
                if (localVersion != null && localVersion == summary.optionsVersion) {
                    return@launch
                }

                currentTripSyncCoordinator.hydrateOptionsIfNeeded(
                    tripKey = tripKey,
                    expectedOptionsVersion = summary.optionsVersion
                )
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to load options", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load options.") }
            } finally {
                _optionsLoading.update { it - eventId }
            }
        }
    }

    private suspend fun loadSharedYelpOptions(
        tripKey: TripKey,
        event: TravelEvent
    ): List<EventOption>? {
        val poolType = yelpPoolTypeForEvent(event) ?: return null
        val initialPoolItems = sharedYelpPools[poolType] ?: tripSyncRemoteDataSource
            .fetchYelpOptionPool(tripKey, poolType)
            .also { items ->
                if (items.isNotEmpty()) {
                    sharedYelpPools[poolType] = items
                }
            }

        if (initialPoolItems.isEmpty()) return null

        val poolItems = maybeExpandSharedYelpPool(
            tripKey = tripKey,
            event = event,
            poolType = poolType,
            poolItems = initialPoolItems,
            rejectedIds = _rejectedOptions.value[event.eventId].orEmpty()
        )

        return synthesizeSharedYelpOptions(
            tripId = tripKey.tripId,
            event = event,
            poolItems = poolItems,
            rejectedIds = _rejectedOptions.value[event.eventId].orEmpty()
        )
    }

    private fun synthesizeSharedYelpOptions(
        tripId: String,
        event: TravelEvent,
        poolItems: List<YelpOptionPoolItem>,
        rejectedIds: Set<String>
    ): List<EventOption> {
        val selectedProviderId = event.selectedOptionId
            .takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)
        val orderedPool = YelpRepository.orderedPoolItemsForEvent(
            pool = poolItems,
            tripId = tripId,
            eventId = event.eventId,
            selectedProviderId = selectedProviderId
        )

        val orderedAlternatives = orderedPool.filterNot { item ->
            item.providerId == selectedProviderId
        }
        val visibleAlternativeCount = SHARED_YELP_VISIBLE_OPTIONS +
            rejectedIds.count { rejectedId -> orderedAlternatives.any { item -> item.providerId == rejectedId } } +
            (sharedYelpWindowBoost[event.eventId] ?: 0)

        val synthesizedOptions = orderedAlternatives
            .take(visibleAlternativeCount)
            .map { item ->
                item.toEventOption(
                    eventId = event.eventId,
                    selected = false
                )
            }
            .toMutableList()

        if (!selectedProviderId.isNullOrBlank()) {
            synthesizedOptions.add(
                index = 0,
                element = EventOption(
                    optionId = selectedProviderId,
                    eventId = event.eventId,
                    source = "yelp",
                    selected = true,
                    imageUrl = event.imageUrl,
                    localImagePath = event.localImagePath,
                    photoUrls = event.photoUrls,
                    details = event.details
                )
            )
        }

        return synthesizedOptions
            .distinctBy(EventOption::optionId)
            .map { option ->
                option.copy(selected = option.optionId == selectedProviderId)
            }
    }

    fun loadMoreOptions(eventId: String) {
        val tripKey = currentTripKey ?: return
        val event = localEventsSnapshot.firstOrNull { it.eventId == eventId }
            ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            ?: return
        val poolType = yelpPoolTypeForEvent(event) ?: return
        val poolItems = sharedYelpPools[poolType].orEmpty()
        if (poolItems.isEmpty()) return

        viewModelScope.launch {
            if (eventId in _optionsLoading.value) return@launch
            _optionsLoading.update { it + eventId }
            try {
                sharedYelpWindowBoost[eventId] =
                    (sharedYelpWindowBoost[eventId] ?: 0) + SHARED_YELP_VISIBLE_OPTIONS
                val expandedPool = maybeExpandSharedYelpPool(
                    tripKey = tripKey,
                    event = event,
                    poolType = poolType,
                    poolItems = poolItems,
                    rejectedIds = _rejectedOptions.value[eventId].orEmpty()
                )
                val updatedOptions = synthesizeSharedYelpOptions(
                    tripId = tripKey.tripId,
                    event = event,
                    poolItems = expandedPool,
                    rejectedIds = _rejectedOptions.value[eventId].orEmpty()
                )
                val updatedOptionsByEvent = _eventOptions.value + (eventId to updatedOptions)
                _eventOptions.value = updatedOptionsByEvent
                publishCurrentEvents(localEventsSnapshot, updatedOptionsByEvent)
            } catch (e: Exception) {
                Log.w("CurrentTripViewModel", "Failed to load more shared Yelp options", e)
            } finally {
                _optionsLoading.update { it - eventId }
            }
        }
    }

    private suspend fun maybeExpandSharedYelpPool(
        tripKey: TripKey,
        event: TravelEvent,
        poolType: String,
        poolItems: List<YelpOptionPoolItem>,
        rejectedIds: Set<String>
    ): List<YelpOptionPoolItem> {
        val selectedProviderId = event.selectedOptionId
            .takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)
        val currentAlternativeCount = poolItems.count { item ->
            item.providerId != selectedProviderId
        }
        val requiredAlternativeCount = SHARED_YELP_VISIBLE_OPTIONS + rejectedIds.size
        if (currentAlternativeCount >= requiredAlternativeCount) {
            return poolItems
        }

        val location = currentTripDestination
            .ifBlank { currentTripSummary?.destination.orEmpty() }
            .ifBlank { return poolItems }
        val additionalItems = YelpRepository.fetchAdditionalPoolItems(
            location = location,
            poolType = poolType,
            existingProviderIds = poolItems.map(YelpOptionPoolItem::providerId).toSet(),
            targetCount = maxOf(
                SHARED_YELP_POOL_EXPANSION_SIZE,
                requiredAlternativeCount - currentAlternativeCount
            )
        )
        if (additionalItems.isEmpty()) return poolItems

        val mergedItems = (poolItems + additionalItems).distinctBy(YelpOptionPoolItem::providerId)
        tripSyncRemoteDataSource.upsertYelpOptionPoolItems(
            tripKey = tripKey,
            poolType = poolType,
            items = additionalItems
        )
        sharedYelpPools[poolType] = mergedItems
        return mergedItems
    }

    private fun yelpPoolTypeForEvent(event: TravelEvent): String? {
        val yelpBusinessId = event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() } ?: return null
        if (yelpBusinessId.isBlank()) return null

        return when (event.type.lowercase(Locale.US)) {
            "restaurant", "dining", "food" -> YELP_POOL_TYPE_RESTAURANTS
            "activity" -> YELP_POOL_TYPE_ACTIVITIES
            else -> null
        }
    }

    private fun persistLocalTripSnapshot(
        tripKey: TripKey? = currentTripKey,
        events: List<TravelEvent> = localEventsSnapshot,
        options: Map<String, List<EventOption>> = _eventOptions.value,
        persistOptions: Boolean = false
    ) {
        val resolvedTripKey = tripKey ?: return
        val viewerUid = auth.currentUser?.uid ?: return
        val summary = currentTripSummary ?: return
        val updatedSummary = summary.copy(eventIds = events.map(TravelEvent::eventId))
        currentTripSummary = updatedSummary
        viewModelScope.launch {
            runCatching {
                tripLocalDataSource.upsertTripSummary(
                    viewerUid = viewerUid,
                    itinerary = updatedSummary,
                    isCurrentCandidate = true
                )
                tripLocalDataSource.replaceTripEvents(
                    tripKey = resolvedTripKey,
                    events = events,
                    eventVersionGroup = updatedSummary.eventsVersion.takeIf { it > 0 } ?: System.currentTimeMillis()
                )
                if (persistOptions) {
                    tripLocalDataSource.replaceTripOptions(
                        tripKey = resolvedTripKey,
                        optionsByEvent = options,
                        optionsVersionGroup = updatedSummary.optionsVersion.takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                }
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to update local current-trip cache", error)
            }
        }
    }

    fun fetchShareTargets() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val groupTargets = db.collection("groups")
                    .whereArrayContains("members", uid)
                    .get()
                    .await()
                    .documents
                    .map { doc ->
                        val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        ShareTarget(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unnamed Group",
                            isGroup = true,
                            memberUids = members
                        )
                    }

                val directChatDocs = db.collection("directChats")
                    .whereArrayContains("members", uid)
                    .get()
                    .await()
                    .documents

                val chatEntries = directChatDocs.mapNotNull { doc ->
                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val otherUid = members.firstOrNull { it != uid } ?: return@mapNotNull null
                    doc.id to otherUid
                }

                val userNames = fetchUserNames(chatEntries.map { it.second }.distinct())
                val directTargets = chatEntries.map { (chatId, otherUid) ->
                    val members = directChatDocs
                        .firstOrNull { doc -> doc.id == chatId }
                        ?.get("members")
                        .let { raw -> (raw as? List<*>)?.filterIsInstance<String>().orEmpty() }
                    ShareTarget(
                        id = chatId,
                        name = userNames[otherUid] ?: "Unknown",
                        isGroup = false,
                        memberUids = members
                    )
                }

                _shareTargets.value = (groupTargets + directTargets).sortedBy { it.name.lowercase(Locale.US) }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to fetch share targets", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load chats.") }
            }
        }
    }

    private suspend fun fetchUserNames(uids: List<String>): Map<String, String> {
        if (uids.isEmpty()) return emptyMap()

        return uids.chunked(30).flatMap { chunk ->
            db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .map { doc ->
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    doc.id to "$first $last".trim().ifBlank { "Unknown" }
                }
        }.toMap()
    }

    private fun currentTripWriteKey(): TripKey? {
        val tripId = _uiState.value.currentTripId ?: return null
        val ownerUid = currentTripKey?.ownerUid ?: auth.currentUser?.uid ?: return null
        return TripKey(ownerUid = ownerUid, tripId = tripId)
    }

    private fun currentTripWriteKeyIfOwner(): TripKey? {
        val tripKey = currentTripWriteKey() ?: return null
        return tripKey.takeIf { key -> key.ownerUid == auth.currentUser?.uid }
    }

    private fun currentTripWriteKeyIfContributor(): TripKey? {
        return currentTripWriteKey()?.takeIf { _uiState.value.canEditTrip }
    }

    private fun requireTripContributorKey(action: String): TripKey? {
        currentTripWriteKeyIfContributor()?.let { return it }
        if (currentTripWriteKey() != null) {
            postError("You do not have permission to $action.")
        }
        return null
    }

    private fun requireOwnerTripKey(action: String): TripKey? {
        currentTripWriteKeyIfOwner()?.let { return it }
        if (currentTripWriteKey() != null) {
            postError("Only the trip owner can $action.")
        }
        return null
    }

    private fun resolveSenderDisplayName(): String {
        val currentUser = auth.currentUser ?: return "Traveler"
        val authDisplayName = currentUser.displayName?.trim().orEmpty()
        if (authDisplayName.isNotBlank()) return authDisplayName

        val emailDisplayName = currentUser.email
            ?.substringBefore('@')
            ?.replace('.', ' ')
            ?.replace('_', ' ')
            ?.trim()
            .orEmpty()
        if (emailDisplayName.isNotBlank()) return emailDisplayName

        return "Traveler"
    }

    fun shareTripToChat(target: ShareTarget) {
        val uid = auth.currentUser?.uid ?: return
        val tripKey = requireOwnerTripKey("share this trip") ?: return

        viewModelScope.launch {
            try {
                tripRepository.ensureTripAccess(
                    key = tripKey,
                    memberUids = target.memberUids
                )

                val senderName = resolveSenderDisplayName()
                val container = if (target.isGroup) "groups" else "directChats"
                val chatRef = db.collection(container).document(target.id)
                val messageRef = chatRef.collection("messages").document()
                val coverImage = _uiState.value.events.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl
                val message = hashMapOf(
                    "text" to "Shared trip: ${_uiState.value.tripTitle}",
                    "senderId" to uid,
                    "senderName" to senderName,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "messageType" to "trip_card",
                    "sharedTripId" to tripKey.tripId,
                    "ownerUid" to tripKey.ownerUid,
                    "tripName" to _uiState.value.tripTitle,
                    "tripDestination" to currentTripDestination,
                    "tripDateFrom" to _uiState.value.dateFrom,
                    "tripDateTo" to _uiState.value.dateTo,
                    "coverImageUrl" to coverImage
                )

                db.runBatch { batch ->
                    batch.set(messageRef, message)
                    batch.update(
                        chatRef,
                        mapOf(
                            "lastMessage" to "Shared a trip",
                            "lastMessageTime" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()
                refreshCurrentTripInBackground(tripKey)
                refreshHomeTripCacheInBackground()
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to share trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to share trip.") }
            }
        }
    }

    fun selectOption(eventId: String, optionId: String) {
        val tripKey = requireTripContributorKey("change selected options") ?: return
        val options = _eventOptions.value[eventId].orEmpty()
        val selectedOption = options.firstOrNull { it.optionId == optionId } ?: return
        val event = localEventsSnapshot.firstOrNull { it.eventId == eventId }
            ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            ?: return
        val isYelpSelectionEvent = selectedOption.source.equals("yelp", ignoreCase = true) &&
            yelpPoolTypeForEvent(event) != null

        viewModelScope.launch {
            try {
                val actionResult = tripPlanActionService.replaceSelectedOption(
                    tripKey = tripKey,
                    event = event,
                    existingOptions = options,
                    optionId = optionId,
                    persistOptions = !isYelpSelectionEvent
                )
                val updatedEvent = actionResult.event ?: return@launch
                val updatedOptions = actionResult.options
                val conflictResolution = resolveTicketmasterConflicts(
                    originalEvent = event,
                    updatedEvent = updatedEvent,
                    updatedOptions = updatedOptions,
                    allEvents = localEventsSnapshot
                )
                localEventsSnapshot = sortPlanEvents(conflictResolution.events)

                val updatedOptionsByEvent = _eventOptions.value + (eventId to updatedOptions)
                _eventOptions.value = updatedOptionsByEvent
                _rejectedOptions.update { current ->
                    val nextRejected = current[eventId].orEmpty() - optionId
                    current + (eventId to nextRejected)
                }
                publishCurrentEvents(localEventsSnapshot, updatedOptionsByEvent)
                persistLocalTripSnapshot(
                    events = localEventsSnapshot,
                    options = updatedOptionsByEvent,
                    persistOptions = !isYelpSelectionEvent
                )
                prefetchSharedEventMedia(listOf(updatedEvent))
                if (conflictResolution.removedEventIds.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            infoMessage = if (conflictResolution.removedEventIds.size == 1) {
                                "Updated showtime and removed 1 conflicting flexible event."
                            } else {
                                "Updated showtime and removed ${conflictResolution.removedEventIds.size} conflicting flexible events."
                            },
                            errorMessage = null
                        )
                    }
                    conflictResolution.removedEventIds.forEach { removedEventId ->
                        runCatching {
                            tripSyncRemoteDataSource.deleteEvent(tripKey = tripKey, eventId = removedEventId)
                        }.onFailure { error ->
                            Log.w("CurrentTripViewModel", "Failed to remove conflicting event", error)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to select option", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update selection.") }
                return@launch
            }

            refreshCurrentTripInBackground(tripKey)
            ensureYelpEventEnriched(eventId)
        }
    }

    fun rejectOption(eventId: String, optionId: String) {
        if (currentTripWriteKeyIfContributor() == null) {
            postError("You do not have permission to change options.")
            return
        }
        _rejectedOptions.update { current ->
            val updatedRejected = current[eventId].orEmpty() + optionId
            val event = localEventsSnapshot.firstOrNull { it.eventId == eventId }
                ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            if (event != null) {
                val poolType = yelpPoolTypeForEvent(event)
                val poolItems = poolType?.let { sharedYelpPools[it] }.orEmpty()
                if (poolItems.isNotEmpty()) {
                    val updatedOptions = synthesizeSharedYelpOptions(
                        tripId = currentTripKey?.tripId ?: event.itineraryId,
                        event = event,
                        poolItems = poolItems,
                        rejectedIds = updatedRejected
                    )
                    _eventOptions.value = _eventOptions.value + (eventId to updatedOptions)
                }
            }
            current + (eventId to updatedRejected)
        }

        val tripKey = currentTripKey ?: return
        val event = localEventsSnapshot.firstOrNull { it.eventId == eventId }
            ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            ?: return
        val poolType = yelpPoolTypeForEvent(event) ?: return
        val poolItems = sharedYelpPools[poolType].orEmpty()
        if (poolItems.isEmpty()) return

        viewModelScope.launch {
            if (eventId in _optionsLoading.value) return@launch
            _optionsLoading.update { it + eventId }
            try {
                val expandedPool = maybeExpandSharedYelpPool(
                    tripKey = tripKey,
                    event = event,
                    poolType = poolType,
                    poolItems = poolItems,
                    rejectedIds = _rejectedOptions.value[eventId].orEmpty()
                )
                val updatedOptions = synthesizeSharedYelpOptions(
                    tripId = tripKey.tripId,
                    event = event,
                    poolItems = expandedPool,
                    rejectedIds = _rejectedOptions.value[eventId].orEmpty()
                )
                val updatedOptionsByEvent = _eventOptions.value + (eventId to updatedOptions)
                _eventOptions.value = updatedOptionsByEvent
                publishCurrentEvents(localEventsSnapshot, updatedOptionsByEvent)
            } catch (e: Exception) {
                Log.w("CurrentTripViewModel", "Failed to expand shared Yelp options", e)
            } finally {
                _optionsLoading.update { it - eventId }
            }
        }
    }

    fun patchEventFields(
        eventId: String,
        title: String,
        startTime: String,
        notes: String
    ) {
        val tripKey = requireTripContributorKey("edit plans") ?: return
        val currentEvent = localEventsSnapshot.firstOrNull { it.eventId == eventId }
            ?: _uiState.value.events.firstOrNull { it.eventId == eventId }
            ?: return

        val updatedDetails = currentEvent.details.toMutableMap().apply {
            val trimmedTitle = title.trim()
            val trimmedNotes = notes.trim()
            if (trimmedTitle.isBlank()) {
                remove("title")
            } else {
                put("title", trimmedTitle)
            }
            if (trimmedNotes.isBlank()) {
                remove("description")
                remove("notes")
            } else {
                put("description", trimmedNotes)
            }
        }

        val updatedEvent = currentEvent.copy(
            startTime = normalizeTime(startTime),
            details = updatedDetails
        )
        localEventsSnapshot = sortPlanEvents(
            localEventsSnapshot.map { if (it.eventId == eventId) updatedEvent else it }
        )
        publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
        persistLocalTripSnapshot(events = localEventsSnapshot)

        viewModelScope.launch {
            try {
                tripPlanActionService.updateEvent(tripKey = tripKey, event = updatedEvent)
                refreshCurrentTripInBackground(tripKey)
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to patch event", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save event changes.") }
            }
        }
    }

    fun moveEventLocally(
        eventId: String,
        fromDate: String,
        toDate: String,
        toIndex: Int
    ) {
        val currentEvents = localEventsSnapshot
        val movingEvent = currentEvents.firstOrNull { it.eventId == eventId } ?: return
        val normalizedFromDate = normalizeDate(fromDate)
        val normalizedToDate = normalizeDate(toDate)

        val grouped = currentEvents
            .groupBy { normalizeDate(it.date) }
            .mapValues { (_, events) ->
                events.sortedWith(
                    compareBy(
                        { it.details["sortOrder"]?.toIntOrNull() ?: 0 },
                        { normalizeTime(it.startTime) }
                    )
                ).toMutableList()
            }
            .toMutableMap()

        val sourceList = grouped[normalizedFromDate] ?: mutableListOf()
        sourceList.removeAll { it.eventId == eventId }

        val targetList = if (normalizedToDate == normalizedFromDate) {
            sourceList
        } else {
            grouped.getOrPut(normalizedToDate) { mutableListOf() }
        }

        val insertionIndex = toIndex.coerceIn(0, targetList.size)
        targetList.add(insertionIndex, movingEvent.copy(date = normalizedToDate))

        localEventsSnapshot = grouped
            .toSortedMap(compareBy<String> { if (it.isBlank()) "9999-12-31" else it })
            .values
            .flatMap { dayEvents ->
                dayEvents.mapIndexed { index, event ->
                    event.copy(
                        details = event.details.toMutableMap().apply {
                            put("sortOrder", index.toString())
                        }
                    )
                }
            }

        publishCurrentEvents(localEventsSnapshot, _eventOptions.value)
    }

    fun persistEventPlacements(affectedDates: Set<String>) {
        val tripKey = requireTripContributorKey("reorder plans") ?: return
        val normalizedDates = affectedDates.map(::normalizeDate).toSet()
        val affectedEvents = _uiState.value.events.filter { normalizeDate(it.date) in normalizedDates }

        if (affectedEvents.isEmpty()) return

        viewModelScope.launch {
            try {
                persistLocalTripSnapshot(events = localEventsSnapshot)
                tripSyncRemoteDataSource.persistEventPlacements(
                    tripKey = tripKey,
                    events = affectedEvents.map { event ->
                        event.copy(date = normalizeDate(event.date))
                    }
                )
                refreshCurrentTripInBackground(tripKey)
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to persist placements", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save event order.") }
            }
        }
    }

    fun archiveTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentTripId = _uiState.value.currentTripId
        val tripKey = requireOwnerTripKey("archive this trip") ?: return
        if (tripKey.tripId != tripId) {
            postError("Load the trip again before archiving it.")
            return
        }

        viewModelScope.launch {
            try {
                tripSyncRemoteDataSource.updateTripSummaryFields(
                    tripKey = tripKey,
                    fields = mapOf(
                        "status" to "archived",
                        "archivedAt" to FieldValue.serverTimestamp()
                    )
                )

                _allTrips.update { trips ->
                    trips.map {
                        if (it.itineraryId == tripId) it.copy(status = "archived") else it
                    }
                }
                currentTripSummary = currentTripSummary?.copy(status = "archived")
                currentTripSummary?.let { summary ->
                    tripLocalDataSource.upsertTripSummary(
                        viewerUid = uid,
                        itinerary = summary,
                        isCurrentCandidate = true
                    )
                }
                _uiState.update { it.copy(infoMessage = "Trip archived.", errorMessage = null) }
                refreshHomeTripCacheInBackground()

                if (currentTripId == tripId) {
                    loadTrip()
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to archive trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to archive trip.") }
            }
        }
    }

    fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripKey = requireOwnerTripKey("delete this trip") ?: return
        if (tripKey.tripId != tripId) {
            postError("Load the trip again before deleting it.")
            return
        }

        viewModelScope.launch {
            try {
                val remaining = _allTrips.value.filterNot { trip ->
                    trip.itineraryId == tripKey.tripId && trip.ownerUid == tripKey.ownerUid
                }
                _allTrips.value = remaining

                tripRepository.deleteTrip(tripKey)
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        TripMediaCacheStore.deleteTripMedia(getApplication(), tripKey)
                    }.onFailure { error ->
                        Log.w("CurrentTripViewModel", "Failed to clear cached trip media", error)
                    }
                }
                refreshHomeTripCacheInBackground()

                val nextTrip = remaining.firstOrNull {
                    !it.status.equals("archived", ignoreCase = true)
                }
                if (nextTrip != null) {
                    loadTrip(TripKey(ownerUid = nextTrip.ownerUid, tripId = nextTrip.itineraryId))
                } else {
                    resetTripState(infoMessage = NO_TRIP_MESSAGE)
                }
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to delete trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete trip.") }
            }
        }
    }

    fun loadAllTrips() {
        val uid = auth.currentUser?.uid ?: return

        if (allTripsObserverUid != uid || allTripsJob == null) {
            allTripsJob?.cancel()
            allTripsObserverUid = uid
            allTripsJob = viewModelScope.launch {
                tripLocalDataSource.observeHomeTripSummaries(uid).collect { trips ->
                    _allTrips.value = trips
                }
            }
        }

        refreshHomeTripCacheInBackground()
    }

    fun renameTrip(newName: String) {
        val tripKey = requireOwnerTripKey("rename this trip") ?: return
        val trimmed = newName.trim().ifBlank { return }
        val viewerUid = auth.currentUser?.uid ?: return

        _tripTitle.value = trimmed
        _uiState.update { it.copy(tripTitle = trimmed) }
        _allTrips.update { trips ->
            trips.map { if (it.itineraryId == tripKey.tripId) it.copy(tripName = trimmed) else it }
        }

        viewModelScope.launch {
            try {
                tripSyncRemoteDataSource.updateTripSummaryFields(
                    tripKey = tripKey,
                    fields = mapOf("tripName" to trimmed)
                )
                currentTripSummary = currentTripSummary?.copy(tripName = trimmed)
                currentTripSummary?.let { summary ->
                    tripLocalDataSource.upsertTripSummary(
                        viewerUid = viewerUid,
                        itinerary = summary,
                        isCurrentCandidate = true
                    )
                }
                refreshHomeTripCacheInBackground()
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "Failed to rename trip", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to rename trip.") }
            }
        }
    }

    fun loadPreview(source: PreviewSource) {
        val viewerUid = auth.currentUser?.uid
        when (source) {
            is PreviewSource.CuratedStarter -> {
                val preview = AiCuratedTripToItineraryMapper.map(
                    starter = source.starter,
                    intakeProfile = source.intakeProfile,
                    viewerUid = viewerUid
                )
                applyPreview(viewerUid = viewerUid, preview = preview)
            }

            is PreviewSource.DestinationLock -> {
                val preview = AiDestinationLockMapper.map(
                    destination = source.destination,
                    intakeProfile = source.intakeProfile,
                    viewerUid = viewerUid
                )
                applyPreview(viewerUid = viewerUid, preview = preview)
            }
        }
    }

    private fun applyPreview(
        viewerUid: String?,
        preview: com.example.travelcents.data.ai.chat.PreviewTrip
    ) {
        resetTripState()
        currentTripKey = null
        currentTripSummary = preview.itinerary
        currentTripDestination = preview.itinerary.destination
        localEventsSnapshot = sortPlanEvents(preview.events)
        _events.value = localEventsSnapshot
        _tripTitle.value = preview.itinerary.tripName
        _uiState.value = CurrentTripUiState(
            isLoading = false,
            currentTripId = preview.tripKey.tripId,
            currentTripOwnerUid = preview.tripKey.ownerUid,
            viewerUid = viewerUid,
            accessRole = TripAccessRole.VIEWER,
            canEditTrip = false,
            canManageTrip = false,
            tripTitle = preview.itinerary.tripName,
            destination = preview.itinerary.destination,
            dateFrom = preview.itinerary.dateFrom,
            dateTo = preview.itinerary.dateTo,
            events = localEventsSnapshot,
            isPreview = true
        )
    }

    fun clearPreview() {
        if (_uiState.value.isPreview) {
            resetTripState()
        }
    }

    fun addPreviewEvent(event: TravelEvent) {
        val summary = currentTripSummary ?: return
        if (!_uiState.value.isPreview) return

        val normalizedEvent = event.copy(
            itineraryId = summary.itineraryId,
            eventId = event.eventId.ifBlank { UUID.randomUUID().toString() }
        )
        val incomingYelpId = normalizedEvent.detailValue(DETAIL_YELP_ID).orEmpty()
        val duplicate = localEventsSnapshot.any { existing ->
            existing.eventId == normalizedEvent.eventId ||
                (
                    incomingYelpId.isNotBlank() &&
                        existing.type.equals(normalizedEvent.type, ignoreCase = true) &&
                        existing.date == normalizedEvent.date &&
                        existing.detailValue(DETAIL_YELP_ID) == incomingYelpId
                    )
        }
        if (duplicate) return

        localEventsSnapshot = sortPlanEvents(localEventsSnapshot + normalizedEvent)
        currentTripSummary = summary.copy(eventIds = localEventsSnapshot.map(TravelEvent::eventId))
        _events.value = localEventsSnapshot
        _uiState.value = _uiState.value.copy(
            events = localEventsSnapshot,
            infoMessage = if (localEventsSnapshot.isEmpty()) EMPTY_PLANS_MESSAGE else null
        )
    }

    fun loadTrip(tripId: String? = null) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Log.e("CurrentTripViewModel", "UID is NULL. Firebase isn't ready yet.")
            resetTripState(
                infoMessage = "Log in to load your current trip."
            )
            return
        }

        if (tripId != null) {
            loadTrip(TripKey(ownerUid = uid, tripId = tripId))
            return
        }

        resetTripState(isLoading = true)
        Log.d("CurrentTripViewModel", "UID found: $uid. Fetching trip: Latest")
        TripPerformanceLogger.beginTripLoad(
            trigger = "load_latest_trip",
            requestedTripId = null
        )
        viewModelScope.launch {
            try {
                val latestTripKey = currentTripSyncCoordinator.resolveLatestTripKey(uid)
                if (latestTripKey == null) {
                    Log.d("CurrentTripViewModel", "No active trips found.")
                    resetTripState(infoMessage = NO_TRIP_MESSAGE)
                    return@launch
                }

                observeCurrentTrip(uid, latestTripKey)
                refreshCurrentTrip(
                    viewerUid = uid,
                    tripKey = latestTripKey,
                    missingTripMessage = NO_TRIP_MESSAGE
                )
            } catch (e: Exception) {
                Log.e("CurrentTripViewModel", "DATABASE ERROR: ${e.message}", e)
                resetTripState(errorMessage = e.message ?: "Failed to load trip.")
            }
        }
    }

    fun loadTrip(tripKey: TripKey) {
        val viewerUid = auth.currentUser?.uid
        if (viewerUid == null) {
            Log.e("CurrentTripViewModel", "UID is NULL. Firebase isn't ready yet.")
            resetTripState(
                infoMessage = "Log in to load your current trip."
            )
            return
        }

        resetTripState(isLoading = true)
        Log.d(
            "CurrentTripViewModel",
            "Loading trip by key: ownerUid=${tripKey.ownerUid}, tripId=${tripKey.tripId}"
        )
        TripPerformanceLogger.beginTripLoad(
            trigger = "load_trip_key",
            requestedTripId = tripKey.tripId
        )
        observeCurrentTrip(viewerUid, tripKey)
        refreshCurrentTrip(viewerUid = viewerUid, tripKey = tripKey)
    }

    private fun prefetchSharedEventMedia(events: List<TravelEvent>) {
        viewModelScope.launch {
            runCatching {
                mediaDetailPipeline.prefetchSharedMedia(events)
            }.onFailure { error ->
                Log.w("CurrentTripViewModel", "Failed to prefetch shared event media", error)
            }
        }
    }

    private fun sortPlanEvents(events: List<TravelEvent>): List<TravelEvent> {
        return events.sortedWith(
            compareBy<TravelEvent>(
                { normalizeDate(it.date) },
                { it.details["sortOrder"]?.toIntOrNull() ?: Int.MAX_VALUE },
                { normalizeTime(it.startTime) },
                { it.eventId }
            )
        )
    }

    private fun isRestaurantEvent(event: TravelEvent): Boolean {
        return when (event.type.lowercase(Locale.US)) {
            "restaurant", "dining", "food" -> true
            else -> false
        }
    }

    private fun isTicketmasterBackedEvent(event: TravelEvent): Boolean {
        return !event.detailValue(ATTR_TICKETMASTER_EVENT_ID).isNullOrBlank()
    }

    private data class TicketmasterConflictResolution(
        val events: List<TravelEvent>,
        val removedEventIds: List<String>
    )

    private fun resolveTicketmasterConflicts(
        originalEvent: TravelEvent,
        updatedEvent: TravelEvent,
        updatedOptions: List<EventOption>,
        allEvents: List<TravelEvent>
    ): TicketmasterConflictResolution {
        val replacedEvents = allEvents.map { existing ->
            if (existing.eventId == updatedEvent.eventId) updatedEvent else existing
        }
        if (!isTicketmasterBackedEvent(updatedEvent)) {
            return TicketmasterConflictResolution(
                events = replacedEvents,
                removedEventIds = emptyList()
            )
        }

        val selectedOption = updatedOptions.firstOrNull { it.selected }
        val scheduleChanged = selectedOption?.detailValue(
            ATTR_OPTION_DATE,
            ATTR_OPTION_START_TIME,
            ATTR_OPTION_END_TIME
        ) != null && (
            normalizeDate(originalEvent.date) != normalizeDate(updatedEvent.date) ||
                normalizeTime(originalEvent.startTime) != normalizeTime(updatedEvent.startTime) ||
                normalizeTime(originalEvent.endTime) != normalizeTime(updatedEvent.endTime)
            )

        if (!scheduleChanged) {
            return TicketmasterConflictResolution(
                events = replacedEvents,
                removedEventIds = emptyList()
            )
        }

        val conflictingIds = replacedEvents
            .filter { candidate ->
                candidate.eventId != updatedEvent.eventId &&
                    isFlexibleEvent(candidate) &&
                    eventsOverlap(candidate, updatedEvent)
            }
            .map(TravelEvent::eventId)

        return TicketmasterConflictResolution(
            events = replacedEvents.filterNot { it.eventId in conflictingIds },
            removedEventIds = conflictingIds
        )
    }

    private fun isFlexibleEvent(event: TravelEvent): Boolean {
        return when (event.type.lowercase(Locale.US)) {
            "flight", "hotel" -> false
            else -> true
        }
    }

    private fun eventsOverlap(
        first: TravelEvent,
        second: TravelEvent
    ): Boolean {
        if (normalizeDate(first.date) != normalizeDate(second.date)) return false
        if (first.startTime.isBlank() || first.endTime.isBlank()) return false
        if (second.startTime.isBlank() || second.endTime.isBlank()) return false

        return normalizeTime(first.startTime) < normalizeTime(second.endTime) &&
            normalizeTime(second.startTime) < normalizeTime(first.endTime)
    }

    private fun resolveTransportAnchor(event: TravelEvent): TransportRepository.TransportAnchor? {
        val targetDate = normalizeDate(event.date)
        val tripEvents = sortPlanEvents(_uiState.value.events)
        val dayEvents = tripEvents.filter { normalizeDate(it.date) == targetDate }
        val targetIndex = dayEvents.indexOfFirst { it.eventId == event.eventId }

        if (targetIndex > 0) {
            dayEvents.subList(0, targetIndex)
                .asReversed()
                .firstNotNullOfOrNull(::transportAnchorForEvent)
                ?.let { return it }
        }

        return tripEvents
            .firstOrNull { it.type.equals("hotel", ignoreCase = true) }
            ?.let(::transportAnchorForEvent)
    }

    private fun transportAnchorForEvent(event: TravelEvent): TransportRepository.TransportAnchor? {
        val latitude = event.detailValue(ATTR_LATITUDE)?.toDoubleOrNull() ?: return null
        val longitude = event.detailValue(ATTR_LONGITUDE)?.toDoubleOrNull() ?: return null
        val label = listOfNotNull(
            event.displayName()?.takeIf { it.isNotBlank() },
            event.details["title"]?.takeIf { it.isNotBlank() },
            event.detailValue(ATTR_BUSINESS_ADDRESS, "address")?.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: return null

        return TransportRepository.TransportAnchor(
            label = label,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun resolveTransportDestination(
        event: TravelEvent
    ): TransportRepository.TransportDestination? {
        val latitude = event.detailValue(ATTR_LATITUDE)?.toDoubleOrNull()
        val longitude = event.detailValue(ATTR_LONGITUDE)?.toDoubleOrNull()
        val address = event.detailValue(ATTR_BUSINESS_ADDRESS, "address")
            ?.takeIf { it.isNotBlank() }
        if ((latitude == null || longitude == null) && address == null) return null

        return TransportRepository.TransportDestination(
            label = event.displayName().orEmpty(),
            latitude = latitude,
            longitude = longitude,
            address = address
        )
    }

    private fun restaurantUsdAnchorAmount(event: TravelEvent): Double? {
        event.detailValue(ATTR_PRICE_LEVEL_USD)
            ?.toDoubleOrNull()
            ?.takeIf { it > 0.0 }
            ?.let { return it }

        return when (event.detailValue(ATTR_PRICE_TIER, "price_tier")?.trim()) {
            "$" -> 15.0
            "$$" -> 35.0
            "$$$" -> 70.0
            "$$$$" -> 120.0
            else -> null
        }
    }

    private fun resolvedHomeCurrencyCode(): String {
        return runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("USD")
    }

    override fun onCleared() {
        currentTripSummaryJob?.cancel()
        currentTripEventsJob?.cancel()
        currentTripMembersJob?.cancel()
        currentTripOptionsJob?.cancel()
        allTripsJob?.cancel()
        super.onCleared()
    }
}

