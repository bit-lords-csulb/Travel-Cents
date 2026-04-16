package com.example.travelcents.ui.main.itinerary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.ui.theme.TravelCentsTheme
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val BackgroundColor = Color(0xFF010E24)
private val SurfaceContainerHigh = Color(0xFF0B203D)
private val SurfaceContainerHighest = Color(0xFF102645)
private val PrimaryBlue = Color(0xFF64B5F6)
private val OnSurfaceVariant = Color(0xFF9EABC8)
private val OnSurface = Color(0xFFDBE6FF)
private val OutlineVariant = Color(0xFF3B4861)
private val TertiaryColor = Color(0xFFB5A0FF)
private val ErrorColor = Color(0xFFFF716C)
private val SecondaryColor = Color(0xFFD5E3FB)
private const val UndatedGroupKey = "__undated__"
private const val UndatedHeader = "DATE TBD"

private fun eventTypeColor(type: String): Color = when (type.lowercase()) {
    "flight" -> PrimaryBlue
    "hotel" -> TertiaryColor
    "restaurant", "dining", "food" -> ErrorColor
    else -> SecondaryColor
}

private fun primaryEventTitle(event: TravelEvent): String {
    return when (event.type.lowercase()) {
        "flight" -> listOfNotNull(
            event.details["title"]?.takeIf { it.isNotBlank() },
            event.details["destination_airport"]?.takeIf { it.isNotBlank() }?.let { destination ->
                when (event.details["trip_segment"]?.lowercase()) {
                    "return" -> "Return to $destination"
                    else -> "Flight to $destination"
                }
            },
            listOfNotNull(
                event.details["airline"]?.takeIf { it.isNotBlank() },
                event.details["flight_number"]?.takeIf { it.isNotBlank() }
            ).joinToString(" ").takeIf { it.isNotBlank() }
        ).firstOrNull()

        "hotel" -> event.details["hotel_name"] ?: event.details["name"]
        "restaurant", "dining", "food" -> event.details["restaurant_name"] ?: event.details["name"]
        else -> event.details["activity_name"] ?: event.details["title"] ?: event.details["name"]
    } ?: event.type.replaceFirstChar { it.uppercase() }
}

private fun secondaryEventText(event: TravelEvent): String {
    return when (event.type.lowercase()) {
        "flight" -> listOfNotNull(
            listOfNotNull(
                event.details["airline"]?.takeIf { it.isNotBlank() },
                event.details["flight_number"]?.takeIf { it.isNotBlank() }
            ).joinToString(" ").takeIf { it.isNotBlank() },
            listOfNotNull(
                event.details["origin_airport"]?.takeIf { it.isNotBlank() },
                event.details["destination_airport"]?.takeIf { it.isNotBlank() }
            ).takeIf { it.isNotEmpty() }?.joinToString(" to "),
            event.details["total_price"]?.takeIf { it.isNotBlank() }?.let { "\$$it" },
            event.details["description"]?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        "hotel" -> listOfNotNull(
            event.details["address"]?.takeIf { it.isNotBlank() },
            event.details["rating"]?.takeIf { it.isNotBlank() }?.let { "★$it" }
        ).joinToString(" · ")

        else -> listOfNotNull(
            event.details["notes"]?.takeIf { it.isNotBlank() },
            event.details["description"]?.takeIf { it.isNotBlank() },
            event.details["location"]?.takeIf { it.isNotBlank() },
            event.details["address"]?.takeIf { it.isNotBlank() }
        ).firstOrNull().orEmpty()
    }
}

private fun formatDateHeader(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().also { it.time = date }
        val month = SimpleDateFormat("MMM", Locale.US).format(date).uppercase()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "TH"
            day % 10 == 1 -> "ST"
            day % 10 == 2 -> "ND"
            day % 10 == 3 -> "RD"
            else -> "TH"
        }
        "$month ${day}$suffix"
    } catch (_: Exception) {
        dateStr
    }
}

