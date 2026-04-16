package com.example.travelcents.data.media

import android.content.Context
import com.example.travelcents.data.trip.model.TravelEvent
import java.util.Locale

fun TravelEvent.selectedMediaUrls(): List<String> = buildList {
    if (imageUrl.isNotBlank()) add(imageUrl)
    addAll(photoUrls.filter { it.isNotBlank() })
}.distinct()

suspend fun prefetchSelectedHotelGalleries(
    context: Context,
    events: List<TravelEvent>
) {
    events
        .asSequence()
        .filter {
            it.type.lowercase(Locale.US) == "hotel" &&
                it.itineraryId.isNotBlank()
        }
        .groupBy { it.itineraryId }
        .forEach { (tripId, hotelEvents) ->
            val urls = hotelEvents
                .flatMap { it.selectedMediaUrls() }
                .distinct()

            if (urls.isNotEmpty()) {
                ImageCacheManager.downloadTripImages(context, tripId, urls)
            }
        }
}
