package com.example.travelcents.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.local.CurrencyRateCache
import com.example.travelcents.data.remote.CurrencyApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {

    private val api: CurrencyApiService = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CurrencyApiService::class.java)

    private val cache = CurrencyRateCache(application)

    // frankfurter.app supported currencies (stable list, no endpoint needed)
    val currencies = listOf(
        "AUD", "BGN", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK",
        "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR", "ISK",
        "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PLN",
        "RON", "SEK", "SGD", "THB", "TRY", "USD", "ZAR"
    )

    var amount by mutableStateOf("1")
        private set
    var fromCurrency by mutableStateOf("USD")
        private set
    var toCurrency by mutableStateOf("EUR")
        private set
    var result by mutableStateOf<Double?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    // Last 3 currencies the user has picked (across both dropdowns), most recent first
    var recentCurrencies by mutableStateOf(listOf<String>())
        private set

    private var convertJob: Job? = null

    init {
        // Show cached result immediately (fast path), then silently refresh in background
        scheduleConvert()
        refreshRecentRatesAtStartup()
    }

    fun onAmountChange(newAmount: String) {
        // Only allow digits and a single decimal point
        if (newAmount.count { it == '.' } <= 1 && newAmount.all { it.isDigit() || it == '.' }) {
            amount = newAmount
            scheduleConvert()
        }
    }

    fun onFromCurrencyChange(currency: String) {
        fromCurrency = currency
        addToRecent(currency)
        cache.saveRecentPair(fromCurrency, toCurrency)
        scheduleConvert()
    }

    fun onToCurrencyChange(currency: String) {
        toCurrency = currency
        addToRecent(currency)
        cache.saveRecentPair(fromCurrency, toCurrency)
        scheduleConvert()
    }

    private fun addToRecent(currency: String) {
        // Prepend, deduplicate, keep max 3
        recentCurrencies = (listOf(currency) + recentCurrencies).distinct().take(3)
    }

    fun swap() {
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
        scheduleConvert()
    }

    // Silently refresh all recently used pairs at startup so cached rates stay fresh
    private fun refreshRecentRatesAtStartup() {
        viewModelScope.launch {
            val recentPairs = cache.getRecentPairs()
            for ((from, to) in recentPairs) {
                try {
                    val response = api.convert(1.0, from, to)
                    val baseRate = response.rates[to] ?: continue
                    cache.saveRate(from, to, baseRate)
                } catch (_: Exception) {
                    // Stale cache is better than no result — swallow silently
                }
            }
            // Recompute with fresh values after all refreshes complete
            convert()
        }
    }

    // Debounce: wait 400ms after the last change before computing/fetching
    private fun scheduleConvert() {
        convertJob?.cancel()
        convertJob = viewModelScope.launch {
            delay(400)
            convert()
        }
    }

    private suspend fun convert() {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        if (parsedAmount <= 0) { result = null; return }

        // Same currency — no API call needed
        if (fromCurrency == toCurrency) { result = parsedAmount; return }

        // Serve from cache when available (instant, no spinner)
        val cachedRate = cache.getRate(fromCurrency, toCurrency)
        if (cachedRate != null) {
            result = parsedAmount * cachedRate
            return
        }

        // Cache miss — fetch base rate from API, then cache it
        isLoading = true
        error = null
        try {
            val response = api.convert(1.0, fromCurrency, toCurrency)
            val baseRate = response.rates[toCurrency]
            if (baseRate != null) {
                cache.saveRate(fromCurrency, toCurrency, baseRate)
                cache.saveRecentPair(fromCurrency, toCurrency)
                result = parsedAmount * baseRate
            }
        } catch (e: Exception) {
            error = "Conversion failed"
            result = null
        } finally {
            isLoading = false
        }
    }
}
