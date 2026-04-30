package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * AviationStack-backed live flight status. Free tier: 100 requests/month, HTTP only.
 * Quota guards: 12-hour time gate and 30-minute cache.
 */
object FlightStatusRepository {

    private const val BASE_URL = "http://api.aviationstack.com/v1/flights"
    private const val CACHE_TTL_MS = 30 * 60 * 1000L
    private const val LIVE_WINDOW_HOURS = 12

    enum class Status { SCHEDULED, ACTIVE, LANDED, CANCELLED, DIVERTED, INCIDENT, UNKNOWN }

    data class Endpoint(
        val airportName: String?,
        val iata: String?,
        val icao: String?,
        val terminal: String?,
        val gate: String?,
        val baggageBelt: String?,
        val timezone: String?,
        val delayMinutes: Long?,
        val scheduledUnix: Long?,
        val estimatedUnix: Long?,
        val actualUnix: Long?
    )

    data class LivePosition(
        val updatedUnix: Long?,
        val latitude: Double?,
        val longitude: Double?,
        val altitudeMeters: Double?,
        val speedKph: Double?,
        val verticalSpeedKph: Double?,
        val headingDeg: Double?,
        val isGround: Boolean?
    ) {
        val hasAnySignal: Boolean
            get() = latitude != null || longitude != null || altitudeMeters != null ||
                speedKph != null || headingDeg != null
    }

    data class Snapshot(
        val status: Status,
        val flightIata: String?,
        val flightNumber: String?,
        val airlineName: String?,
        val airlineIata: String?,
        val aircraftType: String?,
        val aircraftRegistration: String?,
        val aircraftIcao24: String?,
        val departure: Endpoint,
        val arrival: Endpoint,
        val live: LivePosition?,
        val updatedAtUnix: Long
    )

