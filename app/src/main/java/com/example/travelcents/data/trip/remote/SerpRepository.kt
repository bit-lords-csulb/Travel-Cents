package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.SerpFlightLeg
import com.example.travelcents.data.trip.model.SerpFlightOption
import com.example.travelcents.data.trip.model.SerpHotelProperty
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.local.AirportTimeZones
import com.example.travelcents.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object SerpRepository {

    private const val FLIGHT_CACHE_VERSION = "v2"

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

    private enum class FlightSegment(val key: String) {
        OUTBOUND("outbound"),
        RETURN("return")
    }

    private data class FlightSearchResult(
        val event: TravelEvent,
        val selectedOption: SerpFlightOption? = null
    )

    private fun SerpFlightLeg.departureTimestamp(): String =
        departureAirport.time.ifBlank { departureTime }

    private fun SerpFlightLeg.arrivalTimestamp(): String =
        arrivalAirport.time.ifBlank { arrivalTime }

    private fun String.datePart(fallback: String): String =
        substringBefore(" ", fallback).ifBlank { fallback }

    private fun String.timePart(): String =
        substringAfter(" ", "").ifBlank { this }

    private fun buildFlightTitle(
        segment: FlightSegment,
        destinationAirport: String,
        itinerary: Itinerary
    ): String {
        val fallbackDestination = when (segment) {
            FlightSegment.OUTBOUND -> itinerary.destinationIata.ifBlank { itinerary.destination }
            FlightSegment.RETURN -> itinerary.originIata.ifBlank { itinerary.origin }
        }
        val resolvedDestination = destinationAirport.ifBlank { fallbackDestination }
        return when (segment) {
            FlightSegment.OUTBOUND -> "Flight to $resolvedDestination"
            FlightSegment.RETURN -> "Return to $resolvedDestination"
        }
    }

    private fun FlightSegment.timeZoneId(itinerary: Itinerary): String {
        val airportCode = when (this) {
            FlightSegment.OUTBOUND -> itinerary.destinationIata
            FlightSegment.RETURN -> itinerary.originIata
        }
        return AirportTimeZones.zoneIdForIata(airportCode).id
    }

    // Returns outbound + return TravelEvents, each with flight options as alternatives.
    // Falls back through metro airport expansions before giving up.
    suspend fun searchFlights(request: TravelRequest, itinerary: Itinerary): List<TravelEvent> {
        val cacheKey = "${FLIGHT_CACHE_VERSION}_${itinerary.originIata}_${itinerary.destinationIata}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.children}_${request.currency}"

        SerpCache.getFlights(cacheKey)?.let { cached ->
            return cached.map { it.copy(itineraryId = itinerary.itineraryId) }
        }

        val outbound = tryFlightSearch(
            originId = itinerary.originIata,
            destinationId = itinerary.destinationIata,
            request = request,
            itinerary = itinerary,
            stops = "2",
            defaultDate = request.dateFrom,
            segment = FlightSegment.OUTBOUND,
            searchType = "1",
            returnDate = request.dateTo
        ) ?: tryFlightSearch(
            originId = metroAirports[itinerary.originIata] ?: itinerary.originIata,
            destinationId = itinerary.destinationIata,
            request = request,
            itinerary = itinerary,
            stops = "3",
            defaultDate = request.dateFrom,
            segment = FlightSegment.OUTBOUND,
            searchType = "1",
            returnDate = request.dateTo
        ) ?: tryFlightSearch(
            originId = metroAirports[itinerary.originIata] ?: itinerary.originIata,
            destinationId = metroAirports[itinerary.destinationIata] ?: itinerary.destinationIata,
            request = request,
            itinerary = itinerary,
            stops = "3",
            defaultDate = request.dateFrom,
            segment = FlightSegment.OUTBOUND,
            searchType = "1",
            returnDate = request.dateTo
        )

        val result = if (outbound == null) {
            listOf(
                noFlightsPlaceholder(itinerary, request.dateFrom, FlightSegment.OUTBOUND),
                noFlightsPlaceholder(itinerary, request.dateTo, FlightSegment.RETURN)
            )
        } else {
            val returnEvent = tryReturnFlightSearch(
                departureToken = outbound.selectedOption?.departureToken.orEmpty(),
                request = request,
                itinerary = itinerary
            ) ?: tryFlightSearch(
                originId = itinerary.destinationIata,
                destinationId = itinerary.originIata,
                request = request,
                itinerary = itinerary,
                stops = "2",
                defaultDate = request.dateTo,
                segment = FlightSegment.RETURN,
                searchType = "2"
            ) ?: tryFlightSearch(
                originId = metroAirports[itinerary.destinationIata] ?: itinerary.destinationIata,
                destinationId = itinerary.originIata,
                request = request,
                itinerary = itinerary,
                stops = "3",
                defaultDate = request.dateTo,
                segment = FlightSegment.RETURN,
                searchType = "2"
            ) ?: tryFlightSearch(
                originId = metroAirports[itinerary.destinationIata] ?: itinerary.destinationIata,
                destinationId = metroAirports[itinerary.originIata] ?: itinerary.originIata,
                request = request,
                itinerary = itinerary,
                stops = "3",
                defaultDate = request.dateTo,
                segment = FlightSegment.RETURN,
                searchType = "2"
            ) ?: FlightSearchResult(
                noFlightsPlaceholder(itinerary, request.dateTo, FlightSegment.RETURN)
            )

            listOf(outbound.event, returnEvent.event)
        }

        SerpCache.putFlights(cacheKey, result.map { it.copy(itineraryId = "") })
        return result
    }

    private suspend fun tryFlightSearch(
        originId: String,
        destinationId: String,
        request: TravelRequest,
        itinerary: Itinerary,
        stops: String,
        defaultDate: String,
        segment: FlightSegment,
        searchType: String,
        returnDate: String? = null
    ): FlightSearchResult? {
        return try {
            val params = mapOf(
                "engine" to "google_flights",
                "departure_id" to originId,
                "arrival_id" to destinationId,
                "outbound_date" to defaultDate,
                "adults" to request.adults.toString(),
                "children" to request.children.toString(),
                "currency" to request.currency,
                "stops" to stops,
                "type" to searchType,
                "api_key" to BuildConfig.SERP_API_KEY
            ) + listOfNotNull(
                returnDate?.let { "return_date" to it }
            ).toMap()
            val response = api.searchFlights(params)
            val allOptions = (response.bestFlights ?: emptyList()) + (response.otherFlights ?: emptyList())
            if (allOptions.isEmpty()) {
                null
            } else {
                val selectedOption = response.bestFlights?.firstOrNull() ?: allOptions.first()
                val priceLevel = response.priceInsights?.priceLevel ?: ""
                FlightSearchResult(
                    event = buildFlightEvent(
                        allOptions = allOptions,
                        selectedOption = selectedOption,
                        itinerary = itinerary,
                        defaultDate = defaultDate,
                        segment = segment,
                        priceLevel = priceLevel
                    ),
                    selectedOption = selectedOption
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun tryReturnFlightSearch(
        departureToken: String,
        request: TravelRequest,
        itinerary: Itinerary
    ): FlightSearchResult? {
        if (departureToken.isBlank()) return null
        return try {
            val params = mapOf(
                "engine" to "google_flights",
                "departure_token" to departureToken,
                "adults" to request.adults.toString(),
                "children" to request.children.toString(),
                "currency" to request.currency,
                "api_key" to BuildConfig.SERP_API_KEY
            )
            val response = api.searchFlights(params)
            val allOptions = (response.bestFlights ?: emptyList()) + (response.otherFlights ?: emptyList())
            if (allOptions.isEmpty()) {
                null
            } else {
                val selectedOption = response.bestFlights?.firstOrNull() ?: allOptions.first()
                val priceLevel = response.priceInsights?.priceLevel ?: ""
                FlightSearchResult(
                    event = buildFlightEvent(
                        allOptions = allOptions,
                        selectedOption = selectedOption,
                        itinerary = itinerary,
                        defaultDate = request.dateTo,
                        segment = FlightSegment.RETURN,
                        priceLevel = priceLevel
                    ),
                    selectedOption = selectedOption
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildFlightEvent(
        allOptions: List<SerpFlightOption>,
        selectedOption: SerpFlightOption,
        itinerary: Itinerary,
        defaultDate: String,
        segment: FlightSegment,
        priceLevel: String
    ): TravelEvent {
        val eventId = UUID.randomUUID().toString()
        val firstLeg = selectedOption.flights.firstOrNull()
        val lastLeg = selectedOption.flights.lastOrNull()
        val departureTimestamp = firstLeg?.departureTimestamp().orEmpty()
        val arrivalTimestamp = lastLeg?.arrivalTimestamp().orEmpty()
        val destinationAirport = lastLeg?.arrivalAirport?.id.orEmpty()
        val eventOptions = allOptions.mapIndexed { idx, option ->
            flightOptionToEventOption(
                option = option,
                eventId = eventId,
                isSelected = idx == 0,
                priceLevel = priceLevel,
                segment = segment,
                itinerary = itinerary
            )
        }

        return TravelEvent(
            eventId = eventId,
            type = "flight",
            itineraryId = itinerary.itineraryId,
            tz = segment.timeZoneId(itinerary),
            date = departureTimestamp.datePart(defaultDate),
            startTime = departureTimestamp.timePart(),
            endTime = arrivalTimestamp.timePart(),
            imageUrl = selectedOption.airlineLogo ?: "",
            details = buildMap {
                put("trip_segment", segment.key)
                put("title", buildFlightTitle(segment, destinationAirport, itinerary))
                firstLeg?.let {
                    put("airline", it.airline)
                    put("flight_number", it.flightNumber)
                    put("origin_airport", it.departureAirport.id)
                    put("departure_time", departureTimestamp)
                }
                lastLeg?.let {
                    put("destination_airport", it.arrivalAirport.id)
                    put("arrival_date", arrivalTimestamp.datePart(defaultDate))
                    put("arrival_time", arrivalTimestamp.timePart())
                }
                put("flight_duration_min", selectedOption.totalDuration.toString())
                put("total_price", selectedOption.price.toString())
                put("price_level", priceLevel)
                put("stops", (selectedOption.flights.size - 1).toString())
                selectedOption.type?.let { put("trip_type", it) }
                selectedOption.carbonEmissions?.differencePercent?.let {
                    put("carbon_diff_percent", it.toString())
                }
            },
            options = eventOptions
        )
    }

    private fun noFlightsPlaceholder(
        itinerary: Itinerary,
        date: String,
        segment: FlightSegment
    ): TravelEvent {
        val origin = when (segment) {
            FlightSegment.OUTBOUND -> itinerary.originIata
            FlightSegment.RETURN -> itinerary.destinationIata
        }
        val dest = when (segment) {
            FlightSegment.OUTBOUND -> itinerary.destinationIata
            FlightSegment.RETURN -> itinerary.originIata
        }
        return TravelEvent(
            eventId = UUID.randomUUID().toString(),
            type = "flight",
            itineraryId = itinerary.itineraryId,
            tz = segment.timeZoneId(itinerary),
            date = date,
            details = buildMap {
                put("trip_segment", segment.key)
                put("title", buildFlightTitle(segment, dest, itinerary))
                put("description", "No flights found")
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
        priceLevel: String,
        segment: FlightSegment,
        itinerary: Itinerary
    ): EventOption {
        val firstLeg = option.flights.firstOrNull()
        val lastLeg = option.flights.lastOrNull()
        val destinationAirport = lastLeg?.arrivalAirport?.id.orEmpty()
        return EventOption(
            optionId = UUID.randomUUID().toString(),
            eventId = eventId,
            source = "serp",
            selected = isSelected,
            imageUrl = option.airlineLogo ?: "",
            details = buildMap {
                val departureTimestamp = firstLeg?.departureTimestamp().orEmpty()
                val arrivalTimestamp = lastLeg?.arrivalTimestamp().orEmpty()
                val fallbackArrivalDate = when (segment) {
                    FlightSegment.OUTBOUND -> itinerary.dateFrom
                    FlightSegment.RETURN -> itinerary.dateTo
                }
                put("trip_segment", segment.key)
                put("title", buildFlightTitle(segment, destinationAirport, itinerary))
                put("price", option.price.toString())
                put("total_price", option.price.toString())
                put("price_level", priceLevel)
                put("total_duration", option.totalDuration.toString())
                put("flight_duration_min", option.totalDuration.toString())
                put("stops", (option.flights.size - 1).toString())
                firstLeg?.let {
                    put("airline", it.airline)
                    put("flight_number", it.flightNumber)
                    put("origin_airport", it.departureAirport.id)
                    put("departure_time", departureTimestamp)
                    it.legroom?.let { lr -> put("legroom", lr) }
                    if (it.oftenDelayed) put("often_delayed", "true")
                }
                lastLeg?.let {
                    put("destination_airport", it.arrivalAirport.id)
                    put("arrival_date", arrivalTimestamp.datePart(fallbackArrivalDate))
                    put("arrival_time", arrivalTimestamp.timePart())
                }
                option.carbonEmissions?.differencePercent?.let {
                    put("carbon_diff_percent", it.toString())
                }
                option.type?.let { put("trip_type", it) }
                // Per-leg breakdown stored with leg_N_ prefix for detailed display
                option.flights.forEachIndexed { i, leg ->
                    put("leg_${i}_airline", leg.airline)
                    put("leg_${i}_flight_number", leg.flightNumber)
                    put("leg_${i}_from", leg.departureAirport.id)
                    put("leg_${i}_to", leg.arrivalAirport.id)
                    put("leg_${i}_departure", leg.departureTimestamp())
                    put("leg_${i}_arrival", leg.arrivalTimestamp())
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
        itinerary: Itinerary
    ): List<TravelEvent> {
        val destNorm = itinerary.destination.lowercase().trim()
        val cacheKey = "${destNorm}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.children}_${request.currency}_rated"

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
            }

            val response = api.searchHotels(params)
            val properties = response.properties ?: emptyList()
            val roomsNeeded = maxOf(1, request.adults / 2)

            val eventId = UUID.randomUUID().toString()
            val selectedHotel = properties.firstOrNull()

            val eventOptions = properties.mapIndexed { idx, hotel ->
                hotelToEventOption(hotel, eventId, isSelected = idx == 0, roomsNeeded = roomsNeeded)
            }

            val selectedPhotos = selectedHotel?.images
                ?.mapNotNull { it.originalImage ?: it.thumbnail }
                ?: emptyList()

            TravelEvent(
                eventId = eventId,
                type = "hotel",
                itineraryId = itinerary.itineraryId,
                date = request.dateFrom,
                startTime = selectedHotel?.checkInTime ?: "15:00",
                endTime = selectedHotel?.checkOutTime ?: "11:00",
                imageUrl = selectedPhotos.firstOrNull() ?: "",
                photoUrls = selectedPhotos,
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

        val photos = hotel.images?.mapNotNull { it.originalImage ?: it.thumbnail } ?: emptyList()
        return EventOption(
            optionId = UUID.randomUUID().toString(),
            eventId = eventId,
            source = "serp",
            selected = isSelected,
            imageUrl = photos.firstOrNull() ?: "",
            photoUrls = photos,
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


