package com.example.travelcents

import android.app.Application
import android.util.Log
import com.example.travelcents.data.firebase.FirestoreStartupConfig
import com.example.travelcents.data.trip.TripPerformanceLogger
import com.google.firebase.firestore.FirebaseFirestore

class TravelCentsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TripPerformanceLogger.markAppStart()
        FirebaseFirestore.getInstance().firestoreSettings = FirestoreStartupConfig.buildSettings()
        Log.d(TAG, "Configured Firestore offline cache settings at app startup.")
    }

    private companion object {
        private const val TAG = "TravelCentsApplication"
    }
}
