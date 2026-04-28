package com.example.travelcents.data.ai.chat

import com.example.travelcents.BuildConfig
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_TICKET_CURRENCY
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MAX
import com.example.travelcents.data.trip.model.ATTR_TICKET_PRICE_MIN
import com.example.travelcents.data.trip.model.ATTR_VENUE_NAME
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.data.trip.remote.TicketmasterRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class AiSingleEventCoordinator(
    private val searchEvents: suspend (String, String?, String?, String?, String?) -> List<TravelEvent> =
        { city, start, end, keyword, classification ->
            TicketmasterRepository.searchEventsForChat(
                location = city,
                startDate = start,
                endDate = end,
                keyword = keyword,
                classification = classification
            )
        },
    private val isSearchAvailable: () -> Boolean = { BuildConfig.TICKETMASTER_API_KEY.isNotBlank() },
    private val clock: Clock = Clock.systemDefaultZone()
) {

    suspend fun resolve(
        toolCall: AiToolCall.SearchEvents,
        userMessage: String,
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): AiSingleEventResolution? {
        val searchContext = resolveSearchContext(
            toolCall = toolCall,
            userMessage = userMessage,
            intakeProfile = intakeProfile,
            profile = profile
        ) ?: return null
        return AiSingleEventResolution(
            groundingContext = searchContext.toGroundingContext(),
            suggestion = searchContext.events.firstOrNull()?.toSuggestion()
        )
    }

    suspend fun suggest(
        toolCall: AiToolCall.SearchEvents,
        userMessage: String,
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): AiSingleEventSuggestion? {
        return resolve(toolCall, userMessage, intakeProfile, profile)?.suggestion
    }

    suspend fun buildGroundingContext(
        toolCall: AiToolCall.SearchEvents,
        userMessage: String,
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): String? {
        return resolve(toolCall, userMessage, intakeProfile, profile)?.groundingContext
    }

    private suspend fun resolveSearchContext(
        toolCall: AiToolCall.SearchEvents,
        userMessage: String,
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): EventSearchContext? {
        if (!isSearchAvailable()) return null
        val destination = toolCall.city.trim()
            .ifBlank { intakeProfile.destination.ifBlank { profile.destination }.trim() }
        if (destination.isBlank()) return null
        val signal = SingleEventSignal(
            classification = toolCall.classification?.trim()?.ifBlank { null },
            keyword = toolCall.keyword?.trim()?.ifBlank { null },
            timeHint = resolveTimeHint(userMessage)
        )

        val searchWindow = resolveWindow(
            timeHint = signal.timeHint,
            rawDateWindow = intakeProfile.dateWindow.ifBlank { profile.dateWindow },
            durationDays = intakeProfile.durationDays
        ) ?: return null

        val events = runCatching {
            searchEvents(
                destination,
                searchWindow.startDate,
                searchWindow.endDate,
                signal.keyword,
                signal.classification
            )
        }.getOrElse { return null }

        return EventSearchContext(
            signal = signal,
            destination = destination,
            searchWindow = searchWindow,
            events = events.take(MAX_PROMPT_RESULTS)
        )
    }

    private fun TravelEvent.toSuggestion(): AiSingleEventSuggestion {
        val name = displayName() ?: details[ATTR_BUSINESS_NAME] ?: details["activity_name"] ?: "Event"
        val venue = detailValue(ATTR_VENUE_NAME, "location").orEmpty()
        val address = detailValue(ATTR_BUSINESS_ADDRESS, "address").orEmpty()
        val city = address.substringBefore(",").trim()
            .ifBlank { address.trim() }
        val category = detailValue(ATTR_CATEGORIES, "categories").orEmpty()
        val booking = detailValue(ATTR_BOOKING_URL, "booking_url")?.takeIf { it.isNotBlank() }
        val priceLine = buildPriceLine(
            min = detailValue(ATTR_TICKET_PRICE_MIN),
            max = detailValue(ATTR_TICKET_PRICE_MAX),
            currency = detailValue(ATTR_TICKET_CURRENCY)
        )
        val dateLine = formatEventDateTime(date, startTime)

        return AiSingleEventSuggestion(
            headline = name,
            venue = venue,
            cityLine = city,
            dateLine = dateLine,
            priceLine = priceLine,
            category = category.ifBlank { "Event" },
            imageUrl = imageUrl.takeIf { it.isNotBlank() },
            bookingUrl = booking,
            event = this
        )
    }

    private fun EventSearchContext.toGroundingContext(): String {
        return buildString {
            appendLine("Ticketmaster live event grounding for this turn:")
            appendLine("The user is asking about live ticketed events. Answer directly using only the real Ticketmaster results below.")
            appendLine("Do not invent concerts, shows, games, venues, dates, or prices.")
            appendLine("Recommend at most 3 matches and include the Ticketmaster booking URL when you mention one.")
            appendLine("Destination: $destination")
            appendLine("Date window: ${searchWindow.startDate} to ${searchWindow.endDate}")
            signal.classification?.let { classification ->
                appendLine("Requested event type: $classification")
            }
            signal.keyword?.let { keyword ->
                appendLine("Requested keyword: $keyword")
            }
            if (events.isEmpty()) {
                append("Ticketmaster returned no matching events for this destination and date window.")
            } else {
                appendLine("Ticketmaster results:")
                events.forEachIndexed { index, event ->
                    appendLine(formatGroundedEvent(index, event))
                }
            }
        }.trim()
    }

    private fun formatGroundedEvent(index: Int, event: TravelEvent): String {
        val headline = event.displayName() ?: event.detailValue(ATTR_BUSINESS_NAME, "activity_name") ?: "Event"
        val dateLine = formatEventDateTime(event.date, event.startTime)
        val venue = event.detailValue(ATTR_VENUE_NAME, "location", ATTR_BUSINESS_ADDRESS, "address")
            ?.takeIf { it.isNotBlank() }
        val priceLine = buildPriceLine(
            min = event.detailValue(ATTR_TICKET_PRICE_MIN),
            max = event.detailValue(ATTR_TICKET_PRICE_MAX),
            currency = event.detailValue(ATTR_TICKET_CURRENCY)
        )
        val bookingUrl = event.detailValue(ATTR_BOOKING_URL, "booking_url")

        val parts = buildList {
            add(headline)
            if (dateLine.isNotBlank()) add(dateLine)
            if (!venue.isNullOrBlank()) add(venue)
            if (priceLine.isNotBlank()) add(priceLine)
            if (!bookingUrl.isNullOrBlank()) add(bookingUrl)
        }
        return "${index + 1}. ${parts.joinToString(separator = " | ")}"
    }

    private fun buildPriceLine(min: String?, max: String?, currency: String?): String {
        val minValue = min?.toDoubleOrNull()?.toInt()
        val maxValue = max?.toDoubleOrNull()?.toInt()
        val currencySymbol = when (currency?.uppercase(Locale.US)) {
            null, "", "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "$currency "
        }
        return when {
            minValue != null && maxValue != null && minValue != maxValue ->
                "$currencySymbol$minValue - $currencySymbol$maxValue"
            minValue != null -> "From $currencySymbol$minValue"
            maxValue != null -> "Up to $currencySymbol$maxValue"
            else -> ""
        }
    }

    private fun formatEventDateTime(date: String, time: String): String {
        val parsedDate = runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        val datePart = parsedDate?.format(DATE_FORMATTER) ?: date
        val parsedTime = runCatching { LocalTime.parse(time, STORAGE_TIME_FORMATTER) }.getOrNull()
        val timePart = parsedTime?.format(DISPLAY_TIME_FORMATTER) ?: time.takeIf { it.isNotBlank() }
        return if (timePart != null) "$datePart | $timePart" else datePart
    }

    private fun resolveWindow(
        timeHint: TimeHint,
        rawDateWindow: String,
        durationDays: Int?
    ): SearchWindow? {
        val today = LocalDate.now(clock)
        return when (timeHint) {
            TimeHint.TONIGHT, TimeHint.TODAY -> SearchWindow(
                startDate = today.isoString(),
                endDate = today.isoString(),
                label = "today"
            )

            TimeHint.TOMORROW -> SearchWindow(
                startDate = today.plusDays(1).isoString(),
                endDate = today.plusDays(1).isoString(),
                label = "tomorrow"
            )

            TimeHint.THIS_WEEKEND -> {
                val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                SearchWindow(
                    startDate = saturday.isoString(),
                    endDate = saturday.plusDays(1).isoString(),
                    label = "this weekend"
                )
            }

            TimeHint.NONE -> parseDateWindow(
                rawDateWindow = rawDateWindow,
                durationDays = durationDays,
                today = today
            )
        }
    }

    private fun LocalDate.isoString(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun parseDateWindow(
        rawDateWindow: String,
        durationDays: Int?,
        today: LocalDate
    ): SearchWindow? {
        val normalized = rawDateWindow.trim()
        if (normalized.isBlank()) return null

        parseIsoDateWindow(normalized, durationDays)?.let { return it }
        parseMonthDayRange(normalized, today)?.let { return it }
        parseMonthWindow(normalized, today)?.let { return it }
        parseNamedWindow(normalized, today)?.let { return it }
        return null
    }

    private fun parseIsoDateWindow(rawDateWindow: String, durationDays: Int?): SearchWindow? {
        val dates = ISO_DATE_REGEX.findAll(rawDateWindow)
            .map { match -> match.value }
            .toList()
        if (dates.isEmpty()) return null

        val start = parseIsoDate(dates.first()) ?: return null
        val end = when {
            dates.size >= 2 -> parseIsoDate(dates[1]) ?: start
            durationDays != null && durationDays > 1 -> start.plusDays((durationDays - 1).toLong())
            else -> start
        }

        return SearchWindow(
            startDate = start.isoString(),
            endDate = end.isoString(),
            label = rawDateWindow.trim()
        )
    }

    private fun parseMonthDayRange(rawDateWindow: String, today: LocalDate): SearchWindow? {
        val match = MONTH_DAY_RANGE_REGEX.find(rawDateWindow) ?: return null
        val month = monthNumber(match.groupValues[1]) ?: return null
        val startDay = match.groupValues[2].toIntOrNull() ?: return null
        val endDay = match.groupValues[3].toIntOrNull() ?: startDay
        val year = match.groupValues[4].toIntOrNull() ?: inferredYear(month, today)
        val start = buildLocalDate(year, month, startDay) ?: return null
        val end = buildLocalDate(year, month, endDay) ?: return null

        return SearchWindow(
            startDate = start.isoString(),
            endDate = end.isoString(),
            label = rawDateWindow.trim()
        )
    }

    private fun parseMonthWindow(rawDateWindow: String, today: LocalDate): SearchWindow? {
        val monthYearMatch = MONTH_YEAR_REGEX.find(rawDateWindow)
        if (monthYearMatch != null) {
            val month = monthNumber(monthYearMatch.groupValues[1]) ?: return null
            val year = monthYearMatch.groupValues[2].toIntOrNull() ?: return null
            return monthWindow(month = month, year = year, label = rawDateWindow.trim())
        }

        val monthOnlyMatch = MONTH_ONLY_REGEX.find(rawDateWindow) ?: return null
        val month = monthNumber(monthOnlyMatch.groupValues[1]) ?: return null
        val year = inferredYear(month, today)
        return monthWindow(month = month, year = year, label = rawDateWindow.trim())
    }

    private fun parseNamedWindow(rawDateWindow: String, today: LocalDate): SearchWindow? {
        val normalized = rawDateWindow.lowercase(Locale.US)
        return when {
            "next month" in normalized -> {
                val firstDay = today.withDayOfMonth(1).plusMonths(1)
                SearchWindow(
                    startDate = firstDay.isoString(),
                    endDate = firstDay.withDayOfMonth(firstDay.lengthOfMonth()).isoString(),
                    label = rawDateWindow.trim()
                )
            }

            "weekend" in normalized -> {
                val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                SearchWindow(
                    startDate = saturday.isoString(),
                    endDate = saturday.plusDays(1).isoString(),
                    label = rawDateWindow.trim()
                )
            }

            "spring" in normalized -> seasonWindow("spring", rawDateWindow, today)
            "summer" in normalized -> seasonWindow("summer", rawDateWindow, today)
            "fall" in normalized || "autumn" in normalized -> seasonWindow("fall", rawDateWindow, today)
            "winter" in normalized -> seasonWindow("winter", rawDateWindow, today)
            else -> null
        }
    }

    private fun monthWindow(month: Int, year: Int, label: String): SearchWindow {
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        return SearchWindow(
            startDate = start.isoString(),
            endDate = end.isoString(),
            label = label
        )
    }

    private fun seasonWindow(
        season: String,
        rawDateWindow: String,
        today: LocalDate
    ): SearchWindow? {
        val explicitYear = YEAR_REGEX.find(rawDateWindow)?.value?.toIntOrNull()
        val year = explicitYear ?: inferredSeasonYear(season, today)
        return when (season) {
            "spring" -> SearchWindow(
                startDate = LocalDate.of(year, 3, 1).isoString(),
                endDate = LocalDate.of(year, 5, 31).isoString(),
                label = rawDateWindow.trim()
            )

            "summer" -> SearchWindow(
                startDate = LocalDate.of(year, 6, 1).isoString(),
                endDate = LocalDate.of(year, 8, 31).isoString(),
                label = rawDateWindow.trim()
            )

            "fall" -> SearchWindow(
                startDate = LocalDate.of(year, 9, 1).isoString(),
                endDate = LocalDate.of(year, 11, 30).isoString(),
                label = rawDateWindow.trim()
            )

            "winter" -> SearchWindow(
                startDate = LocalDate.of(year, 12, 1).isoString(),
                endDate = LocalDate.of(year + 1, 2, 28).isoString(),
                label = rawDateWindow.trim()
            )

            else -> null
        }
    }

    private fun inferredYear(month: Int, today: LocalDate): Int {
        return if (month < today.monthValue) today.year + 1 else today.year
    }

    private fun inferredSeasonYear(season: String, today: LocalDate): Int {
        val currentSeason = seasonFor(today)
        return if (season == currentSeason) {
            today.year
        } else {
            when {
                seasonOrder(season) > seasonOrder(currentSeason) -> today.year
                season == "winter" && currentSeason == "fall" -> today.year
                else -> today.year + 1
            }
        }
    }

    private fun seasonFor(date: LocalDate): String {
        return when (date.monthValue) {
            in 3..5 -> "spring"
            in 6..8 -> "summer"
            in 9..11 -> "fall"
            else -> "winter"
        }
    }

    private fun seasonOrder(season: String): Int {
        return when (season) {
            "spring" -> 1
            "summer" -> 2
            "fall" -> 3
            else -> 4
        }
    }

    private fun parseIsoDate(rawDate: String): LocalDate? {
        return try {
            LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun buildLocalDate(year: Int, month: Int, day: Int): LocalDate? {
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun monthNumber(rawMonth: String): Int? {
        return MONTH_MAP[rawMonth.lowercase(Locale.US)]
    }

    private fun resolveTimeHint(userMessage: String): TimeHint {
        val normalized = userMessage.lowercase(Locale.US)
        return when {
            "tonight" in normalized -> TimeHint.TONIGHT
            "today" in normalized -> TimeHint.TODAY
            "tomorrow" in normalized -> TimeHint.TOMORROW
            "this weekend" in normalized || "weekend" in normalized -> TimeHint.THIS_WEEKEND
            else -> TimeHint.NONE
        }
    }

    private data class EventSearchContext(
        val signal: SingleEventSignal,
        val destination: String,
        val searchWindow: SearchWindow,
        val events: List<TravelEvent>
    )

    private data class SearchWindow(
        val startDate: String,
        val endDate: String,
        val label: String
    )

    private data class SingleEventSignal(
        val classification: String?,
        val keyword: String?,
        val timeHint: TimeHint
    )

    private enum class TimeHint { TONIGHT, TODAY, TOMORROW, THIS_WEEKEND, NONE }

    private companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
        private val STORAGE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        private val DISPLAY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        private const val MAX_PROMPT_RESULTS = 5

        private val MONTH_MAP = mapOf(
            "jan" to 1,
            "january" to 1,
            "feb" to 2,
            "february" to 2,
            "mar" to 3,
            "march" to 3,
            "apr" to 4,
            "april" to 4,
            "may" to 5,
            "jun" to 6,
            "june" to 6,
            "jul" to 7,
            "july" to 7,
            "aug" to 8,
            "august" to 8,
            "sep" to 9,
            "sept" to 9,
            "september" to 9,
            "oct" to 10,
            "october" to 10,
            "nov" to 11,
            "november" to 11,
            "dec" to 12,
            "december" to 12
        )
        private val MONTH_PATTERN = MONTH_MAP.keys
            .sortedByDescending(String::length)
            .joinToString(separator = "|") { token -> Regex.escape(token) }
        private val ISO_DATE_REGEX = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
        private val YEAR_REGEX = Regex("""\b20\d{2}\b""")
        private val MONTH_DAY_RANGE_REGEX = Regex(
            """\b($MONTH_PATTERN)\s+(\d{1,2})(?:\s*(?:-|–|to)\s*(\d{1,2}))?(?:,?\s*(\d{4}))?\b""",
            RegexOption.IGNORE_CASE
        )
        private val MONTH_YEAR_REGEX = Regex(
            """\b($MONTH_PATTERN)\s+(\d{4})\b""",
            RegexOption.IGNORE_CASE
        )
        private val MONTH_ONLY_REGEX = Regex(
            """\b($MONTH_PATTERN)\b""",
            RegexOption.IGNORE_CASE
        )
    }
}

data class AiSingleEventResolution(
    val groundingContext: String,
    val suggestion: AiSingleEventSuggestion?
)
