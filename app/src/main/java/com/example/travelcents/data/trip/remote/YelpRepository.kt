package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.media.StaticMapUrlFactory
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_DIETARY_TAGS
import com.example.travelcents.data.trip.model.ATTR_HAS_FOOD_ORDER
import com.example.travelcents.data.trip.model.ATTR_HAS_OUTDOOR_SEATING
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
import com.example.travelcents.data.trip.model.ATTR_RESERVATION_OFFER_COUNT
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_DETAIL_ENRICHED
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_ACTIVITIES
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_RESTAURANTS
import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpEvent
import com.example.travelcents.data.trip.model.YelpOptionPoolItem
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.detailValue
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
        targetCount: Int = OPTIONS_PER_EVENT,
        yelpCategories: String? = null,
        term: String? = null
    ): List<YelpBusiness> {
        return try {
            fetchBusinessPool(
                location = location,
                categories = yelpCategories?.takeIf { it.isNotBlank() } ?: "restaurants",
                term = term,
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
        targetCount: Int = OPTIONS_PER_EVENT,
        yelpCategories: String? = null
    ): List<YelpBusiness> {
        return try {
            fetchBusinessPool(
                location = location,
                categories = yelpCategories?.takeIf { it.isNotBlank() }
                    ?: "arts,museums,tours,landmarks",
                term = null,
                targetCount = targetCount,
                sortBy = "rating"
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchAdditionalPoolItems(
        location: String,
        poolType: String,
        existingProviderIds: Set<String>,
        targetCount: Int = OPTIONS_PER_EVENT
    ): List<YelpOptionPoolItem> {
        val categories = categoriesForPoolType(poolType) ?: return emptyList()
        return try {
            fetchAdditionalBusinessPool(
                location = location,
                categories = categories,
                existingProviderIds = existingProviderIds,
                targetCount = targetCount
            ).let(::mapBusinessesToPoolItems)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchBusinessPool(
        location: String,
        categories: String,
        term: String? = null,
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
                buildMap {
                    put("location", location)
                    put("categories", categories)
                    put("limit", limit.toString())
                    put("offset", offset.toString())
                    put("sort_by", sortBy)
                    term?.takeIf { it.isNotBlank() }?.let { put("term", it) }
                }
            ).businesses

            if (page.isEmpty()) break

            val newBusinesses = page.filter { seenIds.add(it.id) }
            businesses += newBusinesses

            if (page.size < limit) break
            offset += limit
        }

        return businesses
    }

    private suspend fun fetchAdditionalBusinessPool(
        location: String,
        categories: String,
        existingProviderIds: Set<String>,
        targetCount: Int
    ): List<YelpBusiness> {
        val desiredCount = targetCount
            .coerceAtLeast(1)
            .coerceAtMost(SEARCH_MAX_RESULTS)

        val businesses = mutableListOf<YelpBusiness>()
        val seenIds = existingProviderIds.toMutableSet()
        var offset = existingProviderIds.size.coerceAtMost(SEARCH_MAX_RESULTS)

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
                    "sort_by" to "rating"
                )
            ).businesses

            if (page.isEmpty()) break

            businesses += page.filter { business -> seenIds.add(business.id) }

            if (page.size < limit) break
            offset += limit
        }

        return businesses
    }

    fun mapBusinessesToPoolItems(pool: List<YelpBusiness>): List<YelpOptionPoolItem> {
        return pool
            .filter { business -> business.id.isNotBlank() }
            .distinctBy(YelpBusiness::id)
            .map(::mapBusinessToPoolItem)
    }

    fun distributePoolToSelectedEvents(
        pool: List<YelpOptionPoolItem>,
        dates: List<String>,
        type: String,
        itineraryId: String
    ): List<TravelEvent> {
        if (pool.isEmpty() || dates.isEmpty()) return emptyList()
        val (startTime, endTime) = if (type == "restaurant") "19:00" to "21:00" else "10:00" to "12:00"

        return dates.mapIndexedNotNull { dayIndex, date ->
            val primary = selectPrimaryPoolItem(pool, dayIndex) ?: return@mapIndexedNotNull null
            TravelEvent(
                eventId = UUID.randomUUID().toString(),
                type = type,
                itineraryId = itineraryId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                imageUrl = primary.imageUrl,
                selectedOptionId = primary.providerId,
                details = primary.toEventDetails(),
                options = emptyList()
            )
        }
    }

    fun orderedPoolItemsForEvent(
        pool: List<YelpOptionPoolItem>,
        tripId: String,
        eventId: String,
        selectedProviderId: String? = null
    ): List<YelpOptionPoolItem> {
        if (pool.isEmpty()) return emptyList()

        val selected = selectedProviderId
            ?.takeIf { it.isNotBlank() }
            ?.let { providerId -> pool.firstOrNull { item -> item.providerId == providerId } }

        val remainder = pool
            .asSequence()
            .filterNot { item -> item.providerId == selected?.providerId }
            .sortedWith(
                compareBy<YelpOptionPoolItem>(
                    { stablePoolSortKey(tripId, eventId, it.providerId) },
                    { it.providerId }
                )
            )
            .toList()

        return buildList {
            selected?.let(::add)
            addAll(remainder)
        }
    }

    private fun mapBusinessToPoolItem(biz: YelpBusiness): YelpOptionPoolItem {
        return YelpOptionPoolItem(
            providerId = biz.id,
            source = "yelp",
            name = biz.name,
            imageUrl = biz.imageUrl,
            rating = biz.rating,
            reviewCount = biz.reviewCount,
            priceTier = biz.price.orEmpty(),
            categories = biz.categories.mapNotNull { category ->
                category.title.takeIf { it.isNotBlank() }
            },
            shortAddress = biz.location?.displayAddress
                ?.joinToString(", ")
                .orEmpty(),
            latitude = biz.coordinates?.latitude,
            longitude = biz.coordinates?.longitude,
            staticMapUrl = buildStaticMapMetadata(biz)?.get(ATTR_STATIC_MAP_URL).orEmpty(),
            staticMapProvider = buildStaticMapMetadata(biz)?.get(ATTR_STATIC_MAP_PROVIDER).orEmpty(),
            yelpUrl = biz.url.orEmpty()
        )
    }

    private fun selectPrimaryPoolItem(
        pool: List<YelpOptionPoolItem>,
        dayIndex: Int
    ): YelpOptionPoolItem? {
        val preferredGroup = pool.drop(dayIndex * OPTIONS_PER_EVENT).take(OPTIONS_PER_EVENT)
        return when {
            preferredGroup.isNotEmpty() -> preferredGroup.first()
            dayIndex < pool.size -> pool[dayIndex]
            else -> null
        }
    }

    private fun stablePoolSortKey(
        tripId: String,
        eventId: String,
        providerId: String
    ): Long {
        val stableKey = "$tripId|$eventId|$providerId"
        return stableKey.hashCode().toLong() and 0x00000000ffffffffL
    }

    private fun categoriesForPoolType(poolType: String): String? {
        return when (poolType) {
            YELP_POOL_TYPE_RESTAURANTS -> "restaurants"
            YELP_POOL_TYPE_ACTIVITIES -> "arts,museums,tours,landmarks"
            else -> null
        }
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
        extractDietaryTags(attributes)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?.let { put(ATTR_DIETARY_TAGS, it) }
        extractBooleanAttribute(
            attributes,
            "outdoor_seating",
            "outdoorseating",
            "outdoor_dining"
        )?.let { put(ATTR_HAS_OUTDOOR_SEATING, it.toString()) }

        extractBooleanAttribute(
            attributes,
            "restaurants_reservations",
            "restaurant_reservation"
        )?.let { put(ATTR_HAS_RESERVATIONS, it.toString()) }
        extractStringAttribute(attributes, "reservation_url")
            ?.let { reservationUrl ->
                put(ATTR_HAS_RESERVATIONS, "true")
                put(ATTR_RESERVATION_OFFER_COUNT, "1")
                put("reservation_offer_0_source", "Yelp")
                put("reservation_offer_0_link", reservationUrl)
                put("reservation_offer_0_deeplink_type", "yelp")
            }

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

    private fun extractDietaryTags(attributes: Map<String, Any>): List<String> {
        val tagMappings = listOf(
            "vegan" to arrayOf("vegan", "vegan_menu", "vegan_options"),
            "vegetarian" to arrayOf("vegetarian", "vegetarian_menu", "vegetarian_options"),
            "gluten-free" to arrayOf("gluten_free", "gluten_free_menu"),
            "halal" to arrayOf("halal", "halal_menu"),
            "kosher" to arrayOf("kosher", "kosher_menu")
        )

        return tagMappings.mapNotNull { (label, keys) ->
            extractBooleanAttribute(attributes, *keys)?.takeIf { it }?.let { label }
        }
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


