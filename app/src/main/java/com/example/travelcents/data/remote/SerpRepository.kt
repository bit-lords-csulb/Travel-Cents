package com.example.travelcents.data.remote

import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.SerpFlightOption
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

    // Metro airport expansion map for the top 30 US/international metros
    private val metroAirports = mapOf(
        "JFK" to "JFK,EWR,LGA", "EWR" to "JFK,EWR,LGA", "LGA" to "JFK,EWR,LGA",
        "LAX" to "LAX,BUR,LGB,ONT,SNA", "BUR" to "LAX,BUR,LGB,ONT,SNA",
        "SFO" to "SFO,OAK,SJC", "OAK" to "SFO,OAK,SJC", "SJC" to "SFO,OAK,SJC",
        "ORD" to "ORD,MDW", "MDW" to "ORD,MDW",
        "DCA" to "DCA,IAD,BWI", "IAD" to "DCA,IAD,BWI", "BWI" to "DCA,IAD,BWI",
        "MIA" to "MIA,FLL,PBI", "FLL" to "MIA,FLL,PBI",
        "BOS" to "BOS,MHT,PVD",
        "SEA" to "SEA,BFI",
        "DEN" to "DEN",
        "ATL" to "ATL",
        "DFW" to "DFW,DAL", "DAL" to "DFW,DAL",
        "HOU" to "IAH,HOU", "IAH" to "IAH,HOU",
        "PHX" to "PHX",
        "LAS" to "LAS",
        "MSP" to "MSP",
        "DTW" to "DTW",
        "CLT" to "CLT",
        "PHL" to "PHL",
        "LHR" to "LHR,LGW,STN,LCY", "LGW" to "LHR,LGW,STN,LCY",
        "CDG" to "CDG,ORY", "ORY" to "CDG,ORY",
        "NRT" to "NRT,HND", "HND" to "NRT,HND",
        "FRA" to "FRA,HHN",
        "AMS" to "AMS",
        "MAD" to "MAD",
        "FCO" to "FCO,CIA",
        "DXB" to "DXB,AUH,SHJ",
        "SYD" to "SYD",
        "YYZ" to "YYZ,YTZ",
        "GRU" to "GRU,GIG"
    )

    // Returns a single TravelEvent with all flight options as EventOption alternatives.
    // Falls back through metro airport expansions before giving up.
    suspend fun searchFlights(request: TravelRequest, itinerary: Itinerary): List<TravelEvent> {
        val cacheKey = "${itinerary.originIata}_${itinerary.destinationIata}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.children}_${request.currency}"

        SerpCache.getFlights(cacheKey)?.let { cached ->
            return cached.map { it.copy(itineraryId = itinerary.itineraryId) }
        }

        val event = tryFlightSearch(
            originId = itinerary.originIata,
            destinationId = itinerary.destinationIata,
            request = request,
            itinerary = itinerary,
            stops = "2"
        ) ?: tryFlightSearch(
            originId = metroAirports[itinerary.originIata] ?: itinerary.originIata,
            destinationId = itinerary.destinationIata,
            request = request,
            itinerary = itinerary,
            stops = "3"
        ) ?: tryFlightSearch(
            originId = metroAirports[itinerary.originIata] ?: itinerary.originIata,
            destinationId = metroAirports[itinerary.destinationIata] ?: itinerary.destinationIata,
            request = request,
            itinerary = itinerary,
            stops = "3"
        ) ?: noFlightsPlaceholder(itinerary)

        val result = listOf(event)
        SerpCache.putFlights(cacheKey, result.map { it.copy(itineraryId = "") })
        return result
    }

    private suspend fun tryFlightSearch(
        originId: String,
        destinationId: String,
        request: TravelRequest,
        itinerary: Itinerary,
        stops: String
    ): TravelEvent? {
        return try {
            val params = mapOf(
                "engine" to "google_flights",
                "departure_id" to originId,
                "arrival_id" to destinationId,
                "outbound_date" to request.dateFrom,
                "return_date" to request.dateTo,
                "adults" to request.adults.toString(),
                "children" to request.children.toString(),
                "currency" to request.currency,
                "stops" to stops,
                "type" to "1",
                "api_key" to BuildConfig.SERP_API_KEY
            )
            val response = api.searchFlights(params)
            val allOptions = (response.bestFlights ?: emptyList()) + (response.otherFlights ?: emptyList())
            if (allOptions.isEmpty()) return null

            val eventId = UUID.randomUUID().toString()
            val selectedOption = response.bestFlights?.firstOrNull() ?: allOptions.first()
            val firstLeg = selectedOption.flights.firstOrNull()
            val lastLeg = selectedOption.flights.lastOrNull()

            val priceLevel = response.priceInsights?.priceLevel ?: ""
            val eventOptions = allOptions.mapIndexed { idx, option ->
                flightOptionToEventOption(option, eventId, isSelected = idx == 0, priceLevel = priceLevel)
            }

            TravelEvent(
                eventId = eventId,
                type = "flight",
                itineraryId = itinerary.itineraryId,
                date = firstLeg?.departureTime?.substringBefore(" ") ?: request.dateFrom,
                startTime = firstLeg?.departureTime?.substringAfter(" ", "") ?: "",
                endTime = lastLeg?.arrivalTime?.substringAfter(" ", "") ?: "",
                imageUrl = selectedOption.airlineLogo ?: "",
                details = buildMap {
                    firstLeg?.let {
                        put("airline", it.airline)
                        put("flight_number", it.flightNumber)
                        put("origin_airport", it.departureAirport.id)
                    }
                    lastLeg?.let { put("destination_airport", it.arrivalAirport.id) }
                    put("total_price", selectedOption.price.toString())
                    put("price_level", priceLevel)
                    put("stops", (selectedOption.flights.size - 1).toString())
                    selectedOption.carbonEmissions?.differencePercent?.let {
                        put("carbon_diff_percent", it.toString())
                    }
                },
                options = eventOptions
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun noFlightsPlaceholder(itinerary: Itinerary): TravelEvent {
        val origin = itinerary.originIata
        val dest = itinerary.destinationIata
        return TravelEvent(
            eventId = UUID.randomUUID().toString(),
            type = "flight",
            itineraryId = itinerary.itineraryId,
            date = "",
            details = buildMap {
                put("title", "No flights found")
                put("origin_airport", origin)
                put("destination_airport", dest)
                put("booking_url", "https://www.google.com/flights?q=flights+from+$origin+to+$dest")
            }
        )
    }

    private fun flightOptionToEventOption(
        option: SerpFlightOption,
        eventId: String,
        isSelected: Boolean,
        priceLevel: String
    ): EventOption {
        val firstLeg = option.flights.firstOrNull()
        val lastLeg = option.flights.lastOrNull()
        return EventOption(
            optionId = UUID.randomUUID().toString(),
            eventId = eventId,
            source = "serp",
            selected = isSelected,
            imageUrl = option.airlineLogo ?: "",
            details = buildMap {
                put("price", option.price.toString())
                put("price_level", priceLevel)
                put("total_duration", option.totalDuration.toString())
                put("stops", (option.flights.size - 1).toString())
                firstLeg?.let {
                    put("airline", it.airline)
                    put("flight_number", it.flightNumber)
                    put("origin_airport", it.departureAirport.id)
                    put("departure_time", it.departureTime)
                    it.legroom?.let { lr -> put("legroom", lr) }
                    if (it.oftenDelayed) put("often_delayed", "true")
                }
                lastLeg?.let {
                    put("destination_airport", it.arrivalAirport.id)
                    put("arrival_time", it.arrivalTime)
                }
                option.carbonEmissions?.differencePercent?.let {
                    put("carbon_diff_percent", it.toString())
                }
                // Per-leg breakdown stored with leg_N_ prefix for detailed display
                option.flights.forEachIndexed { i, leg ->
                    put("leg_${i}_airline", leg.airline)
                    put("leg_${i}_flight_number", leg.flightNumber)
                    put("leg_${i}_from", leg.departureAirport.id)
                    put("leg_${i}_to", leg.arrivalAirport.id)
                    put("leg_${i}_departure", leg.departureTime)
                    put("leg_${i}_arrival", leg.arrivalTime)
                    put("leg_${i}_duration_min", leg.duration.toString())
                    leg.legroom?.let { lr -> put("leg_${i}_legroom", lr) }
                    if (leg.oftenDelayed) put("leg_${i}_often_delayed", "true")
                    if (leg.overnight) put("leg_${i}_overnight", "true")
                }
            }
        )
    }

    // Returns a single TravelEvent with all hotel options as EventOption alternatives.
    // sort_by=8 (highest rated). Prices are per-room; group pricing computed client-side.
    suspend fun searchHotels(
        request: TravelRequest,
        itinerary: Itinerary,
        maxPricePerNight: Double = 0.0
    ): List<TravelEvent> {
        val destNorm = itinerary.destination.lowercase().trim()
        val cacheKey = "${destNorm}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.currency}_${maxPricePerNight.toInt()}"

        SerpCache.getHotels(cacheKey)?.let { cached ->
            return cached.map { it.copy(itineraryId = itinerary.itineraryId) }
        }

        val event = try {
            val params = buildMap<String, String> {
                put("engine", "google_hotels")
                put("q", "Hotels in ${itinerary.destination}")
                put("check_in_date", request.dateFrom)
                put("check_out_date", request.dateTo)
                put("adults", request.adults.toString())
                put("children", request.children.toString())
                put("currency", request.currency)
                put("sort_by", "8") // highest rated
                put("api_key", BuildConfig.SERP_API_KEY)
                if (maxPricePerNight > 0) put("max_price", maxPricePerNight.toInt().toString())
            }

            val response = api.searchHotels(params)
            val properties = response.properties ?: emptyList()
            val roomsNeeded = maxOf(1, request.adults / 2)

            val eventId = UUID.randomUUID().toString()
            val selectedHotel = properties.firstOrNull()

            val eventOptions = properties.mapIndexed { idx, hotel ->
                hotelToEventOption(hotel, eventId, isSelected = idx == 0, roomsNeeded = roomsNeeded)
            }

            TravelEvent(
                eventId = eventId,
                type = "hotel",
                itineraryId = itinerary.itineraryId,
                date = request.dateFrom,
                startTime = selectedHotel?.checkInTime ?: "15:00",
                endTime = selectedHotel?.checkOutTime ?: "11:00",
                imageUrl = selectedHotel?.images?.firstOrNull()?.thumbnail ?: "",
                details = buildMap {
                    selectedHotel?.let { hotel ->
                        put("hotel_name", hotel.name)
                        put("check_in_date", request.dateFrom)
                        put("check_out_date", request.dateTo)
                        hotel.overallRating?.let { put("rating", it.toString()) }
                        hotel.reviews?.let { put("review_count", it.toString()) }
                        hotel.hotelClass?.let { put("hotel_class", it) }
                        hotel.ratePerNight?.extractedLowest?.let { rate ->
                            put("rate_per_night", rate.toString())
                            put("group_rate_per_night", (rate * roomsNeeded).toString())
                            put("rooms_needed", roomsNeeded.toString())
                        }
                        hotel.ratePerNight?.lowest?.let { put("rate_per_night_display", it) }
                        hotel.deal?.let { put("deal", it) }
                        hotel.amenities?.take(5)?.let { put("amenities", it.joinToString(", ")) }
                    }
                },
                options = eventOptions
            )
        } catch (_: Exception) {
            TravelEvent(
                eventId = UUID.randomUUID().toString(),
                type = "hotel",
                itineraryId = itinerary.itineraryId,
                date = request.dateFrom,
                details = mapOf("hotel_name" to "No hotels found")
            )
        }

        val result = listOf(event)
        SerpCache.putHotels(cacheKey, result.map { it.copy(itineraryId = "") })
        return result
    }

    private fun hotelToEventOption(
        hotel: SerpHotelProperty,
        eventId: String,
        isSelected: Boolean,
        roomsNeeded: Int
    ): EventOption {
        val ratePerNight = hotel.ratePerNight?.extractedLowest ?: 0.0
        val groupRatePerNight = if (ratePerNight > 0) ratePerNight * roomsNeeded else 0.0
        val bestBookingUrl = hotel.prices
            ?.minByOrNull { it.ratePerNight?.extractedLowest ?: Double.MAX_VALUE }
            ?.link

        return EventOption(
            optionId = UUID.randomUUID().toString(),
            eventId = eventId,
            source = "serp",
            selected = isSelected,
            imageUrl = hotel.images?.firstOrNull()?.thumbnail ?: "",
            details = buildMap {
                put("hotel_name", hotel.name)
                hotel.overallRating?.let { put("rating", it.toString()) }
                hotel.reviews?.let { put("review_count", it.toString()) }
                hotel.hotelClass?.let { put("hotel_class", it) }
                hotel.extractedHotelClass?.let { put("hotel_class_int", it.toString()) }
                hotel.locationRating?.let { put("location_rating", it.toString()) }
                hotel.deal?.let { put("deal", it) }
                hotel.dealDescription?.let { put("deal_description", it) }
                hotel.checkInTime?.let { put("check_in_time", it) }
                hotel.checkOutTime?.let { put("check_out_time", it) }
                hotel.ecoCertified?.let { if (it) put("eco_certified", "true") }
                hotel.amenities?.take(8)?.let { put("amenities", it.joinToString(", ")) }
                if (ratePerNight > 0) {
                    put("rate_per_night", ratePerNight.toString())
                    put("group_rate_per_night", groupRatePerNight.toString())
                    put("rooms_needed", roomsNeeded.toString())
                }
                hotel.ratePerNight?.lowest?.let { put("rate_per_night_display", it) }
                hotel.totalRate?.lowest?.let { put("total_rate_display", it) }
                bestBookingUrl?.let { put("booking_url", it) }
                hotel.images?.firstOrNull()?.originalImage?.let { put("image_original", it) }
            }
        )
    }
}