package com.example.travelcents.data.trip.model

import java.util.Locale

private val GenericTripNames = setOf(
    "my trip",
    "unnamed trip"
)

fun defaultTripNameForDestination(destination: String): String {
    val normalizedDestination = destination
        .trim()
        .replace(Regex("\\s+"), " ")

    return normalizedDestination.ifBlank { "My Trip" }
}

fun resolveTripName(tripName: String?, destination: String): String {
    val normalizedTripName = tripName
        .orEmpty()
        .trim()
        .replace(Regex("\\s+"), " ")

    return if (
        normalizedTripName.isBlank() ||
        normalizedTripName.lowercase(Locale.US) in GenericTripNames
    ) {
        defaultTripNameForDestination(destination)
    } else {
        normalizedTripName
    }
}
