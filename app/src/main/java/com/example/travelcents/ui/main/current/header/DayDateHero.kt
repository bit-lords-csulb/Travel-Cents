package com.example.travelcents.ui.main.current.header

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.modules.formatDayOfWeekFull
import com.example.travelcents.ui.modules.formatHeroDate
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

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

    CurrentTripHeroLayout(
        previousAction = CurrentTripHeroNavAction(
            enabled = previousDate != null,
            contentDescription = "Previous day",
            onClick = { previousDate?.let(onDateSelected) }
        ),
        nextAction = CurrentTripHeroNavAction(
            enabled = nextDate != null,
            contentDescription = "Next day",
            onClick = { nextDate?.let(onDateSelected) }
        )
    ) {
        Text(
            text = formatDayOfWeekFull(activeDate),
            color = DeepSea4,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            lineHeight = 16.sp,
            fontFamily = TravelCentsFonts.Body
        )
        Text(
            text = formatHeroDate(activeDate),
            color = DeepSea5,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 50.sp,
            fontFamily = TravelCentsFonts.Headline
        )
    }
}

