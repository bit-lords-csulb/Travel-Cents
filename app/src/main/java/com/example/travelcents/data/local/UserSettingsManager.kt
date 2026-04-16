package com.example.travelcents.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val country: String = "United States",
    val countryCode: String = "US",
    val region: String = "California",
    val currency: String = "USD",
    val temperatureUnit: String = "Celsius", // default but US will use Fahrenheit
    val dateFormat: String = "MM/dd/yyyy"
)

class UserSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val country = prefs.getString("country", "United States") ?: "United States"
        val countryCode = prefs.getString("countryCode", "US") ?: "US"
        val region = prefs.getString("region", if (countryCode == "US") "California" else "") ?: ""
        val currency = prefs.getString("currency", "USD") ?: "USD"
        val tempUnit = prefs.getString("tempUnit", if (countryCode == "US") "Fahrenheit" else "Celsius") ?: "Celsius"
        val dateFormat = prefs.getString("dateFormat", if (countryCode == "US") "MM/dd/yyyy" else "dd/MM/yyyy") ?: "dd/MM/yyyy"

        return UserSettings(country, countryCode, region, currency, tempUnit, dateFormat)
    }

    fun updateCountry(countryName: String, countryCode: String, currency: String) {
        val tempUnit = if (countryCode == "US") "Fahrenheit" else "Celsius"
        val dateFormat = if (countryCode == "US") "MM/dd/yyyy" else "dd/MM/yyyy"
        val region = if (countryCode == "US") "California" else ""
        
        prefs.edit {
            putString("country", countryName)
            putString("countryCode", countryCode)
            putString("region", region)
            putString("currency", currency)
            putString("tempUnit", tempUnit)
            putString("dateFormat", dateFormat)
        }
        _settings.value = UserSettings(countryName, countryCode, region, currency, tempUnit, dateFormat)
    }

    fun updateRegion(region: String) {
        prefs.edit {
            putString("region", region)
        }
        val current = _settings.value
        _settings.value = current.copy(region = region)
    }
}
