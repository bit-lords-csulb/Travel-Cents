package com.example.travelcents.data.trip.local

import android.content.Context
import androidx.core.content.edit

class CurrencyRateCache(context: Context) {

    private val prefs = context.getSharedPreferences("currency_rates", Context.MODE_PRIVATE)

    // --- Rate storage ---

    fun getRate(from: String, to: String): Double? {
        val key = "${from}_${to}"
        return if (prefs.contains(key)) prefs.getFloat(key, 0f).toDouble() else null
    }

    fun saveRate(from: String, to: String, rate: Double) {
        prefs.edit {
            putFloat("${from}_${to}", rate.toFloat())
            // Auto-save inverse so reverse pairs are always served locally
            putFloat("${to}_${from}", (1.0 / rate).toFloat())
        }
    }

    // --- Recent pairs storage ---
    // Serialized as "USD,EUR|EUR,JPY|..." (max 5 pairs)

    private val recentPairsKey = "recent_pairs"

    fun getRecentPairs(): List<Pair<String, String>> {
        val raw = prefs.getString(recentPairsKey, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
    }

    fun saveRecentPair(from: String, to: String) {
        val existing = getRecentPairs()
        val updated = (listOf(Pair(from, to)) + existing)
            .distinctBy { it.first + "_" + it.second }
            .take(5)
        prefs.edit {
            putString(recentPairsKey, updated.joinToString("|") { "${it.first},${it.second}" })
        }
    }

    // --- Selected pair (persisted across restarts) ---

    fun saveSelectedPair(from: String, to: String) {
        prefs.edit {
            putString("selected_from", from)
            putString("selected_to", to)
        }
    }

    fun getSelectedPair(): Pair<String, String>? {
        val from = prefs.getString("selected_from", null) ?: return null
        val to = prefs.getString("selected_to", null) ?: return null
        return Pair(from, to)
    }
}


