package com.example.travelcents.data.ai.repository

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.media.StaticMapUrlFactory
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.trip.local.DestinationTimeZones
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_HOURS_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.ATTR_PROFILE_PHOTO_URL
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.ATTR_WEBSITE_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.defaultTripNameForDestination
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.UUID
import java.util.concurrent.TimeUnit

data class EmulatorRequest(val data: Map<String, Any>)

data class EmulatorResponse(val result: EmulatorResultData)

data class EmulatorResultData(val itinerary: List<EmulatorActivity> = emptyList())

data class EmulatorActivity(
    val title: String = "",
    val description: String = "",
    val booking_url: String? = null,
    val url: String? = null,
    val website: String? = null,
    val yelp_url: String? = null,
    val image_url: String? = null,
    val photo_urls: List<String>? = null,
    val real_title: String? = null,
    val isNativeBookable: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val option_id: String? = null,
    val activity_id: String? = null,
    @SerializedName(value = "venue_name", alternate = ["venue", "location_name"])
    val venue_name: String? = null,
    @SerializedName(value = "location", alternate = ["location_label"])
    val location: String? = null,
    @SerializedName(value = "address", alternate = ["business_address", "display_address"])
    val address: String? = null,
    @SerializedName(value = "latitude", alternate = ["lat"])
    val latitude: Double? = null,
    @SerializedName(value = "longitude", alternate = ["lng", "lon"])
    val longitude: Double? = null,
    val rating: Double? = null,
    val review_count: Int? = null,
    val price_tier: String? = null,
    val categories: List<String>? = null,
    val hours_summary: String? = null,
    val profile_photo_url: String? = null,
    val details: Map<String, Any?>? = null,
    val metadata: Map<String, Any?>? = null,
    val options: List<EmulatorActivityOption>? = null
)

data class EmulatorActivityOption(
    val option_id: String? = null,
    val activity_id: String? = null,
    val title: String = "",
    val real_title: String? = null,
    val booking_url: String? = null,
    val url: String? = null,
    val website: String? = null,
    val yelp_url: String? = null,
    val image_url: String? = null,
    val photo_urls: List<String>? = null,
    val isNativeBookable: String? = null,
    @SerializedName(value = "venue_name", alternate = ["venue", "location_name"])
    val venue_name: String? = null,
    @SerializedName(value = "location", alternate = ["location_label"])
    val location: String? = null,
    @SerializedName(value = "address", alternate = ["business_address", "display_address"])
    val address: String? = null,
    @SerializedName(value = "latitude", alternate = ["lat"])
    val latitude: Double? = null,
    @SerializedName(value = "longitude", alternate = ["lng", "lon"])
    val longitude: Double? = null,
    val rating: Double? = null,
    val review_count: Int? = null,
    val price_tier: String? = null,
    val categories: List<String>? = null,
    val hours_summary: String? = null,
    val profile_photo_url: String? = null,
    val details: Map<String, Any?>? = null,
    val metadata: Map<String, Any?>? = null,
    val selected: Boolean = false,
    val score: Double? = null
)

interface EmulatorApiService {
    @POST("travel-cents-3e2d9/us-central1/generate_itinerary")
    suspend fun getLocalItinerary(@Body request: EmulatorRequest): EmulatorResponse
}

object TripPlannerRepository {

    private val gson = Gson()

    private const val SYSTEM_PROMPT =
        "You are a travel planner. Always respond with valid JSON only. No markdown, no extra text."

    private val emulatorClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val emulatorApi: EmulatorApiService = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5001/")
        .client(emulatorClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EmulatorApiService::class.java)

    suspend fun generateItinerary(request: TravelRequest): Itinerary {
        val raw = LlmClient.complete(
            messages = listOf(
                LlmMessage(role = "system", content = SYSTEM_PROMPT),
                LlmMessage(role = "user", content = buildItineraryPrompt(request))
            ),
            maxTokens = 4096,
            responseFormat = mapOf("type" to "json_object")
        )
        return parseItinerary(raw, request.userId)
    }

