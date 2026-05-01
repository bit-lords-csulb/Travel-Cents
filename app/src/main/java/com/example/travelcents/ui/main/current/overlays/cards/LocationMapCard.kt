package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.runtime.Composable
import com.example.travelcents.data.trip.model.TravelEvent

@Composable
fun LocationMapCard(
    title: String = "Where this event happens",
    event: TravelEvent,
    locationLabel: String,
    onOpenMaps: () -> Unit
) {
    StaticMapCard(
        title = title,
        event = event,
        locationLabel = locationLabel,
        onOpenMaps = onOpenMaps
    )
}
