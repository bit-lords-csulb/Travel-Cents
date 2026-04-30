package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_CURRENT_BUSYNESS
import com.example.travelcents.data.trip.model.ATTR_ESTIMATED_WAIT_MIN
import com.example.travelcents.data.trip.model.ATTR_POPULAR_TIMES_JSON
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val BESTTIME_HOUR_OFFSET = 6
private val bestTimeLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)

@Composable
fun WaitTimeCard(event: TravelEvent) {
    val popularTimesJson = event.detailValue(ATTR_POPULAR_TIMES_JSON)?.takeIf { it.isNotBlank() } ?: return
    val weeklyProfile = remember(popularTimesJson) { parsePopularTimes(popularTimesJson) }
    if (weeklyProfile.isEmpty()) return

    val now = ZonedDateTime.now(waitTimeZoneId(event))
    val todayIndex = now.dayOfWeek.toBestTimeDayIndex()
    val dayPeaks = weeklyProfile.map { it.maxOrNull() ?: 0 }
    val currentBusyness = event.detailValue(ATTR_CURRENT_BUSYNESS)
        ?.toIntOrNull()
        ?.coerceIn(0, 100)
    val estimatedWaitMin = event.detailValue(ATTR_ESTIMATED_WAIT_MIN)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
    val quietestTime = quietestRemainingHourLabel(
        dayProfile = weeklyProfile.getOrNull(todayIndex).orEmpty(),
        currentHour = now.hour
    )

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Live",
            title = waitTimeTitle(
                currentBusyness = currentBusyness,
                estimatedWaitMin = estimatedWaitMin
            )
        )

        val badges = listOfNotNull(
            currentBusyness?.let { "$it% of weekly peak" },
            quietestTime?.let { "Best later today · $it" }
        )
        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(
                badges = badges,
                accent = CardCoral
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayPeaks.forEachIndexed { index, intensity ->
                WaitTimeDayBar(
                    label = bestTimeDayLabel(index),
                    intensity = intensity,
                    isToday = index == todayIndex,
                    modifier = Modifier.width(40.dp)
                )
            }
        }

        quietestTime?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Usually best time today: $it.",
                color = CardTextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun WaitTimeDayBar(
    label: String,
    intensity: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(
                    color = if (isToday) CardCoral.copy(alpha = 0.12f) else CardSurfaceHigh,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val fillHeight = if (intensity <= 0) 0.dp else (12 + (intensity.coerceIn(0, 100) * 0.52f)).dp
            if (fillHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fillHeight)
                        .background(
                            color = if (isToday) CardCoral else CardTextMuted.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isToday) CardCoral else CardTextMuted,
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun parsePopularTimes(rawJson: String): List<List<Int>> {
    return runCatching {
        val root = JSONArray(rawJson)
        List(root.length()) { dayIndex ->
            val dayArray = root.optJSONArray(dayIndex) ?: JSONArray()
            List(24) { hourIndex ->
                dayArray.optInt(hourIndex, 0).coerceIn(0, 100)
            }
        }.takeIf { it.isNotEmpty() }.orEmpty()
    }.getOrDefault(emptyList())
}

private fun waitTimeTitle(
    currentBusyness: Int?,
    estimatedWaitMin: Int?
): String {
    return when {
        currentBusyness == null -> "Typical foot traffic this week"
        estimatedWaitMin != null -> "Busy right now · about $estimatedWaitMin min wait"
        currentBusyness >= 55 -> "Steady right now"
        currentBusyness >= 30 -> "Easy right now"
        else -> "Not busy right now"
    }
}

private fun quietestRemainingHourLabel(
    dayProfile: List<Int>,
    currentHour: Int
): String? {
    if (dayProfile.isEmpty()) return null
    val currentBestTimeIndex = ((currentHour - BESTTIME_HOUR_OFFSET) + 24) % 24
    val remaining = (currentBestTimeIndex until dayProfile.size)
        .map { index -> index to dayProfile[index] }
        .filter { (_, intensity) -> intensity > 0 }
    val quietestIndex = remaining.minByOrNull { (_, intensity) -> intensity }?.first ?: return null
    val hour = (quietestIndex + BESTTIME_HOUR_OFFSET) % 24
    return LocalTime.of(hour, 0).format(bestTimeLabelFormatter)
}

private fun waitTimeZoneId(event: TravelEvent): ZoneId {
    val timeZoneId = event.tz.takeIf { it.isNotBlank() } ?: return ZoneId.systemDefault()
    return runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
}

private fun DayOfWeek.toBestTimeDayIndex(): Int = value - 1

private fun bestTimeDayLabel(dayIndex: Int): String {
    return when (dayIndex) {
        0 -> "Mon"
        1 -> "Tue"
        2 -> "Wed"
        3 -> "Thu"
        4 -> "Fri"
        5 -> "Sat"
        6 -> "Sun"
        else -> dayIndex.toString()
    }
}
