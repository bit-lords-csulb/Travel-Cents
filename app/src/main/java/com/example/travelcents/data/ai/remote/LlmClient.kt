package com.example.travelcents.data.ai.remote

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.model.LlmMessage
import com.example.travelcents.data.ai.model.LlmRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LlmClient {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (LlmConfig.apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${LlmConfig.apiKey}")
            }
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: LlmApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LlmConfig.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LlmApiService::class.java)
    }

    suspend fun complete(
        messages: List<LlmMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 1024,
        responseFormat: Map<String, String>? = null
    ): String {
        check(LlmConfig.apiKey.isNotBlank()) {
            "LLM_API_KEY is missing. Add it to local.properties before using AI features."
        }

        val request = LlmRequest(
            model = LlmConfig.model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            responseFormat = responseFormat
        )
        return api.complete(request).choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }
}


