package com.example.travelcents.data.ai.chat

import java.util.Locale
import kotlin.random.Random

object AiChatCardCatalog {
    private const val STARTER_GROUP_ID = "starter_grid"

    private val starterPool = listOf(
        starterOption("plan_trip", "Plan a trip", "Help me plan a trip from scratch."),
        starterOption("warm_places", "Warm places", "I want somewhere warm with a good vacation vibe."),
        starterOption("city_break", "City break", "I want a city break with great food and walkable neighborhoods."),
        starterOption("foodie_spots", "Foodie spots", "I want a food-first trip with standout local dining."),
        starterOption("beach_escape", "Beach escape", "I want a beach escape with a relaxed pace."),
        starterOption("weekend_getaway", "Weekend getaway", "I want a short weekend getaway."),
        starterOption("romantic_trip", "Romantic trip", "I want a romantic trip for two."),
        starterOption("family_trip", "Family trip", "I am planning a family-friendly trip."),
        starterOption("nature_hiking", "Nature and hiking", "I want nature, scenic views, and hiking."),
        starterOption("nightlife", "Nightlife", "I want nightlife and late-evening options."),
        starterOption("budget_friendly", "Budget-friendly", "Keep it budget-friendly."),
        starterOption("luxury_stay", "Luxury stay", "I want something upscale and polished.")
    )

    fun starterCards(sessionId: String?): List<AiChatCardOption> {
        val seed = sessionId?.hashCode()?.toLong() ?: 0L
        return starterPool
            .shuffled(Random(seed))
            .take(8)
    }

    fun nextFollowUpGroup(
        profile: AiTravelerProfile,
        lastUserInput: String
    ): AiChatCardGroup {
        val normalized = lastUserInput.lowercase(Locale.US)

        return when {
            shouldAskCuisine(profile, normalized) -> cuisineGroup()
            profile.partySummary.isBlank() -> partyGroup()
            profile.budgetSummary.isBlank() -> budgetGroup()
            profile.travelPace.isBlank() -> paceGroup()
            profile.interests.isEmpty() -> interestGroup()
            profile.destination.isBlank() -> destinationStyleGroup()
            else -> refinementGroup(profile)
        }
    }

    private fun shouldAskCuisine(profile: AiTravelerProfile, normalized: String): Boolean {
        return profile.cuisinePreferences.isEmpty() && (
            "city break" in normalized ||
                "food" in normalized ||
                "foodie" in normalized ||
                "restaurant" in normalized ||
                "dining" in normalized ||
                profile.interests.any { it.equals("Food", ignoreCase = true) }
            )
    }

    private fun cuisineGroup(): AiChatCardGroup {
        return group(
            id = "cuisine",
            title = "What food scene fits?",
            subtitle = "Pick one or more that sound right.",
            allowMultiple = true,
            options = listOf(
                option("street_food", "Street food", "Street food sounds right for this trip."),
                option("seafood", "Seafood", "Seafood should be part of the trip."),
                option("fine_dining", "Fine dining", "I want some fine dining options."),
                option("vegan", "Vegan", "I want strong vegan-friendly options."),
                option("coffee_bakeries", "Coffee and bakeries", "Coffee, bakeries, and cafe stops matter.")
            )
        )
    }

    private fun partyGroup(): AiChatCardGroup {
        return group(
            id = "party",
            title = "Who is traveling?",
            subtitle = "Choose the best fit.",
            allowMultiple = false,
            options = listOf(
                option("solo", "Solo", "This is a solo trip."),
                option("two_adults", "For two", "This trip is for two adults."),
                option("friends", "Friends trip", "This is a friends trip."),
                option("kids", "With kids", "We are traveling with kids.")
            )
        )
    }

    private fun budgetGroup(): AiChatCardGroup {
        return group(
            id = "budget",
            title = "How should it feel on budget?",
            subtitle = "You can change this later.",
            allowMultiple = false,
            options = listOf(
                option("budget", "Budget", "Keep it budget-friendly."),
                option("comfort", "Comfort", "Aim for comfort over luxury."),
                option("luxury", "Luxury", "Make this feel luxury leaning."),
                option("splurges", "A few splurges", "Keep it balanced with a few splurges.")
            )
        )
    }

    private fun paceGroup(): AiChatCardGroup {
        return group(
            id = "pace",
            title = "What pace do you want?",
            subtitle = "This helps shape the itinerary flow.",
            allowMultiple = false,
            options = listOf(
                option("relaxed", "Relaxed", "Keep the pace relaxed."),
                option("balanced", "Balanced", "Keep the pace balanced."),
                option("packed", "Packed", "I want a fuller schedule with more to do.")
            )
        )
    }

    private fun interestGroup(): AiChatCardGroup {
        return group(
            id = "interests",
            title = "What should the trip lean into?",
            subtitle = "Pick one or more.",
            allowMultiple = true,
            options = listOf(
                option("food_first", "Food-first", "Make this a food-first trip."),
                option("temples_culture", "Temples and culture", "I want temples, culture, and local history."),
                option("beach_time", "Beach time", "I want beach time built in."),
                option("nature_hiking", "Nature and hiking", "Nature, scenic views, and hiking matter."),
                option("nightlife", "Nightlife", "Nightlife and late-evening options matter.")
            )
        )
    }

    private fun destinationStyleGroup(): AiChatCardGroup {
        return group(
            id = "destination_style",
            title = "What destination style sounds right?",
            subtitle = "This helps narrow where to send you next.",
            allowMultiple = true,
            options = listOf(
                option("tropical", "Tropical beaches", "I want a tropical destination with beaches."),
                option("walkable_city", "Walkable city", "I want a walkable city with good neighborhoods."),
                option("temples_culture", "Temples and culture", "I want a destination with temples and strong culture."),
                option("nature_views", "Nature views", "I want mountains, water, or scenic nature views.")
            )
        )
    }

    private fun refinementGroup(profile: AiTravelerProfile): AiChatCardGroup {
        return group(
            id = "refinement",
            title = "What should I refine next?",
            subtitle = buildString {
                append("I already have ")
                append(profile.destination.ifBlank { "the vibe" })
                append(" and the basics.")
            },
            allowMultiple = true,
            options = listOf(
                option("less_crowded", "Less crowded", "Prefer somewhere less crowded."),
                option("walkable", "Keep it walkable", "Walkable neighborhoods matter."),
                option("great_food", "More food focus", "Lean harder into standout food options."),
                option("more_beach", "More beach time", "Include more beach time."),
                option("more_culture", "More culture", "Include more culture and local highlights.")
            )
        )
    }

    private fun starterOption(id: String, label: String, message: String): AiChatCardOption {
        return AiChatCardOption(
            id = "$STARTER_GROUP_ID:$id",
            label = label,
            message = message,
            groupId = STARTER_GROUP_ID
        )
    }

    private fun group(
        id: String,
        title: String,
        subtitle: String,
        allowMultiple: Boolean,
        options: List<AiChatCardOption>
    ): AiChatCardGroup {
        return AiChatCardGroup(
            id = id,
            title = title,
            subtitle = subtitle,
            options = options.map { option -> option.copy(groupId = id) },
            allowMultiple = allowMultiple
        )
    }

    private fun option(id: String, label: String, message: String): AiChatCardOption {
        return AiChatCardOption(
            id = id,
            label = label,
            message = message
        )
    }
}
