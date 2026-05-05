package com.example.travelcents.data.media

import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApiService {
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("orientation") orientation: String? = null,
        @Query("color") color: String? = null,
        @Query("order_by") orderBy: String = "relevant",
        @Query("content_filter") contentFilter: String = "high",
        @Query("per_page") perPage: Int = 5,
        @Query("page") pageIndex: Int = 1
    ): UnsplashSearchResponse
}
