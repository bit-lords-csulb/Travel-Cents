package com.example.travelcents.ui.main.aichat.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.travelcents.data.ai.chat.AiCuratedTripRow
import com.example.travelcents.data.ai.chat.AiCuratedTripSource
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiCuratedTripRow(
    row: AiCuratedTripRow,
    onDurationSelected: (AiCuratedTripStarter, Int) -> Unit,
    onTripSelected: (AiCuratedTripStarter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = row.title,
                color = DeepSea5,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TravelCentsFonts.Headline
            )
            Text(
                text = row.subtitle,
                color = DeepSea4,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = TravelCentsFonts.Body
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(row.trips, key = { starter -> starter.id }) { starter ->
                AiCuratedTripCard(
                    starter = starter,
                    onClick = { onTripSelected(starter) }
                )
            }
        }
    }
}

@Composable
private fun AiCuratedTripCard(
    starter: AiCuratedTripStarter,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = starter.heroImageUrl
    val imageRequest = remember(imageUrl) {
        imageUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .listener(
                    onError = { request, result ->
                        Log.w(
                            "AiCuratedTripCard",
                            "Hero image failed for '${starter.destination}' url='${request.data}': ${result.throwable.message}"
                        )
                    }
                )
                .build()
        }
    }

    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(320.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = TripWizardColors.ContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = starter.destination,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(colors = listOf(DeepSea3, DeepSea2))
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourcePill(source = starter.source)
                DurationPill(durationDays = starter.durationDays)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = starter.destination,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = TravelCentsFonts.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = starter.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TravelCentsFonts.Headline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (starter.matchReason.isNotBlank()) {
                    Text(
                        text = starter.matchReason,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = TravelCentsFonts.Body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcePill(source: AiCuratedTripSource) {
    val (label, icon) = when (source) {
        AiCuratedTripSource.FIRESTORE -> "Saved trip" to Icons.Outlined.CollectionsBookmark
        AiCuratedTripSource.SEEDED -> "Popular trip" to Icons.Outlined.Place
        AiCuratedTripSource.GENERATED -> "Fresh start" to Icons.Outlined.AutoAwesome
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TravelCentsFonts.Body
            )
        }
    }
}

@Composable
private fun DurationPill(durationDays: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.92f)
    ) {
        Text(
            text = "${durationDays}d",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = DeepSea2,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = TravelCentsFonts.Body
        )
    }
}
