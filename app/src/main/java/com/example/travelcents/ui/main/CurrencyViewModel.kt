package com.example.travelcents.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.remote.CurrencyApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CurrencyViewModel : ViewModel() {

    private val api: CurrencyApiService = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CurrencyApiService::class.java)

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

    private var convertJob: Job? = null

    init {
        // Kick off an initial conversion on launch
        scheduleConvert()
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
        scheduleConvert()
    }

    fun onToCurrencyChange(currency: String) {
        toCurrency = currency
        scheduleConvert()
    }

    fun swap() {
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
        scheduleConvert()
    }

    // Debounce: wait 400ms after the last change before hitting the API
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

        isLoading = true
        error = null
        try {
            val response = api.convert(parsedAmount, fromCurrency, toCurrency)
            result = response.rates[toCurrency]
        } catch (e: Exception) {
            error = "Conversion failed"
            result = null
        } finally {
            isLoading = false
        }
    }
}
