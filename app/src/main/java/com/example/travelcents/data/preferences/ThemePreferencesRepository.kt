package com.example.travelcents.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferencesRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _darkModeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DARK_MODE, true)
    )
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_DARK_MODE) {
            _darkModeEnabled.value = prefs.getBoolean(KEY_DARK_MODE, true)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        if (_darkModeEnabled.value == enabled) return

        prefs.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
        _darkModeEnabled.value = enabled
    }

    fun toggleDarkMode() {
        setDarkModeEnabled(!_darkModeEnabled.value)
    }

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_DARK_MODE = "dark_mode_enabled"

        @Volatile
        private var instance: ThemePreferencesRepository? = null

        fun getInstance(context: Context): ThemePreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferencesRepository(context).also { instance = it }
            }
        }
    }
}
