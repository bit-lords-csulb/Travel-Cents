package com.example.travelcents.data

import com.google.firebase.Timestamp
import java.util.UUID

data class ItineraryItem(
    val id: String = UUID.randomUUID().toString(),

    val title: String = "",
    val location: String = "",
    val notes: String = "",

    val color: String = "blue",

    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),

    val timeZone: String = "UTC"
)