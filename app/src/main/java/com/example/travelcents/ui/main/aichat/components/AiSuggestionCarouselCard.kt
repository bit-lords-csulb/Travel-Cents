package com.example.travelcents.ui.main.aichat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.SuggestionItem
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.remote.YelpRepository
import com.example.travelcents.ui.main.current.CurrentTripItineraryCard
import com.example.travelcents.ui.main.current.overlays.CurrentTripEventDetailsDialog
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AiSuggestionCarouselCard(
    card: AiChatItem.SuggestionCarouselCard,
    onAdd: (SuggestionItem) -> Unit,
    onBookmark: (SuggestionItem) -> Unit,
    onSkip: (SuggestionItem) -> Unit,
    onLoadMore: () -> Unit,
    tripDestination: String = "",
    modifier: Modifier = Modifier
) {
    var expandedSuggestion by remember(card.id) { mutableStateOf<SuggestionItem?>(null) }
    var expandedEvent by remember(card.id) { mutableStateOf<TravelEvent?>(null) }
    var expandedReviews by remember(card.id) { mutableStateOf<List<YelpReview>>(emptyList()) }
    var expandedReviewsLoading by remember(card.id) { mutableStateOf(false) }
    var fadingSuggestionIds by remember(card.id) { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    fun fadeThenReplace(suggestion: SuggestionItem, action: () -> Unit) {
        if (suggestion.id in fadingSuggestionIds) return
        fadingSuggestionIds = fadingSuggestionIds + suggestion.id
        scope.launch {
            delay(180)
            action()
            fadingSuggestionIds = fadingSuggestionIds - suggestion.id
        }
    }

    LaunchedEffect(expandedSuggestion?.id) {
        val suggestion = expandedSuggestion
        expandedEvent = suggestion?.rawEvent
        expandedReviews = emptyList()
        expandedReviewsLoading = false

        val yelpId = suggestion
            ?.rawEvent
            ?.detailValue(DETAIL_YELP_ID)
            ?.takeIf(String::isNotBlank)
            ?: return@LaunchedEffect
        if (suggestion.source.equals("demo", ignoreCase = true)) return@LaunchedEffect

        expandedReviewsLoading = true
        val enrichedEvent = runCatching {
            YelpRepository.enrichYelpBackedEvent(suggestion.rawEvent)?.event
        }.getOrNull()
        if (expandedSuggestion?.id == suggestion.id && enrichedEvent != null) {
            expandedEvent = enrichedEvent
        }

        val reviews = runCatching {
            YelpRepository.getBusinessReviews(yelpId)
        }.getOrDefault(emptyList())
        if (expandedSuggestion?.id == suggestion.id) {
            expandedReviews = reviews
            expandedReviewsLoading = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TripWizardColors.ContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = card.label,
                color = DeepSea5,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TravelCentsFonts.Headline
            )

            if (card.suggestions.isEmpty()) {
                Text(
                    text = if (card.exhausted) "No more options for this slot." else "Looking for options...",
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }

            card.suggestions.forEachIndexed { index, suggestion ->
                AnimatedVisibility(
                    visible = suggestion.id !in fadingSuggestionIds,
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SuggestionRow(
                        suggestion = suggestion,
                        isLast = index == card.suggestions.lastIndex,
                        onOpenDetails = { expandedSuggestion = suggestion },
                        onAdd = { onAdd(suggestion) },
                        onBookmark = { fadeThenReplace(suggestion) { onBookmark(suggestion) } },
                        onSkip = { fadeThenReplace(suggestion) { onSkip(suggestion) } }
                    )
                }
            }

            if (card.hasMore && !card.exhausted) {
                Surface(
                    onClick = onLoadMore,
                    color = TripWizardColors.Blue.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Load more options",
                        color = TripWizardColors.Blue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontFamily = TravelCentsFonts.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    expandedSuggestion?.let { suggestion ->
        val event = expandedEvent ?: suggestion.rawEvent
        CurrentTripEventDetailsDialog(
            event = event,
            tripDestination = tripDestination.ifBlank { suggestion.address },
            tripAdults = 1,
            tripChildren = 0,
            currentOptions = emptyList(),
            yelpReviews = expandedReviews,
            reviewsLoading = expandedReviewsLoading,
            canEditTrip = false,
            canShowAlternatives = false,
            onDismiss = {
                expandedSuggestion = null
                expandedEvent = null
                expandedReviews = emptyList()
                expandedReviewsLoading = false
            },
            onEdit = {},
            onDelete = {}
        )
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SuggestionItem,
    isLast: Boolean,
    onOpenDetails: () -> Unit,
    onAdd: () -> Unit,
    onBookmark: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurrentTripItineraryCard(
            event = suggestion.rawEvent,
            isLast = isLast,
            onCardClick = onOpenDetails
        )

        Row(
            modifier = Modifier.padding(start = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionActionButton("Add", Color(0xFF4DB6AC), onAdd)
            SuggestionActionButton("Bookmark", TripWizardColors.Blue, onBookmark)
            SuggestionActionButton("Skip", Color(0xFFFF8A65), onSkip)
        }
    }
}

@Composable
private fun SuggestionActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontFamily = TravelCentsFonts.Body,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
