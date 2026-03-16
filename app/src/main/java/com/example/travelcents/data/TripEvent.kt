package com.example.travelcents.data

sealed class TripEvent {
    // These shared fields must be present in every subclass
    abstract val eventId: String
    abstract val itineraryId: String
    abstract val date: String
    abstract val startTime: String
    abstract val endTime: String
    abstract val type: String
    abstract val tz: String

    data class Flight(
        override val eventId: String = "",
        override val itineraryId: String = "",
        override val date: String = "",
        override val startTime: String = "",
        override val endTime: String = "",
        override val type: String = "flight",
        override val tz: String = "",
        val airline: String = "",
        val flight_number: String = "",
        val origin_airport: String = "",
        val destination_airport: String = ""
    ) : TripEvent()

    data class Hotel(
        override val eventId: String = "",
        override val itineraryId: String = "",
        override val date: String = "",
        override val startTime: String = "",
        override val endTime: String = "",
        override val type: String = "hotel",
        override val tz: String = "",
        val hotel_name: String = "",
        val check_in_date: String = "",
        val check_out_date: String = "",
        val room_type: String = ""
    ) : TripEvent()

    data class Restaurant(
        override val eventId: String = "",
        override val itineraryId: String = "",
        override val date: String = "",
        override val startTime: String = "",
        override val endTime: String = "",
        override val type: String = "restaurant",
        override val tz: String = "",
        val restaurant_name: String = ""
    ) : TripEvent()

    data class Activity(
        override val eventId: String = "",
        override val itineraryId: String = "",
        override val date: String = "",
        override val startTime: String = "",
        override val endTime: String = "",
        override val type: String = "activity",
        override val tz: String = "",
        val activity_name: String = ""
    ) : TripEvent()
}