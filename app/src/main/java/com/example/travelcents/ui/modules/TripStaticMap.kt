package com.example.travelcents.ui.modules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.media.StaticMapUrlFactory
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

const val DEFAULT_STATIC_MAP_ZOOM = 14
const val MIN_STATIC_MAP_ZOOM = 10
const val MAX_STATIC_MAP_ZOOM = 16

fun adjacentStaticMapZoomLevels(zoom: Int): List<Int> {
    val clampedZoom = zoom.coerceIn(MIN_STATIC_MAP_ZOOM, MAX_STATIC_MAP_ZOOM)
    return buildList {
        if (clampedZoom > MIN_STATIC_MAP_ZOOM) add(clampedZoom - 1)
        if (clampedZoom < MAX_STATIC_MAP_ZOOM) add(clampedZoom + 1)
    }
}

fun TravelEvent.staticMapCoordinates(): Pair<Double, Double>? {
    val latitude = detailValue(ATTR_LATITUDE)?.toDoubleOrNull() ?: return null
    val longitude = detailValue(ATTR_LONGITUDE)?.toDoubleOrNull() ?: return null
    return latitude to longitude
}

fun TravelEvent.hasStaticMapSource(): Boolean {
    return staticMapCoordinates() != null || !detailValue(ATTR_STATIC_MAP_URL).isNullOrBlank()
}

fun TravelEvent.canAdjustStaticMapZoom(): Boolean = staticMapCoordinates() != null

fun TravelEvent.staticMapUrl(zoom: Int = DEFAULT_STATIC_MAP_ZOOM): String? {
    val coordinates = staticMapCoordinates()
    if (coordinates != null) {
        val (latitude, longitude) = coordinates
        return StaticMapUrlFactory.buildUrl(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom.coerceIn(MIN_STATIC_MAP_ZOOM, MAX_STATIC_MAP_ZOOM)
        )
    }
    return detailValue(ATTR_STATIC_MAP_URL)?.takeIf {
        it.isNotBlank() && zoom == DEFAULT_STATIC_MAP_ZOOM
    }
}

fun TravelEvent.staticMapModel(
    context: android.content.Context,
    zoom: Int = DEFAULT_STATIC_MAP_ZOOM
): String? {
    val url = staticMapUrl(zoom)?.takeIf { it.isNotBlank() } ?: return null
    return ImageCacheManager.resolveCachedMediaUrl(context, itineraryId, url)
}

suspend fun prefetchStaticMaps(
    context: android.content.Context,
    events: List<TravelEvent>,
    zoomLevels: Iterable<Int> = listOf(DEFAULT_STATIC_MAP_ZOOM)
) {
    val normalizedZoomLevels = zoomLevels
        .map { it.coerceIn(MIN_STATIC_MAP_ZOOM, MAX_STATIC_MAP_ZOOM) }
        .distinct()

    events
        .groupBy { it.itineraryId }
        .forEach { (tripId, tripEvents) ->
            if (tripId.isBlank()) return@forEach
            val urls = tripEvents.flatMap { event ->
                normalizedZoomLevels.mapNotNull { zoom ->
                    event.staticMapUrl(zoom)?.takeIf(String::isNotBlank)
                }
            }
            if (urls.isNotEmpty()) {
                ImageCacheManager.cacheTripMedia(context, tripId, urls)
            }
        }
}

@Composable
fun rememberStaticMapModel(
    event: TravelEvent,
    zoom: Int = DEFAULT_STATIC_MAP_ZOOM
): String? {
    val context = LocalContext.current
    val staticMapUrl = remember(event.eventId, zoom) {
        event.staticMapUrl(zoom)?.takeIf { it.isNotBlank() }
    }
    var model by remember(event.eventId, staticMapUrl) {
        mutableStateOf(event.staticMapModel(context, zoom))
    }

    LaunchedEffect(event.eventId, event.itineraryId, staticMapUrl, zoom) {
        if (staticMapUrl == null) {
            model = null
            return@LaunchedEffect
        }
        val adjacentZooms = adjacentStaticMapZoomLevels(zoom)
        if (adjacentZooms.isNotEmpty()) {
            prefetchStaticMaps(context, listOf(event), adjacentZooms)
        }
        model = event.staticMapModel(context, zoom)
    }

    return model
}
