package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.SerpFlightResponse
import com.example.travelcents.data.trip.model.SerpHotelResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface SerpApiService {
    @GET("search")
    suspend fun searchFlights(@QueryMap params: Map<String, String>): SerpFlightResponse

    @GET("search")
    suspend fun searchHotels(@QueryMap params: Map<String, String>): SerpHotelResponse
}

