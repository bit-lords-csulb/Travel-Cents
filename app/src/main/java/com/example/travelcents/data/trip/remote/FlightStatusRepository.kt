package com.example.travelcents.data.trip.remote

import com.example.travelcents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * OpenSky Network-backed live flight status. Free for non-commercial use:
 * anonymous = 100 req/24h, free account = 1000 req/24h. ADS-B-derived data —
 * we get position + on-ground + actual departure time, but no gate/terminal
 * (those come from airline feeds, not transponders).
 */
object FlightStatusRepository {

    private const val OPEN_SKY_BASE = "https://opensky-network.org/api"
    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private const val LIVE_WINDOW_HOURS = 24

    enum class Status { SCHEDULED, IN_AIR, LANDED, ON_GROUND_AT_ORIGIN, UNKNOWN }

    data class Snapshot(
        val status: Status,
        val callsign: String?,
        val icao24: String?,
        val latitude: Double?,
        val longitude: Double?,
        val onGround: Boolean?,
        val actualDepartureUnix: Long?,
        val delayMinutes: Long?,
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
        if (flightNumber.isBlank() || originIata.isBlank()) return null

        val originIcao = AirportIcao.lookup(originIata) ?: return null
        val scheduledUnix = parseScheduledUnix(scheduledDepartureLocal, scheduledZone) ?: return null
        val nowUnix = Instant.now().epochSecond
        if (kotlin.math.abs(scheduledUnix - nowUnix) > LIVE_WINDOW_HOURS * 3600) return null

        val cacheKey = "${flightNumber.uppercase(Locale.US)}|$scheduledUnix"
        val now = System.currentTimeMillis()
        cache[cacheKey]?.takeIf { it.expiresAtMs > now }?.let { return it.snapshot }

        val snapshot = withContext(Dispatchers.IO) {
            runCatching {
                val begin = scheduledUnix - 3 * 3600
                val end = scheduledUnix + 6 * 3600
                val match = findDeparture(originIcao, begin, end, flightNumber)
                if (match == null) {
                    Snapshot(
                        status = if (nowUnix < scheduledUnix) Status.SCHEDULED else Status.UNKNOWN,
                        callsign = null,
                        icao24 = null,
                        latitude = null,
                        longitude = null,
                        onGround = null,
                        actualDepartureUnix = null,
                        delayMinutes = null,
                        updatedAtUnix = nowUnix
                    )
                } else {
                    val state = fetchLiveState(match.icao24)
                    val onGround = state?.onGround
                    val derivedStatus = when {
                        state == null -> Status.UNKNOWN
                        onGround == false -> Status.IN_AIR
                        onGround == true && state.lastContact != null &&
                            nowUnix - state.lastContact > 600 -> Status.LANDED
                        onGround == true -> Status.ON_GROUND_AT_ORIGIN
                        else -> Status.UNKNOWN
                    }
                    val delay = match.firstSeen?.let { actual ->
                        ((actual - scheduledUnix) / 60).coerceAtLeast(0)
                    }
                    Snapshot(
                        status = derivedStatus,
                        callsign = match.callsign,
                        icao24 = match.icao24,
                        latitude = state?.latitude,
                        longitude = state?.longitude,
                        onGround = onGround,
                        actualDepartureUnix = match.firstSeen,
                        delayMinutes = delay,
                        updatedAtUnix = nowUnix
                    )
                }
            }.getOrNull()
        }

        cache[cacheKey] = CacheEntry(snapshot, now + CACHE_TTL_MS)
        return snapshot
    }

    private data class DepartureMatch(
        val icao24: String,
        val callsign: String?,
        val firstSeen: Long?
    )

    private data class LiveState(
        val latitude: Double?,
        val longitude: Double?,
        val onGround: Boolean?,
        val lastContact: Long?
    )

