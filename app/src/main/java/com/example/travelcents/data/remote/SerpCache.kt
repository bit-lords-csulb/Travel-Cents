package com.example.travelcents.data.remote

import com.example.travelcents.data.model.TravelEvent
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

object SerpCache {

    private val db = Firebase.firestore
    private const val TTL_MS = 24 * 60 * 60 * 1000L

    suspend fun getFlights(key: String): List<TravelEvent>? = get("flight_$key")
    suspend fun putFlights(key: String, events: List<TravelEvent>) = put("flight_$key", events)

    suspend fun getHotels(key: String): List<TravelEvent>? = get("hotel_$key")
    suspend fun putHotels(key: String, events: List<TravelEvent>) = put("hotel_$key", events)

    private suspend fun get(docId: String): List<TravelEvent>? {
        return try {
            val doc = db.collection("serpCache").document(docId).get().await()
            if (!doc.exists()) return null

            val cachedAt = doc.getLong("cachedAt") ?: return null
            if (System.currentTimeMillis() - cachedAt > TTL_MS) return null

            @Suppress("UNCHECKED_CAST")
            val rawList = doc.get("events") as? List<Map<String, Any>> ?: return null
            rawList.map { TravelEvent.fromCacheMap(it) }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun put(docId: String, events: List<TravelEvent>) {
        try {
            db.collection("serpCache").document(docId).set(
                mapOf(
                    "events" to events.map { it.toCacheMap() },
                    "cachedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {
            // cache write failure is non-fatal
        }
    }
}