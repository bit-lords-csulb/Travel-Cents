package com.example.travelcents.ui.main.current.overlays.cards

import android.graphics.Color as AndroidColor
import android.webkit.WebView
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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun StaticMapCard(
    title: String,
    locationLabel: String,
    staticMapModel: String?,
    embeddedMapUrl: String?,
    onOpenMaps: () -> Unit
) {
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
            when {
                !embeddedMapUrl.isNullOrBlank() -> EmbeddedMapPreview(embeddedMapUrl)
                !staticMapModel.isNullOrBlank() -> {
                    val painter = rememberAsyncImagePainter(model = staticMapModel)
                    val state = painter.state
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            MapPreviewState("Loading map preview...")
                        }
                        is AsyncImagePainter.State.Error -> {
                            if (!embeddedMapUrl.isNullOrBlank()) {
                                EmbeddedMapPreview(embeddedMapUrl)
                            } else {
                                MapPreviewState("Map preview unavailable")
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painter,
                                    contentDescription = locationLabel,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
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
                    }
                }
                else -> {
                    MapPreviewState("Map preview unavailable")
                }
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

@Composable
private fun EmbeddedMapPreview(url: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            CardBackground.copy(alpha = 0.08f),
                            CardBackground.copy(alpha = 0.28f)
                        )
                    )
                )
        )
    }
}
