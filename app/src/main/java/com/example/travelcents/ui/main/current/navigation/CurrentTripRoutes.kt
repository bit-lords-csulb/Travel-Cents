package com.example.travelcents.ui.main.current

enum class CurrentDisplayMode {
    ITINERARY,
    DAY,
    WEEK
}

object CurrentTripRoutes {
    const val ROOT = "current"
    const val ITINERARY = "current/itinerary"
    const val DAY = "current/day"
    const val WEEK = "current/week"

    fun routeFor(mode: CurrentDisplayMode): String {
        return when (mode) {
            CurrentDisplayMode.ITINERARY -> ITINERARY
            CurrentDisplayMode.DAY -> DAY
            CurrentDisplayMode.WEEK -> WEEK
        }
    }
}
