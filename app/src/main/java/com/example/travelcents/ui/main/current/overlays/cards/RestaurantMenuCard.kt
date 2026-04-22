package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_ALLERGEN_NOTE
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_CUISINE
import com.example.travelcents.data.trip.model.ATTR_DIETARY_TAGS
import com.example.travelcents.data.trip.model.ATTR_MENU_HIGHLIGHTS
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

private data class RestaurantMenuHighlight(
    val name: String,
    val price: String?
)

@Composable
fun RestaurantMenuCard(event: TravelEvent) {
    val uriHandler = LocalUriHandler.current
    val menuUrl = event.detailValue(ATTR_MENU_URL, "yelp_menu_url")?.takeIf { it.isNotBlank() }
    val yelpUrl = event.detailValue(ATTR_YELP_URL)?.takeIf { it.isNotBlank() }
    val dietaryTags = readDietaryTags(event)
    val highlights = readMenuHighlights(event)
    val allergenNote = event.detailValue(ATTR_ALLERGEN_NOTE, "allergen_note")?.takeIf { it.isNotBlank() }
    val menuDestination = menuUrl ?: yelpUrl

    if (menuUrl == null && dietaryTags.isEmpty() && highlights.isEmpty()) return

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Menu",
            title = event.detailValue(ATTR_CUISINE, "cuisine", ATTR_CATEGORIES, "categories")
                ?.takeIf { it.isNotBlank() }
                ?: "What's on the menu"
        )

        if (dietaryTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(
                badges = dietaryTags,
                accent = CardCoral
            )
        }

        allergenNote?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = CardTextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        if (highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "FEATURED DISHES",
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 2.dp)
            ) {
                items(highlights) { highlight ->
                    RestaurantMenuHighlightCard(highlight = highlight)
                }
            }
        }

        menuDestination?.let {
            Spacer(modifier = Modifier.height(14.dp))
            DetailLinkRow(
                label = "Menu",
                value = if (menuUrl != null) "View full menu" else "Browse on Yelp",
                onClick = { uriHandler.openUri(it) },
                accent = CardCoral
            )
        }
    }
}

@Composable
private fun RestaurantMenuHighlightCard(highlight: RestaurantMenuHighlight) {
    Column(
        modifier = Modifier
            .widthIn(min = 156.dp, max = 220.dp)
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = highlight.name,
            color = CardText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        highlight.price?.let {
            Text(
                text = it,
                color = CardCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun readDietaryTags(event: TravelEvent): List<String> {
    return event.detailValue(ATTR_DIETARY_TAGS, "dietary_tags")
        ?.split(',')
        .orEmpty()
        .mapNotNull { rawTag ->
            normalizeDietaryTag(rawTag)?.let(::dietaryTagLabel)
        }
        .distinct()
}

private fun normalizeDietaryTag(rawTag: String): String? {
    return rawTag.trim()
        .lowercase(Locale.US)
        .replace('_', '-')
        .takeIf { it.isNotBlank() }
}

private fun dietaryTagLabel(tag: String): String {
    return when (tag) {
        "vegan" -> "Vegan"
        "vegetarian" -> "Vegetarian"
        "gluten-free" -> "Gluten-free"
        "halal" -> "Halal"
        "kosher" -> "Kosher"
        else -> tag.split('-')
            .joinToString("-") { segment ->
                segment.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                }
            }
    }
}

private fun readMenuHighlights(event: TravelEvent): List<RestaurantMenuHighlight> {
    return event.detailValue(ATTR_MENU_HIGHLIGHTS, "menu_highlights")
        ?.split('|')
        .orEmpty()
        .mapNotNull { rawEntry ->
            val entry = rawEntry.trim()
            if (entry.isBlank()) return@mapNotNull null

            val name = entry.substringBefore(';').trim().ifBlank { return@mapNotNull null }
            val price = entry.substringAfter(';', "").trim().takeIf { it.isNotBlank() }
            RestaurantMenuHighlight(name = name, price = price)
        }
}
