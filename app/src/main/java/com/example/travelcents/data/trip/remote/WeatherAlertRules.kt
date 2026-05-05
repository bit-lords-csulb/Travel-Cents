package com.example.travelcents.data.trip.remote

import kotlin.math.abs

private const val SIGNIFICANT_TEMP_DELTA_C = 4
private const val SIGNIFICANT_PRECIP_DELTA_PCT = 25
private const val SIGNIFICANT_WIND_DELTA_KPH = 10
private const val WET_THRESHOLD_PCT = 60
private const val WINDY_THRESHOLD_KPH = 28

internal fun buildWeatherAlertMessage(
    eventLabel: String,
    previous: WeatherRepository.WeatherSnapshot?,
    current: WeatherRepository.WeatherSnapshot
): String? {
    val baseline = previous ?: return null
    val label = eventLabel.trim().ifBlank { "This event" }
    val changes = mutableListOf<String>()

    val tempDelta = current.temperatureC - baseline.temperatureC
    if (abs(tempDelta) >= SIGNIFICANT_TEMP_DELTA_C) {
        changes += if (tempDelta > 0) {
            "temperature rose to ${current.temperatureC}C"
        } else {
            "temperature fell to ${current.temperatureC}C"
        }
    }

    val currentPrecip = current.precipPct
    val baselinePrecip = baseline.precipPct
    if (currentPrecip != null && baselinePrecip != null) {
        val precipDelta = currentPrecip - baselinePrecip
        val crossedWetThreshold =
            (baselinePrecip < WET_THRESHOLD_PCT && currentPrecip >= WET_THRESHOLD_PCT) ||
                (baselinePrecip >= WET_THRESHOLD_PCT && currentPrecip < WET_THRESHOLD_PCT)
        if (abs(precipDelta) >= SIGNIFICANT_PRECIP_DELTA_PCT || crossedWetThreshold) {
            changes += when {
                precipDelta > 0 -> "rain chance rose to ${currentPrecip}%"
                precipDelta < 0 -> "rain chance dropped to ${currentPrecip}%"
                else -> "rain chance shifted to ${currentPrecip}%"
            }
        }
    }

    val currentWind = current.windKph
    val baselineWind = baseline.windKph
    if (currentWind != null && baselineWind != null) {
        val windDelta = currentWind - baselineWind
        val crossedWindyThreshold =
            (baselineWind < WINDY_THRESHOLD_KPH && currentWind >= WINDY_THRESHOLD_KPH) ||
                (baselineWind >= WINDY_THRESHOLD_KPH && currentWind < WINDY_THRESHOLD_KPH)
        if (abs(windDelta) >= SIGNIFICANT_WIND_DELTA_KPH || crossedWindyThreshold) {
            changes += when {
                windDelta > 0 -> "wind picked up to ${currentWind} km/h"
                windDelta < 0 -> "wind eased to ${currentWind} km/h"
                else -> "wind changed to ${currentWind} km/h"
            }
        }
    }

    val baselineBand = weatherBand(baseline.condition)
    val currentBand = weatherBand(current.condition)
    if (baselineBand != currentBand && (baselineBand != WeatherBand.DRY || currentBand != WeatherBand.DRY)) {
        changes += "conditions shifted from ${baseline.condition.lowercase()} to ${current.condition.lowercase()}"
    }

    if (changes.isEmpty()) return null
    return "Weather update for $label: ${changes.joinToString(", ")}."
}

private enum class WeatherBand {
    DRY,
    MIST,
    WET,
    SNOW,
    STORM
}

private fun weatherBand(condition: String): WeatherBand {
    return when (condition.trim().lowercase()) {
        "fog" -> WeatherBand.MIST
        "drizzle", "freezing drizzle", "rain", "freezing rain", "showers" -> WeatherBand.WET
        "snow", "snow showers" -> WeatherBand.SNOW
        "thunderstorm" -> WeatherBand.STORM
        else -> WeatherBand.DRY
    }
}
