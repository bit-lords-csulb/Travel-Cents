package com.example.travelcents.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ItineraryScreen(
    modifier: Modifier = Modifier,
    viewModel: ItineraryViewModel = viewModel()
) {
    CurrentPage(
        modifier = modifier,
        viewModel = viewModel
    )
}
