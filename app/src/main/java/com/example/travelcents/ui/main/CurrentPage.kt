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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.platform.LocalDensity
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
import java.time.Duration
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

private data class EventTypeOption(
    val key: String,
    val label: String
)

private data class DisplayModeOption(
    val mode: CurrentDisplayMode,
    val label: String
)

private data class EventRenderSpan(
    val event: TravelEvent,
    val startMinutes: Int,
    val endMinutes: Int,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean
)

private data class EventLayoutInfo(
    val span: EventRenderSpan,
    val columnIndex: Int,
    val columnCount: Int
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
                    controlsContent = {
                        CurrentDisplayModeTabs(
                            selectedMode = displayMode,
                            onModeSelected = { selectedMode ->
                                if (selectedMode == CurrentDisplayMode.ITINERARY && onViewItineraryRequested != null) {
                                    onViewItineraryRequested()
                                }
                                displayModeName = selectedMode.name
                            }
                        )
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
private fun CurrentDisplayModeTabs(
    selectedMode: CurrentDisplayMode,
    onModeSelected: (CurrentDisplayMode) -> Unit
) {
    val tabs = remember {
        listOf(
            DisplayModeOption(CurrentDisplayMode.ITINERARY, "Itinerary"),
            DisplayModeOption(CurrentDisplayMode.DAY, "Day View"),
            DisplayModeOption(CurrentDisplayMode.WEEK, "Week View")
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSea2, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedMode == tab.mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) DeepSea3 else Color.Transparent)
                    .clickable(enabled = !isSelected) { onModeSelected(tab.mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) DeepSea5 else DeepSea4,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DayNavigationBar(
    dates: List<String>,
    activeDate: String,
    onDateSelected: (String) -> Unit
) {
    val activeIndex = dates.indexOf(activeDate).coerceAtLeast(0)
    val previousDate = dates.getOrNull(activeIndex - 1)
    val nextDate = dates.getOrNull(activeIndex + 1)
    val todayDate = todayIsoDate().takeIf { it in dates && it != activeDate }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepSea2)
            .border(1.dp, DeepSea3.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { previousDate?.let(onDateSelected) },
                enabled = previousDate != null,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = if (previousDate != null) DeepSea5 else DeepSea4.copy(alpha = 0.35f)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDayOfWeekShort(activeDate),
                    color = DeepSea4,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = formatLongDayLabel(activeDate),
                    color = DeepSea5,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { nextDate?.let(onDateSelected) },
                enabled = nextDate != null,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (nextDate != null) DeepSea5 else DeepSea4.copy(alpha = 0.35f)
                )
            }
        }

        if (todayDate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepSea3)
                    .clickable { onDateSelected(todayDate) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "GO TO TODAY",
                    color = DeepSea5,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WeekNavigationBar(
    allDates: List<String>,
    visibleDates: List<String>,
    onDateSelected: (String) -> Unit
) {
    val weekStartIndex = allDates.indexOf(visibleDates.firstOrNull()).coerceAtLeast(0)
    val previousAnchor = allDates.getOrNull((weekStartIndex - 7).coerceAtLeast(0))
        ?.takeIf { visibleDates.firstOrNull() != allDates.firstOrNull() }
    val nextAnchor = allDates.getOrNull((weekStartIndex + 7).coerceAtMost(allDates.lastIndex))
        ?.takeIf { visibleDates.lastOrNull() != allDates.lastOrNull() }
    val weekLabel = buildTripDateRange(
        dateFrom = visibleDates.firstOrNull().orEmpty(),
        dateTo = visibleDates.lastOrNull().orEmpty(),
        eventDates = visibleDates
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepSea2)
            .border(1.dp, DeepSea3.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { previousAnchor?.let(onDateSelected) },
                enabled = previousAnchor != null,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous week",
                    tint = if (previousAnchor != null) DeepSea5 else DeepSea4.copy(alpha = 0.35f)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WEEK VIEW",
                    color = DeepSea4,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = weekLabel,
                    color = DeepSea5,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { nextAnchor?.let(onDateSelected) },
                enabled = nextAnchor != null,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next week",
                    tint = if (nextAnchor != null) DeepSea5 else DeepSea4.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun WeekOverviewDayRow(
    date: String,
    selected: Boolean,
    events: List<TravelEvent>,
    canAddPlan: Boolean,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) DeepSea3.copy(alpha = 0.88f) else DeepSea1.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = if (selected) DeepSea4.copy(alpha = 0.45f) else DeepSea3.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(78.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formatDayOfWeekShort(date),
                color = DeepSea4,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = formatMonthDayCompact(date),
                color = DeepSea5,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = if (events.isEmpty()) "Open day" else "${events.size} plan${if (events.size == 1) "" else "s"}",
                color = DeepSea4,
                fontSize = 11.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (events.isEmpty()) {
                Text(
                    text = "No plans yet",
                    color = DeepSea4,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                events.forEach { event ->
                    WeekOverviewEventRow(
                        event = event,
                        date = date,
                        onClick = { onEventClick(event) },
                        onDeleteClick = { onDeleteClick(event) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (canAddPlan) DeepSea2 else DeepSea2.copy(alpha = 0.45f))
                .then(if (canAddPlan) Modifier.clickable(onClick = onAddClick) else Modifier)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (canAddPlan) "+ Plan" else "No Trip",
                color = if (canAddPlan) DeepSea5 else DeepSea4,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeekOverviewEventRow(
    event: TravelEvent,
    date: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val span = remember(event, date) { eventSpanForDate(event, date) }
    val palette = eventPalette(event)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.container)
            .clickable(onClick = onClick)
            .padding(start = 0.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(26.dp)
                .background(palette.accent)
        )

        Text(
            text = span?.let(::renderSpanLabel) ?: formatDisplayTimeRange(event.startTime, event.endTime),
            color = DeepSea4,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(76.dp)
                .padding(start = 8.dp, end = 6.dp)
        )

        Text(
            text = eventTitle(event),
            color = DeepSea5,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete plan",
                tint = Color(0xFFE77D90),
                modifier = Modifier.size(12.dp)
            )
        }
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

    val visibleWeekDates = remember(sortedDates, selectedDate) {
        visibleWeekDates(sortedDates, selectedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        WeekNavigationBar(
            allDates = sortedDates,
            visibleDates = visibleWeekDates,
            onDateSelected = onDateSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSea2)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleWeekDates, key = { it }) { date ->
                    WeekOverviewDayRow(
                        date = date,
                        selected = date == selectedDate,
                        events = eventsForDate(events, date),
                        canAddPlan = date in sortedDates,
                        onEventClick = onEventClick,
                        onDeleteClick = onDeleteClick,
                        onAddClick = { onCreatePlan(date, defaultStartMinutesForDate(events, date)) }
                    )
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
    val dayEvents = remember(events, activeDate) { eventsForDate(events, activeDate) }
    val scheduleWindow = remember(dayEvents, activeDate) {
        buildScheduleWindow(dayEvents, listOf(activeDate))
    }
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current
    val hourHeight = 64.dp
    val initialScrollMinutes = remember(activeDate, dayEvents) {
        preferredDayScrollMinutes(
            date = activeDate,
            events = dayEvents
        )
    }

    LaunchedEffect(activeDate) {
        val targetOffset = with(density) {
            (((initialScrollMinutes - (scheduleWindow.startHour * 60)).coerceAtLeast(0) / 60f) * hourHeight.toPx()).roundToInt()
        }
        verticalScroll.animateScrollTo(targetOffset)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        DayNavigationBar(
            dates = sortedDates,
            activeDate = activeDate,
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
                    events = dayEvents,
                    scheduleWindow = scheduleWindow,
                    hourHeight = hourHeight,
                    compact = false,
                    showHeader = false,
                    selected = true,
                    onHeaderClick = null,
                    onEventClick = onEventClick,
                    onDeleteClick = onDeleteClick,
                    onEmptySlotClick = { startMinutes -> onCreatePlan(activeDate, startMinutes) },
                    showCurrentTimeIndicator = activeDate == todayIsoDate()
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
    topSpacing: Dp = 56.dp,
    useTwentyFourHourLabels: Boolean = false
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
                    text = if (useTwentyFourHourLabels) hourLabel24(hour) else hourLabel(hour),
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
    onEmptySlotClick: (Int) -> Unit,
    showCurrentTimeIndicator: Boolean = false
) {
    val gridHeight = hourHeight * (scheduleWindow.endHour - scheduleWindow.startHour).toFloat()
    val headerShape = RoundedCornerShape(18.dp)
    val cardShape = RoundedCornerShape(20.dp)
    val eventLayouts = remember(events, date, scheduleWindow) {
        buildEventLayouts(
            date = date,
            events = events,
            scheduleWindow = scheduleWindow
        )
    }

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

        BoxWithConstraints(
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
        ) {
            ScheduleGridTapTarget(
                scheduleWindow = scheduleWindow,
                hourHeight = hourHeight,
                onSlotClick = onEmptySlotClick
            )

            currentTimeMinutes(date)
                ?.takeIf { showCurrentTimeIndicator && it in (scheduleWindow.startHour * 60) until (scheduleWindow.endHour * 60) }
                ?.let { nowMinutes ->
                    CurrentTimeIndicator(
                        currentMinutes = nowMinutes,
                        scheduleWindow = scheduleWindow,
                        hourHeight = hourHeight
                    )
                }

            eventLayouts.forEach { layout ->
                ScheduleEventCard(
                    layout = layout,
                    maxWidth = maxWidth,
                    scheduleWindow = scheduleWindow,
                    hourHeight = hourHeight,
                    compact = compact,
                    onClick = { onEventClick(layout.span.event) },
                    onDeleteClick = { onDeleteClick(layout.span.event) }
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
private fun BoxScope.CurrentTimeIndicator(
    currentMinutes: Int,
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp
) {
    val topOffset = hourHeight * ((currentMinutes - (scheduleWindow.startHour * 60)).coerceAtLeast(0) / 60f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = topOffset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B6B))
        )
        Box(
            modifier = Modifier
                .padding(start = 4.dp, end = 8.dp)
                .height(2.dp)
                .weight(1f)
                .background(Color(0xFFFF6B6B))
        )
    }
}

@Composable
private fun BoxScope.ScheduleEventCard(
    layout: EventLayoutInfo,
    maxWidth: Dp,
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    compact: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val span = layout.span
    val startMinutes = span.startMinutes
    val durationMinutes = (span.endMinutes - span.startMinutes).coerceAtLeast(20)
    val topOffset = hourHeight * ((startMinutes - (scheduleWindow.startHour * 60)).coerceAtLeast(0) / 60f)
    val minHeight = if (compact) 40.dp else 52.dp
    val calculatedHeight = hourHeight * (durationMinutes / 60f)
    val eventHeight = if (calculatedHeight < minHeight) minHeight else calculatedHeight
    val palette = eventPalette(span.event)
    val columnGap = if (compact) 4.dp else 6.dp
    val horizontalInset = if (compact) 4.dp else 6.dp
    val totalGap = columnGap * (layout.columnCount - 1).coerceAtLeast(0)
    val availableWidth = (maxWidth - (horizontalInset * 2) - totalGap).coerceAtLeast(80.dp)
    val eventWidth = availableWidth / layout.columnCount.coerceAtLeast(1)
    val xOffset = horizontalInset + ((eventWidth + columnGap) * layout.columnIndex)
    val showDeleteButton = !compact || eventWidth >= 132.dp
    val titleMaxLines = when {
        !compact -> 1
        showDeleteButton -> 2
        else -> 3
    }
    val subtitleMaxLines = when {
        !compact -> 1
        showDeleteButton -> 2
        else -> 3
    }

    Card(
        modifier = Modifier
            .width(eventWidth)
            .offset(x = xOffset, y = topOffset)
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
                        text = renderSpanLabel(span),
                        color = DeepSea4,
                        fontSize = if (compact && !showDeleteButton) 10.sp else if (compact) 9.sp else 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (showDeleteButton) {
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
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eventTitle(span.event),
                    color = DeepSea5,
                    fontSize = if (compact) 11.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (!compact || eventHeight > 68.dp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = eventSubtitle(span.event),
                        color = DeepSea4,
                        fontSize = if (compact) 9.sp else 11.sp,
                        maxLines = subtitleMaxLines,
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
    var planType by remember(initialPlan) {
        mutableStateOf(initialPlan.type.lowercase(Locale.US).ifBlank { "activity" })
    }
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
    val eventTypeOptions = remember {
        listOf(
            EventTypeOption("activity", "Activity"),
            EventTypeOption("restaurant", "Restaurant"),
            EventTypeOption("hotel", "Hotel"),
            EventTypeOption("flight", "Flight")
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

                Text(
                    text = "EVENT TYPE",
                    color = DeepSea4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    eventTypeOptions.forEach { option ->
                        val selected = option.key == planType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) DeepSea3 else DeepSea2)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) DeepSea4 else DeepSea3.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    planType = option.key
                                    colorKey = defaultColorKeyForType(option.key)
                                    validationMessage = null
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = option.label,
                                color = DeepSea5,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
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
                                    type = planType,
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

private fun eventsForDate(
    events: List<TravelEvent>,
    date: String
): List<TravelEvent> {
    return events.filter { eventSpanForDate(it, date) != null }
}

private fun visibleWeekDates(
    allDates: List<String>,
    selectedDate: String
): List<String> {
    if (allDates.isEmpty()) return emptyList()

    val selectedIndex = allDates.indexOf(selectedDate).takeIf { it >= 0 } ?: 0
    val startIndex = (selectedIndex / 7) * 7
    val startDate = parseIsoDate(allDates[startIndex]) ?: return allDates.drop(startIndex).take(7)
    return List(7) { offset -> startDate.plusDays(offset.toLong()).toString() }
}

private fun defaultStartMinutesForDate(
    events: List<TravelEvent>,
    date: String
): Int {
    return eventsForDate(events, date)
        .mapNotNull { eventSpanForDate(it, date)?.startMinutes }
        .minOrNull()
        ?.let { (it - 30).coerceAtLeast(0) }
        ?: 9 * 60
}

private fun buildEventLayouts(
    date: String,
    events: List<TravelEvent>,
    scheduleWindow: ScheduleWindow
): List<EventLayoutInfo> {
    val windowStart = scheduleWindow.startHour * 60
    val windowEnd = scheduleWindow.endHour * 60

    val spans = events.mapNotNull { event ->
        eventSpanForDate(event, date)?.let { span ->
            if (span.endMinutes <= windowStart || span.startMinutes >= windowEnd) {
                null
            } else {
                span.copy(
                    startMinutes = max(span.startMinutes, windowStart),
                    endMinutes = min(span.endMinutes, windowEnd)
                )
            }
        }
    }.sortedWith(
        compareBy<EventRenderSpan>(
            { it.startMinutes },
            { it.endMinutes },
            { eventTitle(it.event) }
        )
    )

    if (spans.isEmpty()) {
        return emptyList()
    }

    val clusters = mutableListOf<MutableList<EventRenderSpan>>()
    var currentCluster = mutableListOf<EventRenderSpan>()
    var currentClusterEnd = -1

    spans.forEach { span ->
        if (currentCluster.isEmpty() || span.startMinutes < currentClusterEnd) {
            currentCluster.add(span)
            currentClusterEnd = max(currentClusterEnd, span.endMinutes)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(span)
            currentClusterEnd = span.endMinutes
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    return clusters.flatMap(::layoutCluster)
}

private fun layoutCluster(cluster: List<EventRenderSpan>): List<EventLayoutInfo> {
    val assignments = linkedMapOf<EventRenderSpan, Int>()
    val active = mutableListOf<Pair<EventRenderSpan, Int>>()
    var maxColumns = 1

    cluster.forEach { span ->
        active.removeAll { (activeSpan, _) -> activeSpan.endMinutes <= span.startMinutes }
        val usedColumns = active.map { it.second }.toSet()
        val nextColumn = generateSequence(0) { it + 1 }.first { it !in usedColumns }
        assignments[span] = nextColumn
        active.add(span to nextColumn)
        maxColumns = max(maxColumns, active.size)
    }

    return cluster.map { span ->
        EventLayoutInfo(
            span = span,
            columnIndex = assignments.getValue(span),
            columnCount = maxColumns
        )
    }
}

private fun eventSpanForDate(
    event: TravelEvent,
    date: String
): EventRenderSpan? {
    val targetDate = parseIsoDate(date) ?: return null
    val (startDateTime, endDateTime) = eventDateTimeRange(event) ?: return null
    val dayStart = targetDate.atStartOfDay()
    val dayEnd = targetDate.plusDays(1).atStartOfDay()

    if (!endDateTime.isAfter(dayStart) || !startDateTime.isBefore(dayEnd)) {
        return null
    }

    val clippedStart = if (startDateTime.isBefore(dayStart)) dayStart else startDateTime
    val clippedEnd = if (endDateTime.isAfter(dayEnd)) dayEnd else endDateTime
    val startMinutes = Duration.between(dayStart, clippedStart).toMinutes().toInt()
    val endMinutes = Duration.between(dayStart, clippedEnd).toMinutes().toInt()

    return EventRenderSpan(
        event = event,
        startMinutes = startMinutes.coerceIn(0, 24 * 60),
        endMinutes = endMinutes.coerceAtLeast(startMinutes + 15).coerceAtMost(24 * 60),
        continuesBefore = startDateTime.isBefore(dayStart),
        continuesAfter = endDateTime.isAfter(dayEnd)
    )
}

private fun eventDateTimeRange(event: TravelEvent): Pair<LocalDateTime, LocalDateTime>? {
    val startDate = parseIsoDate(event.date) ?: return null
    val isAllDay = event.details["is_all_day"]?.equals("true", ignoreCase = true) == true
    val startMinutes = parseTimeToMinutes(event.startTime) ?: if (isAllDay) 0 else 9 * 60
    val startDateTime = startDate.atStartOfDay().plusMinutes(startMinutes.toLong())

    val checkOutDate = event.details["check_out_date"]?.let(::parseIsoDate)
    val baseEndDate = when {
        checkOutDate != null && !checkOutDate.isBefore(startDate) -> checkOutDate
        else -> startDate
    }
    val parsedEndMinutes = parseTimeToMinutes(event.endTime)
    var endDateTime = when {
        isAllDay && baseEndDate.isAfter(startDate) -> baseEndDate.atStartOfDay()
        isAllDay -> startDate.plusDays(1).atStartOfDay()
        parsedEndMinutes != null -> baseEndDate.atStartOfDay().plusMinutes(parsedEndMinutes.toLong())
        else -> startDateTime.plusMinutes(90)
    }

    if (!endDateTime.isAfter(startDateTime)) {
        endDateTime = when {
            baseEndDate.isAfter(startDate) -> {
                baseEndDate.atStartOfDay().plusMinutes((parsedEndMinutes ?: 0).toLong())
            }
            parsedEndMinutes != null -> {
                startDate.plusDays(1).atStartOfDay().plusMinutes(parsedEndMinutes.toLong())
            }
            else -> startDateTime.plusMinutes(90)
        }
    }

    return startDateTime to endDateTime
}

private fun renderSpanLabel(span: EventRenderSpan): String {
    val safeEndMinutes = span.endMinutes.coerceAtMost((23 * 60) + 59)
    return when {
        span.continuesBefore && span.continuesAfter -> "Continues all day"
        span.continuesBefore -> "Until ${formatMinutes(safeEndMinutes)}"
        span.continuesAfter -> "${formatMinutes(span.startMinutes)} onward"
        else -> "${formatMinutes(span.startMinutes)} - ${formatMinutes(safeEndMinutes)}"
    }
}

private fun preferredDayScrollMinutes(
    date: String,
    events: List<TravelEvent>
): Int {
    val todayMinutes = currentTimeMinutes(date)
    val firstEventMinute = events
        .mapNotNull { eventSpanForDate(it, date)?.startMinutes }
        .minOrNull()

    val anchorMinutes = todayMinutes ?: firstEventMinute ?: (8 * 60)
    return (anchorMinutes - 60).coerceIn(0, 22 * 60)
}

private fun currentTimeMinutes(date: String): Int? {
    if (date != todayIsoDate()) return null
    val now = java.time.LocalTime.now()
    return (now.hour * 60) + now.minute
}

private fun buildScheduleWindow(
    events: List<TravelEvent>,
    dates: List<String> = emptyList()
): ScheduleWindow {
    val spans = if (dates.isEmpty()) {
        events.mapNotNull { eventSpanForDate(it, normalizeDate(it.date)) }
    } else {
        dates.flatMap { date ->
            events.mapNotNull { eventSpanForDate(it, date) }
        }
    }

    val startMinutes = spans.map { it.startMinutes }
    val endMinutes = spans.map { it.endMinutes }

    if (startMinutes.isEmpty()) {
        return ScheduleWindow(startHour = 8, endHour = 20)
    }

    val earliest = startMinutes.minOrNull() ?: 8 * 60
    val latest = max(
        endMinutes.maxOrNull() ?: (startMinutes.maxOrNull() ?: 18 * 60) + 90,
        earliest + 120
    )

    val startHour = max(6, (earliest / 60) - 1)
    val endHour = min(24, ceil((latest + 60) / 60f).toInt())

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
