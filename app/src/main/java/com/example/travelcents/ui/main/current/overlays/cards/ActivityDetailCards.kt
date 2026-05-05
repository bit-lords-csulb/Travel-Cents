package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_HOURS_RAW
import com.example.travelcents.data.trip.model.ATTR_HOURS_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_PHONE
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

@Composable
internal fun ActivityContactCard(
    event: TravelEvent,
    websiteLabel: String,
    onOpenWebsite: (() -> Unit)? = null,
    ticketmasterMode: Boolean = false
) {
    val title = event.detailValue(
        ATTR_BUSINESS_NAME,
        ATTR_VENUE_NAME,
        "activity_name",
        "title",
        "name"
    )
    val address = event.detailValue(ATTR_BUSINESS_ADDRESS, "address", "location")
    val phone = event.detailValue(ATTR_PHONE, "phone")
    val email = event.detailValue("email", "contact_email")
    val isTicketmasterBacked = ticketmasterMode || !event.detailValue(ATTR_TICKETMASTER_EVENT_ID).isNullOrBlank()
    val badges = if (isTicketmasterBacked) emptyList() else buildActivityBadges(event)
    val hasWebsite = onOpenWebsite != null
    val shouldShowAddress = !isTicketmasterBacked && !address.isNullOrBlank()
    val shouldShowPhone = !phone.isNullOrBlank()
    val shouldShowEmail = !email.isNullOrBlank()

    if (isTicketmasterBacked) {
        if (!hasWebsite && !shouldShowPhone && !shouldShowEmail) return
    } else if (title.isNullOrBlank() && address.isNullOrBlank() && phone.isNullOrBlank() &&
        onOpenWebsite == null && badges.isEmpty()
    ) return

    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(
            eyebrow = "Contact",
            title = if (isTicketmasterBacked) {
                title ?: "Ticketing details"
            } else {
                title ?: "Activity details"
            }
        )

        Spacer(modifier = Modifier.padding(top = 10.dp))

        if (shouldShowAddress) {
            ActivityInfoRow(
                label = "Address",
                value = address.orEmpty()
            )
        }

        if (shouldShowPhone) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            ActivityInfoRow(
                label = "Phone",
                value = phone.orEmpty()
            )
        }

        if (shouldShowEmail) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            ActivityInfoRow(
                label = "Email",
                value = email.orEmpty()
            )
        }

        if (hasWebsite) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            ActivityInfoRow(
                label = "Website",
                value = websiteLabel,
                accent = CardMint,
                onClick = onOpenWebsite
            )
        }

        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(top = 14.dp))
            DetailBadgeRow(
                badges = badges,
                accent = CardMint
            )
        }
    }
}

@Composable
internal fun ActivityHoursCard(event: TravelEvent) {
    val rawHours = event.detailValue(ATTR_HOURS_RAW, "hours_raw")?.takeIf { it.isNotBlank() }
    if (rawHours != null) {
        ActivityStructuredHoursCard(event = event, rawHours = rawHours)
        return
    }

    val summary = event.detailValue(ATTR_HOURS_SUMMARY, "hours_summary", "hours")?.takeIf { it.isNotBlank() }
    val closedLabel = isClosedLabel(event)
    if (summary == null && closedLabel == null) return

    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(
            eyebrow = "Hours",
            title = summary ?: "Hours unavailable"
        )
        if (closedLabel != null) {
            Spacer(modifier = Modifier.padding(top = 12.dp))
            DetailBadgeRow(
                badges = listOf(closedLabel),
                accent = CardMint
            )
        }
    }
}

@Composable
private fun ActivityStructuredHoursCard(
    event: TravelEvent,
    rawHours: String
) {
    val schedule = parseBusinessHours(rawHours)
    if (schedule.isEmpty()) return

    val zoneId = businessZoneId(event)
    val now = java.time.ZonedDateTime.now(zoneId)
    val todayIndex = now.dayOfWeek.toYelpDayIndex()
    val rows = businessScheduleRows(schedule, todayIndex)
    val todayHours = rows.firstOrNull { it.isToday }?.hoursLabel ?: HOURS_CLOSED_LABEL
    val timeZoneLabel = activityHoursTimeZoneLabel(
        event = event,
        referenceDate = now.toLocalDate().toString(),
        referenceTime = now.toLocalTime().format(hoursFormatter)
    )

    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(
            eyebrow = "Hours",
            title = if (todayHours == HOURS_CLOSED_LABEL) "Closed today" else "Today · $todayHours"
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = timeZoneLabel,
            color = CardTextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.padding(top = 12.dp))
        DetailBadgeRow(
            badges = listOf(if (isBusinessOpenNow(schedule, now)) "Open now" else "Closed"),
            accent = CardMint
        )
        Spacer(modifier = Modifier.padding(top = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (row.isToday) {
                                CardMint.copy(alpha = 0.12f)
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
                        color = if (row.isToday) CardMint else CardText,
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

@Composable
private fun ActivityInfoRow(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color = CardText,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(Locale.US),
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = value,
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun buildActivityBadges(event: TravelEvent): List<String> {
    return listOfNotNull(
        event.detailValue(ATTR_CATEGORIES, "categories"),
        event.detailValue(ATTR_PRICE_TIER, "price_tier", "cost"),
        event.detailValue(ATTR_AVERAGE_RATING, "rating")?.let { "★$it" }
    )
}
