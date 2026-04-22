package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.SerpFlightLeg
import com.example.travelcents.data.trip.model.SerpFlightOption
import com.example.travelcents.data.trip.model.SerpHotelProperty
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.ATTR_AMENITIES
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_CHECK_OUT_TIME
import com.example.travelcents.data.trip.model.ATTR_DEAL_DESCRIPTION
import com.example.travelcents.data.trip.model.ATTR_GROUP_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_GROUP_TOTAL_STAY_RATE
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CITY
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CLASS
import com.example.travelcents.data.trip.model.ATTR_HOTEL_DETAIL_URL
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_OFFER_COUNT
import com.example.travelcents.data.trip.model.ATTR_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_ROOMS_NEEDED
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.ATTR_TOTAL_STAY_RATE
import com.example.travelcents.data.trip.local.AirportTimeZones
import com.example.travelcents.data.media.StaticMapUrlFactory
import com.example.travelcents.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit

object SerpRepository {

    private const val FLIGHT_CACHE_VERSION = "v3"
    private const val HOTEL_CACHE_VERSION = "v4"
    private const val MAX_FLIGHT_OPTIONS = 8
    private const val MAX_HOTEL_OPTIONS = 8
    private const val MAX_HOTEL_PHOTOS = 5
    private const val MAX_HOTEL_OFFERS = 6

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
            val allOptions = ((response.bestFlights ?: emptyList()) + (response.otherFlights ?: emptyList()))
                .take(MAX_FLIGHT_OPTIONS)
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
            val allOptions = ((response.bestFlights ?: emptyList()) + (response.otherFlights ?: emptyList()))
                .take(MAX_FLIGHT_OPTIONS)
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

