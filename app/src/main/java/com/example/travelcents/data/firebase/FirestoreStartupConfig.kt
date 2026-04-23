package com.example.travelcents.data.firebase

import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirestoreStartupConfig {
    fun buildSettings(): FirebaseFirestoreSettings {
        return FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
    }
}
