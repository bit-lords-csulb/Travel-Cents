package com.example.travelcents.ui.modules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

fun TravelEvent.staticMapUrl(): String? = detailValue(ATTR_STATIC_MAP_URL)

fun TravelEvent.staticMapModel(context: android.content.Context): String? {
    val url = staticMapUrl()?.takeIf { it.isNotBlank() } ?: return null
    return ImageCacheManager.localPathForUrl(context, itineraryId, url) ?: url
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
                ImageCacheManager.downloadTripImages(context, tripId, urls)
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
