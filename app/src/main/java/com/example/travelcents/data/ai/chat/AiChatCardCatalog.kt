package com.example.travelcents.data.ai.chat

object AiChatCardCatalog {
    private const val STARTER_GROUP_ID = "starter_grid"
    const val DEAD_END_GROUP_ID = "next_step_actions"

    private val starterPool = listOf(
        starterOption("beach_getaway", "Beach Getaway", "I want to plan a beach getaway."),
        starterOption("romantic_trip", "Romantic Trip", "I want to plan a romantic trip."),
        starterOption("nature_escape", "Nature Escape", "I want to plan a nature escape."),
        starterOption("city_break", "City Break", "I want to plan a city break."),
        starterOption("food_trip", "Food Trip", "I want to plan a food trip."),
        starterOption("family_trip", "Family Trip", "I want to plan a family trip.")
    )

    fun starterCards(sessionId: String?): List<AiChatCardOption> {
        return starterPool
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

    private fun starterOption(id: String, label: String, message: String): AiChatCardOption {
        return AiChatCardOption(
            id = "$STARTER_GROUP_ID:$id",
            label = label,
            message = message,
            groupId = STARTER_GROUP_ID
        )
    }
}
