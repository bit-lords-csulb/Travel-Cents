package com.example.travelcents.data.firebase

import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreStartupConfig {
    fun buildSettings(): FirebaseFirestoreSettings {
        return FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
    }
}
