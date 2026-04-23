package com.example.travelcents.data.firebase

import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreStartupConfigTest {
    @Test
    fun buildSettings_enablesOfflinePersistence() {
        val settings = FirestoreStartupConfig.buildSettings()

        assertTrue(settings.isPersistenceEnabled)
        assertEquals(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED, settings.cacheSizeBytes)
    }
}
