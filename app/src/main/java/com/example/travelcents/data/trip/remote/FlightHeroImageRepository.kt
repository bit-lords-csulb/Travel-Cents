package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.media.UnsplashImageRepository
import com.example.travelcents.data.media.UnsplashSearchParams
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class FlightHeroImageRepository(
    private val destinationImages: DestinationImageRepository,
    private val unsplashImages: UnsplashImageRepository = UnsplashImageRepository()
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
            unsplashSearch(
                query = "$destinationCity skyline",
                params = UnsplashSearchParams(orientation = "squarish")
            )?.let { return it }
            unsplashSearch(
                query = "$destinationCity travel",
                params = UnsplashSearchParams(orientation = "squarish")
            )?.let { return it }
            unsplashSearch(
                query = "$destinationCity aerial",
                params = UnsplashSearchParams(orientation = "squarish")
            )?.let { return it }

            destinationImages.resolveDestinationImage(destinationCity).imageUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { return HeroResolution(it, null) }
        }
        return null
    }

    private suspend fun unsplashSearch(
        query: String,
        params: UnsplashSearchParams
    ): HeroResolution? {
        return unsplashImages.resolveDetailed(query, params)?.let { resolved ->
            HeroResolution(
                imageUrl = resolved.imageUrl,
                attribution = resolved.attribution
            )
        }
    }

    companion object {
        private val cityCache = ConcurrentHashMap<String, HeroResolution>()

        fun extractAirlineIata(flightNumber: String?): String? {
            if (flightNumber.isNullOrBlank()) return null
            val prefix = flightNumber.trim().takeWhile { it.isLetter() }
            return prefix.takeIf { it.length in 2..3 }?.uppercase(Locale.US)
        }
    }
}
