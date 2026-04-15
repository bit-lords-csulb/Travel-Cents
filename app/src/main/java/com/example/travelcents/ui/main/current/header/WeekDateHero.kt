package com.example.travelcents.ui.main.current.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.main.current.calendar.visibleWeekDatesForSelection
import com.example.travelcents.ui.modules.formatWeekRangeHero
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private val PrimaryAccent = Color(0xFFCEBDFF)

@Composable
fun WeekDateHero(
    selectedDate: String,
    sortedDates: List<String>,
    onDateSelected: (String) -> Unit
) {
    val visibleDates = remember(sortedDates, selectedDate) {
        visibleWeekDatesForSelection(sortedDates, selectedDate)
    }
    val weekStartIndex = sortedDates.indexOf(visibleDates.firstOrNull()).coerceAtLeast(0)
    val previousAnchor = sortedDates.getOrNull((weekStartIndex - 7).coerceAtLeast(0))
        ?.takeIf { visibleDates.firstOrNull() != sortedDates.firstOrNull() }
    val nextAnchor = sortedDates.getOrNull((weekStartIndex + 7).coerceAtMost(sortedDates.lastIndex))
        ?.takeIf { visibleDates.lastOrNull() != sortedDates.lastOrNull() }

    val rangeLabel = remember(visibleDates) {
        formatWeekRangeHero(
            startDate = visibleDates.firstOrNull().orEmpty(),
            endDate = visibleDates.lastOrNull().orEmpty()
        )
    }

    Column {
        Text(
            text = "SCHEDULED ITINERARY",
            color = PrimaryAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Left arrow sits in the 24dp margin (button size = 24dp, starts at screen edge)
            IconButton(
                onClick = { previousAnchor?.let(onDateSelected) },
                enabled = previousAnchor != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous week",
                    tint = if (previousAnchor != null) DeepSea5 else DeepSea4.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = rangeLabel,
                color = DeepSea5,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp
            )
            IconButton(
                onClick = { nextAnchor?.let(onDateSelected) },
                enabled = nextAnchor != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next week",
                    tint = if (nextAnchor != null) DeepSea5 else DeepSea4.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

