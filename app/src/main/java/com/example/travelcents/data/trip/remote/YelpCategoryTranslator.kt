package com.example.travelcents.data.trip.remote

object YelpCategoryTranslator {

    fun cuisinesToCategories(cuisines: List<String>): String {
        return cuisines.flatMap { toFoodAliases(it) }.distinct().joinToString(",")
    }

    fun activitiesToCategories(activities: List<String>): String {
        return activities.flatMap { toActivityAliases(it) }.distinct().joinToString(",")
    }

    fun toDietaryTerm(cuisines: List<String>): String? {
        val dietary = setOf("vegan", "vegetarian", "halal", "gluten-free", "gluten free", "kosher")
        return cuisines.firstOrNull { it.trim().lowercase() in dietary }
    }

    private fun toFoodAliases(raw: String): List<String> = when (raw.trim().lowercase()) {
        "italian" -> listOf("italian")
        "pizza" -> listOf("pizza", "italian")
        "pasta" -> listOf("italian")
        "japanese", "sushi" -> listOf("japanese", "sushi")
        "ramen" -> listOf("ramen")
        "mexican" -> listOf("mexican")
        "seafood" -> listOf("seafood")
        "chinese" -> listOf("chinese")
        "thai" -> listOf("thai")
        "indian" -> listOf("indpak")
        "mediterranean" -> listOf("mediterranean")
        "french" -> listOf("french")
        "american" -> listOf("newamerican", "tradamerican")
        "street food", "street" -> listOf("streetfood", "foodstands")
        "cafes", "coffee" -> listOf("cafes", "coffee")
        "desserts", "ice cream" -> listOf("desserts", "icecream")
        "bars", "craft beer" -> listOf("bars", "craftbeer")
        "tapas" -> listOf("tapas")
        "bbq", "barbecue" -> listOf("bbq")
        "sandwiches" -> listOf("sandwiches")
        "vegan", "vegetarian", "halal", "gluten-free", "gluten free", "kosher" -> emptyList()
        else -> emptyList()
    }

    private fun toActivityAliases(raw: String): List<String> = when (raw.trim().lowercase()) {
        "museums", "museum" -> listOf("museums")
        "art", "galleries", "art gallery" -> listOf("galleries", "artmuseums")
        "hiking", "hike" -> listOf("hiking", "parks")
        "kayaking", "water sports" -> listOf("kayaking", "watersports")
        "cycling", "biking" -> listOf("cycling")
        "nightlife" -> listOf("nightlife")
        "bars" -> listOf("bars")
        "jazz", "live music" -> listOf("jazzandblues", "musicvenues")
        "clubs", "dancing" -> listOf("danceclubs")
        "theater", "shows", "theatre" -> listOf("theater")
        "tours", "guided tours" -> listOf("tours")
        "landmarks" -> listOf("landmarks", "historicaltours")
        "wine", "wine tasting" -> listOf("winetastingroom", "wineries")
        "outdoor", "outdoors" -> listOf("parks", "hiking", "outdoormarkets")
        "sports" -> listOf("spectator_sports", "active")
        "shopping" -> listOf("shopping")
        "spa", "wellness" -> listOf("spas", "massage")
        else -> emptyList()
    }
}