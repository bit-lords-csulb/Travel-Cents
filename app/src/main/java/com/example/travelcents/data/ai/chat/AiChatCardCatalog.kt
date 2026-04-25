package com.example.travelcents.data.ai.chat

object AiChatCardCatalog {
    private const val STARTER_GROUP_ID = "starter_grid"

    private val starterPool = listOf(
        starterOption("beach_getaway", "Beach Getaway", "I want a beach getaway with a relaxed vibe."),
        starterOption("romantic_trip", "Romantic Trip", "I want a romantic trip for two."),
        starterOption("nature_escape", "Nature Escape", "I want a nature escape with scenic views and time outdoors."),
        starterOption("city_break", "City Break", "I want a city break with good food and walkable neighborhoods."),
        starterOption("food_trip", "Food Trip", "I want a food-focused trip with standout local restaurants."),
        starterOption("family_trip", "Family Trip", "I am planning a family-friendly trip.")
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
