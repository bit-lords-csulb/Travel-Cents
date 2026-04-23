package com.example.travelcents.ui.modules

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

fun TravelEvent.staticMapUrl(): String? = detailValue(ATTR_STATIC_MAP_URL)

fun TravelEvent.staticMapModel(context: android.content.Context): String? {
    val url = staticMapUrl()?.takeIf { it.isNotBlank() } ?: return null
    return ImageCacheManager.resolveCachedMediaUrl(context, itineraryId, url)
}

fun TravelEvent.embeddedMapUrl(): String? {
    val latitude = detailValue(ATTR_LATITUDE)?.toDoubleOrNull() ?: return null
    val longitude = detailValue(ATTR_LONGITUDE)?.toDoubleOrNull() ?: return null
    val lat = String.format(Locale.US, "%.6f", latitude)
    val lon = String.format(Locale.US, "%.6f", longitude)
    val left = String.format(Locale.US, "%.6f", longitude - 0.015)
    val right = String.format(Locale.US, "%.6f", longitude + 0.015)
    val top = String.format(Locale.US, "%.6f", latitude + 0.01)
    val bottom = String.format(Locale.US, "%.6f", latitude - 0.01)
    val bbox = Uri.encode("$left,$bottom,$right,$top")
    val marker = Uri.encode("$lat,$lon")
    return "https://www.openstreetmap.org/export/embed.html?bbox=$bbox&layer=mapnik&marker=$marker"
}

suspend fun prefetchStaticMaps(
    context: android.content.Context,
    events: List<TravelEvent>
) {
    events
        .groupBy { it.itineraryId }
        .forEach { (tripId, tripEvents) ->
            if (tripId.isBlank()) return@forEach
            val urls = tripEvents.mapNotNull { it.staticMapUrl()?.takeIf(String::isNotBlank) }
            if (urls.isNotEmpty()) {
                ImageCacheManager.cacheTripMedia(context, tripId, urls)
            }
        }
}

@Composable
fun rememberStaticMapModel(event: TravelEvent): String? {
    val context = LocalContext.current
    val staticMapUrl = remember(event) { event.staticMapUrl()?.takeIf { it.isNotBlank() } }
    var model by remember(event.eventId, staticMapUrl) { mutableStateOf(event.staticMapModel(context)) }

    LaunchedEffect(event.eventId, event.itineraryId, staticMapUrl) {
        if (staticMapUrl == null) {
            model = null
            return@LaunchedEffect
        }
        prefetchStaticMaps(context, listOf(event))
        model = event.staticMapModel(context)
    }

    return model
}
