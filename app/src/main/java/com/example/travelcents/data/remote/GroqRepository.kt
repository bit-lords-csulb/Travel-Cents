package com.example.travelcents.data.remote

import com.example.travelcents.data.model.GroqMessage
import com.example.travelcents.data.model.GroqRequest
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.data.model.TravelRequest
import com.example.travelcents.BuildConfig
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
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
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

    // Call 2: Generate restaurants + activities only (flights/hotels come from SerpAPI)
    suspend fun generateEvents(
        itinerary: Itinerary,
        request: TravelRequest,
        realFlights: List<TravelEvent> = emptyList(),
        realHotels: List<TravelEvent> = emptyList(),
        remainingBudget: Double = 0.0
    ): List<TravelEvent> {
        val prompt = buildEventsPrompt(itinerary, request, realFlights, realHotels, remainingBudget)
        val raw = callGroq(prompt)
        return parseEvents(raw, itinerary.itineraryId)
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

    private fun buildEventsPrompt(
        itinerary: Itinerary,
        request: TravelRequest,
        realFlights: List<TravelEvent>,
        realHotels: List<TravelEvent>,
        remainingBudget: Double = 0.0
    ): String {
        val travelersJson = gson.toJson(mapOf("adults" to request.adults, "children" to request.children))

        val flightContext = if (realFlights.isNotEmpty()) {
            val firstFlight = realFlights.first()
            "Arriving via ${firstFlight.details["airline"] ?: "flight"} at ${firstFlight.details["destination_airport"] ?: itinerary.destinationIata} around ${firstFlight.endTime}."
        } else ""

        val hotelName = realHotels.firstOrNull()?.details?.get("hotel_name") ?: "the hotel"
        val hotelContext = "Staying at $hotelName in ${itinerary.destination}. Plan activities near or around this hotel."

        val budgetContext = if (remainingBudget > 0)
            "Remaining budget after flights and hotel: \$${remainingBudget.toInt()}. Choose activities and restaurants accordingly — avoid expensive options if budget is tight."
        else
            "No specific budget constraint. Suggest a balanced mix of experiences."

        return """
Generate a full daily schedule of local events for this itinerary (id: ${itinerary.itineraryId}).
Trip: ${itinerary.origin} to ${itinerary.destination}, ${itinerary.dateFrom} to ${itinerary.dateTo} (${itinerary.durationDays} days).
Travelers: $travelersJson. Style: ${itinerary.travelStyle}.
Interests: ${request.interests.joinToString(", ").ifBlank { "general" }}.
$flightContext
$hotelContext
$budgetContext

Flights and hotel are already booked. Generate ONLY restaurants and activities — one set per day from ${itinerary.dateFrom} to ${itinerary.dateTo}.

Rules:
- Include EVERY day between dateFrom and dateTo (inclusive).
- At least 1 restaurant per day.
- Multiple activities per day UNLESS it is an all-day activity (theme park, full-day tour, cruise) — in that case, just that 1 activity + 1 restaurant/dinner for the day.
- For regular days: aim for 2–3 activities spread through the day.
- Set is_all_day to true only for genuinely all-day activities.
- Use realistic local start/end times.

Return a JSON object: {"events": [...]} where each event uses ONLY the fields listed below.

RESTAURANT:
{
  "event_id": "<uuid>", "type": "restaurant", "itinerary_id": "${itinerary.itineraryId}",
  "tz": "<IANA timezone>", "date": "<YYYY-MM-DD>",
  "start_time": "<HH:MM>", "end_time": "<HH:MM>",
  "restaurant_name": "<name>", "cuisine": "<cuisine type>",
  "location": "<address or neighborhood>",
  "estimated_cost_per_person": "<number>"
}

ACTIVITY:
{
  "event_id": "<uuid>", "type": "activity", "itinerary_id": "${itinerary.itineraryId}",
  "tz": "<IANA timezone>", "date": "<YYYY-MM-DD>",
  "start_time": "<HH:MM>", "end_time": "<HH:MM>",
  "activity_name": "<name>", "location": "<address or neighborhood>",
  "description": "<1-2 sentence description>",
  "is_all_day": <true|false>,
  "estimated_cost_per_person": "<number>"
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
            originIata = json.get("origin_iata")?.asString?.uppercase() ?: "",
            destinationIata = json.get("destination_iata")?.asString?.uppercase() ?: "",
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