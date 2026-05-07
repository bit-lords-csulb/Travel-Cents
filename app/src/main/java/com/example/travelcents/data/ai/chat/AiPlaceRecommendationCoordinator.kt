package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.model.ATTR_AMENITIES
import com.example.travelcents.data.trip.model.ATTR_GROUP_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CLASS
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.data.trip.remote.SerpRepository
import com.example.travelcents.data.trip.remote.YelpCategoryTranslator
import com.example.travelcents.data.trip.remote.YelpRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class AiPlaceRecommendationCoordinator(
    private val curatedTripCatalog: AiCuratedTripCatalog = AiCuratedTripCatalog(),
    private val restaurantProvider: suspend (String, Int) -> List<YelpBusiness> = { loc, n -> YelpRepository.fetchRestaurantPool(loc, n) },
    private val activityProvider: suspend (String, Int) -> List<YelpBusiness> = { loc, n -> YelpRepository.fetchActivityPool(loc, n) },
    private val hotelProvider: suspend (TravelRequest, Itinerary) -> List<TravelEvent> = SerpRepository::searchHotels
) {
    suspend fun recommendRowForToolCall(
        toolCall: AiToolCall,
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): AiPlaceRecommendationRow? {
        return when (toolCall) {
            is AiToolCall.SearchRestaurants -> buildRestaurantRow(
                intakeProfile = intakeProfile,
                profile = profile,
                destination = toolCall.city,
                cuisines = toolCall.cuisines
            )

            is AiToolCall.SearchActivities -> buildActivityRow(
                intakeProfile = intakeProfile,
                profile = profile,
                destination = toolCall.city,
                categories = toolCall.categories
            )

            is AiToolCall.SearchHotels -> buildHotelRow(
                intakeProfile = intakeProfile,
                profile = profile,
                destination = toolCall.city,
                checkIn = toolCall.checkIn,
                checkOut = toolCall.checkOut,
                requireExplicitDates = true
            )

            is AiToolCall.SearchEvents -> null
        }
    }

    suspend fun recommendRow(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        llmRecommendations: List<AiTripIntakePlaceRecommendation> = emptyList()
    ): AiPlaceRecommendationRow? {
        if (!shouldRecommendPlaces(intakeProfile, profile)) return null

        val destination = intakeProfile.destination.ifBlank { profile.destination }.trim()
        if (destination.isBlank()) return null

        val curatedFallback = curatedTripCatalog.recommendPlaceRecommendations(
            profile = profile.copy(destination = destination)
        )
        val providerRow = when (selectRowType(intakeProfile, profile)) {
            AiPlaceRecommendationRowType.HOTELS -> buildHotelRow(intakeProfile, profile, destination)
            AiPlaceRecommendationRowType.RESTAURANTS -> buildRestaurantRow(intakeProfile, profile, destination)
            AiPlaceRecommendationRowType.ACTIVITIES -> buildActivityRow(intakeProfile, profile, destination)
            AiPlaceRecommendationRowType.NEIGHBORHOODS,
            AiPlaceRecommendationRowType.DAY_TRIPS,
            AiPlaceRecommendationRowType.GENERAL -> null
        }

        val mergedProviderRow = mergeWithFallback(providerRow, curatedFallback)
        if (mergedProviderRow != null) return mergedProviderRow

        return AiRecommendationMapper.placeRowFromIntake(llmRecommendations)
            ?: curatedFallback?.copy(
                actionLabels = DEFAULT_ACTION_LABELS,
                actionsEnabled = false,
                rowType = inferCuratedRowType(curatedFallback)
            )
    }

    fun shouldRecommendPlaces(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): Boolean {
        val destination = intakeProfile.destination.ifBlank { profile.destination }
        if (destination.isBlank()) return false

        val destinationConfidence = intakeProfile.confidence["destination"]
            ?: if (destination.isNotBlank()) 0.9 else 0.0
        if (destinationConfidence < DESTINATION_CONFIDENCE_THRESHOLD) return false

        return intakeProfile.interests.isNotEmpty() ||
            intakeProfile.cuisinePreferences.isNotEmpty() ||
            intakeProfile.destinationStyle.isNotEmpty() ||
            intakeProfile.mustHaves.isNotEmpty() ||
            intakeProfile.notes.isNotEmpty() ||
            intakeProfile.budgetLevel != AiBudgetLevel.UNKNOWN ||
            intakeProfile.pace != AiTripPacePreference.UNKNOWN ||
            profile.interests.isNotEmpty() ||
            profile.cuisinePreferences.isNotEmpty() ||
            profile.notes.isNotEmpty()
    }

    private suspend fun buildRestaurantRow(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        destination: String,
        cuisines: List<String> = emptyList()
    ): AiPlaceRecommendationRow? {
        val allTerms = (cuisines + intakeProfile.cuisineSubPreferences).distinct()
        val yelpCategories = YelpCategoryTranslator.cuisinesToCategories(allTerms)
        val dietaryTerm = YelpCategoryTranslator.toDietaryTerm(allTerms)

        val businesses = YelpRepository.fetchRestaurantPool(
            location = destination,
            targetCount = PROVIDER_RESULT_LIMIT,
            yelpCategories = yelpCategories.takeIf { it.isNotBlank() },
            term = dietaryTerm
        ).let { candidates -> rankBusinessesByTerms(candidates, allTerms) }
            .distinctBy(YelpBusiness::id)
            .take(PROVIDER_RESULT_LIMIT)
        if (businesses.size < 2) return null

        return AiPlaceRecommendationRow(
            title = "Food spots in ${shortDestination(destination)}",
            subtitle = allTerms.takeIf(List<String>::isNotEmpty)
                ?.joinToString(separator = ", ")
                ?.let { requested -> "Restaurant ideas pulled from live local results for $requested." }
                ?: "Restaurant ideas pulled from live local results.",
            recommendations = businesses.map { business ->
                AiPlaceRecommendation(
                    name = business.name,
                    category = "Restaurant",
                    area = business.location?.displayAddress?.firstOrNull()
                        ?: business.location?.city.orEmpty(),
                    summary = buildRestaurantSummary(business),
                    matchReason = buildRestaurantMatchReason(intakeProfile, profile, business),
                    imageUrl = business.imageUrl.takeIf { url -> url.isNotBlank() }
                )
            },
            rowType = AiPlaceRecommendationRowType.RESTAURANTS,
            actionLabels = DEFAULT_ACTION_LABELS,
            actionsEnabled = false
        )
    }

    private suspend fun buildActivityRow(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        destination: String,
        categories: List<String> = emptyList()
    ): AiPlaceRecommendationRow? {
        val allTerms = (categories + intakeProfile.activitySubCategories).distinct()
        val yelpCategories = YelpCategoryTranslator.activitiesToCategories(allTerms)

        val businesses = YelpRepository.fetchActivityPool(
            location = destination,
            targetCount = PROVIDER_RESULT_LIMIT,
            yelpCategories = yelpCategories.takeIf { it.isNotBlank() }
        ).let { candidates -> rankBusinessesByTerms(candidates, allTerms) }
            .distinctBy(YelpBusiness::id)
            .take(PROVIDER_RESULT_LIMIT)
        if (businesses.size < 2) return null

        return AiPlaceRecommendationRow(
            title = "Activity ideas in ${shortDestination(destination)}",
            subtitle = allTerms.takeIf(List<String>::isNotEmpty)
                ?.joinToString(separator = ", ")
                ?.let { requested -> "Live activity picks for $requested." }
                ?: "Live activity picks that match the current trip direction.",
            recommendations = businesses.map { business ->
                AiPlaceRecommendation(
                    name = business.name,
                    category = "Activity",
                    area = business.location?.displayAddress?.firstOrNull()
                        ?: business.location?.city.orEmpty(),
                    summary = buildActivitySummary(business),
                    matchReason = buildActivityMatchReason(intakeProfile, profile, business),
                    imageUrl = business.imageUrl.takeIf { url -> url.isNotBlank() }
                )
            },
            rowType = AiPlaceRecommendationRowType.ACTIVITIES,
            actionLabels = DEFAULT_ACTION_LABELS,
            actionsEnabled = false
        )
    }

    private suspend fun buildHotelRow(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        destination: String,
        checkIn: String? = null,
        checkOut: String? = null,
        requireExplicitDates: Boolean = false
    ): AiPlaceRecommendationRow? {
        val request = buildTravelRequest(
            intakeProfile = intakeProfile,
            profile = profile,
            destination = destination,
            explicitDateFrom = checkIn,
            explicitDateTo = checkOut,
            requireExplicitDates = requireExplicitDates
        ) ?: return null
        val itinerary = buildSearchItinerary(request)
        val hotelEvent = hotelProvider(request, itinerary).firstOrNull() ?: return null
        val selectedAndOptions = buildHotelCandidates(hotelEvent)
        if (selectedAndOptions.size < 2) return null

        return AiPlaceRecommendationRow(
            title = "Stay options in ${shortDestination(destination)}",
            subtitle = "Hotel results pulled from live search and trimmed for the current trip shape.",
            recommendations = selectedAndOptions.map { option ->
                AiPlaceRecommendation(
                    name = option.displayName("hotel").orEmpty().ifBlank { "Hotel stay" },
                    category = "Hotel",
                    area = option.detailValue(ATTR_AMENITIES).orEmpty(),
                    summary = buildHotelSummary(option),
                    matchReason = buildHotelMatchReason(intakeProfile, profile, option),
                    imageUrl = option.imageUrl
                )
            },
            rowType = AiPlaceRecommendationRowType.HOTELS,
            actionLabels = DEFAULT_ACTION_LABELS,
            actionsEnabled = false
        )
    }

    private fun buildHotelCandidates(event: TravelEvent): List<EventOption> {
        val selectedOption = event.options.firstOrNull { option -> option.selected }
        val selectedAsOption = selectedOption ?: EventOption(
            eventId = event.eventId,
            source = "serp",
            selected = true,
            imageUrl = event.imageUrl,
            photoUrls = event.photoUrls,
            details = event.details
        )

        return buildList {
            add(selectedAsOption)
            addAll(
                event.options.filterNot { option ->
                    option.optionId == selectedAsOption.optionId
                }
            )
        }.distinctBy { option ->
            option.detailValue(ATTR_HOTEL_NAME, "hotel_name", "name").orEmpty()
        }.take(PROVIDER_RESULT_LIMIT)
    }

    private fun buildRestaurantSummary(business: YelpBusiness): String {
        return listOfNotNull(
            business.categories.take(2).joinToString(" • ") { category -> category.title }
                .takeIf { value -> value.isNotBlank() },
            business.price?.takeIf { value -> value.isNotBlank() },
            business.location?.displayAddress?.firstOrNull()?.takeIf { value -> value.isNotBlank() }
        ).joinToString(" • ")
    }

    private fun buildActivitySummary(business: YelpBusiness): String {
        return listOfNotNull(
            business.categories.take(2).joinToString(" • ") { category -> category.title }
                .takeIf { value -> value.isNotBlank() },
            business.location?.displayAddress?.firstOrNull()?.takeIf { value -> value.isNotBlank() }
        ).joinToString(" • ")
    }

    private fun buildHotelSummary(option: EventOption): String {
        return listOfNotNull(
            option.detailValue(ATTR_HOTEL_RATING)?.let { rating -> "$rating rating" },
            option.detailValue(ATTR_HOTEL_CLASS)?.takeIf { value -> value.isNotBlank() },
            option.detailValue(ATTR_GROUP_RATE_PER_NIGHT)?.let { price ->
                runCatching { price.toDouble() }.getOrNull()?.let { amount ->
                    "\$${amount.toInt()}/night"
                }
            }
        ).joinToString(" • ")
    }

    private fun buildRestaurantMatchReason(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        business: YelpBusiness
    ): String {
        val normalizedCuisine = (intakeProfile.cuisinePreferences + profile.cuisinePreferences)
            .joinToString(" ")
            .lowercase(Locale.US)
        val categoryText = business.categories.joinToString(" ") { category -> category.title }
            .lowercase(Locale.US)

        return buildList {
            if (normalizedCuisine.isNotBlank() && normalizedCuisine.split(Regex("\\s+")).any { token -> token in categoryText }) {
                add("Cuisine match")
            }
            if (wantsFoodFocus(intakeProfile, profile)) add("Fits the food focus")
            if (intakeProfile.budgetLevel == AiBudgetLevel.BUDGET && business.price.orEmpty().length <= 2) add("Easier on budget")
            if (intakeProfile.budgetLevel == AiBudgetLevel.LUXURY && business.price.orEmpty().length >= 3) add("More upscale")
            business.rating.takeIf { rating -> rating > 0 }?.let { rating ->
                if (rating >= 4.5) add("Strong reviews")
            }
        }.take(2).joinToString(" • ").ifBlank { "Good local food fit." }
    }

    private fun buildActivityMatchReason(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        business: YelpBusiness
    ): String {
        val categoryText = business.categories.joinToString(" ") { category -> category.title }
            .lowercase(Locale.US)
        val interestText = (intakeProfile.interests + intakeProfile.destinationStyle + profile.interests)
            .joinToString(" ")
            .lowercase(Locale.US)

        return buildList {
            if (interestText.contains("culture") && categoryText.contains("museum")) add("Strong culture fit")
            if (interestText.contains("nightlife") && categoryText.contains("tour")) add("Good evening pick")
            if (interestText.contains("nature") && categoryText.contains("landmark")) add("Scenic stop")
            if (interestText.contains("art") && categoryText.contains("art")) add("Art-forward")
            business.rating.takeIf { rating -> rating > 0 }?.let { rating ->
                if (rating >= 4.5) add("Highly rated")
            }
        }.take(2).joinToString(" • ").ifBlank { "Fits the activity mix so far." }
    }

    private fun buildHotelMatchReason(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        option: EventOption
    ): String {
        val amenities = option.detailValue(ATTR_AMENITIES).orEmpty().lowercase(Locale.US)
        return buildList {
            if (intakeProfile.budgetLevel == AiBudgetLevel.BUDGET) {
                option.detailValue(ATTR_GROUP_RATE_PER_NIGHT)?.toDoubleOrNull()?.let { rate ->
                    if (rate <= 250.0) add("Safer on budget")
                }
            }
            if (intakeProfile.budgetLevel == AiBudgetLevel.LUXURY || profile.budgetSummary.contains("luxury", ignoreCase = true)) {
                option.detailValue(ATTR_HOTEL_CLASS)?.takeIf { value -> value.startsWith("4") || value.startsWith("5") }
                    ?.let { add("Luxury-leaning") }
            }
            if (intakeProfile.pace == AiTripPacePreference.RELAXED && amenities.contains("pool")) add("Good reset base")
            if (amenities.contains("wifi")) add("Solid hotel setup")
            option.detailValue(ATTR_HOTEL_RATING)?.toDoubleOrNull()?.let { rating ->
                if (rating >= 4.3) add("Strong rating")
            }
        }.take(2).joinToString(" • ").ifBlank { "Fits the stay setup so far." }
    }

    private fun mergeWithFallback(
        primary: AiPlaceRecommendationRow?,
        fallback: AiPlaceRecommendationRow?
    ): AiPlaceRecommendationRow? {
        primary ?: return null
        val fallbackRecommendations = fallback?.recommendations.orEmpty()
        if (fallbackRecommendations.isEmpty() || primary.recommendations.size >= PROVIDER_RESULT_LIMIT) {
            return primary
        }

        val mergedRecommendations = (primary.recommendations + fallbackRecommendations)
            .distinctBy { recommendation -> recommendation.name.lowercase(Locale.US) }
            .take(PROVIDER_RESULT_LIMIT)

        return primary.copy(recommendations = mergedRecommendations)
    }

    private fun rankBusinessesByTerms(
        businesses: List<YelpBusiness>,
        terms: List<String>
    ): List<YelpBusiness> {
        val normalizedTerms = terms
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { term -> term.lowercase(Locale.US) }
        if (normalizedTerms.isEmpty()) return businesses

        return businesses.sortedWith(
            compareByDescending<YelpBusiness> { business ->
                val categoryText = business.categories.joinToString(" ") { category ->
                    category.title
                }.lowercase(Locale.US)
                normalizedTerms.count { term -> term in categoryText }
            }.thenByDescending { business ->
                business.rating
            }.thenBy { business ->
                business.name
            }
        )
    }

    private fun inferCuratedRowType(row: AiPlaceRecommendationRow): AiPlaceRecommendationRowType {
        val categories = row.recommendations.map { recommendation -> recommendation.category.lowercase(Locale.US) }
        return when {
            categories.any { category -> "day trip" in category } -> AiPlaceRecommendationRowType.DAY_TRIPS
            categories.any { category -> "neighborhood" in category || "area" in category || "historic" in category } ->
                AiPlaceRecommendationRowType.NEIGHBORHOODS
            else -> AiPlaceRecommendationRowType.GENERAL
        }
    }

    private fun selectRowType(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): AiPlaceRecommendationRowType {
        val normalized = buildString {
            append(intakeProfile.mustHaves.joinToString(" "))
            append(' ')
            append(intakeProfile.notes.joinToString(" "))
            append(' ')
            append(profile.notes.joinToString(" "))
            append(' ')
            append(profile.interests.joinToString(" "))
            append(' ')
            append(profile.cuisinePreferences.joinToString(" "))
        }.lowercase(Locale.US)

        return when {
            listOf("hotel", "stay", "check-in", "check out", "hotel area", "where to stay").any { token -> token in normalized } ->
                AiPlaceRecommendationRowType.HOTELS

            wantsFoodFocus(intakeProfile, profile) -> AiPlaceRecommendationRowType.RESTAURANTS

            listOf("museum", "art", "culture", "history", "nightlife", "hiking", "activity", "tour").any { token -> token in normalized } ||
                intakeProfile.interests.isNotEmpty() || profile.interests.isNotEmpty() ->
                AiPlaceRecommendationRowType.ACTIVITIES

            else -> AiPlaceRecommendationRowType.HOTELS
        }
    }

    private fun wantsFoodFocus(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): Boolean {
        val normalized = buildString {
            append(intakeProfile.destinationStyle.joinToString(" "))
            append(' ')
            append(intakeProfile.interests.joinToString(" "))
            append(' ')
            append(intakeProfile.cuisinePreferences.joinToString(" "))
            append(' ')
            append(profile.interests.joinToString(" "))
            append(' ')
            append(profile.cuisinePreferences.joinToString(" "))
        }.lowercase(Locale.US)

        return intakeProfile.cuisinePreferences.isNotEmpty() ||
            listOf("food", "cuisine", "restaurant", "dining", "cafe", "food_first").any { token -> token in normalized }
    }

    private fun buildTravelRequest(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile,
        destination: String,
        explicitDateFrom: String? = null,
        explicitDateTo: String? = null,
        requireExplicitDates: Boolean = false
    ): TravelRequest? {
        val dateWindow = resolveSearchDateWindow(
            explicitDateFrom = explicitDateFrom,
            explicitDateTo = explicitDateTo,
            durationDays = intakeProfile.durationDays,
            requireExplicitDates = requireExplicitDates
        ) ?: return null
        val (dateFrom, dateTo) = dateWindow
        return TravelRequest(
            userId = "",
            origin = intakeProfile.origin.ifBlank { profile.origin },
            destination = destination,
            dateFrom = dateFrom,
            dateTo = dateTo,
            adults = inferAdults(intakeProfile, profile),
            children = inferChildren(intakeProfile, profile),
            travelStyle = inferTravelStyle(intakeProfile, profile),
            currency = "USD",
            budgetTotal = intakeProfile.budgetTotal ?: 0.0,
            interests = (intakeProfile.interests + profile.interests).distinct(),
            specialRequests = (intakeProfile.mustHaves + intakeProfile.notes + profile.notes)
                .distinct()
                .joinToString(". ")
        )
    }

    private fun resolveSearchDateWindow(
        explicitDateFrom: String?,
        explicitDateTo: String?,
        durationDays: Int?,
        requireExplicitDates: Boolean
    ): Pair<String, String>? {
        val normalizedDateFrom = explicitDateFrom?.trim().orEmpty()
        val normalizedDateTo = explicitDateTo?.trim().orEmpty()
        if (normalizedDateFrom.isNotBlank() || normalizedDateTo.isNotBlank()) {
            val start = parseIsoDate(normalizedDateFrom) ?: return null
            val end = parseIsoDate(normalizedDateTo) ?: return null
            if (end.isBefore(start)) return null
            return start.format(DateTimeFormatter.ISO_LOCAL_DATE) to end.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        if (requireExplicitDates) return null
        return buildDateWindow(durationDays)
    }

    private fun buildSearchItinerary(request: TravelRequest): Itinerary {
        val durationDays = daysBetween(request.dateFrom, request.dateTo)
        return Itinerary(
            itineraryId = UUID.randomUUID().toString(),
            userId = request.userId,
            tripName = "${shortDestination(request.destination)} stay ideas",
            destination = request.destination,
            origin = request.origin,
            dateFrom = request.dateFrom,
            dateTo = request.dateTo,
            durationDays = durationDays,
            currency = request.currency,
            travelStyle = request.travelStyle,
            adults = request.adults,
            children = request.children,
            createdAt = request.dateFrom,
            status = "draft",
            eventIds = emptyList()
        )
    }

    private fun buildDateWindow(durationDays: Int?): Pair<String, String> {
        val start = LocalDate.now().plusDays(DEFAULT_SEARCH_START_OFFSET_DAYS)
        val end = start.plusDays((durationDays ?: DEFAULT_DURATION_DAYS).coerceAtLeast(2).toLong())
        return start.format(DateTimeFormatter.ISO_LOCAL_DATE) to end.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun parseIsoDate(value: String): LocalDate? {
        return runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    private fun inferAdults(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): Int {
        val partyText = listOf(intakeProfile.partySummary, profile.partySummary)
            .joinToString(" ")
            .lowercase(Locale.US)
        return when {
            "family" in partyText || "kids" in partyText -> 2
            "friends" in partyText || "group" in partyText -> 4
            "two" in partyText || "couple" in partyText || intakeProfile.tripType == AiTripType.ROMANTIC -> 2
            intakeProfile.tripType == AiTripType.SOLO -> 1
            else -> 2
        }
    }

    private fun inferChildren(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): Int {
        val partyText = listOf(intakeProfile.partySummary, profile.partySummary)
            .joinToString(" ")
            .lowercase(Locale.US)
        return if ("family" in partyText || "kids" in partyText || intakeProfile.tripType == AiTripType.FAMILY) 2 else 0
    }

    private fun inferTravelStyle(
        intakeProfile: AiTripIntakeProfile,
        profile: AiTravelerProfile
    ): String {
        return when {
            intakeProfile.budgetLevel == AiBudgetLevel.LUXURY || profile.budgetSummary.contains("luxury", ignoreCase = true) -> "luxury"
            intakeProfile.budgetLevel == AiBudgetLevel.BUDGET || profile.budgetSummary.contains("budget", ignoreCase = true) -> "budget"
            else -> "comfort"
        }
    }

    private fun daysBetween(
        dateFrom: String,
        dateTo: String
    ): Int {
        return runCatching {
            val start = LocalDate.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
            val end = LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE)
            maxOf(1, end.toEpochDay().minus(start.toEpochDay()).toInt())
        }.getOrDefault(DEFAULT_DURATION_DAYS)
    }

    private fun shortDestination(destination: String): String {
        return destination.substringBefore(",").trim().ifBlank { destination }
    }

    private companion object {
        private const val PROVIDER_RESULT_LIMIT = 3
        private const val DEFAULT_DURATION_DAYS = 4
        private const val DEFAULT_SEARCH_START_OFFSET_DAYS = 30L
        private const val DESTINATION_CONFIDENCE_THRESHOLD = 0.65
        private val DEFAULT_ACTION_LABELS = listOf("Add", "Save", "Swap")
    }
}
