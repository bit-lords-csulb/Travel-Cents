package com.example.travelcents.data.sync

import com.example.travelcents.data.media.remoteMediaUrls
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

internal object TripHydrationMediaCollector {
    fun collectUrls(
        events: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ): List<String> {
        return buildSet {
            events.forEach { event ->
                addAll(event.remoteMediaUrls())
                event.detailValue(ATTR_STATIC_MAP_URL)
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
            optionsByEvent.values.flatten().forEach { option ->
                addAll(option.remoteMediaUrls())
                option.detailValue(ATTR_STATIC_MAP_URL)
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }.toList()
    }
}
