package com.example.travelcents.data.ai.chat

import java.util.Locale

object AiTravelerProfileReducer {
    private val knownDestinations = mapOf(
        "paris" to "Paris",
        "tokyo" to "Tokyo",
        "bali" to "Bali",
        "bangkok" to "Bangkok",
        "london" to "London",
        "rome" to "Rome",
        "barcelona" to "Barcelona",
        "cancun" to "Cancun",
        "new york" to "New York",
        "los angeles" to "Los Angeles",
        "san francisco" to "San Francisco",
        "dubai" to "Dubai",
        "singapore" to "Singapore",
        "honolulu" to "Honolulu",
        "mexico city" to "Mexico City",
        "seoul" to "Seoul",
        "sydney" to "Sydney"
    )

    private val interestKeywords = linkedMapOf(
        "food" to "Food",
        "restaurant" to "Food",
        "dining" to "Food",
        "museum" to "Culture",
        "culture" to "Culture",
        "art" to "Art",
        "history" to "History",
        "beach" to "Beach",
        "relax" to "Relaxation",
        "relaxing" to "Relaxation",
        "nature" to "Nature",
        "hiking" to "Hiking",
        "adventure" to "Adventure",
        "nightlife" to "Nightlife",
        "shopping" to "Shopping"
    )

    private val cuisineKeywords = linkedMapOf(
        "sushi" to "Sushi",
        "italian" to "Italian",
        "seafood" to "Seafood",
        "vegan" to "Vegan",
        "street food" to "Street Food",
        "coffee" to "Coffee",
        "dessert" to "Desserts"
    )

    fun merge(profile: AiTravelerProfile, userInput: String): AiTravelerProfile {
        val normalized = userInput.lowercase(Locale.US)

        return profile.copy(
            destination = inferDestination(profile.destination, normalized),
            origin = inferOrigin(profile.origin, normalized),
            dateWindow = inferDateWindow(profile.dateWindow, normalized),
            budgetSummary = inferBudget(profile.budgetSummary, normalized),
            partySummary = inferParty(profile.partySummary, normalized),
            travelPace = inferPace(profile.travelPace, normalized),
            interests = mergeKeywords(profile.interests, normalized, interestKeywords),
            cuisinePreferences = mergeKeywords(profile.cuisinePreferences, normalized, cuisineKeywords),
            dislikes = inferDislikes(profile.dislikes, normalized),
            notes = inferNotes(profile.notes, userInput)
        )
    }

    fun stageFor(profile: AiTravelerProfile): AiChatStage {
        val signalCount = listOf(
            profile.destination.takeIf { it.isNotBlank() },
            profile.budgetSummary.takeIf { it.isNotBlank() },
            profile.partySummary.takeIf { it.isNotBlank() },
            profile.travelPace.takeIf { it.isNotBlank() },
            profile.interests.takeIf { it.isNotEmpty() }?.joinToString(),
            profile.cuisinePreferences.takeIf { it.isNotEmpty() }?.joinToString()
        ).count { !it.isNullOrBlank() }

        return when {
            signalCount >= 4 -> AiChatStage.READY_FOR_OPTIONS
            signalCount >= 2 -> AiChatStage.SCOPING
            else -> AiChatStage.ONBOARDING
        }
    }

    private fun inferDestination(current: String, normalized: String): String {
        if (current.isNotBlank()) return current
        return knownDestinations.entries.firstOrNull { it.key in normalized }?.value.orEmpty()
    }

    private fun inferOrigin(current: String, normalized: String): String {
        if (current.isNotBlank()) return current
        val match = Regex("\\bfrom\\s+([a-z][a-z\\s]+)").find(normalized) ?: return current
        return match.groupValues[1]
            .trim()
            .split(" ")
            .joinToString(" ") { token -> token.replaceFirstChar(Char::uppercase) }
    }

    private fun inferDateWindow(current: String, normalized: String): String {
        if (current.isNotBlank()) return current
        return when {
            "weekend" in normalized -> "Weekend"
            "next month" in normalized -> "Next month"
            "summer" in normalized -> "Summer"
            "spring" in normalized -> "Spring"
            "fall" in normalized || "autumn" in normalized -> "Fall"
            "winter" in normalized -> "Winter"
            else -> current
        }
    }

    private fun inferBudget(current: String, normalized: String): String {
        val amountMatch = Regex("\\$\\s?([\\d,]+)").find(normalized)
        if (amountMatch != null) {
            return "Around \$${amountMatch.groupValues[1]}"
        }
        if (current.isNotBlank()) return current

        return when {
            "budget" in normalized || "cheap" in normalized || "affordable" in normalized ->
                "Budget-conscious"
            "luxury" in normalized || "upscale" in normalized || "high-end" in normalized ->
                "Luxury leaning"
            "comfort" in normalized || "mid-range" in normalized || "balanced" in normalized ->
                "Comfort leaning"
            else -> current
        }
    }

    private fun inferParty(current: String, normalized: String): String {
        if (current.isNotBlank()) return current
        return when {
            "solo" in normalized -> "Solo traveler"
            "couple" in normalized ||
                "two adults" in normalized ||
                "for two" in normalized ||
                "wife" in normalized ||
                "husband" in normalized ||
                "partner" in normalized ||
                "spouse" in normalized ||
                "boyfriend" in normalized ||
                "girlfriend" in normalized ||
                "fiance" in normalized ||
                "fiancee" in normalized ->
                "Two adults"
            "family" in normalized || "kids" in normalized || "children" in normalized ->
                "Family trip"
            "friends" in normalized || "group" in normalized ->
                "Group getaway"
            else -> current
        }
    }

    private fun inferPace(current: String, normalized: String): String {
        if (current.isNotBlank()) return current
        return when {
            "relaxed" in normalized || "chill" in normalized || "slow" in normalized ->
                "Relaxed"
            "packed" in normalized || "fast-paced" in normalized || "busy" in normalized ->
                "Packed"
            "balanced" in normalized || "mix" in normalized ->
                "Balanced"
            else -> current
        }
    }

    private fun mergeKeywords(
        current: List<String>,
        normalized: String,
        keywordMap: Map<String, String>
    ): List<String> {
        val matches = keywordMap.entries
            .filter { (keyword, _) -> keyword in normalized }
            .map { (_, value) -> value }

        return (current + matches).distinct()
    }

    private fun inferDislikes(current: List<String>, normalized: String): List<String> {
        val next = buildList {
            addAll(current)
            if ("avoid crowds" in normalized || "hate crowds" in normalized) add("Crowds")
            if ("avoid nightlife" in normalized) add("Nightlife")
            if ("avoid museums" in normalized) add("Museums")
        }
        return next.distinct()
    }

    private fun inferNotes(current: List<String>, rawInput: String): List<String> {
        val trimmed = rawInput.trim()
        if (trimmed.length < 18) return current
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return current

        return (current + trimmed.take(240)).takeLast(3)
    }
}
