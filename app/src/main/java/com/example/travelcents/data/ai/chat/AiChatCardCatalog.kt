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

    fun postDestinationDeadEndGroup(): AiChatCardGroup {
        return AiChatCardGroup(
            id = DEAD_END_GROUP_ID,
            title = "What would you like to do next?",
            allowMultiple = false,
            options = listOf(
                AiChatCardOption(
                    id = "${DEAD_END_GROUP_ID}_build",
                    label = "Build a starter trip",
                    message = "Build a starter trip itinerary from my profile.",
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
            topicPath = "travel_timeline",
            questionId = "travel_timeline",
            source = PlannerQuestionSource.APP_FALLBACK,
            title = "When are you thinking of going?",
            allowMultiple = false,
            options = listOf(
                AiChatCardOption(id = "travel_timeline_next_month", label = "Next month", message = "I'd like to go next month.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_3_months", label = "In 3 months", message = "I'd like to go in about 3 months.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_6_months", label = "In 6 months", message = "I'd like to go in about 6 months.", groupId = "travel_timeline"),
                AiChatCardOption(id = "travel_timeline_specific", label = "I have specific dates", message = "I have specific dates in mind.", groupId = "travel_timeline")
            )
        )
    }

    fun fallbackQuestionGroup(topicPath: String): AiChatCardGroup? {
        return when (topicPath.toPlannerTopicPath()) {
            "traveler_context", "party" -> plannerGroup(
                id = "traveler_context",
                topicPath = "traveler_context",
                title = "Who is this trip for?",
                allowMultiple = false,
                options = listOf(
                    option("traveler_context_solo", "Solo", "I'm planning a solo trip."),
                    option("traveler_context_couple", "Couple", "I'm planning for two adults."),
                    option("traveler_context_family", "Family", "I'm planning a family trip."),
                    option("traveler_context_friends", "Friends", "I'm planning a trip with friends.")
                )
            )

            "destination_style" -> plannerGroup(
                id = "destination_style",
                topicPath = "destination_style",
                title = "What kind of destination fits?",
                options = listOf(
                    option("destination_style_beach", "Beach", "A beach destination sounds right."),
                    option("destination_style_city", "City", "A city break sounds right."),
                    option("destination_style_nature", "Nature", "Nature and scenery matter most."),
                    option("destination_style_culture", "Culture", "Culture, history, and museums matter most."),
                    option("destination_style_food", "Food", "I want a food-first destination."),
                    option("destination_style_relaxing", "Relaxing", "I want somewhere relaxing.")
                )
            )

            "budget" -> plannerGroup(
                id = "budget",
                topicPath = "budget",
                title = "What budget range fits?",
                allowMultiple = false,
                options = listOf(
                    option("budget_value", "Value", "Keep the trip budget-conscious."),
                    option("budget_comfort", "Comfort", "Comfort matters, but keep it reasonable."),
                    option("budget_splurge", "Some splurges", "Mix value choices with a few splurges."),
                    option("budget_luxury", "Luxury", "Aim for a luxury-leaning trip.")
                )
            )

            "interests" -> plannerGroup(
                id = "interests",
                topicPath = "interests",
                title = "What should the trip center on?",
                options = listOf(
                    option("interests_food", "Food", "Food should be a major focus."),
                    option("interests_history", "History", "History and landmarks matter."),
                    option("interests_outdoors", "Outdoors", "Outdoor time matters."),
                    option("interests_shopping", "Shopping", "Shopping and markets matter."),
                    option("interests_nightlife", "Nightlife", "Nightlife should be included."),
                    option("interests_relaxation", "Relaxation", "Relaxation should be a priority.")
                )
            )

            "pace" -> plannerGroup(
                id = "pace",
                topicPath = "pace",
                title = "What pace feels right?",
                allowMultiple = false,
                options = listOf(
                    option("pace_relaxed", "Relaxed", "Keep the pace relaxed."),
                    option("pace_balanced", "Balanced", "Use a balanced pace."),
                    option("pace_packed", "Packed", "Pack in a lot each day."),
                    option("pace_flexible", "Flexible", "Keep the pace flexible.")
                )
            )

            "food_preferences" -> plannerGroup(
                id = "food_preferences",
                topicPath = "food_preferences",
                title = "What food should I look for?",
                options = listOf(
                    option("food_preferences_local", "Local favorites", "Prioritize local favorites."),
                    option("food_preferences_street_food", "Street food", "Prioritize street food."),
                    option("food_preferences_fine_dining", "Fine dining", "Include fine dining."),
                    option("food_preferences_cafes", "Cafes", "Include cafes and coffee spots."),
                    option("food_preferences_seafood", "Seafood", "Include seafood."),
                    option("food_preferences_vegetarian", "Vegetarian", "Include vegetarian-friendly food.")
                )
            )

            "activity_preferences" -> plannerGroup(
                id = "activity_preferences",
                topicPath = "activity_preferences",
                title = "What activities should stand out?",
                options = listOf(
                    option("activity_preferences_landmarks", "Landmarks", "Prioritize major landmarks."),
                    option("activity_preferences_museums", "Museums", "Include museums and galleries."),
                    option("activity_preferences_outdoors", "Outdoors", "Include outdoor activities."),
                    option("activity_preferences_tours", "Tours", "Include guided tours."),
                    option("activity_preferences_shows", "Shows", "Include shows or live events."),
                    option("activity_preferences_free_time", "Free time", "Leave room for wandering.")
                )
            )

            "travel_timeline", "date_window" -> timelineQuestionGroup()

            "duration" -> plannerGroup(
                id = "duration",
                topicPath = "duration",
                title = "How long should the trip be?",
                allowMultiple = false,
                options = listOf(
                    option("duration_weekend", "Weekend", "Plan for a weekend trip."),
                    option("duration_four_days", "4 days", "Plan for about 4 days."),
                    option("duration_week", "1 week", "Plan for about 1 week."),
                    option("duration_flexible", "Flexible", "I'm flexible on trip length.")
                )
            )

            "discovery_help" -> plannerGroup(
                id = "discovery_help",
                topicPath = "discovery_help",
                title = "Should I find options for you?",
                allowMultiple = false,
                options = listOf(
                    option("discovery_restaurants", "Restaurants", "Yes, find restaurant recommendations in the destination."),
                    option("discovery_activities", "Activities", "Yes, find activity options in the destination."),
                    option("discovery_both", "Both", "Yes, find restaurants and activities in the destination."),
                    option("discovery_skip", "Skip for now", "Skip finding places for now.")
                )
            )

            "must_haves" -> plannerGroup(
                id = "must_haves",
                topicPath = "must_haves",
                title = "What is a must-have?",
                options = listOf(
                    option("must_haves_walkable", "Walkable", "A walkable destination is a must-have."),
                    option("must_haves_easy_flights", "Easy flights", "Easy flights are a must-have."),
                    option("must_haves_great_food", "Great food", "Great food is a must-have."),
                    option("must_haves_scenic", "Scenic views", "Scenic views are a must-have.")
                )
            )

            else -> null
        }
    }

    private fun starterOption(id: String, label: String, message: String): AiChatCardOption {
        return AiChatCardOption(
            id = "$STARTER_GROUP_ID:$id",
            label = label,
            message = message,
            groupId = STARTER_GROUP_ID
        )
    }

    private fun plannerGroup(
        id: String,
        topicPath: String,
        title: String,
        options: List<AiChatCardOption>,
        allowMultiple: Boolean = true
    ): AiChatCardGroup {
        return AiChatCardGroup(
            id = id,
            title = title,
            options = options.map { option -> option.copy(groupId = id) },
            allowMultiple = allowMultiple,
            topicPath = topicPath,
            questionId = id,
            source = PlannerQuestionSource.APP_FALLBACK
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
