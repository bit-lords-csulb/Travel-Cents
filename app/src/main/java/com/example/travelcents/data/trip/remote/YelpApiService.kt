package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpEventsResponse
import com.example.travelcents.data.trip.model.YelpReviewsResponse
import com.example.travelcents.data.trip.model.YelpSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface YelpApiService {
    @GET("v3/businesses/search")
    suspend fun searchBusinesses(@QueryMap params: Map<String, String>): YelpSearchResponse

    @GET("v3/events")
    suspend fun searchEvents(@QueryMap params: Map<String, String>): YelpEventsResponse

    @GET("v3/businesses/{id}")
    suspend fun getBusinessDetail(@Path("id") id: String): YelpBusiness

    @GET("v3/businesses/{id}/reviews")
    suspend fun getBusinessReviews(
        @Path("id") id: String,
        @Query("limit") limit: Int = 3
    ): YelpReviewsResponse
}

