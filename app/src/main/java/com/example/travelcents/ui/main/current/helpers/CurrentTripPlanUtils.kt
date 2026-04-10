package com.example.travelcents.ui.main.current

import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatMinutes
import com.example.travelcents.ui.modules.normalizeDate
import java.util.Locale

data class EventPalette(
    val container: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color
)

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
        "hotel" -> event.details["hotel_name"] ?: event.details["name"] ?: "Hotel Check-in"
        "restaurant", "dining", "food" -> event.details["restaurant_name"] ?: event.details["name"] ?: "Dinner Reservation"
        else -> event.details["activity_name"] ?: event.details["title"] ?: event.details["name"]
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
            event.details["total_price"]?.takeIf { it.isNotBlank() }?.let { "\$$it" }
        ).joinToString(" · ")
        "hotel" -> listOfNotNull(
            event.details["address"]?.takeIf { it.isNotBlank() },
            event.details["rating"]?.takeIf { it.isNotBlank() }?.let { "★$it" }
        ).joinToString(" · ")
        "restaurant", "dining", "food" -> listOfNotNull(
            event.details["cuisine"]?.takeIf { it.isNotBlank() },
            event.details["location"]?.takeIf { it.isNotBlank() },
            event.details["address"]?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        else -> listOfNotNull(
            event.details["description"]?.takeIf { it.isNotBlank() },
            event.details["location"]?.takeIf { it.isNotBlank() },
            event.details["address"]?.takeIf { it.isNotBlank() }
        ).firstOrNull().orEmpty()
    }.ifBlank { "Tap to edit details" }
}

fun editableLocation(event: TravelEvent): String {
    return event.details["location"]
        ?: event.details["address"]
        ?: event.details["hotel_name"]
        ?: event.details["restaurant_name"]
        ?: event.details["origin_airport"]
        ?: event.details["destination_airport"]
        ?: ""
}

fun editableNotes(event: TravelEvent): String {
    return event.details["description"]
        ?: event.details["cuisine"]
        ?: event.details["notes"]
        ?: listOf(event.details["airline"], event.details["flight_number"])
            .filter { !it.isNullOrBlank() }
            .joinToString(" ")
}

fun eventPalette(event: TravelEvent): EventPalette {
    return when ((event.details["colorKey"] ?: defaultColorKeyForType(event.type)).lowercase(Locale.US)) {
        "rose", "flight" -> EventPalette(
            container = androidx.compose.ui.graphics.Color(0xFF3B2637),
            accent = androidx.compose.ui.graphics.Color(0xFFFF677C)
        )
        "teal", "hotel" -> EventPalette(
            container = androidx.compose.ui.graphics.Color(0xFF193744),
            accent = androidx.compose.ui.graphics.Color(0xFF4CA7C5)
        )
        "olive", "restaurant" -> EventPalette(
            container = androidx.compose.ui.graphics.Color(0xFF343D2F),
            accent = androidx.compose.ui.graphics.Color(0xFF8A9365)
        )
        "cyan" -> EventPalette(
            container = androidx.compose.ui.graphics.Color(0xFF12353D),
            accent = androidx.compose.ui.graphics.Color(0xFF268C95)
        )
        "lavender" -> EventPalette(
            container = androidx.compose.ui.graphics.Color(0xFF302B45),
            accent = androidx.compose.ui.graphics.Color(0xFF7B6D9C)
        )
        else -> EventPalette(
            container = androidx.compose.ui.graphics.Color(0xFF2B233B),
            accent = androidx.compose.ui.graphics.Color(0xFF5A2A7B)
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
