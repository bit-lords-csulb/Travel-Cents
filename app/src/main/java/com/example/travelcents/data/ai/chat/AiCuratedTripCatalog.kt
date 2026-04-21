package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.Itinerary
import java.util.Locale
import kotlin.math.abs

class AiCuratedTripCatalog(
    private val tripRepository: TripRepository = FirestoreTripRepository()
) {
    fun recommendDestinationRecommendations(profile: AiTravelerProfile): AiDestinationRecommendationRow? {
        if (profile.destination.isNotBlank()) return null

        val recommendations = recommendSeededTrips(profile)
            .map { starter ->
                AiDestinationRecommendation(
                    destination = starter.destination,
                    summary = starter.summary,
                    matchReason = starter.matchReason,
                    seedId = starter.seedId
                )
            }
            .distinctBy { recommendation -> normalizeDestinationKey(recommendation.destination) }
            .take(3)

        if (recommendations.size < 2) return null

        return AiDestinationRecommendationRow(
            title = "Good destination fits",
            subtitle = "These locations match the direction you have set so far.",
            recommendations = recommendations
        )
    }

    suspend fun recommendTrips(
        profile: AiTravelerProfile,
        viewerUid: String?
    ): AiCuratedTripRow? {
        val curatedTrips = viewerUid
            ?.takeIf { it.isNotBlank() }
            ?.let { uid -> loadCuratedTrips(profile, uid) }
            .orEmpty()
        val seededTrips = recommendSeededTrips(profile)

        return when {
            curatedTrips.isNotEmpty() && seededTrips.isNotEmpty() -> {
                AiCuratedTripRow(
                    title = "Choose a starter",
                    subtitle = if (profile.destination.isNotBlank()) {
                        "Saved matches plus curated templates for ${displayDestination(profile.destination)}."
                    } else {
                        "Saved matches plus curated hotspot starters that fit the direction so far."
                    },
                    trips = (curatedTrips.take(2) + seededTrips.take(2))
                        .distinctBy(::uniqueStarterKey)
                        .take(4)
                )
            }

            curatedTrips.isNotEmpty() -> {
                AiCuratedTripRow(
                    title = "Start from a saved trip",
                    subtitle = "These curated trips match the direction you've set so far.",
                    trips = curatedTrips.take(3)
                )
            }

            seededTrips.isNotEmpty() -> {
                AiCuratedTripRow(
                    title = if (profile.destination.isNotBlank()) {
                        "Curated ${displayDestination(profile.destination)} starters"
                    } else {
                        "Curated hotspot starters"
                    },
                    subtitle = if (profile.destination.isNotBlank()) {
                        "Editable destination templates you can tune before building the trip."
                    } else {
                        "These destinations fit the vibe, pace, and budget you've described."
                    },
                    trips = seededTrips.take(3)
                )
            }

            else -> {
                buildGeneratedStarter(profile)?.let { starter ->
                    AiCuratedTripRow(
                        title = "Start from scratch",
                        subtitle = "I couldn't find a saved or curated match, so here's a fresh starter.",
                        trips = listOf(starter)
                    )
                }
            }
        }
    }

    fun recommendPlaceRecommendations(profile: AiTravelerProfile): AiPlaceRecommendationRow? {
        val destinationKey = normalizeDestinationKey(profile.destination)
        if (destinationKey.isBlank()) return null

        val seed = AiCuratedTripSeedCatalog.seeds.firstOrNull { candidate ->
            candidate.destinationKey == destinationKey
        } ?: return null

        val requestedTags = inferProfileTags(profile)
        val recommendations = seed.placeSuggestions
            .sortedWith(
                compareByDescending<AiCuratedPlaceSuggestion> { suggestion ->
                    suggestion.tags.count { tag -> tag in requestedTags }
                }.thenBy { suggestion -> suggestion.name }
            )
            .take(3)
            .map { suggestion ->
                AiPlaceRecommendation(
                    name = suggestion.name,
                    category = suggestion.category,
                    area = suggestion.area,
                    summary = suggestion.summary,
                    matchReason = buildPlaceMatchReason(requestedTags, suggestion)
                )
            }

        if (recommendations.size < 2) return null

        return AiPlaceRecommendationRow(
            title = "Places to start in ${displayDestination(seed.destination)}",
            subtitle = "Areas and spots that fit the trip direction you have described.",
            recommendations = recommendations
        )
    }

    fun adjustStarterDuration(
        starter: AiCuratedTripStarter,
        durationDays: Int
    ): AiCuratedTripStarter {
        val availableOptions = starter.durationOptions.sorted().distinct()
        if (availableOptions.size <= 1) return starter

        val selectedDuration = availableOptions.minByOrNull { option ->
            abs(option - durationDays)
        } ?: starter.durationDays
        if (selectedDuration == starter.durationDays) return starter

        return when (starter.source) {
            AiCuratedTripSource.SEEDED -> {
                AiCuratedTripSeedCatalog.findSeed(starter.seedId)
                    ?.toStarter(
                        matchReason = starter.matchReason,
                        durationDays = selectedDuration
                    )
                    ?.copy(id = starter.id)
                    ?: starter.copy(durationDays = selectedDuration)
            }

            AiCuratedTripSource.FIRESTORE,
            AiCuratedTripSource.GENERATED -> starter.copy(durationDays = selectedDuration)
        }
    }

    private suspend fun loadCuratedTrips(
        profile: AiTravelerProfile,
        viewerUid: String
    ): List<AiCuratedTripStarter> {
        val destinationKey = normalizeDestinationKey(profile.destination)
        if (destinationKey.isBlank()) return emptyList()

        return runCatching {
            tripRepository.getTripSummaries(viewerUid)
        }.getOrElse {
            emptyList()
        }.filterNot { itinerary ->
            itinerary.status.equals("archived", ignoreCase = true)
        }.mapNotNull { itinerary ->
            val score = scoreTrip(profile, itinerary, destinationKey)
            if (score <= 0) return@mapNotNull null

            ScoredTrip(
                score = score,
                starter = itinerary.toStarter(
                    matchReason = buildMatchReason(profile, itinerary)
                )
            )
        }.sortedWith(
            compareByDescending<ScoredTrip> { scored -> scored.score }
                .thenByDescending { scored -> scored.starter.durationDays }
                .thenBy { scored -> scored.starter.title }
        ).map { scored -> scored.starter }
            .distinctBy(::uniqueStarterKey)
    }

    private fun recommendSeededTrips(profile: AiTravelerProfile): List<AiCuratedTripStarter> {
        val destinationKey = normalizeDestinationKey(profile.destination)
        val preferredDurationDays = inferDurationDays(profile)

        val scoredSeeds = AiCuratedTripSeedCatalog.seeds.mapNotNull { seed ->
            val score = scoreSeed(profile, seed, destinationKey)
            if (score <= 0) return@mapNotNull null
            ScoredSeed(
                score = score,
                starter = seed.toStarter(
                    matchReason = buildSeedMatchReason(profile, seed),
                    durationDays = preferredDurationDays
                )
            )
        }.sortedWith(
            compareByDescending<ScoredSeed> { scored -> scored.score }
                .thenByDescending { scored -> scored.starter.durationDays }
                .thenBy { scored -> scored.starter.title }
        )

        return when {
            destinationKey.isNotBlank() -> scoredSeeds
                .filter { scored -> normalizeDestinationKey(scored.starter.destination) == destinationKey }
                .map { scored -> scored.starter }
                .distinctBy(::uniqueStarterKey)

            else -> scoredSeeds
                .map { scored -> scored.starter }
                .distinctBy(::uniqueStarterKey)
                .take(3)
        }
    }

    private fun scoreTrip(
        profile: AiTravelerProfile,
        itinerary: Itinerary,
        destinationKey: String
    ): Int {
        val itineraryDestinationKey = normalizeDestinationKey(itinerary.destination)
        val destinationScore = when {
            itineraryDestinationKey == destinationKey -> 90
            itinerary.destination.contains(profile.destination, ignoreCase = true) -> 70
            profile.destination.contains(itinerary.destination, ignoreCase = true) -> 55
            else -> 0
        }
        if (destinationScore == 0) return 0

        var score = destinationScore

        val requestedStyle = inferTravelStyle(profile)
        if (requestedStyle.isNotBlank() && itinerary.travelStyle.equals(requestedStyle, ignoreCase = true)) {
            score += 18
        }

        val requestedDays = inferDurationDays(profile)
        if (requestedDays > 0 && itinerary.durationDays > 0) {
            val distance = abs(itinerary.durationDays - requestedDays)
            score += when {
                distance == 0 -> 14
                distance == 1 -> 10
                distance <= 2 -> 6
                else -> 0
            }
        }

        if (profile.partySummary.contains("family", ignoreCase = true) && itinerary.children > 0) {
            score += 10
        }

        if (profile.partySummary.contains("two", ignoreCase = true) && itinerary.adults == 2 && itinerary.children == 0) {
            score += 8
        }

        return score
    }

    private fun scoreSeed(
        profile: AiTravelerProfile,
        seed: AiCuratedTripSeed,
        destinationKey: String
    ): Int {
        if (destinationKey.isNotBlank() && seed.destinationKey != destinationKey) {
            return 0
        }

        val requestedTags = inferProfileTags(profile)
        val overlapCount = seed.tags.count { tag -> tag in requestedTags }

        var score = if (destinationKey.isNotBlank()) 120 else overlapCount * 12
        if (score == 0) return 0

        val requestedStyle = inferTravelStyle(profile)
        if (requestedStyle.isNotBlank() && requestedStyle.equals(seed.travelStyle, ignoreCase = true)) {
            score += 10
        }

        if ("food" in requestedTags && seed.tags.any { tag -> tag == "food" || tag == "food_first" }) {
            score += 8
        }

        if ("romantic" in requestedTags && "romantic" in seed.tags) {
            score += 8
        }

        if ("family" in requestedTags && "family" in seed.tags) {
            score += 8
        }

        if ("relaxed" in requestedTags && "relaxed" in seed.tags) {
            score += 6
        }

        if ("nightlife" in requestedTags && "nightlife" in seed.tags) {
            score += 6
        }

        if ("beach" in requestedTags && "beach" in seed.tags) {
            score += 6
        }

        if ("culture" in requestedTags && "culture" in seed.tags) {
            score += 6
        }

        if ("nature" in requestedTags && "nature" in seed.tags) {
            score += 6
        }

        if (destinationKey.isBlank() && overlapCount < 2) {
            return 0
        }

        return score
    }

    private fun inferProfileTags(profile: AiTravelerProfile): Set<String> {
        val normalized = buildString {
            append(profile.destination)
            append(' ')
            append(profile.partySummary)
            append(' ')
            append(profile.travelPace)
            append(' ')
            append(profile.budgetSummary)
            append(' ')
            append(profile.interests.joinToString(" "))
            append(' ')
            append(profile.cuisinePreferences.joinToString(" "))
            append(' ')
            append(profile.notes.joinToString(" "))
        }.lowercase(Locale.US)

        return buildSet {
            if (normalized.contains("warm")) add("warm_weather")
            if (normalized.contains("tropical")) add("tropical")
            if (normalized.contains("beach")) add("beach")
            if (normalized.contains("city")) add("city")
            if (normalized.contains("walkable")) add("walkable")
            if (normalized.contains("food") || normalized.contains("restaurant") || normalized.contains("dining") || normalized.contains("cafe")) {
                add("food")
                add("food_first")
            }
            if (normalized.contains("culture") || normalized.contains("history") || normalized.contains("museum") || normalized.contains("art")) {
                add("culture")
            }
            if (normalized.contains("temple")) add("temples")
            if (normalized.contains("nightlife") || normalized.contains("late-night") || normalized.contains("bars") || normalized.contains("clubs")) {
                add("nightlife")
            }
            if (normalized.contains("nature") || normalized.contains("scenic")) add("nature")
            if (normalized.contains("hiking") || normalized.contains("hike")) add("hiking")
            if (normalized.contains("romantic") || normalized.contains("couple") || normalized.contains("honeymoon") || normalized.contains("for two")) {
                add("romantic")
            }
            if (normalized.contains("family") || normalized.contains("kids") || normalized.contains("children")) add("family")
            if (normalized.contains("luxury") || normalized.contains("splurge")) add("luxury")
            if (normalized.contains("budget") || normalized.contains("affordable")) add("budget")
            if (normalized.contains("comfort")) add("comfort")
            if (normalized.contains("relaxed")) add("relaxed")
            if (normalized.contains("balanced")) add("balanced")
            if (normalized.contains("packed")) add("packed")
        }
    }

    private fun buildMatchReason(
        profile: AiTravelerProfile,
        itinerary: Itinerary
    ): String {
        val reasons = buildList {
            add("Matches ${itinerary.destination}")

            val requestedStyle = inferTravelStyle(profile)
            if (requestedStyle.isNotBlank() && itinerary.travelStyle.equals(requestedStyle, ignoreCase = true)) {
                add(styleLabel(itinerary.travelStyle))
            }

            val requestedDays = inferDurationDays(profile)
            if (requestedDays > 0 && itinerary.durationDays > 0) {
                add("${itinerary.durationDays}-day fit")
            }
        }

        return reasons.joinToString(" • ")
    }

    private fun buildSeedMatchReason(
        profile: AiTravelerProfile,
        seed: AiCuratedTripSeed
    ): String {
        val requestedTags = inferProfileTags(profile)
        val reasons = buildList {
            if (profile.destination.isNotBlank()) {
                add("Curated ${displayDestination(seed.destination)}")
            } else {
                add("Fits your vibe")
            }

            if ("food" in requestedTags && seed.tags.any { tag -> tag == "food" || tag == "food_first" }) {
                add("Food-first")
            }
            if ("romantic" in requestedTags && "romantic" in seed.tags) {
                add("Great for two")
            }
            if ("family" in requestedTags && "family" in seed.tags) {
                add("Family-ready")
            }
            if ("beach" in requestedTags && "beach" in seed.tags) {
                add("Beach fit")
            }
            if ("culture" in requestedTags && "culture" in seed.tags) {
                add("Culture fit")
            }
            if ("nightlife" in requestedTags && "nightlife" in seed.tags) {
                add("Nightlife fit")
            }
        }

        return reasons.take(3).joinToString(" • ")
    }

    private fun buildPlaceMatchReason(
        requestedTags: Set<String>,
        suggestion: AiCuratedPlaceSuggestion
    ): String {
        val reasons = buildList {
            when {
                "food" in requestedTags && suggestion.tags.any { tag -> tag == "food" || tag == "food_first" } ->
                    add("Great for food")
                "romantic" in requestedTags && "romantic" in suggestion.tags ->
                    add("Good for two")
                "nightlife" in requestedTags && "nightlife" in suggestion.tags ->
                    add("Good at night")
                "culture" in requestedTags && suggestion.tags.any { tag -> tag == "culture" || tag == "history" || tag == "temples" } ->
                    add("Strong culture fit")
                "beach" in requestedTags && "beach" in suggestion.tags ->
                    add("Beach fit")
                "relaxed" in requestedTags && "relaxed" in suggestion.tags ->
                    add("Relaxed pace")
            }

            if (suggestion.area.isNotBlank()) {
                add(suggestion.area)
            }
        }

        return reasons.take(2).joinToString(" • ").ifBlank { "Fits your current trip direction." }
    }

    private fun Itinerary.toStarter(matchReason: String): AiCuratedTripStarter {
        return AiCuratedTripStarter(
            title = tripName,
            destination = destination,
            durationDays = durationDays.coerceAtLeast(1),
            travelStyle = travelStyle.ifBlank { "comfort" },
            summary = buildSummary(this),
            matchReason = matchReason,
            source = AiCuratedTripSource.FIRESTORE,
            tripKey = TripKey(ownerUid = ownerUid, tripId = itineraryId)
        )
    }

    private fun buildGeneratedStarter(profile: AiTravelerProfile): AiCuratedTripStarter? {
        val destination = profile.destination.ifBlank { return null }
        val style = inferTravelStyle(profile).ifBlank { "comfort" }
        val durationDays = inferDurationDays(profile).coerceAtLeast(4)
        val summary = buildString {
            append("${durationDays}-day ")
            append(destination)
            append(" starter")
            profile.travelPace.takeIf { it.isNotBlank() }?.let { pace ->
                append(" with a ")
                append(pace.lowercase())
                append(" pace")
            }
            val highlights = (profile.interests + profile.cuisinePreferences)
                .distinct()
                .take(2)
            if (highlights.isNotEmpty()) {
                append(" focused on ")
                append(highlights.joinToString(" and ") { it.lowercase() })
            }
        }

        return AiCuratedTripStarter(
            title = "${durationDays}-day ${displayDestination(destination)}",
            destination = destination,
            durationDays = durationDays,
            travelStyle = style,
            summary = summary,
            matchReason = "Fresh starter built from your current chat signals.",
            source = AiCuratedTripSource.GENERATED
        )
    }

    private fun buildSummary(itinerary: Itinerary): String {
        val style = styleLabel(itinerary.travelStyle)
        return buildString {
            append("${itinerary.durationDays.coerceAtLeast(1)}-day ")
            append(itinerary.destination)
            if (style.isNotBlank()) {
                append(" • ")
                append(style)
            }
            if (itinerary.adults > 0 || itinerary.children > 0) {
                append(" • ")
                append(
                    when {
                        itinerary.children > 0 -> "family-ready"
                        itinerary.adults == 2 -> "for two"
                        itinerary.adults > 2 -> "group-friendly"
                        else -> "solo-friendly"
                    }
                )
            }
        }
    }

    private fun inferTravelStyle(profile: AiTravelerProfile): String {
        val budget = profile.budgetSummary.lowercase(Locale.US)
        return when {
            "luxury" in budget -> "luxury"
            "budget" in budget -> "budget"
            "comfort" in budget || "splurge" in budget -> "comfort"
            else -> ""
        }
    }

    private fun inferDurationDays(profile: AiTravelerProfile): Int {
        val dateWindow = profile.dateWindow.lowercase(Locale.US)
        return when {
            "weekend" in dateWindow -> 3
            "week" in dateWindow -> 7
            "summer" in dateWindow || "spring" in dateWindow || "fall" in dateWindow || "winter" in dateWindow -> 6
            else -> 4
        }
    }

    private fun styleLabel(rawStyle: String): String {
        return when (rawStyle.lowercase(Locale.US)) {
            "budget" -> "Budget"
            "luxury" -> "Luxury"
            "comfort" -> "Comfort"
            else -> rawStyle.replaceFirstChar { it.uppercase() }
        }
    }

    private fun normalizeDestinationKey(destination: String): String {
        return destination
            .substringBefore(",")
            .trim()
            .lowercase(Locale.US)
    }

    private fun displayDestination(destination: String): String {
        return destination.substringBefore(",").trim().ifBlank { destination }
    }

    private fun uniqueStarterKey(starter: AiCuratedTripStarter): String {
        return starter.tripKey?.let { key -> "${key.ownerUid}:${key.tripId}" }
            ?: starter.seedId?.let { seedId -> "$seedId:${starter.durationDays}" }
            ?: starter.id
    }

    private data class ScoredTrip(
        val score: Int,
        val starter: AiCuratedTripStarter
    )

    private data class ScoredSeed(
        val score: Int,
        val starter: AiCuratedTripStarter
    )
}