private fun planGroupKey(event: TravelEvent): String =
    event.date.ifBlank { UndatedGroupKey }

private fun planHeaderLabel(groupKey: String): String =
    if (groupKey == UndatedGroupKey) UndatedHeader else formatDateHeader(groupKey)

private fun planSortDate(event: TravelEvent): String =
    event.date.ifBlank { "9999-12-31" }

// Keyed items so the reorderable library can track positions correctly
private sealed interface FinalPlanItem {
    val key: String
    data class Header(val date: String) : FinalPlanItem { override val key = "header_$date" }
    data class EventItem(val event: TravelEvent, val isLastInDay: Boolean, val dayIndex: Int) : FinalPlanItem { override val key = event.eventId }
    data class DaySpacer(val date: String) : FinalPlanItem { override val key = "spacer_$date" }
}

private fun buildPlanItems(events: List<TravelEvent>): List<FinalPlanItem> {
    val sorted = events
        .sortedWith(compareBy({ planSortDate(it) }, { it.details["sortOrder"]?.toIntOrNull() ?: 0 }, { it.startTime }))
    val grouped = sorted.groupBy(::planGroupKey).entries.sortedBy { entry ->
        if (entry.key == UndatedGroupKey) "9999-12-31" else entry.key
    }
    return buildList {
        grouped.forEach { (date, dayEvents) ->
            add(FinalPlanItem.Header(planHeaderLabel(date)))
            dayEvents.forEachIndexed { idx, event ->
                add(FinalPlanItem.EventItem(event, isLastInDay = idx == dayEvents.lastIndex, dayIndex = idx))
            }
            add(FinalPlanItem.DaySpacer(date))
        }
    }
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun FinalPlanPage(
    viewModel: ItineraryViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventOptions by viewModel.eventOptions.collectAsState()
    val rejectedOptions by viewModel.rejectedOptions.collectAsState()
    val yelpReviews by viewModel.yelpReviews.collectAsState()
    val reviewsLoading by viewModel.reviewsLoading.collectAsState()
    val shareTargets by viewModel.shareTargets.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()

    val planItems = remember(uiState.events) { buildPlanItems(uiState.events) }

    // Which event's options panel is open
    var optionsPanelEventId by remember { mutableStateOf<String?>(null) }
    // Which event card is expanded
    var expandedEventId by remember { mutableStateOf<String?>(null) }
    // Share sheet open
    var showShareSheet by remember { mutableStateOf(false) }
    var shareConfirmation by remember { mutableStateOf<String?>(null) }
    // Jiggle / reorder mode
    var jiggleMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllTrips()
    }

    // Auto-trigger Yelp review fetch when a card is expanded
    LaunchedEffect(expandedEventId) {
        expandedEventId?.let { eid ->
            val event = uiState.events.firstOrNull { it.eventId == eid } ?: return@let
            val yelpId = event.details["yelp_id"] ?: return@let
            viewModel.fetchYelpReviews(yelpId)
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            FinalPlanTopBar(
                tripTitle = uiState.tripTitle,
                currentTripId = uiState.currentTripId,
                allTrips = allTrips,
                onArchiveTrip = { tripId -> viewModel.archiveTrip(tripId) },
                onDeleteTrip = { tripId -> viewModel.deleteTrip(tripId) },
                onBackClick = onBackClick,
                onShareClick = {
                    viewModel.fetchShareTargets()
                    showShareSheet = true
                },
                isReorderActive = jiggleMode,
                onReorderClick = { jiggleMode = !jiggleMode },
                onSwitchTrip = { tripId -> viewModel.loadTrip(tripId) },
                onRenameTrip = { newName -> viewModel.renameTrip(newName) }
            )
        },
        bottomBar = {
            if (jiggleMode) {
                Surface(
                    color = SurfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Drag cards to reorder",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { jiggleMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            uiState.errorMessage != null -> {
                Box(modifier = modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = uiState.errorMessage ?: "An error occurred.", color = ErrorColor, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp))
                }
            }

            uiState.infoMessage != null && uiState.events.isEmpty() -> {
                Box(modifier = modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = uiState.infoMessage ?: "", color = OnSurfaceVariant, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp))
                }
            }

            else -> {
                val lazyListState = rememberLazyListState()
                // Track all dates touched during a drag so we can persist them together on drag end
                var affectedDragDates by remember { mutableStateOf<Set<String>>(emptySet()) }

                val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                val wobbleAngle by infiniteTransition.animateFloat(
                    initialValue = -0.5f,
                    targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
                    label = "wobble"
                )
                val cardRotation = if (jiggleMode) wobbleAngle else 0f

                val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    val fromItem = planItems.firstOrNull { it.key == from.key } as? FinalPlanItem.EventItem
                        ?: return@rememberReorderableLazyListState
                    val toItem = planItems.firstOrNull { it.key == to.key } as? FinalPlanItem.EventItem
                        ?: return@rememberReorderableLazyListState
                    affectedDragDates = affectedDragDates + setOf(fromItem.event.date, toItem.event.date)
                    viewModel.moveEventLocally(
                        eventId = fromItem.event.eventId,
                        fromDate = fromItem.event.date,
                        toDate = toItem.event.date,
                        toIndex = toItem.dayIndex
                    )
                }

                // Persist changed dates and sort order to Firestore when drag ends
                LaunchedEffect(reorderState.isAnyItemDragging) {
                    if (!reorderState.isAnyItemDragging) {
                        if (affectedDragDates.isNotEmpty()) {
                            viewModel.persistEventPlacements(affectedDragDates)
                            affectedDragDates = emptySet()
                        }
                    }
                }

                LazyColumn(
                    modifier = modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(planItems, key = { it.key }) { item ->
                        when (item) {
                            is FinalPlanItem.Header -> DayHeader(item.date)
                            is FinalPlanItem.EventItem -> {
                                val event = item.event
                                val options = eventOptions[event.eventId] ?: emptyList()
                                val rejected = rejectedOptions[event.eventId] ?: emptySet()
                                val activeOptionCount = options.count { it.optionId !in rejected }

                                ReorderableItem(reorderState, key = event.eventId) { isDragging ->
                                    TimelineEventCard(
                                        event = event,
                                        isLast = item.isLastInDay,
                                        hasAlternatives = activeOptionCount > 1,
                                        isDragging = isDragging,
                                        jiggleMode = jiggleMode,
                                        wobbleAngle = cardRotation,
                                        modifier = Modifier.draggableHandle(enabled = jiggleMode),
                                        onCardClick = { expandedEventId = event.eventId },
                                        onChangeClick = { optionsPanelEventId = event.eventId }
                                    )
                                }
                            }
                            is FinalPlanItem.DaySpacer -> Spacer(modifier = Modifier.height(0.dp))
                        }
                    }
                }
            }
        }

        // Share confirmation snackbar
        shareConfirmation?.let { msg ->
            LaunchedEffect(msg) {
                delay(2500)
                shareConfirmation = null
            }
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter) {
                Surface(
                    color = SurfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
                ) {
                    Text(text = msg, color = OnSurface, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
            }
        }
    }

    // Options panel (bottom sheet)
    optionsPanelEventId?.let { eid ->
        val event = uiState.events.firstOrNull { it.eventId == eid }
        val options = eventOptions[eid] ?: emptyList()
        val rejected = rejectedOptions[eid] ?: emptySet()
        if (event != null) {
            // Collect names of options already selected as primaries on other same-type event slots
            val selectedNamesElsewhere = uiState.events
                .filter { it.eventId != eid && it.type.equals(event.type, ignoreCase = true) }
                .mapNotNull { other ->
                    (eventOptions[other.eventId] ?: emptyList()).firstOrNull { it.selected }
                        ?.let { opt ->
                            when (other.type.lowercase()) {
                                "hotel" -> opt.details["hotel_name"] ?: opt.details["name"]
                                "restaurant", "dining", "food" -> opt.details["restaurant_name"] ?: opt.details["name"]
                                "flight" -> null // flights are per-day/route, skip dedup
                                else -> opt.details["activity_name"] ?: opt.details["title"] ?: opt.details["name"]
                            }
                        }
                }
                .toSet()
            EventOptionsPanel(
                event = event,
                options = options,
                rejectedIds = rejected,
                onSelect = { optId -> viewModel.selectOption(eid, optId) },
                onReject = { optId -> viewModel.rejectOption(eid, optId) },
                onDismiss = { optionsPanelEventId = null },
                selectedNamesElsewhere = selectedNamesElsewhere
            )
        }
    }

    // Expanded event card dialog
    expandedEventId?.let { eid ->
        val event = uiState.events.firstOrNull { it.eventId == eid }
        if (event != null) {
            val yelpId = event.details["yelp_id"] ?: ""
            val reviews = yelpReviews[yelpId] ?: emptyList()
            val loadingReviews = reviewsLoading.contains(yelpId)
            ExpandedEventCard(
                event = event,
                yelpReviews = reviews,
                isLoadingReviews = loadingReviews,
                onChangeClick = {
                    expandedEventId = null
                    optionsPanelEventId = eid
                },
                onDismiss = { expandedEventId = null },
                onSaveEdits = { title, time, notes ->
                    viewModel.patchEventFields(eid, title, time, notes)
                }
            )
        }
    }

    // Share bottom sheet
    if (showShareSheet) {
        ShareTripSheet(
            targets = shareTargets,
            onShare = { target ->
                viewModel.shareTripToChat(target)
                showShareSheet = false
                shareConfirmation = "Trip shared to ${target.name}"
            },
            onDismiss = { showShareSheet = false }
        )
    }
}

