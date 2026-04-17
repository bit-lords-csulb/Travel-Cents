package com.example.travelcents.data.media

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class CachedMediaAsset(
    val remoteUrl: String,
    val localPath: String,
    val contentHash: String
)

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
    ): Map<String, String> {
        return cacheTripMediaAssets(context, tripId, urls)
            .associate { asset -> asset.remoteUrl to asset.localPath }
    }

    suspend fun cacheTripMediaAssets(
        context: Context,
        tripId: String,
        urls: List<String>
    ): List<CachedMediaAsset> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "trip_images/$tripId").also { it.mkdirs() }
        val result = mutableListOf<CachedMediaAsset>()

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
                if (file.exists()) {
                    result += CachedMediaAsset(
                        remoteUrl = url,
                        localPath = file.absolutePath,
                        contentHash = sha256(file.inputStream())
                    )
                }
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
        val hash = sha256(url.byteInputStream())
        val sanitizedUrl = url.substringBefore('?').substringBefore('#')
        val lastPathSegment = sanitizedUrl.substringAfterLast('/')
        val extCandidate = lastPathSegment.substringAfterLast('.', "")
            .lowercase()
            .takeLast(4)
        val ext = extCandidate.takeIf { it.matches(Regex("[a-z0-9]{2,4}")) } ?: "img"
        return "${hash}.${ext}"
    }

    private fun sha256(input: InputStream): String {
        return input.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

