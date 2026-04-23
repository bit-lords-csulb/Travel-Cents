package com.example.travelcents.data.trip.remote

import android.content.Context
import com.example.travelcents.data.trip.local.CurrencyRateCache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurrencyPreviewRepository(context: Context) {

    private val api: CurrencyApiService = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CurrencyApiService::class.java)

    private val cache = CurrencyRateCache(context.applicationContext)

    private val historyClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private data class HistoryCacheEntry(
        val history: String,
        val expiresAtMs: Long
    )

    data class CurrencyPreviewSnapshot(
        val localCurrency: String,
        val homeCurrency: String,
        val localCost: Double,
        val homeCost: Double,
        val fxHistory30d: String?
    )

    private val historyCache = ConcurrentHashMap<String, HistoryCacheEntry>()

    suspend fun buildPreview(
        localCurrency: String,
        homeCurrency: String,
        usdAnchorAmount: Double
    ): CurrencyPreviewSnapshot? = withContext(Dispatchers.IO) {
        val local = localCurrency.trim().uppercase(Locale.US)
        val home = homeCurrency.trim().uppercase(Locale.US)
        if (local.isBlank() || home.isBlank() || local == home || usdAnchorAmount <= 0.0) {
            return@withContext null
        }

        val localCost = convertFromUsd(usdAnchorAmount, local) ?: return@withContext null
        val homeCost = convertFromUsd(usdAnchorAmount, home) ?: return@withContext null

        CurrencyPreviewSnapshot(
            localCurrency = local,
            homeCurrency = home,
            localCost = localCost,
            homeCost = homeCost,
            fxHistory30d = fetchHistory30d(base = local, quote = home)
        )
    }

    private suspend fun convertFromUsd(
        amountUsd: Double,
        targetCurrency: String
    ): Double? {
        if (targetCurrency == "USD") return amountUsd

        val rate = cache.getRate("USD", targetCurrency) ?: runCatching {
            api.convert(
                amount = 1.0,
                from = "USD",
                to = targetCurrency
            ).rates[targetCurrency]
        }.getOrNull()?.also { resolvedRate ->
            cache.saveRate("USD", targetCurrency, resolvedRate)
        } ?: return null

        return amountUsd * rate
    }

    private fun fetchHistory30d(
        base: String,
        quote: String
    ): String? {
        val cacheKey = "$base|$quote"
        val now = System.currentTimeMillis()
        historyCache[cacheKey]
            ?.takeIf { it.expiresAtMs > now }
            ?.let { return it.history }

        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(30)
        val url = "https://api.frankfurter.dev/v2/rates".toHttpUrl().newBuilder()
            .addQueryParameter("base", base)
            .addQueryParameter("quotes", quote)
            .addQueryParameter("from", startDate.toString())
            .addQueryParameter("to", endDate.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val history = runCatching {
            historyClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string()?.takeIf { it.isNotBlank() } ?: return@use null
                val array = JSONArray(body)
                if (array.length() == 0) return@use null

                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val date = item.optString("date").takeIf { it.isNotBlank() } ?: continue
                        val rate = item.optDouble("rate", Double.NaN)
                        if (rate.isNaN()) continue
                        add("$date,${String.format(Locale.US, "%.6f", rate)}")
                    }
                }.takeIf { it.isNotEmpty() }?.joinToString("|")
            }
        }.getOrNull() ?: return null

        historyCache[cacheKey] = HistoryCacheEntry(
            history = history,
            expiresAtMs = now + TimeUnit.HOURS.toMillis(12)
        )
        return history
    }
}
