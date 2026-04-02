package com.example.travelcents.data.model

import com.google.gson.annotations.SerializedName

// ── Flights ──────────────────────────────────────────────────────────────────

data class SerpFlightResponse(
    @SerializedName("best_flights") val bestFlights: List<SerpFlightOption>? = null,
    @SerializedName("other_flights") val otherFlights: List<SerpFlightOption>? = null,
    @SerializedName("price_insights") val priceInsights: SerpPriceInsights? = null
)

data class SerpFlightOption(
    val flights: List<SerpFlightLeg> = emptyList(),
    val price: Int = 0,
    @SerializedName("total_duration") val totalDuration: Int = 0,
    @SerializedName("airline_logo") val airlineLogo: String? = null,
    @SerializedName("carbon_emissions") val carbonEmissions: SerpCarbonEmissions? = null
)

data class SerpFlightLeg(
    @SerializedName("departure_airport") val departureAirport: SerpAirport = SerpAirport(),
    @SerializedName("arrival_airport") val arrivalAirport: SerpAirport = SerpAirport(),
    @SerializedName("departure_time") val departureTime: String = "",
    @SerializedName("arrival_time") val arrivalTime: String = "",
    val airline: String = "",
    @SerializedName("flight_number") val flightNumber: String = "",
    val duration: Int = 0,
    val legroom: String? = null,
    @SerializedName("often_delayed_by_over_30_min") val oftenDelayed: Boolean = false,
    val overnight: Boolean = false,
    @SerializedName("plane_and_crew_by") val planeAndCrewBy: String? = null,
    val extensions: List<String>? = null
)

data class SerpAirport(
    val name: String = "",
    val id: String = ""
)

data class SerpCarbonEmissions(
    @SerializedName("this_flight") val thisFlight: Int? = null,
    @SerializedName("typical_for_this_route") val typicalForRoute: Int? = null,
    @SerializedName("difference_percent") val differencePercent: Int? = null
)

data class SerpPriceInsights(
    @SerializedName("lowest_price") val lowestPrice: Int? = null,
    @SerializedName("price_level") val priceLevel: String? = null,
    @SerializedName("typical_price_range") val typicalPriceRange: List<Int>? = null
)

// ── Hotels ───────────────────────────────────────────────────────────────────

data class SerpHotelResponse(
    val properties: List<SerpHotelProperty>? = null
)

data class SerpHotelProperty(
    val name: String = "",
    @SerializedName("overall_rating") val overallRating: Double? = null,
    val reviews: Int? = null,
    @SerializedName("reviews_breakdown") val reviewsBreakdown: List<SerpReviewBreakdown>? = null,
    @SerializedName("rate_per_night") val ratePerNight: SerpRatePerNight? = null,
    @SerializedName("total_rate") val totalRate: SerpRatePerNight? = null,
    val amenities: List<String>? = null,
    @SerializedName("excluded_amenities") val excludedAmenities: List<String>? = null,
    @SerializedName("hotel_class") val hotelClass: String? = null,
    @SerializedName("extracted_hotel_class") val extractedHotelClass: Int? = null,
    @SerializedName("location_rating") val locationRating: Double? = null,
    val deal: String? = null,
    @SerializedName("deal_description") val dealDescription: String? = null,
    @SerializedName("eco_certified") val ecoCertified: Boolean? = null,
    @SerializedName("check_in_time") val checkInTime: String? = null,
    @SerializedName("check_out_time") val checkOutTime: String? = null,
    @SerializedName("gps_coordinates") val gpsCoordinates: SerpGpsCoordinates? = null,
    val images: List<SerpHotelImage>? = null,
    val prices: List<SerpHotelPrice>? = null,
    @SerializedName("nearby_places") val nearbyPlaces: List<SerpNearbyPlace>? = null
)

data class SerpRatePerNight(
    val lowest: String? = null,
    @SerializedName("extracted_lowest") val extractedLowest: Double? = null
)

data class SerpHotelImage(
    val thumbnail: String? = null,
    @SerializedName("original_image") val originalImage: String? = null
)

data class SerpHotelPrice(
    val source: String? = null,
    val logo: String? = null,
    val link: String? = null,
    @SerializedName("rate_per_night") val ratePerNight: SerpRatePerNight? = null,
    @SerializedName("total_rate") val totalRate: SerpRatePerNight? = null,
    @SerializedName("free_cancellation") val freeCancellation: Boolean? = null,
    @SerializedName("discount_remarks") val discountRemarks: List<String>? = null
)

data class SerpReviewBreakdown(
    val name: String? = null,
    val description: String? = null,
    val total_mentioned: Int? = null,
    val positive: Int? = null,
    val negative: Int? = null,
    val neutral: Int? = null
)

data class SerpGpsCoordinates(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class SerpNearbyPlace(
    val name: String? = null,
    val transportations: List<SerpTransportation>? = null
)

data class SerpTransportation(
    val type: String? = null,
    val duration: String? = null
)