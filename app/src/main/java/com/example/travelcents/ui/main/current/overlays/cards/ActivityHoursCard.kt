package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.trip.model.ATTR_HOURS_SUMMARY
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun ActivityHoursCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    val hours = event.detailValue(ATTR_HOURS_SUMMARY, "hours_summary", "hours")
    val ticketsUrl = event.details["tickets_url"]?.takeIf { it.isNotBlank() }
    val closedLabel = isClosedLabel(event)
    if (hours == null && ticketsUrl == null && closedLabel == null) return

    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(
            eyebrow = "Hours",
            title = hours ?: "Availability details"
        )
        closedLabel?.let {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(badges = listOf(it), accent = CardMint)
        }
        if (ticketsUrl != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLinkRow(
                    label = "Tickets",
                    value = "Open ticket link",
                    onClick = { uriHandler.openUri(ticketsUrl) },
                    accent = CardMint
                )
            }
        }
    }
}
