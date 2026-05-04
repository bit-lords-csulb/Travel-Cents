package com.example.travelcents.data.ai.repository

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
import com.example.travelcents.data.trip.local.DestinationTimeZones
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.defaultTripNameForDestination
import com.google.gson.Gson
import com.google.gson.JsonObject
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
    val image_url: String? = null,
    val photo_urls: List<String>? = null,
    val real_title: String? = null,
    val isNativeBookable: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val option_id: String? = null,
    val activity_id: String? = null,
    val options: List<EmulatorActivityOption>? = null
)

data class EmulatorActivityOption(
    val option_id: String? = null,
    val activity_id: String? = null,
    val title: String = "",
    val real_title: String? = null,
    val booking_url: String? = null,
    val image_url: String? = null,
    val photo_urls: List<String>? = null,
    val isNativeBookable: String? = null,
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
            val eventOptions = buildActivityOptions(
                eventId = eventId,
                selectedOptionId = selectedOptionId,
                fallbackTitle = activityTitle,
                fallbackDescription = activity.description,
                fallbackBookingUrl = activity.booking_url,
                fallbackImageUrl = activityImageUrl,
                fallbackPhotoUrls = activityPhotoUrls,
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
                details = buildMap {
                    put("activity_name", activityTitle)
                    put("title", activityTitle)
                    if (activity.description.isNotBlank()) {
                        put("description", activity.description)
                    }
                    activity.booking_url
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("booking_url", it) }
                    activityImageUrl
                        .takeIf { it.isNotBlank() }
                        ?.let { put("image_url", it) }
                    activity.activity_id
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("viator_activity_id", it) }
                    selectedOptionId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("selected_inventory_option_id", it) }
                    put("isNativeBookable", isNativeBookable.toString())
                    put("source", "emulator")
                },
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
                    details = buildActivityOptionDetails(
                        title = fallbackTitle,
                        description = fallbackDescription,
                        bookingUrl = fallbackBookingUrl,
                        isNativeBookable = !fallbackBookingUrl.isNullOrBlank(),
                        activityId = activity.activity_id,
                        score = null
                    )
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
        selected: Boolean
    ): EventOption {
        val optionPhotoUrls = resolvedPhotoUrls()
        val optionImageUrl = resolvedImageUrl(optionPhotoUrls)
        return EventOption(
            optionId = optionId,
            eventId = eventId,
            source = "pinecone",
            selected = selected,
            imageUrl = optionImageUrl,
            photoUrls = optionPhotoUrls,
            details = buildActivityOptionDetails(
                title = optionTitle,
                description = fallbackDescription,
                bookingUrl = booking_url,
                isNativeBookable = isNativeBookable.isTruthy() || !booking_url.isNullOrBlank(),
                activityId = activity_id,
                score = score
            )
        )
    }

    private fun buildActivityOptionDetails(
        title: String,
        description: String,
        bookingUrl: String?,
        isNativeBookable: Boolean,
        activityId: String?,
        score: Double?
    ): Map<String, String> = buildMap {
        put("activity_name", title)
        put("title", title)
        if (description.isNotBlank()) put("description", description)
        if (!bookingUrl.isNullOrBlank()) {
            put("booking_url", bookingUrl)
            put(ATTR_BOOKING_URL, bookingUrl)
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
