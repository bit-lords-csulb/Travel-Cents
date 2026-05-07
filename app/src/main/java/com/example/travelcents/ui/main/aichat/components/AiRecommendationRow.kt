package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

data class AiRecommendationCardModel(
    val id: String,
    val headline: String,
    val supporting: String,
    val meta: String,
    val kind: String,
    val imageUrl: String? = null,
    val actionLabels: List<String> = emptyList(),
    val actionsEnabled: Boolean = false
)

@Composable
fun AiRecommendationRow(
    title: String,
    subtitle: String,
    recommendations: List<AiRecommendationCardModel>,
    onRecommendationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDone: Boolean = false,
    collapsedLabel: String = title,
    onDoneBrowsing: (() -> Unit)? = null
) {
    var isCollapsed by remember(isDone) { mutableStateOf(isDone) }

    if (isCollapsed) {
        CollapsedCarouselRow(
            label = collapsedLabel,
            itemCount = recommendations.size,
            onExpand = { isCollapsed = false },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = DeepSea5,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TravelCentsFonts.Headline
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = DeepSea4,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = TravelCentsFonts.Body
                        )
                    }
                }
                if (isDone) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Collapse",
                        tint = DeepSea4,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { isCollapsed = true }
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(recommendations, key = { recommendation -> recommendation.id }) { recommendation ->
                    RecommendationCard(
                        recommendation = recommendation,
                        onClick = { onRecommendationSelected(recommendation.id) }
                    )
                }
            }

            if (!isDone && onDoneBrowsing != null) {
                TextButton(
                    onClick = onDoneBrowsing,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Done browsing",
                        color = TripWizardColors.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TravelCentsFonts.Body
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedCarouselRow(
    label: String,
    itemCount: Int,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(16.dp),
        color = TripWizardColors.ContainerLow,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = TravelCentsFonts.Headline
                )
                Text(
                    text = "$itemCount places found",
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = DeepSea4,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: AiRecommendationCardModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .width(244.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = TripWizardColors.ContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (recommendation.imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(TripWizardColors.ContainerHighest)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(recommendation.imageUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = recommendation.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = TripWizardColors.ContainerHighest,
                        border = BorderStroke(
                            width = 1.dp,
                            color = TripWizardColors.Blue.copy(alpha = 0.24f)
                        )
                    ) {
                        Text(
                            text = recommendation.kind,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = TripWizardColors.Blue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = TravelCentsFonts.Body
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = recommendation.headline,
                        color = DeepSea5,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TravelCentsFonts.Headline
                    )
                    if (recommendation.supporting.isNotBlank()) {
                        Text(
                            text = recommendation.supporting,
                            color = DeepSea4,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = TravelCentsFonts.Body
                        )
                    }
                }

                Text(
                    text = recommendation.meta,
                    color = DeepSea5.copy(alpha = 0.76f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = TravelCentsFonts.Body
                )

                if (recommendation.actionLabels.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recommendation.actionLabels.forEach { label ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = TripWizardColors.ContainerHighest.copy(alpha = 0.72f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = if (recommendation.actionsEnabled) {
                                        TripWizardColors.Blue
                                    } else {
                                        DeepSea4
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = TravelCentsFonts.Body
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Use this",
                        color = TripWizardColors.Blue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TravelCentsFonts.Body
                    )
                }
            }
        }
    }
}