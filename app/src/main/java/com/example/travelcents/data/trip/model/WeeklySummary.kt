package com.example.travelcents.data.trip.model

data class WeeklySummary(
    val tripName: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val events: List<EventSummary>,
    val totalCost: Double,
    val budget: Double,
    val currency: String,
    val interestStats: Map<String, Int>, // Interest -> Count of events matching it
    val budgetStatus: BudgetStatus
)

data class EventSummary(
    val eventTitle: String,
    val type: String,
    val cost: Double,
    val date: String
)

enum class BudgetStatus {
    UNDER_BUDGET,
    ON_TRACK,
    OVER_BUDGET
}
