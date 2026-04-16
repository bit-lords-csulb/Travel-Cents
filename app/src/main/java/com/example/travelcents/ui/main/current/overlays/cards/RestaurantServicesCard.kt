package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.trip.model.ATTR_HAS_FOOD_ORDER
import com.example.travelcents.data.trip.model.ATTR_HAS_REQUEST_A_QUOTE
import com.example.travelcents.data.trip.model.ATTR_HAS_RESERVATIONS
import com.example.travelcents.data.trip.model.ATTR_HAS_WAITLIST
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_PHONE
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun RestaurantServicesCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    val badges = listOfNotNull(
        event.detailValue(ATTR_HAS_RESERVATIONS)?.takeIf { it == "true" }?.let { "Reservations" },
        event.detailValue(ATTR_HAS_WAITLIST)?.takeIf { it == "true" }?.let { "Waitlist" },
        event.detailValue(ATTR_HAS_FOOD_ORDER)?.takeIf { it == "true" }?.let { "Food ordering" },
        event.detailValue(ATTR_HAS_REQUEST_A_QUOTE)?.takeIf { it == "true" }?.let { "Quote requests" },
        event.detailValue(ATTR_PHONE)?.takeIf { it.isNotBlank() }
    )

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(eyebrow = "Services", title = "Reservation and contact options")
        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(badges = badges, accent = CardCoral)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            event.detailValue(ATTR_MENU_URL, "yelp_menu_url")?.let {
                DetailLinkRow(
                    label = "Menu",
                    value = "View menu",
                    onClick = { uriHandler.openUri(it) },
                    accent = CardCoral
                )
            }
            event.detailValue(ATTR_YELP_URL)?.let {
                DetailLinkRow(
                    label = "Yelp",
                    value = "Open listing",
                    onClick = { uriHandler.openUri(it) },
                    accent = CardCoral
                )
            }
        }
    }
}
