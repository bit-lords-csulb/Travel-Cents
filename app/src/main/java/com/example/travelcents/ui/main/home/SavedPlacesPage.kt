package com.example.travelcents.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.social.model.BookmarkedPlace
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.ui.main.current.TripEventCard
import com.example.travelcents.ui.main.current.overlays.CurrentTripEventDetailsDialog
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private val Primary = Color(0xFF64B5F6)

@Composable
fun SavedPlacesPage(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks = uiState.bookmarks

    var expandedBookmark by remember { mutableStateOf<BookmarkedPlace?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(DeepSea1)) {
        Column(modifier = Modifier.fillMaxSize()) {
            SavedPlacesHeader(onBack = onBack)

            if (bookmarks.isEmpty()) {
                SavedPlacesEmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        TripEventCard(
                            event = bookmark.toTravelEvent(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedBookmark = bookmark },
                            actionIcon = Icons.Default.DeleteOutline,
                            actionContentDescription = "Remove bookmark",
                            actionIconTint = Color(0xFFE77D90),
                            onActionClick = { homeViewModel.removeBookmark(bookmark.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        expandedBookmark?.let { place ->
            CurrentTripEventDetailsDialog(
                event = place.toTravelEvent(),
                tripDestination = place.area,
                tripAdults = 1,
                tripChildren = 0,
                currentOptions = emptyList<com.example.travelcents.data.trip.model.EventOption>(),
                yelpReviews = emptyList<com.example.travelcents.data.trip.model.YelpReview>(),
                reviewsLoading = false,
                canEditTrip = false,
                canShowAlternatives = false,
                onDismiss = { expandedBookmark = null },
                onEdit = {},
                onDelete = {}
            )
        }
    }
}

@Composable
private fun SavedPlacesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Primary
            )
        }
        Text(
            text = "Saved Places",
            color = DeepSea5,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.padding(end = 16.dp).size(22.dp)
        )
    }
}

@Composable
private fun SavedPlacesEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = DeepSea3,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No saved places yet",
                color = DeepSea5,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Bookmark places from the AI chat\nto see them here",
                color = DeepSea4,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun BookmarkedPlace.toTravelEvent(): TravelEvent {
    val type = when {
        category.contains("restaurant", ignoreCase = true) ||
        category.contains("food", ignoreCase = true) ||
        category.contains("cafe", ignoreCase = true) ||
        category.contains("bar", ignoreCase = true) ||
        category.contains("dining", ignoreCase = true) -> "restaurant"
        category.contains("hotel", ignoreCase = true) ||
        category.contains("hostel", ignoreCase = true) ||
        category.contains("resort", ignoreCase = true) -> "hotel"
        else -> "activity"
    }
    return TravelEvent(
        eventId = id,
        type = type,
        itineraryId = "",
        imageUrl = imageUrl ?: "",
        details = buildMap {
            put(ATTR_BUSINESS_NAME, name)
            if (area.isNotBlank()) put(ATTR_BUSINESS_ADDRESS, area)
            if (category.isNotBlank()) put(ATTR_CATEGORIES, category)
            if (!yelpUrl.isNullOrBlank()) put(ATTR_YELP_URL, yelpUrl)
        }
    )
}
