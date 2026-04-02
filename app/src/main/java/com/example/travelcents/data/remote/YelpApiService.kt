package com.example.travelcents.data.remote

import com.example.travelcents.data.model.YelpEventsResponse
import com.example.travelcents.data.model.YelpSearchResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface YelpApiService {
    @GET("v3/businesses/search")
    suspend fun searchBusinesses(@QueryMap params: Map<String, String>): YelpSearchResponse

    @GET("v3/events")
    suspend fun searchEvents(@QueryMap params: Map<String, String>): YelpEventsResponse
}