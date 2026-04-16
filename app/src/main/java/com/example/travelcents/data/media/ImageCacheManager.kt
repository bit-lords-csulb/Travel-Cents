package com.example.travelcents.data.media

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object ImageCacheManager {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Caches remote media under app internal storage at trip_images/{tripId}/ and
    // returns a map of remote url -> local file path for successfully cached assets.
    suspend fun cacheTripMedia(
        context: Context,
        tripId: String,
        urls: List<String>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "trip_images/$tripId").also { it.mkdirs() }
        val result = mutableMapOf<String, String>()

        urls.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { url ->
            try {
                val fileName = cacheFileNameForUrl(url)
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

    suspend fun downloadTripImages(
        context: Context,
        tripId: String,
        urls: List<String>
    ): Map<String, String> = cacheTripMedia(context, tripId, urls)

    // Deletes all cached images for a trip (called when the trip is deleted).
    fun deleteTripImages(context: Context, tripId: String) {
        File(context.filesDir, "trip_images/$tripId").deleteRecursively()
    }

    fun localPathForUrl(
        context: Context,
        tripId: String,
        url: String
    ): String? {
        if (tripId.isBlank() || url.isBlank()) return null
        val file = File(context.filesDir, "trip_images/$tripId/${cacheFileNameForUrl(url)}")
        return file.absolutePath.takeIf { file.exists() }
    }

    fun resolveCachedMediaUrl(
        context: Context,
        tripId: String,
        remoteUrl: String
    ): String? {
        if (tripId.isBlank() || remoteUrl.isBlank()) return remoteUrl.takeIf { it.isNotBlank() }
        return localPathForUrl(context, tripId, remoteUrl) ?: remoteUrl
    }

    internal fun cacheFileNameForUrl(url: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val sanitizedUrl = url.substringBefore('?').substringBefore('#')
        val lastPathSegment = sanitizedUrl.substringAfterLast('/')
        val extCandidate = lastPathSegment.substringAfterLast('.', "")
            .lowercase()
            .takeLast(4)
        val ext = extCandidate.takeIf { it.matches(Regex("[a-z0-9]{2,4}")) } ?: "img"
        return "${hash}.${ext}"
    }
}

