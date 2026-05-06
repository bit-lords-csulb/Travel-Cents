package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.main.current.calendar.EventLayoutInfo
import com.example.travelcents.ui.main.current.calendar.ScheduleWindow
import com.example.travelcents.ui.main.current.calendar.buildEventLayouts
import com.example.travelcents.ui.main.current.calendar.buildScheduleWindow
import com.example.travelcents.ui.main.current.calendar.currentTimeMinutes
import com.example.travelcents.ui.main.current.calendar.eventsForDate
import com.example.travelcents.ui.main.current.calendar.preferredDayScrollMinutes
import com.example.travelcents.ui.main.current.calendar.renderSpanLabel
import com.example.travelcents.ui.modules.hourLabel
import com.example.travelcents.ui.modules.todayIsoDate
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CurrentTripDayView(
    events: List<TravelEvent>,
    canEditTrip: Boolean,
    sortedDates: List<String>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onEventClick: (TravelEvent) -> Unit,
    onCreatePlan: (String, Int) -> Unit
) {
    val activeDate = selectedDate.takeIf { it.isNotBlank() } ?: sortedDates.firstOrNull() ?: todayIsoDate()
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
        CurrentTripPageSurface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .verticalScroll(verticalScroll)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TimeAxis(
                        scheduleWindow = scheduleWindow,
                        hourHeight = hourHeight
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ScheduleDayColumn(
                        modifier = Modifier.weight(1f),
                        date = activeDate,
                        events = dayEvents,
                        canEditTrip = canEditTrip,
                        scheduleWindow = scheduleWindow,
                        hourHeight = hourHeight,
                        onEventClick = onEventClick,
                        onEmptySlotClick = { startMinutes -> onCreatePlan(activeDate, startMinutes) },
                        showCurrentTimeIndicator = activeDate == todayIsoDate()
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAxis(
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp
) {
    Column(modifier = Modifier.width(50.dp)) {
        Spacer(modifier = Modifier.height(56.dp))
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
    canEditTrip: Boolean,
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    onEventClick: (TravelEvent) -> Unit,
    onEmptySlotClick: (Int) -> Unit,
    showCurrentTimeIndicator: Boolean = false
) {
    val gridHeight = hourHeight * (scheduleWindow.endHour - scheduleWindow.startHour).toFloat()
    val cardShape = CurrentTripInnerShape
    val eventLayouts = remember(events, date, scheduleWindow) {
        buildEventLayouts(
            date = date,
            events = events,
            scheduleWindow = scheduleWindow
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(gridHeight)
            .background(DeepSea2.copy(alpha = 0.85f), cardShape)
            .border(
                width = 1.dp,
                color = DeepSea3,
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
        if (canEditTrip) {
            ScheduleGridTapTarget(
                scheduleWindow = scheduleWindow,
                hourHeight = hourHeight,
                onSlotClick = onEmptySlotClick
            )
        }

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
                canEditTrip = canEditTrip,
                maxWidth = maxWidth,
                scheduleWindow = scheduleWindow,
                hourHeight = hourHeight,
                onClick = { onEventClick(layout.span.event) }
            )
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
                .background(Color(0xFFFF6B6B), CircleShape)
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
    canEditTrip: Boolean,
    maxWidth: Dp,
    scheduleWindow: ScheduleWindow,
    hourHeight: Dp,
    onClick: () -> Unit
) {
    val span = layout.span
    val startMinutes = span.startMinutes
    val durationMinutes = (span.endMinutes - span.startMinutes).coerceAtLeast(20)
    val topOffset = hourHeight * ((startMinutes - (scheduleWindow.startHour * 60)).coerceAtLeast(0) / 60f)
    val minHeight = 52.dp
    val calculatedHeight = hourHeight * (durationMinutes / 60f)
    val eventHeight = if (calculatedHeight < minHeight) minHeight else calculatedHeight
    val palette = eventPalette(span.event)
    val columnGap = 6.dp
    val horizontalInset = 6.dp
    val totalGap = columnGap * (layout.columnCount - 1).coerceAtLeast(0)
    val availableWidth = (maxWidth - (horizontalInset * 2) - totalGap).coerceAtLeast(80.dp)
    val eventWidth = availableWidth / layout.columnCount.coerceAtLeast(1)
    val xOffset = horizontalInset + ((eventWidth + columnGap) * layout.columnIndex)

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
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = renderSpanLabel(span),
                        color = DeepSea4,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = span.titleOverride ?: eventTitle(span.event),
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (eventHeight > 68.dp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = eventSubtitle(span.event),
                        color = DeepSea4,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

