package com.example.travelcents.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripSyncRemoteDataSourceFallbackTest {

    @Test
    fun shouldFallbackOptionQuery_returnsTrueForPermissionDenied() {
        val error = IllegalStateException("Missing or insufficient permissions.")

        assertTrue(shouldFallbackOptionQuery(error))
    }

    @Test
    fun shouldFallbackOptionQuery_returnsTrueForMissingIndex() {
        val error = IllegalStateException("Query requires an index.")

        assertTrue(shouldFallbackOptionQuery(error))
    }

    @Test
    fun shouldFallbackOptionQuery_returnsFalseForUnavailable() {
        val error = IllegalStateException("Service temporarily unavailable.")

        assertFalse(shouldFallbackOptionQuery(error))
    }
}
