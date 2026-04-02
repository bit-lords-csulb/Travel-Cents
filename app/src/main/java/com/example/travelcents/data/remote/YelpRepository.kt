package com.example.travelcents.data.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.YelpBusiness
import com.example.travelcents.data.model.YelpEvent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object YelpRepository {

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

    // Dietary alias map for Yelp category params
    private val dietaryAliases = mapOf(
        "Vegan" to "vegan",
        "Vegetarian" to "vegetarian",
        "Halal" to "halal",
        "Kosher" to "kosher",
        "Gluten-Free" to "gluten_free"
    )

    // 1 call per day for the given location + dietary prefs. Returns a TravelEvent with 5 options.
    suspend fun searchRestaurants(
        location: String,
        date: String,
        itineraryId: String,
        dietary: List<String> = emptyList()
    ): TravelEvent? {
        return try {
            val dietaryCategories = dietary.mapNotNull { dietaryAliases[it] }
            val categories = buildString {
                append("restaurants")
                if (dietaryCategories.isNotEmpty()) {
                    append(",")
                    append(dietaryCategories.joinToString(","))
                }
            }

            val params = mapOf(
                "location" to location,
                "categories" to categories,
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