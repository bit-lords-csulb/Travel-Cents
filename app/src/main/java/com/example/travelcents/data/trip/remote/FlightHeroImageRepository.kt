package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FlightHeroImageRepository(
    private val destinationImages: DestinationImageRepository,
    private val httpClient: OkHttpClient = defaultClient,
    private val unsplashKey: String = BuildConfig.UNSPLASH_ACCESS_KEY
) {

    data class HeroResolution(
        val imageUrl: String,
        val attribution: String?
    )

    suspend fun resolveFlightHero(
        destinationCity: String,
        airlineIata: String?
    ): HeroResolution? {
        val cityKey = destinationCity.trim().lowercase(Locale.US)
        if (cityKey.isNotBlank()) {
            cityCache[cityKey]?.let { return it }
        }

        val resolved = resolveInternal(destinationCity.trim(), airlineIata?.trim().orEmpty())
        if (resolved != null && cityKey.isNotBlank() && resolved.imageUrl.isNotBlank()) {
            cityCache[cityKey] = resolved
        }
        return resolved
    }

    private suspend fun resolveInternal(destinationCity: String, airlineIata: String): HeroResolution? {
        if (destinationCity.isNotBlank()) {
            destinationImages.resolveDestinationImage(destinationCity).imageUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { return HeroResolution(it, null) }

            unsplashSearch("$destinationCity skyline")?.let { return it }
            unsplashSearch("$destinationCity aerial")?.let { return it }
        }

        if (airlineIata.isNotBlank()) {
            return HeroResolution(
                imageUrl = "https://logos.skyscnr.com/images/airlines/favicon/${airlineIata.uppercase(Locale.US)}.png",
                attribution = null
            )
        }
        return null
    }

    private suspend fun unsplashSearch(query: String): HeroResolution? {
        if (unsplashKey.isBlank() || query.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.unsplash.com/search/photos".toHttpUrl().newBuilder()
                    .addQueryParameter("query", query)
                    .addQueryParameter("orientation", "landscape")
                    .addQueryParameter("per_page", "1")
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Client-ID $unsplashKey")
                    .header("Accept-Version", "v1")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string()?.takeIf { it.isNotBlank() }
                        ?: return@use null
                    val first = JSONObject(body).optJSONArray("results")?.optJSONObject(0)
                        ?: return@use null
                    val urls = first.optJSONObject("urls") ?: return@use null
                    val imageUrl = listOf("regular", "full", "small")
                        .firstNotNullOfOrNull { key -> urls.optString(key).takeIf { it.isNotBlank() } }
                        ?: return@use null
                    val photographer = first.optJSONObject("user")?.optString("name")?.takeIf { it.isNotBlank() }
                    HeroResolution(
                        imageUrl = imageUrl,
                        attribution = photographer?.let { "Photo by $it on Unsplash" }
                    )
                }
            }.getOrNull()
        }
    }

    companion object {
        private val cityCache = ConcurrentHashMap<String, HeroResolution>()

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        fun extractAirlineIata(flightNumber: String?): String? {
            if (flightNumber.isNullOrBlank()) return null
            val prefix = flightNumber.trim().takeWhile { it.isLetter() }
            return prefix.takeIf { it.length in 2..3 }?.uppercase(Locale.US)
        }
    }
}