package com.example.travelcents.data.trip

import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.trip.model.Itinerary
import kotlinx.coroutines.flow.Flow

class LocalFirstTripRepository(
    private val localDataSource: TripLocalDataSource,
    private val remoteRepository: TripRepository
) : TripRepository by remoteRepository {

    fun observeHomeTripSummaries(viewerUid: String): Flow<List<Itinerary>> {
        return localDataSource.observeHomeTripSummaries(viewerUid)
    }

    suspend fun refreshHomeTripSummaries(viewerUid: String): List<Itinerary> {
        return runCatching {
            remoteRepository.getTripSummaries(viewerUid)
                .sortedBy { itinerary -> itinerary.dateFrom }
        }.onSuccess { trips ->
            localDataSource.replaceHomeTripSummaries(viewerUid, trips)
        }.onFailure { error ->
            localDataSource.recordHomeRefreshFailure(viewerUid, error)
        }.getOrThrow()
    }

    suspend fun updateLocalHomeImage(viewerUid: String, tripKey: TripKey, imageUrl: String) {
        localDataSource.updateHomeImage(viewerUid = viewerUid, tripKey = tripKey, imageUrl = imageUrl)
    }
}
