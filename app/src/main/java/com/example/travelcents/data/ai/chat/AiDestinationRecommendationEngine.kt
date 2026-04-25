package com.example.travelcents.data.ai.chat

import java.util.Locale

data class AiDestinationRecommendationMatch(
    val destination: String,
    val summary: String,
    val matchReason: String,
    val seedId: String? = null,
    val score: Int
)

class AiDestinationRecommendationEngine(
    private val seeds: List<AiCuratedTripSeed> = AiCuratedTripSeedCatalog.seeds
) {
    fun rankDestinations(
        profile: AiTripIntakeProfile,
        limit: Int = 3
    ): List<AiDestinationRecommendationMatch> {
        if (profile.destination.isNotBlank()) return emptyList()
        if (!hasRecommendationSignals(profile)) return emptyList()

        val requestedTags = inferRequestedTags(profile)

        return seeds.mapNotNull { seed ->
            val score = scoreSeed(profile, seed, requestedTags)
            if (score <= 0) {
                null
            } else {
                AiDestinationRecommendationMatch(
                    destination = seed.destination,
                    summary = seed.summary,
                    matchReason = buildMatchReason(profile, seed, requestedTags),
                    seedId = seed.id,
                    score = score
                )
            }
        }.sortedWith(
            compareByDescending<AiDestinationRecommendationMatch> { recommendation -> recommendation.score }
                .thenBy { recommendation -> recommendation.destination }
        ).distinctBy { recommendation ->
            recommendation.destination.substringBefore(",").trim().lowercase(Locale.US)
        }.take(limit)
    }

    private fun hasRecommendationSignals(profile: AiTripIntakeProfile): Boolean {
        return profile.destinationStyle.isNotEmpty() ||
            profile.interests.isNotEmpty() ||
            profile.cuisinePreferences.isNotEmpty() ||
            profile.tripType != AiTripType.UNKNOWN ||
            profile.budgetLevel != AiBudgetLevel.UNKNOWN ||
            profile.pace != AiTripPacePreference.UNKNOWN ||
            profile.dateWindow.isNotBlank() ||
            profile.origin.isNotBlank() ||
            profile.notes.isNotEmpty()
    }

    private fun scoreSeed(
        profile: AiTripIntakeProfile,
        seed: AiCuratedTripSeed,
        requestedTags: Set<String>
    ): Int {
        var score = 0

        val overlapCount = seed.tags.count { tag -> tag in requestedTags }
        score += overlapCount * 10

        when (profile.tripType) {
            AiTripType.ROMANTIC -> if ("romantic" in seed.tags) score += 10
            AiTripType.FAMILY -> if ("family" in seed.tags) score += 10
            AiTripType.FRIENDS -> if ("nightlife" in seed.tags || "food" in seed.tags) score += 7
            AiTripType.SOLO -> if ("walkable" in seed.tags || "culture" in seed.tags) score += 5
            AiTripType.BUSINESS -> if ("city" in seed.tags || "walkable" in seed.tags) score += 6
            AiTripType.MIXED,
            AiTripType.UNKNOWN -> Unit
        }

        when (profile.budgetLevel) {
            AiBudgetLevel.BUDGET -> {
                if ("budget" in seed.tags || seed.travelStyle.equals("budget", ignoreCase = true)) score += 10
            }

            AiBudgetLevel.COMFORT -> {
                if (seed.travelStyle.equals("comfort", ignoreCase = true)) score += 7
            }

            AiBudgetLevel.LUXURY -> {
                if ("luxury" in seed.tags) score += 10
                else if (seed.travelStyle.equals("comfort", ignoreCase = true)) score += 3
            }

            AiBudgetLevel.MIXED -> {
                if (seed.travelStyle.equals("comfort", ignoreCase = true) || "luxury" in seed.tags) score += 5
            }

            AiBudgetLevel.UNKNOWN -> Unit
        }

        when (profile.pace) {
            AiTripPacePreference.RELAXED -> {
                if ("relaxed" in seed.tags || "beach" in seed.tags) score += 7
            }

            AiTripPacePreference.BALANCED -> {
                if ("city" in seed.tags || "walkable" in seed.tags || "culture" in seed.tags) score += 5
            }

            AiTripPacePreference.PACKED -> {
                if ("city" in seed.tags || "nightlife" in seed.tags || "food_first" in seed.tags) score += 7
            }

            AiTripPacePreference.UNKNOWN -> Unit
        }

        score += seasonalBonus(profile.dateWindow, seed)
        score += originBonus(profile.origin, seed)

        if (requestedTags.isNotEmpty() && overlapCount == 0 && score < 12) {
            return 0
        }

        return score
    }

    private fun buildMatchReason(
        profile: AiTripIntakeProfile,
        seed: AiCuratedTripSeed,
        requestedTags: Set<String>
    ): String {
        val reasons = buildList {
            if ("beach" in requestedTags && "beach" in seed.tags) add("Beach-friendly")
            if ("food" in requestedTags && seed.tags.any { tag -> tag == "food" || tag == "food_first" }) add("Strong food scene")
            if ("culture" in requestedTags && seed.tags.any { tag -> tag == "culture" || tag == "history" || tag == "temples" }) add("Culture-heavy")
            if ("nature" in requestedTags && seed.tags.any { tag -> tag == "nature" || tag == "hiking" }) add("Scenic outdoors")
            if (profile.tripType == AiTripType.ROMANTIC && "romantic" in seed.tags) add("Good for two")
            if (profile.tripType == AiTripType.FAMILY && "family" in seed.tags) add("Family-ready")
            if (profile.pace == AiTripPacePreference.RELAXED && "relaxed" in seed.tags) add("Relaxed pace")
            if (profile.budgetLevel == AiBudgetLevel.BUDGET && ("budget" in seed.tags || seed.travelStyle.equals("budget", ignoreCase = true))) {
                add("Budget-friendly")
            }
            if (profile.budgetLevel == AiBudgetLevel.COMFORT && seed.travelStyle.equals("comfort", ignoreCase = true)) {
                add("Comfort fit")
            }
            if (profile.budgetLevel == AiBudgetLevel.LUXURY && "luxury" in seed.tags) add("Luxury-leaning")
            seasonalReason(profile.dateWindow, seed)?.let(::add)
            originReason(profile.origin, seed)?.let(::add)
        }

        return reasons.take(2).joinToString(" • ").ifBlank {
            "Fits the direction you have set so far."
        }
    }

    private fun inferRequestedTags(profile: AiTripIntakeProfile): Set<String> {
        val notesText = buildString {
            append(profile.destinationStyle.joinToString(" "))
            append(' ')
            append(profile.interests.joinToString(" "))
            append(' ')
            append(profile.cuisinePreferences.joinToString(" "))
            append(' ')
            append(profile.mustHaves.joinToString(" "))
            append(' ')
            append(profile.notes.joinToString(" "))
            append(' ')
            append(profile.dateWindow)
        }.lowercase(Locale.US)

        return buildSet {
            profile.destinationStyle.forEach { style ->
                when (style.lowercase(Locale.US)) {
                    "food_first" -> {
                        add("food")
                        add("food_first")
                    }

                    "culture" -> add("culture")
                    "beach" -> add("beach")
                    "nature" -> add("nature")
                    "nightlife" -> add("nightlife")
                    "shopping" -> add("shopping")
                    "walkable_city" -> {
                        add("walkable")
                        add("city")
                    }

                    "warm_weather" -> add("warm_weather")
                }
            }

            if ("beach" in notesText || "coast" in notesText) add("beach")
            if ("food" in notesText || "dining" in notesText || "restaurant" in notesText || "cafe" in notesText) {
                add("food")
                add("food_first")
            }
            if ("culture" in notesText || "history" in notesText || "museum" in notesText || "art" in notesText || "temple" in notesText) {
                add("culture")
            }
            if ("nature" in notesText || "scenic" in notesText || "mountain" in notesText || "hiking" in notesText) add("nature")
            if ("nightlife" in notesText || "bars" in notesText || "clubs" in notesText || "late-night" in notesText) add("nightlife")
            if ("walkable" in notesText || "compact" in notesText) add("walkable")
            if ("warm" in notesText || "tropical" in notesText) add("warm_weather")
        }
    }

    private fun seasonalBonus(
        dateWindow: String,
        seed: AiCuratedTripSeed
    ): Int {
        val normalized = dateWindow.lowercase(Locale.US)
        return when {
            ("winter" in normalized || "december" in normalized || "january" in normalized || "february" in normalized) &&
                seed.tags.any { tag -> tag == "warm_weather" || tag == "beach" || tag == "tropical" } -> 8

            ("spring" in normalized || "fall" in normalized || "autumn" in normalized) &&
                seed.tags.any { tag -> tag == "city" || tag == "culture" || tag == "walkable" } -> 5

            ("summer" in normalized || "june" in normalized || "july" in normalized || "august" in normalized) &&
                seed.tags.any { tag -> tag == "beach" || tag == "nature" } -> 5

            else -> 0
        }
    }

    private fun seasonalReason(
        dateWindow: String,
        seed: AiCuratedTripSeed
    ): String? {
        val normalized = dateWindow.lowercase(Locale.US)
        return when {
            ("winter" in normalized || "december" in normalized || "january" in normalized || "february" in normalized) &&
                seed.tags.any { tag -> tag == "warm_weather" || tag == "beach" || tag == "tropical" } -> "Good winter sun"

            ("spring" in normalized || "fall" in normalized || "autumn" in normalized) &&
                seed.tags.any { tag -> tag == "city" || tag == "culture" || tag == "walkable" } -> "Great shoulder season"

            ("summer" in normalized || "june" in normalized || "july" in normalized || "august" in normalized) &&
                seed.tags.any { tag -> tag == "beach" || tag == "nature" } -> "Strong summer fit"

            else -> null
        }
    }

    private fun originBonus(
        origin: String,
        seed: AiCuratedTripSeed
    ): Int {
        val originRegion = originRegion(origin) ?: return 0
        val destinationRegion = destinationRegion(seed.destinationKey)
        return if (originRegion == destinationRegion) 4 else 0
    }

    private fun originReason(
        origin: String,
        seed: AiCuratedTripSeed
    ): String? {
        val originRegion = originRegion(origin) ?: return null
        val destinationRegion = destinationRegion(seed.destinationKey)
        if (originRegion != destinationRegion) return null

        return when (originRegion) {
            TravelRegion.PACIFIC -> "Better from the Pacific side"
            TravelRegion.ATLANTIC -> "Better from the Atlantic side"
            TravelRegion.EUROPE -> "Easy Europe fit"
            TravelRegion.ASIA_PACIFIC -> "Closer Asia-Pacific option"
        }
    }

    private fun originRegion(origin: String): TravelRegion? {
        val normalized = origin.lowercase(Locale.US)
        return when {
            normalized.isBlank() -> null
            listOf("los angeles", "san francisco", "seattle", "portland", "vancouver", "west coast", "hawaii").any { it in normalized } -> TravelRegion.PACIFIC
            listOf("new york", "boston", "miami", "toronto", "east coast", "washington dc", "atlanta").any { it in normalized } -> TravelRegion.ATLANTIC
            listOf("london", "paris", "rome", "barcelona", "europe").any { it in normalized } -> TravelRegion.EUROPE
            listOf("tokyo", "singapore", "bangkok", "sydney", "manila", "seoul", "asia").any { it in normalized } -> TravelRegion.ASIA_PACIFIC
            else -> null
        }
    }

    private fun destinationRegion(destinationKey: String): TravelRegion {
        return when (destinationKey) {
            "honolulu", "tokyo" -> TravelRegion.PACIFIC
            "cancun" -> TravelRegion.ATLANTIC
            "paris", "rome", "barcelona" -> TravelRegion.EUROPE
            "bali", "bangkok" -> TravelRegion.ASIA_PACIFIC
            else -> TravelRegion.PACIFIC
        }
    }

    private enum class TravelRegion {
        PACIFIC,
        ATLANTIC,
        EUROPE,
        ASIA_PACIFIC
    }
}
