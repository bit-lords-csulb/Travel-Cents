package com.example.travelcents.data.ai.repository

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.remote.LlmClient
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
    val real_title: String? = null,
    val isNativeBookable: Boolean = false,
    val start_time: String? = null,
    val end_time: String? = null
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
        flightArrival: String
    ): List<TravelEvent> {
        if (request.destination.isBlank() || itineraryId.isBlank() || dates.isEmpty()) return emptyList()

        val payload: Map<String, Any> = mapOf(
            "destination" to request.destination,
            "dateFrom" to request.dateFrom,
            "dateTo" to request.dateTo,
            "adults" to request.adults,
            "children" to request.children,
            "travelStyle" to request.travelStyle,
            "budgetTotal" to request.budgetTotal.toString(),
            "interests" to request.interests,
            "specialRequests" to request.specialRequests,
            "flightArrival" to flightArrival
        )

        val response = emulatorApi.getLocalItinerary(
            EmulatorRequest(data = payload)
        )

        return response.result.itinerary.mapIndexed { index, activity ->
            val activityTitle = activity.real_title
                ?.takeIf { it.isNotBlank() }
                ?: activity.title.ifBlank { "Recommended activity" }

            TravelEvent(
                eventId = UUID.randomUUID().toString(),
                type = "activity",
                itineraryId = itineraryId,
                date = dates[index % dates.size],
                startTime = activity.start_time ?: "10:00",
                endTime = activity.end_time ?: "13:00",
                isNativeBookable = activity.isNativeBookable.toString(),
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
                    put("isNativeBookable", activity.isNativeBookable.toString())
                    put("source", "emulator")
                }
            )
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

        return Itinerary(
            itineraryId = itineraryId,
            userId = userId,
            tripName = defaultTripNameForDestination(destination),
            destination = destination,
            origin = json.get("origin")?.asString ?: "",
            originIata = json.get("origin_iata")?.asString?.uppercase() ?: "",
            destinationIata = json.get("destination_iata")?.asString?.uppercase() ?: "",
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
