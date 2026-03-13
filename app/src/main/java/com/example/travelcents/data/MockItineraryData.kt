package com.example.travelcents.data // Make sure this matches your actual package

import com.example.travelcents.data.model.TravelEvent

object MockItineraryData {

    // Keeping this so your TripHeader doesn't crash
    object sampleTrip {
        val trip_name = "Tokyo Birthday Getaway"
    }

    val sampleEvents: List<TravelEvent> = listOf(
        // 1. THE FLIGHT
        TravelEvent(
            eventId = "flight_001",
            type = "flight",
            itineraryId = "tokyo_123",
            date = "2026-03-20",
            startTime = "10:30 AM",
            endTime = "02:00 PM",
            details = mapOf(
                "destination_airport" to "HND",
                "airline" to "JAL",
                "flight_number" to "JL061"
            )
        ),

        // 2. THE HOTEL
        TravelEvent(
            eventId = "hotel_001",
            type = "hotel",
            itineraryId = "tokyo_123",
            date = "2026-03-20",
            startTime = "03:00 PM",
            endTime = "",
            details = mapOf(
                "hotel_name" to "Shinjuku Granbell Hotel"
            )
        ),

        // 3. THE RESTAURANT
        TravelEvent(
            eventId = "food_001",
            type = "restaurant",
            itineraryId = "tokyo_123",
            date = "2026-03-20",
            startTime = "07:30 PM",
            endTime = "09:00 PM",
            details = mapOf(
                "restaurant_name" to "Gyukatsu Motomura",
                "cuisine" to "Japanese Beef Cutlet",
                "reservation_time" to "07:30 PM"
            )
        )
    )
}