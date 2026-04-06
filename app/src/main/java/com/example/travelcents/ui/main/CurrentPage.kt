package com.example.travelcents.ui.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private enum class CurrentDisplayMode {
    ITINERARY,
    WEEK,
    DAY
}

private data class ScheduleWindow(
    val startHour: Int,
    val endHour: Int
)

private data class EventPalette(
    val container: Color,
    val accent: Color
)

private data class ColorOption(
    val key: String,
    val color: Color
)

@Composable
fun CurrentPage(
    modifier: Modifier = Modifier,
    viewModel: ItineraryViewModel = viewModel(),
    startInCalendar: Boolean = false,
    autoLoadTrip: Boolean = true,
    onViewItineraryRequested: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
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

    var displayModeName by rememberSaveable(startInCalendar) {
        mutableStateOf(
            if (startInCalendar) {
                CurrentDisplayMode.WEEK.name
            } else {
                CurrentDisplayMode.ITINERARY.name
            }
        )
    }
    val displayMode = CurrentDisplayMode.valueOf(displayModeName)
    var selectedDate by rememberSaveable { mutableStateOf("") }
    var editorPlan by remember { mutableStateOf<EditablePlan?>(null) }
    var deleteCandidate by remember { mutableStateOf<EditablePlan?>(null) }

    val tripDateRange = remember(uiState.dateFrom, uiState.dateTo, calendarDates) {
        buildTripDateRange(
            dateFrom = uiState.dateFrom,
            dateTo = uiState.dateTo,
            eventDates = calendarDates
        )
    }

    LaunchedEffect(autoLoadTrip) {
        if (autoLoadTrip) {
            viewModel.loadTrip()
        }
    }

    LaunchedEffect(calendarDates, uiState.dateFrom) {
        val fallbackDate = uiState.dateFrom.ifBlank { todayIsoDate() }
        if (selectedDate.isBlank() || selectedDate !in calendarDates) {
            selectedDate = calendarDates.firstOrNull() ?: fallbackDate
        }
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
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                SharedTripHeader(
                    tripTitle = uiState.tripTitle,
                    dateRange = tripDateRange,
                    canAdd = uiState.currentTripId != null,
                    onAddClick = {
                        if (uiState.currentTripId == null) {
                            viewModel.postError("Create a trip first before adding calendar plans.")
                        } else {
                            editorPlan = newEditablePlan(
                                date = selectedDate.ifBlank {
                                    calendarDates.firstOrNull()
                                        ?: uiState.dateFrom.ifBlank { todayIsoDate() }
                                },
                                startMinutes = 9 * 60
                            )
                        }
                    },
                    primaryActionLabel = when (displayMode) {
                        CurrentDisplayMode.ITINERARY -> "SWITCH TO CALENDAR"
                        CurrentDisplayMode.WEEK -> "SWITCH TO DAY"
                        CurrentDisplayMode.DAY -> "SWITCH TO WEEK"
                    },
                    onPrimaryActionClick = {
                        displayModeName = when (displayMode) {
                            CurrentDisplayMode.ITINERARY -> CurrentDisplayMode.WEEK.name
                            CurrentDisplayMode.WEEK -> CurrentDisplayMode.DAY.name
                            CurrentDisplayMode.DAY -> CurrentDisplayMode.WEEK.name
                        }
                    },
                    secondaryActionLabel = if (displayMode != CurrentDisplayMode.ITINERARY) {
                        "VIEW ITINERARY"
                    } else {
                        null
                    },
                    onSecondaryActionClick = if (displayMode != CurrentDisplayMode.ITINERARY) {
                        {
                            if (onViewItineraryRequested != null) {
                                onViewItineraryRequested()
                            } else {
                                displayModeName = CurrentDisplayMode.ITINERARY.name
                            }
                        }
                    } else {
                        null
                    }
                )
            }

            if (uiState.infoMessage != null || uiState.errorMessage != null) {
                MessageCard(
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
                    uiState.isLoading -> LoadingState()
                    uiState.currentTripId == null -> EmptyState(
                        title = "No Trip Yet",
                        body = uiState.infoMessage ?: "Create a trip from the New Trip tab to populate this calendar."
                    )
                    displayMode == CurrentDisplayMode.ITINERARY -> ItineraryContent(
                        events = events,
                        onEventClick = { event -> editorPlan = event.toEditablePlan() },
                        onDeleteClick = { event -> deleteCandidate = event.toEditablePlan() }
                    )
                    displayMode == CurrentDisplayMode.WEEK -> WeekCalendarContent(
                        events = events,
                        sortedDates = calendarDates,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onEventClick = { event -> editorPlan = event.toEditablePlan() },
                        onDeleteClick = { event -> deleteCandidate = event.toEditablePlan() },
                        onCreatePlan = { date, startMinutes ->
                            editorPlan = newEditablePlan(date = date, startMinutes = startMinutes)
                        }
                    )
                    else -> DayCalendarContent(
                        events = events,
                        sortedDates = calendarDates,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onEventClick = { event -> editorPlan = event.toEditablePlan() },
                        onDeleteClick = { event -> deleteCandidate = event.toEditablePlan() },
                        onCreatePlan = { date, startMinutes ->
                            editorPlan = newEditablePlan(date = date, startMinutes = startMinutes)
                        }
                    )
                }
            }
        }
    }

    deleteCandidate?.let { plan ->
        DeletePlanDialog(
            plan = plan,
            onDismiss = { deleteCandidate = null },
            onConfirmDelete = {
                viewModel.deletePlan(plan)
                if (editorPlan?.eventId == plan.eventId) {
                    editorPlan = null
                }
                deleteCandidate = null
            }
        )
    }

    editorPlan?.let { plan ->
        PlanEditorDialog(
            initialPlan = plan,
            onDismiss = { editorPlan = null },
            onSave = {
                viewModel.upsertPlan(it)
                editorPlan = null
            },
            onDelete = { planToDelete ->
                editorPlan = null
                deleteCandidate = planToDelete
            }
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isError) Color(0xFF3B1722) else DeepSea2)
            .border(
                width = 1.dp,
                color = if (isError) Color(0xFF8C3951) else DeepSea3.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = if (isError) Color(0xFFFFB4C7) else DeepSea5,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(22.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss message",
                tint = if (isError) Color(0xFFFFB4C7) else DeepSea4,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = DeepSea4)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Loading your trip...",
            color = DeepSea5,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DeepSea2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = null,
                tint = DeepSea4,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            color = DeepSea5,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = DeepSea4,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeletePlanDialog(
    plan: EditablePlan,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSea2,
        title = {
            Text(
                text = "Delete Plan?",
                color = DeepSea5,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Remove ${plan.title.ifBlank { "this event" }} from your trip calendar?",
                color = DeepSea4
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text(text = "DELETE", color = Color(0xFFE77D90))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CANCEL", color = DeepSea5)
            }
        }
    )
}

