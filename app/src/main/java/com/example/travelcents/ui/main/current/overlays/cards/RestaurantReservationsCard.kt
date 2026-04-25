package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_HAS_RESERVATIONS
import com.example.travelcents.data.trip.model.ATTR_HAS_WAITLIST
import com.example.travelcents.data.trip.model.ATTR_RESERVATION_OFFER_COUNT
import com.example.travelcents.data.trip.model.ATTR_RESERVATION_PARTY_SIZE
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.data.trip.remote.GoogleReserveResolver
import com.example.travelcents.data.trip.remote.OpenTableResolver
import com.example.travelcents.data.trip.remote.ResyResolver
import com.example.travelcents.data.trip.remote.reservationCitySlug
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatTripDate

private data class RestaurantReservationOffer(
    val source: String,
    val link: String,
    val subtitle: String,
    val logoUrl: String? = null
)

@Composable
fun RestaurantReservationsCard(
    event: TravelEvent,
    tripDestination: String,
    tripAdults: Int,
    tripChildren: Int
) {
    val uriHandler = LocalUriHandler.current
    val partySize = reservationPartySize(event, tripAdults, tripChildren)
    val offers = reservationOffers(
        event = event,
        tripDestination = tripDestination,
        partySize = partySize
    )
    val hasWaitlist = event.detailValue(ATTR_HAS_WAITLIST)?.equals("true", ignoreCase = true) == true
    val hasReservationsSignal = event.detailValue(ATTR_HAS_RESERVATIONS)?.equals("true", ignoreCase = true) == true

    if (offers.isEmpty() && !hasReservationsSignal && !hasWaitlist) return

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Reservations",
            title = "Book a table"
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailBadgeRow(
            badges = listOfNotNull(
                reservationPartyLabel(partySize),
                reservationDateTimeLabel(event)
            ),
            accent = CardCoral
        )

        if (hasWaitlist) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Walk-in waitlist available.",
                color = CardCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardCoral.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }

        if (offers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                offers.forEach { offer ->
                    ProviderOfferRow(
                        source = offer.source,
                        subtitle = offer.subtitle,
                        onClick = { uriHandler.openUri(offer.link) },
                        accent = CardCoral,
                        actionLabel = "Open",
                        logoUrl = offer.logoUrl
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Reservations may be available through partner listings for this restaurant.",
                color = CardTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun reservationOffers(
    event: TravelEvent,
    tripDestination: String,
    partySize: Int
): List<RestaurantReservationOffer> {
    val restaurantName = event.displayName()
        ?: event.detailValue(ATTR_BUSINESS_NAME, "restaurant_name", "title", "name")
        ?: return readStoredReservationOffers(event)
    val address = event.detailValue(ATTR_BUSINESS_ADDRESS, "address")
    val locationHint = listOfNotNull(
        event.details["location"]?.takeIf { it.isNotBlank() },
        extractReservationCity(address),
        tripDestination.takeIf { it.isNotBlank() }
    ).firstOrNull()
    val searchQuery = listOfNotNull(
        restaurantName,
        address?.takeIf { it.isNotBlank() } ?: locationHint
    ).joinToString(", ")

    return buildList {
        addAll(readStoredReservationOffers(event))

        OpenTableResolver.buildSearchUrl(
            restaurantName = restaurantName,
            locationHint = locationHint,
            date = event.date,
            time = event.startTime,
            partySize = partySize
        )?.let { link ->
            add(
                RestaurantReservationOffer(
                    source = "OpenTable",
                    link = link,
                    subtitle = "Search live availability"
                )
            )
        }

        ResyResolver.buildSearchUrl(
            restaurantName = restaurantName,
            citySlug = reservationCitySlug(locationHint ?: address ?: tripDestination),
            date = event.date,
            partySize = partySize
        )?.let { link ->
            add(
                RestaurantReservationOffer(
                    source = "Resy",
                    link = link,
                    subtitle = "Search city reservations"
                )
            )
        }

        GoogleReserveResolver.buildSearchUrl(searchQuery)
            ?.let { link ->
                add(
                    RestaurantReservationOffer(
                        source = "Google Reserve",
                        link = link,
                        subtitle = "Open reserve search"
                    )
                )
            }
    }.distinctBy { it.source.lowercase() }
}

private fun readStoredReservationOffers(event: TravelEvent): List<RestaurantReservationOffer> {
    val count = event.detailValue(ATTR_RESERVATION_OFFER_COUNT)?.toIntOrNull() ?: 0
    if (count <= 0) return emptyList()

    return (0 until count).mapNotNull { index ->
        val source = event.details["reservation_offer_${index}_source"]?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val link = event.details["reservation_offer_${index}_link"]?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val deeplinkType = event.details["reservation_offer_${index}_deeplink_type"]
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()

        RestaurantReservationOffer(
            source = source,
            link = link,
            subtitle = when (deeplinkType) {
                "yelp" -> "Direct Yelp reservation"
                else -> "Open reservation link"
            },
            logoUrl = event.details["reservation_offer_${index}_logo"]?.takeIf { it.isNotBlank() }
        )
    }
}

private fun reservationPartySize(
    event: TravelEvent,
    tripAdults: Int,
    tripChildren: Int
): Int {
    return event.detailValue(ATTR_RESERVATION_PARTY_SIZE, "reservation_party_size")
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: (tripAdults + tripChildren).coerceAtLeast(1)
}

private fun reservationPartyLabel(partySize: Int): String {
    return if (partySize == 1) "1 guest" else "$partySize guests"
}

private fun reservationDateTimeLabel(event: TravelEvent): String? {
    val dateLabel = event.date.takeIf { it.isNotBlank() }?.let(::formatTripDate)
    val timeLabel = event.startTime.takeIf { it.isNotBlank() }?.let(::formatDisplayTime)
    return listOfNotNull(dateLabel, timeLabel).joinToString(" · ").takeIf { it.isNotBlank() }
}

private fun extractReservationCity(address: String?): String? {
    val parts = address
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    return when {
        parts.size >= 3 -> parts[parts.size - 2]
        parts.size == 2 -> parts[0]
        else -> null
    }?.takeIf { it.isNotBlank() }
}
