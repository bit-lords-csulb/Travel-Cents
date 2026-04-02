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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
                event.details["origin_airport"]?.takeIf { it.isNotBlank() },
                event.details["destination_airport"]?.takeIf { it.isNotBlank() }
            ).takeIf { it.isNotEmpty() }?.joinToString(" to "),
            event.details["total_price"]?.takeIf { it.isNotBlank() }?.let { "\$$it" }
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

// Keyed items so the reorderable library can track positions correctly
private sealed interface FinalPlanItem {
    val key: String
    data class Header(val date: String) : FinalPlanItem { override val key = "header_$date" }
    data class EventItem(val event: TravelEvent, val isLastInDay: Boolean, val dayIndex: Int) : FinalPlanItem { override val key = event.eventId }
    data class DaySpacer(val date: String) : FinalPlanItem { override val key = "spacer_$date" }
}

private fun buildPlanItems(events: List<TravelEvent>): List<FinalPlanItem> {
    val sorted = events
        .filter { it.date.isNotEmpty() }
        .sortedWith(compareBy({ it.date }, { it.details["sortOrder"]?.toIntOrNull() ?: 0 }, { it.startTime }))
    val grouped = sorted.groupBy { it.date }.entries.sortedBy { it.key }
    return buildList {
        grouped.forEach { (date, dayEvents) ->
            add(FinalPlanItem.Header(formatDateHeader(date)))
            dayEvents.forEachIndexed { idx, event ->
                add(FinalPlanItem.EventItem(event, isLastInDay = idx == dayEvents.lastIndex, dayIndex = idx))
            }
            add(FinalPlanItem.DaySpacer(date))
        }
    }
}

@Composable
fun FinalPlanPage(
    viewModel: ItineraryViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()
    val archivedTrips by viewModel.archivedTrips.collectAsState()
    val eventOptions by viewModel.eventOptions.collectAsState()
    val rejectedOptions by viewModel.rejectedOptions.collectAsState()
    val yelpReviews by viewModel.yelpReviews.collectAsState()
    val reviewsLoading by viewModel.reviewsLoading.collectAsState()
    val shareTargets by viewModel.shareTargets.collectAsState()

    val planItems = remember(uiState.events) { buildPlanItems(uiState.events) }

    // Which event's options panel is open
    var optionsPanelEventId by remember { mutableStateOf<String?>(null) }
    // Which event card is expanded
    var expandedEventId by remember { mutableStateOf<String?>(null) }
    // Share sheet open
    var showShareSheet by remember { mutableStateOf(false) }
    var shareConfirmation by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTrip()
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
                allTrips = allTrips,
                archivedTrips = archivedTrips,
                currentTripId = uiState.currentTripId,
                onTripSelect = { tripId -> viewModel.loadTrip(tripId) },
                onArchiveTrip = { tripId -> viewModel.archiveTrip(tripId) },
                onRestoreTrip = { tripId -> viewModel.restoreTrip(tripId) },
                onDeleteTrip = { tripId -> viewModel.deleteTrip(tripId) },
                onBackClick = onBackClick,
                onShareClick = {
                    viewModel.fetchShareTargets()
                    showShareSheet = true
                }
            )
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
                // Track last dragged date so we can persist on drag end
                var lastDraggedDate by remember { mutableStateOf<String?>(null) }

                val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    val fromItem = planItems.getOrNull(from.index) as? FinalPlanItem.EventItem ?: return@rememberReorderableLazyListState
                    val toItem = planItems.getOrNull(to.index) as? FinalPlanItem.EventItem ?: return@rememberReorderableLazyListState
                    if (fromItem.event.date == toItem.event.date) {
                        lastDraggedDate = fromItem.event.date
                        viewModel.reorderEventsLocally(fromItem.event.date, fromItem.dayIndex, toItem.dayIndex)
                    }
                }

                // Persist sort order to Firestore when drag ends
                LaunchedEffect(reorderState.isAnyItemDragging) {
                    if (!reorderState.isAnyItemDragging) {
                        lastDraggedDate?.let { date ->
                            viewModel.persistEventOrder(date)
                            lastDraggedDate = null
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
                                        dragHandleModifier = Modifier.longPressDraggableHandle(),
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
                kotlinx.coroutines.delay(2500)
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
            EventOptionsPanel(
                event = event,
                options = options,
                rejectedIds = rejected,
                onSelect = { optId -> viewModel.selectOption(eid, optId) },
                onReject = { optId -> viewModel.rejectOption(eid, optId) },
                onDismiss = { optionsPanelEventId = null }
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

@Composable
private fun FinalPlanTopBar(
    tripTitle: String,
    allTrips: List<TripSummary>,
    archivedTrips: List<TripSummary>,
    currentTripId: String?,
    onTripSelect: (String) -> Unit,
    onArchiveTrip: (String) -> Unit,
    onRestoreTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<TripSummary?>(null) }

    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete trip?", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${trip.tripName}\" and all its events will be permanently deleted.",
                    color = OnSurfaceVariant, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = { onDeleteTrip(trip.tripId); tripToDelete = null }) {
                    Text("Delete", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF02132B))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
        }

        val hasMultiple = allTrips.size > 1 || archivedTrips.isNotEmpty()
        Box(contentAlignment = Alignment.TopCenter) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = hasMultiple) { dropdownExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tripTitle,
                    color = OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                if (hasMultiple) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch trip",
                        tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(Color(0xFF0B203D))
            ) {
                allTrips.forEach { trip ->
                    val isSelected = trip.tripId == currentTripId
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(text = trip.tripName,
                                    color = if (isSelected) PrimaryBlue else OnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                if (trip.destination.isNotEmpty()) {
                                    Text(text = "${trip.destination}  •  ${trip.dateFrom}",
                                        color = OnSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { dropdownExpanded = false; onArchiveTrip(trip.tripId) },
                                    modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Archive, contentDescription = "Archive",
                                        tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { dropdownExpanded = false; tripToDelete = trip },
                                    modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete",
                                        tint = ErrorColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = { dropdownExpanded = false; if (!isSelected) onTripSelect(trip.tripId) },
                        modifier = Modifier.background(if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent)
                    )
                }

                if (archivedTrips.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = OutlineVariant.copy(alpha = 0.4f))
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(text = "ARCHIVED", color = OnSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    archivedTrips.forEach { trip ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(text = trip.tripName, color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    if (trip.destination.isNotEmpty()) {
                                        Text(text = "${trip.destination}  •  ${trip.dateFrom}",
                                            color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            },
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { dropdownExpanded = false; onRestoreTrip(trip.tripId) },
                                        modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.Unarchive, contentDescription = "Restore",
                                            tint = PrimaryBlue.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { dropdownExpanded = false; tripToDelete = trip },
                                        modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete",
                                            tint = ErrorColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            onClick = {}
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share trip", tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile",
                tint = OnSurfaceVariant, modifier = Modifier.padding(end = 8.dp).size(36.dp))
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
    dragHandleModifier: Modifier,
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
                .clickable { onCardClick() },
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
                            .clickable { onChangeClick() }
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

                // Long-press drag handle (invisible area, scope-bound modifier passed in)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .then(dragHandleModifier)
                )
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
