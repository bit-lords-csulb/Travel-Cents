package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.ui.main.current.calendar.buildTripDateRange
import com.example.travelcents.ui.main.current.calendar.defaultStartMinutesForDate
import com.example.travelcents.ui.main.current.calendar.eventSpanForDate
import com.example.travelcents.ui.main.current.calendar.eventsForDate
import com.example.travelcents.ui.main.current.calendar.renderSpanLabel
import com.example.travelcents.ui.main.current.calendar.visibleWeekDatesForSelection
import com.example.travelcents.ui.modules.formatDayOfWeekShort
import com.example.travelcents.ui.modules.formatDisplayTimeRange
import com.example.travelcents.ui.modules.formatMonthDayCompact
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun WeekPage(
    events: List<TravelEvent>,
    sortedDates: List<String>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onCreatePlan: (String, Int) -> Unit
) {
    if (sortedDates.isEmpty()) {
        CurrentTripEmptyState(
            title = "No Calendar Dates",
            body = "Set trip dates first so you can add plans to the calendar."
        )
        return
    }

    val visibleWeekDates = remember(sortedDates, selectedDate) {
        visibleWeekDatesForSelection(sortedDates, selectedDate)
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
            .background(DeepSea2, RoundedCornerShape(18.dp))
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
            .background(if (selected) DeepSea3.copy(alpha = 0.88f) else DeepSea1.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
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
                .background(if (canAddPlan) DeepSea2 else DeepSea2.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
            .background(palette.container, RoundedCornerShape(12.dp))
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
