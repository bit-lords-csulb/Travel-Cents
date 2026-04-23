package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WalkScoreRepository {

    private const val CACHE_TTL_MS = 12 * 60 * 60 * 1000L
    private const val SCORE_URL = "https://api.walkscore.com/score"

    data class NeighborhoodSnapshot(
        val walkScore: Int?,
        val transitScore: Int?,
        val bikeScore: Int?,
        val nearCategories: List<String>,
        val neighborhoodNote: String?
    )

    private data class CacheEntry(
        val snapshot: NeighborhoodSnapshot,
        val expiresAtMs: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSnapshot(
        latitude: Double,
        longitude: Double,
        address: String
    ): NeighborhoodSnapshot? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.WALKSCORE_API_KEY.takeIf { it.isNotBlank() } ?: return@withContext null
        val trimmedAddress = address.trim()
        if (trimmedAddress.isBlank()) return@withContext null

        val cacheKey = listOf(decimal(latitude), decimal(longitude), trimmedAddress.lowercase(Locale.US))
            .joinToString("|")
        val now = System.currentTimeMillis()
        cache[cacheKey]
            ?.takeIf { it.expiresAtMs > now }
            ?.let { return@withContext it.snapshot }

        val url = SCORE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("format", "json")
            .addQueryParameter("address", trimmedAddress)
            .addQueryParameter("lat", decimal(latitude))
            .addQueryParameter("lon", decimal(longitude))
            .addQueryParameter("transit", "1")
            .addQueryParameter("bike", "1")
            .addQueryParameter("wsapikey", apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val snapshot = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val payload = response.body?.string()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::JSONObject)
                    ?: return@use null
                parseSnapshot(payload)
            }
        }.getOrNull() ?: return@withContext null

        cache[cacheKey] = CacheEntry(
            snapshot = snapshot,
            expiresAtMs = now + CACHE_TTL_MS
        )
        snapshot
    }

    private fun parseSnapshot(payload: JSONObject): NeighborhoodSnapshot? {
        if (payload.optInt("status") != 1) return null

        val walkScore = payload.optInt("walkscore", -1).takeIf { it >= 0 }
        val walkDescription = payload.optString("description").takeIf { it.isNotBlank() }
        val transit = payload.optJSONObject("transit")
        val bike = payload.optJSONObject("bike")
        val transitScore = transit?.optInt("score", -1)?.takeIf { it >= 0 }
        val transitDescription = transit?.optString("description")?.takeIf { it.isNotBlank() }
        val transitSummary = transit?.optString("summary")?.takeIf { it.isNotBlank() }
        val bikeScore = bike?.optInt("score", -1)?.takeIf { it >= 0 }
        val bikeDescription = bike?.optString("description")?.takeIf { it.isNotBlank() }

        if (walkScore == null && transitScore == null && bikeScore == null) return null

        return NeighborhoodSnapshot(
            walkScore = walkScore,
            transitScore = transitScore,
            bikeScore = bikeScore,
            nearCategories = inferredNearbyCategories(
                walkScore = walkScore,
                transitScore = transitScore,
                bikeScore = bikeScore
            ),
            neighborhoodNote = listOfNotNull(
                walkDescription,
                transitDescription,
                transitSummary,
                bikeDescription
            ).joinToString(" • ").takeIf { it.isNotBlank() }
        )
    }

    private fun inferredNearbyCategories(
        walkScore: Int?,
        transitScore: Int?,
        bikeScore: Int?
    ): List<String> {
        val categories = linkedSetOf<String>()
        if ((walkScore ?: 0) >= 90) {
            categories += "coffee"
            categories += "groceries"
        } else if ((walkScore ?: 0) >= 70) {
            categories += "restaurants"
        }
        if ((transitScore ?: 0) >= 60) {
            categories += "metro"
        }
        if ((bikeScore ?: 0) >= 60) {
            categories += "bike lanes"
        }
        return categories.toList()
    }

    private fun decimal(value: Double): String = String.format(Locale.US, "%.4f", value)
}
