package com.example.travelcents.data.media

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object ImageCacheManager {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Downloads all URLs in the list to app internal storage under trip_images/{tripId}/.
    // Returns a map of url -> local file path for successfully downloaded images.
    suspend fun downloadTripImages(
        context: Context,
        tripId: String,
        urls: List<String>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "trip_images/$tripId").also { it.mkdirs() }
        val result = mutableMapOf<String, String>()

        urls.filter { it.isNotBlank() }.distinct().forEach { url ->
            try {
                val fileName = urlToFileName(url)
                val file = File(dir, fileName)
                if (!file.exists()) {
                    val req = Request.Builder().url(url).build()
                    http.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.byteStream()?.use { input ->
                                file.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
                }
                if (file.exists()) result[url] = file.absolutePath
            } catch (e: Exception) {
                Log.w("ImageCacheManager", "Failed to download $url: ${e.message}")
            }
        }

        result
    }

    // Deletes all cached images for a trip (called when the trip is deleted).
    fun deleteTripImages(context: Context, tripId: String) {
        File(context.filesDir, "trip_images/$tripId").deleteRecursively()
    }

    private fun urlToFileName(url: String): String {
        val hash = url.hashCode().toString().replace("-", "n")
        val ext = url.substringAfterLast(".", "jpg").take(4).lowercase()
        return "${hash}.${ext}"
    }
}

