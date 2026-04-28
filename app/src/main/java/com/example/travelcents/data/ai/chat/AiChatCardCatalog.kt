package com.example.travelcents.data.ai.chat

object AiChatCardCatalog {
    private const val STARTER_GROUP_ID = "starter_grid"

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

    private fun starterOption(id: String, label: String, message: String): AiChatCardOption {
        return AiChatCardOption(
            id = "$STARTER_GROUP_ID:$id",
            label = label,
            message = message,
            groupId = STARTER_GROUP_ID
        )
    }
}
