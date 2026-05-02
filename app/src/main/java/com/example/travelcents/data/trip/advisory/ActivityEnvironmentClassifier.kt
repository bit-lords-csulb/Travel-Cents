package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.CONFIDENCE_HIGH
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.CONFIDENCE_LOW
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.CONFIDENCE_MEDIUM
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.ENVIRONMENT_INDOOR
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.ENVIRONMENT_MIXED
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.ENVIRONMENT_OUTDOOR
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.ENVIRONMENT_UNKNOWN
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.SENSITIVITY_HEAT
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.SENSITIVITY_NONE
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.SENSITIVITY_RAIN
import com.example.travelcents.data.trip.advisory.ActivityEnvironmentMetadata.Companion.SENSITIVITY_WIND
import com.example.travelcents.data.trip.model.ATTR_ACTIVITY_ENVIRONMENT
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_ENVIRONMENT_CONFIDENCE
import com.example.travelcents.data.trip.model.ATTR_WEATHER_SENSITIVITY
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.util.Locale

object ActivityEnvironmentClassifier {

    fun classify(event: TravelEvent): ActivityEnvironmentMetadata {
        val explicitEnvironment = event.detailValue(ATTR_ACTIVITY_ENVIRONMENT)
            ?.normalizeEnvironment()
        val explicitSensitivity = event.detailValue(ATTR_WEATHER_SENSITIVITY)
            ?.normalizeSensitivity()
        val explicitConfidence = event.detailValue(ATTR_ENVIRONMENT_CONFIDENCE)
            ?.normalizeConfidence()

        if (explicitEnvironment != null) {
            return ActivityEnvironmentMetadata(
                environment = explicitEnvironment,
                weatherSensitivity = explicitSensitivity ?: defaultSensitivity(explicitEnvironment),
                confidence = explicitConfidence ?: CONFIDENCE_HIGH
            )
        }

        val inferred = inferFromText(event)
        return inferred.copy(
            weatherSensitivity = explicitSensitivity ?: inferred.weatherSensitivity,
            confidence = explicitConfidence ?: inferred.confidence
        )
    }

    fun inferFromText(event: TravelEvent): ActivityEnvironmentMetadata {
        val haystack = listOfNotNull(
            event.type,
            event.detailValue("title", "activity_name", ATTR_BUSINESS_NAME),
            event.detailValue("description"),
            event.detailValue("category", ATTR_CATEGORIES)
        ).joinToString(" ").lowercase(Locale.US)

        if (haystack.isBlank()) {
            return unknown()
        }

        val indoorKeywords = listOf(
            "aquarium",
            "arcade",
            "gallery",
            "indoor",
            "mall",
            "market hall",
            "museum",
            "planetarium",
            "spa",
            "theater",
            "theatre"
        )
        val outdoorKeywords = listOf(
            "beach",
            "bike",
            "botanical garden",
            "garden",
            "hike",
            "kayak",
            "park",
            "safari",
            "snorkel",
            "trail",
            "walking tour",
            "zoo"
        )
        val mixedKeywords = listOf(
            "covered market",
            "food hall",
            "stadium",
            "tour"
        )

        val indoorHit = indoorKeywords.any { it in haystack }
        val outdoorHit = outdoorKeywords.any { it in haystack }
        val mixedHit = mixedKeywords.any { it in haystack }

        return when {
            outdoorHit && !indoorHit -> ActivityEnvironmentMetadata(
                environment = ENVIRONMENT_OUTDOOR,
                weatherSensitivity = outdoorSensitivity(haystack),
                confidence = CONFIDENCE_MEDIUM
            )
            indoorHit && !outdoorHit -> ActivityEnvironmentMetadata(
                environment = ENVIRONMENT_INDOOR,
                weatherSensitivity = SENSITIVITY_NONE,
                confidence = CONFIDENCE_MEDIUM
            )
            mixedHit || (indoorHit && outdoorHit) -> ActivityEnvironmentMetadata(
                environment = ENVIRONMENT_MIXED,
                weatherSensitivity = SENSITIVITY_RAIN,
                confidence = CONFIDENCE_LOW
            )
            else -> unknown()
        }
    }

    private fun unknown(): ActivityEnvironmentMetadata {
        return ActivityEnvironmentMetadata(
            environment = ENVIRONMENT_UNKNOWN,
            weatherSensitivity = SENSITIVITY_NONE,
            confidence = CONFIDENCE_LOW
        )
    }

    private fun defaultSensitivity(environment: String): String {
        return when (environment) {
            ENVIRONMENT_OUTDOOR, ENVIRONMENT_MIXED -> SENSITIVITY_RAIN
            else -> SENSITIVITY_NONE
        }
    }

    private fun outdoorSensitivity(haystack: String): String {
        return when {
            "beach" in haystack -> SENSITIVITY_HEAT
            "kayak" in haystack || "snorkel" in haystack -> SENSITIVITY_WIND
            else -> SENSITIVITY_RAIN
        }
    }

    private fun String.normalizeEnvironment(): String? {
        return when (trim().lowercase(Locale.US)) {
            ENVIRONMENT_INDOOR -> ENVIRONMENT_INDOOR
            ENVIRONMENT_OUTDOOR -> ENVIRONMENT_OUTDOOR
            ENVIRONMENT_MIXED -> ENVIRONMENT_MIXED
            ENVIRONMENT_UNKNOWN -> ENVIRONMENT_UNKNOWN
            else -> null
        }
    }

    private fun String.normalizeSensitivity(): String? {
        return when (trim().lowercase(Locale.US)) {
            SENSITIVITY_RAIN -> SENSITIVITY_RAIN
            SENSITIVITY_HEAT -> SENSITIVITY_HEAT
            SENSITIVITY_WIND -> SENSITIVITY_WIND
            SENSITIVITY_NONE -> SENSITIVITY_NONE
            else -> null
        }
    }

    private fun String.normalizeConfidence(): String? {
        return when (trim().lowercase(Locale.US)) {
            CONFIDENCE_HIGH -> CONFIDENCE_HIGH
            CONFIDENCE_MEDIUM -> CONFIDENCE_MEDIUM
            CONFIDENCE_LOW -> CONFIDENCE_LOW
            else -> null
        }
    }
}
