package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.trip.TripPerformanceLogger
import com.example.travelcents.data.trip.advisory.TripAdvisory
import com.example.travelcents.data.trip.model.ATTR_ADVISORY_REASON
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.ui.main.current.calendar.buildTripDateRange
import com.example.travelcents.ui.main.current.header.CurrentTripHeader
import com.example.travelcents.ui.modules.buildCalendarDates
import com.example.travelcents.ui.modules.sortEventsForCalendar
import com.example.travelcents.ui.modules.parseIsoDate
import com.example.travelcents.ui.modules.todayIsoDate
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun CurrentTripScreen(
    modifier: Modifier = Modifier,
    viewModel: CurrentTripViewModel = viewModel(),
    displayMode: CurrentDisplayMode = CurrentDisplayMode.ITINERARY,
    autoLoadTrip: Boolean = true,
    tripId: String? = null,
    onNavigateToMode: (CurrentDisplayMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()
    val eventOptions by viewModel.eventOptions.collectAsState()
    val rejectedOptions by viewModel.rejectedOptions.collectAsState()
    val optionsLoading by viewModel.optionsLoading.collectAsState()
    val advisories by viewModel.advisories.collectAsState()
    val advisoryEvaluationInProgress by viewModel.advisoryEvaluationInProgress.collectAsState()
    val advisoryDemoModeEnabled by viewModel.isAdvisoryDemoModeEnabled.collectAsState()
    val yelpReviews by viewModel.yelpReviews.collectAsState()
    val reviewsLoading by viewModel.reviewsLoading.collectAsState()
    val shareTargets by viewModel.shareTargets.collectAsState()
    val tripMembers by viewModel.tripMembers.collectAsState()
    val events = remember(uiState.events) { sortEventsForCalendar(uiState.events) }
    val eventDates = remember(events) {
        events.map { it.date }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val calendarDates = remember(uiState.dateFrom, uiState.dateTo, eventDates) {
        buildCalendarDates(
            dateFrom = uiState.dateFrom,
            dateTo = uiState.dateTo,
            eventDates = eventDates
        )
    }

    var selectedDate by rememberSaveable { mutableStateOf("") }
    var itineraryVisibleDate by rememberSaveable { mutableStateOf("") }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var editorPlan by remember { mutableStateOf<EditablePlan?>(null) }
    var deleteCandidate by remember { mutableStateOf<EditablePlan?>(null) }
    var jiggleMode by remember { mutableStateOf(false) }
    var optionsPanelEventId by remember { mutableStateOf<String?>(null) }
    var advisorySheetId by remember { mutableStateOf<String?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
    var shareConfirmation by remember { mutableStateOf<String?>(null) }
    var seededTripKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingReorderDates by remember { mutableStateOf<Set<String>>(emptySet()) }
    val activeAdvisory = remember(advisorySheetId, advisories, eventOptions) {
        advisories.firstOrNull { it.advisoryId == advisorySheetId }
            ?.withLatestAdvisoryOptions(eventOptions)
    }

    val tripDateRange = remember(uiState.dateFrom, uiState.dateTo, calendarDates) {
        buildTripDateRange(
            dateFrom = uiState.dateFrom,
            dateTo = uiState.dateTo,
            eventDates = calendarDates
        )
    }

    fun finishReorderSession() {
        if (pendingReorderDates.isNotEmpty()) {
            viewModel.persistEventPlacements(pendingReorderDates)
            pendingReorderDates = emptySet()
        }
        jiggleMode = false
    }

    LaunchedEffect(autoLoadTrip, tripId) {
        if (autoLoadTrip) {
            viewModel.loadTrip(tripId)
        }
    }

    LaunchedEffect(uiState.currentTripId) {
        viewModel.loadAllTrips()
    }

    LaunchedEffect(uiState.currentTripId, uiState.dateFrom, uiState.dateTo, calendarDates) {
        val tripKey = uiState.currentTripId ?: "preview:${uiState.tripTitle}|${uiState.dateFrom}|${uiState.dateTo}"
        if (seededTripKey != tripKey || selectedDate.isBlank()) {
            selectedDate = initialCalendarSelection(
                dateFrom = uiState.dateFrom,
                dateTo = uiState.dateTo,
                calendarDates = calendarDates
            )
            seededTripKey = tripKey
        }
    }

    LaunchedEffect(uiState.canEditTrip) {
        if (!uiState.canEditTrip) {
            jiggleMode = false
            pendingReorderDates = emptySet()
        }
    }

    LaunchedEffect(editorPlan) {
        editorPlan?.eventId?.let(viewModel::ensureEventOptionsLoaded)
        editorPlan?.existingDetails?.get("yelp_id")
            ?.takeIf { it.isNotBlank() }
            ?.let(viewModel::fetchYelpReviews)
    }

    LaunchedEffect(selectedEventId, uiState.events) {
        val event = uiState.events.firstOrNull { it.eventId == selectedEventId }
        val yelpId = event?.details?.get("yelp_id")?.takeIf { it.isNotBlank() }
        if (yelpId != null) {
            viewModel.fetchYelpReviews(yelpId)
            viewModel.ensureYelpEventEnriched(event.eventId)
        }
        event?.eventId?.let(viewModel::refreshEventLiveContext)
        event?.eventId?.let(viewModel::ensureEventOptionsLoaded)
    }

    LaunchedEffect(optionsPanelEventId) {
        optionsPanelEventId?.let(viewModel::ensureEventOptionsLoaded)
    }

    LaunchedEffect(
        displayMode,
        advisoryDemoModeEnabled,
        uiState.isLoading,
        uiState.currentTripId,
        uiState.events.size
    ) {
        if (
            displayMode == CurrentDisplayMode.ITINERARY &&
            advisoryDemoModeEnabled &&
            !uiState.isLoading &&
            uiState.currentTripId != null
        ) {
            viewModel.onItineraryVisible()
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.currentTripId, uiState.events.size) {
        if (!uiState.isLoading && uiState.currentTripId != null) {
            TripPerformanceLogger.recordFirstRender(
                source = "CurrentTripScreen",
                tripId = uiState.currentTripId,
                eventCount = uiState.events.size
            )
        }
    }

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CurrentTripHeader(
                    tripTitle = uiState.tripTitle,
                    tripStatus = uiState.tripStatus,
                    heroDate = itineraryVisibleDate.ifBlank { uiState.dateFrom },
                    currentTripId = uiState.currentTripId,
                    currentTripOwnerUid = uiState.currentTripOwnerUid,
                    viewerUid = uiState.viewerUid,
                    allTrips = allTrips,
                    canAdd = uiState.currentTripId != null && uiState.canEditTrip,
                    canEditTrip = uiState.canEditTrip,
                    canManageTrip = uiState.canManageTrip,
                    isReorderActive = displayMode == CurrentDisplayMode.ITINERARY && jiggleMode,
                    isInCalendarMode = displayMode != CurrentDisplayMode.ITINERARY,
                    isWeekMode = displayMode == CurrentDisplayMode.WEEK,
                    selectedDate = selectedDate,
                    sortedDates = calendarDates,
                    onDateSelected = { selectedDate = it },
                    members = tripMembers,
                    onCalendarClick = { onNavigateToMode(CurrentDisplayMode.DAY) },
                    onBackClick = { onNavigateToMode(CurrentDisplayMode.ITINERARY) },
                    onDoneReordering = ::finishReorderSession,
                    onAddClick = {
                        if (!uiState.canEditTrip) {
                            viewModel.postError("Shared trips are read-only for now.")
                        } else if (uiState.currentTripId == null) {
                            viewModel.postError("Create a trip first before adding calendar plans.")
                        } else {
                            editorPlan = newEditablePlan(
                                date = selectedDate.ifBlank {
                                    calendarDates.firstOrNull() ?: uiState.dateFrom.ifBlank { todayIsoDate() }
                                },
                                startMinutes = 9 * 60
                            )
                        }
                    },
                    onShareClick = {
                        if (!uiState.canManageTrip) {
                            viewModel.postError("Only the trip owner can share this trip.")
                        } else {
                            viewModel.fetchShareTargets()
                            showShareSheet = true
                        }
                    },
                    onToggleReorder = {
                        if (!uiState.canEditTrip) {
                            viewModel.postError("Shared trips are read-only for now.")
                        } else {
                            if (displayMode != CurrentDisplayMode.ITINERARY) {
                                onNavigateToMode(CurrentDisplayMode.ITINERARY)
                            }
                            if (jiggleMode) {
                                finishReorderSession()
                            } else {
                                pendingReorderDates = emptySet()
                                jiggleMode = true
                            }
                        }
                    },
                    onArchiveTrip = { tripId ->
                        pendingReorderDates = emptySet()
                        jiggleMode = false
                        viewModel.archiveTrip(tripId)
                    },
                    onDeleteTrip = { tripId ->
                        pendingReorderDates = emptySet()
                        jiggleMode = false
                        viewModel.deleteTrip(tripId)
                    },
                    onSwitchTrip = { tripKey ->
                        pendingReorderDates = emptySet()
                        jiggleMode = false
                        selectedEventId = null
                        optionsPanelEventId = null
                        advisorySheetId = null
                        editorPlan = null
                        deleteCandidate = null
                        viewModel.loadTrip(tripKey)
                    },
                    onRenameTrip = viewModel::renameTrip,
                    controlsContent = {
                        CurrentTripModeSwitcher(
                            selectedMode = displayMode,
                            onModeSelected = {
                                jiggleMode = false
                                onNavigateToMode(it)
                            }
                        )
                    }
                )

                if (uiState.infoMessage != null || uiState.errorMessage != null) {
                    CurrentTripMessageCard(
                        message = uiState.errorMessage ?: uiState.infoMessage.orEmpty(),
                        isError = uiState.errorMessage != null,
                        onDismiss = viewModel::clearMessages
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        uiState.isLoading -> CurrentTripLoadingState()
                        uiState.currentTripId == null -> CurrentTripEmptyState(
                            title = "No Trip Yet",
                            body = uiState.infoMessage ?: "Create a trip from the New Trip tab to populate this calendar."
                        )
                        displayMode == CurrentDisplayMode.ITINERARY -> CurrentTripItineraryContent(
                            events = events,
                            advisories = advisories,
                            eventOptions = eventOptions,
                            rejectedOptions = rejectedOptions,
                            canEditTrip = uiState.canEditTrip,
                            jiggleMode = jiggleMode,
                            onEventClick = { selectedEventId = it.eventId },
                            onDeleteClick = {
                                if (uiState.canEditTrip) {
                                    deleteCandidate = it.toEditablePlan()
                                } else {
                                    viewModel.postError("Shared trips are read-only for now.")
                                }
                            },
                            onOpenAlternatives = {
                                if (uiState.canEditTrip) {
                                    optionsPanelEventId = it
                                } else {
                                    viewModel.postError("Shared trips are read-only for now.")
                                }
                            },
                            onReviewAdvisory = { advisory ->
                                if (uiState.canEditTrip) {
                                    if (advisory.suggestedOptions.isEmpty() && !advisoryEvaluationInProgress) {
                                        viewModel.onItineraryVisible()
                                    }
                                    advisorySheetId = advisory.advisoryId
                                } else {
                                    viewModel.postError("Shared trips are read-only for now.")
                                }
                            },
                            onDismissAdvisory = viewModel::dismissAdvisory,
                            onCommitEventOrder = viewModel::applyCommittedEventOrder,
                            onPendingReorderDatesChange = { pendingReorderDates = it },
                            onVisibleDateChange = { itineraryVisibleDate = it }
                        )
                        displayMode == CurrentDisplayMode.WEEK -> CurrentTripWeekView(
                            events = events,
                            canEditTrip = uiState.canEditTrip,
                            sortedDates = calendarDates,
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            onEventClick = { selectedEventId = it.eventId },
                            onCreatePlan = { date, startMinutes ->
                                if (uiState.canEditTrip) {
                                    editorPlan = newEditablePlan(date, startMinutes)
                                } else {
                                    viewModel.postError("Shared trips are read-only for now.")
                                }
                            }
                        )
                        else -> CurrentTripDayView(
                            events = events,
                            canEditTrip = uiState.canEditTrip,
                            sortedDates = calendarDates,
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            onEventClick = { selectedEventId = it.eventId },
                            onCreatePlan = { date, startMinutes ->
                                if (uiState.canEditTrip) {
                                    editorPlan = newEditablePlan(date, startMinutes)
                                } else {
                                    viewModel.postError("Shared trips are read-only for now.")
                                }
                            }
                        )
                    }
                }
            }
            CurrentTripOverlayHost(
                uiState = uiState,
                canEditTrip = uiState.canEditTrip,
                eventOptions = eventOptions,
                rejectedOptions = rejectedOptions,
                optionsLoading = optionsLoading,
                yelpReviews = yelpReviews,
                reviewsLoading = reviewsLoading,
                shareTargets = shareTargets,
                selectedEventId = selectedEventId,
                editorPlan = editorPlan,
                deleteCandidate = deleteCandidate,
                optionsPanelEventId = optionsPanelEventId,
                showShareSheet = showShareSheet,
                shareConfirmation = shareConfirmation,
                onSelectedEventIdChange = { selectedEventId = it },
                onEditorPlanChange = { editorPlan = it },
                onDeleteCandidateChange = { deleteCandidate = it },
                onOptionsPanelEventIdChange = { optionsPanelEventId = it },
                onShowShareSheetChange = { showShareSheet = it },
                onShareConfirmationChange = { shareConfirmation = it },
                onSavePlan = viewModel::upsertPlan,
                onDeletePlan = { plan ->
                    viewModel.deletePlan(plan)
                    if (editorPlan?.eventId == plan.eventId) {
                        editorPlan = null
                    }
                    if (selectedEventId == plan.eventId) {
                        selectedEventId = null
                    }
                },
                onShareTrip = viewModel::shareTripToChat,
                onSelectOption = viewModel::selectOption,
                onRejectOption = viewModel::rejectOption,
                onLoadMoreOptions = viewModel::loadMoreOptions
            )

            activeAdvisory?.let { advisory ->
                val event = uiState.events.firstOrNull { it.eventId == advisory.eventId }
                if (event != null) {
                    TripAdvisoryReviewSheet(
                        advisory = advisory,
                        event = event,
                        canEditTrip = uiState.canEditTrip,
                        isLoadingSuggestions = advisoryEvaluationInProgress && advisory.suggestedOptions.isEmpty(),
                        onReplaceOption = { optionId ->
                            viewModel.replaceAdvisoryOption(
                                advisoryId = advisory.advisoryId,
                                eventId = advisory.eventId,
                                optionId = optionId
                            )
                        },
                        onSaveOption = { optionId ->
                            viewModel.saveAdvisoryOption(
                                advisoryId = advisory.advisoryId,
                                eventId = advisory.eventId,
                                optionId = optionId
                            )
                        },
                        onDismissAdvisory = { viewModel.dismissAdvisory(advisory.advisoryId) },
                        onDismiss = { advisorySheetId = null }
                    )
                }
            }
        }
    }
}

private fun initialCalendarSelection(
    dateFrom: String,
    dateTo: String,
    calendarDates: List<String>
): String {
    val today = todayIsoDate()
    val start = parseIsoDate(dateFrom)
    val end = parseIsoDate(dateTo)
    val todayDate = parseIsoDate(today)
    if (start != null && end != null && todayDate != null && !todayDate.isBefore(start) && !todayDate.isAfter(end)) {
        return today
    }
    return dateFrom.ifBlank { calendarDates.firstOrNull() ?: today }
}

private fun TripAdvisory.withLatestAdvisoryOptions(
    optionsByEvent: Map<String, List<EventOption>>
): TripAdvisory {
    if (suggestedOptions.isNotEmpty()) return this

    val latestSuggestions = optionsByEvent[eventId].orEmpty()
        .filter { option -> option.isAdvisorySuggestionFor(reason.name) }
        .take(3)

    return if (latestSuggestions.isEmpty()) {
        this
    } else {
        copy(suggestedOptions = latestSuggestions)
    }
}

private fun EventOption.isAdvisorySuggestionFor(reasonName: String): Boolean {
    val isAdvisorySource = source.equals("dummy_advisory", ignoreCase = true) ||
        source.equals("pinecone_advisory", ignoreCase = true)
    if (!isAdvisorySource) return false

    val optionReason = details[ATTR_ADVISORY_REASON]
    return optionReason.isNullOrBlank() || optionReason.equals(reasonName, ignoreCase = true)
}

