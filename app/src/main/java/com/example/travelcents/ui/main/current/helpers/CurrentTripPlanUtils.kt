package com.example.travelcents.ui.main.current

import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CUISINE
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.data.trip.model.firstNonBlank
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatMinutes
import com.example.travelcents.ui.modules.normalizeDate
import java.util.Locale

data class EventPalette(
    val container: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color
)

private fun tintedEventContainer(accent: Color): Color {
    return accent.copy(alpha = 0.18f).compositeOver(TripWizardColors.SurfaceBright)
}

private fun formatFlightDuration(minStr: String?): String? {
    val min = minStr?.toIntOrNull() ?: return null
    if (min <= 0) return null
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

fun TravelEvent.toEditablePlan(): EditablePlan {
    return EditablePlan(
        eventId = eventId,
        type = type,
        title = eventTitle(this),
        date = normalizeDate(date),
        startTime = formatDisplayTime(startTime),
        endTime = endTime.takeIf { it.isNotBlank() }?.let(::formatDisplayTime).orEmpty(),
        timeZoneId = tz.ifBlank { defaultPlanTimeZoneId() },
        location = editableLocation(this),
        notes = editableNotes(this),
        colorKey = details["colorKey"] ?: defaultColorKeyForType(type),
        imageUrl = imageUrl,
        existingDetails = details
    )
}

fun newEditablePlan(
    date: String,
    startMinutes: Int
): EditablePlan {
    val startTime = formatMinutes(startMinutes)
    return EditablePlan(
        date = normalizeDate(date),
        startTime = startTime,
        endTime = com.example.travelcents.ui.modules.plusMinutes(startTime, 60),
        timeZoneId = defaultPlanTimeZoneId(),
        colorKey = defaultColorKeyForType("activity")
    )
}

fun eventTitle(event: TravelEvent): String {
    return when (event.type.lowercase(Locale.US)) {
        "flight" -> listOfNotNull(
            event.details["title"]?.takeIf { it.isNotBlank() },
            event.details["destination_airport"]?.takeIf { it.isNotBlank() }?.let { destination ->
                when (event.details["trip_segment"]?.lowercase(Locale.US)) {
                    "return" -> "Return to $destination"
                    else -> "Flight to $destination"
                }
            },
            listOfNotNull(
                event.details["airline"]?.takeIf { it.isNotBlank() },
                event.details["flight_number"]?.takeIf { it.isNotBlank() }
            ).joinToString(" ").takeIf { it.isNotBlank() }
        ).firstOrNull()
        "hotel" -> event.displayName() ?: "Hotel Check-in"
        "restaurant", "dining", "food" -> event.displayName() ?: "Dinner Reservation"
        else -> event.displayName()
    } ?: event.type.replaceFirstChar { it.uppercase(Locale.US) }
}

fun eventSubtitle(event: TravelEvent): String {
    return when (event.type.lowercase(Locale.US)) {
        "flight" -> listOfNotNull(
            listOfNotNull(
                event.details["airline"]?.takeIf { it.isNotBlank() },
                event.details["flight_number"]?.takeIf { it.isNotBlank() }
            ).joinToString(" ").takeIf { it.isNotBlank() },
            listOfNotNull(
                event.details["origin_airport"]?.takeIf { it.isNotBlank() },
                event.details["destination_airport"]?.takeIf { it.isNotBlank() }
            ).takeIf { it.isNotEmpty() }?.joinToString(" to "),
            formatFlightDuration(event.details["flight_duration_min"] ?: event.details["total_duration"]),
            event.details["total_price"]?.takeIf { it.isNotBlank() }?.let { "\$$it" }
        ).joinToString(" · ")
        "hotel" -> listOfNotNull(
            event.details.firstNonBlank(ATTR_BUSINESS_ADDRESS, "address")?.takeIf { it.isNotBlank() },
            event.details.firstNonBlank(ATTR_HOTEL_RATING, ATTR_AVERAGE_RATING, "rating")?.takeIf { it.isNotBlank() }?.let { "★$it" }
        ).joinToString(" · ")
        "restaurant", "dining", "food" -> listOfNotNull(
            event.details.firstNonBlank(ATTR_CUISINE, "cuisine")?.takeIf { it.isNotBlank() },
            event.details["location"]?.takeIf { it.isNotBlank() },
            event.details.firstNonBlank(ATTR_BUSINESS_ADDRESS, "address")?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        else -> listOfNotNull(
            event.details["description"]?.takeIf { it.isNotBlank() },
            event.details["location"]?.takeIf { it.isNotBlank() },
            event.details.firstNonBlank(ATTR_BUSINESS_ADDRESS, "address")?.takeIf { it.isNotBlank() }
        ).firstOrNull().orEmpty()
    }.ifBlank { "Tap to edit details" }
}

fun editableLocation(event: TravelEvent): String {
    return event.details["location"]
        ?: event.details.firstNonBlank(ATTR_BUSINESS_ADDRESS, "address")
        ?: event.details.firstNonBlank(ATTR_HOTEL_NAME, "hotel_name")
        ?: event.details.firstNonBlank(ATTR_BUSINESS_NAME, "restaurant_name", "activity_name")
        ?: event.details["origin_airport"]
        ?: event.details["destination_airport"]
        ?: ""
}

fun editableNotes(event: TravelEvent): String {
    return event.details["description"]
        ?: event.details.firstNonBlank(ATTR_CUISINE, "cuisine")
        ?: event.details["notes"]
        ?: listOf(event.details["airline"], event.details["flight_number"])
            .filter { !it.isNullOrBlank() }
            .joinToString(" ")
}

fun eventPalette(event: TravelEvent): EventPalette {
    return when ((event.details["colorKey"] ?: defaultColorKeyForType(event.type)).lowercase(Locale.US)) {
        "rose", "flight" -> EventPalette(
            container = tintedEventContainer(Color(0xFFFF677C)),
            accent = Color(0xFFFF677C)
        )
        "teal", "hotel" -> EventPalette(
            container = tintedEventContainer(Color(0xFF4CA7C5)),
            accent = Color(0xFF4CA7C5)
        )
        "olive", "restaurant" -> EventPalette(
            container = tintedEventContainer(Color(0xFF8A9365)),
            accent = Color(0xFF8A9365)
        )
        "cyan" -> EventPalette(
            container = tintedEventContainer(Color(0xFF268C95)),
            accent = Color(0xFF268C95)
        )
        "lavender" -> EventPalette(
            container = tintedEventContainer(Color(0xFF7B6D9C)),
            accent = Color(0xFF7B6D9C)
        )
        else -> EventPalette(
            container = tintedEventContainer(Color(0xFF5A2A7B)),
            accent = Color(0xFF5A2A7B)
        )
    }
}

fun defaultColorKeyForType(type: String): String {
    return when (type.lowercase(Locale.US)) {
        "flight" -> "rose"
        "hotel" -> "teal"
        "restaurant" -> "olive"
        else -> "plum"
    }
}

