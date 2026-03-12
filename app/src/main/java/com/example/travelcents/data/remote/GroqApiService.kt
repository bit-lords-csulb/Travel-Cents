package com.example.travelcents.data.remote

import com.example.travelcents.data.model.GroqRequest
import com.example.travelcents.data.model.GroqResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApiService {
    @POST("chat/completions")
    suspend fun complete(@Body request: GroqRequest): GroqResponse
}