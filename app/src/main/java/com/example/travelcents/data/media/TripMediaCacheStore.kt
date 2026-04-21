package com.example.travelcents.data.media

import android.content.Context
import com.example.travelcents.data.local.trip.TravelCentsDatabase
import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.trip.TripKey

object TripMediaCacheStore {
    suspend fun cacheTripMedia(
        context: Context,
        tripKey: TripKey,
        urls: List<String>
    ): Map<String, String> {
        val assets = ImageCacheManager.cacheTripMediaAssets(
            context = context,
            tripId = tripKey.tripId,
            urls = urls
        )
        if (assets.isNotEmpty()) {
            TripLocalDataSource(TravelCentsDatabase.getInstance(context))
                .upsertMediaAssets(tripKey, assets)
        }
        return assets.associate { asset -> asset.remoteUrl to asset.localPath }
    }

    suspend fun deleteTripMedia(
        context: Context,
        tripKey: TripKey
    ) {
        ImageCacheManager.deleteTripImages(context, tripKey.tripId)
        TripLocalDataSource(TravelCentsDatabase.getInstance(context))
            .deleteMediaAssetsForTrip(tripKey)
    }
}
