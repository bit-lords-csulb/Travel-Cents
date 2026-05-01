package com.example.travelcents.data.trip.remote

import android.net.Uri
import com.example.travelcents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object TransportRepository {

    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json"

    data class TransportAnchor(
        val label: String,
        val latitude: Double,
        val longitude: Double
    )

    data class TransportDestination(
        val label: String,
        val latitude: Double?,
        val longitude: Double?,
        val address: String?
    )

    data class TransportSnapshot(
        val walkMin: Int?,
        val transitMin: Int?,
        val rideshareMin: Int?,
        val rideshareEstimateUsd: String?,
        val uberDeeplink: String?,
        val lyftDeeplink: String?,
        val transportAnchorLabel: String
    )

    private data class CacheEntry(
        val snapshot: TransportSnapshot,
        val expiresAtMs: Long
    )

    private data class RouteMetrics(
        val durationMin: Int?,
        val distanceMeters: Int?
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSnapshot(
        anchor: TransportAnchor,
        destination: TransportDestination
    ): TransportSnapshot? = withContext(Dispatchers.IO) {
        val destinationQuery = directionsQuery(destination) ?: return@withContext null
        val cacheKey = transportCacheKey(anchor, destination)
        val now = System.currentTimeMillis()

        cache[cacheKey]
            ?.takeIf { it.expiresAtMs > now }
            ?.let { return@withContext it.snapshot }

        val uberDeeplink = buildUberDeepLink(anchor, destination)
        val lyftDeeplink = buildLyftDeepLink(anchor, destination)
        val anchorLabel = "From: ${anchor.label}"
        val directionsKey = BuildConfig.GOOGLE_DIRECTIONS_KEY.takeIf { it.isNotBlank() }

        val snapshot = if (directionsKey == null) {
            if (uberDeeplink == null && lyftDeeplink == null) return@withContext null
            TransportSnapshot(
                walkMin = null,
                transitMin = null,
                rideshareMin = null,
                rideshareEstimateUsd = null,
                uberDeeplink = uberDeeplink,
                lyftDeeplink = lyftDeeplink,
                transportAnchorLabel = anchorLabel
            )
        } else {
            coroutineScope {
                val origin = formatCoordinates(anchor.latitude, anchor.longitude)
                val walkingDeferred = async {
                    fetchRouteMetrics(
                        origin = origin,
                        destination = destinationQuery,
                        mode = "walking",
                        departureTimeNow = false,
                        apiKey = directionsKey
                    )
                }
                val transitDeferred = async {
                    fetchRouteMetrics(
                        origin = origin,
                        destination = destinationQuery,
                        mode = "transit",
                        departureTimeNow = true,
                        apiKey = directionsKey
                    )
                }
                val drivingDeferred = async {
                    fetchRouteMetrics(
                        origin = origin,
                        destination = destinationQuery,
                        mode = "driving",
                        departureTimeNow = true,
                        apiKey = directionsKey
                    )
                }
                val results = awaitAll(
                    walkingDeferred,
                    transitDeferred,
                    drivingDeferred
                )
                val walking = results[0]
                val transit = results[1]
                val driving = results[2]

                if (
                    walking?.durationMin == null &&
                    transit?.durationMin == null &&
                    driving?.durationMin == null &&
                    uberDeeplink == null &&
                    lyftDeeplink == null
                ) {
                    return@coroutineScope null
                }

                TransportSnapshot(
                    walkMin = walking?.durationMin,
                    transitMin = transit?.durationMin,
                    rideshareMin = driving?.durationMin,
                    rideshareEstimateUsd = estimateRideShareUsd(driving?.distanceMeters),
                    uberDeeplink = uberDeeplink,
                    lyftDeeplink = lyftDeeplink,
                    transportAnchorLabel = anchorLabel
                )
            }
        } ?: return@withContext null

        cache[cacheKey] = CacheEntry(
            snapshot = snapshot,
            expiresAtMs = now + CACHE_TTL_MS
        )
        snapshot
    }

    private fun fetchRouteMetrics(
        origin: String,
        destination: String,
        mode: String,
        departureTimeNow: Boolean,
        apiKey: String
    ): RouteMetrics? {
        val url = DIRECTIONS_URL.toHttpUrl().newBuilder()
            .addQueryParameter("origin", origin)
            .addQueryParameter("destination", destination)
            .addQueryParameter("mode", mode)
            .apply {
                if (departureTimeNow) {
                    addQueryParameter("departure_time", "now")
                }
            }
            .addQueryParameter("key", apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val payload = response.body?.string()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::JSONObject)
                    ?: return null

                if (payload.optString("status") != "OK") return null

                val leg = payload.optJSONArray("routes")
                    ?.optJSONObject(0)
                    ?.optJSONArray("legs")
                    ?.optJSONObject(0)
                    ?: return null

                val durationSeconds = leg.optJSONObject("duration_in_traffic")
                    ?.optInt("value", -1)
                    ?.takeIf { it >= 0 }
                    ?: leg.optJSONObject("duration")
                        ?.optInt("value", -1)
                        ?.takeIf { it >= 0 }

                val distanceMeters = leg.optJSONObject("distance")
                    ?.optInt("value", -1)
                    ?.takeIf { it >= 0 }

                RouteMetrics(
                    durationMin = durationSeconds?.let { (it / 60.0).roundToInt().coerceAtLeast(1) },
                    distanceMeters = distanceMeters
                )
            }
        }.getOrNull()
    }

    private fun directionsQuery(destination: TransportDestination): String? {
        val latitude = destination.latitude
        val longitude = destination.longitude
        return if (latitude != null && longitude != null) {
            formatCoordinates(latitude, longitude)
        } else {
            destination.address?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private fun buildUberDeepLink(
        anchor: TransportAnchor,
        destination: TransportDestination
    ): String? {
        val destinationLat = destination.latitude ?: return null
        val destinationLng = destination.longitude ?: return null
        val destinationLabel = destination.label.ifBlank { "Destination" }
        val destinationAddress = destination.address?.ifBlank { null } ?: destinationLabel

        return buildString {
            append("uber://riderequest?")
            append("pickup[latitude]=").append(Uri.encode(decimal(anchor.latitude)))
            append("&pickup[longitude]=").append(Uri.encode(decimal(anchor.longitude)))
            append("&pickup[nickname]=").append(Uri.encode(anchor.label))
            append("&dropoff[latitude]=").append(Uri.encode(decimal(destinationLat)))
            append("&dropoff[longitude]=").append(Uri.encode(decimal(destinationLng)))
            append("&dropoff[nickname]=").append(Uri.encode(destinationLabel))
            append("&dropoff[formatted_address]=").append(Uri.encode(destinationAddress))
        }
    }

    private fun buildLyftDeepLink(
        anchor: TransportAnchor,
        destination: TransportDestination
    ): String? {
        val destinationLat = destination.latitude ?: return null
        val destinationLng = destination.longitude ?: return null

        return buildString {
            append("lyft://ridetype?id=lyft")
            append("&pickup[latitude]=").append(Uri.encode(decimal(anchor.latitude)))
            append("&pickup[longitude]=").append(Uri.encode(decimal(anchor.longitude)))
            append("&destination[latitude]=").append(Uri.encode(decimal(destinationLat)))
            append("&destination[longitude]=").append(Uri.encode(decimal(destinationLng)))
            destination.address
                ?.takeIf { it.isNotBlank() }
                ?.let { address ->
                    append("&destination[address]=").append(Uri.encode(address))
                }
        }
    }

    private fun estimateRideShareUsd(distanceMeters: Int?): String? {
        val distance = distanceMeters ?: return null
        if (distance <= 0) return null

        // No partner pricing API is wired yet, so this is a coarse distance-based range in USD.
        val miles = distance / 1609.34
        val low = 6.0 + (miles * 1.45)
        val high = 9.0 + (miles * 2.25)
        val lowRounded = low.roundToInt().coerceAtLeast(6)
        val highRounded = high.roundToInt().coerceAtLeast(lowRounded + 2)
        return "\$$lowRounded-\$$highRounded"
    }

    private fun transportCacheKey(
        anchor: TransportAnchor,
        destination: TransportDestination
    ): String {
        val destinationKey = listOfNotNull(
            destination.latitude?.let(::decimal),
            destination.longitude?.let(::decimal),
            destination.address?.trim()?.lowercase(Locale.US)
        ).joinToString("|")

        return listOf(
            decimal(anchor.latitude),
            decimal(anchor.longitude),
            destinationKey
        ).joinToString("|")
    }

    private fun formatCoordinates(
        latitude: Double,
        longitude: Double
    ): String = "${decimal(latitude)},${decimal(longitude)}"

    private fun decimal(value: Double): String = String.format(Locale.US, "%.6f", value)
}
