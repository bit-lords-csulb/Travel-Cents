package com.example.travelcents.ui.main.itinerary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.local.UserSettings
import com.example.travelcents.ui.main.CurrentPage
import com.example.travelcents.ui.main.CurrentTripViewModel

@Composable
fun ItineraryScreen(
    modifier: Modifier = Modifier,
    userSettings: UserSettings,
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
        userSettings = userSettings,
        startInCalendar = false,
        autoLoadTrip = false,
        onViewItineraryRequested = null
    )
}
