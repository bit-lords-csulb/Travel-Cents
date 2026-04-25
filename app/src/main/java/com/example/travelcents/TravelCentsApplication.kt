package com.example.travelcents

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.travelcents.data.firebase.FirestoreStartupConfig
import com.example.travelcents.data.trip.TripPerformanceLogger
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class TravelCentsApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        TripPerformanceLogger.markAppStart()
        FirebaseFirestore.getInstance().firestoreSettings = FirestoreStartupConfig.buildSettings()
        Log.d(TAG, "Configured Firestore offline cache settings at app startup.")
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .build()
    }

    private companion object {
        private const val TAG = "TravelCentsApplication"
    }
}