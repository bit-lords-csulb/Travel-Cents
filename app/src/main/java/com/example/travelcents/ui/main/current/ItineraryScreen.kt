package com.example.travelcents.ui.main.current

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ItineraryScreen(
    modifier: Modifier = Modifier,
    onEditEventClick: (String) -> Unit = {},
    onAddEventClick: () -> Unit = {},
    tripId: String? = null,
    viewModel: CurrentTripViewModel = viewModel()
) {
    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    CurrentPage(
        modifier = modifier,
        viewModel = viewModel,
        displayMode = CurrentDisplayMode.ITINERARY,
        autoLoadTrip = false,
        tripId = tripId
    )
}
