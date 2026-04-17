package com.example.travelcents.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.travelcents.data.local.trip.TravelCentsDatabase
import com.example.travelcents.data.local.trip.TripLocalDataSource
import com.example.travelcents.data.media.TripMediaCacheStore
import com.example.travelcents.data.trip.TripKey

class TripHydrationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ownerUid = inputData.getString(KEY_OWNER_UID).orEmpty()
        val tripId = inputData.getString(KEY_TRIP_ID).orEmpty()
        if (ownerUid.isBlank() || tripId.isBlank()) return Result.success()

        val tripKey = TripKey(ownerUid = ownerUid, tripId = tripId)
        val localDataSource = TripLocalDataSource(TravelCentsDatabase.getInstance(applicationContext))
        val events = localDataSource.getTripEvents(tripKey)
        val optionsByEvent = localDataSource.getTripOptions(tripKey)

        val urls = TripHydrationMediaCollector.collectUrls(events, optionsByEvent)

        if (urls.isEmpty()) return Result.success()

        TripMediaCacheStore.cacheTripMedia(
            context = applicationContext,
            tripKey = tripKey,
            urls = urls
        )
        return Result.success()
    }

    companion object {
        private const val KEY_OWNER_UID = "ownerUid"
        private const val KEY_TRIP_ID = "tripId"

        fun enqueue(
            context: Context,
            tripKey: TripKey
        ) {
            val request = OneTimeWorkRequestBuilder<TripHydrationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_OWNER_UID to tripKey.ownerUid,
                        KEY_TRIP_ID to tripKey.tripId
                    )
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(tripKey),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun uniqueWorkName(tripKey: TripKey): String {
            return "trip_hydration:${tripKey.ownerUid}:${tripKey.tripId}"
        }
    }
}
