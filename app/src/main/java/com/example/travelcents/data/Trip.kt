package com.example.travelcents.data


data class Travelers(
    val adults: Int = 0,
    val children: Int = 0
)


data class Trip(
    val itinerary_id: String = "",
    val user_id: String = "",
    val trip_name: String = "",
    val destination: String = "",
    val origin: String = "",
    val date_from: String = "",
    val date_to: String = "",
    val duration_days: Int = 0,
    val currency: String = "USD",
    val estimated_total_budget: Double = 0.0,
    val travel_style: String = "",
    val travelers: Travelers = Travelers(),
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    val event_ids: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val created_at: String = "",
    val status: String = "draft"
)