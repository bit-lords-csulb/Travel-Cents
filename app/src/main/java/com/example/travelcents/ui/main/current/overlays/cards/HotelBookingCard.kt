package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_OFFER_COUNT
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName

private data class HotelBookingOffer(
    val source: String,
    val link: String,
    val logo: String?
)

@Composable
fun HotelBookingCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    val offers = readBookingOffers(event)
    val fallbackBookingUrl = event.detailValue(ATTR_BOOKING_URL, "booking_url")

    if (offers.isEmpty() && fallbackBookingUrl.isNullOrBlank()) return

    DetailCardFrame(accent = CardLavender) {
        DetailCardHeader(
            eyebrow = "Booking",
            title = if (offers.isNotEmpty()) "Reserve this hotel" else "Open hotel offer"
        )

        if (offers.isNotEmpty()) {
            Spacer(modifier = Modifier.size(2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                offers.forEach { offer ->
                    BookingOfferRow(
                        offer = offer,
                        onClick = { uriHandler.openUri(offer.link) }
                    )
                }
            }
        } else {
            BookingOfferRow(
                offer = HotelBookingOffer(
                    source = event.displayName() ?: "Hotel offer",
                    link = fallbackBookingUrl.orEmpty(),
                    logo = null
                ),
                onClick = { fallbackBookingUrl?.let(uriHandler::openUri) }
            )
        }
    }
}

@Composable
private fun BookingOfferRow(
    offer: HotelBookingOffer,
    onClick: () -> Unit
) {
    ProviderOfferRow(
        source = offer.source,
        subtitle = "Official property page",
        onClick = onClick,
        accent = CardLavender,
        actionLabel = "Book",
        logoUrl = offer.logo
    )
}

private fun readBookingOffers(event: TravelEvent): List<HotelBookingOffer> {
    val count = event.detailValue(ATTR_OFFER_COUNT)?.toIntOrNull() ?: 0
    if (count <= 0) return emptyList()
    return (0 until count).mapNotNull { idx ->
        val source = event.details["offer_${idx}_source"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val link = event.details["offer_${idx}_link"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        HotelBookingOffer(
            source = source,
            link = link,
            logo = event.details["offer_${idx}_logo"]?.takeIf { it.isNotBlank() }
        )
    }
}
