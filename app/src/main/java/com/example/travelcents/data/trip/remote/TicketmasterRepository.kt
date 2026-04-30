package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.media.StaticMapUrlFactory
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.ATTR_TICKET_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MAX
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MIN
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.TmClassification
import com.example.travelcents.data.trip.model.TmEvent
import com.example.travelcents.data.trip.model.TmImage
import com.example.travelcents.data.trip.model.TmPriceRange
import com.example.travelcents.data.trip.model.TmVenue
import com.example.travelcents.data.trip.model.TravelEvent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object TicketmasterRepository {

    private const val BASE_URL = "https://app.ticketmaster.com/"
    private const val MAX_PAGE_SIZE = 200
    private const val DESCRIPTION_LIMIT = 200
    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val seasonYearRangeRegex = Regex("""\b\d{4}/\d{2}\b""")
    private val excludedInventoryTerms = listOf(
        "season ticket",
        "season pass",
        "membership",
        "hospitality",
        "parking",
        "car park",
        "subscription"
    )

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: TicketmasterApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TicketmasterApiService::class.java)

    suspend fun searchEventsForTrip(
        location: String,
        startDate: String,
        endDate: String,
        itineraryId: String,
        classification: String? = null,
        size: Int = 20
    ): List<TravelEvent> {
        val apiKey = BuildConfig.TICKETMASTER_API_KEY.takeIf { it.isNotBlank() } ?: return emptyList()
        val city = normalizeLocation(location) ?: return emptyList()
        return try {
            val response = api.searchEvents(
                buildSearchParams(
                    apiKey = apiKey,
                    city = city,
                    startDate = startDate,
                    endDate = endDate,
                    keyword = null,
                    classification = classification,
                    size = size
                )
            )
            response.embedded?.events.orEmpty()
                .asSequence()
                .filter { it.passesTripFilters(startDate, endDate) }
                .mapNotNull { ticketmasterToTravelEvent(it, itineraryId) }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchEventsForChat(
        location: String,
        startDate: String?,
        endDate: String?,
        keyword: String? = null,
        classification: String? = null,
        size: Int = 10
    ): List<TravelEvent> {
        val apiKey = BuildConfig.TICKETMASTER_API_KEY.takeIf { it.isNotBlank() } ?: return emptyList()
        val city = normalizeLocation(location) ?: return emptyList()
        return try {
            val response = api.searchEvents(
                buildSearchParams(
                    apiKey = apiKey,
                    city = city,
                    startDate = startDate,
                    endDate = endDate,
                    keyword = keyword,
                    classification = classification,
                    size = size
                )
            )
            response.embedded?.events.orEmpty()
                .asSequence()
                .filter { event -> event.passesChatFilters(startDate, endDate) }
                .mapNotNull { ticketmasterToTravelEvent(it, itineraryId = "") }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildSearchParams(
        apiKey: String,
        city: String,
        startDate: String?,
        endDate: String?,
        keyword: String?,
        classification: String?,
        size: Int
    ): Map<String, String> = buildMap {
        put("apikey", apiKey)
        put("city", city)
        put("sort", "date,asc")
        put("size", size.coerceIn(1, MAX_PAGE_SIZE).toString())
        put("includeTBA", "no")
        put("includeTBD", "no")
        put("locale", "*")
        keyword?.trim()?.takeIf { it.isNotBlank() }?.let { put("keyword", it) }
        normalizeClassification(classification)?.let { put("classificationName", it) }

        val bufferedStart = startDate?.let(::parseIsoDate)?.minusDays(1)
        val bufferedEnd = endDate?.let(::parseIsoDate)?.plusDays(1)
        bufferedStart?.let { put("startDateTime", "${it.format(isoDateFormatter)}T00:00:00Z") }
        bufferedEnd?.let { put("endDateTime", "${it.format(isoDateFormatter)}T23:59:59Z") }
    }

    private fun ticketmasterToTravelEvent(
        event: TmEvent,
        itineraryId: String
    ): TravelEvent? {
        val localDate = event.dates?.start?.localDate?.takeIf { it.isNotBlank() } ?: return null
        if (event.name.isBlank()) return null
        val venue = event.embedded?.venues.orEmpty().firstOrNull()
        val imageUrls = event.images
            .map(TmImage::url)
            .filter { it.isNotBlank() }
            .distinct()
        val bestImage = selectBestImage(event.images)?.url.orEmpty()
        val priceRange = selectPriceRange(event.priceRanges.orEmpty())
        val latitude = venue?.location?.latitude?.toDoubleOrNull()
        val longitude = venue?.location?.longitude?.toDoubleOrNull()

        return TravelEvent(
            eventId = UUID.randomUUID().toString(),
            type = "activity",
            itineraryId = itineraryId,
            tz = event.dates?.timezone.orEmpty().ifBlank { venue?.timezone.orEmpty() },
            date = localDate,
            startTime = event.dates?.start?.localTime.toStorageTime(),
            endTime = event.dates?.end?.localTime.toStorageTime(),
            imageUrl = bestImage,
            photoUrls = imageUrls.take(6),
            details = buildMap {
                put(ATTR_BUSINESS_NAME, event.name)
                put("activity_name", event.name)
                put(ATTR_TICKETMASTER_EVENT_ID, event.id)
                event.url?.takeIf { it.isNotBlank() }?.let { put(ATTR_BOOKING_URL, it) }
                event.ticketDescription()?.let { put("description", it) }
                event.classificationLabel()?.let { put(ATTR_CATEGORIES, it) }
                venue?.name?.takeIf { it.isNotBlank() }?.let {
                    put(ATTR_VENUE_NAME, it)
                    put("location", it)
                }
                venue?.fullAddress()?.let { put(ATTR_BUSINESS_ADDRESS, it) }
                latitude?.let { put(ATTR_LATITUDE, it.toString()) }
                longitude?.let { put(ATTR_LONGITUDE, it.toString()) }
                buildStaticMapMetadata(latitude, longitude)?.forEach { (key, value) -> put(key, value) }
                priceRange?.min?.let { put(ATTR_TICKET_PRICE_MIN, trimTrailingZero(it)) }
                priceRange?.max?.let { put(ATTR_TICKET_PRICE_MAX, trimTrailingZero(it)) }
                priceRange?.currency?.takeIf { it.isNotBlank() }?.let { put(ATTR_TICKET_CURRENCY, it) }
                event.dates?.status?.code?.takeIf { !it.isNullOrBlank() }?.let { put("ticket_status", it) }
                event.dates?.timezone?.takeIf { !it.isNullOrBlank() }?.let { put("tz", it) }
            }
        )
    }

    private fun TmEvent.ticketDescription(): String? {
        return listOf(info, pleaseNote)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?.take(DESCRIPTION_LIMIT)
    }

    private fun TmEvent.classificationLabel(): String? {
        val primary = classifications.firstOrNull { it.primary } ?: classifications.firstOrNull()
        return primary?.classificationName()
            ?: classifications.asSequence()
                .mapNotNull { it.classificationName() }
                .firstOrNull()
    }

    private fun TmClassification.classificationName(): String? {
        return listOf(segment?.name, genre?.name, subGenre?.name)
            .firstOrNull { !it.isNullOrBlank() }
    }

    private fun TmEvent.isWithinLocalDateWindow(
        startDate: String,
        endDate: String
    ): Boolean {
        val eventDate = dates?.start?.localDate ?: return false
        val start = parseIsoDate(startDate) ?: return true
        val end = parseIsoDate(endDate) ?: return true
        val candidate = parseIsoDate(eventDate) ?: return false
        return !candidate.isBefore(start) && !candidate.isAfter(end)
    }

    private fun TmEvent.passesTripFilters(
        startDate: String,
        endDate: String
    ): Boolean {
        return passesCommonFilters() && isWithinLocalDateWindow(startDate, endDate)
    }

    private fun TmEvent.passesChatFilters(
        startDate: String?,
        endDate: String?
    ): Boolean {
        if (!passesCommonFilters()) return false
        return if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) {
            true
        } else {
            isWithinLocalDateWindow(startDate, endDate)
        }
    }

    // Discovery can return ticket inventory such as season passes, memberships,
    // and parking products. Keep only event-like listings for trip planning.
    private fun TmEvent.passesCommonFilters(): Boolean {
        if (name.isBlank()) return false
        if (isCancelledInventory()) return false
        if (isLikelyNonEventInventory()) return false
        return true
    }

    private fun TmEvent.isCancelledInventory(): Boolean {
        return dates?.status?.code
            ?.trim()
            ?.lowercase(Locale.US) == "cancelled"
    }

    private fun TmEvent.isLikelyNonEventInventory(): Boolean {
        val normalizedName = name.normalizedInventoryText()
        val normalizedUrl = url.orEmpty().normalizedInventoryText()
        val seasonInventory = normalizedName.contains("season") && (
            normalizedName.contains("ticket") ||
                normalizedName.contains("pass") ||
                seasonYearRangeRegex.containsMatchIn(normalizedName) ||
                normalizedUrl.contains("/season/")
            )
        if (seasonInventory) return true

        return excludedInventoryTerms.any { term ->
            normalizedName.contains(term) || normalizedUrl.contains(term)
        }
    }

    private fun String.normalizedInventoryText(): String {
        return lowercase(Locale.US)
            .replace("&", " and ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun selectBestImage(images: List<TmImage>): TmImage? {
        return images
            .filter { image -> image.url.isNotBlank() }
            .maxWithOrNull(
                compareBy<TmImage> { if (it.fallback) 0 else 1 }
                    .thenBy { it.width * it.height }
            )
    }

    private fun selectPriceRange(ranges: List<TmPriceRange>): TmPriceRange? {
        return ranges.firstOrNull { range ->
            range.min != null || range.max != null || !range.currency.isNullOrBlank()
        }
    }

    private fun buildStaticMapMetadata(
        latitude: Double?,
        longitude: Double?
    ): Map<String, String>? {
        if (latitude == null || longitude == null) return null
        return mapOf(
            ATTR_STATIC_MAP_URL to StaticMapUrlFactory.buildUrl(latitude, longitude),
            ATTR_STATIC_MAP_PROVIDER to StaticMapUrlFactory.PROVIDER
        )
    }

    private fun TmVenue.fullAddress(): String? {
        return listOfNotNull(
            address?.line1?.takeIf { it.isNotBlank() },
            address?.line2?.takeIf { it.isNotBlank() },
            city?.name?.takeIf { it.isNotBlank() },
            country?.name?.takeIf { it.isNotBlank() }
        ).joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun normalizeLocation(location: String): String? {
        return location.substringBefore(",").trim().takeIf { it.isNotBlank() }
    }

    private fun normalizeClassification(classification: String?): String? {
        return when (classification?.trim()?.lowercase(Locale.US)) {
            null, "" -> null
            "music" -> "Music"
            "sports" -> "Sports"
            "arts", "art", "arts & theatre", "arts and theatre", "theatre", "theater" -> "Arts & Theatre"
            "family" -> "Family"
            "film" -> "Film"
            "misc", "miscellaneous" -> "Miscellaneous"
            else -> classification.trim()
        }
    }

    private fun parseIsoDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, isoDateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun String?.toStorageTime(): String {
        return this
            ?.substringBefore(".")
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                if (value.length >= 5) value.substring(0, 5) else value
            }
            .orEmpty()
    }

    private fun trimTrailingZero(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }
}
