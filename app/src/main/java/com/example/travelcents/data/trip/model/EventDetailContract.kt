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
const val ATTR_GROUP_RATE_PER_NIGHT = "attr_group_rate_per_night"
const val ATTR_ROOMS_NEEDED = "attr_rooms_needed"
const val ATTR_BOOKING_URL = "attr_booking_url"
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
const val ATTR_MENU_URL = "attr_menu_url"
const val ATTR_LATITUDE = "attr_latitude"
const val ATTR_LONGITUDE = "attr_longitude"
const val ATTR_STATIC_MAP_URL = "attr_static_map_url"
const val ATTR_STATIC_MAP_PROVIDER = "attr_static_map_provider"
const val ATTR_PRICE_TIER = "attr_price_tier"
const val ATTR_TICKET_PRICE_MIN = "attr_ticket_price_min"
const val ATTR_TICKET_PRICE_MAX = "attr_ticket_price_max"
const val ATTR_TICKET_CURRENCY = "attr_ticket_currency"
const val ATTR_TICKETMASTER_EVENT_ID = "attr_ticketmaster_event_id"
const val ATTR_YELP_DETAIL_ENRICHED = "attr_yelp_detail_enriched"
const val DETAIL_YELP_ID = "yelp_id"

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
