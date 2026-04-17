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

    return copy(
        imageUrl = nextImageUrl,
        localImagePath = nextLocalImagePath,
        photoUrls = nextPhotoUrls,
        selectedOptionId = option.optionId,
        details = mergedDetails
    )
}
