package com.example.travelcents.data.model

import com.google.gson.annotations.SerializedName

data class SerpFlightResponse(
    @SerializedName("best_flights") val bestFlights: List<SerpFlightOption>? = null,
    @SerializedName("other_flights") val otherFlights: List<SerpFlightOption>? = null
)

data class SerpFlightOption(
    val flights: List<SerpFlightLeg> = emptyList(),
    val price: Int = 0,
    @SerializedName("total_duration") val totalDuration: Int = 0
)

data class SerpFlightLeg(
    @SerializedName("departure_airport") val departureAirport: SerpAirport = SerpAirport(),
    @SerializedName("arrival_airport") val arrivalAirport: SerpAirport = SerpAirport(),
    @SerializedName("departure_time") val departureTime: String = "",
    @SerializedName("arrival_time") val arrivalTime: String = "",
    val airline: String = "",
    @SerializedName("flight_number") val flightNumber: String = "",
    val duration: Int = 0
)

data class SerpAirport(
    val name: String = "",
    val id: String = ""
)

data class SerpHotelResponse(
    val properties: List<SerpHotelProperty>? = null
)

data class SerpHotelProperty(
    val name: String = "",
    @SerializedName("overall_rating") val overallRating: Double? = null,
    @SerializedName("rate_per_night") val ratePerNight: SerpRatePerNight? = null,
    val amenities: List<String>? = null,
    @SerializedName("hotel_class") val hotelClass: String? = null
)

data class SerpRatePerNight(
    val lowest: String? = null
)