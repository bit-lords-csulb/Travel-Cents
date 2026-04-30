package com.example.travelcents.ui.main.newTrip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.ui.components.TcButton
import com.example.travelcents.ui.theme.TravelCentsFonts

private val GenBackground = Color(0xFF010E24)
private val GenSurface = Color(0xFF0B203D)
private val GenBlue = Color(0xFF64B5F6)
private val GenGreen = Color(0xFF4CAF50)
private val GenPending = Color(0xFF3B4861)
private val GenOnSurface = Color(0xFFDBE6FF)
private val GenOnSurfaceVariant = Color(0xFF9EABC8)

private val generationSteps = listOf(
    GenerationStep.CRAFTING_ITINERARY to "Crafting your itinerary",
    GenerationStep.SEARCHING_FLIGHTS to "Searching for flights",
    GenerationStep.FINDING_HOTELS to "Finding hotels",
    GenerationStep.FINDING_RESTAURANTS to "Finding restaurants for each day",
    GenerationStep.FINDING_ACTIVITIES to "Discovering activities & events",
    GenerationStep.DOWNLOADING_IMAGES to "Saving photos for offline use",
    GenerationStep.SAVING to "Saving your trip"
)

@Composable
fun TripGeneratingPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onTripReady: (TripKey) -> Unit
) {
    val currentStep by viewModel.generationStep.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val generatedItinerary = (uiState as? TripUiState.Success)?.itinerary

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(GenBackground),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = "Generating Your Trip",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GenOnSurface,
            fontFamily = TravelCentsFonts.Headline
        )
        Text(
            text = "Sit tight while we plan your adventure",
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 13.sp,
            color = GenOnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            generationSteps.forEach { (step, label) ->
                val isDone = currentStep.ordinal > step.ordinal || currentStep == GenerationStep.COMPLETE
                val isActive = currentStep == step
                AnimatedVisibility(
                    visible = currentStep.ordinal >= step.ordinal,
                    enter = fadeIn() + expandVertically()
                ) {
                    StepCard(label = label, isDone = isDone, isActive = isActive)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        AnimatedVisibility(
            visible = uiState is TripUiState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut()
        ) {
            Text(
                text = (uiState as? TripUiState.Error)?.message.orEmpty(),
                modifier = Modifier.padding(horizontal = 24.dp),
                color = Color(0xFFFF716C),
                fontSize = 14.sp
            )
        }

        AnimatedVisibility(
            visible = currentStep == GenerationStep.COMPLETE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TcButton(
                onClick = {
                    generatedItinerary?.let { itinerary ->
                        onTripReady(
                            TripKey(
                                ownerUid = itinerary.ownerUid.ifBlank { itinerary.userId },
                                tripId = itinerary.itineraryId
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "View My Trip",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
}

@Composable
private fun StepCard(label: String, isDone: Boolean, isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GenSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> GenGreen.copy(alpha = 0.2f)
                                isActive -> GenBlue.copy(alpha = 0.2f)
                                else -> GenPending.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isDone -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GenGreen
                        )

                        isActive -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = GenBlue
                        )

                        else -> Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = GenPending
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isDone -> GenOnSurface
                        isActive -> GenOnSurface
                        else -> GenOnSurfaceVariant
                    }
                )
            }

            when {
                isDone -> Text(
                    text = "Done",
                    color = GenGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                isActive -> Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = null,
                    tint = Color.Transparent
                )
            }
        }
    }
}

