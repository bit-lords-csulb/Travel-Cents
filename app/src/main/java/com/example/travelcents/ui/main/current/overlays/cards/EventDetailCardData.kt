package com.example.travelcents.ui.main.current.overlays.cards

import android.net.Uri
import androidx.core.net.toUri
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_CHECK_IN_TIME
import com.example.travelcents.data.trip.model.ATTR_CHECK_OUT_TIME
import com.example.travelcents.data.trip.model.ATTR_CUISINE
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CITY
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.ATTR_HOURS_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_IS_CLOSED
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.ui.main.current.eventSubtitle
import com.example.travelcents.ui.main.current.eventTitle
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatDisplayTimeRange
import com.example.travelcents.ui.modules.formatTimeZoneLabel
import com.example.travelcents.ui.modules.formatTripDate
import com.example.travelcents.ui.modules.parseFlexibleTime
import com.example.travelcents.ui.modules.todayIsoDate
import java.time.Duration
import java.time.ZoneId
import java.util.Locale

internal fun eventOfficialUrl(event: TravelEvent): String? {
    return listOf(
        event.detailValue(ATTR_BOOKING_URL, "booking_url"),
        event.details["tickets_url"],
        event.detailValue(ATTR_MENU_URL, "yelp_menu_url"),
        event.detailValue(ATTR_YELP_URL),
        event.details["website"],
        event.details["url"]
    ).firstOrNull { !it.isNullOrBlank() }
}

internal fun eventMapsQuery(event: TravelEvent): String {
    if (event.type.equals("hotel", ignoreCase = true)) {
        val hotelName = event.detailValue(ATTR_HOTEL_NAME, "hotel_name")?.takeIf { it.isNotBlank() }
        if (hotelName != null) {
            val city = event.detailValue(ATTR_HOTEL_CITY)?.takeIf { it.isNotBlank() }
                ?: event.detailValue(ATTR_BUSINESS_ADDRESS, "address")?.takeIf { it.isNotBlank() }
                ?: event.details["location"]?.takeIf { it.isNotBlank() }
            return if (city != null) "$hotelName, $city" else hotelName
        }
    }
    val latitude = event.detailValue(ATTR_LATITUDE)?.toDoubleOrNull()
    val longitude = event.detailValue(ATTR_LONGITUDE)?.toDoubleOrNull()
    if (latitude != null && longitude != null) {
        return "%.6f,%.6f".format(Locale.US, latitude, longitude)
    }
    return listOf(
        event.detailValue(ATTR_BUSINESS_ADDRESS, "address"),
        event.details["location"],
        event.details["destination_airport"],
        event.detailValue(ATTR_HOTEL_NAME, "hotel_name"),
        event.detailValue(ATTR_BUSINESS_NAME, "restaurant_name", "activity_name"),
        event.details["title"],
        eventTitle(event)
    ).firstOrNull { !it.isNullOrBlank() } ?: eventTitle(event)
}

internal fun eventLocationLabel(event: TravelEvent): String {
    if (event.type.equals("hotel", ignoreCase = true)) {
        val latitude = event.detailValue(ATTR_LATITUDE)?.toDoubleOrNull()
        val longitude = event.detailValue(ATTR_LONGITUDE)?.toDoubleOrNull()
        val coordinateLabel = if (latitude != null && longitude != null) {
            "%.6f, %.6f".format(Locale.US, latitude, longitude)
        } else {
            null
        }
        return listOfNotNull(
            event.detailValue(ATTR_BUSINESS_ADDRESS, "address"),
            event.details["location"],
            coordinateLabel,
            event.detailValue("attr_hotel_name", "hotel_name")
        ).firstOrNull { it.isNotBlank() } ?: "Location information unavailable"
    }

    return listOf(
        event.detailValue(ATTR_BUSINESS_ADDRESS, "address"),
        event.details["location"],
        event.details["destination_airport"],
        event.details["origin_airport"],
        event.detailValue("attr_hotel_name", "hotel_name"),
        event.detailValue(ATTR_BUSINESS_NAME, "restaurant_name")
    ).firstOrNull { !it.isNullOrBlank() } ?: "Location information unavailable"
}

internal fun eventTimeSummary(event: TravelEvent): String {
    if (event.type.equals("flight", ignoreCase = true)) {
        return event.date.takeIf { it.isNotBlank() } ?: "Departure date TBD"
    }
    val datePrefix = event.date.takeIf { it.isNotBlank() }?.let { "$it  " }.orEmpty()
    return datePrefix + formatDisplayTimeRange(event.startTime, event.endTime)
}

internal fun eventDurationSummary(event: TravelEvent): String {
    val explicitDuration = formatDuration(
        event.details["flight_duration_min"]
            ?: event.details["total_duration"]
            ?: event.details["duration"]
    )
    if (explicitDuration != null) return explicitDuration

    val start = parseFlexibleTime(event.startTime)
    val end = parseFlexibleTime(event.endTime)
    return if (start != null && end != null && end.isAfter(start)) {
        val totalMinutes = Duration.between(start, end).toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        buildString {
            if (hours > 0) append("${hours}h")
            if (minutes > 0) {
                if (isNotEmpty()) append(' ')
                append("${minutes}m")
            }
        }.ifBlank { "Time set" }
    } else {
        "Starts ${formatDisplayTime(event.startTime)}"
    }
}

internal fun eventRatingLabel(event: TravelEvent, reviews: List<YelpReview>): String {
    return event.detailValue(ATTR_HOTEL_RATING, ATTR_AVERAGE_RATING, "overall_rating", "rating")
        ?: if (reviews.isNotEmpty()) "%.1f".format(Locale.US, reviews.map { it.rating }.average()) else "N/A"
}

