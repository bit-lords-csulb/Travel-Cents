package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.ATTR_AIRLINE_LOGO_URL
import com.example.travelcents.data.trip.model.ATTR_DESTINATION_CITY
import com.example.travelcents.data.trip.model.ATTR_HERO_IMAGE_ATTRIBUTION
import com.example.travelcents.data.trip.model.ATTR_HERO_IMAGE_URL
import com.example.travelcents.data.trip.model.TravelEvent

fun isAirlineLogoFallbackHeroUrl(url: String?): Boolean {
    return !url.isNullOrBlank() &&
        url.contains("logos.skyscnr.com/images/airlines/favicon/", ignoreCase = true)
}

internal fun TravelEvent.needsFlightHeroBackfill(): Boolean {
    if (!type.equals("flight", ignoreCase = true)) return false
    val storedHero = details[ATTR_HERO_IMAGE_URL]
    val currentHero = storedHero?.takeIf { it.isNotBlank() } ?: imageUrl.takeIf { it.isNotBlank() }
    return currentHero.isNullOrBlank() || isAirlineLogoFallbackHeroUrl(currentHero)
}

fun buildFlightHeroImageRepository(): FlightHeroImageRepository {
    val wikipediaClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val wikipedia = retrofit2.Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .client(wikipediaClient)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
        .create(WikipediaApiService::class.java)
    return FlightHeroImageRepository(
        destinationImages = DestinationImageRepository(wikipedia)
    )
}

internal suspend fun enrichFlightHeroImages(
    flights: List<TravelEvent>,
    repository: FlightHeroImageRepository
): List<TravelEvent> {
    if (flights.isEmpty()) return flights

    return flights.map { event ->
        if (!event.type.equals("flight", ignoreCase = true)) return@map event

        val details = event.details
        val storedHero = details[ATTR_HERO_IMAGE_URL]?.takeIf { it.isNotBlank() }
        val existingHero = (storedHero ?: event.imageUrl.takeIf { it.isNotBlank() })
            ?.takeUnless(::isAirlineLogoFallbackHeroUrl)
        if (existingHero != null) {
            val updatedDetails = details.toMutableMap().apply {
                put(ATTR_HERO_IMAGE_URL, existingHero)
            }
            return@map if (updatedDetails == details) event else event.copy(details = updatedDetails)
        }

        val cleanedDetails = details.toMutableMap().apply {
            if (isAirlineLogoFallbackHeroUrl(storedHero)) {
                remove(ATTR_HERO_IMAGE_URL)
                put(ATTR_AIRLINE_LOGO_URL, storedHero.orEmpty())
            }
            if (isAirlineLogoFallbackHeroUrl(event.imageUrl)) {
                put(ATTR_AIRLINE_LOGO_URL, event.imageUrl.orEmpty())
            }
        }
        val destinationCity = details[ATTR_DESTINATION_CITY]
            ?.takeIf { it.isNotBlank() }
            .orEmpty()
        val airlineIata = FlightHeroImageRepository.extractAirlineIata(details["flight_number"])
        val resolved = repository.resolveFlightHero(destinationCity, airlineIata)
            ?: return@map if (cleanedDetails != details || isAirlineLogoFallbackHeroUrl(event.imageUrl)) {
                event.copy(
                    imageUrl = event.imageUrl.takeUnless(::isAirlineLogoFallbackHeroUrl).orEmpty(),
                    details = cleanedDetails
                )
            } else {
                event
            }

        val updatedDetails = cleanedDetails + buildMap {
            put(ATTR_HERO_IMAGE_URL, resolved.imageUrl)
            resolved.attribution?.let { put(ATTR_HERO_IMAGE_ATTRIBUTION, it) }
        }
        event.copy(
            imageUrl = resolved.imageUrl,
            details = updatedDetails
        )
    }
}
