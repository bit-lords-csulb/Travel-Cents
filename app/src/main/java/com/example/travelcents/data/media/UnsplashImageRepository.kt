package com.example.travelcents.data.media

import android.util.Log
import com.example.travelcents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class UnsplashImageRepository(
    private val api: UnsplashApiService = buildApi(BuildConfig.UNSPLASH_ACCESS_KEY),
    private val accessKey: String = BuildConfig.UNSPLASH_ACCESS_KEY
) {

    data class ResolvedImage(
        val imageUrl: String,
        val attribution: String? = null,
        val sourcePageUrl: String? = null
    )

    suspend fun resolve(
        query: String,
        params: UnsplashSearchParams = UnsplashSearchParams()
    ): String? = resolveDetailed(query, params)?.imageUrl

    suspend fun resolveDetailed(
        query: String,
        params: UnsplashSearchParams = UnsplashSearchParams()
    ): ResolvedImage? = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || accessKey.isBlank()) return@withContext null

        val sanitizedParams = params.sanitized()
        runCatching {
            api.searchPhotos(
                query = normalizedQuery,
                orientation = sanitizedParams.orientation,
                color = sanitizedParams.color,
                orderBy = sanitizedParams.orderBy,
                contentFilter = sanitizedParams.contentFilter,
                perPage = sanitizedParams.perPage,
                pageIndex = sanitizedParams.pageIndex
            ).results.firstOrNull()?.let { photo ->
                val urls = photo.urls ?: return@let null
                val imageUrl = urls.regular ?: urls.full ?: urls.small ?: urls.raw ?: urls.thumb
                    ?: return@let null
                ResolvedImage(
                    imageUrl = imageUrl,
                    attribution = photo.user?.name
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "Photo by $it on Unsplash" },
                    sourcePageUrl = photo.links?.html?.takeIf { it.isNotBlank() }
                )
            }
        }.onFailure { error ->
            Log.w(TAG, "Unsplash lookup failed for '$normalizedQuery': ${error.message}")
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "UnsplashImageRepo"

        fun buildApi(accessKey: String): UnsplashApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                        .header("Accept-Version", "v1")
                    if (accessKey.isNotBlank()) {
                        requestBuilder.header("Authorization", "Client-ID $accessKey")
                    }
                    chain.proceed(requestBuilder.build())
                })
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.unsplash.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UnsplashApiService::class.java)
        }
    }
}