    private data class CacheEntry(val snapshot: Snapshot?, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchStatus(
        flightNumber: String,
        scheduledDepartureLocal: String,
        originIata: String,
        scheduledZone: String
    ): Snapshot? {
        val apiKey = BuildConfig.AVIATIONSTACK_KEY
        if (apiKey.isBlank() || flightNumber.isBlank()) return null

        val flightIata = normalizeFlightIata(flightNumber) ?: return null
        val scheduledUnix = parseScheduledUnix(scheduledDepartureLocal, scheduledZone) ?: return null
        val nowUnix = Instant.now().epochSecond
        if (kotlin.math.abs(scheduledUnix - nowUnix) > LIVE_WINDOW_HOURS * 3600) return null

        val flightDate = scheduledDateLocal(scheduledDepartureLocal)
            ?: scheduledDateUtc(scheduledUnix)
        val cacheKey = "$flightIata|$flightDate"
        val now = System.currentTimeMillis()
        cache[cacheKey]?.takeIf { it.expiresAtMs > now }?.let { return it.snapshot }

        val snapshot = withContext(Dispatchers.IO) {
            runCatching { queryAviationStack(apiKey, flightIata, flightDate, originIata) }
                .getOrNull()
        }

        cache[cacheKey] = CacheEntry(snapshot, now + CACHE_TTL_MS)
        return snapshot
    }

    private fun queryAviationStack(
        apiKey: String,
        flightIata: String,
        flightDate: String,
        originIata: String
    ): Snapshot? {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("access_key", apiKey)
            .addQueryParameter("flight_iata", flightIata)
            .addQueryParameter("flight_date", flightDate)
            .build()

        val body = httpGet(url.toString()) ?: return null
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val dataArray = root.optJSONArray("data") ?: return null
        if (dataArray.length() == 0) return null

        var match: JSONObject? = null
        val originKey = originIata.trim().uppercase(Locale.US)
        if (originKey.isNotBlank()) {
            for (i in 0 until dataArray.length()) {
                val obj = dataArray.optJSONObject(i) ?: continue
                val depIata = obj.optJSONObject("departure")?.optString("iata")
                    ?.uppercase(Locale.US).orEmpty()
                if (depIata == originKey) {
                    match = obj
                    break
                }
            }
        }
        if (match == null) match = dataArray.optJSONObject(0) ?: return null
        return parseFlight(match)
    }

    private fun parseFlight(obj: JSONObject): Snapshot {
        val statusRaw = obj.optString("flight_status").lowercase(Locale.US)
        val airline = obj.optJSONObject("airline")
        val flight = obj.optJSONObject("flight")
        val aircraft = obj.optJSONObject("aircraft")
        val live = obj.optJSONObject("live")

        val status = when (statusRaw) {
            "scheduled" -> Status.SCHEDULED
            "active" -> Status.ACTIVE
            "landed" -> Status.LANDED
            "cancelled" -> Status.CANCELLED
            "diverted" -> Status.DIVERTED
            "incident" -> Status.INCIDENT
            else -> Status.UNKNOWN
        }

        return Snapshot(
            status = status,
            flightIata = flight?.optStringOrNull("iata"),
            flightNumber = flight?.optStringOrNull("number"),
            airlineName = airline?.optStringOrNull("name"),
            airlineIata = airline?.optStringOrNull("iata"),
            aircraftType = aircraft?.optStringOrNull("iata"),
            aircraftRegistration = aircraft?.optStringOrNull("registration"),
            aircraftIcao24 = aircraft?.optStringOrNull("icao24"),
            departure = parseEndpoint(obj.optJSONObject("departure"), includeBaggage = false),
            arrival = parseEndpoint(obj.optJSONObject("arrival"), includeBaggage = true),
            live = parseLive(live),
            updatedAtUnix = parseIsoUnix(live?.optStringOrNull("updated"))
                ?: Instant.now().epochSecond
        )
    }

    private fun parseEndpoint(obj: JSONObject?, includeBaggage: Boolean): Endpoint {
        if (obj == null) {
            return Endpoint(null, null, null, null, null, null, null, null, null, null, null)
        }
        return Endpoint(
            airportName = obj.optStringOrNull("airport"),
            iata = obj.optStringOrNull("iata"),
            icao = obj.optStringOrNull("icao"),
            terminal = obj.optStringOrNull("terminal"),
            gate = obj.optStringOrNull("gate"),
            baggageBelt = if (includeBaggage) obj.optStringOrNull("baggage") else null,
            timezone = obj.optStringOrNull("timezone"),
            delayMinutes = obj.optIntOrNull("delay")?.toLong(),
            scheduledUnix = parseIsoUnix(obj.optStringOrNull("scheduled")),
            estimatedUnix = parseIsoUnix(obj.optStringOrNull("estimated")),
            actualUnix = parseIsoUnix(obj.optStringOrNull("actual"))
        )
    }

    private fun parseLive(obj: JSONObject?): LivePosition? {
        if (obj == null) return null
        val pos = LivePosition(
            updatedUnix = parseIsoUnix(obj.optStringOrNull("updated")),
            latitude = obj.optDoubleOrNull("latitude"),
            longitude = obj.optDoubleOrNull("longitude"),
            altitudeMeters = obj.optDoubleOrNull("altitude"),
            speedKph = obj.optDoubleOrNull("speed_horizontal"),
            verticalSpeedKph = obj.optDoubleOrNull("speed_vertical"),
            headingDeg = obj.optDoubleOrNull("direction"),
            isGround = if (obj.has("is_ground") && !obj.isNull("is_ground")) obj.optBoolean("is_ground") else null
        )
        return if (pos.hasAnySignal) pos else null
    }

    private fun httpGet(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull()
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val raw = optString(key, "").trim()
        return raw.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val v = optInt(key, Int.MIN_VALUE)
        return if (v == Int.MIN_VALUE) null else v
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    private fun normalizeFlightIata(input: String): String? {
        val trimmed = input.trim().uppercase(Locale.US).filter { it.isLetterOrDigit() }
        if (trimmed.length < 3) return null
        return trimmed
    }

    private fun scheduledDateLocal(scheduledLocal: String): String? {
        val datePart = scheduledLocal.trim().substringBefore(" ").takeIf { it.isNotBlank() }
            ?: return null
        return runCatching {
            LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull()
    }

    private fun scheduledDateUtc(scheduledUnix: Long): String {
        return Instant.ofEpochSecond(scheduledUnix)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun parseScheduledUnix(localDateTime: String, zoneId: String): Long? {
        val trimmed = localDateTime.trim()
        if (trimmed.isBlank()) return null
        return runCatching {
            val zone = if (zoneId.isBlank()) ZoneOffset.UTC else java.time.ZoneId.of(zoneId)
            val parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            java.time.LocalDateTime.parse(trimmed, parser).atZone(zone).toEpochSecond()
        }.recoverCatching {
            val date = LocalDate.parse(trimmed.substringBefore(" "), DateTimeFormatter.ISO_LOCAL_DATE)
            val zone = if (zoneId.isBlank()) ZoneOffset.UTC else java.time.ZoneId.of(zoneId)
            date.atStartOfDay(zone).toEpochSecond()
        }.getOrNull()
    }

    private fun parseIsoUnix(input: String?): Long? {
        val trimmed = input?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return null
        return runCatching {
            java.time.OffsetDateTime.parse(trimmed).toEpochSecond()
        }.recoverCatching {
            java.time.LocalDateTime.parse(trimmed).atOffset(ZoneOffset.UTC).toEpochSecond()
        }.getOrNull()
    }
}