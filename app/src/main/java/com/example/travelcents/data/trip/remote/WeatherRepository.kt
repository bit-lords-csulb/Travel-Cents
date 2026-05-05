package com.example.travelcents.data.trip.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object WeatherRepository {

    private const val CACHE_TTL_MS = 30 * 60 * 1000L
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"

    data class WeatherSnapshot(
        val temperatureC: Int,
        val condition: String,
        val precipPct: Int?,
        val windKph: Int?,
        val summary: String
    )

    private data class CacheEntry(
        val snapshot: WeatherSnapshot,
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
        date: String,
        startTime: String?,
        timeZoneId: String?,
        forceRefresh: Boolean = false
    ): WeatherSnapshot? = withContext(Dispatchers.IO) {
        val eventDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@withContext null
        val zoneId = resolveZoneId(timeZoneId)
        val eventHour = parseHour(startTime)
        val cacheKey = buildWeatherCacheKey(latitude, longitude, eventDate, eventHour, zoneId)
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            cache[cacheKey]
                ?.takeIf { it.expiresAtMs > now }
                ?.let { return@withContext it.snapshot }
        }

        val url = FORECAST_URL.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", decimal(latitude))
            .addQueryParameter("longitude", decimal(longitude))
            .addQueryParameter(
                "hourly",
                "temperature_2m,precipitation_probability,weather_code,wind_speed_10m"
            )
            .addQueryParameter("forecast_days", "16")
            .addQueryParameter("timezone", zoneId.id)
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
                parseSnapshot(
                    payload = payload,
                    eventDate = eventDate,
                    eventHour = eventHour
                )
            }
        }.getOrNull() ?: return@withContext null

        cache[cacheKey] = CacheEntry(
            snapshot = snapshot,
            expiresAtMs = now + CACHE_TTL_MS
        )
        snapshot
    }

    private fun parseSnapshot(
        payload: JSONObject,
        eventDate: LocalDate,
        eventHour: Int
    ): WeatherSnapshot? {
        val hourly = payload.optJSONObject("hourly") ?: return null
        val times = hourly.optJSONArray("time") ?: return null
        val temperatures = hourly.optJSONArray("temperature_2m") ?: return null
        val weatherCodes = hourly.optJSONArray("weather_code") ?: return null
        val precipProb = hourly.optJSONArray("precipitation_probability")
        val windSpeed = hourly.optJSONArray("wind_speed_10m")

        val target = LocalDateTime.of(eventDate, LocalTime.of(eventHour, 0))
        var bestIndex = -1
        var bestDistanceHours = Int.MAX_VALUE

        for (index in 0 until times.length()) {
            val time = times.optString(index).takeIf { it.isNotBlank() } ?: continue
            val forecastTime = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: continue
            val distanceHours = abs(java.time.Duration.between(forecastTime, target).toHours().toInt())
            if (distanceHours < bestDistanceHours) {
                bestDistanceHours = distanceHours
                bestIndex = index
            }
        }

        if (bestIndex < 0 || bestDistanceHours > 6) return null

        val temperatureC = temperatures.optDouble(bestIndex, Double.NaN)
            .takeUnless { it.isNaN() }
            ?.toInt()
            ?: return null
        val weatherCode = weatherCodes.optInt(bestIndex, -1).takeIf { it >= 0 } ?: return null
        val precipPct = precipProb?.optInt(bestIndex, -1)?.takeIf { it >= 0 }
        val windKph = windSpeed?.optDouble(bestIndex, Double.NaN)
            ?.takeUnless { it.isNaN() }
            ?.toInt()

        val condition = weatherConditionLabel(weatherCode)
        return WeatherSnapshot(
            temperatureC = temperatureC,
            condition = condition,
            precipPct = precipPct,
            windKph = windKph,
            summary = patioWeatherSummary(
                temperatureC = temperatureC,
                weatherCode = weatherCode,
                precipPct = precipPct,
                windKph = windKph
            )
        )
    }

    private fun patioWeatherSummary(
        temperatureC: Int,
        weatherCode: Int,
        precipPct: Int?,
        windKph: Int?
    ): String {
        val isStormy = weatherCode in setOf(95, 96, 99)
        val isWet = weatherCode in setOf(51, 53, 55, 61, 63, 65, 80, 81, 82) || (precipPct ?: 0) >= 60
        val isWindy = (windKph ?: 0) >= 28

        return when {
            isStormy || isWet || isWindy -> "Patio weather looks rough for this time."
            temperatureC in 18..29 && (precipPct ?: 0) <= 20 && (windKph ?: 0) <= 18 ->
                "Great patio weather around your reservation time."
            temperatureC < 13 -> "It will feel cool outside, but still workable."
            temperatureC > 32 -> "Warm patio weather. Shade or indoor seating may feel better."
            else -> "Outdoor seating looks reasonable for this stop."
        }
    }

    private fun weatherConditionLabel(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm"
            else -> "Forecast"
        }
    }

    private fun parseHour(rawTime: String?): Int {
        val value = rawTime?.trim().orEmpty()
        if (value.length < 2) return 18
        return value.substring(0, 2).toIntOrNull()?.coerceIn(0, 23) ?: 18
    }

    private fun resolveZoneId(rawTimeZone: String?): ZoneId {
        val safeZoneId = rawTimeZone?.takeIf { it.isNotBlank() } ?: return ZoneId.of("UTC")
        return runCatching { ZoneId.of(safeZoneId) }.getOrDefault(ZoneId.of("UTC"))
    }

    private fun buildWeatherCacheKey(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        hour: Int,
        zoneId: ZoneId
    ): String {
        return listOf(
            decimal(latitude),
            decimal(longitude),
            date.toString(),
            hour.toString(),
            zoneId.id.lowercase(Locale.US)
        ).joinToString("|")
    }

    private fun decimal(value: Double): String = String.format(Locale.US, "%.4f", value)
}
