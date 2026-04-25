package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.model.TravelEvent
import java.util.UUID

data class AiSingleEventSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val headline: String,
    val venue: String,
    val cityLine: String,
    val dateLine: String,
    val priceLine: String,
    val category: String,
    val imageUrl: String?,
    val bookingUrl: String?,
    val source: String = "ticketmaster",
    val event: TravelEvent
)
