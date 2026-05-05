package com.example.travelcents.ui.main.current

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.heroImageModel
import com.example.travelcents.ui.modules.normalizeTime
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import kotlinx.coroutines.flow.distinctUntilChanged
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val UndatedGroupKey = "__undated__"
private const val UndatedHeader = "DATE TBD"

private sealed interface CurrentTripItineraryItem {
    val key: String

    data class Header(val rawDate: String, val label: String) : CurrentTripItineraryItem { override val key = "header_$rawDate" }
    data class EventCard(val event: TravelEvent, val isLastInDay: Boolean, val dayIndex: Int) : CurrentTripItineraryItem {
        override val key = event.eventId
    }
    data class DaySpacer(val date: String) : CurrentTripItineraryItem { override val key = "spacer_$date" }
}

@Composable
fun CurrentTripItineraryContent(
    events: List<TravelEvent>,
    eventOptions: Map<String, List<EventOption>>,
    rejectedOptions: Map<String, Set<String>>,
    canEditTrip: Boolean,
    jiggleMode: Boolean,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onOpenAlternatives: (String) -> Unit,
    onMoveEvent: (eventId: String, fromDate: String, toDate: String, toIndex: Int) -> Unit,
    onPersistEventPlacements: (Set<String>) -> Unit,
    onVisibleDateChange: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        CurrentTripPageSurface(modifier = Modifier.fillMaxSize()) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tap the + button to create your first reservation or activity.",
                        color = DeepSea4,
                        fontSize = 14.sp
                    )
                }
                return@CurrentTripPageSurface
            }

            val itineraryItems = remember(events) { buildCurrentTripItineraryItems(events) }
            val lazyListState = rememberLazyListState()
            var affectedDragDates by remember { mutableStateOf<Set<String>>(emptySet()) }

            val infiniteTransition = rememberInfiniteTransition(label = "trip_itinerary_jiggle")
            val wobbleAngle by infiniteTransition.animateFloat(
                initialValue = -0.5f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
                label = "trip_itinerary_wobble"
            )
            val cardRotation = if (jiggleMode) wobbleAngle else 0f

            val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromItem = itineraryItems.firstOrNull { it.key == from.key } as? CurrentTripItineraryItem.EventCard
                    ?: return@rememberReorderableLazyListState
                val toItem = itineraryItems.firstOrNull { it.key == to.key } as? CurrentTripItineraryItem.EventCard
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

            LaunchedEffect(lazyListState, itineraryItems, jiggleMode) {
                snapshotFlow { lazyListState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .collect { firstIndex ->
                        val bannerOffset = if (jiggleMode) 1 else 0
                        val adjustedIndex = (firstIndex - bannerOffset).coerceAtLeast(0)
                        for (i in adjustedIndex downTo 0) {
                            val item = itineraryItems.getOrNull(i) as? CurrentTripItineraryItem.Header ?: continue
                            if (item.rawDate != UndatedGroupKey) {
                                onVisibleDateChange(item.rawDate)
                                break
                            }
                        }
                    }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
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

                items(itineraryItems, key = { it.key }) { item ->
                    when (item) {
                        is CurrentTripItineraryItem.Header -> ItineraryDateHeader(item.label)
                        is CurrentTripItineraryItem.EventCard -> {
                            val options = eventOptions[item.event.eventId].orEmpty()
                            val rejected = rejectedOptions[item.event.eventId].orEmpty()
                            val activeOptionCount = options.count { it.optionId !in rejected }
                            val optionsLoaded = item.event.eventId in eventOptions
                            ReorderableItem(reorderState, key = item.event.eventId) { isDragging ->
                                CurrentTripItineraryCard(
                                    event = item.event,
                                    isLast = item.isLastInDay,
                                    canEditTrip = canEditTrip,
                                    hasAlternatives = !optionsLoaded || activeOptionCount > 1,
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

                        is CurrentTripItineraryItem.DaySpacer -> Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryDateHeader(date: String) {
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
private fun CurrentTripItineraryCard(
    event: TravelEvent,
    isLast: Boolean,
    canEditTrip: Boolean,
    hasAlternatives: Boolean,
    isDragging: Boolean,
    jiggleMode: Boolean,
    wobbleAngle: Float,
    modifier: Modifier,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAlternativesClick: () -> Unit
) {
    val context = LocalContext.current
    val accent = eventPalette(event).accent
    val title = eventTitle(event)
    val description = eventSubtitle(event).ifBlank { "Tap to edit details" }
    val heroImage = remember(event, context) { event.heroImageModel(context) }

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
                    if (heroImage.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .width(116.dp)
                                .fillMaxHeight()
                        ) {
                            AsyncImage(
                                model = heroImage,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
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
                    if (canEditTrip && hasAlternatives) {
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

                    if (canEditTrip) {
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

private fun buildCurrentTripItineraryItems(events: List<TravelEvent>): List<CurrentTripItineraryItem> {
    val sorted = events.sortedWith(
        compareBy(
            { it.date.ifBlank { "9999-12-31" } },
            { normalizeTime(it.startTime) },
            { itineraryEventTypePriority(it) },
            { it.details["sortOrder"]?.toIntOrNull() ?: Int.MAX_VALUE },
            { it.eventId }
        )
    )
    val grouped = sorted.groupBy { it.date.ifBlank { UndatedGroupKey } }
        .entries
        .sortedBy { if (it.key == UndatedGroupKey) "9999-12-31" else it.key }

    return buildList {
        grouped.forEach { (date, dayEvents) ->
            add(CurrentTripItineraryItem.Header(rawDate = date, label = itineraryHeaderLabel(date)))
            dayEvents.forEachIndexed { idx, event ->
                add(CurrentTripItineraryItem.EventCard(event, isLastInDay = idx == dayEvents.lastIndex, dayIndex = idx))
            }
            add(CurrentTripItineraryItem.DaySpacer(date))
        }
    }
}

private fun itineraryEventTypePriority(event: TravelEvent): Int {
    return when (event.type.lowercase(Locale.US)) {
        "flight" -> 0
        "hotel" -> 1
        "restaurant", "dining", "food" -> 2
        else -> 3
    }
}

private fun itineraryHeaderLabel(groupKey: String): String {
    return if (groupKey == UndatedGroupKey) UndatedHeader else formatItineraryHeaderDate(groupKey)
}

private fun formatItineraryHeaderDate(dateStr: String): String {
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