// ── Share bottom sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareTripSheet(
    targets: List<ShareTarget>,
    onShare: (ShareTarget) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "Share Trip",
                color = OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
            if (targets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No chats found. Start a conversation first.", color = OnSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                targets.forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShare(target) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar initial
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.name.take(1).uppercase(),
                                color = PrimaryBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = target.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (target.isGroup) "Group" else "Direct message",
                                color = OnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
private fun FinalPlanTopBar(
    tripTitle: String,
    currentTripId: String?,
    allTrips: List<Itinerary>,
    onArchiveTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    isReorderActive: Boolean = false,
    onReorderClick: () -> Unit = {},
    onSwitchTrip: (String) -> Unit = {},
    onRenameTrip: (String) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var switcherExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf(tripTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    // Sync local title when the VM swaps to a different trip
    LaunchedEffect(tripTitle) {
        editableTitle = tripTitle
        isEditingTitle = false
    }

    // Request focus as soon as edit mode activates
    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) titleFocusRequester.requestFocus()
    }

    if (showDeleteDialog && currentTripId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete trip?", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text("\"$tripTitle\" and all its events will be permanently deleted.",
                    color = OnSurfaceVariant, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTrip(currentTripId)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF02132B))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Trip actions",
                        tint = OnSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF0B203D))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Share Trip", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = OnSurfaceVariant)
                        },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isReorderActive) "Done Reordering" else "Reorder Events",
                                color = OnSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DragHandle, contentDescription = null, tint = OnSurfaceVariant)
                        },
                        onClick = { menuExpanded = false; onReorderClick() }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Archive Trip", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Archive, contentDescription = null, tint = OnSurfaceVariant)
                        },
                        onClick = {
                            currentTripId?.let { tripId -> onArchiveTrip(tripId) }
                            menuExpanded = false
                        },
                        enabled = currentTripId != null
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Delete Trip", color = ErrorColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = ErrorColor.copy(alpha = 0.85f))
                        },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                        enabled = currentTripId != null
                    )
                }
            }
        }

        // Editable title + trip switcher arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditingTitle) {
                BasicTextField(
                    value = editableTitle,
                    onValueChange = { new ->
                        // block newlines and tabs; only accept printable input
                        editableTitle = new.filter { it != '\n' && it != '\t' }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = OnSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    cursorBrush = SolidColor(PrimaryBlue),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(titleFocusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && isEditingTitle) {
                                isEditingTitle = false
                                onRenameTrip(editableTitle)
                            }
                        }
                )
            } else {
                Text(
                    text = editableTitle,
                    color = OnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isEditingTitle = true }
                )
            }

            // Trip switcher
            Box {
                IconButton(
                    onClick = { switcherExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Switch trip",
                        tint = OnSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = switcherExpanded,
                    onDismissRequest = { switcherExpanded = false },
                    modifier = Modifier
                        .background(Color(0xFF0B203D))
                        .widthIn(min = 200.dp)
                ) {
                    allTrips.forEach { trip ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = trip.tripName,
                                    color = if (trip.itineraryId == currentTripId) PrimaryBlue else OnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = if (trip.itineraryId == currentTripId) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                switcherExpanded = false
                                if (trip.itineraryId != currentTripId) {
                                    onSwitchTrip(trip.itineraryId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = date, color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(OutlineVariant.copy(alpha = 0.3f)))
    }
}

@Composable
private fun TimelineEventCard(
    event: TravelEvent,
    isLast: Boolean,
    hasAlternatives: Boolean,
    isDragging: Boolean,
    jiggleMode: Boolean,
    wobbleAngle: Float,
    modifier: Modifier,
    onCardClick: () -> Unit,
    onChangeClick: () -> Unit
) {
    val typeColor = eventTypeColor(event.type)
    val title = primaryEventTitle(event)
    val description = secondaryEventText(event)
    val imageUrl = event.imageUrl.ifBlank { event.details["imageUrl"] ?: event.details["image_url"] ?: "" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline dot and line
        Box(
            modifier = Modifier.width(40.dp).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Canvas(
                    modifier = Modifier.fillMaxHeight().width(1.dp).padding(top = 12.dp)
                ) {
                    drawLine(
                        color = OutlineVariant.copy(alpha = 0.4f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(24.dp)
                    .background(SurfaceContainerHighest, CircleShape)
                    .border(2.dp, PrimaryBlue.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
                .graphicsLayer { rotationZ = wobbleAngle }
                .clickable(enabled = !jiggleMode) { onCardClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDragging) SurfaceContainerHigh else SurfaceContainerHighest
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDragging) PrimaryBlue.copy(alpha = 0.3f) else OutlineVariant.copy(alpha = 0.05f)
            )
        ) {
            Box {
                Row(modifier = Modifier.height(120.dp)) {
                    // Thumbnail
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(SurfaceContainerHigh,
                                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = event.type.take(1).uppercase(), color = typeColor,
                                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(2f).padding(12.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = typeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(100.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.2f))
                            ) {
                                Text(text = event.type.uppercase(), color = typeColor, fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            if (event.startTime.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = event.startTime, color = OnSurfaceVariant, fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = description, color = OnSurfaceVariant, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, lineHeight = 16.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (hasAlternatives) {
                    Surface(
                        color = SurfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .clickable(enabled = !jiggleMode) { onChangeClick() }
                    ) {
                        Text(
                            text = "Change",
                            color = OnSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Drag handle — visible icon in jiggle mode, invisible touch target otherwise
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .then(modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (jiggleMode) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = OnSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Default", showBackground = true)
@Preview(name = "Pixel 7", device = Devices.PIXEL_7, showSystemUi = true)
@Composable
fun FinalPlanPreview() {
    TravelCentsTheme(dynamicColor = false) {
        FinalPlanPage(viewModel = viewModel())
    }
}