    private suspend fun findDeparture(
        originIcao: String,
        beginUnix: Long,
        endUnix: Long,
        flightNumber: String
    ): DepartureMatch? {
        val url = "$OPEN_SKY_BASE/flights/departure".toHttpUrl().newBuilder()
            .addQueryParameter("airport", originIcao)
            .addQueryParameter("begin", beginUnix.toString())
            .addQueryParameter("end", endUnix.toString())
            .build()
        val body = httpGet(url.toString()) ?: return null
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return null
        val normalizedTarget = normalizeCallsign(flightNumber)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val callsign = obj.optString("callsign").takeIf { it.isNotBlank() }?.trim()
            if (callsign != null && normalizeCallsign(callsign) == normalizedTarget) {
                return DepartureMatch(
                    icao24 = obj.optString("icao24").lowercase(Locale.US),
                    callsign = callsign,
                    firstSeen = obj.optLong("firstSeen", -1L).takeIf { it > 0 }
                )
            }
        }
        return null
    }

    private suspend fun fetchLiveState(icao24: String): LiveState? {
        if (icao24.isBlank()) return null
        val url = "$OPEN_SKY_BASE/states/all".toHttpUrl().newBuilder()
            .addQueryParameter("icao24", icao24.lowercase(Locale.US))
            .build()
        val body = httpGet(url.toString()) ?: return null
        val obj = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val states = obj.optJSONArray("states") ?: return null
        val first = states.optJSONArray(0) ?: return null
        // OpenSky state vector index map per https://openskynetwork.github.io/opensky-api/rest.html
        return LiveState(
            longitude = first.optDouble(5).takeUnless { it.isNaN() },
            latitude = first.optDouble(6).takeUnless { it.isNaN() },
            onGround = if (first.isNull(8)) null else first.optBoolean(8),
            lastContact = first.optLong(4, -1L).takeIf { it > 0 }
        )
    }

    private const val TOKEN_URL =
        "https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token"

    @Volatile private var cachedAccessToken: String? = null
    @Volatile private var tokenExpiresAtMs: Long = 0L
    private val tokenMutex = Mutex()

    private suspend fun bearerTokenOrNull(): String? {
        val clientId = BuildConfig.OPENSKY_CLIENT_ID
        val clientSecret = BuildConfig.OPENSKY_CLIENT_SECRET
        if (clientId.isBlank() || clientSecret.isBlank()) return null

        val now = System.currentTimeMillis()
        cachedAccessToken?.takeIf { tokenExpiresAtMs - 30_000 > now }?.let { return it }

        return tokenMutex.withLock {
            val recheck = cachedAccessToken
            if (recheck != null && tokenExpiresAtMs - 30_000 > System.currentTimeMillis()) {
                return@withLock recheck
            }
            val form = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build()
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(form)
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string()?.takeIf { it.isNotBlank() }
                        ?: return@use null
                    val obj = JSONObject(body)
                    val token = obj.optString("access_token").takeIf { it.isNotBlank() }
                        ?: return@use null
                    val expiresIn = obj.optLong("expires_in", 60L)
                    cachedAccessToken = token
                    tokenExpiresAtMs = System.currentTimeMillis() + (expiresIn * 1000L)
                    token
                }
            }.getOrNull()
        }
    }

    private suspend fun httpGet(url: String): String? {
        val builder = Request.Builder().url(url).get()
        bearerTokenOrNull()?.let { builder.header("Authorization", "Bearer $it") }
        return withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                }
            }.getOrNull()
        }
    }

    private fun normalizeCallsign(input: String): String {
        return input.uppercase(Locale.US)
            .filter { it.isLetterOrDigit() }
    }

    private fun parseScheduledUnix(localDateTime: String, zoneId: String): Long? {
        val trimmed = localDateTime.trim()
        if (trimmed.isBlank()) return null
        return runCatching {
            val zone = if (zoneId.isBlank()) ZoneOffset.UTC else java.time.ZoneId.of(zoneId)
            val parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            java.time.LocalDateTime.parse(trimmed, parser).atZone(zone).toEpochSecond()
        }.recoverCatching {
            // Fallback: date-only, treat as midnight local
            val date = LocalDate.parse(trimmed.substringBefore(" "), DateTimeFormatter.ISO_LOCAL_DATE)
            val zone = if (zoneId.isBlank()) ZoneOffset.UTC else java.time.ZoneId.of(zoneId)
            date.atStartOfDay(zone).toEpochSecond()
        }.getOrNull()
    }
}

/**
 * Minimal IATA → ICAO airport-code lookup for the airports the app's flight
 * search already covers. OpenSky requires ICAO; SerpAPI gives us IATA.
 */