    suspend fun getAIActivities(
        request: TravelRequest,
        itineraryId: String,
        dates: List<String>,
        flightArrival: String,
        activityWindow: Map<String, Any> = emptyMap(),
        flights: List<Map<String, Any>> = emptyList(),
        hotel: Map<String, Any> = emptyMap()
    ): List<TravelEvent> {
        if (request.destination.isBlank() || itineraryId.isBlank() || dates.isEmpty()) return emptyList()

        val payload: Map<String, Any> = buildMap {
            put("destination", request.destination)
            put("dateFrom", request.dateFrom)
            put("dateTo", request.dateTo)
            put("adults", request.adults)
            put("children", request.children)
            put("travelStyle", request.travelStyle)
            put("budgetTotal", request.budgetTotal.toString())
            put("interests", request.interests)
            put("specialRequests", request.specialRequests)
            put("flightArrival", flightArrival)
            put("activityDates", dates)
            if (activityWindow.isNotEmpty()) put("activityWindow", activityWindow)
            if (flights.isNotEmpty()) put("flights", flights)
            if (hotel.isNotEmpty()) put("hotel", hotel)
        }

        val response = emulatorApi.getLocalItinerary(
            EmulatorRequest(data = payload)
        )

        return response.result.itinerary.mapIndexed { index, activity ->
            val eventId = UUID.randomUUID().toString()
            val activityTitle = activity.real_title
                ?.takeIf { it.isNotBlank() }
                ?: activity.title.ifBlank { "Recommended activity" }
            val selectedOptionId = activity.option_id
                ?.takeIf { it.isNotBlank() }
                ?: activity.options.orEmpty()
                    .firstOrNull { it.selected }
                    ?.resolvedOptionId()
            val isNativeBookable = activity.isNativeBookable.isTruthy()
            val activityPhotoUrls = activity.resolvedPhotoUrls()
            val activityImageUrl = activity.resolvedImageUrl(activityPhotoUrls)
            val normalizedDetails = activity.normalizedActivityDetails(
                title = activityTitle,
                description = activity.description,
                bookingUrl = activity.booking_url,
                fallbackImageUrl = activityImageUrl,
                fallbackPhotoUrls = activityPhotoUrls,
                isNativeBookable = isNativeBookable,
                activityId = activity.activity_id,
                selectedOptionId = selectedOptionId,
                source = "emulator"
            )
            val eventOptions = buildActivityOptions(
                eventId = eventId,
                selectedOptionId = selectedOptionId,
                fallbackTitle = activityTitle,
                fallbackDescription = activity.description,
                fallbackBookingUrl = activity.booking_url,
                fallbackImageUrl = activityImageUrl,
                fallbackPhotoUrls = activityPhotoUrls,
                fallbackDetails = normalizedDetails,
                activity = activity
            )

            TravelEvent(
                eventId = eventId,
                type = "activity",
                itineraryId = itineraryId,
                selectedOptionId = selectedOptionId.orEmpty(),
                date = dates[index % dates.size],
                startTime = activity.start_time ?: "10:00",
                endTime = activity.end_time ?: "13:00",
                imageUrl = activityImageUrl,
                photoUrls = activityPhotoUrls,
                isNativeBookable = isNativeBookable.toString(),
                bookingUrl = activity.booking_url,
                details = normalizedDetails,
                options = eventOptions
            )
        }
    }

