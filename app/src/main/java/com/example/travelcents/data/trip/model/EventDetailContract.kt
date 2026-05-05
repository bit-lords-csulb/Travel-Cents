package com.example.travelcents.data.trip.model

import java.util.Locale

const val ATTR_HOTEL_NAME = "attr_hotel_name"
const val ATTR_HOTEL_RATING = "attr_hotel_rating"
const val ATTR_REVIEW_COUNT = "attr_review_count"
const val ATTR_HOTEL_CLASS = "attr_hotel_class"
const val ATTR_CHECK_IN_TIME = "attr_check_in_time"
const val ATTR_CHECK_OUT_TIME = "attr_check_out_time"
const val ATTR_AMENITIES = "attr_amenities"
const val ATTR_RATE_PER_NIGHT = "attr_rate_per_night"
const val ATTR_TOTAL_STAY_RATE = "attr_total_stay_rate"
const val ATTR_GROUP_RATE_PER_NIGHT = "attr_group_rate_per_night"
const val ATTR_GROUP_TOTAL_STAY_RATE = "attr_group_total_stay_rate"
const val ATTR_ROOMS_NEEDED = "attr_rooms_needed"
const val ATTR_BOOKING_URL = "attr_booking_url"
const val ATTR_HOTEL_DETAIL_URL = "attr_hotel_detail_url"
const val ATTR_HOTEL_CITY = "attr_hotel_city"
const val ATTR_DEAL_DESCRIPTION = "attr_deal_description"
const val ATTR_OFFER_COUNT = "attr_offer_count"
const val ATTR_BUSINESS_NAME = "attr_business_name"
const val ATTR_BUSINESS_ADDRESS = "attr_business_address"
const val ATTR_CATEGORIES = "attr_categories"
const val ATTR_VENUE_NAME = "attr_venue_name"
const val ATTR_PHONE = "attr_phone"
const val ATTR_HOURS_SUMMARY = "attr_hours_summary"
const val ATTR_HOURS_RAW = "attr_hours_raw"
const val ATTR_AVERAGE_RATING = "attr_average_rating"
const val ATTR_IS_CLOSED = "attr_is_closed"
const val ATTR_YELP_URL = "attr_yelp_url"
const val ATTR_PROFILE_PHOTO_URL = "attr_profile_photo_url"
const val ATTR_HAS_RESERVATIONS = "attr_has_reservations"
const val ATTR_HAS_WAITLIST = "attr_has_waitlist"
const val ATTR_HAS_REQUEST_A_QUOTE = "attr_has_request_a_quote"
const val ATTR_HAS_FOOD_ORDER = "attr_has_food_order"
const val ATTR_RESERVATION_OFFER_COUNT = "attr_reservation_offer_count"
const val ATTR_RESERVATION_PARTY_SIZE = "attr_reservation_party_size"
const val ATTR_MENU_URL = "attr_menu_url"
const val ATTR_MENU_HIGHLIGHTS = "attr_menu_highlights"
const val ATTR_DIETARY_TAGS = "attr_dietary_tags"
const val ATTR_ALLERGEN_NOTE = "attr_allergen_note"
const val ATTR_WEBSITE_URL = "attr_website_url"
const val ATTR_CUISINE = "attr_cuisine"
const val ATTR_POPULAR_TIMES_JSON = "attr_popular_times_json"
const val ATTR_CURRENT_BUSYNESS = "attr_current_busyness"
const val ATTR_ESTIMATED_WAIT_MIN = "attr_estimated_wait_min"
const val ATTR_WEATHER_TEMP_C = "attr_weather_temp_c"
const val ATTR_WEATHER_CONDITION = "attr_weather_condition"
const val ATTR_WEATHER_PRECIP_PCT = "attr_weather_precip_pct"
const val ATTR_WEATHER_SUMMARY = "attr_weather_summary"
const val ATTR_WEATHER_WIND_KPH = "attr_weather_wind_kph"
const val ATTR_HAS_OUTDOOR_SEATING = "attr_has_outdoor_seating"
const val ATTR_WALK_MIN = "attr_walk_min"
const val ATTR_TRANSIT_MIN = "attr_transit_min"
const val ATTR_RIDESHARE_MIN = "attr_rideshare_min"
const val ATTR_RIDESHARE_ESTIMATE_USD = "attr_rideshare_estimate_usd"
const val ATTR_UBER_DEEPLINK = "attr_uber_deeplink"
const val ATTR_LYFT_DEEPLINK = "attr_lyft_deeplink"
const val ATTR_TRANSPORT_ANCHOR_LABEL = "attr_transport_anchor_label"
const val ATTR_WALK_SCORE = "attr_walk_score"
const val ATTR_TRANSIT_SCORE = "attr_transit_score"
const val ATTR_BIKE_SCORE = "attr_bike_score"
const val ATTR_NEAR_CATEGORIES = "attr_near_categories"
const val ATTR_NEIGHBORHOOD_NOTE = "attr_neighborhood_note"
const val ATTR_LOCAL_CURRENCY = "attr_local_currency"
const val ATTR_HOME_CURRENCY = "attr_home_currency"
const val ATTR_FX_HISTORY_30D = "attr_fx_history_30d"
const val ATTR_ESTIMATED_LOCAL_COST = "attr_estimated_local_cost"
const val ATTR_ESTIMATED_HOME_COST = "attr_estimated_home_cost"
const val ATTR_LATITUDE = "attr_latitude"
const val ATTR_LONGITUDE = "attr_longitude"
const val ATTR_STATIC_MAP_URL = "attr_static_map_url"
const val ATTR_STATIC_MAP_PROVIDER = "attr_static_map_provider"
const val ATTR_PRICE_TIER = "attr_price_tier"
const val ATTR_PRICE_LEVEL_USD = "attr_price_level_usd"
const val ATTR_TICKET_PRICE_MIN = "attr_ticket_price_min"
const val ATTR_TICKET_PRICE_MAX = "attr_ticket_price_max"
const val ATTR_TICKET_CURRENCY = "attr_ticket_currency"
const val ATTR_TICKETMASTER_EVENT_ID = "attr_ticketmaster_event_id"
const val ATTR_OPTION_DATE = "attr_option_date"
const val ATTR_OPTION_START_TIME = "attr_option_start_time"
const val ATTR_OPTION_END_TIME = "attr_option_end_time"
const val ATTR_OPTION_TIME_LABEL = "attr_option_time_label"
const val ATTR_OPTION_TZ = "attr_option_tz"
const val ATTR_OPTION_VARIANT_KIND = "attr_option_variant_kind"
const val ATTR_YELP_DETAIL_ENRICHED = "attr_yelp_detail_enriched"
const val DETAIL_YELP_ID = "yelp_id"

