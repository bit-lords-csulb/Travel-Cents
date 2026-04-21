package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.TripKey
import java.util.UUID

enum class AiCuratedTripSource {
    FIRESTORE,
    GENERATED
}

data class AiCuratedTripStarter(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val destination: String,
    val durationDays: Int,
    val travelStyle: String,
    val summary: String,
    val matchReason: String,
    val source: AiCuratedTripSource,
    val tripKey: TripKey? = null
)

data class AiCuratedTripRow(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val trips: List<AiCuratedTripStarter>
)
