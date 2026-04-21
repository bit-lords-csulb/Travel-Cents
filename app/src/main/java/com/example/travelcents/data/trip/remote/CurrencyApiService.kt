package com.example.travelcents.data.trip.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApiService {
    // frankfurter.app: free, no API key, ~33 currencies supported
    // GET /latest?amount=1&from=USD&to=EUR
    // Response: { "amount": 1.0, "base": "USD", "date": "...", "rates": { "EUR": 0.91 } }
    @GET("latest")
    suspend fun convert(
        @Query("amount") amount: Double,
        @Query("from") from: String,
        @Query("to") to: String
    ): CurrencyResponse
}

data class CurrencyResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)


