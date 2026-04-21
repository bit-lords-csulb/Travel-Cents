package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiCuratedTripRow
import com.example.travelcents.data.ai.chat.AiCuratedTripSource
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.ui.main.newTrip.TripWizardColors
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
                    onDurationSelected = { durationDays ->
                        onDurationSelected(starter, durationDays)
                    },
                    onClick = { onTripSelected(starter) }
                )
            }
        }
    }
}

@Composable
private fun AiCuratedTripCard(
    starter: AiCuratedTripStarter,
    onDurationSelected: (Int) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = TripWizardColors.ContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourcePill(source = starter.source)
                Text(
                    text = "${starter.durationDays}d",
                    color = TripWizardColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = TravelCentsFonts.Body
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = starter.title,
                    color = DeepSea5,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TravelCentsFonts.Headline
                )
                Text(
                    text = starter.destination,
                    color = TripWizardColors.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = TravelCentsFonts.Body
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = starter.summary,
                    color = DeepSea4,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = TravelCentsFonts.Body
                )
                Text(
                    text = starter.matchReason,
                    color = DeepSea5.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }

            if (starter.highlights.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    starter.highlights.take(3).forEach { highlight ->
                        DetailPill(
                            label = highlight,
                            selected = false,
                            onClick = null
                        )
                    }
                }
            }

            if (starter.durationOptions.size > 1) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Adjust days",
                        color = DeepSea4,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = TravelCentsFonts.Body
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        starter.durationOptions.sorted().distinct().forEach { option ->
                            DetailPill(
                                label = "${option}d",
                                selected = option == starter.durationDays,
                                onClick = { onDurationSelected(option) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = starter.travelStyle.replaceFirstChar { it.uppercase() },
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TravelCentsFonts.Body
                )
                Text(
                    text = "Use starter",
                    color = TripWizardColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TravelCentsFonts.Body
                )
            }
        }
    }
}

@Composable
private fun SourcePill(source: AiCuratedTripSource) {
    val (label, icon) = when (source) {
        AiCuratedTripSource.FIRESTORE -> "Saved trip" to Icons.Outlined.CollectionsBookmark
        AiCuratedTripSource.SEEDED -> "Curated" to Icons.Outlined.Place
        AiCuratedTripSource.GENERATED -> "Fresh start" to Icons.Outlined.AutoAwesome
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = TripWizardColors.ContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = TripWizardColors.Blue.copy(alpha = 0.24f)
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
                    tint = TripWizardColors.Blue,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = DeepSea5,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TravelCentsFonts.Body
            )
        }
    }
}

@Composable
private fun DetailPill(
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            TripWizardColors.Blue.copy(alpha = 0.16f)
        } else {
            TripWizardColors.ContainerHighest
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                TripWizardColors.Blue.copy(alpha = 0.42f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (selected) TripWizardColors.Blue else DeepSea5.copy(alpha = 0.88f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = TravelCentsFonts.Body
        )
    }
}
