package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.media.StaticMapUrlFactory
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_HAS_FOOD_ORDER
import com.example.travelcents.data.trip.model.ATTR_HAS_REQUEST_A_QUOTE
import com.example.travelcents.data.trip.model.ATTR_HAS_RESERVATIONS
import com.example.travelcents.data.trip.model.ATTR_HAS_WAITLIST
import com.example.travelcents.data.trip.model.ATTR_HOURS_RAW
import com.example.travelcents.data.trip.model.ATTR_HOURS_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_IS_CLOSED
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_PHONE
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.ATTR_PROFILE_PHOTO_URL
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_DETAIL_ENRICHED
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.detailValue
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale

object YelpRepository {

    private const val OPTIONS_PER_EVENT = 5
    private const val SEARCH_PAGE_SIZE = 50
    private const val SEARCH_MAX_RESULTS = 240
    private val businessDetailCache = ConcurrentHashMap<String, YelpBusiness>()

    data class YelpEventEnrichmentResult(
        val event: TravelEvent,
        val options: List<EventOption>,
        val updatedOptionIds: Set<String>,
        val yelpId: String
    )

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.YELP_API_KEY}")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: YelpApiService = Retrofit.Builder()
        .baseUrl("https://api.yelp.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YelpApiService::class.java)

    // 1 call per day for the given location. Returns a TravelEvent with 5 options.
    suspend fun searchRestaurants(
        location: String,
        date: String,
        itineraryId: String
    ): TravelEvent? {
        return try {
            val params = mapOf(
                "location" to location,
                "categories" to "restaurants",
                "limit" to "5",
                "sort_by" to "rating"
            )

            val response = api.searchBusinesses(params)
            if (response.businesses.isEmpty()) return null

            val eventId = UUID.randomUUID().toString()
            val options = response.businesses.mapIndexed { idx, biz ->
                mapBusinessToEventOption(biz, eventId, isSelected = idx == 0, source = "yelp")
            }
            val selected = response.businesses.first()

            TravelEvent(
                eventId = eventId,
                type = "restaurant",
                itineraryId = itineraryId,
                date = date,
                startTime = "19:00",
                endTime = "21:00",
                imageUrl = selected.imageUrl,
                details = businessEventDetails(selected),
                options = options
            )
        } catch (_: Exception) {
            null
        }
    }

    // 1 call per day for the given location. Returns a TravelEvent with 5 options.
    suspend fun searchActivities(
        location: String,
        date: String,
        itineraryId: String
    ): TravelEvent? {
        return try {
            val params = mapOf(
                "location" to location,
                "categories" to "arts,museums,tours,landmarks",
                "limit" to "5",
                "sort_by" to "rating"
            )

            val response = api.searchBusinesses(params)
            if (response.businesses.isEmpty()) return null

            val eventId = UUID.randomUUID().toString()
            val options = response.businesses.mapIndexed { idx, biz ->
                mapBusinessToEventOption(biz, eventId, isSelected = idx == 0, source = "yelp")
            }
            val selected = response.businesses.first()

            TravelEvent(
                eventId = eventId,
                type = "activity",
                itineraryId = itineraryId,
                date = date,
                startTime = "10:00",
                endTime = "12:00",
                imageUrl = selected.imageUrl,
                details = businessEventDetails(selected),
                options = options
            )
        } catch (_: Exception) {
            null
        }
    }

    // 1 call per trip covering the full date range. Returns up to 20 local events as TravelEvents.
    suspend fun searchEvents(
        location: String,
        startDate: String,
        endDate: String,
        itineraryId: String
    ): List<TravelEvent> {
        return try {
            val startEpoch = iso8601ToEpoch(startDate)
            val endEpoch = iso8601ToEpoch(endDate)

            val params = buildMap<String, String> {
                put("location", location)
                put("limit", "20")
                if (startEpoch > 0) put("start_date", startEpoch.toString())
                if (endEpoch > 0) put("end_date", endEpoch.toString())
            }

            val response = api.searchEvents(params)
            response.events.mapNotNull { yelpEvent ->
                yelpEventToTravelEvent(yelpEvent, itineraryId)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Single pooled fetch for restaurants. Size the pool to the itinerary, paging as needed.
    suspend fun fetchRestaurantPool(
        location: String,
        targetCount: Int = OPTIONS_PER_EVENT
    ): List<YelpBusiness> {
        return try {
            fetchBusinessPool(
                location = location,
                categories = "restaurants",
                targetCount = targetCount,
                sortBy = "rating"
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Single pooled fetch for activities. Size the pool to the itinerary, paging as needed.
    suspend fun fetchActivityPool(
        location: String,
        targetCount: Int = OPTIONS_PER_EVENT
    ): List<YelpBusiness> {
        return try {
            fetchBusinessPool(
                location = location,
                categories = "arts,museums,tours,landmarks",
                targetCount = targetCount,
                sortBy = "rating"
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchBusinessPool(
        location: String,
        categories: String,
        targetCount: Int,
        sortBy: String
    ): List<YelpBusiness> {
        val desiredCount = targetCount
            .coerceAtLeast(OPTIONS_PER_EVENT)
            .coerceAtMost(SEARCH_MAX_RESULTS)

        val businesses = mutableListOf<YelpBusiness>()
        val seenIds = linkedSetOf<String>()
        var offset = 0

        while (businesses.size < desiredCount && offset < SEARCH_MAX_RESULTS) {
            val remaining = desiredCount - businesses.size
            val limit = minOf(SEARCH_PAGE_SIZE, remaining, SEARCH_MAX_RESULTS - offset)
            if (limit <= 0) break

            val page = api.searchBusinesses(
                mapOf(
                    "location" to location,
                    "categories" to categories,
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                    "sort_by" to sortBy
                )
            ).businesses

            if (page.isEmpty()) break

            val newBusinesses = page.filter { seenIds.add(it.id) }
            businesses += newBusinesses

            if (page.size < limit) break
            offset += limit
        }

        return businesses
    }

    // Distribute a Yelp business pool across trip days.
    // Each day keeps only its preferred chunk so option docs scale linearly with trip length.
    // Small pools still stay fully available until they are exhausted across days.
    fun distributePoolToEvents(
        pool: List<YelpBusiness>,
        dates: List<String>,
        type: String,
        itineraryId: String
    ): List<TravelEvent> {
        if (pool.isEmpty() || dates.isEmpty()) return emptyList()
        val (startTime, endTime) = if (type == "restaurant") "19:00" to "21:00" else "10:00" to "12:00"

        return dates.mapIndexedNotNull { dayIndex, date ->
            val primary = selectPrimaryBusiness(pool, dayIndex) ?: return@mapIndexedNotNull null
            val eventId = UUID.randomUUID().toString()
            val orderedBusinesses = orderedBusinessesForDay(pool, dayIndex, primary)
            val options = orderedBusinesses.mapIndexed { optionIndex, business ->
                mapBusinessToEventOption(
                    biz = business,
                    eventId = eventId,
                    isSelected = optionIndex == 0,
                    source = "yelp"
                )
            }
            TravelEvent(
                eventId = eventId,
                type = type,
                itineraryId = itineraryId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                imageUrl = primary.imageUrl,
                details = businessEventDetails(primary),
                options = options
            )
        }
    }

    private fun selectPrimaryBusiness(
        pool: List<YelpBusiness>,
        dayIndex: Int
    ): YelpBusiness? {
        val preferredGroup = pool.drop(dayIndex * OPTIONS_PER_EVENT).take(OPTIONS_PER_EVENT)
        return when {
            preferredGroup.isNotEmpty() -> preferredGroup.first()
            dayIndex < pool.size -> pool[dayIndex]
            else -> null
        }
    }

    private fun orderedBusinessesForDay(
        pool: List<YelpBusiness>,
        dayIndex: Int,
        primary: YelpBusiness
    ): List<YelpBusiness> {
        if (pool.size <= OPTIONS_PER_EVENT) {
            return pool
        }

        val preferredIds = linkedSetOf<String>()
        val ordered = mutableListOf<YelpBusiness>()

        pool.drop(dayIndex * OPTIONS_PER_EVENT)
            .take(OPTIONS_PER_EVENT)
            .forEach { business ->
                if (preferredIds.add(business.id)) {
                    ordered += business
                }
            }

        if (ordered.isEmpty() && preferredIds.add(primary.id)) {
            ordered += primary
        }

        return ordered
    }

    // Fetch full business details (photos, hours, etc.) — called lazily on first expand
    suspend fun getBusinessDetail(
        yelpId: String,
        forceRefresh: Boolean = false
    ): YelpBusiness? {
        if (!forceRefresh) {
            businessDetailCache[yelpId]?.let { return it }
        }

        return try {
            val detail = api.getBusinessDetail(yelpId)
            businessDetailCache[yelpId] = detail
            detail
        } catch (_: Exception) {
            null
        }
    }

    // Fetch up to 3 review snippets — called lazily on first expand
    suspend fun getBusinessReviews(yelpId: String): List<YelpReview> {
        return try { api.getBusinessReviews(yelpId, limit = 3).reviews } catch (_: Exception) { emptyList() }
    }

    fun needsEventEnrichment(
        event: TravelEvent,
        options: List<EventOption> = event.options
    ): Boolean {
        val yelpId = selectedYelpId(event, options) ?: return false
        val eventNeedsEnrichment = event.details[ATTR_YELP_DETAIL_ENRICHED] != "true"
        val selectedOptionNeedsEnrichment = options
            .firstOrNull { it.selected && it.detailValue(DETAIL_YELP_ID) == yelpId }
            ?.details
            ?.get(ATTR_YELP_DETAIL_ENRICHED)
            ?.let { it != "true" }
            ?: false
        return eventNeedsEnrichment || selectedOptionNeedsEnrichment
    }

    suspend fun enrichYelpBackedEvent(
        event: TravelEvent,
        options: List<EventOption> = event.options,
        forceRefresh: Boolean = false
    ): YelpEventEnrichmentResult? {
        val yelpId = selectedYelpId(event, options) ?: return null
        if (!forceRefresh && !needsEventEnrichment(event, options)) return null

        val detail = getBusinessDetail(yelpId, forceRefresh = forceRefresh) ?: return null
        return applyBusinessDetailEnrichment(event, options, detail)
    }

    internal fun applyBusinessDetailEnrichment(
        event: TravelEvent,
        options: List<EventOption> = event.options,
        detail: YelpBusiness
    ): YelpEventEnrichmentResult? {
        val yelpId = selectedYelpId(event, options) ?: return null
        val enrichmentDetails = businessDetailAttributes(detail) + (ATTR_YELP_DETAIL_ENRICHED to "true")
        val enrichmentPhotos = detail.photos.orEmpty()
            .filter { it.isNotBlank() }
            .distinct()
        val enrichmentImageUrl = detail.imageUrl.takeIf { it.isNotBlank() }

        val updatedOptions = options.map { option ->
            if (option.detailValue(DETAIL_YELP_ID) != yelpId) return@map option
            val updatedDetails = option.details + enrichmentDetails
            option.copy(
                imageUrl = enrichmentImageUrl ?: option.imageUrl,
                photoUrls = enrichmentPhotos.ifEmpty { option.photoUrls },
                details = updatedDetails
            )
        }

        val selectedOption = updatedOptions.firstOrNull { it.selected }
        val updatedEventDetails = event.details + enrichmentDetails
        val updatedEvent = event.copy(
            imageUrl = enrichmentImageUrl ?: selectedOption?.imageUrl?.takeIf { it.isNotBlank() } ?: event.imageUrl,
            photoUrls = enrichmentPhotos.ifEmpty {
                selectedOption?.photoUrls?.takeIf { it.isNotEmpty() } ?: event.photoUrls
            },
            details = updatedEventDetails
        )

        val updatedOptionIds = options.zip(updatedOptions)
            .filter { (before, after) -> before != after }
            .map { (_, after) -> after.optionId }
            .toSet()

        return YelpEventEnrichmentResult(
            event = updatedEvent,
            options = updatedOptions,
            updatedOptionIds = updatedOptionIds,
            yelpId = yelpId
        )
    }

    internal fun mapBusinessToEventOption(
        biz: YelpBusiness,
        eventId: String,
        isSelected: Boolean,
        source: String
    ): EventOption = EventOption(
        optionId = UUID.randomUUID().toString(),
        eventId = eventId,
        source = source,
        selected = isSelected,
        imageUrl = biz.imageUrl,
        details = businessDetailAttributes(biz)
    )

    internal fun businessEventDetails(biz: YelpBusiness): Map<String, String> {
        return businessDetailAttributes(biz)
    }

    internal fun businessDetailAttributes(biz: YelpBusiness): Map<String, String> = buildMap {
        put(DETAIL_YELP_ID, biz.id)
        put(ATTR_BUSINESS_NAME, biz.name)
        biz.price?.let { put(ATTR_PRICE_TIER, it) }
        put(ATTR_AVERAGE_RATING, biz.rating.toString())
        put(ATTR_REVIEW_COUNT, biz.reviewCount.toString())
        put(ATTR_IS_CLOSED, biz.isClosed.toString())
        biz.location?.displayAddress
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
            ?.let { put(ATTR_BUSINESS_ADDRESS, it) }
        biz.categories
            .joinToString(", ") { it.title }
            .takeIf { it.isNotBlank() }
            ?.let { put(ATTR_CATEGORIES, it) }
        biz.phone?.takeIf { it.isNotBlank() }?.let { put(ATTR_PHONE, it) }
        biz.url?.takeIf { it.isNotBlank() }?.let { put(ATTR_YELP_URL, it) }
        biz.imageUrl.takeIf { it.isNotBlank() }?.let { put(ATTR_PROFILE_PHOTO_URL, it) }
        biz.coordinates?.latitude?.let { put(ATTR_LATITUDE, it.toString()) }
        biz.coordinates?.longitude?.let { put(ATTR_LONGITUDE, it.toString()) }
        buildStaticMapMetadata(biz)?.forEach { (key, value) -> put(key, value) }
        formatHoursSummary(biz)?.let { put(ATTR_HOURS_SUMMARY, it) }
        encodeHoursRaw(biz)?.let { put(ATTR_HOURS_RAW, it) }

        val attributes = biz.attributes.orEmpty()
        extractStringAttribute(
            attributes,
            "menu_url",
            "menuUrl"
        )?.let { put(ATTR_MENU_URL, it) }

        extractBooleanAttribute(
            attributes,
            "restaurants_reservations",
            "restaurant_reservation"
        )?.let { put(ATTR_HAS_RESERVATIONS, it.toString()) }
        extractStringAttribute(attributes, "reservation_url")
            ?.let { put(ATTR_HAS_RESERVATIONS, "true") }

        extractBooleanAttribute(
            attributes,
            "waitlist_reservation",
            "waitlist"
        )?.let { put(ATTR_HAS_WAITLIST, it.toString()) }

        extractBooleanAttribute(
            attributes,
            "request_a_quote",
            "request_a_quote_enabled",
            "request_quote"
        )?.let { put(ATTR_HAS_REQUEST_A_QUOTE, it.toString()) }
        extractStringAttribute(attributes, "request_a_quote_url")
            ?.let { put(ATTR_HAS_REQUEST_A_QUOTE, "true") }

        extractBooleanAttribute(
            attributes,
            "food_ordering",
            "food_order",
            "restaurant_ordering"
        )?.let { put(ATTR_HAS_FOOD_ORDER, it.toString()) }
    }

    private fun yelpEventToTravelEvent(event: YelpEvent, itineraryId: String): TravelEvent? {
        val date = event.timeStart.substringBefore("T").ifBlank { return null }
        return TravelEvent(
            eventId = UUID.randomUUID().toString(),
            type = "activity",
            itineraryId = itineraryId,
            date = date,
            startTime = event.timeStart.substringAfter("T", "").take(5),
            endTime = event.timeEnd?.substringAfter("T", "")?.take(5) ?: "",
            imageUrl = event.imageUrl,
            details = buildMap {
                put("activity_name", event.name)
                put("category", event.category)
                put("is_free", event.isFree.toString())
                event.cost?.let { put("cost", it.toString()) }
                event.costMax?.let { put("cost_max", it.toString()) }
                event.ticketsUrl?.let { put("tickets_url", it) }
                event.location?.displayAddress?.joinToString(", ")?.let { put("address", it) }
                if (event.description.isNotBlank()) put("description", event.description.take(200))
                put("yelp_event_id", event.id)
            }
        )
    }

    // Parse "YYYY-MM-DD" to Unix epoch seconds
    private fun iso8601ToEpoch(date: String): Long {
        return try {
            val parts = date.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            // simple Julian-day calculation, accurate enough for Yelp date range filtering
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
            (jdn - 2440588L) * 86400L
        } catch (_: Exception) {
            0L
        }
    }

    private fun buildStaticMapMetadata(biz: YelpBusiness): Map<String, String>? {
        val latitude = biz.coordinates?.latitude ?: return null
        val longitude = biz.coordinates?.longitude ?: return null
        return mapOf(
            ATTR_STATIC_MAP_URL to StaticMapUrlFactory.buildUrl(latitude, longitude),
            ATTR_STATIC_MAP_PROVIDER to StaticMapUrlFactory.PROVIDER
        )
    }

    private fun formatHoursSummary(biz: YelpBusiness): String? {
        val periods = biz.hours.orEmpty().flatMap { it.open.orEmpty() }
        if (periods.isEmpty()) return null
        return periods.joinToString("; ") { period ->
            "${dayLabel(period.day)} ${period.start}-${period.end}"
        }
    }

    private fun encodeHoursRaw(biz: YelpBusiness): String? {
        val periods = biz.hours.orEmpty().flatMap { it.open.orEmpty() }
        if (periods.isEmpty()) return null
        return periods.joinToString("|") { period ->
            listOf(
                period.day.toString(),
                period.start,
                period.end,
                period.isOvernight.toString()
            ).joinToString(",")
        }
    }

    private fun dayLabel(day: Int): String {
        return when (day) {
            0 -> "Mon"
            1 -> "Tue"
            2 -> "Wed"
            3 -> "Thu"
            4 -> "Fri"
            5 -> "Sat"
            6 -> "Sun"
            else -> day.toString()
        }
    }

    private fun extractStringAttribute(attributes: Map<String, Any>, vararg keys: String): String? {
        return keys.asSequence()
            .mapNotNull { key ->
                val value = attributes[key] ?: return@mapNotNull null
                when (value) {
                    is String -> value.takeIf { it.isNotBlank() }
                    else -> value.toString().takeIf { it.isNotBlank() }
                }
            }
            .firstOrNull()
    }

    private fun extractBooleanAttribute(attributes: Map<String, Any>, vararg keys: String): Boolean? {
        return keys.asSequence()
            .mapNotNull { key ->
                when (val value = attributes[key]) {
                    is Boolean -> value
                    is Number -> value.toInt() != 0
                    is String -> when (value.lowercase(Locale.US)) {
                        "true", "1", "yes" -> true
                        "false", "0", "no" -> false
                        else -> null
                    }
                    else -> null
                }
            }
            .firstOrNull()
    }

    private fun selectedYelpId(
        event: TravelEvent,
        options: List<EventOption>
    ): String? {
        return options.firstOrNull { it.selected }
            ?.detailValue(DETAIL_YELP_ID)
            ?.takeIf { it.isNotBlank() }
            ?: event.detailValue(DETAIL_YELP_ID)?.takeIf { it.isNotBlank() }
    }
}


