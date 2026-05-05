package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.ai.chat.AiSingleEventSuggestion
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiSingleEventCard(
    suggestion: AiSingleEventSuggestion,
    onAddToTrip: () -> Unit,
    onOpenTickets: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = TripWizardColors.ContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.16f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(TripWizardColors.ContainerHighest)
            ) {
                if (suggestion.imageUrl != null) {
                    AsyncImage(
                        model = suggestion.imageUrl,
                        contentDescription = suggestion.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xCC000000)
                                    )
                                )
                            )
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = TripWizardColors.Blue.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = suggestion.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TravelCentsFonts.Body
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = suggestion.headline,
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = TravelCentsFonts.Headline,
                        maxLines = 2
                    )
                    if (suggestion.venue.isNotBlank()) {
                        Text(
                            text = suggestion.venue,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontFamily = TravelCentsFonts.Body,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (suggestion.dateLine.isNotBlank()) {
                    InfoRow(
                        icon = @Composable {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = TripWizardColors.Blue,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = suggestion.dateLine
                    )
                }
                if (suggestion.cityLine.isNotBlank()) {
                    InfoRow(
                        icon = @Composable {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                tint = TripWizardColors.Blue,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = suggestion.cityLine
                    )
                }
                if (suggestion.priceLine.isNotBlank()) {
                    Text(
                        text = suggestion.priceLine,
                        color = DeepSea5.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TravelCentsFonts.Body
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onAddToTrip),
                        shape = RoundedCornerShape(14.dp),
                        color = TripWizardColors.Blue
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add to trip",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = TravelCentsFonts.Body
                            )
                        }
                    }
                    if (suggestion.bookingUrl != null) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onOpenTickets),
                            shape = RoundedCornerShape(14.dp),
                            color = TripWizardColors.ContainerHighest,
                            border = BorderStroke(
                                width = 1.dp,
                                color = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.16f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Open tickets",
                                    color = TripWizardColors.Blue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TravelCentsFonts.Body
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Not interested",
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    color = DeepSea4,
                    fontSize = 11.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = text,
            color = DeepSea5,
            fontSize = 13.sp,
            fontFamily = TravelCentsFonts.Body
        )
    }
}
