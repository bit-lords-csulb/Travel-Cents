package com.example.travelcents.ui.main.current.header

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.main.current.calendar.visibleWeekDatesForSelection
import com.example.travelcents.ui.modules.formatWeekRangeHero
import com.example.travelcents.ui.modules.parseIsoDate
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun WeekDateHero(
    selectedDate: String,
    sortedDates: List<String>,
    onDateSelected: (String) -> Unit
) {
    val visibleDates = remember(sortedDates, selectedDate) {
        visibleWeekDatesForSelection(sortedDates, selectedDate)
    }
    val anchorDate = parseIsoDate(selectedDate.ifBlank { visibleDates.firstOrNull().orEmpty() })
        ?: parseIsoDate(visibleDates.firstOrNull().orEmpty())
    val previousAnchor = anchorDate?.minusDays(7)?.toString()
    val nextAnchor = anchorDate?.plusDays(7)?.toString()

    val rangeLabel = remember(visibleDates) {
        formatWeekRangeHero(
            startDate = visibleDates.firstOrNull().orEmpty(),
            endDate = visibleDates.lastOrNull().orEmpty()
        )
    }

    CurrentTripHeroLayout(
        previousAction = CurrentTripHeroNavAction(
            enabled = anchorDate != null,
            contentDescription = "Previous week",
            onClick = { previousAnchor?.let(onDateSelected) }
        ),
        nextAction = CurrentTripHeroNavAction(
            enabled = anchorDate != null,
            contentDescription = "Next week",
            onClick = { nextAnchor?.let(onDateSelected) }
        )
    ) {
        Text(
            text = rangeLabel,
            color = DeepSea5,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 40.sp,
            fontFamily = TravelCentsFonts.Headline
        )
    }
}

