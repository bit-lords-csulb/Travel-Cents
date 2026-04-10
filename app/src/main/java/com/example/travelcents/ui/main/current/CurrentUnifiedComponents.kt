package com.example.travelcents.ui.main.current

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.YelpReview
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatTimeZoneLabel
import com.example.travelcents.ui.modules.parseFlexibleTime
import com.example.travelcents.ui.modules.parseIsoDate
import com.example.travelcents.ui.modules.plusMinutes
import com.example.travelcents.ui.modules.todayIsoDate
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.normalizeTime
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val UnifiedUndatedGroupKey = "__undated__"
private const val UnifiedUndatedHeader = "DATE TBD"

private sealed interface UnifiedPlanItem {
    val key: String

    data class Header(val date: String) : UnifiedPlanItem { override val key = "header_$date" }
    data class EventItem(val event: TravelEvent, val isLastInDay: Boolean, val dayIndex: Int) : UnifiedPlanItem {
        override val key = event.eventId
    }
    data class DaySpacer(val date: String) : UnifiedPlanItem { override val key = "spacer_$date" }
}

@Composable
fun UnifiedTripHeader(
    tripTitle: String,
    dateRange: String,
    currentTripId: String?,
    allTrips: List<Itinerary>,
    canAdd: Boolean,
    isReorderActive: Boolean,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleReorder: () -> Unit,
    onArchiveTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onSwitchTrip: (String) -> Unit,
    onRenameTrip: (String) -> Unit,
    controlsContent: @Composable () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var switcherExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf(tripTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(tripTitle) {
        editableTitle = tripTitle
        isEditingTitle = false
    }

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) titleFocusRequester.requestFocus()
    }

    if (showDeleteDialog && currentTripId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DeepSea2,
            title = { Text("Delete trip?", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "\"$tripTitle\" and all its events will be permanently deleted.",
                    color = DeepSea4,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTrip(currentTripId)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFE77D90), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = DeepSea4)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditingTitle) {
                        BasicTextField(
                            value = editableTitle,
                            onValueChange = { new -> editableTitle = new.filter { it != '\n' && it != '\t' } },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = DeepSea5,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            cursorBrush = SolidColor(DeepSea5),
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
                            color = DeepSea5,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEditingTitle = true }
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { switcherExpanded = true },
                            enabled = allTrips.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch trip",
                                tint = if (allTrips.isNotEmpty()) DeepSea4 else DeepSea4.copy(alpha = 0.35f)
                            )
                        }

                        DropdownMenu(
                            expanded = switcherExpanded,
                            onDismissRequest = { switcherExpanded = false },
                            modifier = Modifier
                                .background(DeepSea2)
                                .widthIn(min = 220.dp)
                        ) {
                            allTrips.forEach { trip ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = trip.tripName,
                                            color = if (trip.itineraryId == currentTripId) DeepSea5 else DeepSea4,
                                            fontWeight = if (trip.itineraryId == currentTripId) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        switcherExpanded = false
                                        if (trip.itineraryId != currentTripId) onSwitchTrip(trip.itineraryId)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateRange,
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (canAdd) DeepSea3 else DeepSea3.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(enabled = canAdd, onClick = onAddClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add plan",
                            tint = if (canAdd) DeepSea5 else DeepSea5.copy(alpha = 0.45f)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Trip actions",
                            tint = DeepSea4
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(DeepSea2)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = DeepSea4) },
                            onClick = {
                                menuExpanded = false
                                onShareClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isReorderActive) "Done Reordering" else "Reorder Events",
                                    color = DeepSea5,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.DragHandle, contentDescription = null, tint = DeepSea4) },
                            onClick = {
                                menuExpanded = false
                                onToggleReorder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null, tint = DeepSea4) },
                            onClick = {
                                currentTripId?.let(onArchiveTrip)
                                menuExpanded = false
                            },
                            enabled = currentTripId != null
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Trip", color = Color(0xFFE77D90), fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFE77D90)) },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            },
                            enabled = currentTripId != null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        controlsContent()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UnifiedItineraryContent(
    events: List<TravelEvent>,
    eventOptions: Map<String, List<EventOption>>,
    rejectedOptions: Map<String, Set<String>>,
    jiggleMode: Boolean,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onOpenAlternatives: (String) -> Unit,
    onMoveEvent: (eventId: String, fromDate: String, toDate: String, toIndex: Int) -> Unit,
    onPersistEventPlacements: (Set<String>) -> Unit
) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Tap the + button to create your first reservation or activity.",
                color = DeepSea4,
                fontSize = 14.sp
            )
        }
        return
    }

    val planItems = remember(events) { buildUnifiedPlanItems(events) }
    val lazyListState = rememberLazyListState()
    var affectedDragDates by remember { mutableStateOf<Set<String>>(emptySet()) }

    val infiniteTransition = rememberInfiniteTransition(label = "unified_jiggle")
    val wobbleAngle by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
        label = "unified_wobble"
    )
    val cardRotation = if (jiggleMode) wobbleAngle else 0f

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromItem = planItems.firstOrNull { it.key == from.key } as? UnifiedPlanItem.EventItem
            ?: return@rememberReorderableLazyListState
        val toItem = planItems.firstOrNull { it.key == to.key } as? UnifiedPlanItem.EventItem
            ?: return@rememberReorderableLazyListState
        affectedDragDates = affectedDragDates + setOf(fromItem.event.date, toItem.event.date)
        onMoveEvent(fromItem.event.eventId, fromItem.event.date, toItem.event.date, toItem.dayIndex)
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && affectedDragDates.isNotEmpty()) {
            onPersistEventPlacements(affectedDragDates)
            affectedDragDates = emptySet()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (jiggleMode) {
            item {
                Surface(
                    color = DeepSea2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = DeepSea4,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Drag cards to reorder events.",
                            color = DeepSea4,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        items(planItems, key = { it.key }) { item ->
            when (item) {
                is UnifiedPlanItem.Header -> UnifiedDayHeader(item.date)
                is UnifiedPlanItem.EventItem -> {
                    val options = eventOptions[item.event.eventId].orEmpty()
                    val rejected = rejectedOptions[item.event.eventId].orEmpty()
                    val activeOptionCount = options.count { it.optionId !in rejected }
                    ReorderableItem(reorderState, key = item.event.eventId) { isDragging ->
                        UnifiedTimelineEventCard(
                            event = item.event,
                            isLast = item.isLastInDay,
                            hasAlternatives = activeOptionCount > 1,
                            isDragging = isDragging,
                            jiggleMode = jiggleMode,
                            wobbleAngle = cardRotation,
                            modifier = Modifier.draggableHandle(enabled = jiggleMode),
                            onCardClick = { onEventClick(item.event) },
                            onDeleteClick = { onDeleteClick(item.event) },
                            onAlternativesClick = { onOpenAlternatives(item.event.eventId) }
                        )
                    }
                }

                is UnifiedPlanItem.DaySpacer -> Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun UnifiedDayHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            color = DeepSea5,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DeepSea3.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun UnifiedTimelineEventCard(
    event: TravelEvent,
    isLast: Boolean,
    hasAlternatives: Boolean,
    isDragging: Boolean,
    jiggleMode: Boolean,
    wobbleAngle: Float,
    modifier: Modifier,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAlternativesClick: () -> Unit
) {
    val accent = unifiedEventTypeColor(event.type)
    val title = unifiedPrimaryEventTitle(event)
    val description = unifiedSecondaryEventText(event).ifBlank { "Tap to edit details" }
    val imageUrl = event.imageUrl.ifBlank { event.details["imageUrl"] ?: event.details["image_url"] ?: "" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(top = 12.dp)
                ) {
                    drawLine(
                        color = DeepSea3.copy(alpha = 0.4f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(22.dp)
                    .background(DeepSea2, CircleShape)
                    .border(2.dp, accent.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(8.dp).background(accent, CircleShape))
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
                .graphicsLayer { rotationZ = wobbleAngle }
                .clickable(enabled = !jiggleMode, onClick = onCardClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDragging) DeepSea2 else DeepSea1),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDragging) accent.copy(alpha = 0.35f) else DeepSea3.copy(alpha = 0.45f)
            )
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 116.dp)
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .width(116.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(116.dp)
                                .fillMaxHeight()
                                .background(DeepSea2, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = event.type.take(1).uppercase(Locale.US),
                                color = accent,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = accent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = event.type.uppercase(Locale.US),
                                    color = accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (event.startTime.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatDisplayTime(event.startTime),
                                    color = DeepSea4,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = title,
                            color = DeepSea5,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            color = DeepSea4,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasAlternatives) {
                        Surface(
                            color = DeepSea2,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = !jiggleMode, onClick = onAlternativesClick)
                        ) {
                            Text(
                                text = "Change",
                                color = DeepSea5,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        color = DeepSea2,
                        shape = CircleShape,
                        modifier = Modifier.clickable(enabled = !jiggleMode, onClick = onDeleteClick)
                    ) {
                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete plan",
                                tint = Color(0xFFE77D90),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp)
                        .size(28.dp)
                        .then(modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (jiggleMode) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = DeepSea4.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildUnifiedPlanItems(events: List<TravelEvent>): List<UnifiedPlanItem> {
    val sorted = events.sortedWith(
        compareBy(
            { it.date.ifBlank { "9999-12-31" } },
            { it.details["sortOrder"]?.toIntOrNull() ?: 0 },
            { normalizeTime(it.startTime) }
        )
    )
    val grouped = sorted.groupBy { it.date.ifBlank { UnifiedUndatedGroupKey } }
        .entries
        .sortedBy { if (it.key == UnifiedUndatedGroupKey) "9999-12-31" else it.key }

    return buildList {
        grouped.forEach { (date, dayEvents) ->
            add(UnifiedPlanItem.Header(unifiedPlanHeaderLabel(date)))
            dayEvents.forEachIndexed { idx, event ->
                add(UnifiedPlanItem.EventItem(event, isLastInDay = idx == dayEvents.lastIndex, dayIndex = idx))
            }
            add(UnifiedPlanItem.DaySpacer(date))
        }
    }
}

private fun unifiedPlanHeaderLabel(groupKey: String): String {
    return if (groupKey == UnifiedUndatedGroupKey) UnifiedUndatedHeader else unifiedFormatDateHeader(groupKey)
}

private fun unifiedFormatDateHeader(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().also { it.time = date }
        val month = SimpleDateFormat("MMM", Locale.US).format(date).uppercase(Locale.US)
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

private fun unifiedEventTypeColor(type: String): Color = when (type.lowercase(Locale.US)) {
    "flight" -> Color(0xFF64B5F6)
    "hotel" -> Color(0xFFB5A0FF)
    "restaurant", "dining", "food" -> Color(0xFFFF716C)
    else -> Color(0xFFD5E3FB)
}

private fun unifiedPrimaryEventTitle(event: TravelEvent): String {
    return when (event.type.lowercase(Locale.US)) {
        "flight" -> listOfNotNull(
            event.details["title"]?.takeIf { it.isNotBlank() },
            event.details["destination_airport"]?.takeIf { it.isNotBlank() }?.let { destination ->
                when (event.details["trip_segment"]?.lowercase(Locale.US)) {
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
    } ?: event.type.replaceFirstChar { it.uppercase(Locale.US) }
}

private fun unifiedSecondaryEventText(event: TravelEvent): String {
    return when (event.type.lowercase(Locale.US)) {
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

@Composable
fun UnifiedPlanEditorDialog(
    initialPlan: EditablePlan,
    currentOptions: List<EventOption>,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (EditablePlan) -> Unit,
    onDelete: (EditablePlan) -> Unit,
    onAlternatives: () -> Unit
) {
    val context = LocalContext.current
    var planType by remember(initialPlan) { mutableStateOf(initialPlan.type.lowercase(Locale.US).ifBlank { "activity" }) }
    var title by remember(initialPlan) { mutableStateOf(initialPlan.title) }
    var date by remember(initialPlan) { mutableStateOf(normalizeDate(initialPlan.date)) }
    var time by remember(initialPlan) { mutableStateOf(formatDisplayTime(initialPlan.startTime)) }
    var endTime by remember(initialPlan) {
        mutableStateOf(initialPlan.endTime.takeIf { it.isNotBlank() }?.let(::formatDisplayTime).orEmpty())
    }
    var location by remember(initialPlan) { mutableStateOf(initialPlan.location) }
    var notes by remember(initialPlan) { mutableStateOf(initialPlan.notes) }
    var colorKey by remember(initialPlan) { mutableStateOf(initialPlan.colorKey.ifBlank { "rose" }) }
    val timeZoneId = remember(initialPlan) { initialPlan.timeZoneId.ifBlank { defaultPlanTimeZoneId() } }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var reviewsExpanded by remember { mutableStateOf(false) }

    var airline by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["airline"].orEmpty()) }
    var flightNumber by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["flight_number"].orEmpty()) }
    var originAirport by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["origin_airport"].orEmpty()) }
    var destinationAirport by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["destination_airport"].orEmpty()) }
    var totalPrice by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["total_price"].orEmpty()) }
    var tripSegment by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["trip_segment"].orEmpty()) }
    var hotelName by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["hotel_name"].orEmpty()) }
    var hotelAddress by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["address"].orEmpty()) }
    var hotelRating by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["rating"].orEmpty()) }
    var checkIn by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["check_in"].orEmpty()) }
    var checkOut by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["check_out"].orEmpty()) }
    var restaurantName by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["restaurant_name"].orEmpty()) }
    var cuisine by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["cuisine"].orEmpty()) }

    val canShowAlternatives = initialPlan.eventId != null && currentOptions.size > 1
    val colorOptions = remember {
        listOf(
            "rose" to Color(0xFFFF677C),
            "teal" to Color(0xFF4CA7C5),
            "olive" to Color(0xFF8A9365),
            "plum" to Color(0xFF5A2A7B),
            "lavender" to Color(0xFF6D5D8E),
            "cyan" to Color(0xFF268C95)
        )
    }
    val eventTypeOptions = remember {
        listOf(
            "activity" to "Activity",
            "restaurant" to "Restaurant",
            "hotel" to "Hotel",
            "flight" to "Flight"
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
                    TextButton(onClick = onDismiss) { Text("Close", color = DeepSea4) }
                    Text(
                        text = if (initialPlan.eventId == null) "ADD PLAN" else "EDIT PLAN",
                        color = DeepSea5,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )
                    Spacer(modifier = Modifier.width(48.dp))
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
                    UnifiedPickerField(
                        modifier = Modifier.weight(1f),
                        label = "Date",
                        value = date,
                        placeholder = "YYYY-MM-DD",
                        icon = Icons.Default.CalendarToday,
                        onClick = {
                            showUnifiedDatePicker(context, date) {
                                date = it
                                validationMessage = null
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnifiedPickerField(
                        modifier = Modifier.weight(1f),
                        label = "Start Time",
                        value = time,
                        placeholder = "Select time",
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            showUnifiedTimePicker(context, time) {
                                time = it
                                validationMessage = null
                            }
                        }
                    )
                    UnifiedPickerField(
                        modifier = Modifier.weight(1f),
                        label = "End Time",
                        value = endTime,
                        placeholder = "Optional",
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            showUnifiedTimePicker(context, endTime.ifBlank { plusMinutes(time, 60) }) {
                                endTime = it
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("TIME ZONE", color = DeepSea4, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
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

                when (planType) {
                    "flight" -> {
                        UnifiedDetailFields(
                            textFieldColors = textFieldColors,
                            fields = listOf(
                                DetailFieldState("Airline", airline, { airline = it }, "Delta"),
                                DetailFieldState("Flight Number", flightNumber, { flightNumber = it }, "DL 123"),
                                DetailFieldState("Origin Airport", originAirport, { originAirport = it }, "LAX"),
                                DetailFieldState("Destination Airport", destinationAirport, { destinationAirport = it }, "JFK"),
                                DetailFieldState("Total Price", totalPrice, { totalPrice = it }, "399"),
                                DetailFieldState("Trip Segment", tripSegment, { tripSegment = it }, "outbound / return")
                            )
                        )
                    }
                    "hotel" -> {
                        UnifiedDetailFields(
                            textFieldColors = textFieldColors,
                            fields = listOf(
                                DetailFieldState("Hotel Name", hotelName, { hotelName = it }, "The Carlyle"),
                                DetailFieldState("Address", hotelAddress, { hotelAddress = it }, "35 E 76th St"),
                                DetailFieldState("Rating", hotelRating, { hotelRating = it }, "4.7"),
                                DetailFieldState("Check-In", checkIn, { checkIn = it }, "3:00 PM"),
                                DetailFieldState("Check-Out", checkOut, { checkOut = it }, "11:00 AM")
                            )
                        )
                    }
                    "restaurant", "dining", "food" -> {
                        UnifiedDetailFields(
                            textFieldColors = textFieldColors,
                            fields = listOf(
                                DetailFieldState("Restaurant Name", restaurantName, { restaurantName = it }, "Via Carota"),
                                DetailFieldState("Cuisine", cuisine, { cuisine = it }, "Italian")
                            )
                        )
                    }
                }

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
                Text("LABEL COLOR", color = DeepSea4, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { (key, color) ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (key == colorKey) 2.dp else 1.dp,
                                    color = if (key == colorKey) DeepSea5 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorKey = key }
                        )
                    }
                }

                if (initialPlan.existingDetails["yelp_id"].orEmpty().isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    UnifiedReviewsSection(
                        reviews = yelpReviews,
                        loading = reviewsLoading,
                        expanded = reviewsExpanded,
                        onToggle = { reviewsExpanded = !reviewsExpanded }
                    )
                }

                validationMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = message, color = Color(0xFFFFB4C7), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canShowAlternatives) {
                        Button(
                            onClick = onAlternatives,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSea2, contentColor = DeepSea5)
                        ) {
                            Text("Alternatives", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (initialPlan.eventId != null) {
                        Button(
                            onClick = { onDelete(initialPlan) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B2637),
                                contentColor = Color(0xFFFFB4C7)
                            )
                        ) {
                            Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            when {
                                title.isBlank() -> validationMessage = "A title is required."
                                date.isBlank() -> validationMessage = "Select a date for this plan."
                                time.isBlank() -> validationMessage = "Select a time for this plan."
                                else -> {
                                    val mergedDetails = initialPlan.existingDetails.toMutableMap().apply {
                                        applyUnifiedDetail("airline", airline, planType == "flight")
                                        applyUnifiedDetail("flight_number", flightNumber, planType == "flight")
                                        applyUnifiedDetail("origin_airport", originAirport, planType == "flight")
                                        applyUnifiedDetail("destination_airport", destinationAirport, planType == "flight")
                                        applyUnifiedDetail("total_price", totalPrice, planType == "flight")
                                        applyUnifiedDetail("trip_segment", tripSegment, planType == "flight")
                                        applyUnifiedDetail("hotel_name", hotelName, planType == "hotel")
                                        applyUnifiedDetail("address", hotelAddress, planType == "hotel")
                                        applyUnifiedDetail("rating", hotelRating, planType == "hotel")
                                        applyUnifiedDetail("check_in", checkIn, planType == "hotel")
                                        applyUnifiedDetail("check_out", checkOut, planType == "hotel")
                                        applyUnifiedDetail("restaurant_name", restaurantName.ifBlank { title.trim() }, planType == "restaurant" || planType == "dining" || planType == "food")
                                        applyUnifiedDetail("cuisine", cuisine, planType == "restaurant" || planType == "dining" || planType == "food")
                                        applyUnifiedDetail("activity_name", title.trim(), planType == "activity")
                                    }
                                    onSave(
                                        initialPlan.copy(
                                            type = planType,
                                            title = title.trim(),
                                            date = date,
                                            startTime = time,
                                            endTime = endTime,
                                            timeZoneId = timeZoneId,
                                            location = location.trim(),
                                            notes = notes.trim(),
                                            colorKey = colorKey,
                                            existingDetails = mergedDetails
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSea3, contentColor = DeepSea5)
                    ) {
                        Text(
                            text = if (initialPlan.eventId == null) "Save" else "Update",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedShareTripSheet(
    targets: List<ShareTarget>,
    onShare: (ShareTarget) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepSea2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Share Trip",
                color = DeepSea5,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            HorizontalDivider(color = DeepSea3.copy(alpha = 0.3f))
            if (targets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No chats found. Start a conversation first.", color = DeepSea4, fontSize = 13.sp)
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DeepSea3.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.name.take(1).uppercase(Locale.US),
                                color = DeepSea5,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = target.name, color = DeepSea5, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (target.isGroup) "Group" else "Direct message",
                                color = DeepSea4,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DetailFieldState(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val placeholder: String
)

@Composable
private fun UnifiedDetailFields(
    textFieldColors: androidx.compose.material3.TextFieldColors,
    fields: List<DetailFieldState>
) {
    fields.forEach { field ->
        OutlinedTextField(
            value = field.value,
            onValueChange = field.onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(field.label) },
            placeholder = { Text(field.placeholder) },
            singleLine = true,
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun UnifiedReviewsSection(
    reviews: List<YelpReview>,
    loading: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = DeepSea2,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yelp Reviews",
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "Hide" else "Show",
                    color = DeepSea4,
                    fontSize = 12.sp
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    loading -> Text("Loading reviews...", color = DeepSea4, fontSize = 12.sp)
                    reviews.isEmpty() -> Text("No reviews available.", color = DeepSea4, fontSize = 12.sp)
                    else -> reviews.forEach { review ->
                        Surface(
                            color = DeepSea1,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = review.user?.name ?: "Reviewer",
                                    color = DeepSea5,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Rating ${review.rating}/5", color = DeepSea4, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = review.text,
                                    color = DeepSea5,
                                    fontSize = 12.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedPickerField(
    modifier: Modifier,
    label: String,
    value: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = label, color = DeepSea4, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
            Icon(imageVector = icon, contentDescription = label, tint = DeepSea4, modifier = Modifier.size(18.dp))
        }
    }
}

private fun MutableMap<String, String>.applyUnifiedDetail(key: String, value: String, enabled: Boolean) {
    if (!enabled || value.isBlank()) remove(key) else put(key, value.trim())
}

private fun unifiedDefaultColorKeyForType(type: String): String {
    return when (type.lowercase(Locale.US)) {
        "flight" -> "rose"
        "hotel" -> "teal"
        "restaurant", "dining", "food" -> "olive"
        else -> "plum"
    }
}

private fun showUnifiedDatePicker(
    context: android.content.Context,
    initialDate: String = "",
    onDateSelected: (String) -> Unit
) {
    val initial = parseIsoDate(initialDate)
    val calendar = Calendar.getInstance().apply {
        if (initial != null) set(initial.year, initial.monthValue - 1, initial.dayOfMonth)
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> onDateSelected("%04d-%02d-%02d".format(year, month + 1, dayOfMonth)) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showUnifiedTimePicker(
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
        { _, hourOfDay, minute -> onTimeSelected(formatDisplayTime("%02d:%02d".format(hourOfDay, minute))) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}