    private fun buildActivityOptions(
        eventId: String,
        selectedOptionId: String?,
        fallbackTitle: String,
        fallbackDescription: String,
        fallbackBookingUrl: String?,
        fallbackImageUrl: String,
        fallbackPhotoUrls: List<String>,
        fallbackDetails: Map<String, String>,
        activity: EmulatorActivity
    ): List<EventOption> {
        val options = activity.options.orEmpty()
            .mapNotNull { option ->
                val optionId = option.resolvedOptionId() ?: return@mapNotNull null
                val optionTitle = option.real_title
                    ?.takeIf { it.isNotBlank() }
                    ?: option.title.ifBlank { fallbackTitle }
                option.toEventOption(
                    eventId = eventId,
                    optionId = optionId,
                    optionTitle = optionTitle,
                    fallbackDescription = fallbackDescription,
                    fallbackBookingUrl = fallbackBookingUrl,
                    fallbackImageUrl = fallbackImageUrl,
                    fallbackPhotoUrls = fallbackPhotoUrls,
                    fallbackDetails = fallbackDetails,
                    activityId = activity.activity_id,
                    selected = optionId == selectedOptionId || option.selected
                )
            }
            .toMutableList()

        if (!selectedOptionId.isNullOrBlank() && options.none { it.optionId == selectedOptionId }) {
            options.add(
                0,
                EventOption(
                    optionId = selectedOptionId,
                    eventId = eventId,
                    source = "pinecone",
                    selected = true,
                    imageUrl = fallbackImageUrl,
                    photoUrls = fallbackPhotoUrls,
                    details = fallbackDetails + buildMap {
                        put("source", "pinecone")
                        put("isNativeBookable", (!fallbackBookingUrl.isNullOrBlank()).toString())
                    }
                )
            )
        }

        val resolvedSelectedOptionId = selectedOptionId
            ?: options.firstOrNull { it.selected }?.optionId
        return options
            .distinctBy(EventOption::optionId)
            .map { option -> option.copy(selected = option.optionId == resolvedSelectedOptionId) }
    }

    private fun EmulatorActivityOption.toEventOption(
        eventId: String,
        optionId: String,
        optionTitle: String,
        fallbackDescription: String,
        fallbackBookingUrl: String?,
        fallbackImageUrl: String,
        fallbackPhotoUrls: List<String>,
        fallbackDetails: Map<String, String>,
        activityId: String?,
        selected: Boolean
    ): EventOption {
        val optionPhotoUrls = resolvedPhotoUrls()
        val optionImageUrl = resolvedImageUrl(optionPhotoUrls)
        val isBookable = isNativeBookable.isTruthy() || !booking_url.isNullOrBlank()
        val normalizedDetails = normalizedActivityDetails(
            title = optionTitle,
            description = fallbackDescription,
            bookingUrl = booking_url ?: fallbackBookingUrl,
            fallbackImageUrl = optionImageUrl.ifBlank { fallbackImageUrl },
            fallbackPhotoUrls = optionPhotoUrls.ifEmpty { fallbackPhotoUrls },
            isNativeBookable = isBookable,
            activityId = activity_id ?: activityId,
            selectedOptionId = optionId,
            source = "pinecone",
            fallbackDetails = fallbackDetails
        )
        return EventOption(
            optionId = optionId,
            eventId = eventId,
            source = "pinecone",
            selected = selected,
            imageUrl = optionImageUrl.ifBlank { fallbackImageUrl },
            photoUrls = optionPhotoUrls.ifEmpty { fallbackPhotoUrls },
            details = buildActivityOptionDetails(
                fallbackDetails = normalizedDetails,
                title = optionTitle,
                description = fallbackDescription,
                bookingUrl = booking_url ?: fallbackBookingUrl,
                fallbackImageUrl = optionImageUrl.ifBlank { fallbackImageUrl },
                fallbackPhotoUrls = optionPhotoUrls.ifEmpty { fallbackPhotoUrls },
                isNativeBookable = isBookable,
                activityId = activity_id ?: activityId,
                score = score
            )
        )
    }

