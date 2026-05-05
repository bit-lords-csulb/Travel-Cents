package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.model.EventOption

data class TripAdvisory(
    val advisoryId: String,
    val eventId: String,
    val severity: AdvisorySeverity,
    val reason: AdvisoryReason,
    val title: String,
    val message: String,
    val affectedDate: String,
    val affectedStartTime: String,
    val contextSummary: String,
    val suggestedOptions: List<EventOption>,
    val generatedAtEpochMs: Long
) {
    val dismissalKey: String = "$eventId:${reason.name}"
}

enum class AdvisorySeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class AdvisoryReason {
    RAIN_OUTDOOR_ACTIVITY,
    EXTREME_HEAT,
    HIGH_WIND,
    TRANSIT_DELAY,
    WALKING_TIME_TOO_LONG,
    RIDESHARE_COST_SPIKE
}

data class WeatherContext(
    val condition: String,
    val precipitationPct: Int,
    val temperatureC: Int,
    val windKph: Int,
    val startsAtLocalTime: String?
)

data class TransportContext(
    val walkMin: Int?,
    val transitMin: Int?,
    val rideshareMin: Int?,
    val delayMin: Int,
    val reliability: String,
    val summary: String
)

data class ActivityEnvironmentMetadata(
    val environment: String,
    val weatherSensitivity: String,
    val confidence: String
) {
    val isOutdoorLike: Boolean
        get() = environment == ENVIRONMENT_OUTDOOR || environment == ENVIRONMENT_MIXED

    companion object {
        const val ENVIRONMENT_INDOOR = "indoor"
        const val ENVIRONMENT_OUTDOOR = "outdoor"
        const val ENVIRONMENT_MIXED = "mixed"
        const val ENVIRONMENT_UNKNOWN = "unknown"

        const val SENSITIVITY_RAIN = "rain"
        const val SENSITIVITY_HEAT = "heat"
        const val SENSITIVITY_WIND = "wind"
        const val SENSITIVITY_NONE = "none"

        const val CONFIDENCE_HIGH = "high"
        const val CONFIDENCE_MEDIUM = "medium"
        const val CONFIDENCE_LOW = "low"
    }
}
