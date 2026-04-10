package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.ui.main.current.calendar.buildTripDateRange
import com.example.travelcents.ui.modules.buildCalendarDates
import com.example.travelcents.ui.modules.sortEventsForCalendar
import com.example.travelcents.ui.modules.todayIsoDate
import com.example.travelcents.ui.theme.DeepSea1

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
    val yelpReviews by viewModel.yelpReviews.collectAsState()
    val reviewsLoading by viewModel.reviewsLoading.collectAsState()
    val shareTargets by viewModel.shareTargets.collectAsState()

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
    var editorPlan by remember { mutableStateOf<EditablePlan?>(null) }
    var deleteCandidate by remember { mutableStateOf<EditablePlan?>(null) }
    var jiggleMode by remember { mutableStateOf(false) }
    var optionsPanelEventId by remember { mutableStateOf<String?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
    var shareConfirmation by remember { mutableStateOf<String?>(null) }

    val tripDateRange = remember(uiState.dateFrom, uiState.dateTo, calendarDates) {
        buildTripDateRange(
            dateFrom = uiState.dateFrom,
            dateTo = uiState.dateTo,
            eventDates = calendarDates
        )
    }

    LaunchedEffect(autoLoadTrip, tripId) {
        if (autoLoadTrip) {
            viewModel.loadTrip(tripId)
        }
    }

    LaunchedEffect(uiState.currentTripId) {
        viewModel.loadAllTrips()
    }

    LaunchedEffect(calendarDates, uiState.dateFrom) {
        val fallbackDate = uiState.dateFrom.ifBlank { todayIsoDate() }
        if (selectedDate.isBlank() || selectedDate !in calendarDates) {
            selectedDate = calendarDates.firstOrNull() ?: fallbackDate
        }
    }

    LaunchedEffect(editorPlan) {
        editorPlan?.existingDetails?.get("yelp_id")
            ?.takeIf { it.isNotBlank() }
            ?.let(viewModel::fetchYelpReviews)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            CurrentTripHeader(
                tripTitle = uiState.tripTitle,
                dateRange = tripDateRange,
                currentTripId = uiState.currentTripId,
                allTrips = allTrips,
                canAdd = uiState.currentTripId != null,
                isReorderActive = displayMode == CurrentDisplayMode.ITINERARY && jiggleMode,
                onAddClick = {
                    if (uiState.currentTripId == null) {
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
                    viewModel.fetchShareTargets()
                    showShareSheet = true
                },
                onToggleReorder = {
                    if (displayMode != CurrentDisplayMode.ITINERARY) {
                        onNavigateToMode(CurrentDisplayMode.ITINERARY)
                    }
                    jiggleMode = !jiggleMode
                },
                onArchiveTrip = { currentTripId ->
                    jiggleMode = false
                    viewModel.archiveTrip(currentTripId)
                },
                onDeleteTrip = { currentTripId ->
                    jiggleMode = false
                    viewModel.deleteTrip(currentTripId)
                },
                onSwitchTrip = { currentTripId ->
                    jiggleMode = false
                    optionsPanelEventId = null
                    editorPlan = null
                    deleteCandidate = null
                    viewModel.loadTrip(currentTripId)
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
                        eventOptions = eventOptions,
                        rejectedOptions = rejectedOptions,
                        jiggleMode = jiggleMode,
                        onEventClick = { editorPlan = it.toEditablePlan() },
                        onDeleteClick = { deleteCandidate = it.toEditablePlan() },
                        onOpenAlternatives = { optionsPanelEventId = it },
                        onMoveEvent = viewModel::moveEventLocally,
                        onPersistEventPlacements = viewModel::persistEventPlacements
                    )
                    displayMode == CurrentDisplayMode.WEEK -> CurrentTripWeekView(
                        events = events,
                        sortedDates = calendarDates,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onEventClick = { editorPlan = it.toEditablePlan() },
                        onDeleteClick = { deleteCandidate = it.toEditablePlan() },
                        onCreatePlan = { date, startMinutes ->
                            editorPlan = newEditablePlan(date, startMinutes)
                        }
                    )
                    else -> CurrentTripDayView(
                        events = events,
                        sortedDates = calendarDates,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onEventClick = { editorPlan = it.toEditablePlan() },
                        onDeleteClick = { deleteCandidate = it.toEditablePlan() },
                        onCreatePlan = { date, startMinutes ->
                            editorPlan = newEditablePlan(date, startMinutes)
                        }
                    )
                }
            }
        }
    }

    CurrentTripOverlayHost(
        uiState = uiState,
        eventOptions = eventOptions,
        rejectedOptions = rejectedOptions,
        yelpReviews = yelpReviews,
        reviewsLoading = reviewsLoading,
        shareTargets = shareTargets,
        editorPlan = editorPlan,
        deleteCandidate = deleteCandidate,
        optionsPanelEventId = optionsPanelEventId,
        showShareSheet = showShareSheet,
        shareConfirmation = shareConfirmation,
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
        },
        onShareTrip = viewModel::shareTripToChat,
        onSelectOption = viewModel::selectOption,
        onRejectOption = viewModel::rejectOption
    )
}
