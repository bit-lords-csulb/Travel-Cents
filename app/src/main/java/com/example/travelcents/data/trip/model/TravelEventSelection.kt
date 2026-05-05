package com.example.travelcents.data.trip.model

import java.util.Locale

fun TravelEvent.withSelectedOption(option: EventOption): TravelEvent {
    val mergedDetails = details.toMutableMap().apply {
        putAll(option.details)
        when (type.lowercase(Locale.US)) {
            "hotel" -> {
                val hotelName = option.detailValue(ATTR_HOTEL_NAME, "hotel_name", "name")
                if (!hotelName.isNullOrBlank()) {
                    put(ATTR_HOTEL_NAME, hotelName)
                    put("title", hotelName)
                }
            }
            "restaurant", "dining", "food" -> {
                val name = option.detailValue(ATTR_BUSINESS_NAME, "restaurant_name", "name")
                if (!name.isNullOrBlank()) {
                    put(ATTR_BUSINESS_NAME, name)
                    put("title", name)
                }
            }
            "activity" -> {
                val name = option.detailValue(ATTR_BUSINESS_NAME, "activity_name", "title", "name")
                if (!name.isNullOrBlank()) {
                    put(ATTR_BUSINESS_NAME, name)
                    put("title", name)
                }
            }
            "flight" -> option.details["title"]?.let { put("title", it) }
        }
    }

    val nextImageUrl = option.imageUrl.ifBlank { imageUrl }
    val nextLocalImagePath = option.localImagePath.ifBlank { localImagePath }
    val nextPhotoUrls = option.photoUrls.ifEmpty { photoUrls }
    val nextDate = option.detailValue(ATTR_OPTION_DATE)?.takeIf { it.isNotBlank() } ?: date
    val nextStartTime = option.detailValue(ATTR_OPTION_START_TIME)?.takeIf { it.isNotBlank() } ?: startTime
    val nextEndTime = option.detailValue(ATTR_OPTION_END_TIME)?.takeIf { it.isNotBlank() } ?: endTime
    val nextTimeZone = option.detailValue(ATTR_OPTION_TZ)?.takeIf { it.isNotBlank() } ?: tz
    val optionNativeBookable = option.detailValue("isNativeBookable", "nativeBookable")
    val optionBookingUrl = option.detailValue("booking_url", "bookingUrl", ATTR_BOOKING_URL)
        ?.takeIf { it.isNotBlank() }
    val selectedNativeBookable = optionNativeBookable ?: isNativeBookable
    val selectedBookingUrl = when {
        optionNativeBookable != null -> optionBookingUrl
        optionBookingUrl != null -> optionBookingUrl
        else -> bookingUrl
    }

    return copy(
        date = nextDate,
        startTime = nextStartTime,
        endTime = nextEndTime,
        tz = nextTimeZone,
        imageUrl = nextImageUrl,
        localImagePath = nextLocalImagePath,
        photoUrls = nextPhotoUrls,
        selectedOptionId = option.optionId,
        isNativeBookable = selectedNativeBookable,
        bookingUrl = selectedBookingUrl,
        details = mergedDetails
    )
}