// ── Flight ────────────────────────────────────────────────────────────────────
const val ATTR_HERO_IMAGE_URL = "attr_hero_image_url"
const val ATTR_HERO_IMAGE_ATTRIBUTION = "attr_hero_image_attribution"
const val ATTR_AIRLINE_LOGO_URL = "attr_airline_logo_url"
const val ATTR_ORIGIN_TZ = "attr_origin_tz"
const val ATTR_DESTINATION_TZ = "attr_destination_tz"
const val ATTR_ARRIVAL_DAY_OFFSET = "attr_arrival_day_offset"
const val ATTR_ORIGIN_CITY = "attr_origin_city"
const val ATTR_DESTINATION_CITY = "attr_destination_city"
const val ATTR_STOP_AIRPORTS = "attr_stop_airports"

// Flight live status (AviationStack-backed)
const val ATTR_FLIGHT_STATUS = "attr_flight_status"
const val ATTR_DELAY_MIN = "attr_delay_min"
const val ATTR_ARRIVAL_DELAY_MIN = "attr_arrival_delay_min"
const val ATTR_LIVE_STATUS_UPDATED_AT = "attr_live_status_updated_at"
const val ATTR_GATE = "attr_gate"
const val ATTR_TERMINAL = "attr_terminal"
const val ATTR_ARRIVAL_GATE = "attr_arrival_gate"
const val ATTR_ARRIVAL_TERMINAL = "attr_arrival_terminal"
const val ATTR_BAGGAGE_BELT = "attr_baggage_belt"
const val ATTR_AIRCRAFT_REGISTRATION = "attr_aircraft_registration"

// Flight booking offers + loyalty (Phase 3)
const val ATTR_AIRLINE_IATA = "attr_airline_iata"
const val ATTR_AIRLINE_BOOKING_URL = "attr_airline_booking_url"
const val ATTR_LOYALTY_PROGRAM = "attr_loyalty_program"
const val ATTR_POINTS_EARNED = "attr_points_earned"
const val ATTR_FREQUENT_FLYER_NUMBER = "attr_frequent_flyer_number"

fun Map<String, String>.firstNonBlank(vararg keys: String): String? {
    return keys.asSequence()
        .mapNotNull { key -> this[key]?.takeIf { it.isNotBlank() } }
        .firstOrNull()
}

fun TravelEvent.detailValue(vararg keys: String): String? = details.firstNonBlank(*keys)

fun EventOption.detailValue(vararg keys: String): String? = details.firstNonBlank(*keys)

fun TravelEvent.displayName(): String? {
    return when (type.lowercase(Locale.US)) {
        "hotel" -> detailValue(ATTR_HOTEL_NAME, "hotel_name", "title", "name")
        "restaurant", "dining", "food" -> detailValue(ATTR_BUSINESS_NAME, "restaurant_name", "title", "name")
        "activity" -> detailValue(ATTR_BUSINESS_NAME, "activity_name", "title", "name")
        else -> detailValue("title", "name")
    }
}

fun EventOption.displayName(eventType: String): String? {
    return when (eventType.lowercase(Locale.US)) {
        "hotel" -> detailValue(ATTR_HOTEL_NAME, "hotel_name", "title", "name")
        "restaurant", "dining", "food" -> detailValue(ATTR_BUSINESS_NAME, "restaurant_name", "title", "name")
        "activity" -> detailValue(ATTR_BUSINESS_NAME, "activity_name", "title", "name")
        else -> detailValue("title", "name")
    }
}
