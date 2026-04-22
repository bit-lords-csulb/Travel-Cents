package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_OFFER_COUNT
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import java.util.Locale

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookingOfferLogo(logo = offer.logo, source = offer.source)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = offer.source,
                color = CardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Official property page",
                color = CardTextMuted,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CardLavender)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Book",
                color = CardBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BookingOfferLogo(
    logo: String?,
    source: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBackground),
        contentAlignment = Alignment.Center
    ) {
        val logoUrl = logo?.takeIf { it.isNotBlank() }
        if (logoUrl != null) {
            val painter = rememberAsyncImagePainter(model = logoUrl)
            if (painter.state is AsyncImagePainter.State.Error) {
                BookingOfferLogoFallback(source)
            } else {
                Image(
                    painter = painter,
                    contentDescription = source,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            BookingOfferLogoFallback(source)
        }
    }
}

@Composable
private fun BookingOfferLogoFallback(source: String) {
    val initial = source.firstOrNull()?.titlecase(Locale.US) ?: "?"
    if (initial == "?") {
        Icon(
            imageVector = Icons.Default.Storefront,
            contentDescription = null,
            tint = CardLavender,
            modifier = Modifier.size(18.dp)
        )
    } else {
        Text(
            text = initial,
            color = CardLavender,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
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
