package com.example.travelcents.data.ai.chat

import kotlin.random.Random

object AiChatCardCatalog {
    private const val STARTER_GROUP_ID = "starter_grid"
    private const val STARTER_CARD_COUNT = 6
    const val DEAD_END_GROUP_ID = "next_step_actions"

    private val starterPool = listOf(
        starterOption("beach_getaway", "Beach Getaway", "I want to plan a beach getaway."),
        starterOption("romantic_trip", "Romantic Trip", "I want to plan a romantic trip."),
        starterOption("nature_escape", "Nature Escape", "I want to plan a nature escape."),
        starterOption("city_break", "City Break", "I want to plan a city break."),
        starterOption("food_trip", "Food Trip", "I want to plan a food trip."),
        starterOption("family_trip", "Family Trip", "I want to plan a family trip."),
        starterOption("road_trip", "Road Trip", "I want to plan a road trip."),
        starterOption("spa_retreat", "Spa Retreat", "I want to plan a spa retreat."),
        starterOption("girls_trip", "Girls Trip", "I want to plan a girls trip."),
        starterOption("adventure_trip", "Adventure Trip", "I want to plan an adventure trip."),
        starterOption("arts_culture", "Arts and Culture", "I want to plan an arts and culture trip."),
        starterOption("nightlife_escape", "Nightlife Escape", "I want to plan a nightlife escape."),
        starterOption("pet_friendly", "Pet Friendly", "I want to plan a pet-friendly trip."),
        starterOption("shopping_weekend", "Shopping Weekend", "I want to plan a shopping weekend."),
        starterOption("wellness_reset", "Wellness Reset", "I want to plan a wellness reset."),
        starterOption("island_hopping", "Island Hopping", "I want to plan an island hopping trip."),
        starterOption("coffee_crawl", "Coffee Crawl", "I want to plan a coffee-focused city trip.")
    )

    fun starterCards(sessionId: String?): List<AiChatCardOption> {
        val seed = sessionId?.takeIf { it.isNotBlank() }?.hashCode()
        val randomizedPool = if (seed != null) {
            starterPool.shuffled(Random(seed))
        } else {
            starterPool.shuffled()
        }
        return randomizedPool.take(STARTER_CARD_COUNT)
    }

    fun deadEndActionGroup(): AiChatCardGroup {
        return AiChatCardGroup(
            id = DEAD_END_GROUP_ID,
            title = "What would you like to do next?",
            allowMultiple = false,
            options = listOf(
                AiChatCardOption(
                    id = "${DEAD_END_GROUP_ID}_suggest",
                    label = "Suggest destinations",
                    message = "Suggest 2 or 3 destinations that fit my profile so far.",
                    groupId = DEAD_END_GROUP_ID
                ),
                AiChatCardOption(
                    id = "${DEAD_END_GROUP_ID}_build",
                    label = "Build a starter trip",
                    message = "Build a starter trip from my profile so far.",
                    groupId = DEAD_END_GROUP_ID
                ),
                AiChatCardOption(
                    id = "${DEAD_END_GROUP_ID}_refine",
                    label = "Keep refining",
                    message = "Ask me a different question to refine the trip.",
                    groupId = DEAD_END_GROUP_ID
                )
            )
        )
    }

    fun timelineQuestionGroup(): AiChatCardGroup {
        return AiChatCardGroup(
            id = "travel_timeline",
            title = "When are you thinking of going?",
            allowMultiple = false,
            options = listOf(
                AiChatCardOption(id = "travel_timeline_this_month", label = "This month", message = "I'd like to go this month.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_next_month", label = "Next month", message = "I'd like to go next month.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_3_months", label = "In 3 months", message = "I'd like to go in about 3 months.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_6_months", label = "In 6 months", message = "I'd like to go in about 6 months.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_specific", label = "I have specific dates", message = "I have specific dates in mind.", groupId = "travel_timeline")
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
}
