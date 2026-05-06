package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.remote.FlightOffersResolver

@Composable
fun FlightBookingOffersCard(event: TravelEvent, adults: Int) {
    val offers = remember(event, adults) { FlightOffersResolver.resolve(event, adults) }
    if (offers.isEmpty()) return

    val uriHandler = LocalUriHandler.current

    DetailCardFrame(accent = CardSky) {
        DetailCardHeader(eyebrow = "Booking", title = "Find this flight")
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            offers.forEach { offer ->
                ProviderOfferRow(
                    source = offer.source,
                    subtitle = offer.subtitle,
                    onClick = { uriHandler.openUri(offer.link) },
                    accent = CardSky,
                    actionLabel = "Book",
                    logoUrl = offer.logoUrl
                )
            }
        }
    }
}
