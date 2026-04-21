package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.trip.model.TravelEvent

@Composable
fun ActivityHoursCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    DetailCardFrame(accent = CardMint) {
        DetailCardHeader(eyebrow = "Hours", title = hoursSummary(event))
        isClosedLabel(event)?.let {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(badges = listOf(it), accent = CardMint)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            event.details["tickets_url"]?.takeIf { it.isNotBlank() }?.let {
                DetailLinkRow(
                    label = "Tickets",
                    value = "Open ticket link",
                    onClick = { uriHandler.openUri(it) },
                    accent = CardMint
                )
            }
            eventOfficialUrl(event)?.let {
                DetailLinkRow(
                    label = "Source",
                    value = compactHostLabel(it),
                    onClick = { uriHandler.openUri(it) },
                    accent = CardMint
                )
            }
        }
    }
}