    private fun buildActivityOptionDetails(
        fallbackDetails: Map<String, String>,
        title: String,
        description: String,
        bookingUrl: String?,
        fallbackImageUrl: String,
        fallbackPhotoUrls: List<String>,
        isNativeBookable: Boolean,
        activityId: String?,
        score: Double?
    ): Map<String, String> = buildMap {
        putAll(fallbackDetails)
        put("activity_name", title)
        put("title", title)
        if (description.isNotBlank()) put("description", description)
        if (!bookingUrl.isNullOrBlank()) {
            put("booking_url", bookingUrl)
            put(ATTR_BOOKING_URL, bookingUrl)
        }
        if (fallbackImageUrl.isNotBlank()) {
            put("image_url", fallbackImageUrl)
            put(ATTR_PROFILE_PHOTO_URL, fallbackImageUrl)
        }
        if (fallbackPhotoUrls.isNotEmpty()) {
            put("photo_urls", fallbackPhotoUrls.joinToString(","))
        }
        if (!activityId.isNullOrBlank()) put("viator_activity_id", activityId)
        score?.let { put("pinecone_score", it.toString()) }
        put("isNativeBookable", isNativeBookable.toString())
        put("source", "pinecone")
    }

    private fun EmulatorActivityOption.resolvedOptionId(): String? {
        return option_id
            ?.takeIf { it.isNotBlank() }
            ?: activity_id
                ?.takeIf { it.isNotBlank() }
                ?.let { "viator::$it" }
    }

    private fun EmulatorActivity.resolvedImageUrl(photoUrls: List<String> = resolvedPhotoUrls()): String {
        return image_url
            ?.takeIf { it.isNotBlank() }
            ?: photoUrls.firstOrNull().orEmpty()
    }

    private fun EmulatorActivity.resolvedPhotoUrls(): List<String> {
        return normalizedPhotoUrls(image_url, photo_urls)
    }

    private fun EmulatorActivityOption.resolvedImageUrl(photoUrls: List<String> = resolvedPhotoUrls()): String {
        return image_url
            ?.takeIf { it.isNotBlank() }
            ?: photoUrls.firstOrNull().orEmpty()
    }

    private fun EmulatorActivityOption.resolvedPhotoUrls(): List<String> {
        return normalizedPhotoUrls(image_url, photo_urls)
    }

