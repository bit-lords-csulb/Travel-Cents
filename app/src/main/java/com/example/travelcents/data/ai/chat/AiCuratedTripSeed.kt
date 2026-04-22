package com.example.travelcents.data.ai.chat

import java.util.Locale
import kotlin.math.abs

data class AiCuratedTripSeed(
    val id: String,
    val destination: String,
    val titleBase: String,
    val summary: String,
    val travelStyle: String,
    val defaultDurationDays: Int,
    val durationOptions: List<Int>,
    val tags: List<String>,
    val highlights: List<String>,
    val placeSuggestions: List<AiCuratedPlaceSuggestion>
) {
    val destinationKey: String = destination
        .substringBefore(",")
        .trim()
        .lowercase(Locale.US)

    fun toStarter(
        matchReason: String,
        durationDays: Int = defaultDurationDays
    ): AiCuratedTripStarter {
        val selectedDuration = nearestDuration(durationDays)
        val shortDestination = destination.substringBefore(",").trim()
        return AiCuratedTripStarter(
            title = "$selectedDuration-day $shortDestination $titleBase",
            destination = destination,
            durationDays = selectedDuration,
            travelStyle = travelStyle,
            summary = summary,
            matchReason = matchReason,
            source = AiCuratedTripSource.SEEDED,
            seedId = id,
            durationOptions = durationOptions.sorted().distinct(),
            highlights = highlights.take(3)
        )
    }

    private fun nearestDuration(durationDays: Int): Int {
        return durationOptions
            .sorted()
            .distinct()
            .minByOrNull { option -> abs(option - durationDays) }
            ?: defaultDurationDays
    }
}

data class AiCuratedPlaceSuggestion(
    val name: String,
    val category: String,
    val area: String = "",
    val summary: String,
    val tags: List<String> = emptyList()
)

