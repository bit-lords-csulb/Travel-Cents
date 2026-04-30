package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.TmSearchResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface TicketmasterApiService {
    @GET("discovery/v2/events.json")
    suspend fun searchEvents(@QueryMap params: Map<String, String>): TmSearchResponse
}
