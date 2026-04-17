package com.example.travelcents.data.trip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreTripRepositoryFallbackTest {

    @Test
    fun shouldFallbackSharedTripQuery_returnsTrueForPermissionDenied() {
        val error = IllegalStateException("Missing or insufficient permissions.")

        assertTrue(shouldFallbackSharedTripQuery(error))
    }

    @Test
    fun shouldFallbackSharedTripQuery_returnsTrueForMissingIndex() {
        val error = IllegalStateException("Query requires an index.")

        assertTrue(shouldFallbackSharedTripQuery(error))
    }

    @Test
    fun shouldFallbackSharedTripQuery_returnsFalseForUnavailable() {
        val error = IllegalStateException("Service temporarily unavailable.")

        assertFalse(shouldFallbackSharedTripQuery(error))
    }
}
