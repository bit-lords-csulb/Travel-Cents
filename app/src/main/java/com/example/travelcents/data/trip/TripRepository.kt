package com.example.travelcents.data.trip

import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import kotlinx.coroutines.flow.Flow

data class TripKey(
    val ownerUid: String,
    val tripId: String
)

interface TripRepository {
    suspend fun getLatestActiveTripKey(viewerUid: String): TripKey?

    suspend fun getTripSummaries(viewerUid: String): List<Itinerary>

    suspend fun getTripSummary(key: TripKey): Itinerary?

    fun observeTripSummary(key: TripKey): Flow<Itinerary?>

    fun observeTripEvents(key: TripKey): Flow<List<TravelEvent>>

    suspend fun getTripMembers(key: TripKey): List<String>

    suspend fun getEventOptions(
        key: TripKey,
        eventIds: List<String>
    ): Map<String, List<EventOption>>

    suspend fun ensureTripAccess(
        key: TripKey,
        memberUids: List<String>,
        defaultRole: TripAccessRole = TripAccessRole.VIEWER
    )

    suspend fun deleteTrip(key: TripKey)
}
