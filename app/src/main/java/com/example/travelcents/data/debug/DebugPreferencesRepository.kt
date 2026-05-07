package com.example.travelcents.data.debug

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugPreferencesRepository {
    private const val PREFS_NAME = "debug_preferences"
    private const val KEY_AI_DEBUG_UI = "show_ai_debug_ui"

    private lateinit var prefs: SharedPreferences

    private val _showAiDebugUi = MutableStateFlow(false)
    val showAiDebugUi: StateFlow<Boolean> = _showAiDebugUi.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _showAiDebugUi.value = prefs.getBoolean(KEY_AI_DEBUG_UI, false)
    }

    fun setShowAiDebugUi(enabled: Boolean) {
        _showAiDebugUi.value = enabled
        prefs.edit().putBoolean(KEY_AI_DEBUG_UI, enabled).apply()
    }
}