private object AirportIcao {
    private val map = mapOf(
        // United States — ICAO is "K" + IATA
        "ATL" to "KATL", "AUS" to "KAUS", "BNA" to "KBNA", "BOS" to "KBOS",
        "BUR" to "KBUR", "BWI" to "KBWI", "CLT" to "KCLT", "DAL" to "KDAL",
        "DCA" to "KDCA", "DEN" to "KDEN", "DFW" to "KDFW", "DTW" to "KDTW",
        "EWR" to "KEWR", "FLL" to "KFLL", "GEG" to "KGEG", "HOU" to "KHOU",
        "IAD" to "KIAD", "IAH" to "KIAH", "JFK" to "KJFK", "LAS" to "KLAS",
        "LAX" to "KLAX", "LGA" to "KLGA", "LGB" to "KLGB", "MCI" to "KMCI",
        "MCO" to "KMCO", "MDW" to "KMDW", "MEM" to "KMEM", "MHT" to "KMHT",
        "MIA" to "KMIA", "MSP" to "KMSP", "MSY" to "KMSY", "OAK" to "KOAK",
        "OKC" to "KOKC", "OMA" to "KOMA", "ONT" to "KONT", "ORD" to "KORD",
        "ORF" to "KORF", "PBI" to "KPBI", "PDX" to "KPDX", "PHL" to "KPHL",
        "PHX" to "KPHX", "PIT" to "KPIT", "PVD" to "KPVD", "RDU" to "KRDU",
        "RNO" to "KRNO", "SAN" to "KSAN", "SAT" to "KSAT", "SEA" to "KSEA",
        "SFO" to "KSFO", "SJC" to "KSJC", "SLC" to "KSLC", "SMF" to "KSMF",
        "SNA" to "KSNA", "STL" to "KSTL", "TPA" to "KTPA",
        // Hawaii / Alaska — keep K prefix exception
        "HNL" to "PHNL", "OGG" to "PHOG", "KOA" to "PHKO", "ITO" to "PHTO",
        "ANC" to "PANC",
        // Canada — ICAO is "C" + IATA for most
        "YYZ" to "CYYZ", "YVR" to "CYVR", "YUL" to "CYUL", "YYC" to "CYYC",
        "YOW" to "CYOW", "YEG" to "CYEG", "YHZ" to "CYHZ", "YWG" to "CYWG",
        "YTZ" to "CYTZ",
        // Europe
        "LHR" to "EGLL", "LGW" to "EGKK", "STN" to "EGSS", "LCY" to "EGLC",
        "MAN" to "EGCC", "EDI" to "EGPH",
        "CDG" to "LFPG", "ORY" to "LFPO", "NCE" to "LFMN",
        "FRA" to "EDDF", "MUC" to "EDDM", "TXL" to "EDDB", "BER" to "EDDB",
        "DUS" to "EDDL", "HHN" to "EDFH", "HAM" to "EDDH",
        "AMS" to "EHAM", "BRU" to "EBBR", "ZRH" to "LSZH", "GVA" to "LSGG",
        "VIE" to "LOWW", "MAD" to "LEMD", "BCN" to "LEBL",
        "FCO" to "LIRF", "CIA" to "LIRA", "MXP" to "LIMC", "VCE" to "LIPZ",
        "ATH" to "LGAV", "IST" to "LTFM",
        "WAW" to "EPWA", "PRG" to "LKPR", "BUD" to "LHBP", "OTP" to "LROP",
        "SOF" to "LBSF", "BEG" to "LYBE", "ZAG" to "LDZA",
        "CPH" to "EKCH", "ARN" to "ESSA", "OSL" to "ENGM", "HEL" to "EFHK",
        "DUB" to "EIDW", "KEF" to "BIKF",
        "SVO" to "UUEE", "DME" to "UUDD", "LED" to "ULLI",
        // Middle East
        "DXB" to "OMDB", "AUH" to "OMAA", "DOH" to "OTHH", "RUH" to "OERK",
        "JED" to "OEJN", "KWI" to "OKBK", "BAH" to "OBBI", "MCT" to "OOMS",
        "TLV" to "LLBG", "AMM" to "OJAI", "BEY" to "OLBA",
        // Asia
        "NRT" to "RJAA", "HND" to "RJTT", "KIX" to "RJBB", "ITM" to "RJOO",
        "ICN" to "RKSI", "GMP" to "RKSS", "PEK" to "ZBAA", "PKX" to "ZBAD",
        "PVG" to "ZSPD", "SHA" to "ZSSS", "CAN" to "ZGGG", "CTU" to "ZUUU",
        "HKG" to "VHHH", "TPE" to "RCTP", "SIN" to "WSSS", "KUL" to "WMKK",
        "BKK" to "VTBS", "DMK" to "VTBD", "HAN" to "VVNB", "SGN" to "VVTS",
        "MNL" to "RPLL", "CGK" to "WIII", "DPS" to "WADD",
        "DEL" to "VIDP", "BOM" to "VABB", "BLR" to "VOBL", "MAA" to "VOMM",
        "HYD" to "VOHS", "CCU" to "VECC",
        "KHI" to "OPKC", "LHE" to "OPLA", "ISB" to "OPRN",
        "DAC" to "VGHS", "CMB" to "VCBI",
        // Oceania
        "SYD" to "YSSY", "MEL" to "YMML", "BNE" to "YBBN", "PER" to "YPPH",
        "ADL" to "YPAD", "CBR" to "YSCB",
        "AKL" to "NZAA", "WLG" to "NZWN", "CHC" to "NZCH",
        // Latin America
        "GRU" to "SBGR", "GIG" to "SBGL", "EZE" to "SAEZ", "AEP" to "SABE",
        "SCL" to "SCEL", "LIM" to "SPJC", "BOG" to "SKBO", "MEX" to "MMMX",
        "CUN" to "MMUN", "PTY" to "MPTO", "UIO" to "SEQM",
        // Africa
        "JNB" to "FAOR", "CPT" to "FACT", "DUR" to "FALE",
        "CAI" to "HECA", "CMN" to "GMMN", "RAK" to "GMMX", "NBO" to "HKJK",
        "ADD" to "HAAB", "LOS" to "DNMM"
    )

    fun lookup(iata: String): String? {
        val key = iata.trim().uppercase(Locale.US)
        if (key.length != 3) return null
        return map[key]
    }
}
