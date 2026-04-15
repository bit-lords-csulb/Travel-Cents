package com.example.travelcents.ui.main.current

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea5
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CurrentTripOverlayHost(
    uiState: CurrentTripUiState,
    eventOptions: Map<String, List<com.example.travelcents.data.trip.model.EventOption>>,
    rejectedOptions: Map<String, Set<String>>,
    yelpReviews: Map<String, List<com.example.travelcents.data.trip.model.YelpReview>>,
    reviewsLoading: Set<String>,
    shareTargets: List<ShareTarget>,
    selectedEventId: String?,
    editorPlan: EditablePlan?,
    deleteCandidate: EditablePlan?,
    optionsPanelEventId: String?,
    showShareSheet: Boolean,
    shareConfirmation: String?,
    onSelectedEventIdChange: (String?) -> Unit,
    onEditorPlanChange: (EditablePlan?) -> Unit,
    onDeleteCandidateChange: (EditablePlan?) -> Unit,
    onOptionsPanelEventIdChange: (String?) -> Unit,
    onShowShareSheetChange: (Boolean) -> Unit,
    onShareConfirmationChange: (String?) -> Unit,
    onSavePlan: (EditablePlan) -> Unit,
    onDeletePlan: (EditablePlan) -> Unit,
    onShareTrip: (ShareTarget) -> Unit,
    onSelectOption: (String, String) -> Unit,
    onRejectOption: (String, String) -> Unit
) {
    selectedEventId?.let { eventId ->
        val event = uiState.events.firstOrNull { it.eventId == eventId }
        if (event != null) {
            val yelpId = event.details["yelp_id"].orEmpty()
            CurrentTripEventDetailsDialog(
                event = event,
                currentOptions = eventOptions[eventId].orEmpty(),
                yelpReviews = yelpReviews[yelpId].orEmpty(),
                reviewsLoading = reviewsLoading.contains(yelpId),
                onDismiss = { onSelectedEventIdChange(null) },
                onEdit = {
                    onSelectedEventIdChange(null)
                    onEditorPlanChange(event.toEditablePlan())
                },
                onDelete = {
                    onSelectedEventIdChange(null)
                    onDeleteCandidateChange(event.toEditablePlan())
                },
                onAlternatives = if (eventOptions[eventId].orEmpty().size > 1) {
                    {
                        onSelectedEventIdChange(null)
                        onOptionsPanelEventIdChange(eventId)
                    }
                } else {
                    null
                }
            )
        }
    }

    deleteCandidate?.let { plan ->
        DeletePlanDialog(
            plan = plan,
            onDismiss = { onDeleteCandidateChange(null) },
            onConfirmDelete = {
                onDeletePlan(plan)
                if (editorPlan?.eventId == plan.eventId) {
                    onEditorPlanChange(null)
                }
                onDeleteCandidateChange(null)
            }
        )
    }

    editorPlan?.let { plan ->
        val yelpId = plan.existingDetails["yelp_id"].orEmpty()
        CurrentPlanEditorDialog(
            initialPlan = plan,
            currentOptions = eventOptions[plan.eventId].orEmpty(),
            yelpReviews = yelpReviews[yelpId].orEmpty(),
            reviewsLoading = reviewsLoading.contains(yelpId),
            onDismiss = { onEditorPlanChange(null) },
            onSave = {
                onSavePlan(it)
                onEditorPlanChange(null)
            },
            onDelete = { planToDelete ->
                onEditorPlanChange(null)
                onDeleteCandidateChange(planToDelete)
            },
            onAlternatives = {
                onEditorPlanChange(null)
                plan.eventId?.let(onOptionsPanelEventIdChange)
            }
        )
    }

    optionsPanelEventId?.let { eventId ->
        val event = uiState.events.firstOrNull { it.eventId == eventId }
        val options = eventOptions[eventId].orEmpty()
        val rejected = rejectedOptions[eventId].orEmpty()
        if (event != null) {
            val selectedNamesElsewhere = uiState.events
                .filter { it.eventId != eventId && it.type.equals(event.type, ignoreCase = true) }
                .mapNotNull { other ->
                    eventOptions[other.eventId].orEmpty().firstOrNull { it.selected }?.let { opt ->
                        when (other.type.lowercase(Locale.US)) {
                            "hotel" -> opt.details["hotel_name"] ?: opt.details["name"]
                            "restaurant", "dining", "food" -> opt.details["restaurant_name"] ?: opt.details["name"]
                            "flight" -> null
                            else -> opt.details["activity_name"] ?: opt.details["title"] ?: opt.details["name"]
                        }
                    }
                }
                .toSet()
            EventOptionsPanel(
                event = event,
                options = options,
                rejectedIds = rejected,
                onSelect = { optionId -> onSelectOption(eventId, optionId) },
                onReject = { optionId -> onRejectOption(eventId, optionId) },
                onDismiss = { onOptionsPanelEventIdChange(null) },
                selectedNamesElsewhere = selectedNamesElsewhere
            )
        }
    }

    if (showShareSheet) {
        CurrentTripShareSheet(
            targets = shareTargets,
            onShare = { target ->
                onShareTrip(target)
                onShowShareSheetChange(false)
                onShareConfirmationChange("Trip shared to ${target.name}")
            },
            onDismiss = { onShowShareSheetChange(false) }
        )
    }

    shareConfirmation?.let { message ->
        LaunchedEffect(message) {
            delay(2500)
            onShareConfirmationChange(null)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = DeepSea2,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DeepSea3.copy(alpha = 0.3f))
            ) {
                Text(
                    text = message,
                    color = DeepSea5,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