internal fun eventReviewCountLabel(event: TravelEvent, reviews: List<YelpReview>): String {
    return event.detailValue(ATTR_REVIEW_COUNT, "review_count")
        ?: event.details["reviews"]
        ?: if (reviews.isNotEmpty()) reviews.size.toString() else ""
}

internal fun hoursSummary(event: TravelEvent): String {
    return event.detailValue(ATTR_HOURS_SUMMARY, "hours_summary", "hours")
        ?: "Hours are not listed for this event."
}

internal fun isClosedLabel(event: TravelEvent): String? {
    return when (event.detailValue(ATTR_IS_CLOSED, "is_closed")) {
        "true" -> "Currently marked closed"
        "false" -> "Currently marked open"
        else -> null
    }
}

internal fun hotelStayWindow(event: TravelEvent): String {
    val checkIn = event.detailValue(ATTR_CHECK_IN_TIME, "check_in_time", "check_in") ?: "Check-in TBD"
    val checkOut = event.detailValue(ATTR_CHECK_OUT_TIME, "check_out_time", "check_out") ?: "Check-out TBD"
    return "$checkIn to $checkOut"
}

internal fun hotelStayDateSummary(event: TravelEvent): String? {
    val checkInDate = event.details["check_in_date"]?.takeIf { it.isNotBlank() }
    val checkOutDate = event.details["check_out_date"]?.takeIf { it.isNotBlank() }
    return when {
        checkInDate != null && checkOutDate != null -> "$checkInDate to $checkOutDate"
        checkInDate != null -> "Check-in $checkInDate"
        checkOutDate != null -> "Check-out $checkOutDate"
        else -> null
    }
}

internal data class HotelStayMoment(
    val label: String,
    val date: String?,
    val time: String?,
    val timeZoneLabel: String?
)

internal fun hotelStayMoments(event: TravelEvent): List<HotelStayMoment> {
    val zoneLabel = hotelStayTimeZoneLabel(event)

    return listOf(
        HotelStayMoment(
            label = "Check-in",
            date = event.details["check_in_date"]?.takeIf { it.isNotBlank() }?.let(::formatTripDate),
            time = event.detailValue(ATTR_CHECK_IN_TIME, "check_in_time", "check_in")
                ?.takeIf { it.isNotBlank() }
                ?.let(::formatDisplayTime),
            timeZoneLabel = zoneLabel
        ),
        HotelStayMoment(
            label = "Check-out",
            date = event.details["check_out_date"]?.takeIf { it.isNotBlank() }?.let(::formatTripDate),
            time = event.detailValue(ATTR_CHECK_OUT_TIME, "check_out_time", "check_out")
                ?.takeIf { it.isNotBlank() }
                ?.let(::formatDisplayTime),
            timeZoneLabel = zoneLabel
        )
    ).filter { moment ->
        !moment.date.isNullOrBlank() || !moment.time.isNullOrBlank()
    }
}

private fun hotelStayTimeZoneLabel(event: TravelEvent): String? {
    return localTimeZoneLabel(
        context = "Hotel",
        event = event,
        referenceDates = listOf(
            event.details["check_in_date"],
            event.details["check_out_date"],
            event.date
        ),
        referenceTimes = listOf(
            event.detailValue(ATTR_CHECK_IN_TIME, "check_in_time", "check_in"),
            event.detailValue(ATTR_CHECK_OUT_TIME, "check_out_time", "check_out"),
            event.startTime
        )
    )
}

internal fun restaurantHoursTimeZoneLabel(
    event: TravelEvent,
    referenceDate: String? = null,
    referenceTime: String? = null
): String {
    return localTimeZoneLabel(
        context = "Restaurant",
        event = event,
        referenceDates = listOf(referenceDate, event.date),
        referenceTimes = listOf(referenceTime, event.startTime)
    )
}

private fun localTimeZoneLabel(
    context: String,
    event: TravelEvent,
    referenceDates: List<String?>,
    referenceTimes: List<String?>
): String {
    val unavailableLabel = "$context local time zone unavailable"
    val timeZoneId = event.tz.takeIf { it.isNotBlank() } ?: return unavailableLabel
    val zoneId = runCatching { ZoneId.of(timeZoneId) }.getOrNull() ?: return unavailableLabel
    val referenceDate = referenceDates.firstNotNullOfOrNull { value ->
        value?.takeIf { it.isNotBlank() }
    } ?: todayIsoDate(zoneId)
    val referenceTime = referenceTimes.firstNotNullOfOrNull { value ->
        value?.takeIf { it.isNotBlank() }
    } ?: "12:00 PM"

    return "$context local time: ${formatTimeZoneLabel(timeZoneId, referenceDate, referenceTime)}"
}

internal fun eventExperienceText(event: TravelEvent): String {
    return listOf(
        event.details["description"],
        event.details["notes"],
        eventSubtitle(event).takeIf { it != "Tap to edit details" },
        event.detailValue(ATTR_CATEGORIES, "categories"),
        event.detailValue(ATTR_CUISINE, "cuisine")
    ).firstOrNull { !it.isNullOrBlank() }
        ?: "Add notes or switch options to give this stop more context."
}

internal fun formatDuration(minStr: String?): String? {
    val min = minStr?.toIntOrNull() ?: return null
    if (min <= 0) return null
    val hours = min / 60
    val minutes = min % 60
    return if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
}

internal fun googleMapsSearchUrl(query: String): String =
    "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"

internal fun googleMapsDirectionsUrl(query: String): String =
    "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(query)}&travelmode=driving"

internal fun compactHostLabel(url: String): String {
    val host = url.toUri().host.orEmpty().removePrefix("www.")
    return host.ifBlank { "Open Link" }
}
