package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.modules.DEFAULT_STATIC_MAP_ZOOM
import com.example.travelcents.ui.modules.MAX_STATIC_MAP_ZOOM
import com.example.travelcents.ui.modules.MIN_STATIC_MAP_ZOOM
import com.example.travelcents.ui.modules.canAdjustStaticMapZoom
import com.example.travelcents.ui.modules.rememberStaticMapModel

@Composable
fun StaticMapCard(
    title: String,
    event: TravelEvent,
    locationLabel: String,
    onOpenMaps: () -> Unit
) {
    val canAdjustZoom = remember(event.eventId) { event.canAdjustStaticMapZoom() }
    var zoomLevel by remember(event.eventId) { mutableStateOf(DEFAULT_STATIC_MAP_ZOOM) }
    val staticMapModel = rememberStaticMapModel(event, zoomLevel)

    DetailCardFrame(
        modifier = Modifier.clickable(onClick = onOpenMaps),
        accent = CardSky
    ) {
        DetailCardHeader(
            eyebrow = "Location",
            title = title
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurfaceHigh)
        ) {
            if (!staticMapModel.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = staticMapModel,
                    contentDescription = locationLabel,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        MapPreviewState("Loading map preview...")
                    },
                    error = {
                        MapPreviewState("Map preview unavailable")
                    },
                    success = success@{
                        Box(modifier = Modifier.fillMaxSize()) {
                            with(this@success) {
                                SubcomposeAsyncImageContent()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                androidx.compose.ui.graphics.Color.Transparent,
                                                CardBackground.copy(alpha = 0.18f),
                                                CardBackground.copy(alpha = 0.55f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                )
            } else {
                MapPreviewState("Map preview unavailable")
            }

            if (canAdjustZoom) {
                ZoomControlStack(
                    canZoomIn = zoomLevel < MAX_STATIC_MAP_ZOOM,
                    canZoomOut = zoomLevel > MIN_STATIC_MAP_ZOOM,
                    onZoomIn = {
                        zoomLevel = (zoomLevel + 1).coerceAtMost(MAX_STATIC_MAP_ZOOM)
                    },
                    onZoomOut = {
                        zoomLevel = (zoomLevel - 1).coerceAtLeast(MIN_STATIC_MAP_ZOOM)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = CardTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = locationLabel,
                color = CardTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ZoomControlStack(
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ZoomControlButton(
            icon = Icons.Default.Add,
            contentDescription = "Zoom in",
            enabled = canZoomIn,
            onClick = onZoomIn
        )
        ZoomControlButton(
            icon = Icons.Default.Remove,
            contentDescription = "Zoom out",
            enabled = canZoomOut,
            onClick = onZoomOut
        )
    }
}

@Composable
private fun ZoomControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground.copy(alpha = 0.58f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) CardText else CardTextMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MapPreviewState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(CardSky.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = CardSky
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            color = CardText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