    internal fun mapHotelPhotos(hotel: SerpHotelProperty): List<String> {
        return hotel.images.orEmpty()
            .mapNotNull { it.originalImage ?: it.thumbnail }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_HOTEL_PHOTOS)
    }

    internal fun mapHotelSearchResultToEvent(
        request: TravelRequest,
        itinerary: Itinerary,
        properties: List<SerpHotelProperty>
    ): TravelEvent {
        val cappedProperties = properties.take(MAX_HOTEL_OPTIONS)
        val roomsNeeded = maxOf(1, request.adults / 2)
        val nights = nightsBetween(request.dateFrom, request.dateTo)
        val eventId = UUID.randomUUID().toString()
        val selectedHotel = cappedProperties.firstOrNull()
        val eventOptions = cappedProperties.mapIndexed { idx, hotel ->
            hotelToEventOption(
                hotel = hotel,
                eventId = eventId,
                isSelected = idx == 0,
                roomsNeeded = roomsNeeded,
                nights = nights,
                city = itinerary.destination
            )
        }
        val selectedPhotos = selectedHotel?.let(::mapHotelPhotos).orEmpty()

        return TravelEvent(
            eventId = eventId,
            type = "hotel",
            itineraryId = itinerary.itineraryId,
            tz = AirportTimeZones.zoneIdForIata(itinerary.destinationIata).id,
            date = request.dateFrom,
            startTime = selectedHotel?.checkInTime ?: "15:00",
            endTime = selectedHotel?.checkOutTime ?: "11:00",
            imageUrl = selectedPhotos.firstOrNull() ?: "",
            photoUrls = selectedPhotos,
            details = buildMap {
                selectedHotel?.let { hotel ->
                    put(ATTR_HOTEL_NAME, hotel.name)
                    itinerary.destination.takeIf { it.isNotBlank() }?.let { put(ATTR_HOTEL_CITY, it) }
                    bestHotelDetailUrlFor(hotel)?.let { put(ATTR_HOTEL_DETAIL_URL, it) }
                    put("check_in_date", request.dateFrom)
                    put("check_out_date", request.dateTo)
                    hotel.overallRating?.let { put(ATTR_HOTEL_RATING, it.toString()) }
                    hotel.reviews?.let { put(ATTR_REVIEW_COUNT, it.toString()) }
                    hotel.hotelClass?.let { put(ATTR_HOTEL_CLASS, it) }
                    hotel.checkInTime?.let { put(ATTR_CHECK_IN_TIME, it) }
                    hotel.checkOutTime?.let { put(ATTR_CHECK_OUT_TIME, it) }
                    putPricingDetails(hotel, nights, roomsNeeded)
                    bestBookingUrlFor(hotel)?.let { put(ATTR_BOOKING_URL, it) }
                    hotel.amenities?.take(5)?.let { put(ATTR_AMENITIES, it.joinToString(", ")) }
                    hotelDealDescription(hotel)?.let { put(ATTR_DEAL_DESCRIPTION, it) }
                    hotel.gpsCoordinates?.latitude?.let { put(ATTR_LATITUDE, it.toString()) }
                    hotel.gpsCoordinates?.longitude?.let { put(ATTR_LONGITUDE, it.toString()) }
                    buildStaticMapMetadata(hotel)?.forEach { (key, value) -> put(key, value) }
                    putOfferDetails(hotel)
                }
            },
            options = eventOptions
        )
    }

    // Returns a single TravelEvent with all hotel options as EventOption alternatives.
    // sort_by=8 (highest rated). Prices are per-room; group pricing computed client-side.
    suspend fun searchHotels(
        request: TravelRequest,
        itinerary: Itinerary
    ): List<TravelEvent> {
        val destNorm = itinerary.destination.lowercase().trim()
        val cacheKey = "${HOTEL_CACHE_VERSION}_${destNorm}_${request.dateFrom}_${request.dateTo}_${request.adults}_${request.children}_${request.currency}_rated"

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
            mapHotelSearchResultToEvent(
                request = request,
                itinerary = itinerary,
                properties = response.properties.orEmpty().take(MAX_HOTEL_OPTIONS)
            )
        } catch (_: Exception) {
            TravelEvent(
                eventId = UUID.randomUUID().toString(),
                type = "hotel",
                itineraryId = itinerary.itineraryId,
                date = request.dateFrom,
                details = mapOf(ATTR_HOTEL_NAME to "No hotels found")
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
        roomsNeeded: Int,
        nights: Int,
        city: String
    ): EventOption {
        val bestBookingUrl = hotel.prices
            ?.minByOrNull { it.ratePerNight?.extractedLowest ?: Double.MAX_VALUE }
            ?.link

        val photos = mapHotelPhotos(hotel)
        return EventOption(
            optionId = UUID.randomUUID().toString(),
            eventId = eventId,
            source = "serp",
            selected = isSelected,
            imageUrl = photos.firstOrNull() ?: "",
            photoUrls = photos,
            details = buildMap {
                put(ATTR_HOTEL_NAME, hotel.name)
                city.takeIf { it.isNotBlank() }?.let { put(ATTR_HOTEL_CITY, it) }
                bestHotelDetailUrlFor(hotel)?.let { put(ATTR_HOTEL_DETAIL_URL, it) }
                hotel.overallRating?.let { put(ATTR_HOTEL_RATING, it.toString()) }
                hotel.reviews?.let { put(ATTR_REVIEW_COUNT, it.toString()) }
                hotel.hotelClass?.let { put(ATTR_HOTEL_CLASS, it) }
                hotel.checkInTime?.let { put(ATTR_CHECK_IN_TIME, it) }
                hotel.checkOutTime?.let { put(ATTR_CHECK_OUT_TIME, it) }
                hotel.amenities?.take(8)?.let { put(ATTR_AMENITIES, it.joinToString(", ")) }
                putPricingDetails(hotel, nights, roomsNeeded)
                bestBookingUrl?.let { put(ATTR_BOOKING_URL, it) }
                hotelDealDescription(hotel)?.let { put(ATTR_DEAL_DESCRIPTION, it) }
                hotel.gpsCoordinates?.latitude?.let { put(ATTR_LATITUDE, it.toString()) }
                hotel.gpsCoordinates?.longitude?.let { put(ATTR_LONGITUDE, it.toString()) }
                buildStaticMapMetadata(hotel)?.forEach { (key, value) -> put(key, value) }
                putOfferDetails(hotel)
            }
        )
    }

    private fun MutableMap<String, String>.putPricingDetails(
        hotel: SerpHotelProperty,
        nights: Int,
        roomsNeeded: Int
    ) {
        val ratePerNight = hotel.ratePerNight?.extractedLowest ?: return
        if (ratePerNight <= 0) return

        put(ATTR_RATE_PER_NIGHT, ratePerNight.toString())
        put(ATTR_ROOMS_NEEDED, roomsNeeded.toString())
        put(ATTR_GROUP_RATE_PER_NIGHT, (ratePerNight * roomsNeeded).toString())

        val totalStayRate = hotel.totalRate?.extractedLowest?.takeIf { it > 0 }
            ?: if (nights > 0) ratePerNight * nights else null
        totalStayRate?.let {
            put(ATTR_TOTAL_STAY_RATE, it.toString())
            put(ATTR_GROUP_TOTAL_STAY_RATE, (it * roomsNeeded).toString())
        }
    }

    private fun MutableMap<String, String>.putOfferDetails(hotel: SerpHotelProperty) {
        val offers = hotel.prices.orEmpty()
            .filter { offer -> !offer.link.isNullOrBlank() && !offer.source.isNullOrBlank() }
            .sortedBy { offer -> offer.ratePerNight?.extractedLowest ?: Double.MAX_VALUE }
            .distinctBy { offer -> offer.source?.lowercase() }
            .take(MAX_HOTEL_OFFERS)
        if (offers.isEmpty()) return

        put(ATTR_OFFER_COUNT, offers.size.toString())
        offers.forEachIndexed { idx, offer ->
            offer.source?.let { put("offer_${idx}_source", it) }
            offer.link?.let { put("offer_${idx}_link", it) }
            offer.logo?.let { put("offer_${idx}_logo", it) }
            offer.ratePerNight?.extractedLowest?.takeIf { it > 0 }?.let {
                put("offer_${idx}_rate_per_night", it.toString())
            }
            offer.totalRate?.extractedLowest?.takeIf { it > 0 }?.let {
                put("offer_${idx}_total_rate", it.toString())
            }
            offer.freeCancellation?.let { put("offer_${idx}_free_cancellation", it.toString()) }
        }
    }

    private fun hotelDealDescription(hotel: SerpHotelProperty): String? {
        return listOf(hotel.dealDescription, hotel.deal)
            .firstOrNull { !it.isNullOrBlank() }
    }

    private fun nightsBetween(start: String, end: String): Int {
        return try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val startDate = LocalDate.parse(start, formatter)
            val endDate = LocalDate.parse(end, formatter)
            ChronoUnit.DAYS.between(startDate, endDate).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }

    private fun buildStaticMapMetadata(hotel: SerpHotelProperty): Map<String, String>? {
        val latitude = hotel.gpsCoordinates?.latitude ?: return null
        val longitude = hotel.gpsCoordinates?.longitude ?: return null
        return mapOf(
            ATTR_STATIC_MAP_URL to StaticMapUrlFactory.buildUrl(latitude, longitude),
            ATTR_STATIC_MAP_PROVIDER to StaticMapUrlFactory.PROVIDER
        )
    }

    private fun bestBookingUrlFor(hotel: SerpHotelProperty): String? {
        return hotel.prices
            ?.minByOrNull { it.ratePerNight?.extractedLowest ?: Double.MAX_VALUE }
            ?.link
    }

    private fun bestHotelDetailUrlFor(hotel: SerpHotelProperty): String? {
        return hotel.link?.takeIf { it.isNotBlank() }
    }
}


