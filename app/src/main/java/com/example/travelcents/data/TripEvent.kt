package com.example.travelcents.data

sealed class TripEvent {
    abstract val event_id: String
    abstract val type: String
    abstract val itinerary_id: String
    abstract val day: Int
    abstract val notes: String?


    data class Flight(
        override val event_id: String = "",
        override val type: String = "flight",
        override val itinerary_id: String = "",
        override val day: Int = 0,
        override val notes: String? = null,

        val date: String = "",
        val airline: String = "",
        val flight_number: String = "",
        val origin_airport: String = "",
        val destination_airport: String = "",
        val departure_time: String = "",
        val arrival_time: String = "",
        val cabin_class: String = "",
        val price_per_person: Double = 0.0,
        val total_price: Double = 0.0,
        val booking_reference: String = "",
        val baggage_allowance_kg: Int = 0
    ) : TripEvent()

    data class Hotel(
        override val event_id: String = "",
        override val type: String = "hotel",
        override val itinerary_id: String = "",
        override val day: Int = 0,
        override val notes: String? = null,

        val check_in_date: String = "",
        val check_out_date: String = "",
        val hotel_name: String = "",
        val star_rating: Int = 0,
        val address: String = "",
        val room_type: String = "",
        val price_per_night: Double = 0.0,
        val total_price: Double = 0.0,
        val amenities: List<String> = emptyList(),
        val breakfast_included: Boolean = false,
        val cancellation_policy: String = "",
        val confirmation_number: String = ""
    ) : TripEvent()

    data class Restaurant(
        override val event_id: String = "",
        override val type: String = "restaurant",
        override val itinerary_id: String = "",
        override val day: Int = 0,
        override val notes: String? = null,

        val date: String = "",
        val meal_time: String = "",
        val restaurant_name: String = "",
        val cuisine: String = "",
        val address: String = "",
        val reservation_time: String = "",
        val price_per_person: Double = 0.0,
        val total_price: Double = 0.0,
        val dress_code: String = "",
        val michelin_stars: Int = 0,
        val dietary_options: List<String> = emptyList(),
        val reservation_id: String = ""
    ) : TripEvent()

    data class Activity(
        override val event_id: String = "",
        override val type: String = "activity",
        override val itinerary_id: String = "",
        override val day: Int = 0,
        override val notes: String? = null,

        val date: String = "",
        val start_time: String = "",
        val end_time: String = "",
        val activity_name: String = "",
        val category: String = "",
        val location: String = "",
        val description: String = "",
        val price_per_person: Double = 0.0,
        val total_price: Double = 0.0,
        val booking_required: Boolean = false,
        val booking_id: String? = null,
        val difficulty_level: String = "",
        val duration_hours: Double = 0.0
    ) : TripEvent()

    data class ConcertEvent(
        override val event_id: String = "",
        override val type: String = "concert_event",
        override val itinerary_id: String = "",
        override val day: Int = 0,
        override val notes: String? = null,

        val date: String = "",
        val event_name: String = "",
        val venue: String = "",
        val address: String = "",
        val start_time: String = "",
        val end_time: String = "",
        val category: String = "",
        val artist_or_team: String = "",
        val seat_section: String = "",
        val price_per_person: Double = 0.0,
        val total_price: Double = 0.0,
        val ticket_id: String = "",
        val age_restriction: String = ""
    ) : TripEvent()

    data class Transport(
        override val event_id: String = "",
        override val type: String = "transport",
        override val itinerary_id: String = "",
        override val day: Int = 0,
        override val notes: String? = null,

        val date: String = "",
        val transport_mode: String = "",
        val provider: String = "",
        val pickup_location: String = "",
        val dropoff_location: String = "",
        val departure_time: String = "",
        val arrival_time: String = "",
        val price: Double = 0.0,
        val confirmation_number: String? = null
    ) : TripEvent()
}