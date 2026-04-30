package com.example.travelcents.data.trip.remote

import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

object OpenTableResolver {

    fun buildSearchUrl(
        restaurantName: String,
        locationHint: String?,
        date: String?,
        time: String?,
        partySize: Int
    ): String? {
        val normalizedName = restaurantName.trim().takeIf { it.isNotBlank() } ?: return null
        val term = listOfNotNull(
            normalizedName,
            locationHint?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        val params = buildList {
            add("covers=${partySize.coerceAtLeast(1)}")
            reservationDateTime(date, time)?.let { add("dateTime=${Uri.encode(it)}") }
            add("term=${Uri.encode(term)}")
        }

        return "https://www.opentable.com/s?${params.joinToString("&")}"
    }
}

object ResyResolver {

    fun buildSearchUrl(
        restaurantName: String,
        citySlug: String?,
        date: String?,
        partySize: Int
    ): String? {
        val normalizedName = restaurantName.trim().takeIf { it.isNotBlank() } ?: return null
        val normalizedCitySlug = citySlug?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val params = buildList {
            normalizeDate(date)?.let { add("date=$it") }
            add("seats=${partySize.coerceAtLeast(1)}")
            add("query=${Uri.encode(normalizedName)}")
        }

        return "https://resy.com/cities/$normalizedCitySlug/search?${params.joinToString("&")}"
    }
}

object GoogleReserveResolver {

    fun buildSearchUrl(query: String): String? {
        val normalizedQuery = query.trim().takeIf { it.isNotBlank() } ?: return null
        return "https://www.google.com/maps/reserve/v/?q=${Uri.encode(normalizedQuery)}"
    }
}

internal fun reservationCitySlug(locationHint: String?): String? {
    val parts = locationHint
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    if (parts.isEmpty()) return null

    val city = when {
        parts.size >= 3 -> parts[parts.size - 2]
        parts.size == 2 -> parts[0]
        else -> parts.last()
    }.takeIf { it.isNotBlank() } ?: return null

    val region = parts.lastOrNull()
        ?.takeIf { it != city }
        ?.substringBefore(' ')
        ?.takeIf { token ->
            token.isNotBlank() && token.length <= 3 && token.any(Char::isLetter)
        }

    val cityPart = slugify(city)
    val regionPart = region?.let(::slugify)?.takeIf { it.isNotBlank() }
    return listOfNotNull(
        cityPart.takeIf { it.isNotBlank() },
        regionPart
    ).joinToString("-").takeIf { it.isNotBlank() }
}

private fun reservationDateTime(date: String?, time: String?): String? {
    val normalizedDate = normalizeDate(date) ?: return null
    val normalizedTime = normalizeTime(time) ?: return null
    return "${normalizedDate}T${normalizedTime}"
}

private fun normalizeDate(date: String?): String? {
    val rawDate = date?.trim().takeIf { !it.isNullOrBlank() } ?: return null
    return runCatching {
        LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()
}

private fun normalizeTime(time: String?): String? {
    val parsed = parseFlexibleReservationTime(time) ?: return null
    return parsed.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
}

private val reservationTimeFormatters: List<DateTimeFormatter> = listOf(
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.HOUR_OF_DAY)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .toFormatter(Locale.US),
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalEnd()
        .appendLiteral(' ')
        .appendText(ChronoField.AMPM_OF_DAY)
        .toFormatter(Locale.US)
)

private fun parseFlexibleReservationTime(time: String?): LocalTime? {
    val rawTime = time?.trim().takeIf { !it.isNullOrBlank() } ?: return null
    val normalized = rawTime
        .replace(".", "")
        .replace(Regex("(?i)(\\d)(am|pm)$"), "$1 $2")
        .uppercase(Locale.US)

    return reservationTimeFormatters.firstNotNullOfOrNull { formatter ->
        runCatching { LocalTime.parse(normalized, formatter) }.getOrNull()
    }
}

private fun slugify(value: String): String {
    return value.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