@Composable
private fun ItineraryContent(
    events: List<TravelEvent>,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit
) {
    if (events.isEmpty()) {
        EmptyState(
            title = "No Plans Yet",
            body = "Tap the + button to create your first reservation or activity."
        )
        return
    }

    val groupedEvents = remember(events) { events.groupBy { it.date } }
    val sortedDates = remember(events) {
        events.map { it.date }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(sortedDates, key = { it }) { date ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = formatItineraryHeader(date),
                    color = DeepSea5,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                groupedEvents[date].orEmpty().forEach { event ->
                    ListPlanCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        onDeleteClick = { onDeleteClick(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListPlanCard(
    event: TravelEvent,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val palette = eventPalette(event)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(palette.accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    text = formatDisplayTimeRange(event.startTime, event.endTime),
                    color = DeepSea4,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = eventTitle(event),
                    color = DeepSea5,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eventSubtitle(event),
                    color = DeepSea4,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .padding(top = 8.dp, end = 8.dp)
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete plan",
                    tint = Color(0xFFE77D90),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekCalendarContent(
    events: List<TravelEvent>,
    sortedDates: List<String>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onCreatePlan: (String, Int) -> Unit
) {
    if (sortedDates.isEmpty()) {
        EmptyState(
            title = "No Calendar Dates",
            body = "Set trip dates first so you can add plans to the calendar."
        )
        return
    }

    val scheduleWindow = remember(events) { buildScheduleWindow(events) }
    val eventsByDate = remember(events) { events.groupBy { it.date } }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val hourHeight = 72.dp
    val timeAxisWidth = 50.dp
    val columnSpacing = 8.dp
    val columnWidth = 116.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        CalendarDateChipRow(
            dates = sortedDates,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            horizontalScrollState = horizontalScroll,
            leadingSpacerWidth = timeAxisWidth,
            chipWidth = columnWidth,
            trailingSpacerWidth = 0.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(horizontalScroll)
            ) {
                TimeAxis(
                    scheduleWindow = scheduleWindow,
                    hourHeight = hourHeight,
                    topSpacing = 0.dp
                )

                Spacer(modifier = Modifier.width(columnSpacing))

                sortedDates.forEach { date ->
                    ScheduleDayColumn(
                        modifier = Modifier.width(columnWidth),
                        date = date,
                        events = eventsByDate[date].orEmpty(),
                        scheduleWindow = scheduleWindow,
                        hourHeight = hourHeight,
                        compact = true,
                        showHeader = false,
                        selected = date == selectedDate,
                        onHeaderClick = { onDateSelected(date) },
                        onEventClick = onEventClick,
                        onDeleteClick = onDeleteClick,
                        onEmptySlotClick = { startMinutes -> onCreatePlan(date, startMinutes) }
                    )
                    Spacer(modifier = Modifier.width(columnSpacing))
                }
            }
        }
    }
}

@Composable
private fun DayCalendarContent(
    events: List<TravelEvent>,
    sortedDates: List<String>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onCreatePlan: (String, Int) -> Unit
) {
    if (sortedDates.isEmpty()) {
        EmptyState(
            title = "No Calendar Dates",
            body = "Set trip dates first so you can add plans to the calendar."
        )
        return
    }

    val activeDate = selectedDate.takeIf { it.isNotBlank() } ?: sortedDates.firstOrNull().orEmpty()
    val scheduleWindow = remember(events) { buildScheduleWindow(events) }
    val verticalScroll = rememberScrollState()
    val hourHeight = 76.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        CalendarDateChipRow(
            dates = sortedDates,
            selectedDate = activeDate,
            onDateSelected = onDateSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TimeAxis(
                    scheduleWindow = scheduleWindow,
                    hourHeight = hourHeight,
                    topSpacing = 0.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                ScheduleDayColumn(
                    modifier = Modifier.weight(1f),
                    date = activeDate,
                    events = events.filter { it.date == activeDate },
                    scheduleWindow = scheduleWindow,
                    hourHeight = hourHeight,
                    compact = false,
                    showHeader = false,
                    selected = true,
                    onHeaderClick = null,
                    onEventClick = onEventClick,
                    onDeleteClick = onDeleteClick,
                    onEmptySlotClick = { startMinutes -> onCreatePlan(activeDate, startMinutes) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDateChipRow(
    dates: List<String>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    horizontalScrollState: ScrollState? = null,
    leadingSpacerWidth: Dp = 0.dp,
    chipWidth: Dp? = null,
    trailingSpacerWidth: Dp? = null
) {
    val scrollState = horizontalScrollState ?: rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingSpacerWidth > 0.dp) {
            Spacer(modifier = Modifier.width(leadingSpacerWidth))
        }
        dates.forEach { date ->
            val selected = date == selectedDate
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) DeepSea3 else DeepSea2)
                    .border(
                        width = 1.dp,
                        color = if (selected) DeepSea4 else DeepSea3.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onDateSelected(date) }
                    .then(
                        if (chipWidth != null) Modifier.width(chipWidth) else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier
                        .then(if (chipWidth != null) Modifier.fillMaxWidth() else Modifier)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatDayOfWeekShort(date),
                        color = if (selected) DeepSea5 else DeepSea4,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatMonthDayCompact(date),
                        color = DeepSea5,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (trailingSpacerWidth != null) {
            Spacer(modifier = Modifier.width(trailingSpacerWidth))
        }
    }
}

@Composable
private fun TimeAxis(
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    topSpacing: Dp = 56.dp
) {
    Column(modifier = Modifier.width(50.dp)) {
        Spacer(modifier = Modifier.height(topSpacing))
        for (hour in scheduleWindow.startHour until scheduleWindow.endHour) {
            Box(
                modifier = Modifier
                    .height(hourHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = hourLabel(hour),
                    color = DeepSea4.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ScheduleDayColumn(
    modifier: Modifier,
    date: String,
    events: List<TravelEvent>,
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    compact: Boolean,
    showHeader: Boolean,
    selected: Boolean,
    onHeaderClick: (() -> Unit)?,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onEmptySlotClick: (Int) -> Unit
) {
    val gridHeight = hourHeight * (scheduleWindow.endHour - scheduleWindow.startHour).toFloat()
    val headerShape = RoundedCornerShape(18.dp)
    val cardShape = RoundedCornerShape(20.dp)

    Column(modifier = modifier) {
        if (showHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(headerShape)
                    .background(if (selected) DeepSea3 else DeepSea2)
                    .border(
                        width = 1.dp,
                        color = if (selected) DeepSea4 else DeepSea3.copy(alpha = 0.55f),
                        shape = headerShape
                    )
                    .clickable(enabled = onHeaderClick != null) { onHeaderClick?.invoke() }
                    .padding(vertical = if (compact) 10.dp else 12.dp, horizontal = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = formatDayOfWeekShort(date),
                        color = DeepSea4,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatLongDayLabel(date),
                        color = DeepSea5,
                        fontSize = if (compact) 12.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .clip(cardShape)
                .background(DeepSea2.copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    color = if (selected) DeepSea3 else DeepSea3.copy(alpha = 0.45f),
                    shape = cardShape
                )
                .drawBehind {
                    val slotHeight = hourHeight.toPx()
                    val strokeWidth = 1.dp.toPx()
                    repeat(scheduleWindow.endHour - scheduleWindow.startHour + 1) { index ->
                        val y = index * slotHeight
                        drawLine(
                            color = DeepSea3.copy(alpha = 0.45f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
                .padding(horizontal = 4.dp)
        ) {
            ScheduleGridTapTarget(
                scheduleWindow = scheduleWindow,
                hourHeight = hourHeight,
                onSlotClick = onEmptySlotClick
            )

            events.forEach { event ->
                ScheduleEventCard(
                    event = event,
                    scheduleWindow = scheduleWindow,
                    hourHeight = hourHeight,
                    compact = compact,
                    onClick = { onEventClick(event) },
                    onDeleteClick = { onDeleteClick(event) }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ScheduleGridTapTarget(
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    onSlotClick: (Int) -> Unit
) {
    val totalMinutes = (scheduleWindow.endHour - scheduleWindow.startHour) * 60

    Box(
        modifier = Modifier
            .matchParentSize()
            .pointerInput(scheduleWindow, hourHeight) {
                detectTapGestures { tapOffset ->
                    val tappedMinutes = ((tapOffset.y / hourHeight.toPx()) * 60).toInt()
                    val roundedMinutes = ((tappedMinutes / 30) * 30)
                        .coerceIn(0, max(totalMinutes - 30, 0))
                    onSlotClick((scheduleWindow.startHour * 60) + roundedMinutes)
                }
            }
    )
}

@Composable
private fun BoxScope.ScheduleEventCard(
    event: TravelEvent,
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    compact: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val startMinutes = parseTimeToMinutes(event.startTime) ?: (scheduleWindow.startHour * 60)
    val endMinutes = parseTimeToMinutes(event.endTime)
    val durationMinutes = when {
        endMinutes != null && endMinutes > startMinutes -> endMinutes - startMinutes
        else -> 90
    }.coerceIn(45, 240)

    val topOffset = hourHeight * ((startMinutes - (scheduleWindow.startHour * 60)).coerceAtLeast(0) / 60f)
    val minHeight = if (compact) 56.dp else 76.dp
    val calculatedHeight = hourHeight * (durationMinutes / 60f)
    val eventHeight = if (calculatedHeight < minHeight) minHeight else calculatedHeight
    val palette = eventPalette(event)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .offset(y = topOffset)
            .height(eventHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(palette.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = formatDisplayTimeRange(event.startTime, event.endTime),
                        color = DeepSea4,
                        fontSize = if (compact) 9.sp else 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(if (compact) 18.dp else 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete plan",
                            tint = Color(0xFFE77D90),
                            modifier = Modifier.size(if (compact) 11.dp else 14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eventTitle(event),
                    color = DeepSea5,
                    fontSize = if (compact) 11.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!compact || eventHeight > 68.dp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = eventSubtitle(event),
                        color = DeepSea4,
                        fontSize = if (compact) 9.sp else 11.sp,
                        maxLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanEditorDialog(
    initialPlan: EditablePlan,
    onDismiss: () -> Unit,
    onSave: (EditablePlan) -> Unit,
    onDelete: (EditablePlan) -> Unit
) {
    val context = LocalContext.current
    var title by remember(initialPlan) { mutableStateOf(initialPlan.title) }
    var date by remember(initialPlan) { mutableStateOf(normalizeDate(initialPlan.date)) }
    var time by remember(initialPlan) { mutableStateOf(formatDisplayTime(initialPlan.startTime)) }
    var endTime by remember(initialPlan) {
        mutableStateOf(
            initialPlan.endTime.takeIf { it.isNotBlank() }?.let(::formatDisplayTime).orEmpty()
        )
    }
    var location by remember(initialPlan) { mutableStateOf(initialPlan.location) }
    var notes by remember(initialPlan) { mutableStateOf(initialPlan.notes) }
    var colorKey by remember(initialPlan) { mutableStateOf(initialPlan.colorKey.ifBlank { "rose" }) }
    val timeZoneId = remember(initialPlan) {
        initialPlan.timeZoneId.ifBlank { defaultPlanTimeZoneId() }
    }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val colorOptions = remember {
        listOf(
            ColorOption("rose", Color(0xFFFF677C)),
            ColorOption("teal", Color(0xFF4CA7C5)),
            ColorOption("olive", Color(0xFF8A9365)),
            ColorOption("plum", Color(0xFF5A2A7B)),
            ColorOption("lavender", Color(0xFF6D5D8E)),
            ColorOption("cyan", Color(0xFF268C95))
        )
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DeepSea2,
        unfocusedContainerColor = DeepSea2,
        focusedTextColor = DeepSea5,
        unfocusedTextColor = DeepSea5,
        focusedBorderColor = DeepSea3,
        unfocusedBorderColor = DeepSea3.copy(alpha = 0.7f),
        focusedLabelColor = DeepSea5,
        unfocusedLabelColor = DeepSea4,
        cursorColor = DeepSea5
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = DeepSea1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close editor",
                            tint = DeepSea4
                        )
                    }

                    Text(
                        text = if (initialPlan.eventId == null) "ADD PLAN" else "EDIT PLAN",
                        color = DeepSea5,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )

                    if (initialPlan.eventId != null) {
                        IconButton(onClick = { onDelete(initialPlan) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete plan",
                                tint = Color(0xFFE77D90)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Scuba Diving") },
                    singleLine = true,
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerField(
                        modifier = Modifier.weight(1f),
                        label = "Date",
                        value = date,
                        placeholder = "YYYY-MM-DD",
                        icon = Icons.Default.CalendarToday,
                        onClick = {
                            showDatePicker(context, date) {
                                date = it
                                validationMessage = null
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerField(
                        modifier = Modifier.weight(1f),
                        label = "Start Time",
                        value = time,
                        placeholder = "Select time",
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            showTimePicker(context, time) {
                                time = it
                                validationMessage = null
                            }
                        }
                    )

                    PickerField(
                        modifier = Modifier.weight(1f),
                        label = "End Time",
                        value = endTime,
                        placeholder = "Optional",
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            showTimePicker(context, endTime.ifBlank { plusMinutes(time, 60) }) {
                                endTime = it
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TIME ZONE",
                    color = DeepSea4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimeZoneLabel(
                        timeZoneId = timeZoneId,
                        date = date.ifBlank { todayIsoDate() },
                        time = time.ifBlank { "12:00 PM" }
                    ),
                    color = DeepSea5,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    placeholder = { Text("Beach, resort, restaurant...") },
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text("Notes") },
                    placeholder = { Text("Add booking notes or reminders") },
                    maxLines = 5,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "LABEL COLOR",
                    color = DeepSea4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { option ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (option.key == colorKey) 2.dp else 1.dp,
                                    color = if (option.key == colorKey) DeepSea5 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorKey = option.key }
                        )
                    }
                }

                validationMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        color = Color(0xFFFFB4C7),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        when {
                            title.isBlank() -> validationMessage = "A title is required."
                            date.isBlank() -> validationMessage = "Select a date for this plan."
                            time.isBlank() -> validationMessage = "Select a time for this plan."
                            else -> onSave(
                                initialPlan.copy(
                                    title = title.trim(),
                                    date = date,
                                    startTime = time,
                                    endTime = endTime,
                                    timeZoneId = timeZoneId,
                                    location = location.trim(),
                                    notes = notes.trim(),
                                    colorKey = colorKey
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepSea3,
                        contentColor = DeepSea5
                    )
                ) {
                    Text(
                        text = if (initialPlan.eventId == null) "CONFIRM PLAN" else "UPDATE PLAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerField(
    modifier: Modifier,
    label: String,
    value: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = DeepSea4,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DeepSea2)
                .border(1.dp, DeepSea3.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) DeepSea4.copy(alpha = 0.65f) else DeepSea5,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeepSea4,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun TravelEvent.toEditablePlan(): EditablePlan {
    return EditablePlan(
        eventId = eventId,
        type = type,
        title = eventTitle(this),
        date = normalizeDate(date),
        startTime = formatDisplayTime(startTime),
        endTime = endTime.takeIf { it.isNotBlank() }?.let(::formatDisplayTime).orEmpty(),
        timeZoneId = tz.ifBlank { defaultPlanTimeZoneId() },
        location = editableLocation(this),
        notes = editableNotes(this),
        colorKey = details["colorKey"] ?: defaultColorKeyForType(type),
        existingDetails = details
    )
}

private fun newEditablePlan(
    date: String,
    startMinutes: Int
): EditablePlan {
    val startTime = formatMinutes(startMinutes)
    return EditablePlan(
        date = normalizeDate(date),
        startTime = startTime,
        endTime = plusMinutes(startTime, 60),
        timeZoneId = defaultPlanTimeZoneId(),
        colorKey = defaultColorKeyForType("activity")
    )
}

private fun eventTitle(event: TravelEvent): String {
    return event.details["title"]
        ?: when (event.type.lowercase(Locale.US)) {
            "flight" -> {
                val destination = event.details["destination_airport"]
                    ?: event.details["location"]
                    ?: "Destination"
                "Flight to $destination"
            }
            "hotel" -> "Hotel Check-in"
            "restaurant" -> event.details["restaurant_name"] ?: "Dinner Reservation"
            else -> event.details["activity_name"]
                ?: event.details["name"]
                ?: event.details["title"]
                ?: event.type.replaceFirstChar { it.uppercase(Locale.US) }
        }
}

private fun eventSubtitle(event: TravelEvent): String {
    return event.details["location"]
        ?: when (event.type.lowercase(Locale.US)) {
            "flight" -> listOf(
                event.details["airline"],
                event.details["flight_number"]
            ).filter { !it.isNullOrBlank() }.joinToString(" ")
            "hotel" -> event.details["hotel_name"]
            "restaurant" -> event.details["cuisine"]
            else -> event.details["description"]
        }.orEmpty().ifBlank { "Tap to edit details" }
}

private fun editableLocation(event: TravelEvent): String {
    return event.details["location"]
        ?: event.details["hotel_name"]
        ?: event.details["restaurant_name"]
        ?: event.details["destination_airport"]
        ?: ""
}

private fun editableNotes(event: TravelEvent): String {
    return event.details["description"]
        ?: event.details["cuisine"]
        ?: listOf(event.details["airline"], event.details["flight_number"])
            .filter { !it.isNullOrBlank() }
            .joinToString(" ")
}

private fun eventPalette(event: TravelEvent): EventPalette {
    return when ((event.details["colorKey"] ?: defaultColorKeyForType(event.type)).lowercase(Locale.US)) {
        "rose", "flight" -> EventPalette(
            container = Color(0xFF3B2637),
            accent = Color(0xFFFF677C)
        )
        "teal", "hotel" -> EventPalette(
            container = Color(0xFF193744),
            accent = Color(0xFF4CA7C5)
        )
        "olive", "restaurant" -> EventPalette(
            container = Color(0xFF343D2F),
            accent = Color(0xFF8A9365)
        )
        "cyan" -> EventPalette(
            container = Color(0xFF12353D),
            accent = Color(0xFF268C95)
        )
        "lavender" -> EventPalette(
            container = Color(0xFF302B45),
            accent = Color(0xFF7B6D9C)
        )
        else -> EventPalette(
            container = Color(0xFF2B233B),
            accent = Color(0xFF5A2A7B)
        )
    }
}

private fun defaultColorKeyForType(type: String): String {
    return when (type.lowercase(Locale.US)) {
        "flight" -> "rose"
        "hotel" -> "teal"
        "restaurant" -> "olive"
        else -> "plum"
    }
}

private fun buildScheduleWindow(events: List<TravelEvent>): ScheduleWindow {
    val startMinutes = events.mapNotNull { parseTimeToMinutes(it.startTime) }
    val endMinutes = events.mapNotNull { parseTimeToMinutes(it.endTime) }

    if (startMinutes.isEmpty()) {
        return ScheduleWindow(startHour = 8, endHour = 20)
    }

    val earliest = startMinutes.minOrNull() ?: 8 * 60
    val latest = max(
        endMinutes.maxOrNull() ?: (startMinutes.maxOrNull() ?: 18 * 60) + 90,
        earliest + 240
    )

    val startHour = max(6, (earliest / 60) - 1)
    val endHour = min(23, ceil((latest + 60) / 60f).toInt())

    return ScheduleWindow(
        startHour = startHour,
        endHour = max(endHour, startHour + 6)
    )
}

private fun buildTripDateRange(
    dateFrom: String,
    dateTo: String,
    eventDates: List<String>
): String {
    val start = dateFrom.ifBlank { eventDates.firstOrNull().orEmpty() }
    val end = dateTo.ifBlank { eventDates.lastOrNull().orEmpty() }

    if (start.isBlank() && end.isBlank()) {
        return "DATES TBD"
    }

    if (start == end || end.isBlank()) {
        return formatTripDate(start).uppercase(Locale.US)
    }

    return "${formatTripDate(start).uppercase(Locale.US)} - ${formatTripDate(end).uppercase(Locale.US)}"
}

private fun showDatePicker(
    context: android.content.Context,
    initialDate: String = "",
    onDateSelected: (String) -> Unit
) {
    val initial = parseIsoDate(initialDate)
    val calendar = Calendar.getInstance().apply {
        if (initial != null) {
            set(initial.year, initial.monthValue - 1, initial.dayOfMonth)
        }
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected("%04d-%02d-%02d".format(year, month + 1, dayOfMonth))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: android.content.Context,
    initialTime: String = "",
    onTimeSelected: (String) -> Unit
) {
    val initial = parseFlexibleTime(initialTime)
    val calendar = Calendar.getInstance().apply {
        if (initial != null) {
            set(Calendar.HOUR_OF_DAY, initial.hour)
            set(Calendar.MINUTE, initial.minute)
        }
    }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(formatDisplayTime("%02d:%02d".format(hourOfDay, minute)))
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}
