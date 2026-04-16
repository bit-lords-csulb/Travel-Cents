package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.runtime.Composable

@Composable
fun LocationMapCard(
    locationLabel: String,
    staticMapModel: String?,
    onOpenMaps: () -> Unit
) {
    StaticMapCard(
        title = "Where this event happens",
        locationLabel = locationLabel,
        staticMapModel = staticMapModel,
        onOpenMaps = onOpenMaps
    )
}
