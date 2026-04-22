package com.example.travelcents.data.ai.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDestinationRecommendationEngineTest {

    private val engine = AiDestinationRecommendationEngine()

    @Test
    fun rankDestinations_returnsSeededMatchesForBeachFoodSignals() {
        val recommendations = engine.rankDestinations(
            AiTripIntakeProfile(
                budgetLevel = AiBudgetLevel.COMFORT,
                pace = AiTripPacePreference.RELAXED,
                destinationStyle = listOf("beach", "food_first", "warm_weather"),
                interests = listOf("Beach", "Food"),
                notes = listOf("Want somewhere warm for winter")
            )
        )

        assertEquals(3, recommendations.size)
        assertTrue(
            recommendations.any { recommendation ->
                recommendation.destination.contains("Bali", ignoreCase = true)
            }
        )
        assertTrue(
            recommendations.any { recommendation ->
                recommendation.destination.contains("Honolulu", ignoreCase = true) ||
                    recommendation.destination.contains("Cancun", ignoreCase = true)
            }
        )
        assertTrue(recommendations.all { recommendation -> recommendation.seedId != null })
        assertTrue(recommendations.all { recommendation -> recommendation.matchReason.isNotBlank() })
    }

    @Test
    fun rankDestinations_returnsEmptyWhenDestinationAlreadyLocked() {
        val recommendations = engine.rankDestinations(
            AiTripIntakeProfile(
                destination = "Tokyo, Japan",
                interests = listOf("Food")
            )
        )

        assertTrue(recommendations.isEmpty())
    }
}
