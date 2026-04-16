package com.example.travelcents.data.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.YelpBusiness
import com.example.travelcents.data.model.YelpEvent
import com.example.travelcents.data.model.YelpReview
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object YelpRepository {

    private const val OPTIONS_PER_EVENT = 5
    private const val SEARCH_PAGE_SIZE = 50
    private const val SEARCH_MAX_RESULTS = 240

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
                businessToEventOption(biz, eventId, isSelected = idx == 0, source = "yelp")
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
                details = buildMap {
                    put("restaurant_name", selected.name)
                    selected.price?.let { put("price_tier", it) }
                    put("rating", selected.rating.toString())
                    put("review_count", selected.reviewCount.toString())
                    selected.location?.displayAddress?.joinToString(", ")?.let { put("address", it) }
                    put("categories", selected.categories.joinToString(", ") { it.title })
                    put("yelp_id", selected.id)
                },
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
                businessToEventOption(biz, eventId, isSelected = idx == 0, source = "yelp")
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
                details = buildMap {
                    put("activity_name", selected.name)
                    selected.price?.let { put("price_tier", it) }
                    put("rating", selected.rating.toString())
                    put("review_count", selected.reviewCount.toString())
                    selected.location?.displayAddress?.joinToString(", ")?.let { put("address", it) }
                    put("categories", selected.categories.joinToString(", ") { it.title })
                    put("yelp_id", selected.id)
                },
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

    // Single pooled fetch for activities (limit=20). Use distributePoolToEvents to assign across days.
    suspend fun fetchActivityPool(location: String): List<YelpBusiness> {
        return try {
            val params = mapOf(
                "location" to location,
                "categories" to "arts,museums,tours,landmarks",
                "limit" to "20",
                "sort_by" to "rating"
            )
            api.searchBusinesses(params).businesses
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

    // Distribute a Yelp business pool across trip days round-robin.
    // Day i gets pool[i] as primary; alternatives are up to 4 other businesses from the pool.
    // Days beyond pool size are skipped (returns fewer events than dates if pool is small).
    fun distributePoolToEvents(
        pool: List<YelpBusiness>,
        dates: List<String>,
        type: String,
        itineraryId: String
    ): List<TravelEvent> {
        if (pool.isEmpty() || dates.isEmpty()) return emptyList()
        val nameKey = if (type == "restaurant") "restaurant_name" else "activity_name"
        val (startTime, endTime) = if (type == "restaurant") "19:00" to "21:00" else "10:00" to "12:00"

        return dates.mapIndexedNotNull { dayIndex, date ->
            if (dayIndex >= pool.size) return@mapIndexedNotNull null
            val primary = pool[dayIndex]
            val eventId = UUID.randomUUID().toString()
            val alternatives = pool.filterIndexed { i, _ -> i != dayIndex }.take(OPTIONS_PER_EVENT - 1)
            val options = listOf(
                businessToEventOption(primary, eventId, isSelected = true, source = "yelp")
            ) + alternatives.map {
                businessToEventOption(it, eventId, isSelected = false, source = "yelp")
            }
            TravelEvent(
                eventId = eventId,
                type = type,
                itineraryId = itineraryId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                imageUrl = primary.imageUrl,
                details = buildMap {
                    put(nameKey, primary.name)
                    primary.price?.let { put("price_tier", it) }
                    put("rating", primary.rating.toString())
                    put("review_count", primary.reviewCount.toString())
                    primary.location?.displayAddress?.joinToString(", ")?.let { put("address", it) }
                    put("categories", primary.categories.joinToString(", ") { it.title })
                    put("yelp_id", primary.id)
                },
                options = options
            )
        }
    }

    // Fetch full business details (photos, hours, etc.) — called lazily on first expand
    suspend fun getBusinessDetail(yelpId: String): YelpBusiness? {
        return try { api.getBusinessDetail(yelpId) } catch (_: Exception) { null }
    }

    // Fetch up to 3 review snippets — called lazily on first expand
    suspend fun getBusinessReviews(yelpId: String): List<YelpReview> {
        return try { api.getBusinessReviews(yelpId, limit = 3).reviews } catch (_: Exception) { emptyList() }
    }

    private fun businessToEventOption(
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
        details = buildMap {
            put("name", biz.name)
            biz.price?.let { put("price_tier", it) }
            put("rating", biz.rating.toString())
            put("review_count", biz.reviewCount.toString())
            biz.location?.displayAddress?.joinToString(", ")?.let { put("address", it) }
            put("categories", biz.categories.joinToString(", ") { it.title })
            put("yelp_id", biz.id)
            biz.phone?.let { put("phone", it) }
            biz.distance?.let { put("distance_m", it.toString()) }
            if (biz.transactions.contains("delivery")) put("delivery", "true")
            if (biz.transactions.contains("pickup")) put("pickup", "true")
        }
    )

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
}