    private fun normalizedPhotoUrls(preferredImageUrl: String?, photoUrls: List<String>?): List<String> {
        return buildList {
            preferredImageUrl
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)
            photoUrls.orEmpty()
                .filter { it.isNotBlank() }
                .forEach(::add)
        }.distinct()
    }

    private fun String?.isTruthy(): Boolean {
        return equals("true", ignoreCase = true)
    }

    private fun EmulatorActivity.normalizedActivityDetails(
        title: String,
        description: String,
        bookingUrl: String?,
        fallbackImageUrl: String,
        fallbackPhotoUrls: List<String>,
        isNativeBookable: Boolean,
        activityId: String?,
        selectedOptionId: String?,
        source: String
    ): Map<String, String> {
        return normalizeActivityDetails(
            title = title,
            description = description,
            bookingUrl = bookingUrl,
            directUrl = url,
            websiteUrl = website,
            yelpUrl = yelp_url,
            fallbackImageUrl = fallbackImageUrl,
            fallbackPhotoUrls = fallbackPhotoUrls,
            isNativeBookable = isNativeBookable,
            activityId = activityId,
            selectedOptionId = selectedOptionId,
            venueName = venue_name,
            location = location,
            address = address,
            latitude = latitude,
            longitude = longitude,
            rating = rating,
            reviewCount = review_count,
            priceTier = price_tier,
            categories = categories,
            hoursSummary = hours_summary,
            profilePhotoUrl = profile_photo_url,
            details = details,
            metadata = metadata,
            source = source
        )
    }

    private fun EmulatorActivityOption.normalizedActivityDetails(
        title: String,
        description: String,
        bookingUrl: String?,
        fallbackImageUrl: String,
        fallbackPhotoUrls: List<String>,
        isNativeBookable: Boolean,
        activityId: String?,
        selectedOptionId: String?,
        source: String,
        fallbackDetails: Map<String, String>
    ): Map<String, String> {
        return fallbackDetails + normalizeActivityDetails(
            title = title,
            description = description,
            bookingUrl = bookingUrl,
            directUrl = url,
            websiteUrl = website,
            yelpUrl = yelp_url,
            fallbackImageUrl = fallbackImageUrl,
            fallbackPhotoUrls = fallbackPhotoUrls,
            isNativeBookable = isNativeBookable,
            activityId = activityId,
            selectedOptionId = selectedOptionId,
            venueName = venue_name,
            location = location,
            address = address,
            latitude = latitude,
            longitude = longitude,
            rating = rating,
            reviewCount = review_count,
            priceTier = price_tier,
            categories = categories,
            hoursSummary = hours_summary,
            profilePhotoUrl = profile_photo_url,
            details = details,
            metadata = metadata,
            source = source
        )
    }

    private fun normalizeActivityDetails(
        title: String,
        description: String,
        bookingUrl: String?,
        directUrl: String?,
        websiteUrl: String?,
        yelpUrl: String?,
        fallbackImageUrl: String,
        fallbackPhotoUrls: List<String>,
        isNativeBookable: Boolean,
        activityId: String?,
        selectedOptionId: String?,
        venueName: String?,
        location: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        rating: Double?,
        reviewCount: Int?,
        priceTier: String?,
        categories: List<String>?,
        hoursSummary: String?,
        profilePhotoUrl: String?,
        details: Map<String, Any?>?,
        metadata: Map<String, Any?>?,
        source: String
    ): Map<String, String> = buildMap {
        put("activity_name", title)
        put("title", title)
        put(ATTR_BUSINESS_NAME, title)
        if (description.isNotBlank()) put("description", description)

        val detailMaps = listOfNotNull(details, metadata)
        val resolvedVenueName = firstNonBlank(venueName, detailMaps, ATTR_VENUE_NAME, "venue_name", "venue", "location_name")
            ?: firstNonBlank(location, detailMaps, "location", "location_label")
        val resolvedAddress = firstNonBlank(address, detailMaps, ATTR_BUSINESS_ADDRESS, "address", "business_address", "display_address")
        val resolvedLocation = firstNonBlank(location, detailMaps, "location", "location_label", ATTR_VENUE_NAME, "venue_name", "venue")
        val resolvedBookingUrl = firstNonBlank(bookingUrl, detailMaps, ATTR_BOOKING_URL, "booking_url")
        val resolvedYelpUrl = firstNonBlank(yelpUrl, detailMaps, ATTR_YELP_URL, "yelp_url")
        val resolvedWebsiteUrl = firstNonBlank(websiteUrl, detailMaps, ATTR_WEBSITE_URL, "website", "website_url")
        val resolvedExternalUrl = firstNonBlank(directUrl, detailMaps, "url")
        val resolvedImageUrl = firstNonBlank(
            profilePhotoUrl,
            detailMaps,
            ATTR_PROFILE_PHOTO_URL,
            "profile_photo_url",
            "image_url"
        )?.ifBlank { null } ?: fallbackImageUrl.takeIf { it.isNotBlank() }
        val resolvedCategories = firstNonBlank(
            categories?.filter { it.isNotBlank() }?.joinToString(", "),
            detailMaps,
            ATTR_CATEGORIES,
            "categories"
        )
        val resolvedPriceTier = firstNonBlank(priceTier, detailMaps, ATTR_PRICE_TIER, "price_tier")
        val resolvedHoursSummary = firstNonBlank(hoursSummary, detailMaps, ATTR_HOURS_SUMMARY, "hours_summary", "hours")
        val resolvedRating = firstDouble(rating, detailMaps, ATTR_AVERAGE_RATING, "rating")
        val resolvedReviewCount = firstInt(reviewCount, detailMaps, ATTR_REVIEW_COUNT, "review_count")
        val resolvedLatitude = firstDouble(latitude, detailMaps, ATTR_LATITUDE, "latitude", "lat")
        val resolvedLongitude = firstDouble(longitude, detailMaps, ATTR_LONGITUDE, "longitude", "lng", "lon")

        if (!resolvedVenueName.isNullOrBlank()) {
            put(ATTR_VENUE_NAME, resolvedVenueName)
            put("location", resolvedVenueName)
        } else if (!resolvedLocation.isNullOrBlank()) {
            put("location", resolvedLocation)
        }
        if (!resolvedAddress.isNullOrBlank()) {
            put(ATTR_BUSINESS_ADDRESS, resolvedAddress)
            put("address", resolvedAddress)
        }
        if (!resolvedBookingUrl.isNullOrBlank()) {
            put("booking_url", resolvedBookingUrl)
            put(ATTR_BOOKING_URL, resolvedBookingUrl)
        }
        if (!resolvedYelpUrl.isNullOrBlank()) {
            put(ATTR_YELP_URL, resolvedYelpUrl)
        }
        if (!resolvedWebsiteUrl.isNullOrBlank()) {
            put(ATTR_WEBSITE_URL, resolvedWebsiteUrl)
        }
        if (!resolvedExternalUrl.isNullOrBlank()) {
            put("url", resolvedExternalUrl)
        }
        if (!resolvedImageUrl.isNullOrBlank()) {
            put("image_url", resolvedImageUrl)
            put(ATTR_PROFILE_PHOTO_URL, resolvedImageUrl)
        }
        if (fallbackPhotoUrls.isNotEmpty()) {
            put("photo_urls", fallbackPhotoUrls.joinToString(","))
        }
        if (!resolvedCategories.isNullOrBlank()) {
            put(ATTR_CATEGORIES, resolvedCategories)
        }
        if (!resolvedPriceTier.isNullOrBlank()) {
            put(ATTR_PRICE_TIER, resolvedPriceTier)
        }
        if (!resolvedHoursSummary.isNullOrBlank()) {
            put(ATTR_HOURS_SUMMARY, resolvedHoursSummary)
        }
        resolvedRating?.let { put(ATTR_AVERAGE_RATING, it.trimTrailingZeros()) }
        resolvedReviewCount?.let { put(ATTR_REVIEW_COUNT, it.toString()) }
        if (resolvedLatitude != null && resolvedLongitude != null) {
            put(ATTR_LATITUDE, resolvedLatitude.trimTrailingZeros())
            put(ATTR_LONGITUDE, resolvedLongitude.trimTrailingZeros())
            put(ATTR_STATIC_MAP_URL, StaticMapUrlFactory.buildUrl(resolvedLatitude, resolvedLongitude))
            put(ATTR_STATIC_MAP_PROVIDER, StaticMapUrlFactory.PROVIDER)
        }
        if (!activityId.isNullOrBlank()) put("viator_activity_id", activityId)
        if (!selectedOptionId.isNullOrBlank()) put("selected_inventory_option_id", selectedOptionId)
        put("isNativeBookable", isNativeBookable.toString())
        put("source", source)
    }

    private fun firstNonBlank(
        preferred: String?,
        detailMaps: List<Map<String, Any?>>,
        vararg keys: String
    ): String? {
        preferred?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return detailMaps.asSequence()
            .mapNotNull { map -> map.firstStringValue(keys.asList()) }
            .firstOrNull()
    }

    private fun firstDouble(
        preferred: Double?,
        detailMaps: List<Map<String, Any?>>,
        vararg keys: String
    ): Double? {
        preferred?.let { return it }
        return detailMaps.asSequence()
            .mapNotNull { map -> map.firstNumberValue(keys.asList())?.toDouble() }
            .firstOrNull()
    }

    private fun firstInt(
        preferred: Int?,
        detailMaps: List<Map<String, Any?>>,
        vararg keys: String
    ): Int? {
        preferred?.let { return it }
        return detailMaps.asSequence()
            .mapNotNull { map -> map.firstNumberValue(keys.asList())?.toInt() }
            .firstOrNull()
    }

    private fun Map<String, Any?>.firstStringValue(keys: List<String>): String? {
        return keys.asSequence()
            .mapNotNull { key -> this[key].stringValue() }
            .firstOrNull()
    }

    private fun Map<String, Any?>.firstNumberValue(keys: List<String>): Number? {
        return keys.asSequence()
            .mapNotNull { key -> this[key].numberValue() }
            .firstOrNull()
    }

    private fun Any?.stringValue(): String? {
        return when (this) {
            null -> null
            is String -> trim().takeIf { it.isNotBlank() }
            is Number, is Boolean -> toString()
            is List<*> -> mapNotNull { it.stringValue() }.firstOrNull()
            else -> null
        }
    }

    private fun Any?.numberValue(): Number? {
        return when (this) {
            is Number -> this
            is String -> trim().toDoubleOrNull()
            else -> null
        }
    }

    private fun Double.trimTrailingZeros(): String {
        return if (this % 1.0 == 0.0) {
            toLong().toString()
        } else {
            toString()
        }
    }

    private fun buildItineraryPrompt(request: TravelRequest): String {
        val requestJson = gson.toJson(request)
        return """
A user submitted this travel request:
$requestJson

Return a single JSON object with these fields only:
{
  "itinerary_id": "<uuid>",
  "user_id": "${request.userId}",
  "trip_name": "<short title>",
  "destination": "<city, country>",
  "origin": "<city, country>",
  "origin_iata": "<IATA airport code for origin city, e.g. LAX>",
  "destination_iata": "<IATA airport code for destination city, e.g. CDG>",
  "date_from": "<YYYY-MM-DD>",
  "date_to": "<YYYY-MM-DD>",
  "duration_days": <int>,
  "currency": "<ISO 4217>",
  "travel_style": "<budget|comfort|luxury>",
  "travelers": {"adults": <int>, "children": <int>},
  "created_at": "<ISO 8601 timestamp>",
  "status": "draft"
}
        """.trimIndent()
    }

    private fun parseItinerary(raw: String, userId: String): Itinerary {
        val json = gson.fromJson(raw, JsonObject::class.java)

        val travelers = json.getAsJsonObject("travelers")
        val itineraryId = json.get("itinerary_id")?.asString
            ?.takeIf { !it.contains("uuid", ignoreCase = true) }
            ?: UUID.randomUUID().toString()
        val destination = json.get("destination")?.asString ?: ""
        val destinationIata = json.get("destination_iata")?.asString?.uppercase() ?: ""

        return Itinerary(
            itineraryId = itineraryId,
            userId = userId,
            tripName = defaultTripNameForDestination(destination),
            destination = destination,
            origin = json.get("origin")?.asString ?: "",
            originIata = json.get("origin_iata")?.asString?.uppercase() ?: "",
            destinationIata = destinationIata,
            timeZoneId = DestinationTimeZones.resolveTimeZoneId(
                destination = destination,
                destinationIata = destinationIata
            ).orEmpty(),
            dateFrom = json.get("date_from")?.asString ?: "",
            dateTo = json.get("date_to")?.asString ?: "",
            durationDays = json.get("duration_days")?.asInt ?: 0,
            currency = json.get("currency")?.asString ?: "USD",
            travelStyle = json.get("travel_style")?.asString ?: "comfort",
            adults = travelers?.get("adults")?.asInt ?: 1,
            children = travelers?.get("children")?.asInt ?: 0,
            createdAt = json.get("created_at")?.asString ?: java.time.Instant.now().toString(),
            status = json.get("status")?.asString ?: "draft",
            eventIds = emptyList()
        )
    }
}
