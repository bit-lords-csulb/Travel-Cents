package com.example.travelcents.ui.main.aichat.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.ai.chat.AiChatTripOption
import com.example.travelcents.data.ai.chat.AiSingleEventSuggestion
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToTripBottomSheet(
    suggestion: AiSingleEventSuggestion,
    trips: List<AiChatTripOption>,
    onTripSelected: (AiChatTripOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSea1,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Add to trip",
                color = DeepSea5,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = TravelCentsFonts.Headline
            )
            Text(
                text = "\"${suggestion.headline}\" will be added as an activity.",
                color = DeepSea4,
                fontSize = 13.sp,
                fontFamily = TravelCentsFonts.Body
            )

            when {
                trips.isEmpty() -> EmptyOrLoadingState()
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trips, key = { trip -> "${trip.ownerUid}/${trip.tripId}" }) { trip ->
                        TripRow(
                            trip = trip,
                            onClick = { onTripSelected(trip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                color = TripWizardColors.Blue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Loading your trips...",
                color = DeepSea4,
                fontSize = 12.sp,
                fontFamily = TravelCentsFonts.Body
            )
        }
    }
}

@Composable
private fun TripRow(
    trip: AiChatTripOption,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = TripWizardColors.ContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TripWizardColors.ContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (trip.imageUrl != null) {
                    AsyncImage(
                        model = trip.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = TripWizardColors.Blue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = trip.title.ifBlank { trip.destination.ifBlank { "Untitled trip" } },
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TravelCentsFonts.Body,
                    maxLines = 1
                )
                if (trip.destination.isNotBlank()) {
                    Text(
                        text = trip.destination,
                        color = DeepSea4,
                        fontSize = 12.sp,
                        fontFamily = TravelCentsFonts.Body,
                        maxLines = 1
                    )
                }
                if (trip.dateWindow.isNotBlank()) {
                    Text(
                        text = trip.dateWindow,
                        color = DeepSea4,
                        fontSize = 11.sp,
                        fontFamily = TravelCentsFonts.Body,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
