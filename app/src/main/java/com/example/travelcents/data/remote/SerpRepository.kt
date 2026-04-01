package com.example.travelcents.data.remote

import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.SerpFlightLeg
import com.example.travelcents.data.model.SerpHotelProperty
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.TravelRequest
import com.example.travelcents.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object SerpRepository {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: SerpApiService = Retrofit.Builder()
        .baseUrl("https://serpapi.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SerpApiService::class.java)

    suspend fun searchFlights(request: TravelRequest, itinerary: Itinerary): List<TravelEvent> {
        val key = "${itinerary.originIata}_${itinerary.destinationIata}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.children}_${request.currency}"

        SerpCache.getFlights(key)?.let { cached ->
            return cached.map { it.copy(itineraryId = itinerary.itineraryId) }
        }

        val params = mapOf(
            "engine" to "google_flights",
            "departure_id" to itinerary.originIata,
            "arrival_id" to itinerary.destinationIata,
            "outbound_date" to request.dateFrom,
            "return_date" to request.dateTo,
            "adults" to request.adults.toString(),
            "children" to request.children.toString(),
            "currency" to request.currency,
            "type" to "1",
            "api_key" to BuildConfig.SERP_API_KEY
        )

        val response = api.searchFlights(params)
        val options = (response.bestFlights ?: emptyList()) + (response.otherFlights ?: emptyList())

        // Take the cheapest option's legs (first result from best_flights is lowest price)
        val events = options.take(1).flatMap { option ->
            option.flights.map { leg -> flightLegToEvent(leg, option.price, itinerary.itineraryId) }
        }

        SerpCache.putFlights(key, events.map { it.copy(itineraryId = "") })
        return events
    }

    suspend fun searchHotels(
        request: TravelRequest,
        itinerary: Itinerary,
        maxPricePerNight: Double = 0.0
    ): List<TravelEvent> {
        val destNorm = itinerary.destination.lowercase().trim()
        val key = "${destNorm}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.currency}_${maxPricePerNight.toInt()}"

        SerpCache.getHotels(key)?.let { cached ->
            return cached.map { it.copy(itineraryId = itinerary.itineraryId) }
        }

        val params = buildMap {
            put("engine", "google_hotels")
            put("q", "Hotels in ${itinerary.destination}")
            put("check_in_date", request.dateFrom)
            put("check_out_date", request.dateTo)
            put("adults", request.adults.toString())
            put("children", request.children.toString())
            put("currency", request.currency)
            put("sort_by", "3")
            put("api_key", BuildConfig.SERP_API_KEY)
            if (maxPricePerNight > 0) put("max_price", maxPricePerNight.toInt().toString())
        }

        val response = api.searchHotels(params)
        val events = (response.properties ?: emptyList())
            .take(1)
            .map { hotelToEvent(it, request.dateFrom, request.dateTo, itinerary.itineraryId) }

        SerpCache.putHotels(key, events.map { it.copy(itineraryId = "") })
        return events
    }

    private fun flightLegToEvent(leg: SerpFlightLeg, totalPrice: Int, itineraryId: String): TravelEvent {
        val date = leg.departureTime.substringBefore(" ").ifBlank { "" }
        val startTime = leg.departureTime.substringAfter(" ", "").ifBlank { "" }
        val endTime = leg.arrivalTime.substringAfter(" ", "").ifBlank { "" }

        return TravelEvent(
            eventId = UUID.randomUUID().toString(),
            type = "flight",
            itineraryId = itineraryId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            details = buildMap {
                put("airline", leg.airline)
                put("flight_number", leg.flightNumber)
                put("origin_airport", leg.departureAirport.id)
                put("destination_airport", leg.arrivalAirport.id)
                put("duration_min", leg.duration.toString())
                put("total_price", totalPrice.toString())
            }
        )
    }

    private fun hotelToEvent(
        hotel: SerpHotelProperty,
        checkIn: String,
        checkOut: String,
        itineraryId: String
    ): TravelEvent = TravelEvent(
        eventId = UUID.randomUUID().toString(),
        type = "hotel",
        itineraryId = itineraryId,
        date = checkIn,
        startTime = "15:00",
        endTime = "11:00",
        details = buildMap {
            put("hotel_name", hotel.name)
            put("check_in_date", checkIn)
            put("check_out_date", checkOut)
            hotel.ratePerNight?.lowest?.let { put("price_per_night", it) }
            hotel.overallRating?.let { put("rating", it.toString()) }
            hotel.hotelClass?.let { put("hotel_class", it) }
            hotel.amenities?.take(5)?.let { put("amenities", it.joinToString(", ")) }
        }
    )
}