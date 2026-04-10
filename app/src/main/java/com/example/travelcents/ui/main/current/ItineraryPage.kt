package com.example.travelcents.ui.main.current

import androidx.compose.runtime.Composable
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.TravelEvent

@Composable
fun ItineraryPage(
    events: List<TravelEvent>,
    eventOptions: Map<String, List<EventOption>>,
    rejectedOptions: Map<String, Set<String>>,
    jiggleMode: Boolean,
    onEventClick: (TravelEvent) -> Unit,
    onDeleteClick: (TravelEvent) -> Unit,
    onOpenAlternatives: (String) -> Unit,
    onMoveEvent: (String, String, String, Int) -> Unit,
    onPersistEventPlacements: (Set<String>) -> Unit
) {
    UnifiedItineraryContent(
        events = events,
        eventOptions = eventOptions,
        rejectedOptions = rejectedOptions,
        jiggleMode = jiggleMode,
        onEventClick = onEventClick,
        onDeleteClick = onDeleteClick,
        onOpenAlternatives = onOpenAlternatives,
        onMoveEvent = onMoveEvent,
        onPersistEventPlacements = onPersistEventPlacements
    )
}
