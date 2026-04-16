package com.example.travelcents.ui.main.shared

import android.app.Application
import com.example.travelcents.data.media.prefetchSelectedHotelGalleries
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.withSelectedOption
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.ui.modules.prefetchStaticMaps

class TripMediaDetailPipeline(
    private val application: Application
) {

    fun applySelectedOptions(
        events: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>,
        sortEvents: (List<TravelEvent>) -> List<TravelEvent>
    ): List<TravelEvent> {
        return sortEvents(
            events.map { event ->
                mergeEventWithOptions(event, optionsByEvent[event.eventId].orEmpty())
            }
        )
    }

    fun mergeEventWithOptions(
        event: TravelEvent,
        options: List<EventOption>
    ): TravelEvent {
        val baseEvent = event.copy(options = options)
        val selectedOption = options.firstOrNull { it.selected }
        return if (selectedOption != null) {
            baseEvent.withSelectedOption(selectedOption).copy(options = options)
        } else {
            baseEvent
        }
    }

    suspend fun prefetchSharedMedia(events: List<TravelEvent>) {
        prefetchSelectedHotelGalleries(application, events)
        prefetchStaticMaps(application, events)
    }

    fun yelpEnrichmentTargetId(
        event: TravelEvent,
        options: List<EventOption>,
        inFlightIds: Set<String>,
        forceRefresh: Boolean
    ): String? {
        val yelpId = options.firstOrNull { it.selected }
            ?.detailValue(DETAIL_YELP_ID)
            ?.takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() }
            ?: return null

        if (!forceRefresh && yelpId in inFlightIds) return null
        if (!forceRefresh && !YelpRepository.needsEventEnrichment(event, options)) return null
        return yelpId
    }
}
