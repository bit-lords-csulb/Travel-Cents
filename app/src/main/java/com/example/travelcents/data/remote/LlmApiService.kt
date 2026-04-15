package com.example.travelcents.data.remote

import com.example.travelcents.data.model.LlmRequest
import com.example.travelcents.data.model.LlmResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LlmApiService {
    @POST("chat/completions")
    suspend fun complete(@Body request: LlmRequest): LlmResponse
}
