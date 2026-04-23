package com.example.travelcents.data.remote

import com.example.travelcents.data.model.GroqRequest
import com.example.travelcents.data.model.GroqResponse
import retrofit2.http.Body
import retrofit2.http.POST

data class EmulatorRequest(val data: Map<String, String>)
data class EmulatorResponse(val result: EmulatorResultData)
data class EmulatorResultData(val itinerary: List<EmulatorActivity>)
data class EmulatorActivity(
    val title: String,
    val description: String,
    val booking_url: String?,
    val real_title: String?,
    val isNativeBookable: Boolean
)

interface GroqApiService {
    @POST("chat/completions")
    suspend fun complete(@Body request: GroqRequest): GroqResponse

    @POST("travel-cents-3e2d9/us-central1/generate_itinerary")
    suspend fun getLocalItinerary(@Body request: EmulatorRequest): EmulatorResponse
}