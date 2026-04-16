package com.example.travelcents.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.travelcents.data.local.UserSettingsManager
import com.example.travelcents.data.model.Country

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val userSettingsManager = UserSettingsManager(application)
    val userSettings = userSettingsManager.settings

    fun updateCountry(country: Country) {
        userSettingsManager.updateCountry(country.name, country.code, country.currencyCode)
    }

    fun updateRegion(region: String) {
        userSettingsManager.updateRegion(region)
    }
}
