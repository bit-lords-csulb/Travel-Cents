package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_HOURS_RAW
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.time.ZonedDateTime

@Composable
fun RestaurantHoursCard(event: TravelEvent) {
    val rawHours = event.detailValue(ATTR_HOURS_RAW, "hours_raw")?.takeIf { it.isNotBlank() } ?: return
    val schedule = parseBusinessHours(rawHours)
    if (schedule.isEmpty()) return

    val zoneId = businessZoneId(event)
    val now = ZonedDateTime.now(zoneId)
    val todayIndex = now.dayOfWeek.toYelpDayIndex()
    val rows = businessScheduleRows(schedule, todayIndex)
    val todayHours = rows.firstOrNull { it.isToday }?.hoursLabel ?: HOURS_CLOSED_LABEL
    val timeZoneLabel = restaurantHoursTimeZoneLabel(
        event = event,
        referenceDate = now.toLocalDate().toString(),
        referenceTime = now.toLocalTime().format(hoursFormatter)
    )

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Hours",
            title = if (todayHours == HOURS_CLOSED_LABEL) "Closed today" else "Today · $todayHours"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = timeZoneLabel,
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOf(if (isBusinessOpenNow(schedule, now)) "Open now" else "Closed"),
            accent = CardCoral
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (row.isToday) {
                                CardCoral.copy(alpha = 0.12f)
                            } else {
                                CardSurfaceHigh
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = row.dayLabel,
                        color = if (row.isToday) CardCoral else CardText,
                        fontSize = 13.sp,
                        fontWeight = if (row.isToday) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = row.hoursLabel,
                        color = if (row.hoursLabel == HOURS_CLOSED_LABEL) CardTextMuted else CardText,
                        fontSize = 13.sp,
                        fontWeight = if (row.isToday) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
