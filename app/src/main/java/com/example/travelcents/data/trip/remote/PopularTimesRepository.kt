package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object PopularTimesRepository {

    private const val CACHE_TTL_MS = 15 * 60 * 1000L
    private const val WEEK_RAW_PATH = "https://besttime.app/api/v1/forecasts/week/raw"
    private const val LIVE_PATH = "https://besttime.app/api/v1/forecast/live"

    data class PopularTimesSnapshot(
        val popularTimesJson: String,
        val currentBusyness: Int?,
        val estimatedWaitMin: Int?
    )

    private data class CacheEntry(
        val snapshot: PopularTimesSnapshot,
        val expiresAtMs: Long
    )

    private data class WeekForecast(
        val venueId: String?,
        val popularTimesJson: String
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSnapshot(
        venueName: String,
        venueAddress: String,
        yelpId: String? = null
    ): PopularTimesSnapshot? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.BESTTIME_API_KEY.takeIf { it.isNotBlank() } ?: return@withContext null
        val trimmedVenueName = venueName.trim()
        val trimmedVenueAddress = venueAddress.trim()
        if (trimmedVenueName.isBlank() || trimmedVenueAddress.isBlank()) return@withContext null

        val cacheKey = normalizedCacheKey(
            yelpId = yelpId,
            venueName = trimmedVenueName,
            venueAddress = trimmedVenueAddress
        )
        val now = System.currentTimeMillis()
        cache[cacheKey]
            ?.takeIf { it.expiresAtMs > now }
            ?.let { return@withContext it.snapshot }

        val weekForecast = fetchWeekForecast(
            apiKey = apiKey,
            venueName = trimmedVenueName,
            venueAddress = trimmedVenueAddress
        ) ?: return@withContext null

        val currentBusyness = fetchLiveBusyness(
            apiKey = apiKey,
            venueId = weekForecast.venueId,
            venueName = trimmedVenueName,
            venueAddress = trimmedVenueAddress
        )

        val snapshot = PopularTimesSnapshot(
            popularTimesJson = weekForecast.popularTimesJson,
            currentBusyness = currentBusyness,
            estimatedWaitMin = estimatedWaitMinutes(currentBusyness)
        )
        cache[cacheKey] = CacheEntry(
            snapshot = snapshot,
            expiresAtMs = now + CACHE_TTL_MS
        )
        snapshot
    }

    private fun fetchWeekForecast(
        apiKey: String,
        venueName: String,
        venueAddress: String
    ): WeekForecast? {
        val response = postBestTime(
            url = WEEK_RAW_PATH,
            formBody = FormBody.Builder()
                .add("api_key_private", apiKey)
                .add("venue_name", venueName)
                .add("venue_address", venueAddress)
                .build()
        ) ?: return null

        val analysis = response.optJSONArray("analysis") ?: return null
        val popularTimesJson = encodePopularTimes(analysis) ?: return null
        val venueId = response.optJSONObject("venue_info")
            ?.optString("venue_id")
            ?.takeIf { it.isNotBlank() }

        return WeekForecast(
            venueId = venueId,
            popularTimesJson = popularTimesJson
        )
    }

    private fun fetchLiveBusyness(
        apiKey: String,
        venueId: String?,
        venueName: String,
        venueAddress: String
    ): Int? {
        val bodyBuilder = FormBody.Builder()
            .add("api_key_private", apiKey)

        if (!venueId.isNullOrBlank()) {
            bodyBuilder.add("venue_id", venueId)
        } else {
            bodyBuilder
                .add("venue_name", venueName)
                .add("venue_address", venueAddress)
        }

        val response = postBestTime(
            url = LIVE_PATH,
            formBody = bodyBuilder.build()
        ) ?: return null

        val analysis = response.optJSONObject("analysis") ?: return null
        return listOf(
            analysis.optInt("venue_live_busyness", -1),
            analysis.optInt("venue_forecasted_busyness", -1)
        ).firstOrNull { it >= 0 }?.coerceIn(0, 100)
    }

    private fun postBestTime(
        url: String,
        formBody: FormBody
    ): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::JSONObject)
            }
        }.getOrNull()
    }

    private fun encodePopularTimes(analysis: JSONArray): String? {
        val days = MutableList(7) { MutableList(24) { 0 } }
        var foundDay = false

        for (index in 0 until analysis.length()) {
            val dayObject = analysis.optJSONObject(index) ?: continue
            val dayIndex = dayObject.optJSONObject("day_info")
                ?.optInt("day_int", -1)
                ?: -1
            if (dayIndex !in 0..6) continue

            val rawValues = dayObject.optJSONArray("day_raw")
                ?: dayObject.optJSONArray("day_raw_whole")
                ?: continue

            val normalized = MutableList(24) { rawIndex ->
                rawValues.optInt(rawIndex, 0).coerceIn(0, 100)
            }
            days[dayIndex] = normalized
            foundDay = true
        }

        if (!foundDay) return null

        return JSONArray(
            days.map { dayValues ->
                JSONArray(dayValues)
            }
        ).toString()
    }

    private fun estimatedWaitMinutes(currentBusyness: Int?): Int? {
        val value = currentBusyness ?: return null

        // BestTime does not expose queue length, so this is a coarse UI heuristic from live busyness.
        return when {
            value < 35 -> 0
            value < 55 -> 10
            value < 75 -> 20
            value < 90 -> 30
            else -> 45
        }
    }

    private fun normalizedCacheKey(
        yelpId: String?,
        venueName: String,
        venueAddress: String
    ): String {
        return yelpId?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.lowercase(Locale.US)
            ?: "${venueName.lowercase(Locale.US)}|${venueAddress.lowercase(Locale.US)}"
    }
}