object AiCuratedTripSeedCatalog {
    val seeds: List<AiCuratedTripSeed> = listOf(
        AiCuratedTripSeed(
            id = "bali_temple_beach",
            destination = "Bali, Indonesia",
            titleBase = "temple and beach",
            summary = "Split time between Ubud temples, south-coast beach clubs, and easy spa resets.",
            travelStyle = "comfort",
            defaultDurationDays = 5,
            durationOptions = listOf(4, 5, 7),
            tags = listOf("bali", "tropical", "beach", "culture", "temples", "romantic", "relaxed", "nature", "warm_weather"),
            highlights = listOf("Temples", "Beaches", "Spas"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Ubud", "Neighborhood", "Central Bali", "Temple-heavy base with rice terraces, cafes, and a calmer pace.", listOf("culture", "relaxed", "food")),
                AiCuratedPlaceSuggestion("Seminyak", "Beach area", "South Bali", "Easy resort zone for beach clubs, dinners, and polished stays.", listOf("beach", "food", "nightlife")),
                AiCuratedPlaceSuggestion("Uluwatu", "Coastal area", "Bukit Peninsula", "Cliffside sunsets, surf, and a more scenic low-key rhythm.", listOf("beach", "nature", "romantic"))
            )
        ),
        AiCuratedTripSeed(
            id = "tokyo_food_neighborhoods",
            destination = "Tokyo, Japan",
            titleBase = "food and neighborhoods",
            summary = "Mix standout neighborhoods, late-night food stops, and polished hotel zones.",
            travelStyle = "comfort",
            defaultDurationDays = 5,
            durationOptions = listOf(4, 5, 7),
            tags = listOf("tokyo", "city", "walkable", "food", "culture", "nightlife", "shopping", "food_first"),
            highlights = listOf("Sushi", "Districts", "Nightlife"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Shinjuku", "Neighborhood", "West Tokyo", "Dense transport hub with nightlife, hotels, and late food options.", listOf("nightlife", "food", "city")),
                AiCuratedPlaceSuggestion("Asakusa", "Neighborhood", "East Tokyo", "Traditional streets, temple access, and slower culture-forward mornings.", listOf("culture", "history", "walkable")),
                AiCuratedPlaceSuggestion("Shimokitazawa", "Neighborhood", "Setagaya", "Small-scale cafes, vintage shops, and a creative local feel.", listOf("food", "shopping", "walkable"))
            )
        ),
        AiCuratedTripSeed(
            id = "paris_romance_cafes",
            destination = "Paris, France",
            titleBase = "romance and cafes",
            summary = "Anchor the trip around cafes, museums, and slow evening walks by the Seine.",
            travelStyle = "comfort",
            defaultDurationDays = 4,
            durationOptions = listOf(3, 4, 6),
            tags = listOf("paris", "city", "romantic", "culture", "walkable", "food", "art", "luxury"),
            highlights = listOf("Cafes", "Museums", "River walks"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Le Marais", "Neighborhood", "Right Bank", "Walkable base for design shops, cafes, and easy dinner plans.", listOf("walkable", "food", "shopping")),
                AiCuratedPlaceSuggestion("Saint-Germain", "Neighborhood", "Left Bank", "Classic cafe district with galleries, romance, and polished hotels.", listOf("romantic", "food", "luxury")),
                AiCuratedPlaceSuggestion("Canal Saint-Martin", "Neighborhood", "10th Arr.", "More local stretch for wine bars, bakeries, and lower-key evenings.", listOf("food", "relaxed", "walkable"))
            )
        ),
        AiCuratedTripSeed(
            id = "rome_classics_trattorias",
            destination = "Rome, Italy",
            titleBase = "classics and trattorias",
            summary = "Build around iconic ruins, long lunches, and compact neighborhoods that stay easy on foot.",
            travelStyle = "comfort",
            defaultDurationDays = 4,
            durationOptions = listOf(3, 4, 6),
            tags = listOf("rome", "city", "culture", "history", "food", "walkable", "food_first"),
            highlights = listOf("History", "Pasta", "Piazzas"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Trastevere", "Neighborhood", "West of Tiber", "Strong evening food scene with lively lanes and easy wandering.", listOf("food", "nightlife", "walkable")),
                AiCuratedPlaceSuggestion("Centro Storico", "Historic core", "Central Rome", "Best for classic sites, piazzas, and short walking connections.", listOf("culture", "history", "walkable")),
                AiCuratedPlaceSuggestion("Monti", "Neighborhood", "Near Colosseum", "Stylish small streets with wine bars and a more local dinner feel.", listOf("food", "culture", "walkable"))
            )
        ),
        AiCuratedTripSeed(
            id = "barcelona_beach_tapas",
            destination = "Barcelona, Spain",
            titleBase = "beach and tapas",
            summary = "Blend tapas nights, design-heavy neighborhoods, and beach downtime in one compact city break.",
            travelStyle = "comfort",
            defaultDurationDays = 4,
            durationOptions = listOf(3, 4, 6),
            tags = listOf("barcelona", "city", "beach", "food", "nightlife", "walkable", "culture", "architecture"),
            highlights = listOf("Tapas", "Beach", "Gaudi"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Eixample", "Neighborhood", "Central Barcelona", "Clean grid, strong hotel base, and fast access to Gaudi highlights.", listOf("walkable", "architecture", "luxury")),
                AiCuratedPlaceSuggestion("El Born", "Neighborhood", "Old City", "Tighter streets, tapas bars, and late-night energy without leaving the center.", listOf("food", "nightlife", "walkable")),
                AiCuratedPlaceSuggestion("Barceloneta", "Beach area", "Waterfront", "Beach time plus seafood stops when you want the city and coast together.", listOf("beach", "food", "relaxed"))
            )
        ),
        AiCuratedTripSeed(
            id = "honolulu_beach_hikes",
            destination = "Honolulu, Hawaii",
            titleBase = "beach and hikes",
            summary = "Keep the days split between Waikiki, scenic hikes, and easy surf-beach time.",
            travelStyle = "comfort",
            defaultDurationDays = 5,
            durationOptions = listOf(4, 5, 7),
            tags = listOf("honolulu", "hawaii", "tropical", "beach", "nature", "family", "relaxed", "hiking", "warm_weather"),
            highlights = listOf("Surf", "Hikes", "Sunsets"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Waikiki", "Beach area", "South Shore", "Simple home base for beach access, dining, and resort-friendly logistics.", listOf("beach", "family", "food")),
                AiCuratedPlaceSuggestion("Kaimana", "Beach stretch", "East Waikiki", "Calmer shoreline with a more local feel and sunrise-friendly walks.", listOf("relaxed", "beach", "romantic")),
                AiCuratedPlaceSuggestion("Kakaako", "Neighborhood", "Urban Honolulu", "Best if you want food halls, coffee stops, and a non-resort city edge.", listOf("food", "city", "walkable"))
            )
        ),
        AiCuratedTripSeed(
            id = "cancun_resort_day_trips",
            destination = "Cancun, Mexico",
            titleBase = "resort and day trips",
            summary = "Use a resort-friendly base with beach days, cenote options, and simple day-trip escapes.",
            travelStyle = "comfort",
            defaultDurationDays = 4,
            durationOptions = listOf(4, 5, 6),
            tags = listOf("cancun", "tropical", "beach", "family", "romantic", "relaxed", "nightlife", "budget", "warm_weather"),
            highlights = listOf("Resorts", "Cenotes", "Beach clubs"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Hotel Zone", "Beach area", "Cancun strip", "Easiest base for resort stays, beach clubs, and direct ocean access.", listOf("beach", "nightlife", "family")),
                AiCuratedPlaceSuggestion("Isla Mujeres", "Day trip", "Offshore island", "Good add-on for calmer water, beach time, and a looser day pace.", listOf("beach", "relaxed", "romantic")),
                AiCuratedPlaceSuggestion("Puerto Morelos", "Coastal town", "South of Cancun", "Better if you want a quieter beach day and lower-key food scene.", listOf("relaxed", "food", "beach"))
            )
        ),
        AiCuratedTripSeed(
            id = "bangkok_food_temples",
            destination = "Bangkok, Thailand",
            titleBase = "food and temples",
            summary = "Start with street food, temple mornings, rooftop nights, and riverfront areas that stay easy to navigate.",
            travelStyle = "budget",
            defaultDurationDays = 4,
            durationOptions = listOf(3, 4, 6),
            tags = listOf("bangkok", "city", "food", "culture", "temples", "nightlife", "budget", "food_first"),
            highlights = listOf("Street food", "Temples", "Rooftops"),
            placeSuggestions = listOf(
                AiCuratedPlaceSuggestion("Old Town", "Historic area", "Rattanakosin", "Best for temple mornings, river views, and slower culture-heavy days.", listOf("culture", "temples", "history")),
                AiCuratedPlaceSuggestion("Ari", "Neighborhood", "North Bangkok", "Cafe-heavy local district with a calmer pace and strong food stops.", listOf("food", "relaxed", "walkable")),
                AiCuratedPlaceSuggestion("Sukhumvit", "Neighborhood", "Central Bangkok", "Easy transit, rooftop bars, and broad hotel choice for mixed itineraries.", listOf("nightlife", "food", "city"))
            )
        )
    )

    fun findSeed(seedId: String?): AiCuratedTripSeed? {
        return seeds.firstOrNull { seed -> seed.id == seedId }
    }

    fun findSeedByDestination(destination: String?): AiCuratedTripSeed? {
        val destinationKey = destination
            ?.substringBefore(",")
            ?.trim()
            ?.lowercase(Locale.US)
            .orEmpty()
        if (destinationKey.isBlank()) return null

        return seeds.firstOrNull { seed -> seed.destinationKey == destinationKey }
    }
}
