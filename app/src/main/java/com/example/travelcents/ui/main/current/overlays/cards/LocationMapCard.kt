package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.runtime.Composable

@Composable
fun LocationMapCard(
    title: String = "Where this event happens",
    locationLabel: String,
    staticMapModel: String?,
    embeddedMapUrl: String?,
    onOpenMaps: () -> Unit
) {
    StaticMapCard(
        title = title,
        locationLabel = locationLabel,
        staticMapModel = staticMapModel,
        embeddedMapUrl = embeddedMapUrl,
        onOpenMaps = onOpenMaps
    )
}
