package com.example.travelcents.data.model

data class TravelRequest(
    val userId: String,
    val origin: String,
    val destination: String,
    val dateFrom: String,
    val dateTo: String,
    val adults: Int,
    val children: Int,
    val travelStyle: String,
    val currency: String,
    val budgetTotal: Double,
    val interests: List<String>,
    val specialRequests: String
)