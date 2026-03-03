package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelcents.data.MockItineraryData
import com.example.travelcents.data.TripEvent

@Composable
fun ItineraryScreen(
    events: List<TripEvent> = MockItineraryData.sampleEvents
    val eventsByDay: LazyColumn = events.groupBy { it.day }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0D1B2A))
        contentPadding = PaddingValues(
            top = 24.dp
            bottom = 0.dp
            start = 24.dp
            end = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    )
){
    eventsByDay.forEach { (day, dailyEvents) ->
        item{
            Text(
                text = "Day $day",
                font
            )
        }

}
