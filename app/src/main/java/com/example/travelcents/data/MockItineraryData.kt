package com.example.travelcents.data

object MockItineraryData {

    // 1. The Trip Overview
    val sampleTrip = Trip(
        itinerary_id = "trip_002",
        user_id = "test_user_99",
        trip_name = "Post-Grad Paris Getaway",
        destination = "Paris, France",
        origin = "Los Angeles, CA",
        date_from = "2026-06-05",
        date_to = "2026-06-12",
        duration_days = 7,
        currency = "EUR",
        estimated_total_budget = 4500.0,
        travel_style = "comfort",
        travelers = Travelers(adults = 2, children = 0),
        summary = "Celebrating the CS degree with a week exploring Paris. Focusing on sights and great food.",
        highlights = listOf("Eiffel Tower", "Le Marais", "Steak Frites"),
        event_ids = listOf("evt_201", "evt_202", "evt_203"),
        tags = listOf("international", "celebration", "couples-trip"),
        created_at = "2026-03-03T12:00:00Z",
        status = "draft"
    )

    // 2. The Detailed Events List
    val sampleEvents: List<TripEvent> = listOf(

        TripEvent.Flight(
            event_id = "evt_201",
            itinerary_id = "trip_002",
            day = 1,
            date = "2026-06-05",
            airline = "Air France",
            flight_number = "AF077",
            origin_airport = "LAX",
            destination_airport = "CDG",
            departure_time = "15:20",
            arrival_time = "11:15",
            cabin_class = "economy",
            price_per_person = 850.0,
            total_price = 1700.0,
            booking_reference = "XYZ892",
            baggage_allowance_kg = 23,
            notes = "Long haul flight. Download some shows to the iPad beforehand."
        ),

        TripEvent.Hotel(
            event_id = "evt_202",
            itinerary_id = "trip_002",
            day = 2,
            check_in_date = "2026-06-06",
            check_out_date = "2026-06-12",
            hotel_name = "Hôtel Caron de Beaumarchais",
            star_rating = 3,
            address = "12 Rue Vieille-du-Temple, 75004 Paris",
            room_type = "Classic Double",
            price_per_night = 210.0,
            total_price = 1260.0,
            amenities = listOf("Free WiFi", "AC", "Minibar"),
            breakfast_included = false,
            cancellation_policy = "moderate",
            confirmation_number = "HCB-4491",
            notes = "Right in the heart of Le Marais. Very walkable."
        ),

        TripEvent.Restaurant(
            event_id = "evt_203",
            itinerary_id = "trip_002",
            day = 2,
            date = "2026-06-06",
            meal_time = "dinner",
            restaurant_name = "Le Relais de l'Entrecôte",
            cuisine = "French Steakhouse",
            address = "20 Rue Saint-Benoît, 75006 Paris",
            reservation_time = "19:30",
            price_per_person = 40.0,
            total_price = 80.0,
            dress_code = "casual",
            michelin_stars = 0,
            dietary_options = listOf("meat-heavy"),
            reservation_id = "No Reservations",
            notes = "Famous for serving only steak and fries. Heavy on the meat, carbs, and fats. Zero vegetables or beans to deal with. They don't take reservations, so we have to wait in line."
        ),
        TripEvent.Restaurant(
        event_id = "evt_204",
        itinerary_id = "trip_002",
        day = 3,
        date = "2026-06-07",
        meal_time = "lunch",
        restaurant_name = "L'As du Fallafel",
        cuisine = "Middle Eastern",
        address = "32-34 Rue des Rosiers, 75004 Paris",
        reservation_time = "13:00",
        price_per_person = 15.0,
        total_price = 30.0,
        dress_code = "casual",
        michelin_stars = 0,
        dietary_options = listOf("meat"),
        reservation_id = "Walk-in",
        notes = "Iconic spot in Le Marais. Stick to the shawarma."
    ),

    // Day 4: Museums
    TripEvent.Restaurant(
    event_id = "evt_205",
    itinerary_id = "trip_002",
    day = 4,
    date = "2026-06-08",
    meal_time = "dinner",
    restaurant_name = "Le Comptoir de La Relais",
    cuisine = "French Bistro",
    address = "9 Carrefour de l'Odéon, 75006 Paris",
    reservation_time = "20:00",
    price_per_person = 60.0,
    total_price = 120.0,
    dress_code = "smart-casual",
    michelin_stars = 0,
    dietary_options = listOf("meat-heavy"),
    reservation_id = "N/A",
    notes = "Classic bistro vibes. Very high quality meat."
    ),

    // Day 5: Day Trip
    TripEvent.Flight(
    event_id = "evt_206",
    itinerary_id = "trip_002",
    day = 5,
    date = "2026-06-09",
    airline = "SNCF (Train)", // Using Flight model for transport for now
    flight_number = "TGV 8812",
    origin_airport = "Gare du Nord",
    destination_airport = "Bordeaux",
    departure_time = "09:00",
    arrival_time = "11:00",
    cabin_class = "first",
    price_per_person = 90.0,
    total_price = 180.0,
    booking_reference = "BXD-992",
    baggage_allowance_kg = 30,
    notes = "Quick day trip out of the city."
    ),

    // Day 6: Final Dinner
    TripEvent.Restaurant(
    event_id = "evt_207",
    itinerary_id = "trip_002",
    day = 6,
    date = "2026-06-10",
    meal_time = "dinner",
    restaurant_name = "Beefbar Paris",
    cuisine = "Steakhouse",
    address = "5 Rue du Commandant Rivière, 75008 Paris",
    reservation_time = "20:30",
    price_per_person = 100.0,
    total_price = 200.0,
    dress_code = "formal",
    michelin_stars = 0,
    dietary_options = listOf("Premium Meat"),
    reservation_id = "BFB-P-112",
    notes = "Fancy final dinner. Great steak selection."
    )
    )
}