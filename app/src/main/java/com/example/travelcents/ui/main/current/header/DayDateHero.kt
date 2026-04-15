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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.modules.formatDayOfWeekShort
import com.example.travelcents.ui.modules.formatHeroDate
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private val PrimaryAccent = Color(0xFFCEBDFF)

@Composable
fun DayDateHero(
    selectedDate: String,
    sortedDates: List<String>,
    onDateSelected: (String) -> Unit
) {
    val activeDate = selectedDate.takeIf { it.isNotBlank() } ?: sortedDates.firstOrNull().orEmpty()
    val activeIndex = sortedDates.indexOf(activeDate).coerceAtLeast(0)
    val previousDate = sortedDates.getOrNull(activeIndex - 1)
    val nextDate = sortedDates.getOrNull(activeIndex + 1)

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
                onClick = { previousDate?.let(onDateSelected) },
                enabled = previousDate != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous day",
                    tint = if (previousDate != null) DeepSea5 else DeepSea4.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = formatDayOfWeekShort(activeDate),
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    lineHeight = 14.sp
                )
                Text(
                    text = formatHeroDate(activeDate),
                    color = DeepSea5,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 50.sp
                )
            }
            IconButton(
                onClick = { nextDate?.let(onDateSelected) },
                enabled = nextDate != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = if (nextDate != null) DeepSea5 else DeepSea4.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

