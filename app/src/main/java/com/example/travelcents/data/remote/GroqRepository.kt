package com.example.travelcents.data.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.model.GroqMessage
import com.example.travelcents.data.model.GroqRequest
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.TravelRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object GroqRepository {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GroqApiService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GroqApiService::class.java)

    private const val SYSTEM_PROMPT =
        "You are a travel planner. Always respond with valid JSON only. No markdown, no extra text."

    // Call 1: Generate itinerary metadata from a travel request
    suspend fun generateItinerary(request: TravelRequest): Itinerary {
        val prompt = buildItineraryPrompt(request)
        val raw = callGroq(prompt)
        return parseItinerary(raw, request.userId)
    }

    // Call 2: Generate events linked to an existing itinerary
    suspend fun generateEvents(itinerary: Itinerary, request: TravelRequest): List<TravelEvent> {
        val prompt = buildEventsPrompt(itinerary, request)
        val raw = callGroq(prompt)
        return parseEvents(raw, itinerary.itineraryId)
    }

    // Full pipeline: itinerary + events
    suspend fun planTrip(request: TravelRequest): Pair<Itinerary, List<TravelEvent>> {
        val itinerary = generateItinerary(request)
        val events = generateEvents(itinerary, request)
        val linkedItinerary = itinerary.copy(
            eventIds = events.map { it.eventId }
        )
        return linkedItinerary to events
    }

    private suspend fun callGroq(userPrompt: String): String {
        val groqRequest = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = SYSTEM_PROMPT),
                GroqMessage(role = "user", content = userPrompt)
            )
        )
        val response = api.complete(groqRequest)
        return response.choices.first().message.content.trim()
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

    private fun buildEventsPrompt(itinerary: Itinerary, request: TravelRequest): String {
        val travelersJson = gson.toJson(mapOf("adults" to request.adults, "children" to request.children))
        return """
Generate travel events for this itinerary (id: ${itinerary.itineraryId}).
Trip: ${itinerary.origin} to ${itinerary.destination}, ${itinerary.dateFrom} to ${itinerary.dateTo}.
Travelers: $travelersJson. Style: ${itinerary.travelStyle}.

Return a JSON object: {"events": [...]} where each event uses ONLY the fields listed below.
Include: outbound flight, return flight, hotel stay, 2 restaurants, 2 activities.

FLIGHT:
{
  "event_id": "<uuid>", "type": "flight", "itinerary_id": "${itinerary.itineraryId}",
  "tz": "<IANA timezone>",
  "date": "<YYYY-MM-DD>",
  "start_time": "<HH:MM>", "end_time": "<HH:MM>",
  "airline": "<name>", "flight_number": "<code>",
  "origin_airport": "<IATA>", "destination_airport": "<IATA>"
}

HOTEL:
{
  "event_id": "<uuid>", "type": "hotel", "itinerary_id": "${itinerary.itineraryId}",
  "tz": "<IANA timezone>",
  "start_time": "<check-in HH:MM>", "end_time": "<check-out HH:MM>",
  "check_in_date": "<YYYY-MM-DD>", "check_out_date": "<YYYY-MM-DD>",
  "hotel_name": "<name>", "room_type": "<type>"
}

RESTAURANT:
{
  "event_id": "<uuid>", "type": "restaurant", "itinerary_id": "${itinerary.itineraryId}",
  "tz": "<IANA timezone>",
  "date": "<YYYY-MM-DD>",
  "start_time": "<HH:MM>", "end_time": "<HH:MM>",
  "restaurant_name": "<name>"
}

ACTIVITY:
{
  "event_id": "<uuid>", "type": "activity", "itinerary_id": "${itinerary.itineraryId}",
  "tz": "<IANA timezone>",
  "date": "<YYYY-MM-DD>",
  "start_time": "<HH:MM>", "end_time": "<HH:MM>",
  "activity_name": "<name>"
}
        """.trimIndent()
    }

    private fun parseItinerary(raw: String, userId: String): Itinerary {
        val json = gson.fromJson(raw, JsonObject::class.java)

        val travelers = json.getAsJsonObject("travelers")
        val itineraryId = json.get("itinerary_id")?.asString
            ?.takeIf { !it.contains("uuid", ignoreCase = true) }
            ?: UUID.randomUUID().toString()

        return Itinerary(
            itineraryId = itineraryId,
            userId = userId,
            tripName = json.get("trip_name")?.asString ?: "My Trip",
            destination = json.get("destination")?.asString ?: "",
            origin = json.get("origin")?.asString ?: "",
            dateFrom = json.get("date_from")?.asString ?: "",
            dateTo = json.get("date_to")?.asString ?: "",
            durationDays = json.get("duration_days")?.asInt ?: 0,
            currency = json.get("currency")?.asString ?: "USD",
            travelStyle = json.get("travel_style")?.asString ?: "comfort",
            adults = travelers?.get("adults")?.asInt ?: 1,
            children = travelers?.get("children")?.asInt ?: 0,
            createdAt = json.get("created_at")?.asString
                ?: java.time.Instant.now().toString(),
            status = json.get("status")?.asString ?: "draft",
            eventIds = emptyList()
        )
    }

    private fun parseEvents(raw: String, itineraryId: String): List<TravelEvent> {
        val json = gson.fromJson(raw, JsonObject::class.java)
        val eventsArray = json.getAsJsonArray("events")
            ?: return emptyList()

        val seenIds = mutableSetOf<String>()
        val type = object : TypeToken<Map<String, Any>>() {}.type

        return eventsArray.map { element ->
            val obj = element.asJsonObject
            val allFields: Map<String, Any> = gson.fromJson(element, type)

            var eventId = obj.get("event_id")?.asString ?: ""
            if (eventId.isBlank() || eventId in seenIds || eventId.contains("uuid", ignoreCase = true)) {
                eventId = UUID.randomUUID().toString()
            }
            seenIds.add(eventId)

            val eventType = obj.get("type")?.asString ?: "event"
            val tz = obj.get("tz")?.asString ?: ""
            val date = obj.get("date")?.asString
                ?: obj.get("check_in_date")?.asString ?: ""
            val startTime = obj.get("start_time")?.asString ?: ""
            val endTime = obj.get("end_time")?.asString ?: ""

            // Collect type-specific fields into details map
            val reservedKeys = setOf(
                "event_id", "type", "itinerary_id", "tz",
                "date", "start_time", "end_time"
            )
            val details = allFields
                .filterKeys { it !in reservedKeys }
                .mapValues { it.value.toString() }

            TravelEvent(
                eventId = eventId,
                type = eventType,
                itineraryId = itineraryId,
                tz = tz,
                date = date,
                startTime = startTime,
                endTime = endTime,
                details = details
            )
        }
    }
}