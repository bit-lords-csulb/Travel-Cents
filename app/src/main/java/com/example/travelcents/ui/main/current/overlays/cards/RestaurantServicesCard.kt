package com.example.travelcents.ui.main.current.overlays.cards

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_PHONE
import com.example.travelcents.data.trip.model.ATTR_WEBSITE_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun RestaurantServicesCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    val phone = event.detailValue(ATTR_PHONE, "phone")?.takeIf { it.isNotBlank() }
    val websiteUrl = event.detailValue(ATTR_WEBSITE_URL, "website", "url")
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { it == event.detailValue(ATTR_YELP_URL) }
        ?.takeUnless { it == event.detailValue(ATTR_MENU_URL, "yelp_menu_url") }
    val yelpUrl = event.detailValue(ATTR_YELP_URL)?.takeIf { it.isNotBlank() }
    if (phone == null && websiteUrl == null && yelpUrl == null) return

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(eyebrow = "Contact", title = "Contact and links")
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            phone?.let {
                DetailLinkRow(
                    label = "Phone",
                    value = it,
                    onClick = { uriHandler.openUri("tel:${phoneDialTarget(it)}") },
                    accent = CardCoral
                )
            }
            websiteUrl?.let {
                DetailLinkRow(
                    label = "Website",
                    value = compactHostLabel(it),
                    onClick = { uriHandler.openUri(it) },
                    accent = CardCoral
                )
            }
            yelpUrl?.let {
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

private fun phoneDialTarget(phone: String): String {
    val sanitized = phone.filter { it.isDigit() || it == '+' || it == ',' || it == ';' }
    return Uri.encode(if (sanitized.isNotBlank()) sanitized else phone)
}
